package org.evtstore.domain.ex;

import com.eclipsesource.json.Json;

import org.evtstore.CommandHandler;
import org.evtstore.Domain;
import org.evtstore.Payload;
import org.evtstore.Provider;
import org.evtstore.domain.ex.cmd.Cmd;
import org.evtstore.domain.ex.cmd.DoOne;
import org.evtstore.provider.MemoryProvider;

public class DomainExample extends Domain<ExampleAgg> {
  private CommandHandler<DoOne, ExampleAgg> doOne = (cmd, agg) -> {
    var event = Json.object();
    event.add("one", cmd.one);
    var payload = new Payload("EvOne", event);
    return payload;
  };

  public DomainExample(Provider provider, String stream) {
    super(stream, provider, new ExampleFold());
    registerHandlers();
  }

  public DomainExample(String stream) {
    super(stream, new MemoryProvider(), new ExampleFold());
    registerHandlers();
  }

  private void registerHandlers() {
    this.register(Cmd.DoOne, this.doOne);
  }
}
