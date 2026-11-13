#pragma once
#include <jni.h>
#include <cstdint>
#include <string>
#include <vector>

namespace StudioMobile {
inline void appendUtf8(std::string& out, uint32_t cp) {
    if (cp <= 0x7f) out.push_back(static_cast<char>(cp));
    else if (cp <= 0x7ff) {
        out.push_back(static_cast<char>(0xc0 | (cp >> 6)));
        out.push_back(static_cast<char>(0x80 | (cp & 0x3f)));
    } else if (cp <= 0xffff) {
        out.push_back(static_cast<char>(0xe0 | (cp >> 12)));
        out.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3f)));
        out.push_back(static_cast<char>(0x80 | (cp & 0x3f)));
    } else {
        out.push_back(static_cast<char>(0xf0 | (cp >> 18)));
        out.push_back(static_cast<char>(0x80 | ((cp >> 12) & 0x3f)));
        out.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3f)));
        out.push_back(static_cast<char>(0x80 | (cp & 0x3f)));
    }
}

inline std::string utf16ToUtf8(const jchar* data, size_t length) {
    std::string out;
    out.reserve(length * 2);
    for (size_t i = 0; i < length; ++i) {
        uint32_t cp = data[i];
        if (cp >= 0xd800 && cp <= 0xdbff) {
            if (i + 1 < length && data[i + 1] >= 0xdc00 && data[i + 1] <= 0xdfff) {
                cp = 0x10000 + ((cp - 0xd800) << 10) + (data[++i] - 0xdc00);
            } else cp = 0xfffd;
        } else if (cp >= 0xdc00 && cp <= 0xdfff) cp = 0xfffd;
        appendUtf8(out, cp);
    }
    return out;
}

inline std::vector<jchar> utf8ToUtf16(const std::string& input) {
    std::vector<jchar> out;
    out.reserve(input.size());
    const auto* p = reinterpret_cast<const unsigned char*>(input.data());
    size_t i = 0;
    while (i < input.size()) {
        uint32_t cp = 0xfffd;
        const unsigned char c = p[i++];
        if (c < 0x80) cp = c;
        else {
            int extra = (c >= 0xc2 && c <= 0xdf) ? 1 :
                        (c >= 0xe0 && c <= 0xef) ? 2 :
                        (c >= 0xf0 && c <= 0xf4) ? 3 : -1;
            if (extra > 0 && i + static_cast<size_t>(extra) <= input.size()) {
                uint32_t value = c & ((1u << (6 - extra)) - 1u);
                bool valid = true;
                for (int n = 0; n < extra; ++n) {
                    unsigned char t = p[i++];
                    if ((t & 0xc0) != 0x80) { valid = false; --i; break; }
                    value = (value << 6) | (t & 0x3f);
                }
                const uint32_t minimum = extra == 1 ? 0x80 : extra == 2 ? 0x800 : 0x10000;
                if (valid && value >= minimum && value <= 0x10ffff &&
                    !(value >= 0xd800 && value <= 0xdfff)) cp = value;
            }
        }
        if (cp <= 0xffff) out.push_back(static_cast<jchar>(cp));
        else {
            cp -= 0x10000;
            out.push_back(static_cast<jchar>(0xd800 + (cp >> 10)));
            out.push_back(static_cast<jchar>(0xdc00 + (cp & 0x3ff)));
        }
    }
    return out;
}

inline std::string readJavaString(JNIEnv* env, jstring value) {
    if (!value) return {};
    const jsize length = env->GetStringLength(value);
    const jchar* chars = env->GetStringChars(value, nullptr);
    if (!chars) return {};
    std::string result = utf16ToUtf8(chars, static_cast<size_t>(length));
    env->ReleaseStringChars(value, chars);
    return result;
}

inline jstring newJavaString(JNIEnv* env, const std::string& value) {
    const std::vector<jchar> chars = utf8ToUtf16(value);
    return env->NewString(chars.empty() ? nullptr : chars.data(), static_cast<jsize>(chars.size()));
}
}
