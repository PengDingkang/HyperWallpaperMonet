package org.hdhmc.hyperwallpapermonet;

import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import dalvik.system.DexFile;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;

public final class ModernEntry extends XposedModule {
    private static final String TAG = "HyperWallpaperMonet";
    private static final String TARGET_PACKAGE = "com.android.systemui";
    private static final String THEME_OVERLAY_CONTROLLER =
            "com.android.systemui.theme.ThemeOverlayController";
    private static final String COLOR_LISTENER_PREFIX = THEME_OVERLAY_CONTROLLER + "$";

    private boolean isTargetProcess;

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        isTargetProcess = TARGET_PACKAGE.equals(param.getProcessName());
        if (!isTargetProcess) {
            detach();
            return;
        }
        if (BuildConfig.DEBUG) {
            log(Log.DEBUG, TAG, "Loaded in process " + param.getProcessName());
            log(Log.DEBUG, TAG, "Debug diagnostics enabled; sdk=" + Build.VERSION.SDK_INT
                    + " release=" + Build.VERSION.RELEASE
                    + " incremental=" + Build.VERSION.INCREMENTAL
                    + " device=" + Build.MANUFACTURER + "/" + Build.MODEL);
        }
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!isTargetProcess) {
            return;
        }
        if (!TARGET_PACKAGE.equals(param.getPackageName()) || !param.isFirstPackage()) {
            return;
        }

        HookLogger logger = new ModernLogger();
        ThemeOverlayColorFix fix = new ThemeOverlayColorFix(logger);
        try {
            ClassLoader classLoader = param.getClassLoader();
            List<String> codePaths = collectCodePaths(param.getApplicationInfo());
            logger.debug("onPackageReady package=" + param.getPackageName()
                    + " firstPackage=" + param.isFirstPackage()
                    + " classLoader=" + classLoader
                    + " codePathCount=" + codePaths.size()
                    + " codePaths=" + codePaths);
            Class<?> controllerClass = Class.forName(THEME_OVERLAY_CONTROLLER, false, classLoader);
            logger.debug("Resolved ThemeOverlayController: " + controllerClass.getName());
            Class<?> listenerClass = findColorListenerClass(
                    classLoader,
                    controllerClass,
                    codePaths,
                    logger
            );
            logger.info("Resolved wallpaper color listener: " + listenerClass.getName());

            int installedHooks = 0;
            Method startMethod = controllerClass.getDeclaredMethod("start");
            startMethod.setAccessible(true);
            logger.debug("Installing hook: " + startMethod);
            hook(startMethod).intercept(chain -> {
                logger.debug("ThemeOverlayController.start called");
                Object result = chain.proceed();
                fix.onControllerStarted(chain.getThisObject());
                return result;
            });
            installedHooks += 1;

            Method reevaluateMethod = controllerClass.getDeclaredMethod("reevaluateSystemTheme", Boolean.TYPE);
            reevaluateMethod.setAccessible(true);
            logger.debug("Installing hook: " + reevaluateMethod);
            hook(reevaluateMethod).intercept(chain -> {
                Object arg = chain.getArg(0);
                logger.debug("reevaluateSystemTheme called forceReload=" + arg);
                boolean injected = fix.beforeReevaluate(chain.getThisObject());
                if (injected) {
                    logger.debug("reevaluateSystemTheme forcing argument to true after color repair");
                    return chain.proceed(new Object[]{Boolean.TRUE});
                }
                logger.debug("reevaluateSystemTheme proceeding without argument change");
                return chain.proceed();
            });
            installedHooks += 1;

            Method colorsChangedMethod = listenerClass.getDeclaredMethod(
                    "onColorsChanged",
                    WallpaperColors.class,
                    Integer.TYPE,
                    Integer.TYPE
            );
            colorsChangedMethod.setAccessible(true);
            logger.debug("Installing hook: " + colorsChangedMethod);
            hook(colorsChangedMethod).intercept(chain -> {
                Object result = chain.proceed();
                Object colors = chain.getArg(0);
                Object which = chain.getArg(1);
                Object userId = chain.getArg(2);
                logger.debug("onColorsChanged after original colors=" + colorSummaryForLog(colors)
                        + " which=" + which
                        + " userId=" + userId
                        + " listener=" + chain.getThisObject().getClass().getName());
                if ((colors == null || colors instanceof WallpaperColors)
                        && which instanceof Integer
                        && userId instanceof Integer) {
                    try {
                        Object controller = findControllerFromListener(chain.getThisObject(), controllerClass);
                        fix.onColorsChanged(
                                controller,
                                (WallpaperColors) colors,
                                (Integer) which,
                                (Integer) userId
                        );
                    } catch (Throwable error) {
                        logger.error("Failed to read ThemeOverlayController from color listener", error);
                    }
                } else {
                    logger.debug("Ignored onColorsChanged with unexpected arguments");
                }
                return result;
            });
            installedHooks += 1;

            logger.info("Installed HyperOS wallpaper color hooks: " + installedHooks);
        } catch (Throwable error) {
            logger.error("Failed to install HyperOS wallpaper color hooks", error);
        }
    }

    private static Class<?> findColorListenerClass(
            ClassLoader classLoader,
            Class<?> controllerClass,
            List<String> codePaths,
            HookLogger logger
    ) throws ClassNotFoundException {
        for (String codePath : codePaths) {
            logger.debug("Scanning code path for ThemeOverlayController listener: " + codePath);
            Class<?> listenerClass = findColorListenerClassInDex(classLoader, controllerClass, codePath, logger);
            if (listenerClass != null) {
                return listenerClass;
            }
        }

        logger.debug("Dex scan did not find listener; trying numbered fallback candidates");
        for (int index = 1; index <= 32; index++) {
            String className = COLOR_LISTENER_PREFIX + index;
            Class<?> listenerClass = loadColorListenerCandidate(classLoader, controllerClass, className, logger);
            if (listenerClass != null) {
                return listenerClass;
            }
        }
        throw new ClassNotFoundException("No ThemeOverlayController wallpaper color listener found");
    }

    private static Class<?> findColorListenerClassInDex(
            ClassLoader classLoader,
            Class<?> controllerClass,
            String codePath,
            HookLogger logger
    ) {
        DexFile dexFile = null;
        int matchingClassCount = 0;
        try {
            dexFile = new DexFile(codePath);
            Enumeration<String> entries = dexFile.entries();
            while (entries.hasMoreElements()) {
                String className = entries.nextElement();
                if (!className.startsWith(COLOR_LISTENER_PREFIX)) {
                    continue;
                }
                matchingClassCount += 1;
                logger.debug("Checking listener candidate from dex: " + className);
                Class<?> listenerClass = loadColorListenerCandidate(classLoader, controllerClass, className, logger);
                if (listenerClass != null) {
                    logger.debug("Accepted listener candidate from dex after "
                            + matchingClassCount + " prefixed classes: " + className);
                    return listenerClass;
                }
            }
            logger.debug("No listener accepted in code path; prefixedClassCount="
                    + matchingClassCount + " codePath=" + codePath);
        } catch (Throwable error) {
            logger.debug("Failed to scan dex for color listener: " + codePath + " error=" + error);
        } finally {
            if (dexFile != null) {
                try {
                    dexFile.close();
                } catch (IOException ignored) {
                }
            }
        }
        return null;
    }

    private static Class<?> loadColorListenerCandidate(
            ClassLoader classLoader,
            Class<?> controllerClass,
            String className,
            HookLogger logger
    ) {
        try {
            Class<?> candidate = Class.forName(className, false, classLoader);
            if (!WallpaperManager.OnColorsChangedListener.class.isAssignableFrom(candidate)) {
                logger.debug("Rejected listener candidate without OnColorsChangedListener: " + className);
                return null;
            }
            candidate.getDeclaredMethod(
                    "onColorsChanged",
                    WallpaperColors.class,
                    Integer.TYPE,
                    Integer.TYPE
            );
            if (!hasControllerField(candidate, controllerClass)) {
                logger.debug("Rejected listener candidate without controller field: " + className);
                return null;
            }
            logger.debug("Accepted listener candidate: " + className);
            return candidate;
        } catch (Throwable error) {
            logger.debug("Rejected listener candidate " + className + " error=" + error);
            return null;
        }
    }

    private static boolean hasControllerField(Class<?> listenerClass, Class<?> controllerClass) {
        Class<?> current = listenerClass;
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                if (controllerClass.isAssignableFrom(field.getType())) {
                    return true;
                }
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private static Object findControllerFromListener(Object listener, Class<?> controllerClass)
            throws IllegalAccessException, NoSuchFieldException {
        Class<?> current = listener.getClass();
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                if (!controllerClass.isAssignableFrom(field.getType())) {
                    continue;
                }
                field.setAccessible(true);
                Object controller = field.get(listener);
                if (controller != null) {
                    return controller;
                }
            }
            current = current.getSuperclass();
        }
        throw new NoSuchFieldException(listener.getClass().getName() + " has no ThemeOverlayController field");
    }

    private static String colorSummaryForLog(Object value) {
        if (!(value instanceof WallpaperColors)) {
            return String.valueOf(value);
        }
        WallpaperColors colors = (WallpaperColors) value;
        try {
            return "primary=" + colorToString(colors.getPrimaryColor())
                    + " secondary=" + colorToString(colors.getSecondaryColor())
                    + " tertiary=" + colorToString(colors.getTertiaryColor())
                    + " hints=0x" + Integer.toHexString(colors.getColorHints());
        } catch (Throwable error) {
            return String.valueOf(colors);
        }
    }

    private static String colorToString(android.graphics.Color color) {
        if (color == null) {
            return "null";
        }
        return "#" + String.format("%08x", color.toArgb());
    }

    private static List<String> collectCodePaths(ApplicationInfo applicationInfo) {
        List<String> codePaths = new ArrayList<>();
        if (applicationInfo == null) {
            return codePaths;
        }
        if (applicationInfo.sourceDir != null) {
            codePaths.add(applicationInfo.sourceDir);
        }
        if (applicationInfo.splitSourceDirs != null) {
            for (String splitSourceDir : applicationInfo.splitSourceDirs) {
                if (splitSourceDir != null) {
                    codePaths.add(splitSourceDir);
                }
            }
        }
        return codePaths;
    }

    private final class ModernLogger implements HookLogger {
        @Override
        public boolean isDebugEnabled() {
            return BuildConfig.DEBUG;
        }

        @Override
        public void info(String message) {
            log(Log.INFO, TAG, message);
        }

        @Override
        public void debug(String message) {
            if (isDebugEnabled()) {
                log(Log.DEBUG, TAG, message);
            }
        }

        @Override
        public void error(String message) {
            log(Log.ERROR, TAG, message);
        }

        @Override
        public void error(String message, Throwable error) {
            log(Log.ERROR, TAG, message, error);
        }
    }
}
