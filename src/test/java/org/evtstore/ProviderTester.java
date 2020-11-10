package org.evtstore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Iterator;

import org.evtstore.domain.ex.ExampleAgg;
import org.evtstore.domain.ex.cmd.Commands;
import org.junit.Test;

public abstract class ProviderTester {
  public abstract Domain<ExampleAgg> getOne();

  public abstract Domain<ExampleAgg> getTwo();

  public abstract Provider getProvider();

  public abstract String getName();

  @Test
  public void appendEvent() {
    ExampleAgg actual = getOne().execute("one", Commands.doOne(42));
    assertEquals((Integer) 1, actual.version);
    assertEquals((Integer) 42, actual.one);
  }

  @Test
  public void appendAnother() {
    String id = "appendAnother";
    ExampleAgg first = getOne().execute(id, Commands.doOne(1));
    ExampleAgg second = getOne().execute(id, Commands.doTwo(2));
    ExampleAgg actual = getOne().getAggregate(id);
    assertEquals((Integer) 1, first.version);
    assertEquals((Integer) 2, second.version);
    assertEquals((Integer) 2, actual.version);
    assertEquals((Integer) 1, actual.one);
    assertEquals((Integer) 2, actual.two);
  }

  @Test
  public void appendToNewAggregate() {
    String id = "appendToNewAggregate";
    ExampleAgg agg = getOne().getAggregate(id);
    assertEquals((Integer) 0, agg.version);
    getOne().execute(id, Commands.doOne(10));
    ExampleAgg actual = getOne().getAggregate(id);
    assertEquals((Integer) 1, actual.version);
    assertEquals((Integer) 10, actual.one);
  }

  @Test
  public void handlerProcess() {
    String id = "three";
    getOne().execute(id, Commands.doOne(8));
    getOne().execute(id, Commands.doOne(16));
    Field<Integer> actual = new Field<Integer>(0);
    Integer expected = 24;
    EventHandler model = getOne().createHandler("test-1", "bm1");
    model.handle("EvOne", ev -> {
      if (!ev.aggregateId.equals(id)) {
        return;
      }

      int one = ev.payload.get("one").asInt();
      actual.set(actual.get() + one);
    });
    model.runOnce();
    assertEquals(expected, actual.get());
  }

  @Test
  public void emptyAggregate() {
    ExampleAgg actual = getOne().getAggregate("non-exist");
    assertEquals((Integer) 0, actual.version);
  }

  @Test
  public void commandResult() {
    ExampleAgg actual = getOne().execute("cmdresult", Commands.doOne(20));
    assertEquals((Integer) 20, actual.one);
  }

  @Test
  public void writeToDifferentStreams() {
    getOne().execute("multi-11", Commands.doOne(1));
    getOne().execute("multi-11", Commands.doOne(2));
    getTwo().execute("multi-11", Commands.doOne(4));

    ExampleAgg one = getOne().getAggregate("multi-11");
    ExampleAgg two = getTwo().getAggregate("multi-11");

    assertEquals((Integer) 3, one.one);
    assertEquals((Integer) 4, two.one);
  }

  @Test
  public void modelFromMultipleStreams() {
    getOne().execute("multi-21", Commands.doOne(1));
    getOne().execute("multi-21", Commands.doOne(2));
    getTwo().execute("multi-22", Commands.doOne(4));

    Provider prv = getProvider();
    String[] streams = new String[] { "test-1", "test-2" };
    EventHandler model = new EventHandler(prv, streams, "multimodel");

    Field<Integer> one = new Field<Integer>(0);
    Field<Integer> two = new Field<Integer>(0);

    model.handle("EvOne", ev -> {
      int val = ev.payload.get("one").asInt();
      boolean isAgg = ev.aggregateId.equals("multi-21") || ev.aggregateId.equals("multi-22");

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

  @Test
  public void foldDifferentEvents() {
    getOne().execute("diff-1", Commands.doOne(1));
    getOne().execute("diff-1", Commands.doTwo(2));

    ExampleAgg actual = getOne().getAggregate("diff-1");
    assertEquals((Integer) 1, actual.one);
    assertEquals((Integer) 2, actual.two);
  }

  @Test
  public void throwOnConflict() {
    getOne().execute("conflict-1", Commands.doOne(1));
    Iterable<StoreEvent> events = getProvider().getEventsFor("test-1", "conflict-1", "");
    Iterator<StoreEvent> iter = events.iterator();
    StoreEvent event = iter.next();
    ExampleAgg agg = new ExampleAgg();
    agg.aggregateId = "conflict-1";

    assertThrows(VersionConflictException.class, () -> {
      getProvider().append(event, agg);
    });
  }

}
