//
// Created by SyXhOwN on 18/01/2025.
//

#include "JavaWrapper.h"

#include "../../reGTA.h"
#include "../Log/Log.h"

extern "C" JavaVM *javaVM;

JNIEnv *CJavaWrapper::GetEnv() {
    JNIEnv *env = nullptr;
    int getEnvStat = javaVM->GetEnv((void **) &env, JNI_VERSION_1_6);

    if (getEnvStat == JNI_EDETACHED) {
        Log("GetEnv: not attached");
        if (javaVM->AttachCurrentThread(&env, nullptr) != 0) { // Original = NULL
            Log("Failed to attach");
            return nullptr;
        }
    }
    if (getEnvStat == JNI_EVERSION) {
        Log("GetEnv: version not supported");
        return nullptr;
    }

    if (getEnvStat == JNI_ERR) {
        Log("GetEnv: JNI_ERR");
        return nullptr;
    }

    return env;
}

CJavaWrapper::CJavaWrapper(JNIEnv *env, jobject activity) {
    this->activity = env->NewGlobalRef(activity);

    jclass nvEventClass = env->GetObjectClass(activity);

    if (!nvEventClass) {
        Log("nvEventClass null");
        return;
    }

    env->DeleteLocalRef(nvEventClass);
    env->DeleteLocalRef(activity);
}

CJavaWrapper::~CJavaWrapper() {
    JNIEnv *pEnv = GetEnv();
    if (pEnv) {
        pEnv->DeleteGlobalRef(this->activity);
    }
}

CJavaWrapper *g_pJavaWrapper = nullptr;