package com.ryanharter.auto.value.gson;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

@Retention(SOURCE)
@Target(METHOD)
public @interface GsonTypeAdapter {
    // A TypeAdapter or TypeAdapterFactory
    Class<?> value();
}
