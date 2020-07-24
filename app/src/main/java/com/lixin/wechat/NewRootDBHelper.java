package com.lixin.wechat;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.text.TextUtils;


import com.lixin.wechat.Util.SharedPreferencesHelper;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;


import static com.lixin.wechat.MainActivity.ISHook;

import static com.lixin.wechat.Util.DBUtil.WXSQL;
import static com.lixin.wechat.Util.DBUtil.copyFile;
import static com.lixin.wechat.Util.DBUtil.execShell;
import static com.lixin.wechat.Util.DBUtil.getFileTime;
import static com.lixin.wechat.Util.DBUtil.getIMEI;
import static com.lixin.wechat.Util.DBUtil.getMD5;
import static com.lixin.wechat.Util.DBUtil.readSDFile;
import static com.lixin.wechat.Util.DBUtil.searchFile;
import static com.lixin.wechat.Util.DBUtil.timeCompare;
import static com.lixin.wechat.MainHook.classLoader;
import static com.lixin.wechat.Util.MyApplication.getContextObject;
import static com.lixin.wechat.NewHookDBHelper.HookrawQuery;


/**
 * Created by Administrator on 2019/7/17 0017.
 */

public class NewRootDBHelper {
    // EnMicroMsg.db 存放位置
    public static final String WXEnMicroMsg = Environment.getExternalStoragePublicDirectory("") + "/wxDB.txt";


    private static final String WX_ROOT_PATH = "/data/data/com.tencent.mm/";
    // U ID 文件路径
    private static final String WX_SP_UIN_PATH = WX_ROOT_PATH + "shared_prefs/auth_info_key_prefs.xml";
    private static final String WX_DB_DIR_PATH = WX_ROOT_PATH + "MicroMsg";
    public static List<File> mWxDbPathList = new ArrayList<>();
    private static final String WX_DB_FILE_NAME = "EnMicroMsg.db";
    private static String mCurrApkPath = "/data/data/" + getContextObject().getPackageName() + "/";
    //  public static String mCurrApkPath = "/sdcard/";
    private static final String COPY_WX_DATA_DB = "wx_data.db";

    //将微信数据库拷贝出来，因为直接连接微信的db，会导致微信崩溃
    private static final String copyFilePath = mCurrApkPath + COPY_WX_DATA_DB;
    ;

    private static SQLiteDatabase db;

    //EnMicroMsg
    private static File getWXdb() {


        getDBPassword();

        File copyWxDataDb = null;
        //  递归查询微信本地数据库文件
        File wxDataDir = new File(WX_DB_DIR_PATH);
        mWxDbPathList.clear();
        searchFile(wxDataDir, WX_DB_FILE_NAME);

//        //处理多账号登陆情况
//        for (int i = 0; i < mWxDbPathList.size(); i++) {
//            File file = mWxDbPathList.get(i);
//            WXSQL("file.getAbsolutePath()"+file.getAbsolutePath()+"文件文件创建时间"+  getFileTime(new File(file.getAbsolutePath())));
//
//        }


        copyFile(getEnMicroMsg().getAbsolutePath(), copyFilePath);
        copyWxDataDb = new File(copyFilePath);
        return copyWxDataDb;

    }

    //SnsMicroMsg
    private static File getSNSdb() {
        File copyWxDataDb = null;
        // 获取root权限
        execShell("chmod -R 777 " + WX_ROOT_PATH);
        //  递归查询微信本地数据库文件
        File wxDataDir = new File(WX_DB_DIR_PATH);
        mWxDbPathList.clear();
        searchFile(wxDataDir, "SnsMicroMsg.db");


        //处理多账号登陆情况
        for (int i = 0; i < mWxDbPathList.size(); i++) {
            File file = mWxDbPathList.get(i);
            WXSQL("file.getAbsolutePath()" + file.getAbsolutePath());
            String copyFilePath = mCurrApkPath + "wx_sns_data.db";
            //将微信数据库拷贝出来，因为直接连接微信的db，会导致微信崩溃
            copyFile(file.getAbsolutePath(), copyFilePath);
            copyWxDataDb = new File(copyFilePath);

        }
        WXSQL("getSNSdb " + copyWxDataDb);
        return copyWxDataDb;
    }

    private static void getDBPassword() {
        // 获取root权限
        execShell("chmod -R 777 " + WX_ROOT_PATH);
        execShell("chmod  777 /data/data/com.tencent.mm/shared_prefs/auth_info_key_prefs.xml");

        // 获取微信的U id
        String UIN = initCurrWxUin();
        // 根据imei和uin生成的md5码，获取数据库的密码（去前七位的小写字母）
        initDbPassword(getIMEI(getContextObject()), UIN);
    }

