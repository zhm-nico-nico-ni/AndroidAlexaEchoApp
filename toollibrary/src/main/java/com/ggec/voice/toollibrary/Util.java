package com.ggec.voice.toollibrary;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;

import com.ggec.voice.toollibrary.log.Log;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ggec on 2017/4/14.
 */

public class Util {
    private static final SimpleDateFormat DATE_FORMAT_yyMMdd_HHmmss;

    static {
        DATE_FORMAT_yyMMdd_HHmmss = new SimpleDateFormat("yyMMdd_HHmmss", Locale.getDefault());
    }

    /**
     * 检查WIFI是否已经连接
     */
    public static boolean isWifiAvailable(Context ctx) {
        ConnectivityManager conMan = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (conMan == null) {
            return false;
        }

        NetworkInfo wifiInfo = null;
        try {
            wifiInfo = conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return (wifiInfo != null && wifiInfo.getState() == NetworkInfo.State.CONNECTED);
    }

    /**
     * 检查网络是否连接，WIFI或者手机网络其一
     */
    public static boolean isNetworkAvailable(Context ctx) {
        ConnectivityManager conMan = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (conMan == null) {
            return false;
        }

        NetworkInfo mobileInfo = null;
        try {
            mobileInfo = conMan.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        if (mobileInfo != null && mobileInfo.isConnectedOrConnecting()) {
            return true;
        }

        NetworkInfo wifiInfo = null;
        try {
            wifiInfo = conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        if (wifiInfo != null && wifiInfo.isConnectedOrConnecting()) {
            return true;
        }

        NetworkInfo activeInfo = null;
        try {
            activeInfo = conMan.getActiveNetworkInfo();
        } catch (NullPointerException e) {
            Log.i("Log.TAG_NETWORK", "Exception thrown when getActiveNetworkInfo. " + e.getMessage());
        }
        return activeInfo != null && activeInfo.isConnectedOrConnecting();

    }

    public static boolean isExternalStorageExists() {
        try {
            return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
        }
        catch (Exception ex) {
            return false;
        }
    }

    public static boolean copyFile(File srcFile, File destFile) {
        if (!srcFile.exists() && !srcFile.isFile()) {
            return false;
        }

        try {
            deleteAndCreateNewFile(destFile);
        } catch (IOException ex) {
        }
        if (!destFile.exists() && !destFile.isFile()) {
            return false;
        }

        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(srcFile);
            fos = new FileOutputStream(destFile);
            byte[] buf = new byte[1024 * 8];
            int len;
            while ((len = fis.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }
            return true;
        } catch (FileNotFoundException ex) {
        } catch (IOException ex) {
        } finally {
            IOUtils.closeQuietly(fis);
            IOUtils.closeQuietly(fos);
        }
        return false;
    }

    public static boolean deleteAndCreateNewFile(File file) throws IOException {
        deleteFile(file);
        return createFile(file);
    }

    public static boolean createFile(File file) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
        }
        return (file.exists() && file.isFile());
    }

    public static void deleteFile(File file) {
        if (file.exists() && file.isFile()) {
            file.delete();
        }
    }

    public static String getProcessNameByPID(final Context context, final int pid) {
        final ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningAppProcessInfo> processInfos = am.getRunningAppProcesses();
        if (processInfos != null) {
            for (final ActivityManager.RunningAppProcessInfo info : processInfos) {
                if (info.pid == pid) {
                    return info.processName;
                }
            }
        }
        else {
            FileReader fileReader = null;
            BufferedReader bufferedReader = null;
            try {
                fileReader = new FileReader("/proc/" + pid + "/cmdline");
                bufferedReader = new BufferedReader(fileReader);
                return bufferedReader.readLine().trim();
            }
            catch (Exception e) {
                Log.w("Utils", "get process name by pid failed", e);
            }
            finally {
                IOUtils.closeQuietly(bufferedReader);
                IOUtils.closeQuietly(fileReader);
            }
        }
        return null;
    }

    public static String getMyProcessName(final Context context) {
        return getProcessNameByPID(context, android.os.Process.myPid());
    }

    public static boolean isUIProcess(final String processName) {
        return processName == null || !processName.contains(":");
    }

    public static boolean isUIProcess(final Context context) {
        return isUIProcess(getMyProcessName(context));
    }

    public static String getPackageVersionName(final Context context) {
        try {
            final PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_CONFIGURATIONS);
            return pi.versionName;
        }
        catch (Exception e) {
            Log.w("Utils.TAG", "get package version name failed", e);
            return "unknown";
        }
    }

