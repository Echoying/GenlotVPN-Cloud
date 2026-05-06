package com.ruoyi.common.core.exception.yianlian;

import com.ruoyi.common.core.enums.IResultCode;
import com.ruoyi.common.core.enums.ResultCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 异常基类
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class YiAnLianException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private int errorCode;

    private String detailMessage;

    private String errorMessage;

    private transient Object[] args;

    /**
     * 不允许外部调用
     * 开放只用于反序列化时调用
     */
    public YiAnLianException() {

    }

    public YiAnLianException(String errorMessage) {
        super(errorMessage);
        this.errorCode = ResultCode.FAILURE.getCode();
        this.errorMessage = errorMessage;

    }

    public YiAnLianException(int errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /**
     * @param resultCode 异常枚举
     */
    public YiAnLianException(IResultCode resultCode) {
        super(resultCode.getMsg());
        this.errorCode = resultCode.getCode();
        this.errorMessage = resultCode.getMsg();
    }

    /**
     * @param resultCode 异常枚举
     * @param cause      原因
     */
    public YiAnLianException(IResultCode resultCode, Throwable cause) {
        this(resultCode, null, cause);
    }

    /**
     * @param resultCode 错误枚举
     * @param cause      cause
     * @param args       不定参数,对应枚举中的描述的内容,注意缺少的话会报错
     */
    public YiAnLianException(IResultCode resultCode, Throwable cause, Object... args) {

        super(String.format(resultCode.getMsg(), args),
                cause,
                true,
                false);

        this.errorCode = resultCode.getCode();
        this.errorMessage = super.getMessage();
        this.args = args;
    }

    /**
     * @param resultCode    异常枚举
     * @param detailMessage 明细信息
     */
    public YiAnLianException(IResultCode resultCode, String detailMessage) {
        this(resultCode, detailMessage, null);
    }

    /**
     * @param resultCode    异常枚举
     * @param detailMessage 明细信息
     * @param cause         原因
     **/
    public YiAnLianException(IResultCode resultCode, String detailMessage, Throwable cause) {
        super((detailMessage == null) ? resultCode.getMsg() : detailMessage,
                cause,
                true,
                false);

        this.errorCode = resultCode.getCode();
        this.errorMessage = super.getMessage();
        this.detailMessage = detailMessage;
    }
}
