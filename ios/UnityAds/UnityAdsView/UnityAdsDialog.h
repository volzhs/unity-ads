//
//  UnityAdsDialog.h
//  UnityAds
//
//  Created by Pekka Palmu on 4/15/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsUIView.h"

@interface UnityAdsDialog : UnityAdsUIView
  @property (nonatomic, assign) BOOL animating;

- (id)initWithFrame:(CGRect)frame useSpinner:(BOOL)createSpinner;
@end
