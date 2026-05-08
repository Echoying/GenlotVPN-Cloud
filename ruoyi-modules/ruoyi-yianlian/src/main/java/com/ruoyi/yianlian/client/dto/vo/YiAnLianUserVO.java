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
