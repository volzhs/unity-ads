//
//  UnityAdsCacheOperation.m
//  testApp
//
//  Created by Sergey D on 3/10/14.
//  Copyright (c) 2014 Unity Technologies. All rights reserved.
//

#import "UnityAdsCacheOperation.h"

@implementation UnityAdsCacheOperation
#pragma mark - NSURLConnectionDelegate

//- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response {
//	NSHTTPURLResponse *httpResponse = nil;
//
//    if ([response isKindOfClass:[NSHTTPURLResponse class]]) {
//        httpResponse = (NSHTTPURLResponse *)response;
//    }
//
//	NSString *resumeStatus = [self.currentDownload objectForKey:kUnityAdsCacheResumeKey];
//	BOOL resumeExpected = [resumeStatus isEqualToString:kUnityAdsCacheDownloadResumeExpected];
//
//	if (resumeExpected && [httpResponse statusCode] == 200) {
//		UALOG_DEBUG(@"Resume expected but got status code 200, restarting download.");
//
//		[self.fileHandle truncateFileAtOffset:0];
//	}
//	else if ([httpResponse statusCode] == 206) {
//		UALOG_DEBUG(@"Resuming download.");
//	}
//
//	NSNumber *contentLength = [[httpResponse allHeaderFields] objectForKey:@"Content-Length"];
//	if (contentLength != nil) {
//		long long size = [contentLength longLongValue];
//		[self _saveCurrentlyDownloadingCampaignToIndexWithFilesize:size];
//		NSDictionary *fsAttributes = [[NSFileManager defaultManager] attributesOfFileSystemForPath:[self _cachePath] error:nil];
//
//        if (fsAttributes != nil) {
//			long long freeSpace = [[fsAttributes objectForKey:NSFileSystemFreeSize] longLongValue];
//
//            if (size > freeSpace) {
//				UALOG_DEBUG(@"Not enough space, canceling download. (%lld needed, %lld free)", size, freeSpace);
//				[connection cancel];
//				[self _downloadFinishedWithFailure:YES];
//			}
//		}
//	}
//}
//
//- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data {
//    [self.fileHandle writeData:data];
//}
//
//- (void)connectionDidFinishLoading:(NSURLConnection *)connection {
//	[self _downloadFinishedWithFailure:NO];
//}
//
//- (void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error {
//	UALOG_DEBUG(@"%@", error);
//	[self _downloadFinishedWithFailure:YES];
//}

@end
