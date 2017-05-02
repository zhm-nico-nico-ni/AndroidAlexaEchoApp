package com.ggec.voice.toollibrary.log;

import android.content.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileLogger {
    private static final String TAG = "FileLogger";
    private Context mContext;
    private String mFileName;
    private File mLogFile;
    private Writer mWriter;
    private boolean mIsOpened;
    private SimpleDateFormat mFormat;

    public FileLogger(final Context context, final String name) {
        this.mIsOpened = false;
        this.mFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
        this.mContext = context;
        this.mFileName = name;
    }

    public FileLogger(final Context context, final File logFile) {
        this.mIsOpened = false;
        this.mFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
        this.mContext = context;
        this.mLogFile = logFile;
    }

    public synchronized boolean open() {
        try {
            if (this.mLogFile == null) {
                final FileOutputStream fs = this.mContext.openFileOutput(this.mFileName, Context.MODE_APPEND);
                this.mWriter = new OutputStreamWriter(fs);
                this.mIsOpened = true;
            } else {
                final FileOutputStream fs = new FileOutputStream(this.mLogFile, true);
                this.mWriter = new OutputStreamWriter(fs);
                this.mIsOpened = true;
            }
            return true;
        } catch (FileNotFoundException e) {
            Log.w(TAG, "open file failed", e);
            return this.mIsOpened = false;
        }
    }

    public synchronized void close() {
        if (this.mIsOpened) {
            try {
                this.mWriter.close();
            } catch (IOException e) {
                Log.w(TAG, "close file failed", e);
            }
            this.mIsOpened = false;
        }
    }

    public void log(final String tag, final String msg, final Throwable th) {
        if (this.mIsOpened) {
            final String line = String.format("[%s:%s]%s\n", this.mFormat.format(new Date()), tag, msg);
            String stack = null;
            if (th != null) {
                Writer writer = null;
                PrintWriter printWriter = null;
                try {
                    writer = new StringWriter();
                    printWriter = new PrintWriter(writer);
                    th.printStackTrace(printWriter);
                    printWriter.flush();
                    writer.flush();
                    stack = writer.toString();
                } catch (Exception e) {
                    Log.w(TAG, "print stack to string failed", e);
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (Exception e1) {
                            Log.w(TAG, "close string writer failed", e1);
                        }
                    }
                    if (printWriter != null) {
                        try {
                            printWriter.close();
                        } catch (Exception e2) {
                            Log.w(TAG, "close print writer failed", e2);
                        }
                    }
                } finally {
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (Exception e2) {
                            Log.w(TAG, "close string writer failed", e2);
                        }
                    }
                    if (printWriter != null) {
                        try {
                            printWriter.close();
                        } catch (Exception e2) {
                            Log.w(TAG, "close print writer failed", e2);
                        }
                    }
                }
            }
            try {
                this.mWriter.write(line);
                if (stack != null) {
                    this.mWriter.write(stack);
                }
                this.mWriter.flush();
            } catch (IOException e3) {
                Log.w(TAG, "write to file failed", e3);
            }
        }
    }
}
