package org.evtstore.domain.ex.cmd;

import org.evtstore.Command;

public class DoOne extends Command {

  public Integer one;

  public DoOne(Integer one) {
    super("DoOne");
    this.one = one;
  }

  public DoOne() {
    super("DoOne");
  }
}
