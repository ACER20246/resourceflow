# ResourceFlow MVP 接口文档

## 统一约定

- 基础路径：`/api`；JSON 请求使用 `Content-Type: application/json`。
- 登录接口外，需在请求头携带 `Authorization: Bearer <token>`；`/api/admin/**` 需管理员角色。
- 成功与错误响应统一为：

```json
{"code":200,"message":"success","data":{},"traceId":"a1b2c3d4"}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | integer | 200 成功；400 参数/业务冲突；401 未认证；403 无权限；404 不存在 |
| `message` | string | 结果消息 |
| `data` | object/array/null | 响应数据 |
| `traceId` | string | 请求追踪 ID |

## 用户注册与登录

### 注册
`POST /api/auth/register`

请求参数：
```json
{"username":"zhangsan","password":"Password123","nickname":"张三","phone":"13800138000"}
```
响应 `data`：
```json
{"userId":10001,"username":"zhangsan","nickname":"张三","role":"USER"}
```

### 登录
`POST /api/auth/login`

请求参数：
```json
{"username":"zhangsan","password":"Password123"}
```
响应 `data`：
```json
{"token":"eyJhbGciOiJIUzI1NiJ9...","tokenType":"Bearer","expiresIn":7200,"user":{"userId":10001,"username":"zhangsan","nickname":"张三","role":"USER"}}
```

## 资源分类

### 查询分类
`GET /api/category`；请求参数：无。

响应 `data`：
```json
[{"categoryId":1,"name":"研修室","description":"会议与学习空间","status":"ENABLED"}]
```

### 新增分类（管理员）
`POST /api/admin/category`

请求参数：`{"name":"研修室","description":"会议与学习空间"}`
响应 `data`：`{"categoryId":1,"name":"研修室","description":"会议与学习空间","status":"ENABLED"}`

### 修改分类（管理员）
`PUT /api/admin/categories/{categoryId}`；路径参数：`categoryId`（long，必填）。

请求参数：`{"name":"大型研修室","description":"可容纳 20 人"}`
响应 `data`：同新增分类。

### 启用/停用分类（管理员）
`PATCH /api/admin/categories/{categoryId}/status`；路径参数：`categoryId`（long，必填）。

请求参数：`{"status":"DISABLED"}`
响应 `data`：`{"categoryId":1,"status":"DISABLED"}`

## 资源管理

### 资源列表
`GET /api/resources`

请求参数：`categoryId`（long，可选）、`keyword`（string，可选）、`status`（ENABLED/DISABLED，可选）、`page`（integer，可选，默认 1）、`pageSize`（integer，可选，默认 20）。

响应 `data`：
```json
{"records":[{"resourceId":1001,"categoryId":1,"categoryName":"研修室","name":"A101 研修室","location":"1 楼 A101","capacity":12,"description":"配备投影仪","status":"ENABLED"}],"page":1,"pageSize":20,"total":1}
```

### 资源详情
`GET /api/resources/{resourceId}`；路径参数：`resourceId`（long，必填）。
响应 `data`：资源对象，字段同资源列表。

### 新增资源（管理员）
`POST /api/admin/resources`

请求参数：`{"categoryId":1,"name":"A101 研修室","location":"1 楼 A101","capacity":12,"description":"配备投影仪"}`
响应 `data`：资源对象，字段同资源列表。

### 修改资源（管理员）
`PUT /api/admin/resources/{resourceId}`；路径参数：`resourceId`（long，必填）。
请求参数：同新增资源。响应 `data`：资源对象，字段同资源列表。

### 启用/停用资源（管理员）
`PATCH /api/admin/resources/{resourceId}/status`；路径参数：`resourceId`（long，必填）。
请求参数：`{"status":"DISABLED"}`
响应 `data`：`{"resourceId":1001,"status":"DISABLED"}`

## 可预约时间段

### 查询时间段
`GET /api/resources/{resourceId}/slots`

请求参数：`resourceId`（long，路径必填）、`date`（string，必填，`yyyy-MM-dd`）。

响应 `data`：
```json
{"resourceId":1001,"date":"2026-10-08","slots":[{"slotId":90001,"startTime":"09:00:00","endTime":"10:00:00","status":"AVAILABLE","remaining":1},{"slotId":90002,"startTime":"10:00:00","endTime":"11:00:00","status":"FULL","remaining":0}]}
```
`status`：`AVAILABLE` 可预约、`FULL` 已满、`DISABLED` 不可预约、`EXPIRED` 已过期。

## 预约与候补

### 创建预约
`POST /api/reservations`

请求参数：
```json
{"resourceId":1001,"slotId":90001,"date":"2026-10-08","remark":"项目讨论","requestId":"8f6f6d60-2df8-4db8-9f6b-7e1d1f8f1234"}
```
响应 `data`：
```json
{"reservationId":50001,"resourceId":1001,"slotId":90001,"date":"2026-10-08","startTime":"09:00:00","endTime":"10:00:00","status":"CONFIRMED","createdAt":"2026-10-07T10:00:00+08:00"}
```
重复提交同一 `requestId` 返回同一预约；并发冲突或名额已满时返回错误结构，`data` 为 null。

### 加入候补
`POST /api/waitlists`

请求参数：`{"resourceId":1001,"slotId":90002,"date":"2026-10-08","requestId":"c4c2a1b0-91e8-4d1c-8d30-7e1d1f8f5678"}`
响应 `data`：
```json
{"waitlistId":70001,"resourceId":1001,"slotId":90002,"date":"2026-10-08","queueNo":3,"status":"WAITING","createdAt":"2026-10-07T10:01:00+08:00"}
```

### 查询我的预约
`GET /api/reservations/my`

请求参数：`status`（CONFIRMED/CANCELLED/COMPLETED，可选）、`from`（yyyy-MM-dd，可选）、`to`（yyyy-MM-dd，可选）、`page`（integer，可选，默认 1）、`pageSize`（integer，可选，默认 20）。

响应 `data`：
```json
{"records":[{"reservationId":50001,"resourceId":1001,"resourceName":"A101 研修室","slotId":90001,"date":"2026-10-08","startTime":"09:00:00","endTime":"10:00:00","status":"CONFIRMED","createdAt":"2026-10-07T10:00:00+08:00","cancelledAt":null}],"page":1,"pageSize":20,"total":1}
```

### 取消预约
`POST /api/reservations/{reservationId}/cancel`；路径参数：`reservationId`（long，必填）。

请求参数：`{"reason":"临时有事"}`
响应 `data`：`{"reservationId":50001,"status":"CANCELLED","cancelledAt":"2026-10-07T10:20:00+08:00","promotedReservationId":50002}`
无候补递补时 `promotedReservationId` 为 null；递补成功时为新预约 ID。

### 查询我的候补
`GET /api/waitlists/my`

请求参数：`status`（WAITING/PROMOTED/CANCELLED/EXPIRED，可选）、`page`（integer，可选，默认 1）、`pageSize`（integer，可选，默认 20）。

响应 `data`：
```json
{"records":[{"waitlistId":70001,"resourceId":1001,"resourceName":"A101 研修室","slotId":90002,"date":"2026-10-08","queueNo":1,"status":"PROMOTED","reservationId":50002,"createdAt":"2026-10-07T10:01:00+08:00","promotedAt":"2026-10-07T10:20:01+08:00"}],"page":1,"pageSize":20,"total":1}
```

## 管理员预约与排队查询

### 查询预约（管理员）
`GET /api/admin/reservations`

请求参数：`resourceId`（long，可选）、`userId`（long，可选）、`date`（yyyy-MM-dd，可选）、`status`（CONFIRMED/CANCELLED/COMPLETED，可选）、`page`（integer，可选，默认 1）、`pageSize`（integer，可选，默认 20）。

响应 `data`：
```json
{"records":[{"reservationId":50001,"userId":10001,"username":"zhangsan","resourceId":1001,"resourceName":"A101 研修室","slotId":90001,"date":"2026-10-08","startTime":"09:00:00","endTime":"10:00:00","status":"CONFIRMED","createdAt":"2026-10-07T10:00:00+08:00"}],"page":1,"pageSize":20,"total":1}
```

### 查询候补队列（管理员）
`GET /api/admin/waitlists`

请求参数：`resourceId`（long，可选）、`slotId`（long，可选）、`date`（yyyy-MM-dd，可选）、`status`（WAITING/PROMOTED/CANCELLED/EXPIRED，可选）、`page`（integer，可选，默认 1）、`pageSize`（integer，可选，默认 20）。

响应 `data`：
```json
{"records":[{"waitlistId":70001,"queueNo":1,"userId":10001,"username":"zhangsan","resourceId":1001,"resourceName":"A101 研修室","slotId":90002,"date":"2026-10-08","status":"WAITING","createdAt":"2026-10-07T10:01:00+08:00","promotedAt":null,"reservationId":null}],"page":1,"pageSize":20,"total":1}
```

### 查询时段预约/候补汇总（管理员）
`GET /api/admin/resources/{resourceId}/slots/{slotId}/summary`

请求参数：`resourceId`（long，路径必填）、`slotId`（long，路径必填）、`date`（yyyy-MM-dd，必填）。
响应 `data`：`{"resourceId":1001,"slotId":90002,"date":"2026-10-08","capacity":1,"reserved":1,"remaining":0,"reservationCount":1,"waitingCount":3,"reservations":[],"waitlists":[]}`

## 状态与错误响应

- 预约状态：`CONFIRMED` 已确认、`CANCELLED` 已取消、`COMPLETED` 已完成。
- 候补状态：`WAITING` 排队中、`PROMOTED` 已递补、`CANCELLED` 已取消、`EXPIRED` 已失效。
- 错误响应示例：`{"code":400,"message":"预约时间段已被占用","data":null,"traceId":"a1b2c3d4"}`。参数错误、并发冲突及业务失败均使用统一响应结构。
