//
//  UnityAdsUtils.m
//  UnityAds
//
//  Created by bluesun on 10/23/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAdsUtils.h"
#import "../UnityAds.h"

@implementation UnityAdsUtils

+ (NSString *)escapedStringFromString:(NSString *)string
{
	if (string == nil)
	{
		UALOG_DEBUG(@"Input is nil.");
		return nil;
	}
	
	NSString *escapedString = [string stringByReplacingOccurrencesOfString:@"\"" withString:@"\\\""];
	escapedString = [escapedString stringByReplacingOccurrencesOfString:@"'" withString:@"\'"];
	NSArray *components = [escapedString componentsSeparatedByCharactersInSet:[NSCharacterSet newlineCharacterSet]];
	escapedString = [components componentsJoinedByString:@""];
	
	return escapedString;
}

@end