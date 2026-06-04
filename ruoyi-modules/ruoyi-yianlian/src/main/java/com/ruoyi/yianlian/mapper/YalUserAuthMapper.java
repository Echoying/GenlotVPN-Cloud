package com.ruoyi.yianlian.mapper;

import com.ruoyi.yianlian.domain.YalUserAuth;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户授权Mapper接口
 *
 * @author ruoyi
 */
public interface YalUserAuthMapper
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
    public List<YalUserAuth> selectYalUserAuthByUserId(Long userId);

    /**
     * 根据用户ID和线路查询授权列表
     */
    public List<YalUserAuth> selectYalUserAuthByUserIdAndLineId(@Param("userId") Long userId, @Param("lineId") String lineId);

    /**
     * 新增用户授权
     *
     * @param yalUserAuth 用户授权
     * @return 结果
     */
    public int insertYalUserAuth(YalUserAuth yalUserAuth);

    /**
     * 批量插入用户授权
     *
     * @param list 用户授权列表
     * @return 结果
     */
    public int batchInsertYalUserAuth(@Param("list") List<YalUserAuth> list);

    /**
     * 修改用户授权
     *
     * @param yalUserAuth 用户授权
     * @return 结果
     */
    public int updateYalUserAuth(YalUserAuth yalUserAuth);

    /**
     * 删除用户授权
     *
     * @param id 用户授权主键
     * @return 结果
     */
    public int deleteYalUserAuthById(Long id);

    /**
     * 批量删除用户授权
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteYalUserAuthByIds(Long[] ids);

    /**
   * 根据用户ID删除授权
     *
     * @param userId 用户ID
     * @return 结果
     */
    public int deleteYalUserAuthByUserId(Long userId);

    /**
     * 根据用户ID和线路删除授权
     */
    public int deleteYalUserAuthByUserIdAndLineId(@Param("userId") Long userId, @Param("lineId") String lineId);
}
