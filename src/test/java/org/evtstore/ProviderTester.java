package org.evtstore;

import static org.junit.Assert.assertEquals;

import org.evtstore.domain.ex.ExampleAgg;
import org.evtstore.domain.ex.cmd.DoOne;
import org.junit.Test;

public abstract class ProviderTester {
  public abstract Domain<ExampleAgg> getOne();

  public abstract Domain<ExampleAgg> getTwo();

  public abstract Provider getProvider();

  @Test
  public void appendEvent() {
    var actual = getOne().execute("one", new DoOne(42));
    assertEquals((Integer) 1, actual.version);
    assertEquals((Integer) 42, actual.one);
  }

  @Test
  public void appendAnother() {
    var agg = getOne().getAggregate("one");
    assertEquals((Integer) 1, agg.version);
    getOne().execute("one", new DoOne(42));
    var actual = getOne().getAggregate("one");
    assertEquals((Integer) 2, actual.version);
    assertEquals((Integer) 84, actual.one);
  }

  @Test
  public void appendToNewAggregate() {
    var agg = getOne().getAggregate("two");
    assertEquals((Integer) 0, agg.version);
    getOne().execute("two", new DoOne(10));
    var actual = getOne().getAggregate("two");
    assertEquals((Integer) 1, actual.version);
    assertEquals((Integer) 10, actual.one);
  }

  @Test
  public void handlerProcess() {
    var id = "three";
    getOne().execute(id, new DoOne(8));
    getOne().execute(id, new DoOne(16));
    var actual = new Field<Integer>(0);
    Integer expected = 24;
    var model = getOne().createHandler("test-1", "bm1");
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

  @Test
  public void emptyAggregate() {
    var actual = getOne().getAggregate("non-exist");
    assertEquals((Integer) 0, actual.version);
  }

  @Test
  public void commandResult() {
    var actual = getOne().execute("cmdresult", new DoOne(20));
    assertEquals((Integer) 20, actual.one);
  }

  @Test
  public void writeToDifferentStreams() {
    getOne().execute("multi-11", new DoOne(1));
    getOne().execute("multi-11", new DoOne(2));
    getTwo().execute("multi-11", new DoOne(4));

    var one = getOne().getAggregate("multi-11");
    var two = getTwo().getAggregate("multi-11");

    assertEquals((Integer) 3, one.one);
    assertEquals((Integer) 4, two.one);
  }

  @Test
  public void modelFromMultipleStreams() {
    getOne().execute("multi-21", new DoOne(1));
    getOne().execute("multi-21", new DoOne(2));
    getTwo().execute("multi-22", new DoOne(4));

    var prv = getProvider();
    var streams = new String[] { "test-1", "test-2" };
    var model = new EventHandler(prv, streams, "multimodel");

    var one = new Field<Integer>(0);
    var two = new Field<Integer>(0);

    model.handle("EvOne", ev -> {
      var val = ev.payload.get("one").asInt();
      var isAgg = ev.aggregateId.equals("multi-21") || ev.aggregateId.equals("multi-22");

      if (!isAgg) {
        return;
      }

      if (ev.stream.equals("test-1")) {
        one.set(one.get() + val);
      }

      if (ev.stream.equals("test-2")) {
        two.set(two.get() + val);
      }
    });

    model.runOnce();

    assertEquals((Integer) 3, one.get());
    assertEquals((Integer) 4, two.get());
  }
}
