//
//  UnityAds.m
//  UnityAds
//
//  Created by Johan Halin on 9/4/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAds.h"
#import "UnityAdsiOS4.h"
#import "UnityAdsDevice/UnityAdsDevice.h"

@implementation UnityAds

#pragma mark - Private

- (id)initAdsInstance {
	if ((self = [super init])) {
	}
	
	return self;
}

#pragma mark - Public

static UnityAds *sharedAdsInstance = nil;

- (id)init {
	UALOG_ERROR(@"Use the +sharedInstance singleton instead of initializing this class directly.");
	
	[self doesNotRecognizeSelector:_cmd];
	
	return nil;
}

+ (id)sharedInstance {
	@synchronized(self) {
		if (sharedAdsInstance == nil) {
			// check if we're on at least iOS 4.0
      if ([self respondsToSelector:@selector(autoContentAccessingProxy)]) {
        sharedAdsInstance = [[UnityAdsiOS4 alloc] initAdsInstance];
      }
      else {
				sharedAdsInstance = [[self alloc] initAdsInstance];
      }
		}
	}
	
	return sharedAdsInstance;
}

- (void)setTestMode:(BOOL)testModeEnabled {
	UALOG_DEBUG(@"Disabled on older versions of iOS.");
}

- (void)startWithGameId:(NSString *)gameId {
  UALOG_DEBUG(@"Disabled on older versions of iOS.");
}

- (void)startWithGameId:(NSString *)gameId andViewController:(UIViewController *)viewController {
  UALOG_DEBUG(@"Disabled on older versions of iOS.");
}

- (void)setViewController:(UIViewController *)viewController showImmediatelyInNewController:(BOOL)applyAds {
  UALOG_DEBUG(@"Disabled on older versions of iOS.");
}

- (BOOL)canShow {
	UALOG_DEBUG(@"Disabled on older versions of iOS.");
	return NO;
}

- (BOOL)show {
	UALOG_DEBUG(@"Disabled on older versions of iOS.");
	return NO;
}

- (BOOL)hide {
	UALOG_DEBUG(@"Disabled on older versions of iOS.");
	return NO;
}

- (void)stopAll {
	UALOG_DEBUG(@"Disabled on older versions of iOS.");
}

- (void)trackInstall {
	UALOG_DEBUG(@"Disabled on older versions of iOS.");
}

@end
