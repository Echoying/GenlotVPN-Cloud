package com.ruoyi.yianlian.client.dto;

import com.ruoyi.yianlian.client.YiAnLianBase.YiAnLianRequest;
import com.ruoyi.yianlian.client.dto.vo.YiAnLianUserVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 创建人员请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("创建人员请求")
public class YiAnLianUserCreateRequest extends YiAnLianRequest
{
    @ApiModelProperty(value = "账号", required = true)
    private String username;

    @ApiModelProperty(value = "姓名", required = true)
    private String name;

    @ApiModelProperty(value = "明文密码")
    private String password;

    @ApiModelProperty(value = "手机号")
    private String mobile;

    @ApiModelProperty(value = "邮箱")
    private String email;

    @ApiModelProperty(value = "组织id", required = true)
    private List<String> groups;

    @ApiModelProperty(value = "描述")
    private String description;

    @ApiModelProperty(value = "性别 0女 1男")
    private String gender;

    @ApiModelProperty(value = "生日")
    private String birthDay;

    @ApiModelProperty(value = "身份证号")
    private String idCard;

    public YiAnLianUserVO toUserVO()
    {
        YiAnLianUserVO userVO = new YiAnLianUserVO();
        userVO.setUsername(this.username);
        userVO.setName(this.name);
        userVO.setPassword(this.password);
        userVO.setMobile(this.mobile);
        userVO.setEmail(this.email);
        userVO.setGroups(this.groups);
        userVO.setDescription(this.description);
        userVO.setGender(this.gender);
        userVO.setBirthDay(this.birthDay);
        userVO.setIdCard(this.idCard);
        return userVO;
    }
}
