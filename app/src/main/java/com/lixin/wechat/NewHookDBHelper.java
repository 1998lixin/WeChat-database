package com.lixin.wechat;

import android.database.Cursor;


import static com.lixin.wechat.Util.DBUtil.WXSQL;
import static com.lixin.wechat.MainHook.classLoader;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;


public class NewHookDBHelper {
    private static String TAG = "NewHookDBHelper: ";
    public static Object getModelXM(){
        return   callStaticMethod(findClass("com.tencent.mm.model.c", classLoader), "XM");
    }

    public static Cursor HookrawQuery(String sql) {
        WXSQL(TAG + "HOOK rawQuery " + sql);


            Object bSd = getObjectField(getModelXM(), "bSd");
            Class<?> parameterTypes[] = {String.class, String[].class};
            Cursor cursor = (Cursor) callMethod(bSd, "rawQuery",
                    parameterTypes, sql, null);
            WXSQL(TAG + "HOOK rawQuery 成功");
            return cursor;


    }







}
