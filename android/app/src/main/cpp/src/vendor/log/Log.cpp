//
// Created by SyXhOwN on 17/01/2025.
//

#include "Log.h"

void Log(const char *fmt, ...)
{
    static char buffer[512] {};

    memset(buffer, 0, sizeof(buffer));

    va_list arg;
    va_start(arg, fmt);
    vsnprintf(buffer, sizeof(buffer), fmt, arg);
    va_end(arg);

    __android_log_write(ANDROID_LOG_INFO, "AXL", buffer);

#if USE_FILE_LOG
	static FILE* flLog = nullptr;

	if(flLog == nullptr && g_pszStorage != nullptr)
	{
		sprintf(buffer, "%sreGTA/log.txt", g_pszStorage);
		flLog = fopen(buffer, "ab");
	}

	if(flLog == nullptr) return;
	fprintf(flLog, "%s\n", buffer);
	fflush(flLog);
#endif
}

void CrashLog(const char* fmt, ...)
{
    static char buffer[512] {};
    memset(buffer, 0, sizeof(buffer));

    va_list arg;
    va_start(arg, fmt);
    vsnprintf(buffer, sizeof(buffer), fmt, arg);
    va_end(arg);

    __android_log_write(ANDROID_LOG_FATAL, "AXL", buffer);

#if USE_FILE_LOG
    static FILE* flLog = nullptr;

	if (flLog == nullptr && g_pszStorage != nullptr)
	{
		sprintf(buffer, "%sreGTA/crash_log.txt", g_pszStorage);
		flLog = fopen(buffer, "ab");
	}

	if (flLog == nullptr) return;
	fprintf(flLog, "%s\n", buffer);
	fflush(flLog);
#endif
}