package org.evtstore.domain.ex.cmd;

public enum Cmd {
  DoOne("DoOne"), DoTwo("DoTwo"), DoThree("DoThree");

  private String cmd;

  Cmd(String cmd) {
    this.cmd = cmd;
  }

  public String get() {
    return cmd;
  }
}
