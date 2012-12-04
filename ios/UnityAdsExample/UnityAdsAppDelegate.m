//
//  UnityAdsAppDelegate.m
//  UnityAdsExample
//
//  Created by bluesun on 7/30/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAdsAppDelegate.h"
#import "UnityAdsViewController.h"
#import <UnityAds/UnityAds.h>

@interface UnityAdsAppDelegate ()
@end

@implementation UnityAdsAppDelegate

@synthesize window = _window;
@synthesize viewController = _viewController;

#pragma mark - UIApplicationDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    self.window = [[UIWindow alloc] initWithFrame:[[UIScreen mainScreen] bounds]];

	if ([self.window respondsToSelector:@selector(setRootViewController:)]) {
        NSString *xibName = @"UnityAdsViewController";
        
        /*
        if (UI_USER_INTERFACE_IDIOM() != UIUserInterfaceIdiomPhone) {
            xibName = @"UnityAds_iPad";
        }*/
        
		self.viewController = [[UnityAdsViewController alloc] initWithNibName:xibName bundle:nil];
		self.window.rootViewController = self.viewController;
	}
	else {
		UIView *view = [[UIView alloc] initWithFrame:[UIScreen mainScreen].applicationFrame];
		view.backgroundColor = [UIColor greenColor];
		[self.window addSubview:view];
	}
	
    [self.window makeKeyAndVisible];
    return YES;
}

- (void)applicationWillResignActive:(UIApplication *)application {
}

- (void)applicationDidEnterBackground:(UIApplication *)application {
    [self.viewController.loadingImage setImage:[UIImage imageNamed:@"unityads_loading"]];
    [self.viewController.buttonView setEnabled:NO];
}

- (void)applicationWillEnterForeground:(UIApplication *)application {
}

- (void)applicationDidBecomeActive:(UIApplication *)application {
}

- (void)applicationWillTerminate:(UIApplication *)application {
}

@end
