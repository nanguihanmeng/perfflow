# PerfFlow 前端对接文档（Backend API）

> 版本：基于当前后端代码生成（Spring Boot 3.3.4 + Spring Security + MyBatis-Plus）
> 生成日期：2026-08-20
> 说明：本文档字段名、路径、类型均直接对齐后端 Java 源码，作为前后端契约，修改后端前请先同步本文档。

---

## 0. 通用约定

### 0.1 Base URL
- 开发环境：`http://localhost:8080`（由后端 `server.port` 决定）
- 所有接口均挂载在 Base URL 之后，例如登录：`POST http://localhost:8080/auth/login`

### 0.2 鉴权
- 除 `POST /auth/login`、`POST /auth/refresh`、Swagger 文档、Spring Boot Actuator、/error 外，所有接口**必须登录**。
- 请求头携带：
  ```
  Authorization: Bearer <accessToken>
  ```
- `admin` 角色账号**禁止**访问业务接口（`/assessment-tables`、`/periods`、`/home` 等），访问会返回 `HTTP 403` + `ResultCode.ADMIN_NO_BUSINESS_VISIBILITY(1007)`。
- `/admin/**` 仅 `ADMIN` 角色可访问，其它角色返回 `HTTP 403`。

### 0.3 统一响应结构 `Result<T>`
所有接口（除导出/打印两个二进制接口外）返回如下 JSON：
```json
{
  "code": 0,
  "message": "ok",
  "data": { }
}
```
| 字段 | 类型 | 说明 |
|------|------|------|
| code | number(long) | `0` 成功；非 0 见错误码表（第 5 节） |
| message | string | 成功时为 `"ok"`；失败时为错误描述 |
| data | object / array / null | 业务数据，结构见各接口 |

> ⚠️ 前端判断逻辑：**必须同时判断 HTTP 状态码与 `code`**。
> - 鉴权/权限类错误：`HTTP 401/403`，但响应体仍是 `Result`（含 `code`/`message`）。
> - 参数校验错误：`HTTP 400` + `code=1006`。
> - 业务规则错误：`HTTP 200` + `code=2xxx`（如 2010 状态不允许、2021 分数越界）。
> 建议封装：`if (httpStatus >= 200 && httpStatus < 300 && res.code === 0) 成功 else 失败`。

