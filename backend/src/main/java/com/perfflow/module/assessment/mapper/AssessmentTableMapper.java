package com.perfflow.module.assessment.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.perfflow.module.assessment.entity.AssessmentTable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
// 个人考核主表 Mapper。
@Mapper
public interface AssessmentTableMapper extends BaseMapper<AssessmentTable> {

    @Select("SELECT id FROM assessment_table WHERE period_id = #{periodId} AND dept_id = #{deptId}")
    List<Long> listIdsByDept(@Param("periodId") Long periodId, @Param("deptId") Long deptId);
    @Select("SELECT id FROM assessment_table WHERE period_id = #{periodId} AND user_id = #{userId}")
    List<Long> listIdsByUser(@Param("periodId") Long periodId, @Param("userId") Long userId);
}
