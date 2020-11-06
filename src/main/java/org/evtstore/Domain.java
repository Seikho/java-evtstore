package org.evtstore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Domain<Agg extends Aggregate> {
  private String stream;
  private Provider provider;
  private Folder<Agg> folder;
  private Map<String, CommandHandler<Command, Agg>> commands = new HashMap<String, CommandHandler<Command, Agg>>();

  public Domain(String stream, Provider provider, Folder<Agg> folder) {
    this.stream = stream;
    this.provider = provider;
    this.folder = folder;
  }

  public <Cmd extends Command> void register(CommandHandler<Cmd, Agg> handler) {
    var casted = (CommandHandler<Command, Agg>) handler;
    this.commands.put(handler.type, casted);
  }

  public <Cmd extends Command> Agg execute(String aggregateId, Cmd cmd) {
    var agg = getAggregate(aggregateId);
    var type = cmd.getClass().getSimpleName();
    // Should we throw here?
    // This may be unexpected
    if (!commands.containsKey(type)) {
      return agg;
    }

    var command = this.commands.get(type);

    // The handler did not return an event, no need to append
    var payload = command.handler.apply(cmd, agg);
    if (payload == null) {
      return agg;
    }

    var storeEvent = new StoreEvent(payload);
    storeEvent.version = agg.version + 1;
    storeEvent.stream = this.stream;
    storeEvent.aggregateId = aggregateId;
    provider.append(storeEvent, agg);
    var nextAgg = folder.fold(storeEvent, agg);
    return nextAgg;
  }

  public Agg getAggregate(String aggregateId) {
    var events = getAllEventsFor(aggregateId);
    var nextAgg = this.folder.fold(events);
    nextAgg.aggregateId = aggregateId;
    return nextAgg;
  }

  public EventHandler createHandler(String stream, String bookmark) {
    return new EventHandler(provider, stream, bookmark);
  }

  private Iterable<StoreEvent> getAllEventsFor(String id) {
    var allEvents = new ArrayList<StoreEvent>();
    var limit = provider.getLimit();
    var lastPos = "";

    while (true) {
      var events = provider.getEventsFor(stream, id, lastPos);
      if (limit == 0) {
        return events;
      }

      var length = 0;
      var iterator = events.iterator();
      while (iterator.hasNext()) {
        length++;
        var event = iterator.next();
        lastPos = event.position;
        allEvents.add(event);
      }

      if (length < limit || length == 0) {
        return allEvents;
      }
    }
  }
}
