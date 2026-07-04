package com.campus.lostfound.common.exception;

import com.campus.lostfound.common.result.ResultCode;

/**
 * 业务异常
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Integer code;
    private final String msg;

    public BizException(Integer code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public BizException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.code = resultCode.getCode();
        this.msg = resultCode.getMsg();
    }

    public BizException(ResultCode resultCode, String msg) {
        super(msg);
        this.code = resultCode.getCode();
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
