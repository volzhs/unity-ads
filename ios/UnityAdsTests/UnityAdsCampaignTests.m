//
//  UnityAdsCampaignTests.m
//  UnityAds
//
//  Created by Sergey D on 4/30/14.
//  Copyright (c) 2014 Unity Technologies. All rights reserved.
//

#import <SenTestingKit/SenTestingKit.h>
#import "UnityAdsCampaign.h"
#import "UnityAdsConstants.h"
#import "UnityAds.h"

@interface UnityAdsCampaignTests : SenTestCase

@end

@implementation UnityAdsCampaignTests

- (void)setUp
{
  [[UnityAds sharedInstance] setDebugMode:YES];
    [super setUp];
    // Put setup code here. This method is called before the invocation of each test method in the class.
}

- (void)tearDown
{
    // Put teardown code here. This method is called after the invocation of each test method in the class.
    [super tearDown];
}

- (void)testCampaignInit {
  NSError *error = nil;
  NSStringEncoding encoding = NSStringEncodingConversionAllowLossy;
  NSString *pathToResource = [[NSBundle bundleForClass:[self class]] pathForResource:@"jsonData.txt" ofType:nil];
  NSString *jsonString = [[NSString alloc] initWithContentsOfFile:pathToResource
                                                      usedEncoding:&encoding
                                                             error:&error];

  NSData *jsonData = [jsonString dataUsingEncoding:NSUTF8StringEncoding];
  NSDictionary *jsonDataDictionary = [NSJSONSerialization JSONObjectWithData:jsonData options:0 error:nil];
  NSDictionary *jsonDictionary = [jsonDataDictionary objectForKey:kUnityAdsJsonDataRootKey];
  NSArray *campaignsDataArray = [jsonDictionary objectForKey:kUnityAdsCampaignsKey];
  NSMutableArray *campaigns = [NSMutableArray array];
	
	for (id campaignDictionary in campaignsDataArray) {
		if ([campaignDictionary isKindOfClass:[NSDictionary class]]) {
			UnityAdsCampaign *campaign = [[UnityAdsCampaign alloc] initWithData:campaignDictionary];
      if (campaign.isValidCampaign) {
        [campaigns addObject:campaign];
      }
		}
	}  
}

@end
