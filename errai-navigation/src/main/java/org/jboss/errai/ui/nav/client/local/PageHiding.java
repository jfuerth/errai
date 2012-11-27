package org.jboss.errai.ui.nav.client.local;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the target method should be called when the {@code @Page}
 * widget it belongs to is about to be removed from the navigation content
 * panel. The snapshot of the current page state for use in navigation history
 * will be taken after the method has returned. Thus, any updates made by the
 * target method to the widget's {@code @PageState} fields will be preserved in
 * the navigation history.
 * <p>
 * The target method must not take any parameters.
 * <p>
 * The target method's return type will usually be {@code void}, in which case
 * the framework constructs a {@link HistoryToken} from the current values
 * of the {@code @PageState} fields when the method returns. A return type of
 * {@code HistoryToken} is also permitted. If the target method returns a
 * non-null {@code HistoryToken}, that token will be used as the page state for
 * the navigation history, <i>and the {@code @PageState} fields of the method
 * will be ignored</i>.
 * <p>
 * The target method can have any access type: public, protected, default, or private.
 * <p>
 * If the target method throws an exception when called, behaviour is undefined.
 *
 * @see Page
 * @see PageState
 * @see Navigation
 * @author Jonathan Fuerth <jfuerth@redhat.com>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PageHiding {

}
