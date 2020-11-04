package org.evtstore;

import com.eclipsesource.json.JsonObject;

public class Payload {
  public String type;
  public JsonObject event;

  public Payload(String type, JsonObject event) {
    this.type = type;
    this.event = event;
    event.add("type", type);
  }
}
