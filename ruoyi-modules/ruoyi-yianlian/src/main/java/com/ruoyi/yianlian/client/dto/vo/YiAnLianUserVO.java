package com.ruoyi.yianlian.client.dto.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 易安联人员实体
 */
@Data
@ApiModel("人员实体")
public class YiAnLianUserVO implements Serializable
{
    @ApiModelProperty(value = "用户ID", example = "xxx")
    private String id;

    @ApiModelProperty(value = "账号", example = "aaa")
    private String username;

    @ApiModelProperty(value = "姓名", example = "张三")
    private String name;

    @ApiModelProperty(value = "明文密码", example = "123456")
    private String password;

    @ApiModelProperty(value = "手机号", example = "13800000000")
    private String mobile;

    @ApiModelProperty(value = "邮箱", example = "aaa@qq.com")
    private String email;

    @ApiModelProperty(value = "组织ID列表")
    private List<String> groups;

    @ApiModelProperty(value = "父级组织ID列表")
    private List<String> parentIds;

    @ApiModelProperty(value = "绑定组织ID列表")
    private List<String> bindGroupIds;

    @ApiModelProperty(value = "认证类型ID")
    private String authTypeId;

    @ApiModelProperty(value = "认证类型")
  private String authType;

    @ApiModelProperty(value = "统一ID")
    private String unionId;
    @ApiModelProperty(value = "额外信息")
    private String extraInfo;

    @ApiModelProperty(value = "是否加密")
    private Boolean encryptor;

    @ApiModelProperty(value = "岗位名称")
    private String postName;

    @ApiModelProperty(value = "职级名称")
    private String gradeName;

    @ApiModelProperty(value = "工号")
    private String employeeNo;

    @ApiModelProperty(value = "手机区号")
    private String mobileAreaCode;

    @ApiModelProperty(value = "电话")
    private String tel;

    @ApiModelProperty(value = "生效开始时间")
    private String effectStart;

    @ApiModelProperty(value = "生效结束时间")
    private String effectEnd;

    @ApiModelProperty(value = "绑定IP")
    private String bindIp;

    @ApiModelProperty(value = "归属")
    private String attribution;

    @ApiModelProperty(value = "头像路径")
    private String profilePath;

    @ApiModelProperty(value = "背景颜色")
    private String backColour;

    @ApiModelProperty(value = "租户ID")
    private String tenantId;

    @ApiModelProperty(value = "额外字段映射")
    private Object extraFieldMap;

    @ApiModelProperty(value = "是否需要锁定账号")
    private Boolean needLockAccount;

    @ApiModelProperty(value = "类型")
    private Integer type;

    @ApiModelProperty(value = "原因")
    private String reason;

    @ApiModelProperty(value = "描述", example = "")
    private String description;

    @ApiModelProperty(value = "性别 0女 1男", example = "1")
    private String gender;

    @ApiModelProperty(value = "生日", example = "")
    private String birthDay;

    @ApiModelProperty(value = "身份证号", example = "")
    private String idCard;

    @ApiModelProperty(value = "状态", example = "A")
    private String status;
}
