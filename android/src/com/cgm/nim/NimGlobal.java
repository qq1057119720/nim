package com.cgm.nim;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.uikit.R;
import com.netease.nim.uikit.api.NimUIKit;
import com.netease.nim.uikit.business.session.viewholder.MsgViewHolderThumbBase;
import com.netease.nim.uikit.impl.preference.UserPreferences;
import com.netease.nimlib.sdk.AbortableFuture;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.SDKOptions;
import com.netease.nimlib.sdk.StatusBarNotificationConfig;
import com.netease.nimlib.sdk.StatusCode;
import com.netease.nimlib.sdk.auth.AuthServiceObserver;
import com.netease.nimlib.sdk.auth.LoginInfo;
import com.netease.nimlib.sdk.msg.MsgServiceObserve;
import com.netease.nimlib.sdk.msg.SystemMessageObserver;
import com.netease.nimlib.sdk.msg.SystemMessageService;
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum;
import com.netease.nimlib.sdk.msg.model.CustomNotification;
import com.netease.nimlib.sdk.msg.model.SystemMessage;
import com.netease.nimlib.sdk.uinfo.UserInfoProvider;
import com.netease.nimlib.sdk.uinfo.model.UserInfo;

import java.util.logging.Logger;

import me.leolin.shortcutbadger.ShortcutBadger;

public class NimGlobal {
    private static volatile NimGlobal mInstance = null;
    private AbortableFuture<LoginInfo> loginRequest;
    private Context context;
    private NimGlobal(Context context) {
        this.context=context;
    }
    public static NimGlobal getInstance(Context context) {
        if (mInstance == null) {
            synchronized (NimGlobal.class) {
                if (mInstance == null) {
                    mInstance = new NimGlobal(context);
                }
            }
        }
        return mInstance;
    }
    public static NimGlobal getInstance() {
        return mInstance;
    }

    public boolean inMainProcess() {
        return context.getPackageName().equals(getProcessName(context));
    }

    //初始化IM
    public void initNim(Class c){
        Log.v("NimPlugin","初始化IM");
        NIMClient.init(context, loginInfo(), getOptions(c));
        if (inMainProcess()){
            Log.v("NimPlugin","初始化initUIKit");
            initUIKit(context);
        }
        Log.v("NimPlugin","初始化IM完成");
    }
    private void initUIKit(Context context) {
        NimUIKit.init(context);
//        NimUIKit.registerTipMsgViewHolder(MsgViewHolderTip.class);
    }

