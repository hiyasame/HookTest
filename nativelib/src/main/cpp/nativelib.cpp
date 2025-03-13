#include <jni.h>
#include <sstream>
#include <string>
#include <unistd.h>
#include <dlfcn.h>
#include "mylog.h"
#include "gotutil.h"

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

// JNI LoadNativeLibrary中调用
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    Dl_info info;
    dladdr((void*)JNI_OnLoad, &info);
    __android_log_print(ANDROID_LOG_DEBUG, "SO_ADDR",
                        "Loaded at: %p | Path: %s", info.dli_fbase, info.dli_fname);
    if (nullptr == vm) return JNI_ERR;
    LOGE("call from JNI_OnLoad\n");
    hack();
    return JNI_VERSION_1_6;
}


extern "C" JNIEXPORT jstring JNICALL
Java_com_example_nativelib_NativeLib_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    int pid = getpid();
    std::ostringstream oss;
    oss << "Hello from C++, pid: " << pid;

    return env->NewStringUTF(oss.str().c_str());
}