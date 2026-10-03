# ResourceFlow

高并发资源预约与候补排队平台。

## 简介

面向研修室场景，解决瞬时抢约超卖、重复提交、
取消后自动补位、候补公平排队等问题。

## 技术栈

- Java 21 + Spring Boot 3
- Spring Security + JWT
- MySQL 8 + MyBatis-Plus
- Redis 8 + Redisson
- RocketMQ / Redis Stream
- Vue3 + Vite + Element Plus
- Docker Compose + Nginx
- JMeter

## 核心功能

- 用户注册登录、JWT 鉴权
- 资源与时间片管理
- 高并发预约、取消
- Redis Lua 原子预扣，防超卖
- 幂等提交，防重复预约
- 候补 FIFO 排队
- 取消后自动递补、超时释放
- 管理端预约/候补查询
- JMeter 压测报告

## 快速开始

### 环境要求

- JDK 21
- Maven 3.8+
- Docker / Docker Compose

### 启动依赖

```bash
docker compose up -d
