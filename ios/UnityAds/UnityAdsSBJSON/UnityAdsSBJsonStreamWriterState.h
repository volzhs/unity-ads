/*
 Copyright (c) 2010, Stig Brautaset.
 All rights reserved.
 
 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are
 met:
 
   Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
  
   Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
 
   Neither the name of the the author nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.
 
 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#import <Foundation/Foundation.h>

@class UnityAdsSBJsonStreamWriter;

@interface UnityAdsSBJsonStreamWriterState : NSObject
+ (id)sharedInstance;
- (BOOL)isInvalidState:(UnityAdsSBJsonStreamWriter*)writer;
- (void)appendSeparator:(UnityAdsSBJsonStreamWriter*)writer;
- (BOOL)expectingKey:(UnityAdsSBJsonStreamWriter*)writer;
- (void)transitionState:(UnityAdsSBJsonStreamWriter*)writer;
- (void)appendWhitespace:(UnityAdsSBJsonStreamWriter*)writer;
@end

@interface UnityAdsSBJsonStreamWriterStateObjectStart : UnityAdsSBJsonStreamWriterState
@end

@interface UnityAdsSBJsonStreamWriterStateObjectKey : UnityAdsSBJsonStreamWriterStateObjectStart
@end

@interface UnityAdsSBJsonStreamWriterStateObjectValue : UnityAdsSBJsonStreamWriterState
@end

@interface UnityAdsSBJsonStreamWriterStateArrayStart : UnityAdsSBJsonStreamWriterState
@end

@interface UnityAdsSBJsonStreamWriterStateArrayValue : UnityAdsSBJsonStreamWriterState
@end

@interface UnityAdsSBJsonStreamWriterStateStart : UnityAdsSBJsonStreamWriterState
@end

@interface UnityAdsSBJsonStreamWriterStateComplete : UnityAdsSBJsonStreamWriterState
@end

@interface UnityAdsSBJsonStreamWriterStateError : UnityAdsSBJsonStreamWriterState
@end

