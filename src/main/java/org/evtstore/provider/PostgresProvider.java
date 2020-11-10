package org.evtstore.provider;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.function.Supplier;

import org.evtstore.Aggregate;
import org.evtstore.Provider;
import org.evtstore.StoreEvent;
import org.evtstore.VersionConflictException;

public class PostgresProvider implements Provider {
  private Integer limit = 0;
  private Supplier<Connection> getConnection;
  private String events;
  private String bookmarks;

  public PostgresProvider(Supplier<Connection> getConnection, String events, String bookmarks, Integer limit) {
    this.getConnection = getConnection;
    this.events = events;
    this.bookmarks = bookmarks;
    this.limit = limit;
  }

  public PostgresProvider(Supplier<Connection> getConnection, String events, String bookmarks) {
    this.getConnection = getConnection;
    this.events = events;
    this.bookmarks = bookmarks;
  }

  @Override
  public String getPosition(String bookmark) {
    String query = String.format("SELECT position FROM %s WHERE bookmark = ?", bookmarks);

    try {
      ResultSet result = query(query, bookmark).getResultSet();

      if (result == null) {
        return "0";
      }

      if (result.next()) {
        int pos = result.getInt(1);
        return String.valueOf(pos);
      }

      return "0";
    } catch (SQLException ex) {
      return "0";
    } catch (VersionConflictException e) {
      return "0";
    }
  }

  @Override
  public void setPosition(String bookmark, String position) {
    String query = String.format(
        "INSERT INTO %s (bookmark, position) VALUES (?, ?) ON CONFLICT (bookmark) UPDATE SET position = ?", bookmarks);
    try {
      query(query, bookmark, position);
    } catch (VersionConflictException e) {
    }
  }

  @Override
  public Iterable<StoreEvent> getEventsFor(String stream, String aggregateId, String position) {
    String query = String.format("SELECT position, version, timestamp, event FROM %s "
        + "WHERE stream = ? AND aggregate_id = ? AND position > ? ORDER BY version ASC", events);

    if (limit > 0) {
      query += " LIMIT " + limit;
    }

    ArrayList<StoreEvent> events = new ArrayList<StoreEvent>();
    try {
      int pos = position.equals("") ? 0 : Integer.parseInt(position);
      ResultSet rs = query(query, stream, aggregateId, pos).getResultSet();

      while (rs.next()) {
        StoreEvent event = new StoreEvent();
        event.stream = stream;
        event.aggregateId = aggregateId;
        event.position = rs.getString(1);
        event.version = rs.getInt(2);
        event.timestamp = rs.getTimestamp(3);
        event.event = rs.getString(4);
        events.add(event);
      }
    } catch (SQLException ex) {
      ex.printStackTrace();
    } catch (VersionConflictException e) {
    }
    return events;
  }

  @Override
  public Iterable<StoreEvent> getEventsFrom(String stream, String position) {
    return getEventsFrom(new String[] { stream }, position);
  }

  @Override
  public Iterable<StoreEvent> getEventsFrom(String[] streams, String position) {
    String query = String.format("SELECT position, version, timestamp, event, stream, aggregate_id FROM %s "
        + "WHERE stream IN (%s) AND position > ? ORDER BY position ASC", events, toStreamList(streams));
    if (limit > 0) {
      query += " LIMIT " + limit;
    }

    ArrayList<StoreEvent> events = new ArrayList<StoreEvent>();
    try {
      int pos = position.equals("") ? 0 : Integer.parseInt(position);
      ResultSet rs = query(query, pos).getResultSet();

      while (rs.next()) {
        StoreEvent event = new StoreEvent();
        event.position = rs.getString(1);
        event.version = rs.getInt(2);
        event.timestamp = rs.getTimestamp(3);
        event.event = rs.getString(4);
        event.stream = rs.getString(5);
        event.aggregateId = rs.getString(6);
        events.add(event);
      }
    } catch (SQLException ex) {
    } catch (VersionConflictException e) {
    }
    return events;
  }

