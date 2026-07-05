package org.hdhmc.hyperwallpapermonet;

interface HookLogger {
    boolean isDebugEnabled();

    void info(String message);

    void debug(String message);

    void error(String message);

    void error(String message, Throwable error);
}
