#include <jni.h>
#include <android/log.h>
#ifndef TYPEDEFINE_H
#define TYPEDEFINE_H
#if 1
#define  LOG_TAG    "cdebug"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#else
#define  LOG_TAG    "wsddebug"
#define  LOGD(...)
#define  LOGI(...)
#define  LOGE(...)
#endif
#endif

