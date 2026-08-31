# 代码审查报告

**审查时间**: 2026-08-28 16:42:53
**审查目录**: `src\main\java`
**扫描文件**: 121 / 121


## 📊 审查统计

- **总问题数**: 12
- **严重问题**: 1 🔴
- **一般问题**: 0 🟡
- **优化建议**: 11 🔵

### 代码指标

- **总代码行数**: 7816
- **注释行数**: 383
- **注释覆盖率**: 4.9%


## 🏆 华为Java编程规范评分

**平均得分**: 88.14/100
**评级**: 🟡 良好
**Java文件数**: 121

### 各类别评分详情

| 类别 | 满分 | 扣分 | 得分 |
|------|------|------|------|
| 排版规范 | 20 | -310 | -290 |
| 注释规范 | 25 | -923 | -898 |
| 命名规范 | 20 | -113 | -93 |
| 代码编写规范 | 20 | -89 | -69 |
| 性能与可靠性 | 15 | -0 | 15 |

### 📝 修改建议

#### 排版规范

1. 一行只写一条语句
2. 分界符应独占一行，与引用它们的语句左对齐
3. 长语句应在低优先级操作符处换行

#### 注释规范

1. 为公有和保护方法添加完整的JavaDoc注释
2. 为类添加JavaDoc注释，包含功能描述、@author、@since等信息
3. 增加代码注释，确保注释量达到30%以上

#### 命名规范

1. 变量名使用camelCase，首字母小写
2. 常量名使用UPPER_SNAKE_CASE，全大写下划线分隔

#### 代码编写规范

1. 为集合指定泛型类型，如List<String>
2. 使用有意义的常量代替魔法数字

### 📄 各文件详细评分

