#pragma once

#include <stdio.h>
#include <dlfcn.h>
#include <execinfo.h>
#include <dlfcn.h>
#include <unwind.h>

#include "../../reGTA.h"

// Log
#include "../Log/Log.h"

#if VERSION_x32
#define PRINT_CRASH_STATES(context) \
	CrashLog("register states:"); \
	CrashLog("r0: 0x%X, r1: 0x%X, r2: 0x%X, r3: 0x%X", (context)->uc_mcontext.arm_r0, (context)->uc_mcontext.arm_r1, (context)->uc_mcontext.arm_r2, (context)->uc_mcontext.arm_r3); \
	CrashLog("r4: 0x%x, r5: 0x%x, r6: 0x%x, r7: 0x%x", (context)->uc_mcontext.arm_r4, (context)->uc_mcontext.arm_r5, (context)->uc_mcontext.arm_r6, (context)->uc_mcontext.arm_r7); \
	CrashLog("r8: 0x%x, r9: 0x%x, sl: 0x%x, fp: 0x%x", (context)->uc_mcontext.arm_r8, (context)->uc_mcontext.arm_r9, (context)->uc_mcontext.arm_r10, (context)->uc_mcontext.arm_fp); \
	CrashLog("ip: 0x%x, sp: 0x%x, lr: 0x%x, pc: 0x%x", (context)->uc_mcontext.arm_ip, (context)->uc_mcontext.arm_sp, (context)->uc_mcontext.arm_lr, (context)->uc_mcontext.arm_pc); \
    CrashLog("1: libGTASA.so + 0x%X", context->uc_mcontext.arm_pc - g_libGTASA); \
    CrashLog("2: libGTASA.so + 0x%X", context->uc_mcontext.arm_lr - g_libGTASA); \
    CrashLog("1: libreGTA.so + 0x%X", context->uc_mcontext.arm_pc - g_libREGTA); \
    CrashLog("2: libreGTA.so + 0x%X", context->uc_mcontext.arm_lr - g_libREGTA);
#else
#define PRINT_CRASH_STATES(context) \
    CrashLog("1: libGTASA.so + 0x%llx", context->uc_mcontext.pc - g_libGTASA); \
    CrashLog("2: libGTASA.so + 0x%llx", context->uc_mcontext.regs[30] - g_libGTASA); \
    CrashLog("1: libREGTA.so + 0x%llx", context->uc_mcontext.pc - g_libREGTA); \
    CrashLog("2: libREGTA.so + 0x%llx", context->uc_mcontext.regs[30] - g_libREGTA);
#endif

class CStackTrace
{
public:
    static void printBacktrace()
    {
        CrashLog("------------ START BACKTRACE ------------");
        CrashLog(" ");
        PrintStackTrace();
    }

private:
    static _Unwind_Reason_Code TraceFunction(_Unwind_Context* context, void* arg) {
        uintptr_t pc = _Unwind_GetIP(context);

        Dl_info info;
        if (dladdr(reinterpret_cast<void*>(pc), &info) && info.dli_sname != nullptr) {
            CrashLog("[adr: %p regta: %p gta: %p] %s\n",
                     reinterpret_cast<void*>(pc),
                     reinterpret_cast<void*>(pc - g_libREGTA),
                     reinterpret_cast<void*>(pc - g_libGTASA),
                     info.dli_sname);
        } else {
            CrashLog("[adr: %p regta: %p gta: %p] name not found\n",
                     reinterpret_cast<void*>(pc),
                     reinterpret_cast<void*>(pc - g_libREGTA),
                     reinterpret_cast<void*>(pc - g_libGTASA));
        }

        return _URC_NO_REASON;
    }

    static void PrintStackTrace() {
        _Unwind_Backtrace(TraceFunction, nullptr);
    }
};