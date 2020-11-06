package org.evtstore;

import java.util.function.BiFunction;

public class CommandHandler<C extends Command, A extends Aggregate> {
  public Class<C> command;
  public BiFunction<C, A, Payload> handler;
  public String type;

  public CommandHandler(Class<C> command, BiFunction<C, A, Payload> handler) {
    this.command = command;
    this.handler = handler;
    this.type = command.getSimpleName();
  }
}
