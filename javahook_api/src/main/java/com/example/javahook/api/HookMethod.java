package com.example.javahook.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface HookMethod {
    String targetOwner();

    String targetName();

    String targetDescriptor();

    boolean targetIsStatic();
}
