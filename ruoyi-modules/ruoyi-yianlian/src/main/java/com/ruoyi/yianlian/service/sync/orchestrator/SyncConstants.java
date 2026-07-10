package com.ruoyi.yianlian.service.sync.orchestrator;

/**
 * 统一同步的业务类型 / 操作 / 来源 / 优先级常量
 */
public final class SyncConstants
{
    private SyncConstants()
    {
    }

    // ===== 业务类型 bizType =====
    public static final String BIZ_DEPT = "DEPT";
    public static final String BIZ_ROLE = "ROLE";
    public static final String BIZ_SERVICE = "SERVICE";
    public static final String BIZ_SERVICE_GROUP = "SERVICE_GROUP";
    public static final String BIZ_VPN_USER = "VPN_USER";
    public static final String BIZ_LOCAL_USER = "LOCAL_USER";
    public static final String BIZ_YAL_DEPT_AUTH = "YAL_DEPT_AUTH";
    public static final String BIZ_YAL_ROLE_AUTH = "YAL_ROLE_AUTH";
    public static final String BIZ_YAL_USER_AUTH = "YAL_USER_AUTH";

    // ===== 操作 operation =====
    public static final String OP_CREATE = "CREATE";
    public static final String OP_UPDATE = "UPDATE";
    public static final String OP_DELETE = "DELETE";
    public static final String OP_RESET_PASSWORD = "RESET_PASSWORD";
    public static final String OP_CHANGE_STATUS = "CHANGE_STATUS";
    public static final String OP_ASSIGN_ROLES = "ASSIGN_ROLES";

    // ===== 来源 source =====
    public static final String SOURCE_API = "API";
    public static final String SOURCE_RETRY_JOB = "RETRY_JOB";

    // ===== 优先级（越小越优先，同线路批次内排序 DEPT<ROLE<USER<AUTH）=====
    public static final int PRIORITY_DEPT = 10;
    public static final int PRIORITY_SERVICE_GROUP = 10;
    public static final int PRIORITY_ROLE = 20;
    public static final int PRIORITY_SERVICE = 20;
    public static final int PRIORITY_USER = 30;
    public static final int PRIORITY_AUTH = 40;
    public static final int PRIORITY_DEFAULT = 100;

    /**
     * 按业务类型解析补偿优先级
     */
    public static int priorityOf(String bizType)
    {
        if (bizType == null)
        {
            return PRIORITY_DEFAULT;
        }
        switch (bizType)
        {
            case BIZ_DEPT:
            case BIZ_SERVICE_GROUP:
                return PRIORITY_DEPT;
            case BIZ_ROLE:
            case BIZ_SERVICE:
                return PRIORITY_ROLE;
            case BIZ_VPN_USER:
            case BIZ_LOCAL_USER:
                return PRIORITY_USER;
            case BIZ_YAL_DEPT_AUTH:
            case BIZ_YAL_ROLE_AUTH:
            case BIZ_YAL_USER_AUTH:
                return PRIORITY_AUTH;
            default:
                return PRIORITY_DEFAULT;
        }
    }
}
