# HookTest

上班摸鱼写的玩具项目，分别实现了 Java 层和 native 层的 Hook 方式

## java hook

基于 asm 插桩实现，提供两种使用方式

基于注解：

~~~java
public class JavaHookTest {
    @HookMethod(
            targetOwner = "java/lang/Math",
            targetName = "random",
            targetDescriptor = "()D",
            targetIsStatic = true
    )
    public static double hookRandom() {
        // 使用 Origin#call 调用原方法
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
~~~

基于 gradle extension:

~~~kotlin
javahook {
    hook {
        method {
            owner = "java/lang/Math"
            name = "random"
            descriptor = "()D"
            isStatic = true
        }

        replaceWith {
            owner = "com/example/hooktest/JavaHookTest"
            name = "hookRandom"
            descriptor = "()D"
        }
    }
}
~~~

## native hook

修改 elf 结构中的 GOT 表实现 hook

~~~cpp
// GOT hook getpid 返回 114514

typedef int (*getpid_fun)();

// 原方法的备份
getpid_fun getpidOri;

// 替换方法
int getpidReplace() {
    LOGE("before hook getpid\n");
    //调用原方法
    int pid = getpidOri();
    LOGE("after hook getpid: %d\n", pid);
    return 114514;
}

void hack() {
    uintptr_t ori = hackGOT("libnativelib.so", "libc.so", "getpid",
                            (uintptr_t) getpidReplace);
    getpidOri = (getpid_fun) ori;
}

//so加载时由linker调用
void __attribute__((constructor)) init() {
    LOGE("call from constructor\n");
    hack();
    LOGE("constructor finish.\n");
}
~~~