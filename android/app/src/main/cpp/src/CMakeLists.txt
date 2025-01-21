cmake_minimum_required(VERSION 3.12)
project(reGTA)

set(CMAKE_CXX_STANDARD 20)

if(${ANDROID_ABI} STREQUAL "armeabi-v7a")
    add_definitions(-DVERSION_x32=true)
elseif(${ANDROID_ABI} STREQUAL "arm64-v8a")
    add_definitions(-DVERSION_x32=false)
endif()

# Add source files
file(GLOB_RECURSE SOURCES
        ${CMAKE_CURRENT_LIST_DIR}/*.cpp

        # Header Files Code
        #${CMAKE_CURRENT_LIST_DIR}/Code/*.cpp
        #${CMAKE_CURRENT_LIST_DIR}/Code/**/*.cpp
        #${CMAKE_CURRENT_LIST_DIR}/Code/**/**/*.cpp
        #${CMAKE_CURRENT_LIST_DIR}/Code/**/**/**/*.cpp
        #${CMAKE_CURRENT_LIST_DIR}/Code/**/**/**/*/*.cpp

        # Header Files RWMBR
        #${CMAKE_CURRENT_LIST_DIR}/RWMBR/SDK/*.cpp
        #${CMAKE_CURRENT_LIST_DIR}/RWMBR/SDK/**/*.cpp
        #${CMAKE_CURRENT_LIST_DIR}/RWMBR/SDK/**/**/*.cpp
        #${CMAKE_CURRENT_LIST_DIR}/RWMBR/SDK/**/**/**/*.cpp
        #${CMAKE_CURRENT_LIST_DIR}/RWMBR/SDK/**/**/**/*/*.cpp

        # Vendor - NOTSA
        ${CMAKE_CURRENT_LIST_DIR}/Vendor/ReversibleHooks/*.cpp
        ${CMAKE_CURRENT_LIST_DIR}/Vendor/JavaWrapper/*.cpp
        ${CMAKE_CURRENT_LIST_DIR}/Vendor/Log/*.cpp
        ${CMAKE_CURRENT_LIST_DIR}/Vendor/Trace/*.cpp
)

# Add include directories
include_directories(
        ${CMAKE_CURRENT_LIST_DIR}/Vendor/
)

add_library(reGTA SHARED ${SOURCES})
target_link_libraries(reGTA
        log
        GLESv3
        shadowhook::shadowhook
)

# Set compilation flags
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -s -w -fstack-protector -Wall -O3 -fexceptions")

