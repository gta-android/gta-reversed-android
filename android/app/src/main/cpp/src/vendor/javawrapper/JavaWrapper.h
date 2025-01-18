//
// Created by SyXhOwN on 18/01/2025.
//

#pragma once

#include <jni.h>

#include <string>

class CJavaWrapper
{

public:
	static JNIEnv* GetEnv();

	CJavaWrapper(JNIEnv* env, jobject activity);
	~CJavaWrapper();

	jobject activity;
};

extern CJavaWrapper* g_pJavaWrapper;