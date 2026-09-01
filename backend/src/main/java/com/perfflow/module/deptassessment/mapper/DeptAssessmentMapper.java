package com.perfflow.module.deptassessment.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.perfflow.module.deptassessment.entity.DeptAssessment;
import org.apache.ibatis.annotations.Mapper;
@Mapper
// 部门考核主表 Mapper。

public interface DeptAssessmentMapper extends BaseMapper<DeptAssessment> {
}
