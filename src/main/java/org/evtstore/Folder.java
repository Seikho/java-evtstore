package org.evtstore;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class Folder<Agg extends Aggregate> {
  public Supplier<Agg> aggregate;
  private Map<String, BiFunction<Event, Agg, Agg>> handlers = new HashMap<String, BiFunction<Event, Agg, Agg>>();

  public Folder(Supplier<Agg> aggregate) {
    this.aggregate = aggregate;
  }

  public Folder<Agg> register(String type, BiFunction<Event, Agg, Agg> handler) {
    this.handlers.put(type, handler);
    return this;
  }

  public Agg fold(Iterable<StoreEvent> events) {
    Agg nextAgg = this.aggregate.get();
    for (Iterator<StoreEvent> i = events.iterator(); i.hasNext();) {
      StoreEvent storeEvent = i.next();
      nextAgg = this.fold(storeEvent, nextAgg);
    }
    return nextAgg;
  }

  public Agg fold(StoreEvent storeEvent, Agg agg) {
    Event event = new Event(storeEvent);
    BiFunction<Event, Agg, Agg> handler = this.handlers.get(event.payload.get("type").asString());
    Agg nextAgg = handler.apply(event, agg);
    nextAgg.version = event.version;
    return nextAgg;
  }
}
