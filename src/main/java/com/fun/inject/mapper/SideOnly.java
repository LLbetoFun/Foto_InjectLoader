package com.fun.inject.mapper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SideOnly {
    Type value() default Type.AGENT;

    enum Type {
        AGENT,
        INJECTOR
    }
}
