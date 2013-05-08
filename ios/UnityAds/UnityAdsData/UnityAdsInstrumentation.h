//
//  UnityAdsInstrumentation.h
//  UnityAds
//
//  Created by Pekka Palmu on 5/7/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "../UnityAdsCampaign/UnityAdsCampaign.h"

@interface UnityAdsInstrumentation : NSObject

+ (void)gaInstrumentationVideoPlay:(UnityAdsCampaign *)campaign withValuesFrom:(NSDictionary *)additionalValues;
+ (void)gaInstrumentationVideoError:(UnityAdsCampaign *)campaign withValuesFrom:(NSDictionary *)additionalValues;
+ (void)gaInstrumentationVideoAbort:(UnityAdsCampaign *)campaign withValuesFrom:(NSDictionary *)additionalValues;
+ (void)gaInstrumentationVideoCaching:(UnityAdsCampaign *)campaign withValuesFrom:(NSDictionary *)additionalValues;

@end
