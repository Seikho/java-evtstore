package org.evtstore;

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

  public <C extends Command, P extends Payload> void register(String type, CommandHandler<C, Agg> handler) {
    var casted = (CommandHandler<Command, Agg>) handler;
    this.commands.put(type, casted);
  }

  public <Cmd extends Command> Agg execute(String aggregateId, Cmd cmd) {
    var agg = getAggregate(aggregateId);

    // Should we throw here?
    // This may be unexpected
    if (!commands.containsKey(cmd.type)) {
      return agg;
    }

    var handler = this.commands.get(cmd.type);

    // The handler did not return an event, no need to append
    var payload = handler.apply(cmd, agg);
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
    var events = this.provider.getEventsFor(stream, aggregateId, "");
    var nextAgg = this.folder.fold(events);
    nextAgg.aggregateId = aggregateId;
    return nextAgg;
  }

  public EventHandler createHandler(String stream, String bookmark) {
    return new EventHandler(provider, stream, bookmark);
  }

}
