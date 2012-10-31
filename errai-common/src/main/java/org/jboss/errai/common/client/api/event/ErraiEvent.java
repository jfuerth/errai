package org.jboss.errai.common.client.api.event;

import org.jboss.errai.common.client.api.Assert;

public class ErraiEvent<T> {

  private final T event;

  public ErraiEvent(T event) {
    this.event = Assert.notNull(event);
  }

  public T get() {
    return event;
  }
}
