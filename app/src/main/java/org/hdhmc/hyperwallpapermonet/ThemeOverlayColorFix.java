package org.hdhmc.hyperwallpapermonet;

import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;

final class ThemeOverlayColorFix {
    private static final int FLAG_SYSTEM = 1;
    private static final int FLAG_LOCK = 2;
    private static final int USER_UNKNOWN = Integer.MIN_VALUE;

    private final HookLogger logger;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ThreadLocal<Boolean> forcingReevaluate = ThreadLocal.withInitial(() -> false);

    ThemeOverlayColorFix(HookLogger logger) {
        this.logger = logger;
    }

    void onControllerStarted(Object controller) {
        logger.info("ThemeOverlayController started; scheduling wallpaper color repair");
        logger.debug("Controller state at start: " + controllerState(controller));
        refreshController(controller, "start");
        scheduleRefresh(controller, 1500L, "start+1500ms");
        scheduleRefresh(controller, 5000L, "start+5000ms");
    }

    void onColorsChanged(Object controller, WallpaperColors colors, int which, int userId) {
        try {
            logger.debug("Repair handling onColorsChanged which=" + which
                    + " userId=" + userId
                    + " colors=" + colorSummary(colors)
                    + " stateBefore=" + controllerState(controller));
            if (colors == null) {
                refreshController(controller, "onColorsChanged:null");
                return;
            }
            if (injectColors(controller, colors, userId, "onColorsChanged which=" + which)) {
                forceReevaluate(controller, "onColorsChanged");
            }
        } catch (Throwable error) {
            logger.error("Failed to repair wallpaper color event", error);
        }
    }

    boolean beforeReevaluate(Object controller) {
        if (forcingReevaluate.get()) {
            logger.debug("Skipping beforeReevaluate while forced reevaluate is already in progress");
            return false;
        }
        try {
            int userId = currentUserId(controller, USER_UNKNOWN);
            if (userId == USER_UNKNOWN) {
                logger.debug("Skipping beforeReevaluate because current user id is unknown; state="
                        + controllerState(controller));
                return false;
            }
            WallpaperColors existingColors = currentColorsForUser(controller, userId);
            logger.debug("beforeReevaluate user=" + userId
                    + " existingColors=" + colorSummary(existingColors)
                    + " state=" + controllerState(controller));
            if (existingColors != null) {
                if (needsForcedReevaluate(controller)) {
                    logger.info("Wallpaper colors already exist but overlay state needs a forced reevaluate: "
                            + overlayState(controller));
                    return true;
                }
                logger.debug("beforeReevaluate found existing colors and overlay state is complete");
                return false;
            }
            return injectCurrentWallpaperColors(controller, userId, "before reevaluate");
        } catch (Throwable error) {
            logger.error("Failed to prepare wallpaper colors before reevaluate", error);
            return false;
        }
    }

    private void scheduleRefresh(Object controller, long delayMillis, String reason) {
        logger.debug("Scheduling refresh reason=" + reason + " delayMillis=" + delayMillis);
        mainHandler.postDelayed(() -> refreshController(controller, reason), delayMillis);
    }

    private void refreshController(Object controller, String reason) {
        try {
            int userId = currentUserId(controller, 0);
            logger.debug("Refreshing controller reason=" + reason
                    + " user=" + userId
                    + " stateBefore=" + controllerState(controller));
            if (injectCurrentWallpaperColors(controller, userId, reason)) {
                forceReevaluate(controller, reason);
            } else {
                logger.debug("Refresh did not inject colors reason=" + reason
                        + " stateAfter=" + controllerState(controller));
            }
        } catch (Throwable error) {
            logger.error("Failed to refresh wallpaper colors: " + reason, error);
        }
    }

