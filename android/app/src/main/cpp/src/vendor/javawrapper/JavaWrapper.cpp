#include "JavaWrapper.h"
#include "../main.h"

extern "C" JavaVM *javaVM;

#include "UI/Keyboard.h"
#include "..//Settings.h"
#include "network/Network.h"
#include "../Multiplayer/Multiplayer.h"
#include "../../gtasa/game_sa/Entity/Ped/Ped.h"

extern CNetGame *pNetGame;
extern CGame *pGame;

JNIEnv *CJavaWrapper::GetEnv() {
    JNIEnv *env = nullptr;
    int getEnvStat = javaVM->GetEnv((void **) &env, JNI_VERSION_1_6);

    if (getEnvStat == JNI_EDETACHED) {
        Log("GetEnv: not attached");
        if (javaVM->AttachCurrentThread(&env, NULL) != 0) {
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

#include "UI/Debug.h"
#include "UI/Chat.h"
#include "UI/UIWrapper.h"

void CJavaWrapper::Vibrate(int milliseconds) {
    JNIEnv *env = GetEnv();

    if (!env) {
        Log("No env");
        return;
    }
    env->CallVoidMethod(this->activity, this->j_Vibrate, milliseconds);
}

void CJavaWrapper::SetPauseState(bool a1) {
    JNIEnv *env = GetEnv();

    if (!env) {
        Log("No env");
        return;
    }
    env->CallVoidMethod(this->activity, this->s_setPauseState, a1);
}

void CJavaWrapper::ExitGame() {

    JNIEnv *env = GetEnv();

    if (!env) {
        Log("No env");
        return;
    }

    env->CallVoidMethod(this->activity, this->s_ExitGame);
}

CJavaWrapper::CJavaWrapper(JNIEnv *env, jobject activity) {
    this->activity = env->NewGlobalRef(activity);

    jclass nvEventClass = env->GetObjectClass(activity);

    if (!nvEventClass) {
        Log("nvEventClass null");
        return;
    }

//    s_GetClipboardText = env->GetMethodID(nvEventClass, "getClipboardText", "()[B");

    j_Vibrate = env->GetMethodID(nvEventClass, "goVibrate", "(I)V");
    s_setPauseState = env->GetMethodID(nvEventClass, "setPauseState", "(Z)V");
    s_ExitGame = env->GetMethodID(nvEventClass, "exitGame", "()V");

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