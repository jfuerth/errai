package org.jboss.errai.common.client.api.event;

public interface ErraiListener<T> {
  public void onEvent(T e);
}
