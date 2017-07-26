package ai.kitt.snowboy.audio;

import ai.kitt.snowboy.MsgEnum;

/**
 * Created by ggec on 2017/7/26.
 */

public interface ISnowEventCallback {
    void onEvent(MsgEnum message, Object obj);
}
