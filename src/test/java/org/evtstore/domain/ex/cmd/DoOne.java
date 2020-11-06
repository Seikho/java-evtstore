package org.evtstore.domain.ex.cmd;

import org.evtstore.Command;

public class DoOne extends Command {

  public Integer one;

  public DoOne(Integer one) {
    super();
    this.one = one;
  }
}
