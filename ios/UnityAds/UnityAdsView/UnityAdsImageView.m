//
//  UnityAdsImageView.m
//  UnityAds
//
//  Created by Pekka Palmu on 4/17/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsImageView.h"
#import "../UnityAds.h"

@interface UnityAdsImageView ()
  @property (nonatomic, strong) NSURLConnection* connection;
  @property (nonatomic, strong) NSMutableData* data;
@end

@implementation UnityAdsImageView

- (id)initWithFrame:(CGRect)frame {
    self = [super initWithFrame:frame];
    if (self) {
    }
    return self;
}

- (void)loadImageFromURL:(NSURL*)url {
  UALOG_DEBUG(@"");

  self.connection = nil;
  self.data = nil;
  
  NSURLRequest* request = [NSURLRequest requestWithURL:url cachePolicy:NSURLRequestUseProtocolCachePolicy timeoutInterval:60.0];
  self.connection = [[NSURLConnection alloc] initWithRequest:request delegate:self];
  
  
  //TODO error handling, what if connection is nil?
}

- (void)connection:(NSURLConnection *)theConnection didReceiveData:(NSData *)incrementalData {
  UALOG_DEBUG(@"");
  
  if (self.data == nil) {
    self.data = [[NSMutableData alloc] initWithCapacity:2048];
  }
  
  [self.data appendData:incrementalData];
}

- (void)connectionDidFinishLoading:(NSURLConnection*)theConnection {
  UALOG_DEBUG(@"");
  self.connection = nil;
  
  [self setImage:[UIImage imageWithData:self.data]];
  self.center = self.window.center;
  self.contentMode = UIViewContentModeScaleAspectFit;
  self.autoresizingMask = (UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight);
  
  float viewAspect = self.superview.bounds.size.width / self.superview.bounds.size.height;
  float imageAspect = self.image.size.width / self.image.size.height;
  
  float scaleFactor = 1;
  
  if (viewAspect < imageAspect) {
    scaleFactor = self.superview.bounds.size.height / self.image.size.height;
  }
  else {
    scaleFactor = self.superview.bounds.size.width / self.image.size.width;
  }
  
  CGRect rect = CGRectMake(0, 0, self.image.size.width * scaleFactor, self.image.size.height * scaleFactor);
  CGRect newPos = CGRectMake((self.superview.bounds.size.width / 2) - (rect.size.width / 2), (self.superview.bounds.size.height / 2) - (rect.size.height / 2), rect.size.width, rect.size.height);
  
  [self setFrame:newPos];
  [self setNeedsLayout];
  self.data = nil;
}

@end
