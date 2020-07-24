package com.lixin.wechat;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;


import com.lixin.wechat.Util.SharedPreferencesHelper;

import org.w3c.dom.Text;

import java.util.LinkedHashMap;

import static com.lixin.wechat.Util.DBUtil.WXSQL;
import static com.lixin.wechat.Util.DBUtil.execShell;
import static com.lixin.wechat.Util.MyApplication.getContextObject;

import static com.lixin.wechat.NewRootDBHelper.ISrawQuery;

public class MainActivity extends AppCompatActivity {

    public static boolean  ISHook =false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        execShell("su");

        new Thread(new Runnable() {
            @Override
            public void run() {
                getUserinfo();
            }
        }).start();





    }


    // 获取当前微信唯一 id
    public static void getUserinfo() {

        try {
            Cursor c1 = ISrawQuery("SELECT * FROM userinfo  WHERE  id = 4 or id = 2 or id=6 or id=42 or id=12292 or id=12293 or id=12291");
            if (c1 == null) return;
            while (c1.moveToNext()) {
                int username_id = Integer.parseInt(c1.getString(c1.getColumnIndex("id")));
                String username_value = c1.getString(c1.getColumnIndex("value"));

                if (username_id == 2) {
                    WXSQL( "getUserinfo_Hook " + username_id + " value wxid:  " + username_value);

                }


            }
            c1.close();
        } catch (Exception e) {
            WXSQL("getUserinfo_Hook 获取wxid失败 " + e);
        }


    }




}
