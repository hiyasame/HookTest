//
// Created by hiyasame on 2025/3/10.
//

#ifndef HOOKTEST_MYLOG_H
#define HOOKTEST_MYLOG_H

#include <android/log.h>

#if defined(__LP64__)
#define LOG_TAG   "NInject-64"
#else
#define LOG_TAG   "NInject"
#endif

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

#endif //HOOKTEST_MYLOG_H
