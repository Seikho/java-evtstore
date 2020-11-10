# java-evtstore

> A Java compatible implementation of [evtstore](https://github.com/seikho/evtstore)
> Opinionated CQRS and Event Sourcing for Java

## Why

I reguarly use event sourcing and wanted to lower the barrier for entry and increase productivity for colleagues.

The motivation for creating implementations in different run-times is to provide familiarity, consistency, and predictability.  
Database providers are intended to be compatible across all of the run-times supported by `evtstore`.  
An event log driven by Java can be consumed by the equivalent database provider in Node.js.

The design goals are:

- To allow interopability with `evtstore` implementations written in other languages (i.e., [Node.js](https://github.com/seikho/evtstore))
- Make creating domains quick and intuitive
- Be easy to test
- Allow developers to focus on application/business problems instead of Event Sourcing and CQRS problems

To obtain these goals the design is highly opinionated, but still flexible.

## Tests

- Use `docker-compose up -d` to ensure the databases are available
- `./gradlew clean test`

## Example

An example domain is implemented in the [tests](https://gitlab.mypassglobal.com/cwinkler/java-evtstore/-/tree/master/src/test/java/org/evtstore/domain/ex) folder

```java
MyAggregateFolder folder = new MyAggregateFolder(); // Extends Folder
Neo4jProvider provider = new Neo4jProvider(db, "Events", "Bookmarks");
Domain<MyAggregate> domain = new Domain<MyAggregate>("my-event-stream", myDbProvider, myAggregateFolder);
```

## Database Providers

### Custom Providers

Custom providers must implement the `Provider` interface.  
You can use the `ProviderTester` test suite to ensure your provider is compatible.

### Neo4j

> Depends on neo4j-java-driver

You can provide your own label names for Events and Bookmarks in the constructor

```java
Neo4jProvider provider = new Neo4jProvider(dbDriver, "MyEventsLabel", "MyBookmarksLabel");

// Creates indexes and constraints to ensure event log validity
provier.migrate();
```

#### Event Nodes

```
(ev: Events {
  stream: String,
  position: DateTime,
  version: Integer,
  aggregateId: String,
  event: String, // JSON string of the event payload
  timestamp: DateTime,
  _streamPosition: String, // Used internally for unique constraints
  _streamIdVersion: String. // Used internally for unique constraints
})
```

#### Bookmark Nodes

```
(bm: Bookmarks {
  bookmark: String,
  position: DateTime
})
```
