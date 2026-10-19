CREATE DATABASE `resourceflow`;
CREATE TABLE `user` (
                        `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
                        `student_number` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '学号或工号',
                        `username` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '登录用户名',
                        `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
                        `email` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
                        `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '加密后的密码',
                        `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'USER' COMMENT '用户角色：USER/ADMIN',
                        `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ENABLED' COMMENT '账号状态：ENABLED/DISABLED',
                        `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
                        `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
                        PRIMARY KEY (`user_id`),
                        UNIQUE KEY `uk_users_username` (`username`),
                        UNIQUE KEY `uk_users_student_number` (`student_number`),
                        UNIQUE KEY `uk_users_phone` (`phone`),
                        UNIQUE KEY `uk_users_email` (`email`),
                        KEY `idx_users_role_status` (`role`,`status`),
                        CONSTRAINT `ck_users_role` CHECK ((`role` in (_utf8mb4'USER',_utf8mb4'ADMIN'))),
                        CONSTRAINT `ck_users_status` CHECK ((`status` in (_utf8mb4'ENABLED',_utf8mb4'DISABLED')))
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
CREATE TABLE `resource` (
                            `resource_id` bigint NOT NULL AUTO_INCREMENT COMMENT '研修室 ID',
                            `category_id` bigint NOT NULL COMMENT '所属分类 ID',
                            `name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '研修室名称',
                            `location` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '研修室位置',
                            `capacity` int NOT NULL COMMENT '研修室物理容纳人数',
                            `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '研修室说明',
                            `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
                            `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
                            `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
                            PRIMARY KEY (`resource_id`),
                            UNIQUE KEY `uk_resources_name_location` (`name`,`location`),
                            KEY `idx_resources_category_status` (`category_id`,`status`),
                            KEY `idx_resources_status_name` (`status`,`name`),
                            CONSTRAINT `ck_resources_capacity` CHECK ((`capacity` > 0)),
                            CONSTRAINT `ck_resources_status` CHECK ((`status` in (_utf8mb4'ENABLED',_utf8mb4'DISABLED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研修室表';
CREATE TABLE `category` (
                            `category_id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类 ID',
                            `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
                            `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类说明',
                            `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
                            `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
                            `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
                            PRIMARY KEY (`category_id`),
                            UNIQUE KEY `uk_categories_name` (`name`),
                            KEY `idx_categories_status` (`status`),
                            CONSTRAINT `ck_categories_status` CHECK ((`status` in (_utf8mb4'ENABLED',_utf8mb4'DISABLED')))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研修室分类表';
-- resourceflow.reservation 定义

CREATE TABLE `reservation` (
                               `reservation_id` bigint NOT NULL AUTO_INCREMENT COMMENT '预约 ID',
                               `user_id` bigint NOT NULL COMMENT '用户 ID',
                               `slot_id` bigint NOT NULL COMMENT '时间段 ID',
                               `source_waitlist_id` bigint DEFAULT NULL COMMENT '递补来源候补记录 ID',
                               `request_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '客户端幂等请求号',
                               `active_key` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '活跃预约唯一键',
                               `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '预约备注',
                               `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CONFIRMED' COMMENT '预约状态',
                               `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '预约创建时间',
                               `cancelled_at` datetime(3) DEFAULT NULL COMMENT '取消时间',
                               `completed_at` datetime(3) DEFAULT NULL COMMENT '完成时间',
                               `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
                               PRIMARY KEY (`reservation_id`),
                               UNIQUE KEY `uk_reservations_user_request` (`user_id`,`request_id`),
                               UNIQUE KEY `uk_reservations_active_key` (`active_key`),
                               UNIQUE KEY `uk_reservations_source_waitlist` (`source_waitlist_id`),
                               KEY `idx_reservations_conflict_user_slot_status` (`user_id`,`slot_id`,`status`,`created_at`),
                               KEY `idx_reservations_slot_status_created` (`slot_id`,`status`,`created_at`),
                               KEY `idx_reservations_user_status_created` (`user_id`,`status`,`created_at`),
                               CONSTRAINT `ck_reservations_status` CHECK ((`status` in (_utf8mb4'CONFIRMED',_utf8mb4'CANCELLED',_utf8mb4'COMPLETED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研修室预约表';
-- resourceflow.resource_slot 定义

CREATE TABLE `resource_slot` (
                                 `slot_id` bigint NOT NULL AUTO_INCREMENT COMMENT '时间段 ID',
                                 `resource_id` bigint NOT NULL COMMENT '研修室 ID',
                                 `slot_date` date NOT NULL COMMENT '预约日期',
                                 `start_time` time NOT NULL COMMENT '开始时间',
                                 `end_time` time NOT NULL COMMENT '结束时间',
                                 `booking_capacity` int NOT NULL DEFAULT '1' COMMENT '该时间段允许的预约数量',
                                 `reserved_count` int NOT NULL DEFAULT '0' COMMENT '当前已预约数量',
                                 `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
                                 `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态',
                                 `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
                                 `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
                                 PRIMARY KEY (`slot_id`),
                                 UNIQUE KEY `uk_resource_slot_time` (`resource_id`,`slot_date`,`start_time`,`end_time`),
                                 KEY `idx_slots_resource_date_status` (`resource_id`,`slot_date`,`status`),
                                 KEY `idx_slots_date_status` (`slot_date`,`status`),
                                 CONSTRAINT `ck_slots_booking_capacity` CHECK ((`booking_capacity` > 0)),
                                 CONSTRAINT `ck_slots_reserved_count` CHECK (((`reserved_count` >= 0) and (`reserved_count` <= `booking_capacity`))),
                                 CONSTRAINT `ck_slots_status` CHECK ((`status` in (_utf8mb4'AVAILABLE',_utf8mb4'FULL',_utf8mb4'DISABLED',_utf8mb4'EXPIRED'))),
                                 CONSTRAINT `ck_slots_time_range` CHECK ((`end_time` > `start_time`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研修室预约时间段表';

-- resourceflow.waitlist 定义

CREATE TABLE `waitlist` (
                            `waitlist_id` bigint NOT NULL AUTO_INCREMENT COMMENT '候补记录 ID',
                            `user_id` bigint NOT NULL COMMENT '用户 ID',
                            `resource_id` bigint NOT NULL COMMENT '研修室 ID',
                            `slot_id` bigint NOT NULL COMMENT '时间段 ID',
                            `request_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '客户端幂等请求号',
                            `active_key` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '活跃候补唯一键',
                            `queue_no` int NOT NULL COMMENT '候补序号',
                            `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'WAITING' COMMENT '候补状态',
                            `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '加入候补时间',
                            `promoted_at` datetime(3) DEFAULT NULL COMMENT '递补成功时间',
                            `cancelled_at` datetime(3) DEFAULT NULL COMMENT '取消候补时间',
                            `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
                            PRIMARY KEY (`waitlist_id`),
                            UNIQUE KEY `uk_waitlists_user_request` (`user_id`,`request_id`),
                            UNIQUE KEY `uk_waitlists_slot_queue` (`slot_id`,`queue_no`),
                            UNIQUE KEY `uk_waitlists_active_key` (`active_key`),
                            KEY `idx_waitlists_resource_slot_status_queue` (`resource_id`,`slot_id`,`status`,`queue_no`,`created_at`),
                            KEY `idx_waitlists_slot_status_queue` (`slot_id`,`status`,`queue_no`,`created_at`),
                            KEY `idx_waitlists_user_status_created` (`user_id`,`status`,`created_at`),
                            CONSTRAINT `ck_waitlists_queue_no` CHECK ((`queue_no` > 0)),
                            CONSTRAINT `ck_waitlists_status` CHECK ((`status` in (_utf8mb4'WAITING',_utf8mb4'PROMOTED',_utf8mb4'CANCELLED',_utf8mb4'EXPIRED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研修室候补队列表';
-- resourceflow.operation_log 定义

CREATE TABLE `operation_log` (
                                 `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志 ID',
                                 `operator_user_id` bigint DEFAULT NULL COMMENT '操作用户 ID，系统任务可为空',
                                 `operator_username` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作用户名快照',
                                 `module` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务模块',
                                 `action` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作类型',
                                 `target_type` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作对象类型',
                                 `target_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作对象 ID',
                                 `request_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务请求 ID',
                                 `trace_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '链路追踪 ID',
                                 `result` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SUCCESS' COMMENT '操作结果',
                                 `detail_json` json DEFAULT NULL COMMENT '操作详情',
                                 `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户端 IP',
                                 `user_agent` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户端 User-Agent',
                                 `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '操作时间',
                                 PRIMARY KEY (`log_id`),
                                 KEY `idx_logs_operator_time` (`operator_user_id`,`created_at`),
                                 KEY `idx_logs_module_action_time` (`module`,`action`,`created_at`),
                                 KEY `idx_logs_target_time` (`target_type`,`target_id`,`created_at`),
                                 KEY `idx_logs_trace_id` (`trace_id`),
                                 KEY `idx_logs_request_id` (`request_id`),
                                 CONSTRAINT `ck_logs_result` CHECK ((`result` in (_utf8mb4'SUCCESS',_utf8mb4'FAILURE')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统操作日志表';