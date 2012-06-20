package org.jboss.errai.jpa.client.local;

import java.util.Iterator;

import javax.persistence.metamodel.SingularAttribute;

import com.google.gwt.json.client.JSONValue;

/**
 * Extends the JPA SingularAttribute interface with methods required by Errai
 * persistence but missing from the JPA metamodel.
 *
 * @param <X>
 *          The type containing the represented attribute
 * @param <T>
 *          The type of the represented attribute
 * @author Jonathan Fuerth <jfuerth@gmail.com>
 */
public interface ErraiSingularAttribute<X, T> extends ErraiAttribute<X, T>, SingularAttribute<X, T> {

  /**
   * Can the attribute's value be generated (usually for ID attributes).
   */
  public boolean isGeneratedValue();

  /**
   * Returns a generator for the values of this attribute. Only works for
   * attributes that are annotated with {@code @GeneratedValue}.
   *
   * @return the ID generator for this generated attribute. Never null.
   * @throws UnsupportedOperationException
   *           if this attribute is not a {@code @GeneratedValue}.
   */
  public Iterator<T> getValueGenerator();

  /**
   * Converts the given JSON value into a Java Object, using the conversion
   * logic appropriate to this attribtute's Java type.
   *
   * @param jsonValue
   *          The JSON value to convert. Must be either JSONNull or the JSON
   *          type that would be returned by
   *          {@link #toJson(T)}. The Java {@code null}
   *          value is not permitted.
   * @return The Java value corresponding with the given JSON value. Will be
   *         null if the input value is a JSONNull.
   */
  public T fromJson(JSONValue jsonValue);

  /**
   * Converts the given value into a JSON value, using the conversion logic
   * appropriate to this attribtute's Java type.
   *
   * @param value
   *          The value to convert. Null is permitted.
   * @return The JSON representation of the given object. Never null (JSONNull
   *         is returned for a null input value). Can be converted back to a
   *         Java value by calling {@link #fromJson(JSONValue)} on the same
   *         attribute that created it.
   */
  public JSONValue toJson(T value);

}
