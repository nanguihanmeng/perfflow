package com.perfflow.module.assessment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.perfflow.module.assessment.entity.AssessmentTable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AssessmentTableMapper extends BaseMapper<AssessmentTable> {

    /** 数据权限用：查找某部门某周期的所有表（轻量）。 */
    @Select("SELECT id FROM assessment_table WHERE period_id = #{periodId} AND dept_id = #{deptId}")
    List<Long> listIdsByDept(@Param("periodId") Long periodId, @Param("deptId") Long deptId);

    /** 数据权限用：按 user_id 查找某周期所有表（员工本人）。 */
    @Select("SELECT id FROM assessment_table WHERE period_id = #{periodId} AND user_id = #{userId}")
    List<Long> listIdsByUser(@Param("periodId") Long periodId, @Param("userId") Long userId);
}
