#import "NimPlugin.h"
#import "NIMKit.h"
@implementation NimPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    
  FlutterMethodChannel* channel = [FlutterMethodChannel
      methodChannelWithName:@"nim"
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

    else {
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
@end
