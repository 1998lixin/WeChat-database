package com.lixin.wechat;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.lixin.wechat.Util.DBHook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class MainHook  implements IXposedHookLoadPackage {
    public static ClassLoader classLoader;
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("com.tencent.mm") )
        {

            findAndHookMethod(Application.class, "attach",
                    Context.class, new XC_MethodHook() {

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                            ClassLoader classLoader1 = ((Context) param.args[0]).getClassLoader();
                            classLoader = classLoader1;
                            new DBHook(classLoader1);

                        }


                    });
        }
    }
}
