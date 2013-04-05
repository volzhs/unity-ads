//
//  UnityAdsInitializer.h
//  UnityAds
//
//  Created by Pekka Palmu on 4/5/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>

@protocol UnityAdsInitializerDelegate <NSObject>

@required
- (void)initComplete;
- (void)initFailed;
@end

@interface UnityAdsInitializer : NSObject
  @property (nonatomic, assign) id<UnityAdsInitializerDelegate> delegate;
  @property (nonatomic, strong) NSThread *backgroundThread;
  @property (nonatomic, assign) dispatch_queue_t queue;

- (void)init:(NSDictionary *)options;

@end
