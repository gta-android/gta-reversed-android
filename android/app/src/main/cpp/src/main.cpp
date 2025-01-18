//
// Created by SyXhOwN on 17/01/2025.
//

#include "main.h"

// Trace
#include "vendor/trace/StackTrace.h"

void InstallHooks();

uintptr_t g_libGTASA = 0;
uintptr_t g_libREGTA = 0;

struct sigaction act_old;
struct sigaction act1_old;
struct sigaction act2_old;
struct sigaction act3_old;

void PrintBuildCrashInfo()
{
    std::time_t currentTime = std::time(nullptr);
    std::tm* timeInfo = std::localtime(&currentTime);

    CrashLog("Crash time: %d:%d:%d %d:%d:%d", timeInfo->tm_mday, timeInfo->tm_mon, timeInfo->tm_year, timeInfo->tm_hour, timeInfo->tm_min, timeInfo->tm_sec);
    CrashLog("Build times: %s %s. ABI: %s", __TIME__, __DATE__, (VER_x32 ? "armeabi-v7a" : "arm64-v8a"));
}

void handler(int signum, siginfo_t *info, void* contextPtr)
{
    ucontext* context = (ucontext_t*)contextPtr;

    if (act_old.sa_sigaction)
    {
        act_old.sa_sigaction(signum, info, contextPtr);
    }

    if(info->si_signo == SIGSEGV)
    {
        CrashLog(" ");
        PrintBuildCrashInfo();

        CrashLog("SIGSEGV | Fault address: 0x%X", info->si_addr);

        PRINT_CRASH_STATES(context);

        CStackTrace::printBacktrace();
    }
}

void handler1(int signum, siginfo_t* info, void* contextPtr)
{
    auto context = (ucontext_t*)contextPtr;

    if (act1_old.sa_sigaction)
    {
        act1_old.sa_sigaction(signum, info, contextPtr);
    }

    if (info->si_signo == SIGABRT)
    {
        CrashLog(" ");
        PrintBuildCrashInfo();

        CrashLog("SIGABRT | Fault address: 0x%X", info->si_addr);

        PRINT_CRASH_STATES(context);

        CStackTrace::printBacktrace();
    }
}

void handler2(int signum, siginfo_t* info, void* contextPtr)
{
    ucontext* context = (ucontext_t*)contextPtr;

    if (act2_old.sa_sigaction)
    {
        act2_old.sa_sigaction(signum, info, contextPtr);
    }

    if (info->si_signo == SIGFPE)
    {
        PrintBuildCrashInfo();

        CrashLog("SIGFPE | Fault address: 0x%X", info->si_addr);

        PRINT_CRASH_STATES(context);

        CStackTrace::printBacktrace();

    }

}

void handler3(int signum, siginfo_t* info, void* contextPtr)
{
    ucontext* context = (ucontext_t*)contextPtr;

    if (act3_old.sa_sigaction)
    {
        act3_old.sa_sigaction(signum, info, contextPtr);
    }

    if (info->si_signo == SIGBUS)
    {
        PrintBuildCrashInfo();

        CrashLog("SIGBUS | Fault address: 0x%X", info->si_addr);

        PRINT_CRASH_STATES(context);

        CStackTrace::printBacktrace();
    }

}

extern "C"
{
    JavaVM* javaVM = nullptr;

    JavaVM* alcGetJavaVM(void) {
        return javaVM;
    }
}

extern "C"
JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved)
{
    javaVM = vm;

    g_libGTASA = Patch::FindLib("libGTASA.so");

    if(g_libGTASA == 0) {
        Log("ERROR: libGTASA.so address not found!");
        return 0;
    }

    Log("libGTASA.so image base address: 0x%X", g_libGTASA);

    g_libREGTA = Patch::FindLib("libreGTA.so");

    if(g_libREGTA == 0) {
        Log("ERROR: libreGTA.so address not found!");
        return 0;
    }

    Log("libreGTA.so image base address: 0x%X", g_libREGTA);

    Patch::InitHookStuff();

    InstallHooks();

    struct sigaction act;
    act.sa_sigaction = handler;
    sigemptyset(&act.sa_mask);
    act.sa_flags = SA_SIGINFO;
    sigaction(SIGSEGV, &act, &act_old);

    struct sigaction act1;
    act1.sa_sigaction = handler1;
    sigemptyset(&act1.sa_mask);
    act1.sa_flags = SA_SIGINFO;
    sigaction(SIGABRT, &act1, &act1_old);

    struct sigaction act2;
    act2.sa_sigaction = handler2;
    sigemptyset(&act2.sa_mask);
    act2.sa_flags = SA_SIGINFO;
    sigaction(SIGFPE, &act2, &act2_old);

    struct sigaction act3;
    act3.sa_sigaction = handler3;
    sigemptyset(&act3.sa_mask);
    act3.sa_flags = SA_SIGINFO;
    sigaction(SIGBUS, &act3, &act3_old);

    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_gta_reversed_REGTA_initreGTA(JNIEnv *env, jobject thiz)
{
    Log("Initializing reGTA..");

    g_pJavaWrapper = new CJavaWrapper(env, thiz);
}