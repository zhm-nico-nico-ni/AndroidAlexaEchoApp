package com.ggec.voice.toollibrary.log;

import android.os.Process;
import android.os.SystemClock;

import com.ggec.voice.toollibrary.Util;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DebugFileLogger extends Thread {
    private static final String TAG = "DebugFileLogger";
    private static final int KEEP_DAY = 7;
    private static List<LogEntry> logs;
    private static SimpleDateFormat timeFormatter;
    private static SimpleDateFormat dateFormatter;
    private static File sLogDir;
    private static AtomicInteger sCounter;
    private static String sToday;
    private static String sFileTag;
    private static boolean sInited;
    private static int sLogLevel;
    private static boolean sPaused;
    private static final Object mLock;

    private DebugFileLogger() {
        super("debug-logger");
    }

    public static void initialize(final File logDir, final String fileTag) {
        initialize(logDir, fileTag, android.util.Log.INFO);
    }

    public static void initialize(final File logDir, final String fileTag, final int logLevel) {
        if (!Util.isExternalStorageExists()) {
            DebugFileLogger.sInited = false;
            return;
        }
        DebugFileLogger.sInited = true;
        DebugFileLogger.sLogDir = logDir;
        DebugFileLogger.sFileTag = fileTag;
        DebugFileLogger.sLogLevel = logLevel;
        Log.e(TAG, "###init file logger:" + fileTag + "->" + logDir + ",lv:" + DebugFileLogger.sLogLevel);
        new DebugFileLogger().start();
    }

    public static void log(final int level, final String tag, final String message) {
        log(level, tag, message, null);
    }

    public static void log(final int level, final String tag, final String message, final Throwable t) {
        if (!DebugFileLogger.sInited) {
            return;
        }
        if (level < DebugFileLogger.sLogLevel) {
            return;
        }
        final LogEntry entry = new LogEntry();
        entry.counter = DebugFileLogger.sCounter.getAndIncrement();
        entry.rtcTime = System.currentTimeMillis();
        entry.clockTime = SystemClock.elapsedRealtime();
        entry.level = level;
        entry.tag = tag;
        entry.message = message;
        entry.throwable = t;
        DebugFileLogger.logs.add(entry);
        if (DebugFileLogger.sPaused) {
            synchronized (DebugFileLogger.mLock) {
                DebugFileLogger.sPaused = false;
                DebugFileLogger.mLock.notify();
            }
        }
    }

    @Override
    public void run() {
        Process.setThreadPriority(1);
        while (true) {
            final long now = System.currentTimeMillis();
            final String today = DebugFileLogger.dateFormatter.format(new Date(now));
            if (DebugFileLogger.sLogDir.isDirectory() && !today.equals(DebugFileLogger.sToday)) {
                DebugFileLogger.sToday = today;
                final File[] oldFiles = DebugFileLogger.sLogDir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(final File dir, final String filename) {
                        if (!filename.endsWith(".txt") || !filename.startsWith(DebugFileLogger.sFileTag)) {
                            return false;
                        }
                        try {
                            final long time = DebugFileLogger.dateFormatter.parse(filename.substring(DebugFileLogger.sFileTag.length() + 1, filename.length() - 3)).getTime();
                            return now - time > 86400000L * KEEP_DAY;
                        } catch (Exception e) {
                            Log.w(TAG, "parse date failed", e);
                            return false;
                        }
                    }
                });
                if (oldFiles != null) {
                    for (final File file : oldFiles) {
                        file.delete();
                    }
                }
            } else {
                DebugFileLogger.sLogDir.mkdirs();
            }
            final String newFileName = DebugFileLogger.sLogDir + File.separator + DebugFileLogger.sFileTag + "_" + today + ".txt";
            FileWriter writer = null;
            try {
                writer = new FileWriter(newFileName, true);
                while (!DebugFileLogger.logs.isEmpty()) {
                    final LogEntry entry = DebugFileLogger.logs.get(0);
                    String text = entry.message;
                    if (entry.throwable != null) {
                        final StringWriter sw = new StringWriter();
                        entry.throwable.printStackTrace(new PrintWriter(sw));
                        final StringBuilder textBuilder = new StringBuilder(text);
                        textBuilder.append("\n");
                        textBuilder.append(sw.toString());
                        text = textBuilder.toString();
                    }
                    final String time = DebugFileLogger.timeFormatter.format(new Date(entry.rtcTime));
                    writer.write(String.format("[%d][%s][%s(%d)]%s:%s\n", entry.counter, getLogLevel(entry.level), time, entry.clockTime, entry.tag, text));
                    DebugFileLogger.logs.remove(0);
                }
            } catch (IOException e) {
                if (!Util.isExternalStorageExists()) {
                    DebugFileLogger.logs.clear();
                    DebugFileLogger.sInited = false;
                }
            } finally {
                IOUtils.closeQuietly(writer);
            }
            synchronized (DebugFileLogger.mLock) {
                DebugFileLogger.sPaused = true;
                try {
                    DebugFileLogger.mLock.wait();
                } catch (InterruptedException ex2) {
                }
            }
        }
    }

    private static String getLogLevel(final int level) {
        switch (level) {
            case android.util.Log.VERBOSE: {
                return "VERBOSE";
            }
            case android.util.Log.DEBUG: {
                return "DEBUG";
            }
            case android.util.Log.INFO: {
                return "INFO";
            }
            case android.util.Log.WARN: {
                return "WARN";
            }
            case android.util.Log.ERROR: {
                return "ERROR";
            }
            case android.util.Log.ASSERT: {
                return "ASSERT";
            }
            default: {
                return "UNKNOWN";
            }
        }
    }

    static {
        DebugFileLogger.logs = Collections.synchronizedList(new LinkedList<LogEntry>());
        DebugFileLogger.timeFormatter = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
        DebugFileLogger.dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        DebugFileLogger.sCounter = new AtomicInteger(0);
        DebugFileLogger.sLogLevel = android.util.Log.INFO;
        DebugFileLogger.sPaused = true;
        mLock = new Object();
    }

    private static class LogEntry {
        int counter;
        long rtcTime;
        long clockTime;
        int level;
        String tag;
        String message;
        Throwable throwable;
    }
}
