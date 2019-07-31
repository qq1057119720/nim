package com.cgm.nim;

import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.uikit.common.util.sys.TimeUtil;
import com.netease.nim.uikit.impl.NimUIKitImpl;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.RequestCallbackWrapper;
import com.netease.nimlib.sdk.auth.AuthService;
import com.netease.nimlib.sdk.msg.MsgService;
import com.netease.nimlib.sdk.msg.MsgServiceObserve;
import com.netease.nimlib.sdk.msg.model.RecentContact;
import com.netease.nimlib.sdk.uinfo.UserService;
import com.netease.nimlib.sdk.uinfo.model.NimUserInfo;
import com.netease.nimlib.sdk.uinfo.model.UserInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.app.FlutterActivity;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * NimPlugin
 */
public class NimPlugin implements MethodCallHandler {
    private static NimPlugin instance;
    private static final String CHANNEL = "nim";
    private FlutterActivity activity;
    private Result pendingResult;
    //  创建观察者对象
    Observer<List<RecentContact>> messageObserver;
    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        if (instance == null) {
            final MethodChannel channel = new MethodChannel(registrar.messenger(), CHANNEL);
            instance = new NimPlugin((FlutterActivity) registrar.activity());
            channel.setMethodCallHandler(instance);
        }
    }


    public NimPlugin(FlutterActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (call.method.equals("getPlatformVersion")) {
            result.success("Android " + android.os.Build.VERSION.RELEASE);
        } else if (call.method.equals("initIMLogin")) {
            if (!(call.arguments instanceof Map)) {
                throw new IllegalArgumentException("Plugin not passing a map as parameter: " + call.arguments);
            } else {
                Map<String, String> userMap = (Map<String, String>) call.arguments;
                Log.v("NimPlugin", userMap.toString());
                NimGlobal.getInstance().initIMLogin(userMap.get("accid"), userMap.get("token"));

            }
        } else if (call.method.equals("startChat")) {
            if (!(call.arguments instanceof Map)) {
                throw new IllegalArgumentException("Plugin not passing a map as parameter: " + call.arguments);
            } else {
                Map<String, String> userMap = (Map<String, String>) call.arguments;
                Log.v("NimPlugin", userMap.toString());
                startChat(activity.getBaseContext(), userMap.get("accid"));
            }
        } else if (call.method.equals("startTeam")) {
            if (!(call.arguments instanceof Map)) {
                throw new IllegalArgumentException("Plugin not passing a map as parameter: " + call.arguments);
            } else {
                Map<String, String> userMap = (Map<String, String>) call.arguments;
                Log.v("NimPlugin", userMap.toString());
                startTeam(activity.getBaseContext(), userMap.get("tic"));
            }
        } else if (call.method.equals("loginOut")) {
            loginOut();
        } else if (call.method.equals("queryRecentContacts")) {
            pendingResult=result;
            queryRecentContacts();
        }   else if (call.method.equals("keepRecent")) {
            pendingResult=result;
        }else {
            result.notImplemented();
        }
    }

    /**
     * 打开单聊页面
     */
    public void startChat(Context context, String accid) {
        NimUIKitImpl.startP2PSession(context, accid);
    }

    /**
     * 打开群聊页面
     */
    public void startTeam(Context context, String tid) {
        NimUIKitImpl.startTeamSession(context, tid);
    }

    /**
     * 登出
     */
    public void loginOut() {
        NIMClient.getService(AuthService.class).logout();
    }

    /**
     * 注册新会话信息监听
     */
    public void queryRecentContacts() {
        messageObserver =
                new Observer<List<RecentContact>>() {
                    @Override
                    public void onEvent(List<RecentContact> messages) {
                        Log.v("nimUserInfos","新的消息");
                        Log.v("nimUserInfos",JSONObject.toJSONString(messages));
                        getRecentContact();
                    }
                };

//  注册/注销观察者
        NIMClient.getService(MsgServiceObserve.class)
                .observeRecentContact(messageObserver, true);
        getRecentContact();

    }

    /**
     * 获取会话列表
     */
    private void getRecentContact(){
        NIMClient.getService(MsgService.class).queryRecentContacts()
                .setCallback(new RequestCallbackWrapper<List<RecentContact>>() {
                    @Override
                    public void onResult(int code, final  List<RecentContact> recents, Throwable e) {
                        // recents参数即为最近联系人列表（最近会话列表）
                        final Map<String,Object> map=new HashMap<>();
                        map.put("code",code);

                        Log.v("nimUserInfos",JSONObject.toJSONString(recents));
                        if (code==200){
                            List<String> accounts=new ArrayList<>();
                            for (int i=0;i<recents.size();i++){
                                accounts.add(recents.get(i).getContactId());
                            }
                            NIMClient.getService(UserService.class).fetchUserInfo(accounts)
                                    .setCallback(new RequestCallback<List<NimUserInfo>>() {
                                        @Override
                                        public void onSuccess(List<NimUserInfo> nimUserInfos) {
                                            Log.v("nimUserInfos",JSONObject.toJSONString(nimUserInfos));
                                            List<Map<String,Object>> conList=new ArrayList<>();
                                            for (int i=0;i<recents.size();i++){
                                                Map<String,Object> userMap=new HashMap<>();
                                                userMap.put("contactId",recents.get(i).getContactId());
                                                userMap.put("content",recents.get(i).getContent());
                                                userMap.put("fromAccount",recents.get(i).getFromAccount());
                                                userMap.put("fromNick",recents.get(i).getFromNick());
                                                userMap.put("msgStatus",recents.get(i).getMsgStatus());
                                                userMap.put("msgType",recents.get(i).getMsgType());
                                                userMap.put("sessionType",recents.get(i).getSessionType());
                                                userMap.put("time",TimeUtil.getTimeShowString(recents.get(i).getTime(), false));
                                                userMap.put("tag",recents.get(i).getTag());
                                                userMap.put("unreadCount",recents.get(i).getUnreadCount());
                                                userMap.put("attachment",recents.get(i).getAttachment());
                                                for (int j=0;j<nimUserInfos.size();j++){
                                                    if (recents.get(i).getContactId().equals(nimUserInfos.get(j).getAccount())){
                                                        userMap.put("name",nimUserInfos.get(j).getName());
                                                        userMap.put("avatar",nimUserInfos.get(j).getAvatar());
                                                        break;
                                                    }
                                                }
                                                conList.add(userMap);
                                            }
                                            map.put("msg", JSONObject.toJSONString(conList));
                                            pendingResult.success(map);
                                        }

                                        @Override
                                        public void onFailed(int i) {

                                        }

                                        @Override
                                        public void onException(Throwable throwable) {

                                        }
                                    });
                        }
                    }
                });
    }

}
