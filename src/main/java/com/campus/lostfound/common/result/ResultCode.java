package com.campus.lostfound.common.result;

/**
 * 响应码枚举
 */
public enum ResultCode {

    SUCCESS(200, "success"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    PASSWORD_ERROR(401, "用户名或密码错误"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    USERNAME_EXISTS(409, "用户名已存在"),
    ITEM_NOT_BELONG(403, "无权操作他人物品"),
    ACCOUNT_DISABLED(403, "账号已被禁用"),
    SYSTEM_ERROR(500, "系统繁忙，请稍后重试");

    private final int code;
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
