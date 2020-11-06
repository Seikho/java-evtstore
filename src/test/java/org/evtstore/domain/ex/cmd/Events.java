package org.evtstore.domain.ex.cmd;

import org.evtstore.Payload;

public class Events {
  public static Payload evOne(Integer one) {
    var payload = new Payload("EvOne");
    payload.add("one", one);
    return payload;
  }

  public static Payload evTwo(Integer two) {
    var payload = new Payload("EvTwo");
    payload.add("two", two);
    return payload;
  }

  public static Payload evThree(Integer three) {
    var payload = new Payload("EvThree");
    payload.add("two", three);
    return payload;
  }
}
