package com.perfflow.module.system.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.perfflow.module.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
@Mapper
// 系统用户 Mapper。

public interface SysUserMapper extends BaseMapper<SysUser> {
}
