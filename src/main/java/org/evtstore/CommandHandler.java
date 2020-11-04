package org.evtstore;

import java.util.function.BiFunction;

public interface CommandHandler<C extends Command, A extends Aggregate> extends BiFunction<C, A, Payload> {

}
