package org.evtstore;

public interface Provider<Agg extends Aggregate> {
  public Iterable<StoreEvent> getEventsFor(String stream, String aggregateId, String position);

  public Iterable<StoreEvent> getEventsFrom(String stream, String position);

  public void setPosition(String bookmark, String position);

  public String getPosition(String bookmark);

  public StoreEvent append(StoreEvent event, Agg agg);
}