| 文件 | 得分 | 评级 |
|------|------|------|
| com\perfflow\PerfFlowApplication.java | 85/100 | 🟡 良好 |
| com\perfflow\common\api\Result.java | 92/100 | 🟢 优秀 |
| com\perfflow\common\api\ResultCode.java | 95/100 | 🟢 优秀 |
| com\perfflow\common\constant\RoleConst.java | 99/100 | 🟢 优秀 |
| com\perfflow\common\enums\NotificationType.java | 100/100 | 🟢 优秀 |
| com\perfflow\common\excel\ExcelController.java | 85/100 | 🟡 良好 |
| com\perfflow\common\excel\ExcelImportService.java | 83/100 | 🟡 良好 |
| com\perfflow\common\excel\ExcelTemplateService.java | 92/100 | 🟢 优秀 |
| com\perfflow\common\exception\BizException.java | 93/100 | 🟢 优秀 |
| com\perfflow\common\exception\GlobalExceptionHandler.java | 81/100 | 🟡 良好 |
| com\perfflow\common\util\SecurityContextHelper.java | 88/100 | 🟡 良好 |
| com\perfflow\config\AdminBusinessGuardInterceptor.java | 92/100 | 🟢 优秀 |
| com\perfflow\config\MybatisPlusConfig.java | 85/100 | 🟡 良好 |
| com\perfflow\config\OpenApiConfig.java | 83/100 | 🟡 良好 |
| com\perfflow\config\SecurityConfig.java | 79/100 | 🟠 合格 |
| com\perfflow\config\WebMvcConfig.java | 93/100 | 🟢 优秀 |
| com\perfflow\module\assessment\controller\AssessmentExportController.java | 78/100 | 🟠 合格 |
| com\perfflow\module\assessment\controller\AssessmentFlowController.java | 86/100 | 🟡 良好 |
| com\perfflow\module\assessment\controller\AssessmentRowController.java | 83/100 | 🟡 良好 |
| com\perfflow\module\assessment\controller\AssessmentTableController.java | 79/100 | 🟠 合格 |
| com\perfflow\module\assessment\dto\AssessmentRejectReq.java | 85/100 | 🟡 良好 |
| com\perfflow\module\assessment\dto\AssessmentTableResp.java | 98/100 | 🟢 优秀 |
| com\perfflow\module\assessment\dto\ExtendSuspendReq.java | 85/100 | 🟡 良好 |
| com\perfflow\module\assessment\dto\FlowLogResp.java | 85/100 | 🟡 良好 |
| com\perfflow\module\assessment\dto\LeadScoreReq.java | 85/100 | 🟡 良好 |
| com\perfflow\module\assessment\dto\RowReq.java | 100/100 | 🟢 优秀 |
| com\perfflow\module\assessment\dto\RowResp.java | 96/100 | 🟢 优秀 |
| com\perfflow\module\assessment\entity\AssessmentFlowLog.java | 85/100 | 🟡 良好 |
| com\perfflow\module\assessment\entity\AssessmentRow.java | 93/100 | 🟢 优秀 |
| com\perfflow\module\assessment\entity\AssessmentTable.java | 94/100 | 🟢 优秀 |
| com\perfflow\module\assessment\enums\AssessmentState.java | 100/100 | 🟢 优秀 |
| com\perfflow\module\assessment\enums\RowCategory.java | 100/100 | 🟢 优秀 |
| com\perfflow\module\assessment\mapper\AssessmentFlowLogMapper.java | 90/100 | 🟢 优秀 |
| com\perfflow\module\assessment\mapper\AssessmentRowMapper.java | 90/100 | 🟢 优秀 |
| com\perfflow\module\assessment\mapper\AssessmentTableMapper.java | 89/100 | 🟡 良好 |
| com\perfflow\module\assessment\service\AssessmentCalcService.java | 80/100 | 🟡 良好 |
| com\perfflow\module\assessment\service\AssessmentExportService.java | 70/100 | 🟠 合格 |
| com\perfflow\module\assessment\service\AssessmentFlowService.java | 74/100 | 🟠 合格 |
| com\perfflow\module\assessment\service\AssessmentImportService.java | 80/100 | 🟡 良好 |
| com\perfflow\module\assessment\service\AssessmentPermissionService.java | 79/100 | 🟠 合格 |
| com\perfflow\module\assessment\service\AssessmentRowService.java | 91/100 | 🟢 优秀 |
| com\perfflow\module\assessment\service\AssessmentStateMachine.java | 80/100 | 🟡 良好 |
| com\perfflow\module\assessment\service\AssessmentTableService.java | 70/100 | 🟠 合格 |
| com\perfflow\module\audit\controller\AdjustLogController.java | 94/100 | 🟢 优秀 |
| com\perfflow\module\audit\entity\AdjustLog.java | 97/100 | 🟢 优秀 |
| com\perfflow\module\audit\mapper\AdjustLogMapper.java | 90/100 | 🟢 优秀 |
| com\perfflow\module\audit\service\AdjustLogService.java | 97/100 | 🟢 优秀 |
| com\perfflow\module\auth\controller\AuthController.java | 82/100 | 🟡 良好 |
| com\perfflow\module\auth\dto\ChangePasswordReq.java | 100/100 | 🟢 优秀 |
| com\perfflow\module\auth\dto\LoginReq.java | 85/100 | 🟡 良好 |
| com\perfflow\module\auth\dto\LoginResp.java | 84/100 | 🟡 良好 |
| com\perfflow\module\auth\dto\ProfileReq.java | 96/100 | 🟢 优秀 |
| com\perfflow\module\auth\dto\RefreshReq.java | 85/100 | 🟡 良好 |
| com\perfflow\module\auth\service\AuthService.java | 80/100 | 🟡 良好 |
| com\perfflow\module\auth\service\AuthUserDetailsService.java | 75/100 | 🟠 合格 |
| com\perfflow\module\deptassessment\controller\DeptAssessmentController.java | 88/100 | 🟡 良好 |
| com\perfflow\module\deptassessment\dto\DeptAssessmentReq.java | 99/100 | 🟢 优秀 |
| com\perfflow\module\deptassessment\dto\DeptAssessmentResp.java | 94/100 | 🟢 优秀 |
| com\perfflow\module\deptassessment\dto\DeptKpiRowReq.java | 95/100 | 🟢 优秀 |
| com\perfflow\module\deptassessment\dto\DeptKpiRowResp.java | 95/100 | 🟢 优秀 |
| com\perfflow\module\deptassessment\entity\DeptAssessment.java | 95/100 | 🟢 优秀 |
| com\perfflow\module\deptassessment\entity\DeptKpiRow.java | 95/100 | 🟢 优秀 |
| com\perfflow\module\deptassessment\enums\DeptAssessmentState.java | 98/100 | 🟢 优秀 |
| com\perfflow\module\deptassessment\mapper\DeptAssessmentMapper.java | 90/100 | 🟢 优秀 |
| com\perfflow\module\deptassessment\mapper\DeptKpiRowMapper.java | 90/100 | 🟢 优秀 |
| com\perfflow\module\deptassessment\service\DeptAssessmentService.java | 78/100 | 🟠 合格 |
| com\perfflow\module\deptassessment\service\DeptAssessmentStateMachine.java | 77/100 | 🟠 合格 |
| com\perfflow\module\grade\controller\GradeController.java | 88/100 | 🟡 良好 |
| com\perfflow\module\grade\dto\GradeQuotaReq.java | 94/100 | 🟢 优秀 |
| com\perfflow\module\grade\dto\WeightConfigReq.java | 96/100 | 🟢 优秀 |
| com\perfflow\module\grade\entity\GradeQuotaConfig.java | 94/100 | 🟢 优秀 |
| com\perfflow\module\grade\entity\WeightConfig.java | 94/100 | 🟢 优秀 |
| com\perfflow\module\grade\mapper\GradeQuotaConfigMapper.java | 90/100 | 🟢 优秀 |
| com\perfflow\module\grade\mapper\WeightConfigMapper.java | 90/100 | 🟢 优秀 |
| com\perfflow\module\grade\service\GradeCalculationService.java | 82/100 | 🟡 良好 |
| com\perfflow\module\grade\service\GradeQuotaService.java | 92/100 | 🟢 优秀 |
| com\perfflow\module\grade\service\WeightConfigService.java | 95/100 | 🟢 优秀 |
| com\perfflow\module\home\controller\HomeController.java | 85/100 | 🟡 良好 |
| com\perfflow\module\home\dto\RemindersResp.java | 92/100 | 🟢 优秀 |
| com\perfflow\module\home\service\HomeService.java | 78/100 | 🟠 合格 |
| com\perfflow\module\monitor\controller\MonitorController.java | 85/100 | 🟡 良好 |
| com\perfflow\module\monitor\dto\DeptProgressResp.java | 98/100 | 🟢 优秀 |
| com\perfflow\module\monitor\dto\ProgressResp.java | 97/100 | 🟢 优秀 |
| com\perfflow\module\monitor\service\ProgressService.java | 83/100 | 🟡 良好 |
| com\perfflow\module\monitor\service\ReminderService.java | 98/100 | 🟢 优秀 |
| com\perfflow\module\notification\controller\NotificationController.java | 91/100 | 🟢 优秀 |
| com\perfflow\module\notification\entity\Notification.java | 96/100 | 🟢 优秀 |
| com\perfflow\module\notification\mapper\NotificationMapper.java | 90/100 | 🟢 优秀 |
| com\perfflow\module\notification\service\NotificationService.java | 93/100 | 🟢 优秀 |
| com\perfflow\module\period\controller\PeriodController.java | 78/100 | 🟠 合格 |
| com\perfflow\module\period\dto\PeriodCreateReq.java | 93/100 | 🟢 优秀 |
| com\perfflow\module\period\dto\PeriodOpenReq.java | 100/100 | 🟢 优秀 |
| com\perfflow\module\period\dto\PeriodResp.java | 91/100 | 🟢 优秀 |
| com\perfflow\module\period\entity\AssessmentPeriod.java | 93/100 | 🟢 优秀 |
| com\perfflow\module\period\enums\PeriodType.java | 88/100 | 🟡 良好 |
| com\perfflow\module\period\mapper\AssessmentPeriodMapper.java | 90/100 | 🟢 优秀 |
| com\perfflow\module\period\service\AssessmentPeriodService.java | 90/100 | 🟢 优秀 |
| com\perfflow\module\period\service\PeriodImportService.java | 78/100 | 🟠 合格 |
| com\perfflow\module\system\controller\DeptOptionController.java | 89/100 | 🟡 良好 |
| com\perfflow\module\system\controller\SysDeptController.java | 82/100 | 🟡 良好 |
| com\perfflow\module\system\controller\SysUserController.java | 77/100 | 🟠 合格 |
| com\perfflow\module\system\controller\UserOptionController.java | 94/100 | 🟢 优秀 |
| com\perfflow\module\system\dto\DeptReq.java | 85/100 | 🟡 良好 |
| com\perfflow\module\system\dto\DeptResp.java | 84/100 | 🟡 良好 |
| com\perfflow\module\system\dto\PasswordResetResp.java | 85/100 | 🟡 良好 |
| com\perfflow\module\system\dto\UserCreateReq.java | 92/100 | 🟢 优秀 |
| com\perfflow\module\system\dto\UserResp.java | 84/100 | 🟡 良好 |
| com\perfflow\module\system\dto\UserUpdateReq.java | 85/100 | 🟡 良好 |
| com\perfflow\module\system\entity\SysDepartment.java | 85/100 | 🟡 良好 |
| com\perfflow\module\system\entity\SysUser.java | 93/100 | 🟢 优秀 |
| com\perfflow\module\system\mapper\SysDepartmentMapper.java | 90/100 | 🟢 优秀 |
| com\perfflow\module\system\mapper\SysUserMapper.java | 90/100 | 🟢 优秀 |
| com\perfflow\module\system\service\SysDeptService.java | 72/100 | 🟠 合格 |
| com\perfflow\module\system\service\SysUserService.java | 83/100 | 🟡 良好 |
| com\perfflow\security\DataScopeContext.java | 90/100 | 🟢 优秀 |
| com\perfflow\security\DataScopeContextFilter.java | 88/100 | 🟡 良好 |
| com\perfflow\security\JwtAuthenticationFilter.java | 77/100 | 🟠 合格 |
| com\perfflow\security\JwtUtil.java | 76/100 | 🟠 合格 |
| com\perfflow\security\SecurityUser.java | 84/100 | 🟡 良好 |
| com\perfflow\task\PendingScheduleTask.java | 83/100 | 🟡 良好 |
| com\perfflow\task\ReminderScheduleTask.java | 86/100 | 🟡 良好 |


