package com.ruoyi.yianlian.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ruoyi.yianlian.domain.VpnService;
import com.ruoyi.yianlian.domain.VpnServiceGroup;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 应用树结构实体类（应用组+应用合并树）
 *
 * @author ruoyi
 */
public class ServiceTreeSelect implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 节点ID */
    private String id;

    /** 节点名称 */
    private String label;

    /** 节点类型: group-应用组, service-应用 */
    private String type;

    /** 线路ID */
    private String appId;

    /** 节点禁用 */
    private boolean disabled = false;

    /** 子节点 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<ServiceTreeSelect> children;

    public ServiceTreeSelect()
    {
    }

    public ServiceTreeSelect(VpnServiceGroup group, List<ServiceTreeSelect> children)
    {
        this.id = "group_" + group.getId();
        this.label = group.getGroupName();
        this.type = "group";
        this.appId = group.getAppId();
        this.disabled = "1".equals(group.getStatus());
        this.children = children;
    }

    public ServiceTreeSelect(VpnService service)
    {
        this.id = "service_" + service.getId();
        this.label = service.getName();
        this.type = "service";
        this.appId = service.getAppId();
        this.disabled = "1".equals(service.getStatus());
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getAppId()
    {
        return appId;
    }

    public void setAppId(String appId)
    {
        this.appId = appId;
    }

    public boolean isDisabled()
    {
        return disabled;
    }

    public void setDisabled(boolean disabled)
    {
        this.disabled = disabled;
    }

    public List<ServiceTreeSelect> getChildren()
    {
        return children;
    }

    public void setChildren(List<ServiceTreeSelect> children)
    {
        this.children = children;
    }
}
