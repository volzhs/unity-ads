//
//  UnityAdsUnityWrapper.m
//  UnityAdsUnity
//
//  Created by Pekka Palmu on 3/8/13.
//  Copyright (c) 2013 Pekka Palmu. All rights reserved.
//

#import "UnityAdsUnityWrapper.h"
#import "AppController.h"

static UnityAdsUnityWrapper *unityAds = NULL;

void UnitySendMessage(const char* obj, const char* method, const char* msg);
void UnityPause(bool pause);

extern "C" {
    NSString* UnityAdsCreateNSString (const char* string) {
        return string ? [NSString stringWithUTF8String: string] : [NSString stringWithUTF8String: ""];
    }
}

@interface UnityAdsUnityWrapper () <UnityAdsDelegate>
    @property (nonatomic, strong) NSString* gameObjectName;
    @property (nonatomic, strong) NSString* gameId;
@end

@implementation UnityAdsUnityWrapper

- (id)initWithGameId:(NSString*)gameId testModeOn:(bool)testMode debugModeOn:(bool)debugMode withGameObjectName:(NSString*)gameObjectName {
    self = [super init];
    
    if (self != nil) {
        self.gameObjectName = gameObjectName;
        self.gameId = gameId;
        NSLog(@"Game object name=%@, gameId=%@", self.gameObjectName, self.gameId);
        [[UnityAds sharedInstance] setDelegate:self];
        [[UnityAds sharedInstance] setDebugMode:YES];
        [[UnityAds sharedInstance] setTestMode:YES];
        [[UnityAds sharedInstance] startWithGameId:gameId andViewController:UnityGetGLViewController()];
    }
    
    return self;
}

- (void)unityAds:(UnityAds *)unityAds completedVideoWithRewardItemKey:(NSString *)rewardItemKey {
    NSLog(@"unityAds");
    UnitySendMessage([self.gameObjectName UTF8String], "onVideoCompleted", [rewardItemKey UTF8String]);
}

- (void)unityAdsWillShow:(UnityAds *)unityAds {
    NSLog(@"unityAdsWillShow");
}

- (void)unityAdsDidShow:(UnityAds *)unityAds {
    NSLog(@"unityAdsDidShow");
    UnitySendMessage([self.gameObjectName UTF8String], "onShow", "");
    UnityPause(true);
}

- (void)unityAdsWillHide:(UnityAds *)unityAds {
    NSLog(@"unityAdsWillHide");
}

- (void)unityAdsDidHide:(UnityAds *)unityAds {
    NSLog(@"unityAdsDidHide");
    UnityPause(false);
    UnitySendMessage([self.gameObjectName UTF8String], "onHide", "");
}

- (void)unityAdsWillLeaveApplication:(UnityAds *)unityAds {
    NSLog(@"unityAdsWillLeaveApplication");
}

- (void)unityAdsVideoStarted:(UnityAds *)unityAds {
    NSLog(@"unityAdsVideoStarted");
    UnitySendMessage([self.gameObjectName UTF8String], "onVideoStarted", "");
}

- (void)unityAdsFetchCompleted:(UnityAds *)unityAds {
    NSLog(@"unityAdsFetchCompleted");
    UnitySendMessage([self.gameObjectName UTF8String], "onFetchCompleted", "");
}

- (void)unityAdsFetchFailed:(UnityAds *)unityAds {
    NSLog(@"unityAdsFetchFailed");
    UnitySendMessage([self.gameObjectName UTF8String], "onFetchFailed", "");
}


extern "C" {
    void init (const char *gameId, bool testMode, bool debugMode, const char *gameObjectName) {
        NSLog(@"init");
        if (unityAds == NULL) {
            unityAds = [[UnityAdsUnityWrapper alloc] initWithGameId:UnityAdsCreateNSString(gameId) testModeOn:testMode debugModeOn:debugMode withGameObjectName:UnityAdsCreateNSString(gameObjectName)];
            NSLog(@"gameId=%@, gameObjectName=%@", UnityAdsCreateNSString(gameId), UnityAdsCreateNSString(gameObjectName));
        }
    }
    
	bool show (bool openAnimated, bool noOfferscreen, const char *gamerSID) {
        NSLog(@"show");
        return false;
    }
	
	void hide () {
        NSLog(@"show");
    }
	
	bool isSupported () {
        NSLog(@"hide");
        return false;
    }
	
	char* getSDKVersion () {
        NSLog(@"getSDKVersion");
        return "moi";
    }
    
	bool canShowAds () {
        NSLog(@"canShowAds");
        return [[UnityAds sharedInstance] canShow];
    }
    
	bool canShow () {
        NSLog(@"canShow");
        return [[UnityAds sharedInstance] canShow];
    }
	
	void stopAll () {
        NSLog(@"stopAll");
    }
    
	bool hasMultipleRewardItems () {
        NSLog(@"hasMultipleRewardItems");
        return false;
    }
	
	char* getRewardItemKeys () {
        NSLog(@"getRewardItemKeys");
        return "moi,moi,moi";
    }
    
	char* getDefaultRewardItemKey () {
        NSLog(@"getDefaultRewardItemKey");
        return "ship";
    }
	
	char* getCurrentRewardItemKey () {
        NSLog(@"getCurrentRewardItemKey");
        return "ship";
    }
    
	bool setRewardItemKey (const char *rewardItemKey) {
        NSLog(@"setRewardItemKey");
        return false;
    }
	
	void setDefaultRewardItemAsRewardItem () {
        NSLog(@"setDefaultRewardItemAsRewardItem");
    }
    
	char* getRewardItemDetailsWithKey (const char *rewardItemKey) {
        NSLog(@"getRewardItemDetailsWithKey");
        return "moi,moi,moi";
    }
}

@end

