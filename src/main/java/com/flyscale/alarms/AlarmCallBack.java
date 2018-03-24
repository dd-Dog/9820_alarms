package com.flyscale.alarms;

import com.flyscale.alarms.provider.AlarmInstance;

/**
 * Created by Administrator on 2018/3/23 0023.
 */

public interface AlarmCallBack {

    void onExecuted(AlarmInstance instance);
}
