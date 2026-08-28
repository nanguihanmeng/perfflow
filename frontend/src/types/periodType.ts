/**
 * 周期类型元数据（与后端 PeriodType 枚举对齐，对应七张表）
 */
export interface PeriodTypeMeta {
  code: string
  label: string
  line: '个人' | '部门'
  annual: boolean
  importFormat: string
}

/** 七种周期类型 */
export const PERIOD_TYPES: PeriodTypeMeta[] = [
  {
    code: 'QUARTER_GOAL',
    label: '季度目标填报',
    line: '个人',
    annual: false,
    importFormat:
      'A 列：员工登录名（每员工 10 行首行标注）；B 列序号；C 列指标类别(PLAN/OPEN/BONUS)；D 列指标名称；E 列指标分数；F 列工作目标；G 列评分标准。'
  },
  {
    code: 'QUARTER_ASSESS',
    label: '岗位季度绩效考核',
    line: '个人',
    annual: false,
    importFormat:
      'A 列：员工登录名（每员工 10 行首行标注）；B 列序号；C 列指标类别(PLAN/OPEN/BONUS)；D 列指标名称；E 列指标分数；F 列工作目标；G 列评分标准。'
  },
  {
    code: 'ANNUAL_ASSESS',
    label: '个人年度绩效考核',
    line: '个人',
    annual: true,
    importFormat:
      'A 列：员工登录名（每员工 10 行首行标注）；B 列序号；C 列指标类别(PLAN/OPEN/BONUS)；D 列指标名称；E 列指标分数；F 列工作目标；G 列评分标准。'
  },
  {
    code: 'BONUS_APPLY',
    label: '加减分项信息申请',
    line: '个人',
    annual: false,
    importFormat:
      'A 列：员工登录名（每员工 10 行首行标注）；B 列序号；C 列指标类别(建议全部 BONUS)；D 列指标名称；E 列指标分数(正为加分/负为减分)；F 列工作目标；G 列评分标准。'
  },
  {
    code: 'DEPT_YEAR_TASK',
    label: '年度部门任务分解',
    line: '部门',
    annual: true,
    importFormat:
      'A 列：行类型(经营业绩/运营指标/重点工作)；B 列序号；C 列指标名称；D 列目标值；E 列实际完成值；F 列评分标准；G 列得分；H 列权重(0-100)。'
  },
  {
    code: 'DEPT_QUARTER_ADJUST',
    label: '季度部门任务调整',
    line: '部门',
    annual: false,
    importFormat:
      'A 列：行类型(经营业绩/运营指标/重点工作)；B 列序号；C 列指标名称；D 列目标值；E 列实际完成值；F 列评分标准；G 列得分；H 列权重(0-100)。'
  },
  {
    code: 'DEPT_YEAR_SCORE',
    label: '年度部门绩效考核评分',
    line: '部门',
    annual: true,
    importFormat:
      'A 列：行类型(经营业绩/运营指标/重点工作)；B 列序号；C 列指标名称；D 列目标值；E 列实际完成值；F 列评分标准；G 列得分；H 列权重(0-100)。'
  }
]

/** 按编码取类型 */
export const getPeriodType = (code?: string | null): PeriodTypeMeta | undefined =>
  PERIOD_TYPES.find((t) => t.code === code)
