package com.ggec.voice.toollibrary.crashreport;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.text.TextUtils;
import android.util.Base64;

import com.ggec.voice.toollibrary.BuildConfig;
import com.ggec.voice.toollibrary.Util;
import com.ggec.voice.toollibrary.log.Log;

import org.acra.ReportField;
import org.acra.collector.CrashReportData;
import org.acra.model.Element;
import org.acra.model.StringElement;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Date;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LogSender extends BroadcastReceiver implements ReportSender {
	private static final String TAG = "ACRA-LogSender";

	private static Context mContext;
	private static boolean mHaveConfigInfo = false;
	private static long mUid;
	private static int mAppId;
	private static byte []mCookie;
	private static String mUrl;
	private static String mVersion;

	private static final String URL_PREFIX = "?";
	private static final String LOG_FILE_EXTENDS = ".txt";

	public LogSender(Context context) {
		mContext = context;
		mHaveConfigInfo = false;
		mAppId = -1;
		mUid = 0;
		mCookie = null;
		mVersion = Util.getPackageVersionName(mContext);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(this, filter);
	}

//	public static void sendExistLogs() {
//		Log.d(TAG,"sendExistLogs");
//		Daemon.handler().postDelayed(sendExistLogsTask, 3210);
//	}

	public static void setConfigInfo(Context context, int appId, int uid) {
		mContext = context;
		setConfigInfo(appId, uid);
	}

	private static void setConfigInfo(int appId, int uid) {
		Log.d(TAG, "setConfigInfo");
		mAppId = appId;
		mUid = 0xffffffffl & uid;
		mHaveConfigInfo = true;
	}

	public static void setCookie(byte []cookie) {
		Log.d(TAG, "setCookie");
		mCookie = cookie;
		if(cookie == null) {
			return;
		}
		mUrl = URL_PREFIX + "cookie=" + Base64.encodeToString(mCookie, Base64.NO_WRAP) + "&appId=" + mAppId;

	}

	/**
	 * 升级ACRA到4.8.5 ACRA另启新进程，这个方法跑在ACRA进程，sendExistLogs之前需要使用loadConfig设置参数
	 * @param context
	 * @param intent
	 */
	@Override
    public void onReceive(Context context, Intent intent) {
        boolean available = true;
        try {
            available = Util.isNetworkAvailable(context);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        if (available) {
//			loadConfigIfNeeded(context);
//			sendExistLogs();
//        }
    }

	/**
	 * 升级ACRA到4.8.5 ACRA另启新进程，这个方法跑在ACRA进程，sendExistLogs之前需要使用loadConfig设置参数
	 * @param context
	 * @param report
	 * @throws ReportSenderException
	 */
	@Override
	public void send(Context context, CrashReportData report) throws ReportSenderException {
		Log.d(TAG, "send");
		String logDirPath = mContext.getExternalCacheDir().getPath();
		saveLog2File(logDirPath, report);
		loadConfigIfNeeded(context);
//        sendExistLogs();
	}

	private void loadConfigIfNeeded(Context context) {
		if(!mHaveConfigInfo || mCookie == null || mCookie.length == 0) {
//			YYConfig config = new YYConfig(context);
//			int appId = config.appId();
//			int mUid = config.uid();
//			setConfigInfo(appId, mUid);
//			setCookie(config.cookie());
		}
	}

    private static Runnable sendExistLogsTask = new Runnable() {
        @Override
        public void run() {
            if (mContext == null) {
	            return;
            }

            if (mContext.getCacheDir() != null) {
	            uploadDirLog(mUrl, mContext.getCacheDir().getPath());
            }
        }
    };

	private static synchronized String saveLog2File(String logDirPath, CrashReportData report) {
		Log.d(TAG, "saveLog2File");
		File logDirFile = new File(logDirPath);
		if (!logDirFile.exists()) {
			logDirFile.mkdirs();
		}
		
		String now = Util.getFormatedTime(new Date());
		String fileName = logDirPath + "/java_log_ver" + mVersion + "_uid" + mUid  + "_" + now + LOG_FILE_EXTENDS;
		String networkType = Util.getNetworkType(mContext);

		FileOutputStream fos = null;
		PrintStream ps = null;
		FileLock fileLock = null;
		try {
			// If uid is known, set the log file name to java_log_verxxx_uidxxx_now.txt, otherwise java_log_now.txt
			// when the uid is set and uploading log files, rename java_log_now.txt to java_log_verxxx_uidxxx_now.txt
			fos = new FileOutputStream( new File( fileName ) , true );
			FileChannel fc = fos.getChannel();
			fileLock = fc.tryLock();
			// The file is in writing state if fileLock is null
			if ( fileLock == null ) {
				return null;
			}
			ps = new PrintStream( fos );
			ps.println("NETWORK_TYPE=" + networkType);
			for ( Map.Entry<ReportField, Element> entry : report.entrySet() ) {
				ps.println( entry.toString() );
				if (entry.getKey() == ReportField.STACK_TRACE && (entry.getValue() instanceof StringElement) && (
						((String)entry.getValue().value()).contains("at android.view.HardwareRenderer")
						|| ((String)entry.getValue().value()).contains("at android.view.ThreadedRenderer")
				)) {
					Log.w(TAG, "HardwareRenderer crashed");
//					DeviceSetting.setHardwareRendererCrashed(mContext);
				}
			}
			ps.println(Util.getApplicationWorkspaceInfo(mContext));
		} catch ( Exception e ) {
			Log.e( TAG , "Exception occurs when saving crash log: " , e );
			return null;
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if ( fileLock != null ) {
				try {
					fileLock.release();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		Log.d(TAG, "saveLog2File fileName " + fileName);

		return fileName;
	}

    // 如果jni崩溃文件没有微会的so文件，则不上传
    // yymeet用来确保如果后续增加了so文件而这里没更新，通过路径com.yy.yymeet也能知道是微会的so文件
	// dead用来判断是否是native的内存搞坏了导致的崩溃(deadbaad等)
    private static final String[] nativeLibraryNames = new String[] { "audiosdk", "newaudio" , "videosdk", "newvideo", "yycommonlib", "yymobilepatch", "yyutil", "yymeet", "dead" };

	private synchronized static void uploadDirLog(String url , String logDirPath) {
        if (!mHaveConfigInfo || url == null) {
			return;
		}

        File logDirFile = new File(logDirPath);
		String [] fileList = logDirFile.list();
        if (fileList == null) {
            return;
        }

		for (String filename : fileList) {
			if (!filename.startsWith("java_") && !filename.startsWith("jni_")) {
				continue;
			}
			if (!filename.endsWith(".txt") && !filename.endsWith(".zip")) {
				continue;
			}

            // 发布版检查下jni崩溃文件，如果文件中没有微会的so文件，则不上传
			if (!BuildConfig.DEBUG) {
				if (filename.startsWith("jni_") && filename.endsWith(".txt")) {
					BufferedReader reader = null;
					String filepath = logDirPath + File.separator + filename;
					try {
						boolean yymeetCrash = false;
						reader = new BufferedReader(new FileReader(filepath));
						String line = null;
						outer:
						while ((line = reader.readLine()) != null) {
							if (!TextUtils.isEmpty(line)) {
								for (String nativeLibraryName : nativeLibraryNames) {
									if (line.contains(nativeLibraryName)) {
										yymeetCrash = true;
										break outer;
									}
								}
							}
						}
						if (!yymeetCrash) {
							try {
								reader.close();
								reader = null;
							} catch (Exception e) {
								Log.w(TAG, "close " + filename + " throws exception", e);
							}
							new File(filepath).delete();
							continue;
						}
					} catch (Exception e) {
						Log.w(TAG, "process " + filename + " throws exception", e);
					} finally {
						if (reader != null) {
							try {
								reader.close();
							} catch (Exception e) {
								Log.w(TAG, "close " + filename + " throws exception", e);
							}
						}
					}
				}
			}

			if (filename.contains("ver0_") || filename.contains("uid0_")) {
				String newFileName = filename;
				newFileName = newFileName.replace("ver0_", "ver" + mVersion + "_");
				newFileName = newFileName.replace("uid0_", "uid" + mUid + "_");

				File oldFile = new File(logDirPath + File.separator + filename);
				File newFile = new File(logDirPath + File.separator + newFileName);
				if (oldFile.renameTo(newFile)) {
					filename = newFileName;
				}
			}

            if (BuildConfig.DEBUG) {
	            File destDir = new File(android.os.Environment.getExternalStorageDirectory(), BuildConfig.APPLICATION_ID + "/debug/");
	            if (!destDir.isDirectory()) {
		            destDir.mkdirs();
	            }

                File srcFile = new File(logDirPath + File.separator + filename);
	            File destFile = new File(destDir, filename);

                if (Util.isExternalStorageExists()) {
                    Log.i(Log.TAG_APP, "[LogSender]saving log file to /sdcard: " + srcFile + "->" + destFile);
					Util.copyFile(srcFile, destFile);
                    if (BuildConfig.DEBUG) {
                        //本地debug版本，不用上传服务器
                        srcFile.delete();
                    }
                }
//                if (!BuildConfig.DEBUG) {
//                    Log.e(Log.TAG_APP, "[LogSender]uploading -> http for snapshot ver.");
//	                if (filename.startsWith("java_log") || filename.startsWith("jni_log")) {
//						waiting2Send( url , logDirPath , filename );
//	                } else if (Utils.getMyNetworkType(mContext) == Utils.NET_WIFI) {
//						waiting2Send( url , logDirPath , filename );
//	                }
//                }
            } else {
                Log.e(Log.TAG_APP, "[LogSender]uploading -> http for release ver.");

	            String srcPath = logDirPath + File.separator + filename;
	            String desPath = logDirPath + File.separator + filename.replace(".txt", ".zip");
	            if (filename.endsWith(".txt") && compress(filename, srcPath, desPath)) {
		            File txtFile = new File(srcPath);
		            if (txtFile.delete()) {
			            filename = filename.replace(".txt", ".zip");
		            }
	            }

//				if (Utils.getNetworkType(mContext).equals(Utils.NetworkType.Wifi)) {
//					waiting2Send( url , logDirPath , filename );
//                } else if (checkSendWithoutWifi()) {
//					waiting2Send( url , logDirPath , filename );
//					break;
//				} else {
//					break;
//				}
            }
		}
	}

//	private static void waiting2Send( String url , String logDirPath , String filename ){
//		File desFile = new File(logDirPath + File.separator + filename);
//		File newLogsDir = new File(logDirPath + File.separator + "waiting2SendLogs");
//		File newDesFile = new File(logDirPath + File.separator + "waiting2SendLogs"
//				+ File.separator + filename);
//
//		if (!newLogsDir.exists()) {
//			newLogsDir.mkdir();
//		}
//
//		String uploadDirPath = logDirPath;
//		if (desFile.renameTo(newDesFile)) {
//			uploadDirPath = newLogsDir.getAbsolutePath();
//		}
//
//		HttpUtils.uploadLogFile(url, logDirPath , uploadDirPath , filename );
//	}

	private static boolean checkSendWithoutWifi() {
		try {
			final SharedPreferences sp = mContext.getSharedPreferences("LOG_SENDER", Context.MODE_PRIVATE);
			long lastSendTime = sp.getLong("last_send_time", 0);
			long nowTime = System.currentTimeMillis();
			long pastTime = nowTime - lastSendTime;
			if (pastTime < 0 || pastTime > 6 * 60 * 60 * 1000) {
				SharedPreferences.Editor editor = sp.edit();
				editor.putLong("last_send_time", nowTime);
				editor.commit();

				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	private static boolean compress(String entryName, String srcFile, String desFile) {
		BufferedOutputStream bos = null;
		ZipOutputStream zos = null;
		BufferedInputStream bis = null;

		try {
			bos = new BufferedOutputStream(new FileOutputStream(desFile));
			zos = new ZipOutputStream(bos);

			ZipEntry entry = new ZipEntry(entryName);
			zos.putNextEntry(entry);

			bis = new BufferedInputStream(new FileInputStream(srcFile));

			byte[] b = new byte[2048];
			int bytesRead;

			while ((bytesRead = bis.read(b)) != -1) {
				zos.write(b, 0, bytesRead);
			}

			zos.closeEntry();
			zos.flush();

			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (bis != null) {
					bis.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				if (zos != null) {
					zos.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				if (bos != null) {
					bos.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return false;
	}
}