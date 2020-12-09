package org.evtstore;

import java.util.HashMap;
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
    var nextAgg = this.aggregate.get();
    for (var i = events.iterator(); i.hasNext();) {
      var storeEvent = i.next();
      nextAgg = this.fold(storeEvent, nextAgg);
    }
    return nextAgg;
  }

  public Agg fold(StoreEvent storeEvent, Agg agg) {
    var event = new Event(storeEvent);
    var type = event.payload.get("type").asString();
    var handler = this.handlers.get(type);

    if (handler == null) {
      agg.version = event.version;
      return agg;
    }

    var nextAgg = handler.apply(event, agg);
    nextAgg.version = event.version;
    return nextAgg;
  }
}
