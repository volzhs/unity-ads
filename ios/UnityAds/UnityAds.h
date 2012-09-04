//
//  UnityAds.h
//  UnityAds
//
//  Created by Johan Halin on 9/4/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>

//
//  All delegate methods and public methods in this header are based on the tentative iOS specification document,
//  and will probably change during development.
//

@class UnityAds;

@protocol UnityAdsDelegate <NSObject>

@optional
- (void)unityAdsWillShow:(UnityAds *)unityAds;
- (void)unityAdsWillHide:(UnityAds *)unityAds;
- (void)unityAdsVideoStarted:(UnityAds *)unityAds;
- (void)unityAdsVideoCompleted:(UnityAds *)unityAds;
- (void)unityAdsFetchCompleted:(UnityAds *)unityAds;

@end

@interface UnityAds : NSObject

@property (nonatomic, assign) id<UnityAdsDelegate> delegate;

+ (id)sharedInstance;

- (void)startWithGameId:(NSString *)gameId;
- (BOOL)show;
- (BOOL)hasCampaigns;
- (void)stopAll;

@end
