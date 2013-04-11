//
//  UnityAdsViewState.m
//  UnityAds
//
//  Created by Pekka Palmu on 4/4/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsViewState.h"

@implementation UnityAdsViewState


- (id)init {
  self = [super init];
  self.waitingToBeShown = false;
  return self;
}

- (void)enterState:(NSDictionary *)options {
  UALOG_DEBUG(@"");
}

- (void)exitState:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  self.waitingToBeShown = false;
}

- (void)willBeShown {
  UALOG_DEBUG(@"");
  self.waitingToBeShown = true;
}

- (void)wasShown {
  UALOG_DEBUG(@"");
  self.waitingToBeShown = false;
}

- (void)applyOptions:(NSDictionary *)options {
  UALOG_DEBUG(@"");
}

- (UnityAdsViewStateType)getStateType {
  return kUnityAdsViewStateTypeInvalid;
}

@end
