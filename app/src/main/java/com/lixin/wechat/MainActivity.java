package com.lixin.wechat;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.widget.Toast;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String WX_ROOT_PATH = "/data/data/com.tencent.mm/";

    // U ID 文件路径
    private static final String WX_SP_UIN_PATH = WX_ROOT_PATH + "shared_prefs/auth_info_key_prefs.xml";



    private String mDbPassword;



    private static final String WX_DB_DIR_PATH = WX_ROOT_PATH + "MicroMsg";
    private List<File> mWxDbPathList = new ArrayList<>();
    private static final String WX_DB_FILE_NAME = "EnMicroMsg.db";


    private String mCurrApkPath = "/data/data/" + MyApplication.getContextObject().getPackageName() + "/";
    private static final String COPY_WX_DATA_DB = "wx_data.db";


    // 提交参数
    private int  count=0;

    private String IMEI;

    private String Uin;
    ;

    private Thread type;

   // private EditText link;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // 获取root权限
        execRootCmd("chmod -R 777 " + WX_ROOT_PATH);
        execRootCmd("chmod  777 /data/data/com.tencent.mm/shared_prefs/auth_info_key_prefs.xml");


        // 获取微信的U id
        initCurrWxUin();



        // 获取 IMEI 唯一识别码
        TelephonyManager phone = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        IMEI = phone.getDeviceId();

        System.out.println("IMEI"+IMEI);

        // 根据imei和uin生成的md5码，获取数据库的密码（去前七位的小写字母）
        initDbPassword(IMEI, Uin);

        System.out.println(mDbPassword + "数据库的密码");

        System.out.println("开始统计好友数量");


        //  递归查询微信本地数据库文件
        File wxDataDir = new File(WX_DB_DIR_PATH);
        mWxDbPathList.clear();
        searchFile(wxDataDir, WX_DB_FILE_NAME);

        System.out.println("查询数据库文件");
        //处理多账号登陆情况
        for (int i = 0; i < mWxDbPathList.size(); i++) {
            File file = mWxDbPathList.get(i);
            String copyFilePath = mCurrApkPath + COPY_WX_DATA_DB;
            //将微信数据库拷贝出来，因为直接连接微信的db，会导致微信崩溃
            copyFile(file.getAbsolutePath(), copyFilePath);
            File copyWxDataDb = new File(copyFilePath);
            openWxDb(copyWxDataDb);
        }
    }
    /**
     * 执行linux指令
     *
     * @param paramString
     */
    public void execRootCmd(String paramString) {
        try {
            Process localProcess = Runtime.getRuntime().exec("su");
            Object localObject = localProcess.getOutputStream();
            DataOutputStream localDataOutputStream = new DataOutputStream((OutputStream) localObject);
            String str = String.valueOf(paramString);
            localObject = str + "\n";
            localDataOutputStream.writeBytes((String) localObject);
            localDataOutputStream.flush();
            localDataOutputStream.writeBytes("exit\n");
            localDataOutputStream.flush();
            localProcess.waitFor();
            localObject = localProcess.exitValue();
        } catch (Exception localException) {
            localException.printStackTrace();
        }
    }



    /**
     * 获取微信的uid
     * 微信的uid存储在SharedPreferences里面
     * 存储位置\data\data\com.tencent.mm\shared_prefs\auth_info_key_prefs.xml
     */
    private void initCurrWxUin() {
        Uin = null;
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
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.e("获取微信uid失败，请检查auth_info_key_prefs文件权限");
        }
    }
    /**
     * 根据imei和uin生成的md5码，获取数据库的密码（去前七位的小写字母）
     *
     * @param imei
     * @param uin
     * @return
     */
    private void initDbPassword(String imei, String uin) {
        if (TextUtils.isEmpty(imei) || TextUtils.isEmpty(uin)) {
            LogUtil.e("初始化数据库密码失败：imei或uid为空");
            return;
        }
        String md5 = getMD5(imei + uin);
        System.out.println(imei+uin+"初始数值");
        System.out.println(md5+"MD5");
        String password = md5.substring(0, 7).toLowerCase();
        System.out.println("加密后"+password);
        mDbPassword = password;
    }

    public String getMD5(String info)
    {
        try
        {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(info.getBytes("UTF-8"));
            byte[] encryption = md5.digest();

            StringBuffer strBuf = new StringBuffer();
            for (int i = 0; i < encryption.length; i++)
            {
                if (Integer.toHexString(0xff & encryption[i]).length() == 1)
                {
                    strBuf.append("0").append(Integer.toHexString(0xff & encryption[i]));
                }
                else
                {
                    strBuf.append(Integer.toHexString(0xff & encryption[i]));
                }
            }

            return strBuf.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            return "";
        }
        catch (UnsupportedEncodingException e)
        {
            return "";
        }
    }

    /**
     * md5加密
     *
     * @param content
     * @return
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

    /**
     * 递归查询微信本地数据库文件
     *
     * @param file     目录
     * @param fileName 需要查找的文件名称
     */
    private void searchFile(File file, String fileName) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File childFile : files) {
                    searchFile(childFile, fileName);
                }
            }
        } else {
            if (fileName.equals(file.getName())) {
                mWxDbPathList.add(file);
            }
        }
    }

    /**
     * 复制单个文件
     *
     * @param oldPath String 原文件路径 如：c:/fqf.txt
     * @param newPath String 复制后路径 如：f:/fqf.txt
     * @return boolean
     */
    public void copyFile(String oldPath, String newPath) {
        try {
            int byteRead = 0;
            File oldFile = new File(oldPath);
            if (oldFile.exists()) { //文件存在时
                InputStream inStream = new FileInputStream(oldPath); //读入原文件
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1444];
                while ((byteRead = inStream.read(buffer)) != -1) {
                    fs.write(buffer, 0, byteRead);
                }
                inStream.close();
            }
        } catch (Exception e) {
            System.out.println("复制单个文件操作出错");
            e.printStackTrace();

        }
    }
    /**
     * 连接数据库
     *
     * @param dbFile
     */
    private void openWxDb(File dbFile) {
        Context context = MyApplication.getContextObject();
        SQLiteDatabase.loadLibs(context);
        SQLiteDatabaseHook hook = new SQLiteDatabaseHook() {
            public void preKey(SQLiteDatabase database) {
            }

            public void postKey(SQLiteDatabase database) {
                database.rawExecSQL("PRAGMA cipher_migrate;"); //兼容2.0的数据库
            }
        };

        try {
            //打开数据库连接
            SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, mDbPassword, null, hook);

            //查询所有联系人（verifyFlag!=0:公众号等类型，群里面非好友的类型为4，未知类型2）
            // Cursor c1 = db.rawQuery("select * from rcontact where verifyFlag = 0  and type != 4 and type != 2 and type !=33 limit 20, 9999", null);
            Cursor c1 = db.rawQuery("select * from rcontact where username not like 'gh_%' and verifyFlag<>24 and verifyFlag<>29 and verifyFlag<>56 and type<>33 and type<>70 and verifyFlag=0 and type<>4 and type<>0 and showHead<>43 and type<>65536",null);

            while (c1.moveToNext()) {
                String type = c1.getString(c1.getColumnIndex("type"));
                System.out.println(type+"参数");
                count++;



            }

            System.out.println("总共参数"+count);
            //  Toast.makeText(getApplicationContext(),"好友总数"+count,Toast.LENGTH_SHORT).show();
            c1.close();
            db.close();
        } catch (Exception e) {
            LogUtil.e("读取数据库信息失败 尝试MEID破解");
//            e.printStackTrace();
            //打开数据库连接
            // 根据imei和uin生成的md5码，获取数据库的密码（去前七位的小写字母）


            // 请自行添加自己手机的MEID  MEID 无法直接获取
              initDbPassword("A100004AF6C883",  Uin);


            /*  String MEID=readFileSdcard("/mnt/sdcard/meid.txt");
            String dMEID=MEID.replace("\r\n","");
            initDbPassword(dMEID.toUpperCase(),Uin);*/

            System.out.println(mDbPassword+"MEID---密码");
            count=0;
            SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, mDbPassword, null, hook);

            //查询所有联系人（verifyFlag!=0:公众号等类型，群里面非好友的类型为4，未知类型2）
            // Cursor c1 = db.rawQuery("select * from rcontact where verifyFlag = 0  and type !=
            // 4 and type != 2 and type !=33 limit 20, 9999", null);

            // 查询联系人 总数量
            Cursor c1 = db.rawQuery("select * from rcontact where username not like 'gh_%' and " +
                    "verifyFlag<>24 and verifyFlag<>29 and verifyFlag<>56 and type<>33 and type<>70 and " +
                    "verifyFlag=0 and type<>4 and type<>0 and showHead<>43 and type<>65536",null);

            while (c1.moveToNext()) {
                String type = c1.getString(c1.getColumnIndex("type"));
                System.out.println(type+"参数");
                count++;

                //Toast.makeText(getApplicationContext(),type,Toast.LENGTH_SHORT).show();

            }
            // Toast.makeText(getApplicationContext(),"好友总数"+count,Toast.LENGTH_SHORT).how();
            System.out.println("总共参数"+count);
            Toast.makeText(this,"好友总数"+count,Toast.LENGTH_SHORT).show();
            c1.close();
            db.close();





        }
    }



 /*   /*//*//**//*读在/mnt/sdcard/目录下面的文件
    public String readFileSdcard(String fileName){

        String res="";

        try{

            FileInputStream fin = new FileInputStream(fileName);

            int length = fin.available();

            byte [] buffer = new byte[length];

            fin.read(buffer);

            res = EncodingUtils.getString(buffer, "UTF-8");

            fin.close();

        }

        catch(Exception e){

            e.printStackTrace();

        }

        return res;

    }*/

}
