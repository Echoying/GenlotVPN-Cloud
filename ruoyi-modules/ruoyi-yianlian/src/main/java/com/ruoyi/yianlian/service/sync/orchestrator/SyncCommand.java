package com.ruoyi.yianlian.service.sync.orchestrator;

import com.alibaba.fastjson2.JSON;

import java.io.Serializable;

/**
 * 统一同步命令：可序列化写入补偿任务表，供实时执行与 Job 重放共用。
 */
public class SyncCommand implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 业务类型（{@link SyncConstants} BIZ_*） */
    private String bizType;

    /** 操作类型（{@link SyncConstants} OP_*） */
    private String operation;

    /** 线路ID */
    private String appId;

    /** 本地业务主键（CREATE 可为空） */
    private String bizId;

    /** 业务快照 JSON（实体 + 操作特有参数） */
    private String payload;

    /** 来源（API / RETRY_JOB） */
    private String source;

    /** 实时执行时的活对象（不序列化）：优先于 payload 反序列化，便于生成主键回写调用方 */
    private transient Object payloadObject;

    public SyncCommand()
    {
    }

    public SyncCommand(String bizType, String operation, String appId, String bizId, String payload, String source)
    {
        this.bizType = bizType;
        this.operation = operation;
        this.appId = appId;
        this.bizId = bizId;
        this.payload = payload;
        this.source = source;
    }

    /**
     * 构建实时 API 命令，payload 由对象序列化
     */
    public static SyncCommand ofApi(String bizType, String operation, String appId, Object bizId, Object payloadObj)
    {
        String payload = payloadObj == null ? null : JSON.toJSONString(payloadObj);
        String bizIdStr = bizId == null ? null : String.valueOf(bizId);
        SyncCommand command = new SyncCommand(bizType, operation, appId, bizIdStr, payload, SyncConstants.SOURCE_API);
        command.payloadObject = payloadObj;
        return command;
    }

    /**
     * 解析 payload：实时执行优先返回活对象（可回写生成主键），否则反序列化 JSON（Job 重放）
     */
    @SuppressWarnings("unchecked")
    public <T> T parsePayload(Class<T> clazz)
    {
        if (payloadObject != null && clazz.isInstance(payloadObject))
        {
            return (T) payloadObject;
        }
        return payload == null ? null : JSON.parseObject(payload, clazz);
    }

    public Object getPayloadObject()
    {
        return payloadObject;
    }

    public void setPayloadObject(Object payloadObject)
    {
        this.payloadObject = payloadObject;
    }

    public String getBizType()
    {
        return bizType;
    }

    public void setBizType(String bizType)
    {
        this.bizType = bizType;
    }

    public String getOperation()
    {
        return operation;
    }

    public void setOperation(String operation)
    {
        this.operation = operation;
    }

    public String getAppId()
    {
        return appId;
    }

    public void setAppId(String appId)
    {
        this.appId = appId;
    }

    public String getBizId()
    {
        return bizId;
    }

    public void setBizId(String bizId)
    {
        this.bizId = bizId;
    }

    public String getPayload()
    {
        return payload;
    }

    public void setPayload(String payload)
    {
        this.payload = payload;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }
}
