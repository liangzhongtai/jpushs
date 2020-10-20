//
//  NSJPushUtil.m
//  网优助手
//
//  Created by 梁仲太 on 2018/11/23.
//

#import "NSJPushUtil.h"
#import "JPushPlugin.h"
#import <objc/runtime.h>
#import <AdSupport/AdSupport.h>
#import <UserNotifications/UserNotifications.h>
#import "JPushDefine.h"
#import "JPUSHService.h"

@implementation NSJPushUtil

//获取指定偏离天数的指导格式的星期。
+(NSInteger)week:(NSString *)format andOffset:(NSInteger)offset{
    return [NSJPushUtil weekDate:[NSJPushUtil nsFormat:format andOffset:offset]];
}

+(NSDate *)nsFormat:(NSString *)format andOffset:(NSInteger)offset{
    NSTimeInterval  oneDay = 24*60*60*1;  //1天的长度
    return [[NSDate date] initWithTimeIntervalSinceNow: oneDay*offset ];
}

//日期转星期，p1 :日期 p2:YES 是周六 ，NO 是星期六
+(NSInteger)weekDate:(NSDate*)date{
    NSCalendar *calendar = [[NSCalendar alloc] initWithCalendarIdentifier:NSCalendarIdentifierGregorian];
    NSTimeZone *timeZone = [[NSTimeZone alloc] initWithName:@"Asia/Shanghai"];
    [calendar setTimeZone: timeZone];
    NSCalendarUnit calendarUnit = NSCalendarUnitWeekday;
    NSDateComponents *components = [calendar components:calendarUnit fromDate:date];
    return [NSJPushUtil weekFormat:components.weekday];
}

+(NSInteger)weekFormat:(NSInteger)index{
    if(index==1)return 7;
    if(index==2)return 1;
    if(index==3)return 2;
    if(index==4)return 3;
    if(index==5)return 4;
    if(index==6)return 5;
    return 6;
}

//获取指定偏离天数的指定格式的日期
+(NSString *)format:(NSString *)format andOffset:(NSInteger)offset{
    NSDate *nowDate = [NSDate date];
    NSDate *theDate;
    NSTimeInterval  oneDay = 24*60*60*1;  //1天的长度
    //之后的天数
    theDate = [nowDate initWithTimeIntervalSinceNow: oneDay*offset];
    //之前的天数
    //theDate = [nowDate initWithTimeIntervalSinceNow: -oneDay*dis];
    NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
    [dateFormatter setLocale:[[NSLocale alloc] initWithLocaleIdentifier:@"zh_CN"]];
    //@"MM-dd"
    [dateFormatter setDateFormat:format];
    NSString * currentDateStr = [dateFormatter stringFromDate:theDate];
    return currentDateStr;
}

