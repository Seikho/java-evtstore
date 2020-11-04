package org.evtstore;

import java.util.Date;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

public class Event {
  public String stream;
  public String aggregateId;
  public Integer version;
  public Date timestamp;
  public JsonObject payload;
  public String position;
  public String type;

  public Event(StoreEvent event) {
    this.stream = event.stream;
    this.aggregateId = event.aggregateId;
    this.version = event.version;
    this.timestamp = event.timestamp;
    this.position = event.position;
    this.payload = Json.parse(event.event).asObject();
    this.type = this.payload.get("type").asString();
  }
}
