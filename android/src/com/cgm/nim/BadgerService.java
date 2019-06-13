package com.cgm.nim;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.cgm.nim.badger.BadgeUtil;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.msg.MsgServiceObserve;
import com.netease.nimlib.sdk.msg.model.IMMessage;

import java.util.List;

public class BadgerService extends Service {
    public static String TAG = "com.cgm.nim.BadgerService";
    public static final int NOTIFY_ID = 100;
    Observer<List<IMMessage>> incomingMessageObserver =
            new Observer<List<IMMessage>>() {
                @Override
                public void onEvent(List<IMMessage> messages) {
                    // 处理新收到的消息，为了上传处理方便，SDK 保证参数 messages 全部来自同一个聊天对象。


                    try {
                        final int count = messages.size();
                        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext())
                                .setSmallIcon(getApplicationInfo().icon)
                                .setWhen(System.currentTimeMillis())
                                .setAutoCancel(true);
            //            mBuilder.setContentTitle("test");
            //            mBuilder.setTicker("test");
            //            mBuilder.setContentText("test");
                        //点击set 后，app退到桌面等待3s看效果（有的launcher当app在前台设置未读数量无效）
                        final Notification notification = mBuilder.build();
                        new Handler(getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                BadgeUtil.sendBadgeNotification(notification, NOTIFY_ID, getApplicationContext(), count, count);
                                Toast.makeText(getApplicationContext(), "success", Toast.LENGTH_SHORT).show();
                            }
                        }, 3 * 1000);

                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {

            NIMClient.getService(MsgServiceObserve.class)
                    .observeReceiveMessage(incomingMessageObserver, true);
        }catch (Exception e){

        }
    }
}
