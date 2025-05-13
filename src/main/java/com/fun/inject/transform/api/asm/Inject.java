package com.fun.inject.transform.api.asm;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Inject {
    String method();

    String descriptor() default "()V";
}
