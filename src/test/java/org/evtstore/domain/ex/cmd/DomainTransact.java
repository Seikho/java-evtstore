package org.evtstore.domain.ex.cmd;

import org.evtstore.Command;
import org.evtstore.CommandHandler;
import org.evtstore.Domain;
import org.evtstore.Payload;
import org.evtstore.domain.ex.ExampleAgg;
import org.evtstore.domain.ex.ExampleFold;
import org.evtstore.provider.TransactNeo4jProvider;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;

public class DomainTransact extends Domain<ExampleAgg> {
  private TransactNeo4jProvider prv;

  private CommandHandler<DoOne, ExampleAgg> doOne = new CommandHandler<DoOne, ExampleAgg>(DoOne.class, (cmd, agg) -> {
    Payload payload = Events.evOne(cmd.one);
    return payload;
  });

  private CommandHandler<DoTwo, ExampleAgg> doTwo = new CommandHandler<DoTwo, ExampleAgg>(DoTwo.class, (cmd, agg) -> {
    Payload payload = Events.evTwo(cmd.two);
    return payload;
  });

  public DomainTransact(TransactNeo4jProvider provider, String stream) {
    super(stream, provider, new ExampleFold());
    this.prv = provider;
    registerHandlers();
  }

  @Override
  public <Cmd extends Command> ExampleAgg execute(String aggregateId, Cmd cmd) {
    try (Session sess = prv.getSession()) {
      Transaction trx = sess.beginTransaction();
      prv.transact(trx);
      ExampleAgg result = super.execute(aggregateId, cmd);
      trx.commit();
      return result;
    }
  }

  private void registerHandlers() {
    this.register(this.doOne);
    this.register(this.doTwo);
  }
}
