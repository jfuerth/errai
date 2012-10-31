package org.jboss.errai.common.client.api.event;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public final class Registry {

  private static final Multimap<Class<?>, ErraiListener<?>> listeners = HashMultimap.create();

  public static <T> void addListener(Class<T> eventType, ErraiListener<T> listener) {
    listeners.put(eventType, listener);
  }

  public static <T> boolean removeListener(Class<T> eventType, ErraiListener<T> listener) {
    return listeners.remove(eventType, listener);
  }
}
