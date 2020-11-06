package org.evtstore.domain.ex.cmd;

import org.evtstore.Command;

public class DoTwo extends Command {

  public Integer two;

  public DoTwo(Integer two) {
    super();
    this.two = two;
  }

}
