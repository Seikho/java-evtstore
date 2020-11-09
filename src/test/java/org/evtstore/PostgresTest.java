package org.evtstore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.function.Supplier;

import org.evtstore.domain.ex.DomainExample;
import org.evtstore.domain.ex.ExampleAgg;
import org.evtstore.provider.PostgresProvider;
import org.junit.BeforeClass;

public class PostgresTest extends ProviderTester {
  private static Provider provider;
  private static DomainExample two;
  private static DomainExample one;

  @Override
  public String getName() {
    return "PostgresTest";
  }

  @BeforeClass
  public static void before() {

    try {
      Class.forName("org.postgresql.Driver");
    } catch (ClassNotFoundException ex) {
      ex.printStackTrace();
    }

    try (var conn = DriverManager.getConnection("jdbc:postgresql://localhost:30002/postgres", "admin", "admin")) {
      conn.createStatement().execute("DROP DATABASE IF EXISTS postgres_test");
      conn.createStatement().execute("CREATE DATABASE postgres_test");
    } catch (SQLException ex) {
      ex.printStackTrace();
    }

    Supplier<Connection> connection = () -> {
      try {
        var conn = DriverManager.getConnection("jdbc:postgresql://localhost:30002/postgres_test", "admin", "admin");
        return conn;
      } catch (SQLException ex) {
        ex.printStackTrace();
        return null;
      }
    };

    var provider = new PostgresProvider(connection, "jevents", "jbookmarks");
    provider.migrate();
    PostgresTest.provider = provider;
    PostgresTest.one = new DomainExample(provider, "test-1");
    PostgresTest.two = new DomainExample(provider, "test-2");
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
