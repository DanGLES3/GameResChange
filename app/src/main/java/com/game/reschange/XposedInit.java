package com.game.reschange;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_MethodHook;
import android.content.Context;

import java.io.PrintWriter;

public class XposedInit implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        XposedBridge.hookAllMethods(
                XposedHelpers.findClass("android.app.Application", lpparam.classLoader),
                "onCreate",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        PackageManager mPkgMgr = ((Context) param.thisObject).getPackageManager();

                        XposedBridge.hookAllMethods(
                                mPkgMgr.getClass(),
                                "getApplicationInfoAsUser",
                                new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        ApplicationInfo info = (ApplicationInfo) param.getResultOrThrowable();
                                        info.category = ApplicationInfo.CATEGORY_GAME;
                                        param.setResult(info);
                                    }
                                });
                    }
                }
        );
    }
}

