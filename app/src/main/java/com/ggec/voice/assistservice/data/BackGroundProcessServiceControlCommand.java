package com.ggec.voice.assistservice.data;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by ggec on 2017/3/29.
 */

public class BackGroundProcessServiceControlCommand implements Parcelable {

    public int type; // 控制类型， 1 启动 2 停止 3 取消

    public  BackGroundProcessServiceControlCommand(int type){
        this.type = type;
    }

    protected BackGroundProcessServiceControlCommand(Parcel in) {
        type = in.readInt();
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type);
    }
}
