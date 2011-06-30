package com.dreamfabric.jac64;

public class EventQueue {

  private TimeEvent first;
  public long nextTime;

  public EventQueue() {
  }

  public void addEvent(TimeEvent event, long time) {
    event.time = time;
    addEvent(event);
  }

  public void addEvent(TimeEvent event) {
    if (event.scheduled) {
      removeEvent(event);
    }
    if (first == null) {
      first = event;
    } else {
      TimeEvent pos = first;
      TimeEvent lastPos = first;
      while (pos != null && pos.time < event.time) {
        lastPos = pos;
        pos = pos.nextEvent;
      }
      // Here pos will be the first TE after event
      // and lastPos the first before
      if (pos == first) {
        // Before all other
        event.nextEvent = pos;
        first = event;
      } else {
        event.nextEvent = pos;
        lastPos.nextEvent = event;
      }
    }
    if (first != null) {
      nextTime = first.time;
    } else {
      nextTime = 0;
    }
    event.scheduled = true;
  }

  // Not yet impl.
  public boolean removeEvent(TimeEvent event) {
    TimeEvent pos = first;
    TimeEvent lastPos = first;
//    System.out.println("Removing: " + event.getShort() + "  Before remove: ");
//    print();
    while (pos != null && pos != event) {
      lastPos = pos;
      pos = pos.nextEvent;
    }
    if (pos == null) return false;
    // pos == event!
    if (pos == first) {
      // remove it from first pos.
      first = pos.nextEvent;
    } else {
      // else link prev to next...
      lastPos.nextEvent = pos.nextEvent;
    }
    // unlink
    pos.nextEvent = null;

    if (first != null) {
      nextTime = first.time;
    } else {
      nextTime = 0;
    }
//    System.out.println("Removed =>");
//    print();
    event.scheduled = false;
    return true;
  }

  public TimeEvent popFirst() {
    TimeEvent tmp = first;
    if (tmp != null) {
      first = tmp.nextEvent;
      // Unlink.
      tmp.nextEvent = null;
    }

    if (first != null) {
      nextTime = first.time;
    } else {
      nextTime = 0;
    }
    tmp.scheduled = false;
    return tmp;
  }

  public void print() {
    TimeEvent t = first;
    System.out.print("nxt: " + nextTime + " [");
    while(t != null) {
      System.out.print(t.getShort());
      t = t.nextEvent;
      if (t != null) System.out.print(", ");
    }
    System.out.println("]");
  }

  public void empty() {
	  first = null;
	  nextTime = 0;
  }
} // LLEventQueue
