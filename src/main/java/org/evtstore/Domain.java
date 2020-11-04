package org.evtstore;

import java.util.HashMap;
import java.util.Map;

public class Domain<Agg extends Aggregate> {
  private String stream;
  private Provider<Agg> provider;
  private Folder<Agg> folder;
  private Map<String, CommandHandler<Command, Agg>> commands = new HashMap<String, CommandHandler<Command, Agg>>();

  public Domain(String stream, Provider<Agg> provider, Folder<Agg> folder) {
    this.stream = stream;
    this.provider = provider;
    this.folder = folder;
  }

  public <C extends Command, P extends Payload> void register(String type, CommandHandler<C, Agg> handler) {
    var casted = (CommandHandler<Command, Agg>) handler;
    this.commands.put(type, casted);
    System.out.println(String.format("Registered %s", type));
  }

  public <Cmd extends Command> Agg execute(String aggregateId, Cmd cmd) {
    if (!commands.containsKey(cmd.type)) {
      System.out.println(String.format("No cmd handler for %s", cmd.type));
      // Throw?
    }

    var handler = this.commands.get(cmd.type);
    var agg = getAggregate(aggregateId);

    var payload = handler.apply(cmd, agg);
    if (payload == null) {
      System.out.println(cmd.type);
      System.out.println("No still payload returned");
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
    return nextAgg;
  }

}
