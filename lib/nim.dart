import 'dart:async';

import 'package:flutter/services.dart';

class Nim {

  static Nim _instance;

  static Nim get instance => _getInstance();

  factory Nim() => _getInstance();

  static Nim _getInstance() {
    if (_instance == null) {
      _instance = Nim._internal();
    }
    return _instance;
  }

  Nim._internal() {}

  static const MethodChannel _channel =
  const MethodChannel('nim');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
  ///登录
  Future<Map> initImLogin(String accid,String token) async {
    Map params = <String, dynamic>{
      "accid": accid,
      "token": token,
    };
    return await _channel.invokeMethod('initIMLogin', params);
  }
  ///打开单聊页面
  Future<Map> startChat(String accid) async {
    Map params = <String, dynamic>{
      "accid": accid,
    };
    return await _channel.invokeMethod('startChat', params);
  }
  ///登出
  Future<Map> loginOut() async {
    Map params = <String, dynamic>{
    };
    return await _channel.invokeMethod('loginOut', params);
  }
  ///获取会话列表
  Future<Map> queryRecentContacts() async {
    Map params = <String, dynamic>{
    };
    return await _channel.invokeMethod('queryRecentContacts', params);
  }
  ///实时更新会话列表数据
  Future<Map> keepRecent() async {
    Map params = <String, dynamic>{
    };
    return await _channel.invokeMethod('keepRecent', params);
  }

///获取会话未读数

}
