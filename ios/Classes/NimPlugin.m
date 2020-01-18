#import "NimPlugin.h"
#import "NIMKit.h"
#import "NIMKitUtil.h"
#import "NIMKitInfoFetchOption.h"
@implementation NimPlugin
FlutterResult flutterRsult;
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {

  FlutterMethodChannel* channel = [FlutterMethodChannel
      methodChannelWithName:@"nim_plugin"
            binaryMessenger:[registrar messenger]];
  NimPlugin* instance = [[NimPlugin alloc] init];
  [registrar addMethodCallDelegate:instance channel:channel];
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
  if ([@"getPlatformVersion" isEqualToString:call.method]) {
    result([@"iOS " stringByAppendingString:[[UIDevice currentDevice] systemVersion]]);
  }else if ([@"initIMLogin" isEqualToString:call.method]) {//登录
      NSMutableDictionary * accDic=call.arguments;
      NSLog(@"%@",accDic);
      [self initIMLogin:accDic[@"accid"] token:accDic[@"token"]];

  }else if ([@"startChat" isEqualToString:call.method]) {//启动单聊页面
      NSMutableDictionary * accDic=call.arguments;
      NSLog(@"%@",accDic);
      [self startChat:accDic[@"accid"]];
  }
  else if ([@"queryRecentContacts" isEqualToString:call.method]) {//获得会话列表
      flutterRsult=result;
      NSMutableDictionary * accDic=call.arguments;
      NSLog(@"查询用户会话");
     NSMutableArray * chatList=  [self getAllRecentSessions];
       NSLog(@"------%@",chatList);

      NSMutableDictionary * resultDic=[NSMutableDictionary dictionary];
      if(chatList){
          [resultDic setObject:@"200" forKey:@"code"];
          [resultDic setObject:[self arrayToJSONString:chatList] forKey:@"msg"];
      }else{
          [resultDic setObject:@"0" forKey:@"code"];
      }
      result(resultDic);
  }else {
    result(FlutterMethodNotImplemented);
  }
}
    /**
     登录
     */
    -(void)initIMLogin:(NSString*)accid token:(NSString*)token{
        [[[NIMSDK sharedSDK] loginManager] login:accid
                                           token:token
                                      completion:^(NSError *error) {
                                          if (error==nil) {
                                              NSLog(@"login success");
                                          }else{
                                              NSLog(@"login error,%@",error);
                                          }
                                      }];
    }

    /**
    启动单聊页面
     */
    -(void)startChat:(NSString*)accid{
        NIMSession *session = [NIMSession session:accid type:NIMSessionTypeP2P];
        NIMSessionViewController *vc = [[NIMSessionViewController alloc] initWithSession:session];

        [[UIApplication sharedApplication].delegate.window.rootViewController  presentViewController:vc animated:YES completion:nil];
//          [_viewController presentViewController:_qrcodeViewController animated:NO completion:nil];

    }
/**
 获得会话列表
 */
-(NSMutableArray * )getAllRecentSessions{
    NSArray<NIMRecentSession *>  *recentSessions = [NIMSDK sharedSDK].conversationManager.allRecentSessions;
    NSLog(@"会话条数%d:",recentSessions.count);
    NSLog(@"%@",recentSessions);
       __weak typeof(self) wself = self;



    NSMutableArray * chatList=[NSMutableArray array];

    NSMutableArray * userList=[NSMutableArray array];

     for(int i=0;i<recentSessions.count;i++){

            NIMRecentSession * recent=recentSessions[i];
         [userList addObject:recent.lastMessage.session.sessionId];
     }

    [[NIMSDK sharedSDK].userManager fetchUserInfos:userList completion:^(NSArray *users, NSError *error) {
           NSLog(@"%@",users);
//        if (users.count) {
//            NTESPersonalCardViewController *vc = [[NTESPersonalCardViewController alloc] initWithUserId:userId];
//            [wself.navigationController pushViewController:vc animated:YES];
//        }else{
//            if (wself) {
//                UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"该用户不存在" message:@"请检查你输入的帐号是否正确" delegate:nil cancelButtonTitle:@"确定" otherButtonTitles:nil, nil];
//                [alert show];
//            }
//        }
    }];

    for(int i=0;i<recentSessions.count;i++){
        NIMRecentSession * recent=recentSessions[i];
        NSMutableDictionary * chatDic=[NSMutableDictionary dictionary];
        [chatDic setObject:recent.lastMessage.session.sessionId forKey:@"contactId"];

        [chatDic setObject:[self messageContent:recent.lastMessage] forKey:@"content"];
        //    cell.nameLabel.text = [self nameForRecentSession:recent];
        [chatDic setObject:[self nameForRecentSession:recent] forKey:@"name"];

        [chatDic setObject:[self timestampDescriptionForRecentSession:recent] forKey:@"time"];

        [chatDic setObject:[self setAvatarBySession:recent.session] forKey:@"avatar"];

//        [chatDic setObject:recent.unreadCount forKey:@"unreadCount"];

        [chatList addObject:chatDic];


    }

    return chatList;


}

