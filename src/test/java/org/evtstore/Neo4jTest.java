package org.evtstore;

import static org.junit.Assert.assertEquals;

import org.evtstore.domain.ex.DomainExample;
import org.evtstore.domain.ex.ExampleAgg;
import org.evtstore.domain.ex.cmd.DoOne;
import org.evtstore.provider.Neo4jProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;

public class Neo4jTest {
  public static DomainExample domain;

  @BeforeClass
  public static void before() {
    var driver = GraphDatabase.driver("bolt://localhost:30010", AuthTokens.basic("neo4j", "admin"));
    var session = driver.session();
    session.run("MATCH (n: JEvents) DETACH DELETE n");
    session.run("MATCH (n: JBookmarks) DETACH DELETE n");
    var provider = new Neo4jProvider<ExampleAgg>(session, "JEvents", "JBookmarks");
    provider.migrate();
    Neo4jTest.domain = new DomainExample(provider, "test1");
  }

  @Test
  public void appendEvent() {
    var actual = domain.execute("one", new DoOne(42));
    assertEquals((Integer) 1, actual.version);
    assertEquals((Integer) 42, actual.one);
  }

  @Test
  public void appendAnother() {
    var agg = domain.getAggregate("one");
    assertEquals((Integer) 1, agg.version);
    domain.execute("one", new DoOne(42));
    var actual = domain.getAggregate("one");
    assertEquals((Integer) 2, actual.version);
    assertEquals((Integer) 84, actual.one);
  }

  @Test
  public void appendToNewAggregate() {
    var agg = domain.getAggregate("two");
    assertEquals((Integer) 0, agg.version);
    domain.execute("two", new DoOne(10));
    var actual = domain.getAggregate("two");
    assertEquals((Integer) 1, actual.version);
    assertEquals((Integer) 10, actual.one);
  }

  @Test
  public void handlerProcess() {
    var id = "three";
    domain.execute(id, new DoOne(8));
    domain.execute(id, new DoOne(16));
    var actual = new Field<Integer>(0);
    Integer expected = 24;
    var model = domain.createHandler("test1", "bm1");
    model.handle("EvOne", ev -> {
      if (ev.aggregateId.equals(id) == false) {
        return;
      }

      var one = ev.payload.get("one").asInt();
      actual.set(actual.get() + one);
    });
    model.runOnce();
    assertEquals(expected, actual.get());
  }
}
