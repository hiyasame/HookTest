package com.example.javahook.api;

public class Origin {
    public static <T> T call(Object... obj) {
        throw new RuntimeException("This function should be called at a hook method");
    }

}
