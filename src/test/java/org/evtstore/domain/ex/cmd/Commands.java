package org.evtstore.domain.ex.cmd;

public class Commands {
  public static DoOne doOne(Integer one) {
    return new DoOne(one);
  }

  public static DoTwo doTwo(Integer two) {
    return new DoTwo(two);
  }
}
