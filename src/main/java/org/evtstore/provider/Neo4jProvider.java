package org.evtstore.provider;

import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.evtstore.Aggregate;
import org.evtstore.Provider;
import org.evtstore.StoreEvent;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Value;

public class Neo4jProvider implements Provider {
  private Driver driver;
  private String events;
  private String bookmarks;
  private Integer limit = 1000;

  public Neo4jProvider(Driver driver, String eventsLabel, String bookmarksLabel) {
    this.driver = driver;
    this.events = eventsLabel;
    this.bookmarks = bookmarksLabel;
  }

  public void setLimit(Integer limit) {
    this.limit = limit;
  }

  @Override
  public Integer getLimit() {
    return limit;
  }

  @Override
  public String getPosition(String bookmark) {
    Map<String, Object> params = Map.of("bm", bookmark);
    var query = String.format("MATCH (bm: %s) WHERE bm.bookmark = $bm RETURN bm", this.bookmarks);

    try (var session = driver.session()) {
      var result = session.run(query, params)
          .list(r -> r.get("bm").get("position").asOffsetDateTime().format(DateTimeFormatter.ISO_DATE_TIME));
      if (result.isEmpty()) {
        return toISOString(new Date(0));
      }

      var pos = result.get(0);
      return pos;
    }
  }

  @Override
  public void setPosition(String bookmark, String position) {
    Map<String, Object> params = Map.of("bookmark", bookmark, "position", position);
    var query = String.format(
        "MERGE (bm: %s { bookmark: $bookmark } ) ON CREATE SET bm.position = $position ON MATCH SET bm.position = $position",
        bookmarks);

    try (var session = driver.session()) {
      session.run(query, params);
    }
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
    var lim = limit > 0 ? String.format("LIMIT %d", limit) : "";
    var query = String.format("MATCH (ev: %s) WHERE ev.stream IN [%s] AND ev.position > datetime($position) "
        + "RETURN ev ORDER BY ev.position ASC %s", events, streamList, lim);

    try (var session = driver.session()) {
      var results = session.run(query, params).list(r -> convert(r.get("ev")));
      return results;
    }
  }

  @Override
  public Iterable<StoreEvent> getEventsFor(String stream, String aggregateId, String position) {
    var pos = position.equals("") ? toISOString(new Date(0)) : position;
    Map<String, Object> params = Map.of("stream", stream, "id", aggregateId, "pos", pos);

    var lim = limit > 0 ? String.format("LIMIT %d", limit) : "";
    var query = String
        .format("MATCH (ev: %s) WHERE ev.stream = $stream AND ev.position > datetime($pos) AND ev.aggregateId = $id "
            + "RETURN ev ORDER BY ev.position ASC %s", events, lim);

    try (var session = driver.session()) {
      var results = session.run(query, params).list(r -> convert(r.get("ev")));
      return results;
    }
  }

  @Override
  public <Agg extends Aggregate> StoreEvent append(StoreEvent event, Agg agg) {
    var streamIdVersion = event.stream + "_" + agg.aggregateId + "_" + (agg.version + 1);
    Map<String, Object> params = Map.of("stream", event.stream, "version", agg.version + 1, "timestamp",
        toISOString(event.timestamp), "event", event.event, "streamIdVersion", streamIdVersion, "id",
        event.aggregateId);
    var query = String.format("WITH $stream + \"_\" + toString(datetime.transaction()) as streampos "
        + "CREATE (ev: %s { stream: $stream, position: datetime.transaction(), version: $version, timestamp: datetime($timestamp), aggregateId: $id, event: $event, _streamPosition: streampos, _streamIdVersion: $streamIdVersion }) RETURN ev",
        events);

    try (var session = driver.session()) {
      var result = session.run(query, params);
      var ev = result.single().get("ev");

      var stored = event.clone();
      stored.version = ev.get("version").asInt();
      stored.position = ev.get("position").asOffsetDateTime().format(DateTimeFormatter.ISO_DATE_TIME);
      return stored;
    }
  }

  public void migrate() {
    try (var session = driver.session()) {
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
            "CREATE CONSTRAINT %s_streampos_unique IF NOT EXISTS " + "ON (ev: %s) ASSERT ev._streamPos IS UNIQUE",
            events, events);
        trx.run(query);
      }
      {
        var query = String.format("CREATE CONSTRAINT %s_streamidversion_unique IF NOT EXISTS "
            + "ON (ev: %s) ASSERT ev._streamIdVersion IS UNIQUE", events, events);
        trx.run(query);
      }

      trx.commit();
    }
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

  private StoreEvent convert(Value record) {
    var ev = new StoreEvent();
    ev.position = record.get("position").asOffsetDateTime().format(DateTimeFormatter.ISO_DATE_TIME);
    ev.aggregateId = record.get("aggregateId").asString();
    ev.event = record.get("event").asString();
    ev.stream = record.get("stream").asString();
    ev.version = record.get("version").asInt();
    var timestamp = record.get("timestamp").asOffsetDateTime().format(DateTimeFormatter.ISO_DATE_TIME);
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
