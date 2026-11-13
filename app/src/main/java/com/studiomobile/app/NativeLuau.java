package com.studiomobile.app;

final class NativeLuau {
    private static final boolean AVAILABLE;
    static {
        boolean loaded;
        try {
            System.loadLibrary("studiomobile");
            loaded = true;
        } catch (UnsatisfiedLinkError error) {
            loaded = false;
        }
        AVAILABLE = loaded;
    }

    private NativeLuau() {}

    static boolean isAvailable() {
        return AVAILABLE;
    }

    static String run(String prelude, String source) {
        if (!AVAILABLE) {
            return "Luau native runtime is not included in this build.";
        }
        return runNative(prelude == null ? "" : prelude, source == null ? "" : source);
    }

    private static native String runNative(String prelude, String source);
}
