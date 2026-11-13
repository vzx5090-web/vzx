cmake_minimum_required(VERSION 3.22.1)
project(studiomobile_native LANGUAGES C CXX)
include(FetchContent)

set(LUAU_BUILD_CLI OFF CACHE BOOL "" FORCE)
set(LUAU_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LUAU_BUILD_WEB OFF CACHE BOOL "" FORCE)

FetchContent_Declare(luau
    GIT_REPOSITORY https://github.com/luau-lang/luau.git
    GIT_TAG 0.740
    GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(luau)

add_library(studiomobile SHARED native_luau.cpp)
target_compile_features(studiomobile PRIVATE cxx_std_17)
target_compile_options(studiomobile PRIVATE -Wall -Wextra -Wno-unused-parameter)
target_link_libraries(studiomobile PRIVATE Luau.Compiler Luau.VM android log)
