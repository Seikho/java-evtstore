/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package org.evtstore;

import org.evtstore.domain.ex.DomainExample;
import org.evtstore.domain.ex.ExampleAgg;
import org.evtstore.provider.Neo4jProvider;
import org.junit.BeforeClass;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;

public class Neo4jLimitTest extends ProviderTester {
  private static Provider provider;
  private static DomainExample two;
  private static DomainExample one;

  @Override
  public String getName() {
    return "Neo4jLimitTest";
  }

  @BeforeClass
  public static void before() {
    var driver = GraphDatabase.driver("bolt://localhost:30010", AuthTokens.basic("neo4j", "admin"));
    try (var session = driver.session()) {
      session.run("MATCH (n: JLimitEvents) DETACH DELETE n");
      session.run("MATCH (n: JLimitBookmarks) DETACH DELETE n");
    }
    var provider = new Neo4jProvider(driver, "JLimitEvents", "JLimitBookmarks");
    provider.setLimit(1);
    provider.migrate();
    Neo4jLimitTest.provider = provider;
    Neo4jLimitTest.one = new DomainExample(provider, "test-1");
    Neo4jLimitTest.two = new DomainExample(provider, "test-2");
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
