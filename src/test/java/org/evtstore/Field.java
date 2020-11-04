package org.evtstore;

public class Field<T> {
  private T val;

  public Field(T value) {
    val = value;
  }

  public T get() {
    return val;
  }

  public void set(T value) {
    val = value;
  }
}
