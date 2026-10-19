package com.ylh.picturego.common;

import cn.hutool.crypto.digest.DigestUtil;

public class EncryptUtil {
    public static String getEncryptPassword(String password, String salt) {
        return DigestUtil.md5Hex(password + salt);
    }
}
