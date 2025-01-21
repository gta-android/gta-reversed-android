//
// Created by SyXhOwN on 17/01/2025.
//

#pragma once

#include <cstdlib>
#include <string>
#include <vector>
#include <list>
#include <unistd.h>
#include <sys/mman.h>
#include <cassert>
#include <dlfcn.h>
#include <jni.h>
#include <android/log.h>
#include <ucontext.h>
#include <pthread.h>

extern uintptr_t g_libGTASA;
extern uintptr_t g_libREGTA;