    private static File getEnMicroMsg() {
        int DBNumber = mWxDbPathList.size();
        WXSQL("历史微信账号数量:" + DBNumber);
        if (DBNumber == 1) {
            return mWxDbPathList.get(0).getAbsoluteFile();
        } else if (DBNumber == 2) {
            return timeCompare(getFileTime(mWxDbPathList.get(0).getAbsoluteFile()), getFileTime(mWxDbPathList.get(1).getAbsoluteFile()));
        }
        return mWxDbPathList.get(0).getAbsoluteFile();


    }


    /**
     * 获取微信的uid
     * 微信的uid存储在SharedPreferences里面
     * 存储位置\data\data\com.tencent.mm\shared_prefs\auth_info_key_prefs.xml
     */
    private static String initCurrWxUin() {
        String Uin = null;
        File file = new File(WX_SP_UIN_PATH);
        try {
            FileInputStream in = new FileInputStream(file);
            SAXReader saxReader = new SAXReader();
            Document document = saxReader.read(in);
            Element root = document.getRootElement();
            List<Element> elements = root.elements();
            for (Element element : elements) {
                if ("_auth_uin".equals(element.attributeValue("name"))) {
                    Uin = element.attributeValue("value");
                    return Uin;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            WXSQL("获取微信uid失败，请检查auth_info_key_prefs文件权限");
        }
        return Uin;
    }

    /**
     * 根据imei和uin生成的md5码，获取数据库的密码（去前七位的小写字母）
     */
    private static void initDbPassword(String imei, String uin) {
        if (TextUtils.isEmpty(imei) || TextUtils.isEmpty(uin)) {
            WXSQL("初始化数据库密码失败：imei或uid为空 " + imei + "|" + uin);
            return;
        }
        String md5 = getMD5(imei + uin);
        System.out.println(imei + uin + "初始数值");
        System.out.println(md5 + "MD5");
        String password = md5.substring(0, 7).toLowerCase();
        System.out.println("加密后" + password);

        SharedPreferencesHelper shar = new SharedPreferencesHelper(getContextObject());
        shar.put("mDbPassword", password);

    }


    /**
     * md5加密
     */
    private String md5(String content) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            md5.update(content.getBytes("UTF-8"));
            byte[] encryption = md5.digest();//加密
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < encryption.length; i++) {
                if (Integer.toHexString(0xff & encryption[i]).length() == 1) {
                    sb.append("0").append(Integer.toHexString(0xff & encryption[i]));
                } else {
                    sb.append(Integer.toHexString(0xff & encryption[i]));
                }
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    protected static Cursor rawQuery(String sql) {
        File dbFile;
        String wcdb = readSDFile(WXEnMicroMsg);
        if (wcdb != null) {
            WXSQL("rawQuery wcdb " + wcdb);
         //   getDBPassword();
            copyFile(wcdb, copyFilePath);
            dbFile = new File(copyFilePath);

        } else {

            dbFile = getWXdb();
        }

        String mDbPassword = getDBPassword(dbFile);
        if (mDbPassword == null) {
            return null;
        }
        SQLiteDatabaseHook hook = getSqlhook();


        //打开数据库连接
        db = SQLiteDatabase.openOrCreateDatabase(dbFile, mDbPassword, null, hook);

        Cursor cursor = db.rawQuery(sql, null);
        return cursor;
    }

    // 朋友圈DB
    private static Cursor rawQuerySnS(String sql) {
        try {
            File dbFile = getSNSdb();

            SQLiteDatabaseHook hook = getSqlhook();

            //打开数据库连接
            db = SQLiteDatabase.openOrCreateDatabase(dbFile, null, null, hook);

            Cursor cursor = db.rawQuery(sql, null);
            return cursor;
        } catch (Exception e) {
            WXSQL("rawQuerySnS " + e);
        }
        return null;
    }


    private static String getDBPassword(File dbFile) {
        String mDbPassword = null;
        SharedPreferencesHelper shar = new SharedPreferencesHelper(getContextObject());
        boolean password = shar.contain("mDbPassword");
        if (password == true && dbFile != null) {
            mDbPassword = (String) shar.getSharValue("mDbPassword", "");
            WXSQL("mDbPassword " + mDbPassword);
            WXSQL("dbFile " + dbFile);
        } else {

            WXSQL("密码或者数据库不存在  password " + password + "###" + "dbFile " + dbFile);
            return mDbPassword;
        }
        return mDbPassword;
    }

    private static SQLiteDatabaseHook getSqlhook() {
        Context context = getContextObject();         //MyApplication.getContextObject();
        SQLiteDatabase.loadLibs(context);
        SQLiteDatabaseHook hook = new SQLiteDatabaseHook() {
            public void preKey(SQLiteDatabase database) {
            }

            public void postKey(SQLiteDatabase database) {
                database.rawExecSQL("PRAGMA cipher_migrate;"); //兼容2.0的数据库
            }
        };
        return hook;
    }

    public static Cursor ISrawQuery(String sql) {

        if (ISHook ==true){
            WXSQL("hook环境:"+classLoader);
            return  HookrawQuery(sql);
        }else {
            WXSQL("Root环境");
            return rawQuery(sql);
        }

    }



}
