package com.ggec.voice.assistservice.data;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.ggec.voice.assistservice.BgProcessIntentService;
import com.ggec.voice.assistservice.MyApplication;

/**
 * Created by ggec on 2017/3/29.
 */

public class BackGroundProcessServiceControlCommand implements Parcelable {
    public final static int BEGIN_ALARM = 4;
    public final static int STOP_ALARM = 5;
    public final static int MUTE_CHANGE = 6; // mute
    public final static int VOLUME_CHANGE = 7; // volume
//    public final static int R

    public int type; // 控制类型， 1 启动 2 停止 3 取消
    public long waitMicDelayMillSecond;
    public Bundle bundle = new Bundle();

    public BackGroundProcessServiceControlCommand(int type){
        this.type = type;
    }

    public static Intent createMuteChangeIntent(boolean mute){
        Intent intent = new Intent(MyApplication.getContext(), BgProcessIntentService.class);
        BackGroundProcessServiceControlCommand controlCommand = new BackGroundProcessServiceControlCommand(MUTE_CHANGE);
        controlCommand.bundle.putBoolean("mute", mute);
        intent.putExtra(BgProcessIntentService.EXTRA_CMD, controlCommand);
        return intent;
    }

    public static Intent createVolumeChangeIntent(long volume){
        Intent intent = new Intent(MyApplication.getContext(), BgProcessIntentService.class);
        BackGroundProcessServiceControlCommand controlCommand = new BackGroundProcessServiceControlCommand(VOLUME_CHANGE);
        controlCommand.bundle.putLong("volume", volume);
        intent.putExtra(BgProcessIntentService.EXTRA_CMD, controlCommand);
        return intent;
    }


    protected BackGroundProcessServiceControlCommand(Parcel in) {
        type = in.readInt();
        waitMicDelayMillSecond = in.readLong();
        bundle = in.readBundle();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type);
        dest.writeLong(waitMicDelayMillSecond);
        dest.writeBundle(bundle);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BackGroundProcessServiceControlCommand> CREATOR = new Creator<BackGroundProcessServiceControlCommand>() {
        @Override
        public BackGroundProcessServiceControlCommand createFromParcel(Parcel in) {
            return new BackGroundProcessServiceControlCommand(in);
        }

        @Override
        public BackGroundProcessServiceControlCommand[] newArray(int size) {
            return new BackGroundProcessServiceControlCommand[size];
        }
    };
}
