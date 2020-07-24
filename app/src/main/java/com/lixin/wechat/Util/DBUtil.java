package com.lixin.wechat.Util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.lixin.wechat.NewRootDBHelper.mWxDbPathList;


public class DBUtil {
    public static void  execShell(String cmd) {

        try {
            //   if (hasRootPerssion()) {
            //权限设置
            Process p = Runtime.getRuntime().exec("su");  //开始执行shell脚本
            //获取输出流
            OutputStream outputStream = p.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            //将命令写入
            dataOutputStream.writeBytes(cmd);
            //提交命令
            dataOutputStream.flush();
            //关闭流操作
            dataOutputStream.close();
            outputStream.close();


//            } else {
//                Acesslog("没有 root权限 不执行");
//            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }

    /**
     * 时间戳转换成日期格式字符串
     * @param seconds 精确到秒的字符串
     * @return
     */
    public static String timeStamp2Date(String seconds,String format) {
        if(seconds == null || seconds.isEmpty() || seconds.equals("null")){
            return "";
        }
        if(format == null || format.isEmpty()){
            format = "yyyy-MM-dd HH:mm:ss";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date(Long.valueOf(seconds+"000")));
    }
    /**
     * 复制单个文件
     *
     * @param oldPath String 原文件路径 如：c:/fqf.txt
     * @param newPath String 复制后路径 如：f:/fqf.txt
     * @return boolean
     */
    public static boolean copyFile(String oldPath, String newPath) {
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
                return true;
            }
        } catch (Exception e) {
            System.out.println("复制单个文件操作出错 "+e);
            e.printStackTrace();
            return false;
        }
        return false;
    }

    /**
     * 递归查询微信本地数据库文件
     *
     * @param file     目录
     * @param fileName 需要查找的文件名称
     */
    public static void searchFile(File file, String fileName) {
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

    public static String getMD5(String info)
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
     * 获取手机IMEI号
     */
    public static String getIMEI(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {

            return null;
        }
        String imei = telephonyManager.getDeviceId();

        return imei;
    }
    public static String readSDFile(String fname) {
        File file1 = new File(fname);
        if (file1.isFile() && file1.exists()) {
            String result=null;
            long millisRead = System.currentTimeMillis();
            try {
                File f=new File(fname);   //File(Environment.getExternalStorageDirectory().getPath()+fname);
                int length=(int)f.length();
                byte[] buff=new byte[length];
                FileInputStream fin=new FileInputStream(f);
                fin.read(buff);
                fin.close();
                result=new String(buff,"UTF-8");

//            Acesslog("readSDFile time = "
//                    + (System.currentTimeMillis() - millisRead)+"ms;");

                return result;

            }catch (Exception e){

                WXSQL("读取文件异常:"+e);

            }

        }
        return null;
    }
    /**
     * 判断2个时间大小
     * yyyy-MM-dd HH:mm 格式（自己可以修改成想要的时间格式）
     * @param startTime
     * @param endTime
     * @return
     */
    public static File timeCompare(String startTime, String endTime){
        int i=0;
        //注意：传过来的时间格式必须要和这里填入的时间格式相同
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try {
            Date date1 = dateFormat.parse(startTime);//时间1
            Date date2 = dateFormat.parse(endTime);//时间2
            // 1 结束时间小于开始时间 2 开始时间与结束时间相同 3 结束时间大于开始时间
            if (date2.getTime()<date1.getTime()){
                //时间2小于时间1
                i= 1;
                return mWxDbPathList.get(0).getAbsoluteFile();
            }else if (date2.getTime()==date1.getTime()){
                //时间1与时间2相同
                i= 2;
                return mWxDbPathList.get(0).getAbsoluteFile();
            }else if (date2.getTime()>date1.getTime()){
                //时间2大于时间1
                i= 3;
                return mWxDbPathList.get(1).getAbsoluteFile();
            }
        } catch (Exception e) {

        }
        return mWxDbPathList.get(0).getAbsoluteFile();
    }

    public static String getFileTime(File f){
        try {

            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm") .format(new Date(f.lastModified()));
            //       WXSQL(f+"getFileTime 文件文件创建时间" + time);
            return time;
//            WXSQL("getFileTime文件名称：" + f.getName());
//            WXSQL("getFileTime文件是否存在：" + f.exists());
//            WXSQL("getFileTime文件的相对路径：" + f.getPath());
//            WXSQL("getFileTime文件的绝对路径：" + f.getAbsolutePath());
//            WXSQL("getFileTime文件可以读取：" + f.canRead());
//            WXSQL("getFileTime文件可以写入：" + f.canWrite());
//            WXSQL("getFileTime文件上级路径：" + f.getParent());
//            WXSQL("getFileTime文件大小：" + f.length() + "B");
//            WXSQL("getFileTime文件最后修改时间：" + new Date(f.lastModified()));
//            WXSQL("getFileTime是否是文件类型：" + f.isFile());
//            WXSQL("getFileTime是否是文件夹类型：" + f.isDirectory());

        } catch (Exception e) {
            e.printStackTrace();
            WXSQL("getFileTime Exception " + e);
        }
        return null;
    }

    /**
     * 文件数据写入（如果文件夹和文件不存在，则先创建，再写入）
     * @param filePath
     * @param content
     * @param flag true:如果文件存在且存在内容，则内容换行追加；false:如果文件存在且存在内容，则内容替换
     */
    public static String fileLinesWrite(String filePath,String content,boolean flag){
        String filedo = "write";
        FileWriter fw = null;
        try {
            File file=new File(filePath);
            //如果文件夹不存在，则创建文件夹
            if (!file.getParentFile().exists()){
                file.getParentFile().mkdirs();
            }
            if(!file.exists()){//如果文件不存在，则创建文件,写入第一行内容
                file.createNewFile();
                fw = new FileWriter(file);
                filedo = "create";
            }else{//如果文件存在,则追加或替换内容
                fw = new FileWriter(file, flag);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        PrintWriter pw = new PrintWriter(fw);
        pw.print(content);
        pw.flush();
        try {
            fw.flush();
            pw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filedo;
    }
    public static void  WXSQL(String info)
    {
        Log.v("WXSQL", info);
    }
}