    public static int getPackageVersionCode(final Context context) {
        try {
            final PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_CONFIGURATIONS);
            return pi.versionCode;
        }
        catch (Exception e) {
            Log.w("Utils.TAG", "get package version code failed", e);
            return 0;
        }
    }

    public static String getFormatedTime(final Date date) {
        return Util.DATE_FORMAT_yyMMdd_HHmmss.format(date);
    }

    public static String getNetworkType(final Context context) {
        String networkType = "";
        final ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = null;
        try {
            netInfo = cm.getActiveNetworkInfo();
        }
        catch (Exception e) {
            Log.w("Utils", "get active network info failed", e);
        }
        if (netInfo != null) {
            final int type = netInfo.getType();
            if (type == 1) {
                networkType = ",w";
            }
            else if (type == 0) {
                networkType = ",3";
//                final int subType = netInfo.getSubtype();
//                if (Utils.MODE_SET_2G.contains(subType)) {
//                    networkType = ",2";
//                }
//                else if (Utils.MODE_SET_3G.contains(subType)) {
//                    networkType = ",3";
//                }
//                else {
//                    Log.w("Utils", "[getNetworkType]unknown mobile subtype:" + subType + ", consider as 3G.");
//                    networkType = ",3";
//                }
            }
        }
        return networkType;
    }

    public static String getApplicationWorkspaceInfo(final Context context) {
        final StringBuilder sb = new StringBuilder();
        try {
            sb.append("SOURCE_PATH=");
            sb.append(context.getApplicationInfo().sourceDir);
            sb.append(" :");
            sb.append(new File(context.getApplicationInfo().sourceDir).length());
            sb.append('\n');
            sb.append("FILES_PATH=");
            sb.append(context.getFilesDir().getAbsolutePath());
            sb.append('\n');
            sb.append("LIB_PATH=");
            sb.append(context.getApplicationInfo().nativeLibraryDir);
            sb.append('\n');
            sb.append("LIB_LIST=");
            getFilesList(sb, new File(context.getApplicationInfo().nativeLibraryDir).list());
            sb.append('\n');
            sb.append("LIB_V7A_LIST=");
            getFilesList(sb, new File(context.getFilesDir().getAbsolutePath().replace("files", "app_lib_v7a")).list());
            sb.append('\n');
        }
        catch (Exception ex) {}
        return sb.toString();
    }

    private static void getFilesList(final StringBuilder sb, final String[] files) {
        if (files == null) {
            return;
        }
        for (final String file : files) {
            sb.append(file);
            sb.append(' ');
        }
    }

    public static void trimCache(Context context) {
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        if (dir != null) {
            // The directory is now empty so delete it
            return dir.delete();
        }
        return false;
    }

    public static String getProductId(Context context){
        String mac = recupAdresseMAC(context);
        String productId = Base64.encodeToString(mac.getBytes(), Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
        Log.d("getProductId", "mac:"+mac +" pid:"+productId);
        return productId;
    }

    public static String recupAdresseMAC(Context context) {
        WifiManager wifiMan = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        @SuppressLint("MissingPermission") WifiInfo wifiInf = wifiMan.getConnectionInfo();
        String mac = wifiInf.getMacAddress();

        if(Build.VERSION.SDK_INT <23){
            if(TextUtils.isEmpty(mac)){
                return Settings.Secure.getString(context.getContentResolver(),
                        Settings.Secure.ANDROID_ID);
            }else
                return mac;
        }
        if("02:00:00:00:00:00".equals(mac)){
            try {
                List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
                for (NetworkInterface nif : all) {
                    if (nif.getName().equalsIgnoreCase("wlan0")) {
                        byte[] macBytes = nif.getHardwareAddress();
                        if (macBytes == null) {
                            return "";
                        }

                        StringBuilder res1 = new StringBuilder();
                        for (byte b : macBytes) {
                            res1.append(String.format("%02X:", b));
                        }

                        if (res1.length() > 0) {
                            res1.deleteCharAt(res1.length() - 1);
                        }
                        return res1.toString();
                    }
                }
            } catch (Exception e) {
                Log.e("MobileAcces", "Erreur lecture propriete Adresse MAC ");
            }
        } else{
            return mac;
        }
        return "02:00:00:00:00:00";
    }

    /**
     * Encode a byte array into a string, while trimming off the last characters, as required by the Amazon token server
     *
     * See: http://brockallen.com/2014/10/17/base64url-encoding/
     *
     * @param arg our hashed string
     * @return a new Base64 encoded string based on the hashed string
     */
    public static String base64UrlEncode(byte[] arg)
    {
        String s = Base64.encodeToString(arg, 0); // Regular base64 encoder
//        s = s.split("=")[0]; // Remove any trailing '='s
//        s = s.replace('+', '-'); // 62nd char of encoding
//        s = s.replace('/', '_'); // 63rd char of encoding
//        s = s.trim();
        Pattern p = Pattern.compile("/|\t|\r|\n");
        Matcher m = p.matcher(s);
        String res = m.replaceAll("");
        if(res.length() >30){
            return res.substring(0, 29);
        }
        return res;
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        if(bytes==null){
            return "empty bytes";
        }
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
