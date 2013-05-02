//
//  UnityAdsDialog.h
//  UnityAds
//
//  Created by Pekka Palmu on 4/15/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsUIView.h"
#import "UnityAdsNativeButton.h"

@interface UnityAdsDialog : UnityAdsUIView
  @property (nonatomic, assign) BOOL animating;
  @property (nonatomic, strong) UIView *spinner;
  @property (nonatomic, strong) UILabel *label;
  @property (nonatomic, strong) UnityAdsNativeButton *button;

- (id)initWithFrame:(CGRect)frame useSpinner:(BOOL)createSpinner useLabel:(BOOL)createLabel useButton:(BOOL)createButton;
- (void)createView;
@end
