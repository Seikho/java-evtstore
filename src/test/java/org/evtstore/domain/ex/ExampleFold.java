package org.evtstore.domain.ex;

import java.util.function.BiFunction;

import org.evtstore.Event;
import org.evtstore.Folder;

public class ExampleFold extends Folder<ExampleAgg> {
  private BiFunction<Event, ExampleAgg, ExampleAgg> EvOne = (event, agg) -> {
    var one = event.payload.get("one").asInt();
    agg.one += one;
    return agg;
  };

  private BiFunction<Event, ExampleAgg, ExampleAgg> EvTwo = (event, agg) -> {
    var two = event.payload.get("two").asInt();
    agg.two += two;
    return agg;
  };

  public ExampleFold() {
    super(() -> new ExampleAgg());
    this.register("EvOne", this.EvOne);
    this.register("EvTwo", this.EvTwo);
  }
}
