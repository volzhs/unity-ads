//
//  UnityAdsCacheOperation.h
//  testApp
//
//  Created by Sergey D on 3/10/14.
//  Copyright (c) 2014 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "UnityAdsCacheOperation.h"

@interface UnityAdsCacheFileOperation : UnityAdsCacheOperation

@property (nonatomic, strong) NSURL * downloadURL;
@property (nonatomic, copy)   NSString * filePath, * directoryPath;


@end
