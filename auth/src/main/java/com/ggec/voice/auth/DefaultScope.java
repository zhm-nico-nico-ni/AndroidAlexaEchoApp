package com.ggec.voice.auth;

import android.content.Context;
import android.provider.Settings;

import com.amazon.identity.auth.device.api.authorization.Scope;
import com.amazon.identity.auth.device.api.authorization.ScopeFactory;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ggec on 2017/3/31.
 */

public class DefaultScope {
    public static Scope alexaScope(Context context) {
        String PRODUCT_DSN = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        String scope_data = "{\"productID\":\"" + BuildConfig.PRODUCT_ID +
                "\", \"productInstanceAttributes\":{\"deviceSerialNumber\":\"" +
                PRODUCT_DSN + "\"}}";

//     这个是错误示范   String scope_data = "{\"alexa:all\":{\"productID\":\"" + mProductId +
//                "\", \"productInstanceAttributes\":{\"deviceSerialNumber\":\"" +
//                PRODUCT_DSN + "\"}}}";

        try {
            JSONObject j = new JSONObject(scope_data);
            return ScopeFactory.scopeNamed("alexa:all", j);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
