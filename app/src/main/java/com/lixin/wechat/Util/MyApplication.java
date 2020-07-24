package com.lixin.wechat.Util;

/**
 * Created by Administrator on 2017/2/18 0018.
 */

import android.app.Application;
import android.content.Context;

/**
 * 编写自己的Application，管理全局状态信息，比如Context
 * @author yy
 *
 */
public class MyApplication extends Application {
    private static Context context;

    public void onCreate() {
        //获取Context
        context = getApplicationContext();
    }
    //返回
    public static Context getContextObject(){
        return context;
    }
}