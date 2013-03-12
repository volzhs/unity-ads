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
    
    char* UnityAdsMakeStringCopy (const char* string) {
        if (string == NULL)
            return NULL;
        char* res = (char*)malloc(strlen(string) + 1);
        strcpy(res, string);
        return res;
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
    UnitySendMessage(UnityAdsMakeStringCopy([self.gameObjectName UTF8String]), "onVideoCompleted", [rewardItemKey UTF8String]);
}

- (void)unityAdsWillShow:(UnityAds *)unityAds {
    NSLog(@"unityAdsWillShow");
}

- (void)unityAdsDidShow:(UnityAds *)unityAds {
    NSLog(@"unityAdsDidShow");
    UnitySendMessage(UnityAdsMakeStringCopy([self.gameObjectName UTF8String]), "onShow", "");
    UnityPause(true);
}

- (void)unityAdsWillHide:(UnityAds *)unityAds {
    NSLog(@"unityAdsWillHide");
}

- (void)unityAdsDidHide:(UnityAds *)unityAds {
    NSLog(@"unityAdsDidHide");
    UnityPause(false);
    UnitySendMessage(UnityAdsMakeStringCopy([self.gameObjectName UTF8String]), "onHide", "");
}

- (void)unityAdsWillLeaveApplication:(UnityAds *)unityAds {
    NSLog(@"unityAdsWillLeaveApplication");
}

- (void)unityAdsVideoStarted:(UnityAds *)unityAds {
    NSLog(@"unityAdsVideoStarted");
    UnitySendMessage(UnityAdsMakeStringCopy([self.gameObjectName UTF8String]), "onVideoStarted", "");
}

- (void)unityAdsFetchCompleted:(UnityAds *)unityAds {
    NSLog(@"unityAdsFetchCompleted");
    UnitySendMessage(UnityAdsMakeStringCopy([self.gameObjectName UTF8String]), "onFetchCompleted", "");
}

- (void)unityAdsFetchFailed:(UnityAds *)unityAds {
    NSLog(@"unityAdsFetchFailed");
    UnitySendMessage(UnityAdsMakeStringCopy([self.gameObjectName UTF8String]), "onFetchFailed", "");
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
        NSNumber *noOfferscreenObjectiveC = [NSNumber numberWithBool:noOfferscreen];
        NSNumber *openAnimatedObjectiveC = [NSNumber numberWithBool:openAnimated];
        
        if ([[UnityAds sharedInstance] canShow] && [[UnityAds sharedInstance] canShow]) {
            NSDictionary *props = @{kUnityAdsOptionGamerSIDKey: UnityAdsCreateNSString(gamerSID), kUnityAdsOptionNoOfferscreenKey: noOfferscreenObjectiveC, kUnityAdsOptionOpenAnimatedKey: openAnimatedObjectiveC};
            return [[UnityAds sharedInstance] show:props];
        }
        
        return false;
    }
	
	void hide () {
        NSLog(@"show");
        [[UnityAds sharedInstance] hide];
    }
	
	bool isSupported () {
        NSLog(@"isSupported");
        return [UnityAds isSupported];
    }
	
	const char* getSDKVersion () {
        NSLog(@"getSDKVersion");
        return UnityAdsMakeStringCopy([[UnityAds getSDKVersion] UTF8String]);
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
        [[UnityAds sharedInstance] stopAll];
    }
    
	bool hasMultipleRewardItems () {
        NSLog(@"hasMultipleRewardItems");
        return [[UnityAds sharedInstance] hasMultipleRewardItems];
    }
	
	const char* getRewardItemKeys () {
        NSLog(@"getRewardItemKeys");
        NSArray *keys = [[UnityAds sharedInstance] getRewardItemKeys];
        NSString *keyString = @"";
        
        for (NSString *key in keys) {
            if ([keyString length] <= 0) {
                keyString = [NSString stringWithFormat:@"%@", key];
            }
            else {
                keyString = [NSString stringWithFormat:@"%@;%@", keyString, key];
            }
        }
        
        return UnityAdsMakeStringCopy([keyString UTF8String]);
    }
    
	const char* getDefaultRewardItemKey () {
        NSLog(@"getDefaultRewardItemKey");
        return UnityAdsMakeStringCopy([[[UnityAds sharedInstance] getDefaultRewardItemKey] UTF8String]);
    }
	 
	const char* getCurrentRewardItemKey () {
        NSLog(@"getCurrentRewardItemKey");
        return UnityAdsMakeStringCopy([[[UnityAds sharedInstance] getCurrentRewardItemKey] UTF8String]);
    }
    
	bool setRewardItemKey (const char *rewardItemKey) {
        NSLog(@"setRewardItemKey");
        return [[UnityAds sharedInstance] setRewardItemKey:UnityAdsCreateNSString(rewardItemKey)];
    }
	
	void setDefaultRewardItemAsRewardItem () {
        NSLog(@"setDefaultRewardItemAsRewardItem");
        [[UnityAds sharedInstance] setDefaultRewardItemAsRewardItem];
    }
    
	const char* getRewardItemDetailsWithKey (const char *rewardItemKey) {
        NSLog(@"getRewardItemDetailsWithKey");
        if (rewardItemKey != NULL) {
            NSDictionary *details = [[UnityAds sharedInstance] getRewardItemDetailsWithKey:UnityAdsCreateNSString(rewardItemKey)];
            return UnityAdsMakeStringCopy([[NSString stringWithFormat:@"%@;%@", [details objectForKey:kUnityAdsRewardItemNameKey], [details objectForKey:kUnityAdsRewardItemPictureKey]] UTF8String]);
        }
        
        return UnityAdsMakeStringCopy("");
    }
    
    const char *getRewardItemDetailsKeys () {
        return UnityAdsMakeStringCopy([[NSString stringWithFormat:@"%@;%@", kUnityAdsRewardItemNameKey, kUnityAdsRewardItemPictureKey] UTF8String]);
    }
}

@end