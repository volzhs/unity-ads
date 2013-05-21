//
//  UnityAdsUnityWrapper.h
//  UnityAdsUnity
//
//  Created by Pekka Palmu on 3/8/13.
//  Copyright (c) 2013 Pekka Palmu. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UnityAds/UnityAds.h>

extern UIViewController* UnityGetGLViewController();

@interface UnityAdsUnityWrapper : NSObject <UnityAdsDelegate> {
}

- (id)initWithGameId:(NSString*)gameId testModeOn:(bool)testMode debugModeOn:(bool)debugMode withGameObjectName:(NSString*)gameObjectName useNativeUI:(bool)useNativeWhenPossible;

@end