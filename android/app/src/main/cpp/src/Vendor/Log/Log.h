//
// Created by SyXhOwN on 17/01/2025.
//

#pragma once

#include "../../reGTA.h"

#define NDEBUG  false
#define USE_FILE_LOG    false

void Log(const char *fmt, ...);
void CrashLog(const char* fmt, ...);

#ifdef NDEBUG
#define DLOG(...)
#else
#define DLOG(...) __android_log_print(ANDROID_LOG_DEBUG, "DEBUG", __VA_ARGS__)
#endif