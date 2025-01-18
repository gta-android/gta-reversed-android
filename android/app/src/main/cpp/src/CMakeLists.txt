cmake_minimum_required(VERSION 3.12)
project(reGTA)

set(CMAKE_CXX_STANDARD 20)

if(${ANDROID_ABI} STREQUAL "armeabi-v7a")
    add_definitions(-DVER_x32=true)
elseif(${ANDROID_ABI} STREQUAL "arm64-v8a")
    add_definitions(-DVER_x32=false)
endif()

# Add source files
file(GLOB_RECURSE SOURCES
        ${CMAKE_CURRENT_LIST_DIR}/*.cpp

        # Header Files
        #${CMAKE_CURRENT_LIST_DIR}/game_sa/*.cpp
        #${CMAKE_CURRENT_LIST_DIR}/game_sa/**/*.cpp
        #${CMAKE_CURRENT_LIST_DIR}/game_sa/**/**/*.cpp
        #${CMAKE_CURRENT_LIST_DIR}/game_sa/**/**/**/*.cpp
        #${CMAKE_CURRENT_LIST_DIR}/game_sa/**/**/**/*/*.cpp

        # vendor
        ${CMAKE_CURRENT_LIST_DIR}/vendor/javawrapper/*.cpp
        ${CMAKE_CURRENT_LIST_DIR}/vendor/log/*.cpp
        )

# Add include directories
include_directories(
        ${CMAKE_CURRENT_LIST_DIR}
        ${CMAKE_CURRENT_LIST_DIR}/vendor/
)

add_library(reGTA SHARED ${SOURCES})
target_link_libraries(reGTA
        log
        GLESv3
        shadowhook::shadowhook
)

# Set compilation flags
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -s -w -fstack-protector -Wall -O3 -fexceptions")

