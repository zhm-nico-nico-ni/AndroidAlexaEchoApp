package com.ggec.voice.assistservice.data;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.ggec.voice.assistservice.BgProcessIntentService;
import com.ggec.voice.assistservice.MyApplication;

/**
 * Created by ggec on 2017/3/29.
 */

public class BackGroundProcessServiceControlCommand {
    public final static int START_VOICE_RECORD = 1;
    public final static int BEGIN_ALARM = 4;
//    public final static int STOP_ALARM = 5;
    public final static int MUTE_CHANGE = 6; // mute
    public final static int VOLUME_CHANGE = 7; // volume
    public final static int SEND_PING = 8;
//    public final static int SYSTEM_SYNCHRONIZE_STATE = 9; // no needed
    public final static int NETWORK_CONNECT = 10; // 处理网络恢复,连接open down channel
    public final static int USER_INACTIVITY_REPORT = 11; // inactiveTimeInSeconds
    public final static int REFRESH_TOKEN = 12;
    public final static int LOAD_ALARM = 13;

    public int type; // 控制类型， 1 启动 2 停止 3 取消
    public Bundle bundle = new Bundle();

    public BackGroundProcessServiceControlCommand(int type){
        this.type = type;
    }

//    public static Intent createMuteChangeIntent(boolean mute){
//        Intent intent = new Intent(MyApplication.getContext(), BgProcessIntentService.class);
//        BackGroundProcessServiceControlCommand controlCommand = new BackGroundProcessServiceControlCommand();
//        controlCommand.bundle.putBoolean("mute", mute);
//        intent.putExtra(BgProcessIntentService.EXTRA_CMD, MUTE_CHANGE);
//        intent.put
//        return intent;
//    }

    public static Intent createVolumeChangeIntent(long volume){
        Intent intent = new Intent(MyApplication.getContext(), BgProcessIntentService.class);
        BackGroundProcessServiceControlCommand controlCommand = new BackGroundProcessServiceControlCommand(VOLUME_CHANGE);
        controlCommand.bundle.putLong("volume", volume);
        intent.putExtra(BgProcessIntentService.EXTRA_CMD, VOLUME_CHANGE);
        intent.putExtra("cmd_bundle", controlCommand.bundle);
        return intent;
    }

    public static Intent createIntentByType(Context context, int type){
        Intent intent = new Intent(context, BgProcessIntentService.class);
        intent.putExtra(BgProcessIntentService.EXTRA_CMD, type);
        return intent;
    }


}
