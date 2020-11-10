package org.evtstore;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class EventHandler {
  private boolean running = false;
  private Map<String, Consumer<Event>> handlers = new HashMap<String, Consumer<Event>>();
  private Provider provider;
  private String position = "";
  private String bookmark;
  private String[] streams;

  public EventHandler(Provider provider, String stream, String bookmark) {
    this.provider = provider;
    this.bookmark = bookmark;
    String bm = provider.getPosition(bookmark);
    this.position = bm;
    this.process();
    this.streams = new String[] { stream };
  }

  public EventHandler(Provider provider, String[] streams, String bookmark) {
    this.provider = provider;
    this.bookmark = bookmark;
    String bm = provider.getPosition(bookmark);
    this.position = bm;
    this.process();
    this.streams = streams;
  }

  public void start() {
    this.running = true;
  }

  public void stop() {
    this.running = false;
  }

  public void process() {
    if (this.running) {
      int handled = 0;
      do {
        handled = this.runOnce();
      } while (handled == 0);
    }

    this.pause();
  }

  public Integer runOnce() {
    return runOnce(0);
  }

  public Integer runOnce(Integer prevCount) {
    Iterable<StoreEvent> events = this.provider.getEventsFrom(streams, position);

    Iterator<StoreEvent> iterator = events.iterator();
    int size = 0;
    while (iterator.hasNext()) {
      size++;
      StoreEvent storeEvent = iterator.next();
      Event event = new Event(storeEvent);
      String type = event.payload.get("type").asString();
      Consumer<Event> handler = handlers.get(type);

      if (handler == null) {
        provider.setPosition(this.bookmark, event.position);
        this.position = event.position;
        continue;
      }

      handler.accept(event);
      provider.setPosition(this.bookmark, event.position);
      this.position = event.position;
      continue;
    }

    if (size == 0) {
      return prevCount;
    }

    if (size > 0) {
      return runOnce(size + prevCount);
    }

    return size + prevCount;
  }

  public void pause() {
    ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
    exec.schedule(new Runnable() {
      public void run() {
        process();
      }
    }, 1, TimeUnit.SECONDS);
  }

  public <E extends Event> void handle(String type, Consumer<E> handler) {
    Consumer<Event> casted = (Consumer<Event>) handler;
    this.handlers.put(type, casted);
  }
}
