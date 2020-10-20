//
//  NSJPushUtil.h
//  网优助手
//
//  Created by 梁仲太 on 2018/11/23.
//

#import <Foundation/Foundation.h>

@interface NSJPushUtil : NSObject

+(NSInteger)week:(NSString *)format andOffset:(NSInteger)offset;

+(NSDate *)nsFormat:(NSString *)format andOffset:(NSInteger)offset;

+(NSInteger)weekDate:(NSDate*)date;

+(NSInteger)weekFormat:(NSInteger)index;

+(NSString *)format:(NSString *)format andOffset:(NSInteger)offset;

-(void)checkSetting:(NSNotification *)notification;

@end