-(void)checkSetting:(NSNotification *)notification{
    NSDictionary * userInfo = [notification userInfo];
    NSString *content       = [userInfo valueForKey:@"content"];
    NSString *messageID     = [userInfo valueForKey:@"_j_msgid"];
    NSDictionary *extras    = [userInfo valueForKey:@"extras"];
    //NSString *customizeField1 = [extras valueForKey:@"customizeField1"]; //服务端传递的 Extras 附加字段，key 是自己定义的
    
    //读取推送数据
    NSString *Id = [NSString stringWithFormat:@"%d",arc4random_uniform(1000) + 1];
    NSString *title = @"工单派发";
    NSString *subTitle = @"站点勘测";
    NSString *count = @"12";
    NSString *platform = @"ios";
    
    //检查推送设置
    BOOL urgency = NO;
    if([@"紧急通知" isEqualToString:title]){
        urgency = true;
    }
    
    NSUserDefaults *ud = [NSUserDefaults standardUserDefaults];
    NSTimeInterval date = [[NSDate date] timeIntervalSince1970]*1000;
    BOOL usePush = [ud valueForKey:USE_PUSH];
    BOOL sound   = [ud valueForKey:SOUND];
    BOOL shake   = [ud valueForKey:SHAKE];
    NSInteger sWeek = [[ud valueForKey:S_SHOWWEEK] integerValue];
    NSInteger eWeek = [[ud valueForKey:E_SHOWWEEK] integerValue];
    NSInteger sTime = [[ud valueForKey:S_SHOWTIME] integerValue];
    NSInteger eTime = [[ud valueForKey:E_SHOWTIME] integerValue];
    NSInteger frequency = [[ud valueForKey:FREQUENCY] integerValue];
    
    long long lasNoTime = [[ud valueForKey:LAST_NOT_TIME] longLongValue];
    long long nowTime   = [[NSNumber numberWithDouble:date] longLongValue];
    
    NSString *nowDate   = [NSJPushUtil format:@"yyyy-MM-dd HH:mm:ss" andOffset:0];
    NSInteger nowWeek   = [NSJPushUtil week:@"yyyy-MM-dd HH:mm:ss" andOffset:0];
    long delayTime      = 0;
    
    if(sWeek==0&&eWeek==0){
        sWeek = 1;
        eWeek = 7;
    }
    if(sTime==0&&eTime==0){
        sTime = 8;
        eTime = 23;
    }
    if(urgency){
    }else{
        if(usePush)return;
        if(nowTime-lasNoTime<(frequency*24*60*60*1000))return;
        if(nowWeek<sWeek&&nowWeek>eWeek)return;
    }
    
    NSArray *hmDates = [[nowDate componentsSeparatedByString:@" "][1] componentsSeparatedByString:@":"];
    NSInteger hh = [hmDates[0] hasPrefix:@"0"]?[[hmDates[0] substringFromIndex:1] integerValue]:[hmDates[0] integerValue];
    NSInteger mm = [hmDates[1] hasPrefix:@"0"]?[[hmDates[1] substringFromIndex:1] integerValue]:[hmDates[1] integerValue];
    NSInteger ss = [hmDates[2] hasPrefix:@"0"]?[[hmDates[2] substringFromIndex:1] integerValue]:[hmDates[2] integerValue];
    if(hh<sTime){
        delayTime = (sTime-hh)*1000*60*60-mm*1000*60-ss*1000;
    }else if(hh>=eTime){
        delayTime = (24-hh+eTime)*1000*60*60-mm*1000*60-ss*1000;
    }
    delayTime = delayTime/1000;
    JPushNotificationTrigger *trigger = [[JPushNotificationTrigger alloc] init];
    if ([[[UIDevice currentDevice] systemVersion] floatValue] >= 10.0) {
        trigger.timeInterval = delayTime+5;
    }else {
        NSDate *fireDate = [NSDate dateWithTimeIntervalSinceNow:delayTime+5];
        trigger.fireDate = fireDate;
    }
    trigger.repeat = NO;
    //发送本地通知
    JPushNotificationRequest *request = [[JPushNotificationRequest alloc] init];
    request.content = [self generateNotificationCotent:Id andTitle:title andSUbTile:subTitle andContent:content andSound:sound andShake:shake];
    request.content.userInfo = notification.userInfo;
    request.trigger = trigger;
    request.requestIdentifier = Id;
    
    [JPUSHService addNotification:request];
    [ud setValue:[NSNumber numberWithLongLong:nowTime] forKey:LAST_NOT_TIME];
}

- (JPushNotificationContent *)generateNotificationCotent:(NSString *)Id andTitle:(NSString *)title andSUbTile:(NSString *)subTitle andContent:(NSString *)content andSound:(BOOL)sound andShake:(BOOL)shake{
    JPushNotificationContent *noContent = [[JPushNotificationContent alloc] init];
    noContent.title = title;
    noContent.subtitle = subTitle;
    noContent.body = content;
    
    //content.badge = @([self.badgeTF.text integerValue]);
    //content.action = self.actionTF.text;
    noContent.categoryIdentifier = Id;
    noContent.threadIdentifier = Id;
    //  content.userInfo = @{@"extra":@"xxxx"};
    //  UNNotificationAttachment *attachment = [UNNotificationAttachment attachmentWithIdentifier:@"pushTest" URL:[NSURL fileURLWithPath:[[NSBundle mainBundle] pathForResource:@"ios7" ofType:@"png"]] options:nil error:nil];
    //  content.attachments = @[attachment];
    //  content.launchImageName = @"";
    
    if ([[[UIDevice currentDevice] systemVersion] floatValue] >= 10.0) {
        JPushNotificationSound *soundSetting = [[JPushNotificationSound alloc] init];
        if(sound&&shake){
            soundSetting.soundName = @"default";
        }else if(sound){
            soundSetting.soundName = @"default";
        }else if(shake){
            soundSetting.soundName = @"default";
        }else{
            soundSetting.soundName = nil;
        }
        noContent.soundSetting = soundSetting;
    }else {
        noContent.sound = @"default";
    }
    /*if (@available(iOS 12.0, *)) {
     noContent.summaryArgument = self.summaryArgumentTF.text;
     noContent.summaryArgumentCount = [self.summaryArgCountTF.text integerValue];
     }
     if ([[[UIDevice currentDevice] systemVersion] floatValue] >= 10.0) {
     if (self.requestIdentifierTF.text.length == 0) {
     [self showAlertControllerWithTitle:nil message:@"通知identifier不能为空"];
     }
     }*/
    return noContent;
}

@end
