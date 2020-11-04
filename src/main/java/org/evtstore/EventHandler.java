package org.evtstore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class EventHandler<Agg extends Aggregate> {
  private boolean running = false;
  private Map<String, Consumer<Event>> handlers = new HashMap<String, Consumer<Event>>();
  private Provider<Agg> provider;
  private String position = "";
  private String stream = "";
  private String bookmark;

  public EventHandler(Provider<Agg> provider, String stream, String bookmark) {
    this.provider = provider;
    this.stream = stream;
    this.bookmark = bookmark;
    var bm = provider.getPosition(bookmark);
    this.position = bm;
    this.process();
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
    var events = this.provider.getEventsFrom(stream, position);
    var iterator = events.iterator();
    int size = 0;
    while (iterator.hasNext()) {
      size++;
      var storeEvent = iterator.next();
      var event = new Event(storeEvent);
      var type = event.payload.get("type").asString();
      var handler = handlers.get(type);

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
      return 0;
    }

    return size;
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
    var casted = (Consumer<Event>) handler;
    this.handlers.put(type, casted);
  }
}
