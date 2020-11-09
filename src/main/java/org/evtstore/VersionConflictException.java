package org.evtstore;

public class VersionConflictException extends Exception {

  private static final long serialVersionUID = -1486759932420383890L;

  public VersionConflictException(String message) {
    super(message);
  }

  public VersionConflictException() {
    super("Version conflict error");
  }
}
