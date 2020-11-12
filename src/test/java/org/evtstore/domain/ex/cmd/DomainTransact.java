package org.evtstore.domain.ex.cmd;

import org.evtstore.CommandHandler;
import org.evtstore.Domain;
import org.evtstore.domain.ex.ExampleAgg;
import org.evtstore.domain.ex.ExampleFold;
import org.evtstore.provider.TransactNeo4jProvider;

public class DomainTransact extends Domain<ExampleAgg> {
  private TransactNeo4jProvider prv;

  private CommandHandler<DoOne, ExampleAgg> doOne = new CommandHandler<>(DoOne.class, (cmd, agg) -> {
    var payload = Events.evOne(cmd.one);
    return payload;
  });

  private CommandHandler<DoTwo, ExampleAgg> doTwo = new CommandHandler<>(DoTwo.class, (cmd, agg) -> {
    var payload = Events.evTwo(cmd.two);
    return payload;
  });

  public DomainTransact(TransactNeo4jProvider provider, String stream) {
    super(stream, provider, new ExampleFold());
    this.prv = provider;
    registerHandlers();
  }

  @Override
  public <Cmd> ExampleAgg execute(String aggregateId, Cmd cmd) {
    try (var sess = prv.getSession()) {
      var trx = sess.beginTransaction();
      prv.transact(trx);
      var result = super.execute(aggregateId, cmd);
      trx.commit();
      return result;
    }
  }

  private void registerHandlers() {
    this.register(this.doOne);
    this.register(this.doTwo);
  }
}
