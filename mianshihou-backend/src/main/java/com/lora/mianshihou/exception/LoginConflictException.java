package com.lora.mianshihou.exception;


/**
 * 登录冲突异常
 */
public class LoginConflictException extends RuntimeException {

    private final int code = 40110;

    public LoginConflictException(String message) {
        super(message);
    }

    public int getCode() {
        return code;
    }
}