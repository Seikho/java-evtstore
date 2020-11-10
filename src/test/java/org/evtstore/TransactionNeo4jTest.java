package org.evtstore;

import org.evtstore.domain.ex.DomainExample;
import org.evtstore.domain.ex.ExampleAgg;
import org.evtstore.provider.TransactNeo4jProvider;
import org.junit.BeforeClass;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;

public class TransactionNeo4jTest extends ProviderTester {
  private static Provider provider;
  private static DomainExample two;
  private static DomainExample one;

  @Override
  public String getName() {
    return "Neo4jTest";
  }

  @BeforeClass
  public static void before() {
    Driver driver = GraphDatabase.driver("bolt://localhost:30003", AuthTokens.basic("neo4j", "admin"));
    try (Session session = driver.session()) {
      session.run("MATCH (n: JEvents) DETACH DELETE n");
      session.run("MATCH (n: JBookmarks) DETACH DELETE n");
    }
    TransactNeo4jProvider provider = new TransactNeo4jProvider(driver, "JEvents", "JBookmarks");
    provider.migrate();
    TransactionNeo4jTest.provider = provider;
    TransactionNeo4jTest.one = new DomainExample(provider, "test-1");
    TransactionNeo4jTest.two = new DomainExample(provider, "test-2");
  }

  @Override
  public Provider getProvider() {
    return provider;
  }

  @Override
  public Domain<ExampleAgg> getOne() {
    return one;
  }

  @Override
  public Domain<ExampleAgg> getTwo() {
    return two;
  }

}
