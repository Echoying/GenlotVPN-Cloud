package com.ruoyi.yianlian.constant;

import com.ruoyi.common.core.enums.IResultCode;

/**
 * <p> 通用返回代码 </p>
 *
 * @author echoying
 * @version 1.0
 * @date 2026/5/06 19:18
 */
public enum YiAnLianResultCode implements IResultCode {


    /**
     * 成功状态码
     */
    SUCCESS(200, "成功"),
    ;

    private int code;

    private String message;

    YiAnLianResultCode(int code, String message) {
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

    public YiAnLianResultCode format(Object... msgArgs) {
        this.message = String.format(this.message, msgArgs);
        return this;
    }
}