  @Override
  public <Agg extends Aggregate> StoreEvent append(StoreEvent event, Agg agg) throws VersionConflictException {
    String query = String.format(
        "INSERT INTO %s (stream, version, aggregate_id, timestamp, event) VALUES (?, ?, ?, ?, ?) RETURNING position",
        events);
    int version = agg.version + 1;

    try {
      ResultSet rs = query(query, event.stream, version, agg.aggregateId, event.timestamp, event.event).getResultSet();
      StoreEvent stored = event.clone();
      rs.next();
      stored.position = String.valueOf(rs.getInt(1));
      return stored;

    } catch (SQLException ex) {
      // What do?
      throw new VersionConflictException();
    }

  }

  public void migrate() {

    try (Connection conn = getConnection.get()) {
      DatabaseMetaData meta = conn.getMetaData();
      ResultSet eventsTable = meta.getTables(null, null, events, null);
      ResultSet bookmarksTable = meta.getTables(null, null, bookmarks, null);

      boolean hasEvents = eventsTable.next();
      boolean hasBookmarks = bookmarksTable.next();

      if (!hasEvents) {
        query(String.format(
            "CREATE TABLE \"%s\" (\"position\" bigserial primary key, \"version\" integer, \"stream\" varchar(255), \"aggregate_id\" varchar(255), \"timestamp\" timestamptz, \"event\" text)",
            events));

        query(String.format(
            "ALTER TABLE \"%s\" ADD CONSTRAINT \"events_stream_position_unique\" unique (\"stream\", \"position\")",
            events));
        query(String.format(
            "ALTER TABLE \"%s\" ADD CONSTRAINT \"events_stream_aggregate_id_version_unique\" unique (\"stream\", \"aggregate_id\", \"version\")",
            events));
      }

      if (!hasBookmarks) {
        query(String.format("CREATE TABLE \"%s\" (\"bookmark\" varchar(255), \"position\" bigint)", bookmarks));

        query(String.format("ALTER TABLE \"%s\" ADD CONSTRAINT \"bookmarks_pkey\" primary key (\"bookmark\")",
            bookmarks));
      }
    } catch (SQLException ex) {
      ex.printStackTrace();
    } catch (VersionConflictException ex) {
    }
  }

  private PreparedStatement query(String query, Object... values) throws VersionConflictException {
    try (Connection conn = getConnection.get()) {
      PreparedStatement ps = conn.prepareStatement(query);
      for (int i = 0; i < values.length; i++) {
        Object value = values[i];
        int idx = i + 1;

        if (value instanceof Date) {
          Date d = (Date) value;
          java.sql.Date date = new java.sql.Date(d.getTime());
          ps.setDate(idx, date);
          continue;
        }

        // ps.setObject(idx, value);
        // continue;

        if (value instanceof Integer) {
          ps.setInt(idx, (Integer) value);
          continue;
        }

        if (value instanceof String) {
          ps.setString(idx, (String) value);
          continue;
        }
      }

      ps.executeQuery();
      return ps;

    } catch (SQLException ex) {
      String lowered = query.toLowerCase();
      String msg = ex.getMessage();
      if (msg.contains("violates unique constraint")) {
        throw new VersionConflictException();
      }

      if (lowered.contains("select") || lowered.contains("returning")) {
        System.out.println("Query failed:");
        System.out.println(query);
        System.out.println(ex.getSQLState());
        ex.printStackTrace();
        return null;
      }

      return null;
    }
  }

  private String toStreamList(String[] streams) {
    Object[] stream = Arrays.stream(streams).map(s -> "'" + s + "'").toArray();
    String clause = "";

    for (int i = 0; i < stream.length; i++) {
      if (i == stream.length - 1) {
        clause += stream[i];
        break;
      }

      clause += stream[i] + ", ";
    }
    return clause;
  }

  @Override
  public Integer getLimit() {
    return limit;
  }
}
