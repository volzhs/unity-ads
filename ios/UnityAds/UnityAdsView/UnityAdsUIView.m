//
//  UnityAdsUIView.m
//  UnityAds
//
//  Created by Pekka Palmu on 4/15/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsUIView.h"

@implementation UnityAdsUIView

- (id)initWithFrame:(CGRect)frame {
    self = [super initWithFrame:frame];
    if (self) {
      self.drawSpinner = false;
      [self setBackgroundColor:[UIColor clearColor]];
    }
    return self;
}

- (void)drawRect:(CGRect)rect {
  UIBezierPath *whitePath = [UIBezierPath bezierPathWithRoundedRect:rect byRoundingCorners:UIRectCornerAllCorners cornerRadii:CGSizeMake(10, 10)];
  [[UIColor whiteColor] setFill];
  [whitePath fill];
  
  UIBezierPath *blackPath = [UIBezierPath bezierPathWithRoundedRect:CGRectMake(rect.origin.x + 3, rect.origin.y + 3, rect.size.width - 6, rect.size.height - 6) byRoundingCorners:UIRectCornerAllCorners cornerRadii:CGSizeMake(8, 8)];
  [[UIColor blackColor] setFill];
  [blackPath fill];
}

@end
