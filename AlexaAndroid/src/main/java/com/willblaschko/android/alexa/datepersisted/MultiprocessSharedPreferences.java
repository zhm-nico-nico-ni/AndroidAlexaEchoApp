package com.willblaschko.android.alexa.datepersisted;


import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import com.ggec.voice.toollibrary.log.Log;
import com.willblaschko.android.alexa.BuildConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 使用ContentProvider实现多进程SharedPreferences读写;<br>
 * 1、ContentProvider天生支持多进程访问；<br>
 * 2、使用内部私有BroadcastReceiver实现多进程OnSharedPreferenceChangeListener监听；<br>
 * <p>
 * 使用方法：AndroidManifest.xml中添加provider申明：<br>
 * <pre>
 * &lt;provider android:name="MultiprocessSharedPreferences"
 * android:authorities="MultiprocessSharedPreferences"
 * android:process="MultiprocessSharedPreferences"
 * android:exported="false" /&gt;
 * &lt;!-- authorities属性里面最好使用包名做前缀，apk在安装时authorities同名的provider需要校验签名，否则无法安装；--!/&gt;<br>
 * </pre>
 * <p>
 * ContentProvider方式实现要注意：<br>
 * 1、当ContentProvider所在进程android.os.Process.killProcess(pid)时，会导致整个应用程序完全意外退出或者ContentProvider所在进程重启；<br>
 * 重启报错信息：Acquiring provider <processName> for user 0: existing object's process dead；<br>
 * <p>
 * 其他方式实现SharedPreferences的问题：<br>
 * 使用FileLock和FileObserver也可以实现多进程SharedPreferences读写，但是维护成本高，需要定期对照系统实现更新新的特性；
 */
public class MultiprocessSharedPreferences extends ContentProvider implements SharedPreferences {
    private static final String TAG = "MultiprocessSharedPreferences";
    public static final boolean DEBUG = BuildConfig.DEBUG;
    private Context mContext;
    private String mName;
    private int mMode;
    private static final Object CONTENT = new Object();
    private WeakHashMap<OnSharedPreferenceChangeListener, Object> mListeners;
    private BroadcastReceiver mReceiver;

