package org.evtstore;

import java.util.Date;

public class StoreEvent {
  public String stream;
  public String aggregateId;
  public Double version;
  public Date timestamp;
  public String event;
  public String position;

  public StoreEvent(Payload payload) {
    this.timestamp = new Date();
    this.event = payload.event.toString();
  }

  public StoreEvent() {

  }

  public StoreEvent clone() {
    var newEvent = new StoreEvent();
    newEvent.stream = stream;
    newEvent.aggregateId = aggregateId;
    newEvent.version = version;
    newEvent.timestamp = new Date();
    newEvent.timestamp.setTime(timestamp.getTime());
    newEvent.event = event;
    newEvent.position = position;
    return newEvent;
  }
}
