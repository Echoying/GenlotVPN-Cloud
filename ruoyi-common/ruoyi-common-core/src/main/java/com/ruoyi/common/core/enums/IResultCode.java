package com.ruoyi.common.core.enums;

/**
 * <p> add description here </p>
 *
 * @author echoying
 * @version 1.0
 * @date 2023/2/21 19:17
 */
public interface IResultCode {

    /**
     * 返回code
     *
     * @return
     */
    int getCode();

    /**
     * 返回msg
     *
     * @return
     */
    String getMsg();

    static IResultCode create(final IResultCode obj, final Object... params) {
        return new IResultCode() {
            public int getCode() {
                return obj.getCode();
            }

            public String getMsg() {
                return params == null ? obj.getMsg() : String.format(obj.getMsg(), params);
            }
        };
    }
}