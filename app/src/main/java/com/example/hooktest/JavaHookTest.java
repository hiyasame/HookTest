package com.example.hooktest;

import com.example.javahook.api.HookMethod;
import com.example.javahook.api.Origin;

import java.util.Random;

public class JavaHookTest {
    @HookMethod(
            targetOwner = "java/lang/Math",
            targetName = "random",
            targetDescriptor = "()D",
            targetIsStatic = true
    )
    public static double hookRandom() {
        double origin = Origin.call();
        return origin * 100;
    }

    @HookMethod(
            targetOwner = "java/util/Random",
            targetName = "nextInt",
            targetDescriptor = "()I",
            targetIsStatic = false
    )
    public static int hookRandom(Random random) {
        int origin = Origin.call(random);
        return origin % 10;
    }

}
