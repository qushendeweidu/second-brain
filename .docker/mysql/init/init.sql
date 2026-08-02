create database if not exists second_brain;

use second_brain;

create table if not exists user
(
    id          bigint primary key comment '主键id',
    username    varchar(255) not null comment '用户名',
    password    varchar(255) not null comment '密码',
    nickname    varchar(255) not null comment '用户昵称',
    email       varchar(255) not null comment '邮箱',
    phone       varchar(255) not null comment '电话',
    status      tinyint default 1 comment '用户状态 1:正常 0:禁用',
    create_time timestamp default current_timestamp comment '创建时间',
    update_time timestamp default current_timestamp on update current_timestamp comment '更新时间'
) comment ='用户表';

create table if not exists note
(
    id          bigint primary key comment '主键id',
    title       varchar(255) not null comment '标题',
    content     longtext     not null comment '内容',
    create_time timestamp default current_timestamp comment '创建时间',
    update_time timestamp default current_timestamp on update current_timestamp comment '更新时间'
) comment ='笔记表';

create table if not exists user_role
(
    id          bigint primary key comment '主键id',
    user_id     bigint not null comment '用户id',
    roles       JSON      DEFAULT NULL COMMENT '角色字段列表',
    permissions JSON      DEFAULT NULL COMMENT '权限字段列表',
    create_time timestamp default current_timestamp comment '创建时间',
    update_time timestamp default current_timestamp on update current_timestamp comment '更新时间'
);

-- 用户数据
INSERT INTO user (id, username, password, nickname, email, phone,status)
VALUES (1, 'admin', '123456', '超级管理员', 'admin@example.com', '13800000001',1),
       (2, 'zhangsan', '123456', '张三', 'zhangsan@example.com', '13800000002',1),
       (3, 'lisi', '123456', '李四', 'lisi@example.com', '13800000003',1);


-- 笔记数据
INSERT INTO note (id, title, content)
VALUES (1,
        'Spring Boot学习笔记',
        'Spring Boot是目前Java开发中最主流的框架之一，能够快速搭建Web应用。'),
       (2,
        'Redis总结',
        'Redis常用于缓存、分布式锁、消息队列、排行榜等业务场景。'),
       (3,
        'MySQL索引',
        'B+Tree索引能够提高查询效率，合理建立联合索引非常重要。');


-- 用户角色数据
INSERT INTO user_role (id,
                       user_id,
                       roles,
                       permissions)
VALUES (1,
        1,
        JSON_ARRAY('ADMIN', 'USER'),
        JSON_ARRAY(
                'user:add',
                'user:update',
                'user:delete',
                'note:add',
                'note:update',
                'note:delete'
        )),
       (2,
        2,
        JSON_ARRAY('USER'),
        JSON_ARRAY(
                'note:add',
                'note:update'
        )),
       (3,
        3,
        JSON_ARRAY('GUEST'),
        JSON_ARRAY(
                'note:view'
        ));