## 🔴 严重问题 (1)

| 文件 | 类型 | 行号 | 描述 | 建议 |
|------|------|------|------|------|

### 问题分布
- **安全性**: 1

### 详细列表
| com\perfflow\module\system\service\SysUserService.java | 安全性 | 35 | 检测到硬编码的敏感信息 | 使用环境变量或配置文件存储敏感信息 |

### 🔍 严重问题代码详情

#### com\perfflow\module\system\service\SysUserService.java:35

```
    private static final String DEFAULT_PWD = "12345678";
```

**问题**: 检测到硬编码的敏感信息
**建议**: 使用环境变量或配置文件存储敏感信息


## 🔵 优化问题 (11)

| 文件 | 类型 | 行号 | 描述 | 建议 |
|------|------|------|------|------|

### 问题分布
- **代码可读性**: 11

### 详细列表
| com\perfflow\module\assessment\service\AssessmentExportService.java | 代码可读性 | 197 | 代码行过长（124字符） | 建议将长行拆分为多行，推荐不超过80字符 |
| com\perfflow\module\assessment\service\AssessmentStateMachine.java | 代码可读性 | 27 | 代码行过长（125字符） | 建议将长行拆分为多行，推荐不超过80字符 |
| com\perfflow\module\assessment\service\AssessmentTableService.java | 代码可读性 | 182 | 代码行过长（137字符） | 建议将长行拆分为多行，推荐不超过80字符 |
| com\perfflow\module\assessment\service\AssessmentTableService.java | 代码可读性 | 211 | 代码行过长（137字符） | 建议将长行拆分为多行，推荐不超过80字符 |
| com\perfflow\module\assessment\service\AssessmentTableService.java | 代码可读性 | 233 | 代码行过长（137字符） | 建议将长行拆分为多行，推荐不超过80字符 |
| com\perfflow\module\assessment\service\AssessmentTableService.java | 代码可读性 | 258 | 代码行过长（137字符） | 建议将长行拆分为多行，推荐不超过80字符 |
| com\perfflow\module\assessment\service\AssessmentTableService.java | 代码可读性 | 304 | 代码行过长（137字符） | 建议将长行拆分为多行，推荐不超过80字符 |
| com\perfflow\module\deptassessment\service\DeptAssessmentService.java | 代码可读性 | 475 | 代码行过长（131字符） | 建议将长行拆分为多行，推荐不超过80字符 |
| com\perfflow\module\deptassessment\service\DeptAssessmentStateMachine.java | 代码可读性 | 26 | 代码行过长（138字符） | 建议将长行拆分为多行，推荐不超过80字符 |
| com\perfflow\module\deptassessment\service\DeptAssessmentStateMachine.java | 代码可读性 | 27 | 代码行过长（140字符） | 建议将长行拆分为多行，推荐不超过80字符 |
| com\perfflow\module\deptassessment\service\DeptAssessmentStateMachine.java | 代码可读性 | 28 | 代码行过长（134字符） | 建议将长行拆分为多行，推荐不超过80字符 |

