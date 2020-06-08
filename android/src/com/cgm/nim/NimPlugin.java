package com.cgm.nim;

/**
 * @author lgx
 * @date 2020/1/14.
 * description：
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.uikit.common.util.sys.TimeUtil;
import com.netease.nim.uikit.impl.NimUIKitImpl;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.RequestCallbackWrapper;
import com.netease.nimlib.sdk.auth.AuthService;
import com.netease.nimlib.sdk.friend.FriendService;
import com.netease.nimlib.sdk.msg.MessageBuilder;
import com.netease.nimlib.sdk.msg.MsgService;
import com.netease.nimlib.sdk.msg.MsgServiceObserve;
import com.netease.nimlib.sdk.msg.constant.MsgTypeEnum;
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum;
import com.netease.nimlib.sdk.msg.model.IMMessage;
import com.netease.nimlib.sdk.msg.model.QueryDirectionEnum;
import com.netease.nimlib.sdk.msg.model.RecentContact;
import com.netease.nimlib.sdk.uinfo.UserService;
import com.netease.nimlib.sdk.uinfo.constant.UserInfoFieldEnum;
import com.netease.nimlib.sdk.uinfo.model.NimUserInfo;
import com.netease.nimlib.sdk.uinfo.model.UserInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
    private static final String CHANNEL = "nim_plugin";

    public static final String START_CHAT="startChat";
    public static final String START_TEAM="startTeam";
    public static final String QUERY_MESSAGE_LIST_EX="queryMessageListEx";
    public static final String SAVE_IMAGE="save_image";
    public static final String DELETE_CONTACT="delete_contact";
    public static final String KEEP_RECENT ="keepRecent";
    private Context context;
    public static Result pendingResult;

    public static Map<String,Result> resultMap=new HashMap<>();
    //  创建观察者对象
    Observer<List<RecentContact>> messageObserver;
    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        if (instance == null) {
            final MethodChannel channel = new MethodChannel(registrar.messenger(), CHANNEL);
            instance = new NimPlugin(registrar.context());
            channel.setMethodCallHandler(instance);
        }
    }

    public NimPlugin(Context context) {
        this.context = context;
    }

    @Override
    public void onMethodCall(MethodCall call, final Result result) {
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
        } else if (call.method.equals(START_CHAT)) {
            resultMap.put(START_CHAT,result);
            if (!(call.arguments instanceof Map)) {
                throw new IllegalArgumentException("Plugin not passing a map as parameter: " + call.arguments);
            } else {
                Map<String, String> userMap = (Map<String, String>) call.arguments;
                Log.v("NimPlugin", userMap.toString());
                startChat(context, userMap.get("accid"),userMap);
            }
        } else if (call.method.equals("startTeam")) {
            resultMap.put(START_TEAM,result);
            if (!(call.arguments instanceof Map)) {
                throw new IllegalArgumentException("Plugin not passing a map as parameter: " + call.arguments);
            } else {
                Map<String, String> userMap = (Map<String, String>) call.arguments;
                Log.v("NimPlugin", userMap.toString());
                startTeam(context, userMap.get("tic"),userMap);
            }
        } else if (call.method.equals("loginOut")) {
            loginOut();
        } else if (call.method.equals("queryRecentContacts")) {
            pendingResult=result;
            getRecentContact(new RecentContactListener() {
                @Override
                public void getContactSuccess(Map<String, Object> map) {
                    if (result!=null){
                        result.success(map);
                    }
                }
            });
        }
        else if (call.method.equals(KEEP_RECENT)) {
            resultMap.put(KEEP_RECENT,result);
            queryRecentContacts();
        } else if (call.method.equals("updateUserInfo")) {//更新用户资料
            pendingResult=result;
            Map<String, String> userMap = (Map<String, String>) call.arguments;
            updateUserInfo(userMap.get("nickname"),userMap.get("avatar"));

        }else if (call.method.equals("setMessageNotify")) {
            Map<String, String> userMap = (Map<String, String>) call.arguments;
            pendingResult=result;
            setMessageNotify(userMap.get("account"),userMap.get("notify").equals("1")?true:false);
        }
        else if (call.method.equals(QUERY_MESSAGE_LIST_EX)) {
            Map<String, String> userMap = (Map<String, String>) call.arguments;
            resultMap.put(QUERY_MESSAGE_LIST_EX,result);
            queryMessageListEx(userMap.get("account"),Integer.parseInt(userMap.get("type")),Long.parseLong(userMap.get("time")));
        }
        else if (call.method.equals(DELETE_CONTACT)) {
            resultMap.put(DELETE_CONTACT,result);
            //保存图片
            Map<String, String> userMap = (Map<String, String>) call.arguments;
            deleteRecentContact(userMap.get("account"),userMap.get("type").equals("1")?SessionTypeEnum.P2P:SessionTypeEnum.Team);
        }
        else {
            result.notImplemented();
        }
    }

    /**
     * 打开单聊页面
     */
    private void startChat(Context context, String accid,Map<String, String> userMap ) {
        NimUIKitImpl.startP2PSessionByAccount(context, accid,userMap);
    }

    /**
     * 打开群聊页面
     */
    private void startTeam(Context context, String tid,Map<String, String> userMap ) {
        NimUIKitImpl.startTeamSession(context, tid,userMap);
    }
    private void updateUserInfo(String nickname, String avatar) {

        Map<UserInfoFieldEnum, Object> fields = new HashMap<>(2);
        if (!TextUtils.isEmpty(nickname)){
            fields.put(UserInfoFieldEnum.Name,nickname);
        }
        if (!TextUtils.isEmpty(avatar)){
            fields.put(UserInfoFieldEnum.AVATAR,avatar);
        }

        NIMClient.getService(UserService.class).updateUserInfo(fields)
                .setCallback(new RequestCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.v("nimUserInfos","更新 success");
                    }

                    @Override
                    public void onFailed(int i) {

                    }

                    @Override
                    public void onException(Throwable throwable) {

                    }
                });
    }

    /**
     * 登出
     */
    private void loginOut() {
        NIMClient.getService(AuthService.class).logout();
    }

    /**
     * 注册新会话信息监听
     */
    private void queryRecentContacts() {
        messageObserver =
                new Observer<List<RecentContact>>() {
                    @Override
                    public void onEvent(List<RecentContact> messages) {
                        Log.v("nimUserInfos","新的消息");
                        Log.v("nimUserInfos",JSONObject.toJSONString(messages));
                        getRecentContact(new RecentContactListener() {
                            @Override
                            public void getContactSuccess(Map<String, Object> map) {
                                if (resultMap.get(KEEP_RECENT)!=null){
                                    resultMap.get(KEEP_RECENT).success(map);
                                    if (resultMap.get(KEEP_RECENT)!=null){
                                        resultMap.put(KEEP_RECENT
                                                ,null);
                                    }
                                }
                            }
                        });
                    }
                };
//  注册/注销观察者
        NIMClient.getService(MsgServiceObserve.class)
                .observeRecentContact(messageObserver, true);

    }

    /**
     * 获取会话列表
     */
    private void getRecentContact(final RecentContactListener recentContactListener){
        NIMClient.getService(MsgService.class).queryRecentContacts()
                .setCallback(new RequestCallbackWrapper<List<RecentContact>>() {
                    @Override
                    public void onResult(int code, final  List<RecentContact> recents, Throwable e) {
                        // recents参数即为最近联系人列表（最近会话列表）
                        final Map<String,Object> map=new HashMap<>();
                        map.put("code",code+"");
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
                                            recentContactListener.getContactSuccess(map);
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

    /**
     * 设置消息提醒/静音
     * @param account 要设置消息提醒的帐号
     * @param notify 是否提醒该用户发来的消息，false 为静音（不提醒）
     */
    private void setMessageNotify(String account,boolean notify){
        NIMClient.getService(FriendService.class).setMessageNotify(account, notify).setCallback(new RequestCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Map<String,Object> resultMap=new HashMap<>();
                resultMap.put("code","200");
                pendingResult.success(resultMap);
            }
            @Override
            public void onFailed(int i) {
                Map<String,Object> resultMap=new HashMap<>();
                resultMap.put("code","201");
                pendingResult.success(resultMap);
            }
            @Override
            public void onException(Throwable throwable) {

            }
        });
    }
    /**
     * 查询云端历史消息
     */
    public void queryMessageListEx(String anchor,int type,long time){

        final IMMessage imMessage= MessageBuilder.createEmptyMessage(anchor, SessionTypeEnum.typeOfValue(type),time);
        NIMClient.getService(MsgService.class).pullMessageHistoryExType(imMessage,time-7*24*60*60*1000,
                10,QueryDirectionEnum.QUERY_OLD,
                new MsgTypeEnum[]{MsgTypeEnum.text,MsgTypeEnum.image,MsgTypeEnum.audio,MsgTypeEnum.video,MsgTypeEnum.location,MsgTypeEnum.file}).setCallback(new RequestCallbackWrapper<List<IMMessage>>() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onResult(int i, List<IMMessage> imMessages, Throwable throwable) {
                Log.v("userlogin",JSONObject.toJSONString(imMessages));
                Log.v("userlogin",imMessages.size()+"");
                List<Map<String,Object>> mapList=new ArrayList<>();
                for (IMMessage imMessage1:imMessages){
                    Log.v("userlogin",JSONObject.toJSONString(imMessage1));
                    Map<String,Object> map=new HashMap<>(1);
                    map.put("content",imMessage1.getContent());
                    map.put("fromNick",imMessage1.getFromNick());
                    map.put("fromAccount",imMessage1.getFromAccount());
                    map.put("time",imMessage1.getTime());
                    map.put("sessionId",imMessage1.getSessionId());
                    map.put("msgType",imMessage1.getMsgType());
                    map.put("sessionType",imMessage1.getSessionType());
                    if (imMessage1.getMsgType().equals(MsgTypeEnum.image)){
                        map.put("url",imMessage1.getAttachment().toJson(true));
                    }
                    mapList.add(map);
                }
                if (resultMap.get(QUERY_MESSAGE_LIST_EX)!=null){
                    Map<String,Object> map=new HashMap<>();
                    map.put("code","200");
                    map.put("list",JSONObject.toJSONString(imMessages));
                    resultMap.get(QUERY_MESSAGE_LIST_EX).success(map);
                }
            }
        });
    }
    /**
     * 删除最近联系人记录。
     * 调用该接口后，会触发{@link MsgServiceObserve#observeRecentContactDeleted(Observer, boolean)}通知
     * @param account
     * @param sessionType
     */
    private void deleteRecentContact(String account, SessionTypeEnum sessionType){
        NIMClient.getService(MsgService.class).deleteRecentContact2(account, sessionType);
        if (resultMap.get(DELETE_CONTACT)!=null){
            Map<String,Object> map=new HashMap<>();
            map.put("code","200");
            resultMap.get(DELETE_CONTACT).success(map);
        }
    }
    public interface RecentContactListener{
        void getContactSuccess(Map<String,Object> map);
    }
}
