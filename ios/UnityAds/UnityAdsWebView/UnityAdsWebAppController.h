//
//  UnityAdsWebAppController.h
//  UnityAds
//
//  Created by bluesun on 10/23/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

extern NSString * const kUnityAdsWebViewPrefix;
extern NSString * const kUnityAdsWebViewJSInit;
extern NSString * const kUnityAdsWebViewJSChangeView;
extern NSString * const kUnityAdsWebViewAPIPlayVideo;
extern NSString * const kUnityAdsWebViewAPINavigateTo;
extern NSString * const kUnityAdsWebViewAPIInitComplete;
extern NSString * const kUnityAdsWebViewAPIClose;
extern NSString * const kUnityAdsWebViewAPIAppStore;

extern NSString * const kUnityAdsWebViewViewTypeCompleted;
extern NSString * const kUnityAdsWebViewViewTypeStart;

@protocol UnityAdsWebAppControllerDelegate <NSObject>

@required
- (void)webAppReady;
@end

@interface UnityAdsWebAppController : NSObject <UIWebViewDelegate, UIScrollViewDelegate>

@property (nonatomic, strong) UIWebView* webView;
@property (nonatomic, assign) BOOL webViewLoaded;
@property (nonatomic, assign) BOOL webViewInitialized;
@property (nonatomic, assign) id<UnityAdsWebAppControllerDelegate> delegate;

- (void)setWebViewCurrentView:(NSString *)view data:(NSDictionary *)data;
- (void)setup:(CGRect)frame webAppParams:(NSDictionary *)webAppParams;
- (void)openExternalUrl:(NSString *)urlString;
- (void)handleWebEvent:(NSString *)type data:(NSDictionary *)data;

+ (id)sharedInstance;
@end