## 📁 文件级别分析

| 文件 | 语言 | 行数 | 问题数 | 注释行 |
|------|------|------|--------|--------|
| com\perfflow\PerfFlowApplication.java | java | 19 | 0 | 0 |
| com\perfflow\common\api\Result.java | java | 48 | 0 | 1 |
| com\perfflow\common\api\ResultCode.java | java | 75 | 0 | 9 |
| com\perfflow\common\constant\RoleConst.java | java | 45 | 0 | 8 |
| com\perfflow\common\enums\NotificationType.java | java | 26 | 0 | 5 |
| com\perfflow\common\excel\ExcelController.java | java | 72 | 0 | 1 |
| com\perfflow\common\excel\ExcelImportService.java | java | 164 | 0 | 4 |
| com\perfflow\common\excel\ExcelTemplateService.java | java | 58 | 0 | 6 |
| com\perfflow\common\exception\BizException.java | java | 29 | 0 | 1 |
| com\perfflow\common\exception\GlobalExceptionHandler.java | java | 101 | 0 | 5 |
| com\perfflow\common\util\SecurityContextHelper.java | java | 43 | 0 | 2 |
| com\perfflow\config\AdminBusinessGuardInterceptor.java | java | 55 | 0 | 3 |
| com\perfflow\config\MybatisPlusConfig.java | java | 52 | 0 | 2 |
| com\perfflow\config\OpenApiConfig.java | java | 31 | 0 | 0 |
| com\perfflow\config\SecurityConfig.java | java | 101 | 0 | 4 |
| com\perfflow\config\WebMvcConfig.java | java | 39 | 0 | 2 |
| com\perfflow\module\assessment\controller\AssessmentExportController.java | java | 48 | 0 | 0 |
| com\perfflow\module\assessment\controller\AssessmentFlowController.java | java | 30 | 0 | 1 |
| com\perfflow\module\assessment\controller\AssessmentRowController.java | java | 39 | 0 | 0 |
| com\perfflow\module\assessment\controller\AssessmentTableController.java | java | 111 | 0 | 0 |
| com\perfflow\module\assessment\dto\AssessmentRejectReq.java | java | 16 | 0 | 0 |
| com\perfflow\module\assessment\dto\AssessmentTableResp.java | java | 45 | 0 | 6 |
| com\perfflow\module\assessment\dto\ExtendSuspendReq.java | java | 22 | 0 | 0 |
| com\perfflow\module\assessment\dto\FlowLogResp.java | java | 20 | 0 | 0 |
| com\perfflow\module\assessment\dto\LeadScoreReq.java | java | 23 | 0 | 0 |
| com\perfflow\module\assessment\dto\RowReq.java | java | 33 | 0 | 1 |
| com\perfflow\module\assessment\dto\RowResp.java | java | 30 | 0 | 2 |
| com\perfflow\module\assessment\entity\AssessmentFlowLog.java | java | 30 | 0 | 0 |
| com\perfflow\module\assessment\entity\AssessmentRow.java | java | 43 | 0 | 3 |
| com\perfflow\module\assessment\entity\AssessmentTable.java | java | 52 | 0 | 5 |
| com\perfflow\module\assessment\enums\AssessmentState.java | java | 22 | 0 | 6 |
| com\perfflow\module\assessment\enums\RowCategory.java | java | 18 | 0 | 4 |
| com\perfflow\module\assessment\mapper\AssessmentFlowLogMapper.java | java | 10 | 0 | 0 |
| com\perfflow\module\assessment\mapper\AssessmentRowMapper.java | java | 10 | 0 | 0 |
| com\perfflow\module\assessment\mapper\AssessmentTableMapper.java | java | 22 | 0 | 2 |
| com\perfflow\module\assessment\service\AssessmentCalcService.java | java | 122 | 0 | 7 |
| com\perfflow\module\assessment\service\AssessmentExportService.java | java | 223 | 1 | 14 |
| com\perfflow\module\assessment\service\AssessmentFlowService.java | java | 90 | 0 | 1 |
| com\perfflow\module\assessment\service\AssessmentImportService.java | java | 129 | 0 | 8 |
| com\perfflow\module\assessment\service\AssessmentPermissionService.java | java | 245 | 0 | 22 |
| com\perfflow\module\assessment\service\AssessmentRowService.java | java | 122 | 0 | 8 |
| com\perfflow\module\assessment\service\AssessmentStateMachine.java | java | 54 | 1 | 1 |
| com\perfflow\module\assessment\service\AssessmentTableService.java | java | 500 | 5 | 29 |
| com\perfflow\module\audit\controller\AdjustLogController.java | java | 35 | 0 | 1 |
| com\perfflow\module\audit\entity\AdjustLog.java | java | 32 | 0 | 3 |
| com\perfflow\module\audit\mapper\AdjustLogMapper.java | java | 10 | 0 | 0 |
| com\perfflow\module\audit\service\AdjustLogService.java | java | 71 | 0 | 3 |
| com\perfflow\module\auth\controller\AuthController.java | java | 68 | 0 | 0 |
| com\perfflow\module\auth\dto\ChangePasswordReq.java | java | 21 | 0 | 1 |
| com\perfflow\module\auth\dto\LoginReq.java | java | 17 | 0 | 0 |
| com\perfflow\module\auth\dto\LoginResp.java | java | 22 | 0 | 0 |
| com\perfflow\module\auth\dto\ProfileReq.java | java | 25 | 0 | 1 |
| com\perfflow\module\auth\dto\RefreshReq.java | java | 14 | 0 | 0 |
| com\perfflow\module\auth\service\AuthService.java | java | 166 | 0 | 4 |
| com\perfflow\module\auth\service\AuthUserDetailsService.java | java | 43 | 0 | 0 |
| com\perfflow\module\deptassessment\controller\DeptAssessmentController.java | java | 84 | 0 | 1 |
| com\perfflow\module\deptassessment\dto\DeptAssessmentReq.java | java | 21 | 0 | 2 |
| com\perfflow\module\deptassessment\dto\DeptAssessmentResp.java | java | 34 | 0 | 1 |
| com\perfflow\module\deptassessment\dto\DeptKpiRowReq.java | java | 28 | 0 | 1 |
| com\perfflow\module\deptassessment\dto\DeptKpiRowResp.java | java | 25 | 0 | 1 |
| com\perfflow\module\deptassessment\entity\DeptAssessment.java | java | 46 | 0 | 4 |
| com\perfflow\module\deptassessment\entity\DeptKpiRow.java | java | 39 | 0 | 2 |
| com\perfflow\module\deptassessment\enums\DeptAssessmentState.java | java | 42 | 0 | 8 |
| com\perfflow\module\deptassessment\mapper\DeptAssessmentMapper.java | java | 10 | 0 | 0 |
| com\perfflow\module\deptassessment\mapper\DeptKpiRowMapper.java | java | 10 | 0 | 0 |
| com\perfflow\module\deptassessment\service\DeptAssessmentService.java | java | 497 | 1 | 28 |
| com\perfflow\module\deptassessment\service\DeptAssessmentStateMachine.java | java | 54 | 3 | 1 |
| com\perfflow\module\grade\controller\GradeController.java | java | 102 | 0 | 1 |
| com\perfflow\module\grade\dto\GradeQuotaReq.java | java | 36 | 0 | 1 |
| com\perfflow\module\grade\dto\WeightConfigReq.java | java | 27 | 0 | 1 |
| com\perfflow\module\grade\entity\GradeQuotaConfig.java | java | 36 | 0 | 1 |
| com\perfflow\module\grade\entity\WeightConfig.java | java | 33 | 0 | 1 |
| com\perfflow\module\grade\mapper\GradeQuotaConfigMapper.java | java | 10 | 0 | 0 |
| com\perfflow\module\grade\mapper\WeightConfigMapper.java | java | 10 | 0 | 0 |
| com\perfflow\module\grade\service\GradeCalculationService.java | java | 219 | 0 | 17 |
| com\perfflow\module\grade\service\GradeQuotaService.java | java | 100 | 0 | 5 |
| com\perfflow\module\grade\service\WeightConfigService.java | java | 73 | 0 | 4 |
| com\perfflow\module\home\controller\HomeController.java | java | 29 | 0 | 0 |
| com\perfflow\module\home\dto\RemindersResp.java | java | 27 | 0 | 3 |
| com\perfflow\module\home\service\HomeService.java | java | 98 | 0 | 2 |
| com\perfflow\module\monitor\controller\MonitorController.java | java | 67 | 0 | 2 |
| com\perfflow\module\monitor\dto\DeptProgressResp.java | java | 23 | 0 | 2 |
| com\perfflow\module\monitor\dto\ProgressResp.java | java | 21 | 0 | 1 |
| com\perfflow\module\monitor\service\ProgressService.java | java | 109 | 0 | 3 |
| com\perfflow\module\monitor\service\ReminderService.java | java | 38 | 0 | 2 |
| com\perfflow\module\notification\controller\NotificationController.java | java | 50 | 0 | 1 |
| com\perfflow\module\notification\entity\Notification.java | java | 28 | 0 | 2 |
| com\perfflow\module\notification\mapper\NotificationMapper.java | java | 10 | 0 | 0 |
| com\perfflow\module\notification\service\NotificationService.java | java | 99 | 0 | 5 |
| com\perfflow\module\period\controller\PeriodController.java | java | 77 | 0 | 0 |
| com\perfflow\module\period\dto\PeriodCreateReq.java | java | 43 | 0 | 2 |
| com\perfflow\module\period\dto\PeriodOpenReq.java | java | 23 | 0 | 3 |
| com\perfflow\module\period\dto\PeriodResp.java | java | 46 | 0 | 3 |
| com\perfflow\module\period\entity\AssessmentPeriod.java | java | 38 | 0 | 2 |
| com\perfflow\module\period\enums\PeriodType.java | java | 96 | 0 | 15 |
| com\perfflow\module\period\mapper\AssessmentPeriodMapper.java | java | 10 | 0 | 0 |
| com\perfflow\module\period\service\AssessmentPeriodService.java | java | 177 | 0 | 12 |
| com\perfflow\module\period\service\PeriodImportService.java | java | 162 | 0 | 8 |
| com\perfflow\module\system\controller\DeptOptionController.java | java | 43 | 0 | 1 |
| com\perfflow\module\system\controller\SysDeptController.java | java | 47 | 0 | 0 |
| com\perfflow\module\system\controller\SysUserController.java | java | 73 | 0 | 0 |
| com\perfflow\module\system\controller\UserOptionController.java | java | 35 | 0 | 1 |
| com\perfflow\module\system\dto\DeptReq.java | java | 16 | 0 | 0 |
| com\perfflow\module\system\dto\DeptResp.java | java | 25 | 0 | 0 |
| com\perfflow\module\system\dto\PasswordResetResp.java | java | 13 | 0 | 0 |
| com\perfflow\module\system\dto\UserCreateReq.java | java | 26 | 0 | 1 |
| com\perfflow\module\system\dto\UserResp.java | java | 41 | 0 | 0 |
| com\perfflow\module\system\dto\UserUpdateReq.java | java | 19 | 0 | 0 |
| com\perfflow\module\system\entity\SysDepartment.java | java | 30 | 0 | 0 |
| com\perfflow\module\system\entity\SysUser.java | java | 38 | 0 | 2 |
| com\perfflow\module\system\mapper\SysDepartmentMapper.java | java | 10 | 0 | 0 |
| com\perfflow\module\system\mapper\SysUserMapper.java | java | 10 | 0 | 0 |
| com\perfflow\module\system\service\SysDeptService.java | java | 81 | 0 | 3 |
| com\perfflow\module\system\service\SysUserService.java | java | 243 | 1 | 9 |
| com\perfflow\security\DataScopeContext.java | java | 156 | 0 | 15 |
| com\perfflow\security\DataScopeContextFilter.java | java | 77 | 0 | 3 |
| com\perfflow\security\JwtAuthenticationFilter.java | java | 128 | 0 | 5 |
| com\perfflow\security\JwtUtil.java | java | 100 | 0 | 4 |
| com\perfflow\security\SecurityUser.java | java | 42 | 0 | 1 |
| com\perfflow\task\PendingScheduleTask.java | java | 78 | 0 | 2 |
| com\perfflow\task\ReminderScheduleTask.java | java | 87 | 0 | 2 |

