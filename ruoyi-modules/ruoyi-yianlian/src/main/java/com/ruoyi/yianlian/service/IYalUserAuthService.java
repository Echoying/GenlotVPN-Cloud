package com.ruoyi.yianlian.service;

import com.ruoyi.yianlian.domain.YalUserAuth;
import com.ruoyi.yianlian.domain.vo.ServiceTreeSelect;

import java.util.List;

/**
 * 用户授权Service接口
 *
 * @author ruoyi
 */
public interface IYalUserAuthService
{
    /**
     * 查询用户授权
     *
     * @param id 用户授权主键
     * @return 用户授权
     */
    public YalUserAuth selectYalUserAuthById(Long id);

    /**
     * 查询用户授权列表
     *
     * @param yalUserAuth 用户授权
     * @return 用户授权集合
     */
    public List<YalUserAuth> selectYalUserAuthList(YalUserAuth yalUserAuth);

    /**
     * 根据用户ID查询授权列表
     *
     * @param userId 用户ID
     * @return 用户授权集合
     */
    public List<YalUserAuth> selectByUserId(Long userId);

    /**
     * 新增用户授权
     *
     * @param yalUserAuth 用户授权
     * @return 结果
     */
    public int insertYalUserAuth(YalUserAuth yalUserAuth);

    /**
     * 修改用户授权
     *
     * @param yalUserAuth 用户授权
     * @return 结果
     */
    public int updateYalUserAuth(YalUserAuth yalUserAuth);

    /**
     * 批量删除用户授权
     *
     * @param ids 需要删除的用户授权主键集合
   * @return 结果
     */
    public int deleteYalUserAuthByIds(Long[] ids);

    /**
     * 删除用户授权信息
     *
     * @param id 用户授权主键
     * @return 结果
     */
    public int deleteYalUserAuthById(Long id);

    /**
     * 批量保存用户授权（先删后插），并同步给易安联
     * @param userId   用户ID
   * @param authList 授权列表
     * @return 结果
     */
    public int batchSaveUserAuth(Long userId, List<YalUserAuth> authList);

    /**
     * 构建应用服务树
     *
     * @param appId 线路appId
     * @return 树结构
     */
    public List<ServiceTreeSelect> buildServiceTree(String appId);
}
