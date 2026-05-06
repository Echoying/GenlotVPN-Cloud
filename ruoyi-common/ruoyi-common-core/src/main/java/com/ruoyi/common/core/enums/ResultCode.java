package com.ruoyi.common.core.enums;

/**
 * <p> 通用返回代码 </p>
 *
 * @author echoying
 * @version 1.0
 * @date 2026/5/06 19:18
 */
public enum ResultCode implements IResultCode {
    /**
     * 失败状态码
     */
    FAILURE(9999, "失败"),

    /**
     * 成功状态码
     */
    SUCCESS(200, "成功"),

    UNKNOW_ERROR(9998, "未知错误！"),

    ;

    private int code;

    private String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return this.code;
    }

    @Override
    public String getMsg() {
        return this.message;
    }

    public String getMsg(Object... msgArgs) {
        return String.format(this.message, msgArgs);
    }

    public ResultCode format(Object... msgArgs) {
        this.message = String.format(this.message, msgArgs);
        return this;
    }
}
