package com.ruoyi.yianlian.client.YiAnLianBase;

import com.ruoyi.common.core.enums.IResultCode;
import com.ruoyi.common.core.enums.ResultCode;
import com.ruoyi.common.core.exception.yianlian.YiAnLianException;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@ApiModel(
        description = "请求响应对象"
)
@Data
public class YiAnLianResponse<T> implements Serializable {

    int OK_STATUS = ResultCode.SUCCESS.getCode();
    int FAIL_STATUE = ResultCode.FAILURE.getCode();


    private int code;
    private String messages;
    private T data;

    public static YiAnLianResponse SUCCESS = new YiAnLianResponse();

    public static YiAnLianResponse FAILURE = new YiAnLianResponse(ResultCode.FAILURE);

    public YiAnLianResponse() {
        this(ResultCode.SUCCESS, null);
    }

    public YiAnLianResponse(int code){
        this(code, "", null);
    }

    public YiAnLianResponse(T data){
        this(ResultCode.SUCCESS, data);
    }

    public YiAnLianResponse(boolean data){
        if (data) {
            this.code =ResultCode.SUCCESS.getCode();
            this.messages = ResultCode.SUCCESS.getMsg();
        }else {
            this.code =ResultCode.FAILURE.getCode();
            this.messages = ResultCode.FAILURE.getMsg();
        }
    }

    public YiAnLianResponse(int code, String message) {
        this(code, message, null);
    }

    public YiAnLianResponse(IResultCode resultCode){
        this(resultCode, null);
    }

    public YiAnLianResponse(IResultCode resultCode, T data){
        this.code = resultCode.getCode();
        this.messages = resultCode.getMsg();
        this.data = data;
    }


    public YiAnLianResponse(int code, String message, T data) {
        this.code = code;
        this.messages = message;
        this.data = data;
    }

    public static <E> YiAnLianResponse<E> failed(Exception e) {
        if (e instanceof YiAnLianException) {
            YiAnLianException channelException = (YiAnLianException)e;
            return new YiAnLianResponse<>(channelException.getErrorCode(), channelException.getMessage());
        } else {
            return new YiAnLianResponse<>(ResultCode.UNKNOW_ERROR.getCode(), ResultCode.UNKNOW_ERROR.getMsg());
        }
    }

    public static <E> YiAnLianResponse<E> failed(String errorMessage) {
        return new YiAnLianResponse<>(ResultCode.FAILURE.getCode(), errorMessage);
    }

    public static <E> YiAnLianResponse<E> failed(IResultCode resultCode) {
        return new YiAnLianResponse<>(resultCode.getCode(), resultCode.getMsg());
    }

    public static <T> YiAnLianResponse<T> success(T data) {
        return new YiAnLianResponse<>(data);
    }
}
