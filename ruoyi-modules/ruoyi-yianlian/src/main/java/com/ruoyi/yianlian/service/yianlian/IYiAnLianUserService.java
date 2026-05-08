package com.ruoyi.yianlian.service.yianlian;

import com.ruoyi.yianlian.client.dto.vo.YiAnLianUserVO;
import com.ruoyi.yianlian.client.dto.YiAnLianUserListRequest;
import com.ruoyi.yianlian.client.dto.YiAnLianUserListResp;
import com.ruoyi.yianlian.client.dto.YiAnLianUserPasswordResetRequest;

import java.util.List;

/**
 * 易安联人员服务
 */
public interface IYiAnLianUserService
{
    /**
     * 获取易安联人员列表
     *
     * @param request 请求参数
     * @return 人员列表数据
     */
    YiAnLianUserListResp getUserList(YiAnLianUserListRequest request);

    /**
     * 5.3.1 创建人员接口
     *
     * @param users 人员列表
     * @return 是否成功
     */
    Boolean create(String appId, List<YiAnLianUserVO> users);

    /**
     * 5.3.2 更新人员接口
     *
     * @param user 人员信息
     * @return 是否成功
     */
    Boolean update(String appId, YiAnLianUserVO user);

    /**
     * 5.3.3 删除人员接口
     *
     * @param ids 人员ID列表
     * @return 是否成功
     */
    Boolean delete(String appId, List<String> ids);

    /**
     * 5.3.4 用户重置密码接口
     *
     * @param request 重置密码请求
     * @return 是否成功
     */
    Boolean resetPassword(String appId, YiAnLianUserPasswordResetRequest request);
}
