package org.evtstore;

import static org.junit.Assert.assertEquals;

import org.evtstore.domain.ex.ExampleAgg;
import org.evtstore.domain.ex.cmd.DoOne;
import org.junit.Test;

public abstract class ProviderTester {
  public abstract Domain<ExampleAgg> getDomain();

  @Test
  public void appendEvent() {
    var actual = getDomain().execute("one", new DoOne(42));
    assertEquals((Integer) 1, actual.version);
    assertEquals((Integer) 42, actual.one);
  }

  @Test
  public void appendAnother() {
    var agg = getDomain().getAggregate("one");
    assertEquals((Integer) 1, agg.version);
    getDomain().execute("one", new DoOne(42));
    var actual = getDomain().getAggregate("one");
    assertEquals((Integer) 2, actual.version);
    assertEquals((Integer) 84, actual.one);
  }

  @Test
  public void appendToNewAggregate() {
    var agg = getDomain().getAggregate("two");
    assertEquals((Integer) 0, agg.version);
    getDomain().execute("two", new DoOne(10));
    var actual = getDomain().getAggregate("two");
    assertEquals((Integer) 1, actual.version);
    assertEquals((Integer) 10, actual.one);
  }

  @Test
  public void handlerProcess() {
    var id = "three";
    getDomain().execute(id, new DoOne(8));
    getDomain().execute(id, new DoOne(16));
    var actual = new Field<Integer>(0);
    Integer expected = 24;
    var model = getDomain().createHandler("test1", "bm1");
    model.handle("EvOne", ev -> {
      if (!ev.aggregateId.equals(id)) {
        return;
      }

      var one = ev.payload.get("one").asInt();
      actual.set(actual.get() + one);
    });
    model.runOnce();
    assertEquals(expected, actual.get());
  }
}