### 0.4 分页响应
列表分页接口统一返回 MyBatis-Plus `Page<T>` 结构：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "records": [ /* T[] */ ],
    "total": 86,
    "size": 20,
    "current": 1,
    "pages": 5,
    "searchCount": true
  }
}
```
- 前端分页请求参数：`pageNo`（默认 1）、`pageSize`（默认 20）。

### 0.5 时间格式
- 日期 `LocalDate`：`"2026-03-01"`（YYYY-MM-DD）
- 时间 `LocalDateTime`：`"2026-03-01T10:30:00"`（ISO-8601，可能带毫秒/时区，前端统一用 `dayjs`/原生 `Date` 解析即可）

### 0.6 数值
- 分数均为**字符串形式的 BigDecimal**（如 `"95.50"`），前端按字符串存储/展示，提交时同样传字符串，避免浮点精度问题。
- 状态类字段多为 `string` 枚举名（见第 6 节）。

---

## 1. 鉴权模块 `/auth`

### 1.1 登录
- `POST /auth/login`
- 权限：匿名
- 请求体 `LoginReq`：
  | 字段 | 类型 | 必填 | 校验 | 说明 |
  |------|------|------|------|------|
  | username | string | 是 | @NotBlank | 登录名 |
  | password | string | 是 | @NotBlank | 密码（明文，HTTPS 传输） |
- 响应 `Result<LoginResp>`：
  | 字段 | 类型 | 说明 |
  |------|------|------|
  | accessToken | string | 访问令牌，请求业务接口携带 |
  | refreshToken | string | 刷新令牌，用于换发 accessToken |
  | expiresIn | number | accessToken 有效期（秒），当前 `3600` |
  | tokenType | string | 固定 `"Bearer"` |
  | userId | number | 用户 ID |
  | username | string | 登录名 |
  | realName | string | 真实姓名 |
  | role | string | 主角色：`EMP`/`DEPT_LEAD`/`LEAD`/`HR`/`ADMIN` |
  | deptId | number | 部门 ID |
  | deptName | string | 部门名 |
  | deptLead | boolean | 是否部门负责人 |
  | mustChangePassword | boolean | **true 时强制跳转改密页** |

请求示例：
```json
{ "username": "zhangsan", "password": "123456" }
```
响应示例：
```json
{
  "code": 0, "message": "ok",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJ...",
    "expiresIn": 3600, "tokenType": "Bearer",
    "userId": 12, "username": "zhangsan", "realName": "张三",
    "role": "EMP", "deptId": 3, "deptName": "研发部",
    "deptLead": false, "mustChangePassword": false
  }
}
```

### 1.2 刷新令牌
- `POST /auth/refresh`
- 权限：匿名
- 请求体 `RefreshReq`：
  | 字段 | 类型 | 必填 | 校验 | 说明 |
  |------|------|------|------|------|
  | refreshToken | string | 是 | @NotBlank | 登录/刷新返回的 refreshToken |
- 响应：同 `LoginResp`（返回**新的** accessToken + refreshToken，旧的 refreshToken 失效）

### 1.3 当前用户信息
- `GET /auth/me`
- 权限：登录
- 响应 `Result<Object>`：返回当前用户在 `DataScopeContext.CurrentUser` 的快照（含 `userId`、`deptId`、`username`、`realName`、`primaryRole`、`roles`、`deptLead`、`mustChangePassword`）。建议前端优先以登录接口返回的 `LoginResp` 为准，本接口用于刷新上下文。

### 1.4 登出
- `POST /auth/logout`
- 权限：登录
- 响应：`Result<Void>`（code=0）。**后端为无状态 JWT，登出只需前端清除本地 token 即可**，本接口仅为语义兼容。

---

## 2. 考核周期 `/periods`

| 接口 | 方法 | 路径 | 权限 |
|------|------|------|------|
| 列表 | GET | `/periods` | 登录 |
| 当前周期 | GET | `/periods/current` | 登录 |
| 新建 | POST | `/periods` | ADMIN |
| 开启 | POST | `/periods/{id}/open` | ADMIN |
| 关闭 | POST | `/periods/{id}/close` | ADMIN |

### 2.1 周期列表 `GET /periods`
- 响应 `Result<List<PeriodResp>>`
- `PeriodResp`：
  | 字段 | 类型 | 说明 |
  |------|------|------|
  | id | number | 周期 ID |
  | name | string | 周期名称 |
  | year | number | 年份 |
  | quarter | number | 季度（1-4） |
  | startDate | string(YYYY-MM-DD) | 开始日期 |
  | suspendEndDate | string | 自评挂起截止 |
  | deptReviewEndDate | string | 部门审核截止 |
  | leadScoreEndDate | string | 领导评分截止 |
  | autoPushOnExpire | boolean | 到期是否自动推送 |
  | status | number | `0`=未开始 `1`=进行中 `2`=已结束 |

### 2.2 当前周期 `GET /periods/current`
- 响应 `Result<PeriodResp>`（无活跃周期时 `data=null`）

### 2.3 新建周期 `POST /periods`
- 请求体 `PeriodCreateReq`：
  | 字段 | 类型 | 必填 | 校验 | 说明 |
  |------|------|------|------|------|
  | name | string | 是 | @NotBlank | 名称 |
  | year | number | 是 | 2000-2100 | 年份 |
  | quarter | number | 是 | 1-4 | 季度 |
  | startDate | string(YYYY-MM-DD) | 是 | @NotNull | 开始日期 |
  | suspendEndDate | string | 是 | @NotNull | 自评挂起截止 |
  | deptReviewEndDate | string | 是 | @NotNull | 部门审核截止 |
  | leadScoreEndDate | string | 是 | @NotNull | 领导评分截止 |
  | autoPushOnExpire | boolean | 否 | - | 默认 false |
- 响应 `Result<number>`（返回新建周期 ID）

### 2.4 开启周期 `POST /periods/{id}/open`
- 作用：状态置为进行中，并自动为全员生成主表 + 10 行模板。
- 响应 `Result<Void>`

### 2.5 关闭周期 `POST /periods/{id}/close`
- 响应 `Result<Void>`

---

## 3. 考核主表 `/assessment-tables`

> 类级 `@PreAuthorize("isAuthenticated()")`，所有接口需登录。`/admin` 账号不可访问。

| 接口 | 方法 | 路径 | 权限 |
|------|------|------|------|
| 详情 | GET | `/assessment-tables/{id}` | 登录 |
| 分页 | GET | `/assessment-tables` | 登录 |
| 员工提交 | POST | `/assessment-tables/{id}/submit` | EMP（本人） |
| 人事推送 | POST | `/assessment-tables/{id}/push` | HR |
| 部门通过 | POST | `/assessment-tables/{id}/approve` | DEPT_LEAD |
| 部门打回 | POST | `/assessment-tables/{id}/reject` | DEPT_LEAD |
| 领导评分 | POST | `/assessment-tables/{id}/lead-score` | LEAD |
| 延长挂起 | POST | `/assessment-tables/{id}/extend-suspend` | HR / ADMIN |

### 3.1 主表详情 `GET /assessment-tables/{id}`
- 响应 `Result<AssessmentTableResp>`（完整结构见第 7.1 节）

### 3.2 主表分页 `GET /assessment-tables`
- Query 参数：
  | 参数 | 类型 | 必填 | 默认 | 说明 |
  |------|------|------|------|------|
  | periodId | number | 否 | - | 按周期过滤 |
  | deptId | number | 否 | - | 按部门过滤 |
  | state | string | 否 | - | 状态名，如 `SELF_DRAFTING`（见第 6.1 节） |
  | pageNo | number | 否 | 1 | 页码 |
  | pageSize | number | 否 | 20 | 每页条数 |
- 响应 `Result<Page<AssessmentTableResp>>`（元素不含 `rows`/`logs`，仅主表字段）

### 3.3 状态机流转（重要，前后端必须一致）
```
SELF_DRAFTING ──(员工 submit)──▶ SELF_SUSPENDED
SELF_SUSPENDED ──(人事 push)──▶ DEPT_REVIEW
DEPT_REVIEW ──(部门领导 approve)──▶ LEAD_SCORING
DEPT_REVIEW ──(部门领导 reject)──▶ SELF_DRAFTING
LEAD_SCORING ──(领导 lead-score)──▶ FINISHED
```
- 任何非法的状态流转都会返回 `HTTP 200` + `code=2010 (STATE_NOT_ALLOWED)`。

### 3.4 员工提交 `POST /assessment-tables/{id}/submit`
- 仅本人 `SELF_DRAFTING` 状态可提交；要求所有行均已填写（否则 `2011 SUBMIT_REQUIRED_FIELDS`）。
- 响应 `Result<Void>`

### 3.5 人事推送 `POST /assessment-tables/{id}/push`
- 响应 `Result<Void>`

### 3.6 部门通过 `POST /assessment-tables/{id}/approve`
- 请求体（可选）`AssessmentRejectReq`（仅取 `comment` 字段作为通过备注）：
  | 字段 | 类型 | 必填 | 校验 | 说明 |
  |------|------|------|------|------|
  | comment | string | 否 | @Size(max=500) | 通过备注 |
- 响应 `Result<Void>`

### 3.7 部门打回 `POST /assessment-tables/{id}/reject`
- 请求体 `AssessmentRejectReq`：
  | 字段 | 类型 | 必填 | 校验 | 说明 |
  |------|------|------|------|------|
  | comment | string | 是 | @NotBlank @Size(max=500) | 打回原因（**必填**，否则 `code=2012`） |
- 响应 `Result<Void>`

### 3.8 领导评分 `POST /assessment-tables/{id}/lead-score`
- 请求体 `LeadScoreReq`：
  | 字段 | 类型 | 必填 | 校验 | 说明 |
  |------|------|------|------|------|
  | leaderScore | string(BigDecimal) | 是 | 0-100 | 领导评分（否则 `code=2021`） |
  | comment | string | 否 | @Size(max=500) | 备注 |
- 响应 `Result<Void>`

### 3.9 延长挂起 `POST /assessment-tables/{id}/extend-suspend`
- 请求体 `ExtendSuspendReq`：
  | 字段 | 类型 | 必填 | 校验 | 说明 |
  |------|------|------|------|------|
  | days | number | 是 | 1-30 | 延长天数（累计上限 30，否则 `code=2030`） |
  | reason | string | 否 | @Size(max=500) | 原因 |
- 响应 `Result<Void>`

---

## 4. 考核行 `/assessment-tables/{tableId}/rows`

| 接口 | 方法 | 路径 | 权限 |
|------|------|------|------|
| 更新行 | PUT | `/assessment-tables/{tableId}/rows/{rowId}` | EMP（本人主表） |
| 部门调分 | POST | `/assessment-tables/{tableId}/rows/{rowId}/adjust` | DEPT_LEAD |

### 4.1 更新行 `PUT /assessment-tables/{tableId}/rows/{rowId}`
- 请求体 `RowReq`（员工仅可填非得分列）：
  | 字段 | 类型 | 必填 | 校验 | 说明 |
  |------|------|------|------|------|
  | indicatorName | string | 否 | @Size(max=128) | 指标名 |
  | workTarget | string | 否 | @Size(max=5000) | 工作目标 |
  | scoreCriteria | string | 否 | @Size(max=5000) | 评分标准 |
  | completionRate | string(BigDecimal) | 否 | 0-150 | 完成率 |
  | rowResult | string | 否 | @Size(max=64) | 行结果 |
- 响应 `Result<Void>`

### 4.2 部门调分 `POST /assessment-tables/{tableId}/rows/{rowId}/adjust`
- 请求体 `RowAdjustReq`：
  | 字段 | 类型 | 必填 | 校验 | 说明 |
  |------|------|------|------|------|
  | adjustedScore | string(BigDecimal) | 是 | -50 ~ 150 | 调整后得分 |
  | adjustRemark | string | 是 | @NotBlank | 调分原因（**必填**，否则 `code=2013`） |
  | rowResult | string | 否 | @Size(max=64) | 行结果 |
- 响应 `Result<Void>`

---

## 5. 流程日志 `/assessment-tables/{id}/logs`

- `GET /assessment-tables/{id}/logs`
- 权限：`HR` / `LEAD` / `DEPT_LEAD`
- 响应 `Result<List<FlowLogResp>>`
- `FlowLogResp`：
  | 字段 | 类型 | 说明 |
  |------|------|------|
  | id | number | 日志 ID |
  | fromState | string | 原状态 |
  | toState | string | 目标状态 |
  | action | string | 动作 |
  | operatorId | number | 操作人 ID |
  | operatorName | string | 操作人姓名 |
  | operatorRole | string | 操作人角色 |
  | comment | string | 备注 |
  | createdAt | string(ISO) | 操作时间 |

---

## 6. 导出与首页

### 6.1 导出 Excel `GET /assessment-tables/export`
- 权限：`HR`
- Query：`periodId`(number，可选，不传则全部)
- 响应：**二进制文件流**（`application/octet-stream`），非 `Result` JSON。
- 前端处理：使用 `blob` 下载，文件名由后端 `Content-Disposition` 给出（`assessment_<periodId>.xlsx` 或 `assessment_all.xlsx`）。
```js
// axios 示例
const res = await axios.get('/assessment-tables/export', { params: { periodId }, responseType: 'blob' });
```

### 6.2 打印 HTML `GET /assessment-tables/export/print`
- 权限：`HR`
- Query：`periodId`(可选)
- 响应：`text/html`（UTF-8），直接 `window.open` 或写入新窗口打印即可。

### 6.3 首页提醒 `GET /home/reminders`
- 权限：登录
- 响应 `Result<RemindersResp>`：
  | 字段 | 类型 | 说明 |
  |------|------|------|
  | todos | Reminder[] | 当前用户待办 |
  | upcomingSuspends | Reminder[] | 挂起结束前 3 天提醒 |
  | systemNotices | Reminder[] | 公示提醒 |
  - `Reminder`：`type`(string) / `title`(string) / `description`(string) / `targetTableId`(number) / `severity`(number: 1=info 2=warn 3=urgent)

---

## 7. 管理员模块 `/admin`

> 仅 `ADMIN` 可访问；`ADMIN` 角色**不可**访问业务接口（见 0.2 节）。

### 7.1 部门 `/admin/departments`
| 接口 | 方法 | 说明 |
|------|------|------|
| 列表 | GET `/admin/departments` | 返回 `Result<List<DeptResp>>` |
| 新建 | POST `/admin/departments` | 入参 `DeptReq` |
| 修改 | PUT `/admin/departments/{id}` | 入参 `DeptReq` |
| 删除 | DELETE `/admin/departments/{id}` | - |

`DeptReq`：
| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| name | string | 是 | @NotBlank | 部门名 |
| parentId | number | 否 | - | 父部门 ID |
| leaderUserId | number | 否 | - | 部门负责人用户 ID |
| sort | number | 否 | - | 排序 |
| remark | string | 否 | - | 备注 |

`DeptResp`：`id` / `name` / `parentId` / `leaderUserId` / `sort` / `remark`

### 7.2 用户 `/admin/users`
| 接口 | 方法 | 说明 |
|------|------|------|
| 分页 | GET `/admin/users` | 返回 `Result<Page<UserResp>>` |
| 新建 | POST `/admin/users` | 入参 `UserCreateReq` |
| 修改 | PUT `/admin/users/{id}` | 入参 `UserUpdateReq` |
| 删除 | DELETE `/admin/users/{id}` | - |
| 重置密码 | POST `/admin/users/{id}/reset-password` | 返回 `Result<PasswordResetResp>` |
| 启用/禁用 | POST `/admin/users/{id}/toggle-status` | - |

分页 Query：`username`(string) / `role`(string) / `deptId`(number) / `status`(number) / `pageNo`(默认1) / `pageSize`(默认20)

`UserCreateReq`：
| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| username | string | 是 | @NotBlank | 登录名 |
| realName | string | 是 | @NotBlank | 真实姓名 |
| role | string | 是 | `EMP|DEPT_LEAD|LEAD|HR|ADMIN` | 角色 |
| deptId | number | 否 | - | 部门 ID |
| deptLead | boolean | 否 | - | 是否部门负责人 |
| email | string | 否 | @Email | 邮箱 |
| phone | string | 否 | - | 电话 |

`UserUpdateReq`：`realName` / `role`(同上正则) / `deptId` / `deptLead` / `email` / `phone` / `status`(number)

`UserResp`：`id` / `username` / `realName` / `role` / `deptId` / `deptName` / `deptLead` / `email` / `phone` / `status` / `lastLoginAt` / `mustChangePassword`

`PasswordResetResp`：`initialPassword`(string 临时密码)

---

## 8. 枚举与常量（前端下拉/状态映射用）

### 8.1 角色 `role`
`EMP` / `DEPT_LEAD` / `LEAD` / `HR` / `ADMIN`

### 8.2 主表状态 `AssessmentState`（对应 `state` 字段）
| 枚举名 | 含义 | 典型操作角色 |
|--------|------|------|
| SELF_DRAFTING | 1 自评中 | EMP 填写/提交 |
| SELF_SUSPENDED | 2 自评挂起（待人事推送） | HR 推送 |
| DEPT_REVIEW | 3 部门审核 | DEPT_LEAD 通过/打回 |
| LEAD_SCORING | 4 领导评分 | LEAD 评分 |
| FINISHED | 5 已完成 | - |

### 8.3 行分类 `RowCategory`（对应 `category` 字段）
| 枚举名 | 含义 | seq 范围 |
|--------|------|------|
| PLAN | 个人季度工作计划 | 1-5 |
| OPEN | 开放型指标 | 6-7 |
| BONUS | 加减分项 | 8-10 |

> 每个主表固定 10 行（seq 1-10）。前端渲染模板可直接按 `category` 分组。

### 8.4 周期状态 `status`（number）
`0`=未开始 / `1`=进行中 / `2`=已结束

### 8.5 用户状态 `status`（number，SysUser）
`0`=禁用 / `1`=启用（具体以 `SysUserService` 为准，建议前端用「启用/禁用」文案映射）

---

## 9. 业务错误码 `ResultCode`
前端可根据 `code` 做统一 toast / 跳转：

| code | 常量 | 含义 | 前端建议动作 |
|------|------|------|------|
| 0 | SUCCESS | 成功 | - |
| 1001 | BAD_REQUEST | 请求参数不合法 | 提示 message |
| 1002 | UNAUTHORIZED | 请先登录 | 跳登录页 |
| 1003 | FORBIDDEN | 无权访问 | 提示无权限 |
| 1004 | NOT_FOUND | 资源不存在 | 提示 |
| 1005 | METHOD_NOT_ALLOWED | 请求方式不支持 | 检查 method |
| 1006 | VALIDATION_FAILED | 参数校验未通过 | 提示 message / 字段校验 |
| 1007 | ADMIN_NO_BUSINESS_VISIBILITY | 管理员不可访问业务数据 | 提示切换账号 |
| 1101 | LOGIN_INVALID | 用户名或密码错误 | 提示重输 |
| 1102 | ACCOUNT_DISABLED | 账号已被禁用 | 提示联系管理员 |
| 1103 | TOKEN_INVALID | 凭证无效或过期 | 跳登录 |
| 1104 | TOKEN_EXPIRED | 凭证已过期 | 自动 refresh 或跳登录 |
| 1105 | MUST_CHANGE_PASSWORD_INIT | 首次登录必须改密 | 强制改密页 |
| 1106 | INIT_PASSWORD | 请设置新密码 | 改密页 |
| 2001 | PERIOD_NOT_OPEN | 考核周期未开启 | 提示 |
| 2002 | PERMISSION_DENIED_FOR_ROW | 无可操作权限 | 提示 |
| 2010 | STATE_NOT_ALLOWED | 当前状态不允许该操作 | 提示状态不符 |
| 2011 | SUBMIT_REQUIRED_FIELDS | 请完成所有行填写再提交 | 提示 |
| 2012 | REJECT_COMMENT_REQUIRED | 打回必须填原因 | 校验表单 |
| 2013 | ADJUST_REMARK_REQUIRED | 调分必须填原因 | 校验表单 |
| 2020 | LEADER_SCORE_REQUIRED | 请填写领导评分 | 校验表单 |
| 2021 | SCORE_OUT_OF_RANGE | 分数必须在 0-100 | 校验表单 |
| 2030 | EXTEND_OVER_LIMIT | 延长挂起天数达上限(30) | 提示 |
| 3000 | INTERNAL_ERROR | 系统异常 | 提示稍后重试 |

---

## 10. 对接注意事项（避坑）

1. **`admin` 账号不能调业务接口**：管理后台与业务前台建议拆成两个前端应用或两个路由空间，避免用 ADMIN 令牌请求 `/assessment-tables` 等。
2. **分数用字符串**：`selfScore`/`leaderScore`/`adjustedScore`/`finalScore`/`completionRate` 均为字符串，前端提交时传字符串（如 `"92.50"`），不要传 number 以免后端 BigDecimal 解析异常。
3. **脱敏字段**：非本人/非授权角色查看时，`RowResp.selfScore`/`adjustedScore`/`leaderScore` 可能为 `null`，且 `masked=true`，前端应显示为「**」而非报错。
4. **导出接口不是 JSON**：`/assessment-tables/export` 与 `/export/print` 返回文件流/HTML，需单独处理 `responseType`，不要走统一 JSON 解析。
5. **刷新令牌**：`refreshToken` 用完即废（旋转策略），前端应无感刷新；若 `code=1103/1104` 才强制跳登录。
6. **状态机严格**：非法的状态流转后端返回 `code=2010`，按钮应根据当前 `state` 动态显隐（参考 8.2 节）。
7. **分页参数名**：使用 `pageNo` + `pageSize`（不是 `page`/`size`）。
8. **CORS**：后端允许 `localhost:4200` 等来源并携带凭证；前端若自托管域名需同步加入 `SecurityConfig.CORS_ALLOWED_ORIGINS`。
