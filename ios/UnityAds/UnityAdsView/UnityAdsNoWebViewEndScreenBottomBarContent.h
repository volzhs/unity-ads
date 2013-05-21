//
//  UnityAdsNoWebViewEndScreenBottomBarContent.h
//  UnityAds
//
//  Created by Pekka Palmu on 4/18/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "UnityAdsNativeButton.h"

@interface UnityAdsNoWebViewEndScreenBottomBarContent : UIView
@property (nonatomic, strong) UnityAdsNativeButton *downloadButton;
- (void)updateViewData;
- (void)destroyView;
@end