    public static final String getProcessName(Context context) {
        String processName = null;

        // ActivityManager
        ActivityManager am = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE));

        while (true) {
            for (ActivityManager.RunningAppProcessInfo info : am.getRunningAppProcesses()) {
                if (info.pid == android.os.Process.myPid()) {
                    processName = info.processName;

                    break;
                }
            }

            // go home
            if (!TextUtils.isEmpty(processName)) {
                return processName;
            }

            // take a rest and again
            try {
                Thread.sleep(100L);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }





    // 如果已经存在用户登录信息，返回LoginInfo，否则返回null即可
    private LoginInfo loginInfo() {

        return  null;
    }
    //登录IM
    public void initIMLogin( final String account, final String token) {
        Log.v("NimPlugin","开始登陆IM\n"+"account:"+account+"\ntoken:"+token);
        this.loginRequest = NimUIKit.login(new LoginInfo(account, token), new RequestCallback<LoginInfo>() {
            public void onSuccess(LoginInfo param) {
                Log.v("NimPlugin", "login success");
                Intent intent = new Intent(context, BadgerService.class);
                intent.setAction(BadgerService.TAG);
               context.startService(intent);
                // 开启/关闭通知栏消息提醒
                NIMClient.toggleNotification(true);
                NIMClient.getService(SystemMessageObserver.class)
                        .observeReceiveSystemMsg(new Observer<SystemMessage>() {
                            @Override
                            public void onEvent(SystemMessage message) {
                                // 收到系统通知，可以做相应操作
                                Log.v("tongshizhi","---------");
                            }
                        }, true);
                int unread = NIMClient.getService(SystemMessageService.class)
                        .querySystemMessageUnreadCountBlock();
                ShortcutBadger.applyCount(context, unread);
                NIMClient.getService(MsgServiceObserve.class).observeCustomNotification(new Observer<CustomNotification>() {
                    @Override
                    public void onEvent(CustomNotification message) {
                        // 在这里处理自定义通知。
                        Log.v("tongshizhi","---------"+ JSONObject.toJSONString(message));
//                        if (!inMeetingChat){
//                            if(!message.getApnsText().equals("会议聊天消息")){
//                                Intent intent = new Intent();
//                                intent.setClass(getApplicationContext(), SplashActivity.class);
//                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                NotificationUtils.getInstance(getApplicationContext()).showIntentNotification(NotificationUtils.APP_LOGOUT, "提醒",
//                                        message.getApnsText(), "提醒",
//                                        R.mipmap.ic_launcher, null, null, true, -1);
//                            }
//                        }
                    }
                }, true);
//                if (!BaseUtility.isNull(UserCache.getInstance(getApplicationContext()).getTXDate())){
//                    if (UserCache.getInstance(getApplicationContext()).getTXDate().equals("close")){
//                        // 开启/关闭通知栏消息提醒
//                        NIMClient.toggleNotification(true);
//                    }else {
//                        // 开启/关闭通知栏消息提醒
//                        NIMClient.toggleNotification(true);
//                    }
//                }


                NIMClient.getService(AuthServiceObserver.class).observeOnlineStatus(
                        new Observer<StatusCode>() {
                            public void onEvent(StatusCode status) {
                                if (status.wontAutoLogin()) {
                                    // 被踢出、账号被禁用、密码错误等情况，自动登录失败，需要返回到登录界面进行重新登录操作
                                    Log.v("NimPlugin", "被踢掉了");
//                                    UserCache.getInstance(getApplicationContext()).removeUser();
//                                    AppManager.getInstance().finishAllActivity();
//                                    Intent intent=new Intent(getApplicationContext(),MainActivity.class);
//                                    intent.putExtra("login","nologin");
//                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                    getApplication().startActivity(intent);

                                }
                            }
                        }, true);
            }

            public void onFailed(int code) {
                Log.v("NimPlugin", "login error" + code);
               initIMLogin(account,
                        token);
            }

            public void onException(Throwable exception) {
                Log.v("NimPlugin", "login error" + exception.getMessage());
            }
        });
    }
    private SDKOptions getOptions(Class c) {
        SDKOptions options = new SDKOptions();

        // 如果将新消息通知提醒托管给 SDK 完成，需要添加以下配置。否则无需设置。
        StatusBarNotificationConfig config = new StatusBarNotificationConfig();
        config.notificationEntrance =c; // 点击通知栏跳转到该Activity
        config.notificationSmallIconId = R.drawable.icon_full;
        // 呼吸灯配置
        config.ledARGB = Color.GREEN;
        config.ledOnMs = 1000;
        config.ledOffMs = 1500;
        // 通知铃声的uri字符串
        config.notificationSound = "android.resource://com.netease.nim.demo/raw/msg";
        options.statusBarNotificationConfig = config;


        options.sdkStorageRootPath = Environment.getExternalStorageDirectory() + "/" + "com.cgmcomm.cgmstore" + "/nim";
        options.databaseEncryptKey = "NETEASE";
        options.preloadAttach = true;
        options.thumbnailSize = MsgViewHolderThumbBase.getImageMaxEdge();
        options.userInfoProvider = new UserInfoProvider() {
            @Override
            public UserInfo getUserInfo(String s) {
                return null;
            }

            @Override
            public String getDisplayNameForMessageNotifier(String s, String s1, SessionTypeEnum sessionTypeEnum) {
                return null;
            }

            @Override
            public Bitmap getAvatarForMessageNotifier(SessionTypeEnum sessionTypeEnum, String s) {
                return null;
            }
        };
        options.sessionReadAck = true;
        return options;
    }


}
