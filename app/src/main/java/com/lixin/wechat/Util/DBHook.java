package com.lixin.wechat.Util;


import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

import static com.lixin.wechat.Util.DBUtil.WXSQL;
import static com.lixin.wechat.Util.DBUtil.fileLinesWrite;
import static com.lixin.wechat.Util.DBUtil.readSDFile;
import static com.lixin.wechat.NewRootDBHelper.WXEnMicroMsg;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class DBHook {
    private static final String TAG = "DBHook: ";

    public DBHook(ClassLoader classLoader) {
        // /data/data/com.tencent.mm/MicroMsg/18b1982f9f317f178c6c50d065e33ffa/EnMicroMsg.db
        XposedHelpers.findAndHookMethod(findClass("com.tencent.wcdb.database.SQLiteDatabase", classLoader), "getPath",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {

                        String getPath = String.valueOf(param.getResult());
                        if (getPath.contains("EnMicroMsg.db")) {
                            String wcdb = readSDFile(WXEnMicroMsg);
                            if (wcdb == null || !wcdb.equals(getPath)) {
                                WXSQL(TAG + "微信账号变更- " + getPath + "--更新本地DB地址---" + wcdb);
                                fileLinesWrite(WXEnMicroMsg, getPath, false);
                            }

                        }

                    }
                });

    }
}
