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
        [[UnityAds sharedInstance] setDelegate:self];
        [[UnityAds sharedInstance] setDebugMode:debugMode];
        [[UnityAds sharedInstance] setTestMode:testMode];
        [[UnityAds sharedInstance] startWithGameId:gameId andViewController:UnityGetGLViewController()];
    }
    
    return self;
}

- (void)unityAds:(UnityAds *)unityAds completedVideoWithRewardItemKey:(NSString *)rewardItemKey {
    UnitySendMessage(UnityAdsMakeStringCopy([self.gameObjectName UTF8String]), "onVideoCompleted", [rewardItemKey UTF8String]);
}

- (void)unityAdsWillShow:(UnityAds *)unityAds {
}

- (void)unityAdsDidShow:(UnityAds *)unityAds {
    UnitySendMessage(UnityAdsMakeStringCopy([self.gameObjectName UTF8String]), "onShow", "");
    UnityPause(true);
}

- (void)unityAdsWillHide:(UnityAds *)unityAds {
}

- (void)unityAdsDidHide:(UnityAds *)unityAds {
    UnityPause(false);
    UnitySendMessage(UnityAdsMakeStringCopy([self.gameObjectName UTF8String]), "onHide", "");
}

- (void)unityAdsWillLeaveApplication:(UnityAds *)unityAds {
}

- (void)unityAdsVideoStarted:(UnityAds *)unityAds {
    UnitySendMessage(UnityAdsMakeStringCopy([self.gameObjectName UTF8String]), "onVideoStarted", "");
}

- (void)unityAdsFetchCompleted:(UnityAds *)unityAds {
    UnitySendMessage(UnityAdsMakeStringCopy([self.gameObjectName UTF8String]), "onFetchCompleted", "");
}

- (void)unityAdsFetchFailed:(UnityAds *)unityAds {
    UnitySendMessage(UnityAdsMakeStringCopy([self.gameObjectName UTF8String]), "onFetchFailed", "");
}


extern "C" {
    void init (const char *gameId, bool testMode, bool debugMode, const char *gameObjectName) {
        if (unityAds == NULL) {
            unityAds = [[UnityAdsUnityWrapper alloc] initWithGameId:UnityAdsCreateNSString(gameId) testModeOn:testMode debugModeOn:debugMode withGameObjectName:UnityAdsCreateNSString(gameObjectName)];
        }
    }
    
	bool show (bool openAnimated, bool noOfferscreen, const char *gamerSID) {
        NSNumber *noOfferscreenObjectiveC = [NSNumber numberWithBool:noOfferscreen];
        NSNumber *openAnimatedObjectiveC = [NSNumber numberWithBool:openAnimated];
        
        if ([[UnityAds sharedInstance] canShow] && [[UnityAds sharedInstance] canShow]) {
            NSDictionary *props = @{kUnityAdsOptionGamerSIDKey: UnityAdsCreateNSString(gamerSID), kUnityAdsOptionNoOfferscreenKey: noOfferscreenObjectiveC, kUnityAdsOptionOpenAnimatedKey: openAnimatedObjectiveC};
            return [[UnityAds sharedInstance] show:props];
        }
        
        return false;
    }
	
	void hide () {
        [[UnityAds sharedInstance] hide];
    }
	
	bool isSupported () {
        return [UnityAds isSupported];
    }
	
	const char* getSDKVersion () {
        return UnityAdsMakeStringCopy([[UnityAds getSDKVersion] UTF8String]);
    }
    
	bool canShowAds () {
        return [[UnityAds sharedInstance] canShow];
    }
    
	bool canShow () {
        return [[UnityAds sharedInstance] canShow];
    }
	
	void stopAll () {
        [[UnityAds sharedInstance] stopAll];
    }
    
	bool hasMultipleRewardItems () {
        return [[UnityAds sharedInstance] hasMultipleRewardItems];
    }
	
	const char* getRewardItemKeys () {
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
        return UnityAdsMakeStringCopy([[[UnityAds sharedInstance] getDefaultRewardItemKey] UTF8String]);
    }
	 
	const char* getCurrentRewardItemKey () {
        return UnityAdsMakeStringCopy([[[UnityAds sharedInstance] getCurrentRewardItemKey] UTF8String]);
    }
    
	bool setRewardItemKey (const char *rewardItemKey) {
        return [[UnityAds sharedInstance] setRewardItemKey:UnityAdsCreateNSString(rewardItemKey)];
    }
	
	void setDefaultRewardItemAsRewardItem () {
        [[UnityAds sharedInstance] setDefaultRewardItemAsRewardItem];
    }
    
	const char* getRewardItemDetailsWithKey (const char *rewardItemKey) {
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