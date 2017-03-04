package com.ryanharter.auto.value.gson;

import com.google.gson.TypeAdapterFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Annotation to indicate that a given class should generate a concrete implementation of a
 * {@link TypeAdapterFactory} that handles all the publicly denoted adapter implementations of this
 * project.
 * <p>
 * <code><pre>
 *   &#64;GsonTypeAdapterFactory
 *   public abstract class Factory implements TypeAdapterFactory {
 *     public static Factory create() {
 *       return new AutoValueGson_Factory();
 *     }
 *   }
 * </pre></code>
 */
@Retention(SOURCE)
@Target(TYPE)
public @interface GsonTypeAdapterFactory {
}
