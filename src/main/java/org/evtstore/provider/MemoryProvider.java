package org.evtstore.provider;

import org.evtstore.Aggregate;
import org.evtstore.Provider;
import org.evtstore.StoreEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Collections2;

public class MemoryProvider<Agg extends Aggregate> implements Provider<Agg> {
  private ArrayList<StoreEvent> events = new ArrayList<StoreEvent>();
  private Map<String, String> bookmarks = new HashMap<String, String>();

  @Override
  public Iterable<StoreEvent> getEventsFor(String stream, String id, String position) {
    var events = Collections2.filter(this.events,
        event -> event.stream.equals(stream) && event.aggregateId.equals(id) && event.position.compareTo(position) > 0);
    return events;
  }

  @Override
  public StoreEvent append(StoreEvent event, Agg agg) {
    var toPersist = event.clone();
    toPersist.version = agg.version + 1;
    toPersist.position = String.valueOf(events.size());
    this.events.add(toPersist);
    return toPersist;
  }

  @Override
  public Iterable<StoreEvent> getEventsFrom(String stream, String position) {
    var events = Collections2.filter(this.events,
        ev -> ev.stream.equals(stream) && ev.position.compareTo(position) > 0);
    return events;
  }

  @Override
  public String getPosition(String bookmark) {
    var bm = this.bookmarks.getOrDefault(bookmark, "");
    return bm;
  }

  @Override
  public void setPosition(String bookmark, String position) {
    this.bookmarks.put(bookmark, position);
  }

}
