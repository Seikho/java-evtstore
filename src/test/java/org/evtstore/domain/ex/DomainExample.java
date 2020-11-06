package org.evtstore.domain.ex;

import org.evtstore.CommandHandler;
import org.evtstore.Domain;
import org.evtstore.Provider;
import org.evtstore.domain.ex.cmd.DoOne;
import org.evtstore.domain.ex.cmd.DoTwo;
import org.evtstore.domain.ex.cmd.Events;
import org.evtstore.provider.MemoryProvider;

public class DomainExample extends Domain<ExampleAgg> {
  private CommandHandler<DoOne, ExampleAgg> doOne = new CommandHandler<DoOne, ExampleAgg>(DoOne.class, (cmd, agg) -> {
    var payload = Events.evOne(cmd.one);
    return payload;
  });

  private CommandHandler<DoTwo, ExampleAgg> doTwo = new CommandHandler<DoTwo, ExampleAgg>(DoTwo.class, (cmd, agg) -> {
    var payload = Events.evTwo(cmd.two);
    return payload;
  });

  public DomainExample(Provider provider, String stream) {
    super(stream, provider, new ExampleFold());
    registerHandlers();
  }

  public DomainExample(String stream) {
    super(stream, new MemoryProvider(), new ExampleFold());
    registerHandlers();
  }

  private void registerHandlers() {
    this.register(this.doOne);
    this.register(this.doTwo);
  }

}
