package org.evtstore.provider;

import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.evtstore.Aggregate;
import org.evtstore.Provider;
import org.evtstore.StoreEvent;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;

public class Neo4jProvider<Agg extends Aggregate> implements Provider<Agg> {
  private Session session;
  private String events;
  private String bookmarks;

  public Neo4jProvider(Session session, String eventsLabel, String bookmarksLabel) {
    this.session = session;
    this.events = eventsLabel;
    this.bookmarks = bookmarksLabel;
  }

  @Override
  public String getPosition(String bookmark) {
    Map<String, Object> params = Map.of("bm", bookmark);
    var query = String.format("MATCH (bm: %s) WHERE bm.bookmark = $bm RETURN bm", this.bookmarks);
    var result = session.run(query, params);
    var position = result.single().get("position").asString();
    return position;
  }

  @Override
  public void setPosition(String bookmark, String position) {
    Map<String, Object> params = Map.of("bookmark", bookmark, "position", position);
    params.put("bookmark", bookmark);
    params.put("position", position);
    var query = String.format("MATCH (bm: %s) WHERE bm.bookmark = $bookmark SET bm.position = $position RETURN bm",
        bookmarks);
    session.run(query, params);
  }

  @Override
  public Iterable<StoreEvent> getEventsFrom(String stream, String position) {
    var streams = new String[] { stream };
    return getEventsFrom(streams, position);
  }

  @Override
  public Iterable<StoreEvent> getEventsFrom(String[] streams, String position) {
    Map<String, Object> params = Map.of("position", position);
    var streamList = toStreamList(streams);
    var query = String.format("MATCH (ev: %s) WHERE ev.stream IN [%s] AND ev.position > datetime($position) "
        + "RETURN ev ORDER BY ev.position ASC", events, streamList);
    var results = session.run(query, params).list(r -> convert(r));
    return results;
  }

  @Override
  public Iterable<StoreEvent> getEventsFor(String stream, String aggregateId, String position) {
    Map<String, Object> params = Map.of("stream", stream, "id", aggregateId, "pos", position);
    var query = String
        .format("MATCH (ev: %s) WHERE ev.stream = $stream AND ev.position > datetime($pos) AND ev.aggregateId = $id "
            + "RETURN ev ORDER BY ev.position ASC");
    var results = session.run(query, params).list(r -> convert(r));
    return results;
  }

  @Override
  public StoreEvent append(StoreEvent event, Agg agg) {
    var streamIdVersion = event.stream + "_" + agg.aggregateId + "_" + (agg.version + 1);
    Map<String, Object> params = Map.of("stream", event.stream, "version", agg.version + 1, "timestamp",
        toISOString(event.timestamp), "event", event.event, "streamIdVersion", streamIdVersion);
    var query = String.format("WITH $stream + \"_\" + toString(datetime.transaction()) as streampos "
        + "CREATE (ev: %s { stream: $stream, position: datetime.transaction(), version: $version, timestamp: datetime($timestamp), aggregateId: $id, event: $event, _streamPosition: streampos, _steamIdVersion: $streamIdVersion }) RETURN ev",
        events);
    var result = session.run(query, params).single();
    var stored = convert(result);
    return stored;
  }

  public void migrate() {
    var trx = session.beginTransaction();
    {
      var query = String.format(
          "CREATE INDEX %s_stream_position " + "IF NOT EXISTS " + "FOR (ev: %s) " + "ON (ev.stream, ev.position)",
          events, events);
      trx.run(query);
    }
    {
      var query = String.format(
          "CREATE INDEX %s_stream_id_pos IF NOT EXISTS " + "FOR (ev: %s) ON (ev.stream, ev.aggregateId, ev.position)",
          events, events);
      trx.run(query);
    }
    {
      var query = String.format(
          "CREATE CONSTRAINT %s_streampos_unique IF NOT EXISTS " + "ON (ev: %s) ASSERT ev._streamPos IS UNIQUE", events,
          events);
      trx.run(query);
    }
    {
      var query = String.format("CREATE CONSTRAINT %s_streamidversion_unique IF NOT EXISTS "
          + "ON (ev: %s) ASSERT ev._streamIdVersion IS UNIQUE", events, events);
      trx.run(query);
    }

    trx.commit();
  }

  private String toStreamList(String[] streams) {
    var stream = Arrays.stream(streams).map(s -> "'" + s + "'").toArray();
    var clause = "";

    for (int i = 0; i < stream.length; i++) {
      if (i == stream.length - 1) {
        clause += stream[i];
        break;
      }

      clause += stream[i] + ", ";
    }
    return clause;
  }

  private StoreEvent convert(Record record) {
    var ev = new StoreEvent();
    ev.position = record.get("position").asString();
    ev.aggregateId = record.get("aggregateId").asString();
    ev.event = record.get("event").asString();
    ev.stream = record.get("stream").asString();
    ev.version = record.get("version").asInt();
    var timestamp = record.get("timestamp").asString();
    var dt = OffsetDateTime.parse(timestamp);
    ev.timestamp = new Date(dt.toInstant().toEpochMilli());
    return ev;
  }

  private String toISOString(Date date) {
    var tz = TimeZone.getTimeZone("UTC");
    var df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    df.setTimeZone(tz);
    var iso = df.format(date);
    return iso;
  }

}