## 📖 代码可读性评估

**整体评级**: 🔴 需改进

### 评估指标

1. **注释覆盖率**: 4.9%
   - 评价: 注释覆盖率偏低，建议增加函数和复杂逻辑的注释

### 改进建议

1. **函数和类**: 为每个公共函数和类添加文档字符串
2. **复杂逻辑**: 为复杂的算法和业务逻辑添加详细注释
3. **常量说明**: 为魔法数字和常量添加说明
4. **代码格式**: 保持一致的代码格式和缩进风格

## 📝 附录

### 严重性定义

- **严重** 🔴: 可能导致功能错误、安全漏洞或系统崩溃的问题，必须立即修复
- **一般** 🟡: 影响代码质量、可维护性或可读性的问题，建议在下次迭代中修复
- **优化** 🔵: 性能优化、代码风格或最佳实践建议，可根据项目进度安排

### 检查类型说明

- **代码规范性**: 文件命名、变量命名、代码格式等规范问题
- **潜在Bug**: 可能导致运行时错误的代码模式
- **性能和安全**: 性能问题和安全漏洞风险
- **代码可读性**: 代码长度、复杂度等可读性问题
- **代码维护性**: TODO、FIXME等未完成项
- **命名规范**: 不符合语言命名规范的标识符
- **安全性**: 硬编码密钥、SQL注入风险等安全问题

### 华为Java编程规范评分说明

评分基于《华为Java编程规范》，总分100分，分为5个维度：

- **排版规范**（20分）：缩进、分界符、行长度、语句格式等
- **注释规范**（25分）：注释量、类注释、方法注释、JavaDoc等
- **命名规范**（20分）：类名、方法名、变量名、常量名等
- **代码编写规范**（20分）：日志使用、魔法数字、泛型、异常处理等
- **性能与可靠性**（15分）：日志级别判断、字符串拼接、性能优化等

**评级标准**：
- 🟢 优秀（90-100分）
- 🟡 良好（80-89分）
- 🟠 合格（70-79分）
- 🔴 需改进（<70分）

---

*本报告由代码审查工具自动生成 - 2026-08-28 16:43:18*