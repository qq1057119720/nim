package com.cgm.nim;

import android.content.Context;
import android.util.Log;

import com.netease.nim.uikit.impl.NimUIKitImpl;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.auth.AuthService;

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
        } else {
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

}
