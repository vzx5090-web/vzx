#include <jni.h>
#include <lua.h>
#include <lualib.h>
#include <luacode.h>
#include "string_codec.h"

#include <algorithm>
#include <chrono>
#include <cstdlib>
#include <string>

namespace {
constexpr size_t MAX_SOURCE = 256 * 1024;
constexpr size_t MAX_OUTPUT = 64 * 1024;
constexpr size_t MAX_MEMORY = 24 * 1024 * 1024;
constexpr auto TIME_BUDGET = std::chrono::milliseconds(2000);

struct Context {
    std::string output;
    std::chrono::steady_clock::time_point deadline;
    size_t allocated = 0;
    bool truncated = false;
    bool timedOut = false;
};

void* boundedAllocator(void* userdata, void* ptr, size_t oldSize, size_t newSize) {
    auto* ctx = static_cast<Context*>(userdata);
    const size_t oldActual = ptr ? oldSize : 0;
    if (newSize == 0) {
        if (ptr) {
            ctx->allocated = oldActual > ctx->allocated ? 0 : ctx->allocated - oldActual;
            std::free(ptr);
        }
        return nullptr;
    }
    if (newSize > oldActual) {
        const size_t growth = newSize - oldActual;
        if (growth > MAX_MEMORY || ctx->allocated > MAX_MEMORY - growth) return nullptr;
    }
    void* next = std::realloc(ptr, newSize);
    if (next) {
        ctx->allocated -= std::min(ctx->allocated, oldActual);
        ctx->allocated += newSize;
    }
    return next;
}

void interruptCallback(lua_State* L, int gc) {
    if (gc != -1) return;
    lua_Callbacks* callbacks = lua_callbacks(L);
    auto* ctx = callbacks ? static_cast<Context*>(callbacks->userdata) : nullptr;
    if (ctx && std::chrono::steady_clock::now() > ctx->deadline) {
        ctx->timedOut = true;
        luaL_error(L, "%s", "execution time limit exceeded");
    }
}

int capturePrint(lua_State* L) {
    auto* ctx = static_cast<Context*>(lua_touserdata(L, lua_upvalueindex(1)));
    if (!ctx) return 0;
    int count = lua_gettop(L);
    for (int i = 1; i <= count; ++i) {
        size_t length = 0;
        const char* value = luaL_tolstring(L, i, &length);
        if (ctx->output.size() < MAX_OUTPUT) {
            if (i > 1) ctx->output.push_back('\t');
            size_t remaining = MAX_OUTPUT - ctx->output.size();
            if (value && length) ctx->output.append(value, std::min(length, remaining));
            if (length > remaining) ctx->truncated = true;
        } else ctx->truncated = true;
        lua_pop(L, 1);
    }
    if (ctx->output.size() < MAX_OUTPUT) ctx->output.push_back('\n');
    else ctx->truncated = true;
    return 0;
}

void removeGlobal(lua_State* L, const char* name) {
    lua_pushnil(L);
    lua_setglobal(L, name);
}
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_studiomobile_app_NativeLuau_runNative(JNIEnv* env, jclass, jstring prelude, jstring source) {
    const std::string userSource = StudioMobile::readJavaString(env, source);
    const std::string prefix = StudioMobile::readJavaString(env, prelude);
    if (userSource.empty()) return StudioMobile::newJavaString(env, "ERROR: source is empty");
    if (userSource.size() > MAX_SOURCE || prefix.size() > 128 * 1024)
        return StudioMobile::newJavaString(env, "ERROR: script exceeds the preview size limit");

    std::string combined;
    combined.reserve(prefix.size() + userSource.size() + 48);
    combined.append(prefix);
    combined.append("\n-- user script\n");
    combined.append(userSource);

    size_t bytecodeSize = 0;
    char* bytecode = luau_compile(combined.data(), combined.size(), nullptr, &bytecodeSize);
    if (!bytecode || bytecodeSize == 0) {
        if (bytecode) std::free(bytecode);
        return StudioMobile::newJavaString(env, "COMPILE ERROR: Luau compiler returned no bytecode");
    }

    Context ctx;
    ctx.deadline = std::chrono::steady_clock::now() + TIME_BUDGET;
    lua_State* L = lua_newstate(boundedAllocator, &ctx);
    if (!L) {
        std::free(bytecode);
        return StudioMobile::newJavaString(env, "ERROR: unable to allocate the 24 MiB VM");
    }

    luaL_openlibs(L);
    const char* blocked[] = {"io","os","debug","package","require","loadstring","dofile","loadfile","getfenv","setfenv","collectgarbage"};
    for (const char* name : blocked) removeGlobal(L, name);
    luaL_sandbox(L);
    luaL_sandboxthread(L);

    lua_pushlightuserdata(L, &ctx);
    lua_pushcclosure(L, capturePrint, "print", 1);
    lua_setglobal(L, "print");

    lua_Callbacks* callbacks = lua_callbacks(L);
    callbacks->userdata = &ctx;
    callbacks->interrupt = interruptCallback;

    const int loadStatus = luau_load(L, "StudioMobilePreview", bytecode, bytecodeSize, 0);
    std::free(bytecode);
    if (loadStatus != 0) {
        const char* error = lua_tostring(L, -1);
        ctx.output = std::string("LOAD ERROR: ") + (error ? error : "unknown");
        lua_close(L);
        return StudioMobile::newJavaString(env, ctx.output);
    }

    const auto started = std::chrono::steady_clock::now();
    const int runStatus = lua_pcall(L, 0, LUA_MULTRET, 0);
    const auto elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::steady_clock::now() - started).count();
    if (runStatus != 0) {
        const char* error = lua_tostring(L, -1);
        ctx.output += ctx.timedOut ? "RUNTIME TIMEOUT: 2 second execution budget exceeded\n" :
            std::string("RUNTIME ERROR: ") + (error ? error : "unknown") + "\n";
    } else if (ctx.output.empty()) ctx.output = "Execution completed (no output).\n";

    if (ctx.truncated) ctx.output += "\n[Output truncated at 64 KiB]";
    ctx.output += "\n[Luau preview: " + std::to_string(elapsed) + " ms; VM " +
        std::to_string(lua_totalbytes(L, -1) / 1024) + " KiB]";
    lua_close(L);
    return StudioMobile::newJavaString(env, ctx.output);
}
