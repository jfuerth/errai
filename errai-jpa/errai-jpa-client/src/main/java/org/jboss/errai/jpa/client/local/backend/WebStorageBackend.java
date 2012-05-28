package org.jboss.errai.jpa.client.local.backend;

import java.util.HashSet;
import java.util.Set;

import org.jboss.errai.common.client.framework.Assert;
import org.jboss.errai.jpa.client.local.ErraiEntityManager;
import org.jboss.errai.jpa.client.local.Key;
import org.jboss.errai.marshalling.client.Marshalling;
import org.jboss.errai.marshalling.client.MarshallingSessionProviderFactory;
import org.jboss.errai.marshalling.client.api.MappingContext;
import org.jboss.errai.marshalling.client.api.Marshaller;
import org.jboss.errai.marshalling.client.api.MarshallingSession;
import org.jboss.errai.marshalling.client.api.json.EJValue;
import org.jboss.errai.marshalling.client.api.json.impl.gwt.GWTJSONValue;

import com.google.common.base.Strings;
import com.google.gwt.json.client.JSONParser;

/**
 * The storage backend for HTML WebStorage, a storage facility supported by most
 * browsers for at least 2.5 million characters of data, (5 megabytes of unicode text).
 *
 * @author Jonathan Fuerth <jfuerth@gmail.com>
 */
public class WebStorageBackend implements StorageBackend {

  private class CrazyMarshallingSession implements MarshallingSession {

    private int depth = 0;
    private final MarshallingSession encodingSession = MarshallingSessionProviderFactory.getEncoding();

    @Override
    public MappingContext getMappingContext() {
      return encodingSession.getMappingContext();
    }

    @Override
    public Marshaller<Object> getMarshallerInstance(String fqcn) {
      return encodingSession.getMarshallerInstance(fqcn);
    }

    @Override
    public String determineTypeFor(String formatType, Object o) {
      return encodingSession.determineTypeFor(formatType, o);
    }

    @Override
    public String getAssumedElementType() {
      return encodingSession.getAssumedElementType();
    }

    @Override
    public void setAssumedElementType(String assumedElementType) {
      encodingSession.setAssumedElementType(assumedElementType);
    }

    @Override
    public void recordObjectHash(String keyJson, Object value) {
      log("recordObjectHash("+keyJson+")");
      try {
        depth++;
        // We supply all object hashes. This verifies the hash is what we expected.
//        assert (keyJson.equals(getObjectHash(value))) : "Existing key didn't match expected key";

        String valueJson = getMarshallerInstance(value.getClass().getName()).marshall(value, this);

        putImpl(keyJson, valueJson);
      } finally {
        depth--;
      }
    }

    @Override
    public boolean hasObjectHash(Object reference) {
      log("hasObjectHash("+reference+")");
      try {
        depth++;
        log("<-" + true);
        return true;
      } finally {
        depth--;
      }
    }

    @Override
    public boolean hasObjectHash(String keyJson) {
      // XXX should we just return true here too?
      log("hasObjectHash(\""+keyJson+"\")");
      try {
        depth++;
        boolean hasObjectHash = !keysBeingDemarshalled.contains(keyJson) && getImpl(keyJson) != null;
        log("<-" + hasObjectHash);
        return hasObjectHash;
      } finally {
        depth--;
      }
    }

    @Override
    public String getObjectHash(Object reference) {
      log("getObjectHash("+reference+")");
      try {
        depth++;
        return entityManager.keyFor(reference).toJson();
      } finally {
        depth--;
      }
    }

    /**
     * Keys of all the objects that are being demarshalled right now.
     */
    private Set<String> keysBeingDemarshalled = new HashSet<String>();

    @Override
    public <T> T getObject(Class<T> type, String keyJson) {
      log("getObject("+keyJson+")");
      if (keysBeingDemarshalled.contains(keyJson)) {
        throw new RuntimeException("Got asked again for " + keyJson);
      }
      try {
        depth++;
        keysBeingDemarshalled.add(keyJson);
        String valueJson = getImpl(keyJson);
        log("getObject: found " + valueJson);
        if (valueJson == null) {
          return null;
        }
        EJValue ejValue = new GWTJSONValue(JSONParser.parseStrict(valueJson));
        return (T) getMarshallerInstance(type.getName()).demarshall(ejValue, this);
      } finally {
        keysBeingDemarshalled.remove(keyJson);
        depth--;
      }
    }

    private void log(String message) {
      System.out.print(Strings.repeat("  ", depth));
      System.out.println(message);
    }
  }

  private final MarshallingSession marshallingSession = new CrazyMarshallingSession();
  private final ErraiEntityManager entityManager;

  public WebStorageBackend(ErraiEntityManager entityManager) {
    this.entityManager = Assert.notNull(entityManager);
  }

  private native void putImpl(String key, String value) /*-{
    $wnd.sessionStorage.setItem(key, value);
  }-*/;

  private native String getImpl(String key) /*-{
    return $wnd.sessionStorage.getItem(key);
  }-*/;

  private native String removeImpl(String key) /*-{
    return $wnd.sessionStorage.removeItem(key);
  }-*/;

  @Override
  public <X> void put(Key<X, ?> key, X value) {
    String keyJson = key.toJson();
    marshallingSession.recordObjectHash(keyJson, value);
  }

  @Override
  public <X> X get(Key<X, ?> key) {
    String keyJson = key.toJson();
    return marshallingSession.getObject(key.getEntityType().getJavaType(), keyJson);
  }

  @Override
  public <X> void remove(Key<X, ?> key) {
    String keyJson = key.toJson();
    removeImpl(keyJson);
  }

  @Override
  public <X> boolean isModified(Key<X, ?> key, X value) {
    // FIXME this needs to be reworked not to resolve embedded object references
    String keyJson = key.toJson();
    String valueJson = Marshalling.toJSON(value);
    return !valueJson.equals(getImpl(keyJson));
  }

}