//数组转为json字符串
- (NSString *)arrayToJSONString:(NSArray *)array {

    NSError *error = nil;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:array options:NSJSONWritingPrettyPrinted error:&error];
    NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
    NSString *jsonTemp = [jsonString stringByReplacingOccurrencesOfString:@"\n" withString:@""];
    //    NSString *jsonResult = [jsonTemp stringByReplacingOccurrencesOfString:@" " withString:@""];
    return jsonTemp;
}

- (NSString *)nameForRecentSession:(NIMRecentSession *)recent{
    if (recent.session.sessionType == NIMSessionTypeP2P) {
        return [NIMKitUtil showNick:recent.session.sessionId inSession:recent.session];
    }else{
        NIMTeam *team = [[NIMSDK sharedSDK].teamManager teamById:recent.session.sessionId];
        return team.teamName;
    }
}
- (NSString *)timestampDescriptionForRecentSession:(NIMRecentSession *)recent{
    return [NIMKitUtil showTime:recent.lastMessage.timestamp showDetail:NO];
}

- (NSString *)messageContent:(NIMMessage*)lastMessage{
    NSString *text = @"";
    switch (lastMessage.messageType) {
        case NIMMessageTypeText:
            text = lastMessage.text;
            break;
        case NIMMessageTypeAudio:
            text = @"[语音]";
            break;
        case NIMMessageTypeImage:
            text = @"[图片]";
            break;
        case NIMMessageTypeVideo:
            text = @"[视频]";
            break;
        case NIMMessageTypeLocation:
            text = @"[位置]";
            break;
//        case NIMMessageTypeNotification:{
//            return [self notificationMessageContent:lastMessage];
//        }
        case NIMMessageTypeFile:
            text = @"[文件]";
            break;
        case NIMMessageTypeTip:
            text = lastMessage.text;
            break;
//        case NIMMessageTypeRobot:
//            text = [self robotMessageContent:lastMessage];
//            break;
        default:
            text = @"[未知消息]";
    }
    if (lastMessage.session.sessionType == NIMSessionTypeP2P || lastMessage.messageType == NIMMessageTypeTip)
    {
        return text;
    }
    else
    {
        NSString *from = lastMessage.from;
        if (lastMessage.messageType == NIMMessageTypeRobot)
        {
            NIMRobotObject *object = (NIMRobotObject *)lastMessage.messageObject;
            if (object.isFromRobot)
            {
                from = object.robotId;
            }
        }
        NSString *nickName = [NIMKitUtil showNick:from inSession:lastMessage.session];
        return nickName.length ? [nickName stringByAppendingFormat:@" : %@",text] : @"";
    }
}



- (NSString *)setAvatarBySession:(NIMSession *)session
{
    NIMKitInfo *info = nil;
    if (session.sessionType == NIMSessionTypeTeam)
    {
        info = [[NIMKit sharedKit] infoByTeam:session.sessionId option:nil];
    }
    else
    {
        NIMKitInfoFetchOption *option = [[NIMKitInfoFetchOption alloc] init];
        option.session = session;
        info = [[NIMKit sharedKit] infoByUser:session.sessionId option:option];
    }
    NSURL *url = info.avatarUrlString ? [NSURL URLWithString:info.avatarUrlString] : nil;
    return info.avatarUrlString?info.avatarUrlString:@"";
}

- (NSAttributedString *)contentForRecentSession:(NIMRecentSession *)recent{
    NSString *content = [self messageContent:recent.lastMessage];
    return [[NSAttributedString alloc] initWithString:content ?: @""];
}

@end
