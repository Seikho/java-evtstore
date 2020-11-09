package org.evtstore;

public interface Provider {
  public Iterable<StoreEvent> getEventsFor(String stream, String aggregateId, String position);

  public Iterable<StoreEvent> getEventsFrom(String stream, String position);

  public Iterable<StoreEvent> getEventsFrom(String[] streams, String position);

  public void setPosition(String bookmark, String position);

  public String getPosition(String bookmark);

  public <Agg extends Aggregate> StoreEvent append(StoreEvent event, Agg agg) throws VersionConflictException;

  public Integer getLimit();
}
