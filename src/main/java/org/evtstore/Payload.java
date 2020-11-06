package org.evtstore;

import com.eclipsesource.json.JsonObject;

public class Payload {
  public String type;
  public JsonObject event = new JsonObject();

  public Payload(String type, JsonObject event) {
    this.type = type;
    this.event = event;
    event.add("type", type);
  }

  public Payload(String type) {
    event.add("type", type);
  }

  public Payload add(String name, String value) {
    event.add(name, value);
    return this;
  }

  public Payload add(String name, Integer value) {
    event.add(name, value);
    return this;
  }

  public Payload add(String name, JsonObject value) {
    event.add(name, value);
    return this;
  }

  public Payload add(String name, boolean value) {
    event.add(name, value);
    return this;
  }

  public Payload add(String name, long value) {
    event.add(name, value);
    return this;
  }

  public Payload add(String name, double value) {
    event.add(name, value);
    return this;
  }
}