    private final static String AUTHORITY = BuildConfig.APP_PACKAGE_NAME + ".provider.MultiprocessSharedPreferences";
    private static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);
    private UriMatcher mUriMatcher;
    private static final String KEY = "value";
    private static final String KEY_NAME = "name";
    private static final String PATH_WILDCARD = "*/";
    private static final String PATH_GET_ALL = "getAll";
    private static final String PATH_GET_STRING = "getString";
    private static final String PATH_GET_INT = "getInt";
    private static final String PATH_GET_LONG = "getLong";
    private static final String PATH_GET_FLOAT = "getFloat";
    private static final String PATH_GET_BOOLEAN = "getBoolean";
    private static final String PATH_CONTAINS = "contains";
    private static final String PATH_APPLY = "apply";
    private static final String PATH_COMMIT = "commit";
    private static final String PATH_REGISTER_ON_SHARED_PREFERENCE_CHANGE_LISTENER = "registerOnSharedPreferenceChangeListener";
    private static final String PATH_UNREGISTER_ON_SHARED_PREFERENCE_CHANGE_LISTENER = "unregisterOnSharedPreferenceChangeListener";
    private static final String PATH_GET_STRING_SET = "getStringSet";
    private static final int GET_ALL = 1;
    private static final int GET_STRING = 2;
    private static final int GET_INT = 3;
    private static final int GET_LONG = 4;
    private static final int GET_FLOAT = 5;
    private static final int GET_BOOLEAN = 6;
    private static final int CONTAINS = 7;
    private static final int APPLY = 8;
    private static final int COMMIT = 9;
    private static final int REGISTER_ON_SHARED_PREFERENCE_CHANGE_LISTENER = 10;
    private static final int UNREGISTER_ON_SHARED_PREFERENCE_CHANGE_LISTENER = 11;
    private static final int GET_STRING_SET = 12;
    private HashMap<String, Integer> mListenersCount;

    private static ContentValues contentValuesNewInstance(HashMap<String, Object> values) {
        ContentValues r = new ContentValues();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getValue() instanceof String) {
                r.put(entry.getKey(), (String) entry.getValue());
            } else if (entry.getValue() instanceof Integer) {
                r.put(entry.getKey(), (Integer) entry.getValue());
            } else if (entry.getValue() instanceof Long) {
                r.put(entry.getKey(), (Long) entry.getValue());
            } else if (entry.getValue() instanceof Float) {
                r.put(entry.getKey(), (Float) entry.getValue());
            } else if (entry.getValue() instanceof Boolean) {
                r.put(entry.getKey(), (Boolean) entry.getValue());
            } else {
                r.put(entry.getKey(), (short) 22);
            }
        }
        return r;
    }


    /**
     * mode不使用{@link Context#MODE_MULTI_PROCESS}特可以支持多进程了；
     *
     * @param mode
     * @see Context#MODE_PRIVATE
     * @see Context#MODE_WORLD_READABLE
     * @see Context#MODE_WORLD_WRITEABLE
     */
    public static SharedPreferences getSharedPreferences(Context context, String name, int mode) {
        return new MultiprocessSharedPreferences(context, name, mode);
    }

    /**
     * @deprecated 此默认构造函数只用于父类ContentProvider在初始化时使用；
     */
    @Deprecated
    public MultiprocessSharedPreferences() {

    }

    private MultiprocessSharedPreferences(Context context, String name, int mode) {
        mContext = context;
        mName = name;
        mMode = mode;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, ?> getAll() {
        Map<String, ?> value = (Map<String, ?>) getValue(PATH_GET_ALL, null, null);
        return value == null ? new HashMap<String, Object>() : value;
    }

    @Override
    public String getString(String key, String defValue) {
        return (String) getValue(PATH_GET_STRING, key, defValue);
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        return (Set<String>) getValue(PATH_GET_STRING_SET, key, defValues);
    }

    @Override
    public int getInt(String key, int defValue) {
        return (Integer) getValue(PATH_GET_INT, key, defValue);
    }

    @Override
    public long getLong(String key, long defValue) {
        return (Long) getValue(PATH_GET_LONG, key, defValue);
    }

    @Override
    public float getFloat(String key, float defValue) {
        return (Float) getValue(PATH_GET_FLOAT, key, defValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return (Boolean) getValue(PATH_GET_BOOLEAN, key, defValue);
    }

    @Override
    public boolean contains(String key) {
        return (Boolean) getValue(PATH_CONTAINS, key, false);
    }

    @Override
    public Editor edit() {
        return new EditorImpl();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        if (mListeners == null) {
            mListeners = new WeakHashMap<>();
        }
        Boolean result = (Boolean) getValue(PATH_REGISTER_ON_SHARED_PREFERENCE_CHANGE_LISTENER, null, false);
        if (result != null && result) {
            mListeners.put(listener, CONTENT);
            if (mReceiver == null) {
                mReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String name = intent.getStringExtra(KEY_NAME);
                        @SuppressWarnings("unchecked")
                        List<String> keysModified = (List<String>) intent.getSerializableExtra(KEY);
                        if (mName.equals(name) && keysModified != null) {
                            Set<OnSharedPreferenceChangeListener> listeners = new HashSet<OnSharedPreferenceChangeListener>(mListeners.keySet());
                            for (int i = keysModified.size() - 1; i >= 0; i--) {
                                final String key = keysModified.get(i);
                                for (OnSharedPreferenceChangeListener listener : listeners) {
                                    if (listener != null) {
                                        listener.onSharedPreferenceChanged(MultiprocessSharedPreferences.this, key);
                                    }
                                }
                            }
                        }
                    }
                };
                LocalBroadcastManager.getInstance(mContext)
                    .registerReceiver(mReceiver, new IntentFilter(makeAction(mName)));
            }
        }
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        getValue(PATH_UNREGISTER_ON_SHARED_PREFERENCE_CHANGE_LISTENER, null, false); // WeakHashMap
        if (mListeners != null) {
            mListeners.remove(listener);
            if (mListeners.isEmpty() && mReceiver != null) {
                LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mReceiver);
            }
        }
    }

    public final class EditorImpl implements Editor {
        private final Map<String, Object> mModified = new HashMap<>();
        private boolean mClear = false;

        @Override
        public Editor putString(String key, String value) {
            mModified.put(key, value);
            return this;
        }

        @Override
        public Editor putStringSet(String key, Set<String> values) {
            mModified.put(key, (values == null) ? null : new HashSet<>(values));
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            mModified.put(key, value);
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            mModified.put(key, value);
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            mModified.put(key, value);
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            mModified.put(key, value);
            return this;
        }

        @Override
        public Editor remove(String key) {
            mModified.put(key, null);
            return this;
        }

        @Override
        public Editor clear() {
            mClear = true;
            return this;
        }

        @Override
        public void apply() {
            setValue(PATH_APPLY);
        }

        @Override
        public boolean commit() {
            return setValue(PATH_COMMIT);
        }

        private boolean setValue(String pathSegment) {
            boolean result = false;
            String[] selectionArgs = new String[]{String.valueOf(mMode), String.valueOf(mClear)};
            Uri uri = Uri.withAppendedPath(Uri.withAppendedPath(AUTHORITY_URI, mName), pathSegment);
            ContentValues values = contentValuesNewInstance((HashMap<String, Object>) mModified);
            try {
                result = mContext.getContentResolver().update(uri, values, null, selectionArgs) > 0;
            } catch (RuntimeException e) {
                if (DEBUG) e.printStackTrace();
            }

            if (DEBUG) {
                Log.d(TAG, "setValue.mName = " + mName + ", pathSegment = " + pathSegment + ", mModified.size() = " + mModified.size());
            }
            return result;
        }
    }

    private Object getValue(String pathSegment, String key, Object defValue) {
        Object v = null;
        {
            Uri uri = Uri.withAppendedPath(Uri.withAppendedPath(AUTHORITY_URI, mName), pathSegment);
            String[] projection = null;
            if (PATH_GET_STRING_SET.equals(pathSegment) && defValue != null) {
                @SuppressWarnings("unchecked")
                Set<String> set = (Set<String>) defValue;
                projection = new String[set.size()];
                set.toArray(projection);
            }
            String[] selectionArgs = new String[]{String.valueOf(mMode), key, defValue == null ? null : String.valueOf(defValue)};
            Cursor cursor = null;
            try {
                cursor = mContext.getContentResolver().query(uri, projection, null, selectionArgs, null);
            } catch (SecurityException e) {
                // 解决崩溃：
                // java.lang.SecurityException: Permission Denial: reading com.qihoo.storager.MultiprocessSharedPreferences uri content://com.qihoo.appstore.MultiprocessSharedPreferences/LogUtils/getBoolean from pid=2446, uid=10116 requires the provider be exported, or grantUriPermission()
                // at android.content.ContentProvider$Transport.enforceReadPermission(ContentProvider.java:332)
                // ...
                // at android.content.ContentResolver.query(ContentResolver.java:317)
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
            if (cursor != null) {
                Bundle bundle = null;
                try {
                    bundle = cursor.getExtras();
                } catch (RuntimeException e) {
                    // 解决ContentProvider所在进程被杀时的抛出的异常：
                    // java.lang.RuntimeException: android.os.DeadObjectException
                    // at android.database.BulkCursorToCursorAdaptor.getExtras(BulkCursorToCursorAdaptor.java:173)
                    // at android.database.CursorWrapper.getExtras(CursorWrapper.java:94)
                    if (DEBUG) {
                        e.printStackTrace();
                    }
                }
                if (bundle != null) {
                    v = bundle.get(KEY);
                    bundle.clear();
                }
                cursor.close();
            }
        }
        if (DEBUG) {
            Log.d(TAG, "getValue.mName = " + mName + ", pathSegment = " + pathSegment + ", key = " + key + ", defValue = " + defValue);
        }
        return v == null ? defValue : v;
    }

    private String makeAction(String name) {
        return String.format("%1$s_%2$s", MultiprocessSharedPreferences.class.getName(), name);
    }

    @Override
    public boolean onCreate() {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(AUTHORITY, PATH_WILDCARD + PATH_GET_ALL, GET_ALL);
        mUriMatcher.addURI(AUTHORITY, PATH_WILDCARD + PATH_GET_STRING, GET_STRING);
        mUriMatcher.addURI(AUTHORITY, PATH_WILDCARD + PATH_GET_INT, GET_INT);
        mUriMatcher.addURI(AUTHORITY, PATH_WILDCARD + PATH_GET_LONG, GET_LONG);
        mUriMatcher.addURI(AUTHORITY, PATH_WILDCARD + PATH_GET_FLOAT, GET_FLOAT);
        mUriMatcher.addURI(AUTHORITY, PATH_WILDCARD + PATH_GET_BOOLEAN, GET_BOOLEAN);
        mUriMatcher.addURI(AUTHORITY, PATH_WILDCARD + PATH_CONTAINS, CONTAINS);
        mUriMatcher.addURI(AUTHORITY, PATH_WILDCARD + PATH_APPLY, APPLY);
        mUriMatcher.addURI(AUTHORITY, PATH_WILDCARD + PATH_COMMIT, COMMIT);
        mUriMatcher.addURI(AUTHORITY, PATH_WILDCARD + PATH_REGISTER_ON_SHARED_PREFERENCE_CHANGE_LISTENER, REGISTER_ON_SHARED_PREFERENCE_CHANGE_LISTENER);
        mUriMatcher.addURI(AUTHORITY, PATH_WILDCARD + PATH_UNREGISTER_ON_SHARED_PREFERENCE_CHANGE_LISTENER, UNREGISTER_ON_SHARED_PREFERENCE_CHANGE_LISTENER);
        mUriMatcher.addURI(AUTHORITY, PATH_WILDCARD + PATH_GET_STRING_SET, GET_STRING_SET);
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String name = uri.getPathSegments().get(0);
        int mode = Integer.parseInt(selectionArgs[0]);
        String key = selectionArgs[1];
        String defValue = selectionArgs[2];
        Bundle bundle = new Bundle();
        switch (mUriMatcher.match(uri)) {
            case GET_ALL:
                bundle.putSerializable(KEY, (HashMap<String, ?>) getSystemSharedPreferences(name, mode).getAll());
                break;
            case GET_STRING:
                bundle.putString(KEY, getSystemSharedPreferences(name, mode).getString(key, defValue));
                break;
            case GET_INT:
                bundle.putInt(KEY, getSystemSharedPreferences(name, mode).getInt(key, Integer.parseInt(defValue)));
                break;
            case GET_LONG:
                bundle.putLong(KEY, getSystemSharedPreferences(name, mode).getLong(key, Long.parseLong(defValue)));
                break;
            case GET_FLOAT:
                bundle.putFloat(KEY, getSystemSharedPreferences(name, mode).getFloat(key, Float.parseFloat(defValue)));
                break;
            case GET_BOOLEAN:
                bundle.putBoolean(KEY, getSystemSharedPreferences(name, mode).getBoolean(key, Boolean.parseBoolean(defValue)));
                break;
            case CONTAINS:
                bundle.putBoolean(KEY, getSystemSharedPreferences(name, mode).contains(key));
                break;
            case REGISTER_ON_SHARED_PREFERENCE_CHANGE_LISTENER: {
                checkInitListenersCount();
                Integer countInteger = mListenersCount.get(name);
                int count = (countInteger == null ? 0 : countInteger) + 1;
                mListenersCount.put(name, count);
                countInteger = mListenersCount.get(name);
                bundle.putBoolean(KEY, count == (countInteger == null ? 0 : countInteger));
            }
            break;
            case UNREGISTER_ON_SHARED_PREFERENCE_CHANGE_LISTENER: {
                checkInitListenersCount();
                Integer countInteger = mListenersCount.get(name);
                int count = (countInteger == null ? 0 : countInteger) - 1;
                if (count <= 0) {
                    mListenersCount.remove(name);
                    bundle.putBoolean(KEY, !mListenersCount.containsKey(name));
                } else {
                    mListenersCount.put(name, count);
                    countInteger = mListenersCount.get(name);
                    bundle.putBoolean(KEY, count == (countInteger == null ? 0 : countInteger));
                }
            }
            break;
            case GET_STRING_SET: {
                if (Build.VERSION.SDK_INT >= 11) { // Android 3.0
                    Set<String> set = null;
                    if (projection != null) {
                        set = new HashSet<>(Arrays.asList(projection));
                    }
                    bundle.putSerializable(KEY, (HashSet<String>) getSystemSharedPreferences(name, mode).getStringSet(key, set));
                }
            }
            break;
            default:
//                if (!YYDebug.RELEASE_VER) {
//                    throw new IllegalArgumentException("At query, This is Unknown Uri：" + uri + ", AUTHORITY = " + AUTHORITY);
//                }
//                ExceptionReporter.reportException(getContext(), TAG + "_query", null, "At query, This is Unknown Uri：" + uri + ", AUTHORITY = " + AUTHORITY, 3);
        }
        return new BundleCursor(bundle);
    }

    @SuppressWarnings("unchecked")
    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int result = 0;
        String name = uri.getPathSegments().get(0);
        int mode = Integer.parseInt(selectionArgs[0]);
        SharedPreferences preferences = getSystemSharedPreferences(name, mode);
        int match = mUriMatcher.match(uri);
        switch (match) {
            case APPLY:
            case COMMIT:
                boolean hasListeners = mListenersCount != null && mListenersCount.get(name) != null && mListenersCount.get(name) > 0;
                ArrayList<String> keysModified = null;
                Map<String, Object> map = null;
                if (hasListeners) {
                    keysModified = new ArrayList<>();
                    map = (Map<String, Object>) preferences.getAll();
                }
                Editor editor = preferences.edit();
                boolean clear = Boolean.parseBoolean(selectionArgs[1]);
                if (clear) {
                    if (hasListeners && !map.isEmpty()) {
                        for (Map.Entry<String, Object> entry : map.entrySet()) {
                            keysModified.add(entry.getKey());
                        }
                    }
                    editor.clear();
                }
                for (Map.Entry<String, Object> entry : values.valueSet()) {
                    String k = entry.getKey();
                    Object v = entry.getValue();
                    // Android 5.L_preview : "this" is the magic value for a removal mutation. In addition,
                    // setting a value to "null" for a given key is specified to be
                    // equivalent to calling remove on that key.
                    if (v instanceof EditorImpl || v == null) {
                        editor.remove(k);
                        if (hasListeners && map.containsKey(k)) {
                            keysModified.add(k);
                        }
                    } else {
                        if (hasListeners && (!map.containsKey(k) || (map.containsKey(k) && !v.equals(map.get(k))))) {
                            keysModified.add(k);
                        }
                    }

                    if (v instanceof String) {
                        editor.putString(k, (String) v);
                    } else if (v instanceof Set) {
                        editor.putStringSet(k, (Set<String>) v);
                    } else if (v instanceof Integer) {
                        editor.putInt(k, (Integer) v);
                    } else if (v instanceof Long) {
                        editor.putLong(k, (Long) v);
                    } else if (v instanceof Float) {
                        editor.putFloat(k, (Float) v);
                    } else if (v instanceof Boolean) {
                        editor.putBoolean(k, (Boolean) v);
                    } else if(v instanceof Short){
                        editor.remove(k);
                    }
                }
                if (hasListeners && keysModified.isEmpty()) {
                    result = 1;
                } else {
                    switch (match) {
                        case APPLY:
                            editor.apply();
                            result = 1;
                            // Okay to notify the listeners before it's hit disk
                            // because the listeners should always get the same
                            // SharedPreferences instance back, which has the
                            // changes reflected in memory.
                            notifyListeners(name, keysModified);
                            break;
                        case COMMIT:
                            if (editor.commit()) {
                                result = 1;
                                notifyListeners(name, keysModified);
                            }
                            break;
                        default:
                            break;
                    }
                }
                values.clear();
                break;
            default:
//                if (!YYDebug.RELEASE_VER) {
//                    throw new IllegalArgumentException("At update, This is Unknown Uri：" + uri + ", AUTHORITY = " + AUTHORITY);
//                }
//                ExceptionReporter.reportException(getContext(), TAG + "_update", null, "At update, This is Unknown Uri：" + uri + ", AUTHORITY = " + AUTHORITY, 3);
        }
        return result;
    }

    @Override
    public String getType(@NonNull Uri uri) {
//        if (!YYDebug.RELEASE_VER) {
//            throw new UnsupportedOperationException("No external call");
//        }
//        ExceptionReporter.reportException(getContext(), TAG + "_getType", null, "No external call, uri=" + uri, 3);
        return "";
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
//        if (!YYDebug.RELEASE_VER) {
//            throw new UnsupportedOperationException("No external insert");
//        }
//        ExceptionReporter.reportException(getContext(), TAG + "_insert", null, "No external call, uri=" + uri + ", values[" + values + "]", 3);
        return uri;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
//        if (!YYDebug.RELEASE_VER) {
//            throw new UnsupportedOperationException("No external delete");
//        }
//        ExceptionReporter.reportException(getContext(), "MultiprocessSharedPreferences_delete", null, "No external delete", 3);
        return 0;
    }

    private SharedPreferences getSystemSharedPreferences(String name, int mode) {
        return getContext().getSharedPreferences(name, mode);
    }

    private void checkInitListenersCount() {
        if (mListenersCount == null) {
            mListenersCount = new HashMap<String, Integer>();
        }
    }

    private void notifyListeners(String name, ArrayList<String> keysModified) {
        if (keysModified != null && !keysModified.isEmpty()) {
            Intent intent = new Intent();
            intent.setAction(makeAction(name));
            intent.setPackage(getContext().getPackageName());
            intent.putExtra(KEY_NAME, name);
            intent.putExtra(KEY, keysModified);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
        }
    }

    private static final class BundleCursor extends MatrixCursor {
        private Bundle mBundle;

        public BundleCursor(Bundle extras) {
            super(new String[]{}, 0);
            mBundle = extras;
        }

        @Override
        public Bundle getExtras() {
            return mBundle;
        }

        @Override
        public Bundle respond(Bundle extras) {
            mBundle = extras;
            return mBundle;
        }
    }
}