    private boolean injectCurrentWallpaperColors(Object controller, int userId, String reason)
            throws ReflectiveOperationException {
        WallpaperManager wallpaperManager = (WallpaperManager) ReflectionUtils.getField(controller, "mWallpaperManager");
        WallpaperColors colors = wallpaperManager.getWallpaperColors(FLAG_SYSTEM);
        String source = "system";
        logger.debug("WallpaperManager.getWallpaperColors(system) for " + reason
                + " returned " + colorSummary(colors));
        if (colors == null) {
            colors = wallpaperManager.getWallpaperColors(FLAG_LOCK);
            source = "lock";
            logger.debug("WallpaperManager.getWallpaperColors(lock) for " + reason
                    + " returned " + colorSummary(colors));
        }
        if (colors == null) {
            logger.debug("No WallpaperColors available for " + reason
                    + " state=" + controllerState(controller));
            return false;
        }
        return injectColors(controller, colors, userId, reason + " source=" + source);
    }

    @SuppressWarnings("unchecked")
    private boolean injectColors(Object controller, WallpaperColors colors, int eventUserId, String reason)
            throws ReflectiveOperationException {
        int currentUserId = currentUserId(controller, eventUserId);
        logger.debug("injectColors reason=" + reason
                + " eventUser=" + eventUserId
                + " currentUser=" + currentUserId
                + " incoming=" + colorSummary(colors));
        if (eventUserId != USER_UNKNOWN && currentUserId != eventUserId) {
            logger.debug("Ignoring wallpaper colors for non-current user: eventUser="
                    + eventUserId + " currentUser=" + currentUserId);
            return false;
        }

        SparseArray<WallpaperColors> currentColors =
                (SparseArray<WallpaperColors>) ReflectionUtils.getField(controller, "mCurrentColors");
        WallpaperColors oldColors = currentColors.get(currentUserId);
        logger.debug("mCurrentColors before inject size=" + currentColors.size()
                + " oldForCurrentUser=" + colorSummary(oldColors)
                + " state=" + controllerState(controller));
        if (colors.equals(oldColors)) {
            if (needsForcedReevaluate(controller)) {
                logger.info("Wallpaper colors already injected, forcing incomplete overlay state: "
                        + colorSummary(colors) + " " + overlayState(controller));
                return true;
            }
            logger.debug("Wallpaper colors already injected for user " + currentUserId + ": "
                    + colorSummary(colors));
            return false;
        }

        currentColors.put(currentUserId, colors);
        setAcceptColorEvents(controller);
        logger.info("Injected wallpaper colors for user " + currentUserId
                + " from " + reason + ": " + colorSummary(colors)
                + " stateAfter=" + controllerState(controller));
        return true;
    }

    private WallpaperColors currentColorsForUser(Object controller, int userId)
            throws ReflectiveOperationException {
        @SuppressWarnings("unchecked")
        SparseArray<WallpaperColors> currentColors =
                (SparseArray<WallpaperColors>) ReflectionUtils.getField(controller, "mCurrentColors");
        return currentColors.get(userId);
    }

    private boolean needsForcedReevaluate(Object controller) {
        int mainWallpaperColor = ReflectionUtils.getIntField(controller, "mMainWallpaperColor", 0);
        if (mainWallpaperColor == 0) {
            logger.debug("Overlay state needs reevaluate because mMainWallpaperColor=0");
            return true;
        }
        boolean needsReevaluate = isNullField(controller, "mColorScheme")
                || isNullField(controller, "mNeutralOverlay")
                || isNullField(controller, "mSecondaryOverlay")
                || isNullField(controller, "mDynamicOverlay");
        if (needsReevaluate) {
            logger.debug("Overlay state needs reevaluate because overlay fields are incomplete: "
                    + overlayState(controller));
        }
        return needsReevaluate;
    }

