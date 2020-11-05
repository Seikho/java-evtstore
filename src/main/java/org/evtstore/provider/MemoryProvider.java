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
  public Iterable<StoreEvent> getEventsFrom(String stream, String position) {
    var streams = new String[] { stream };
    return getEventsFrom(streams, position);
  }

  @Override
  public Iterable<StoreEvent> getEventsFrom(String[] streams, String position) {
    var events = Collections2.filter(this.events,
        event -> includes(streams, event.stream) && event.position.compareTo(position) > 0);
    return events;
    // var l = events.stream().toArray(StoreEvent[]::new);
    // return Arrays.asList(l);
    // return events;
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
  public String getPosition(String bookmark) {
    var bm = this.bookmarks.getOrDefault(bookmark, "");
    return bm;
  }

  @Override
  public void setPosition(String bookmark, String position) {
    this.bookmarks.put(bookmark, position);
  }

  private boolean includes(String[] list, String value) {
    for (int i = 0; i < list.length; i++) {
      if (list[i].equals(value)) {
        return true;
      }
    }
    return false;
  }

}
