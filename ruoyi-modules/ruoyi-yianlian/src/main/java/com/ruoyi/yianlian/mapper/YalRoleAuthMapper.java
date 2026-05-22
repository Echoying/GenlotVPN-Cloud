package com.ruoyi.yianlian.mapper;

import java.util.List;
import com.ruoyi.yianlian.domain.YalRoleAuth;
import org.apache.ibatis.annotations.Param;

/**
 * 角色授权Mapper接口
 *
 * @author ruoyi
 */
public interface YalRoleAuthMapper
{
    public YalRoleAuth selectYalRoleAuthById(Long id);

    public List<YalRoleAuth> selectYalRoleAuthList(YalRoleAuth yalRoleAuth);

    public int insertYalRoleAuth(YalRoleAuth yalRoleAuth);

    public int updateYalRoleAuth(YalRoleAuth yalRoleAuth);

    public int deleteYalRoleAuthById(Long id);

    public int deleteYalRoleAuthByIds(Long[] ids);

    public List<YalRoleAuth> selectYalRoleAuthByRoleId(Long roleId);

    public int deleteYalRoleAuthByRoleId(Long roleId);

    public int batchInsertYalRoleAuth(@Param("list") List<YalRoleAuth> list);
}
