#pragma once

#include <jni.h>

#include <string>

#define EXCEPTION_CHECK(env) \
	if ((env)->ExceptionCheck()) \ 
	{ \
		(env)->ExceptionDescribe(); \
		(env)->ExceptionClear(); \
		return; \
	}

class CJavaWrapper
{
	jmethodID j_Vibrate;
	jmethodID s_setPauseState;
	jmethodID s_ExitGame;


public:
	static JNIEnv* GetEnv();

	CJavaWrapper(JNIEnv* env, jobject activity);
	~CJavaWrapper();
	
	void SetPauseState(bool a1);
	void ExitGame();
	void Vibrate(int milliseconds);

	jobject activity;
};

extern CJavaWrapper* g_pJavaWrapper;