    private boolean isNullField(Object target, String fieldName) {
        try {
            return ReflectionUtils.getField(target, fieldName) == null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private int currentUserId(Object controller, int fallback) {
        try {
            Object userTracker = ReflectionUtils.getField(controller, "mUserTracker");
            Object result = ReflectionUtils.callNoArg(userTracker, "getUserId");
            if (result instanceof Integer) {
                logger.debug("Read current user id=" + result);
                return (Integer) result;
            }
            logger.debug("Unexpected current user id result=" + result + "; fallback=" + fallback);
        } catch (Throwable error) {
            logger.debug("Unable to read current user id; fallback=" + fallback + " error=" + error);
        }
        return fallback;
    }

    private void setAcceptColorEvents(Object controller) {
        try {
            ReflectionUtils.setBooleanField(controller, "mAcceptColorEvents", true);
            logger.debug("Set mAcceptColorEvents=true");
        } catch (Throwable error) {
            logger.debug("Unable to set mAcceptColorEvents=true error=" + error);
        }
    }

    private void forceReevaluate(Object controller, String reason) {
        if (forcingReevaluate.get()) {
            return;
        }
        forcingReevaluate.set(true);
        try {
            logger.debug("Calling reevaluateSystemTheme(true) reason=" + reason
                    + " stateBefore=" + controllerState(controller));
            ReflectionUtils.callBoolean(controller, "reevaluateSystemTheme", true);
            logger.info("Forced ThemeOverlayController reevaluate: " + reason
                    + " stateAfter=" + controllerState(controller));
        } catch (Throwable error) {
            logger.error("Failed to force ThemeOverlayController reevaluate: " + reason, error);
        } finally {
            forcingReevaluate.set(false);
        }
    }

    private String colorSummary(WallpaperColors colors) {
        if (colors == null) {
            return "null";
        }
        try {
            return "primary=" + colorToString(colors.getPrimaryColor())
                    + " secondary=" + colorToString(colors.getSecondaryColor())
                    + " tertiary=" + colorToString(colors.getTertiaryColor())
                    + " hints=0x" + Integer.toHexString(colors.getColorHints());
        } catch (Throwable ignored) {
            return String.valueOf(colors);
        }
    }

    private String colorToString(android.graphics.Color color) {
        if (color == null) {
            return "null";
        }
        return "#" + String.format("%08x", color.toArgb());
    }

    private String controllerState(Object controller) {
        if (!logger.isDebugEnabled()) {
            return "";
        }
        try {
            int userId = currentUserId(controller, USER_UNKNOWN);
            String currentColors = "unknown";
            try {
                WallpaperColors colors = currentColorsForUser(controller, userId);
                currentColors = colorSummary(colors);
            } catch (Throwable error) {
                currentColors = "error=" + error.getClass().getSimpleName();
            }
            return "user=" + userId
                    + " currentColors=" + currentColors
                    + " acceptColorEvents=" + booleanFieldState(controller, "mAcceptColorEvents")
                    + " " + overlayState(controller);
        } catch (Throwable error) {
            return "stateError=" + error;
        }
    }

    private String overlayState(Object controller) {
        if (!logger.isDebugEnabled()) {
            return "";
        }
        return "mainWallpaperColor=" + colorIntState(controller, "mMainWallpaperColor")
                + " colorScheme=" + objectFieldState(controller, "mColorScheme")
                + " neutralOverlay=" + objectFieldState(controller, "mNeutralOverlay")
                + " secondaryOverlay=" + objectFieldState(controller, "mSecondaryOverlay")
                + " dynamicOverlay=" + objectFieldState(controller, "mDynamicOverlay");
    }

    private String colorIntState(Object target, String fieldName) {
        try {
            int value = ReflectionUtils.findField(target.getClass(), fieldName).getInt(target);
            return "#" + String.format("%08x", value);
        } catch (Throwable error) {
            return "error=" + error.getClass().getSimpleName();
        }
    }

    private String booleanFieldState(Object target, String fieldName) {
        try {
            return String.valueOf(ReflectionUtils.findField(target.getClass(), fieldName).getBoolean(target));
        } catch (Throwable error) {
            return "error=" + error.getClass().getSimpleName();
        }
    }

    private String objectFieldState(Object target, String fieldName) {
        try {
            Object value = ReflectionUtils.getField(target, fieldName);
            if (value == null) {
                return "null";
            }
            return value.getClass().getName();
        } catch (Throwable error) {
            return "error=" + error.getClass().getSimpleName();
        }
    }
}
