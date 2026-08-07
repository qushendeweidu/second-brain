# 项目地图

## 技术栈

- 后端：Java 21、Spring Boot 4.0.6、Spring Security 7.0.6、MyBatis-Plus 3.5.17、MySQL、Redis、JJWT 0.12.6。

## 目录

- `backend/`：Spring Boot 单体后端服务。
- `.docker/`：本地容器相关配置。
- `docker-compose.yml`：本地基础设施编排。

## 业务规则

- 创建用户时同步创建默认 `USER` 角色、`user:read` 权限和用户资料。
- 删除用户时同步删除用户角色、用户资料和 Redis 登录会话。
- 用户新增、更新、删除和角色管理仅管理员可操作；用户资料仅本人或管理员可操作。

## 说明

- 当前目录未检测到 Git 元数据。
- 后端技术细节以 `backend/pom.xml` 和当前源码为准。
