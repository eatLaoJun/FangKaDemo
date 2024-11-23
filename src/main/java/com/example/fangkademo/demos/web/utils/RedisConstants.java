package com.example.fangkademo.demos.web.utils;

public class RedisConstants {
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 36000L;
    public static final Long LOGIN_CODE_TTL = 600L;
    public static final String SENDCODE_SENDTIME_KEY ="sms:sendtime:";
    public static final String TWO_LEVERLIMIT_KEY = "limit:twolevel:";
    public static final String ONE_LEVERLIMIT_KEY = "limit:onelevel:";
}
