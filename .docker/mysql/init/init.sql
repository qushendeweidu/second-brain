create database if not exists second_brain;

use second_brain;

create table if not exists user
(
    id          bigint primary key comment '主键id',
    username    varchar(255) not null comment '用户名',
    password    varchar(255) not null comment '密码',
    nickname    varchar(255) default null comment '用户昵称',
    email       varchar(255) default null comment '邮箱',
    phone       varchar(255) default null comment '电话',
    status      tinyint default 1 comment '用户状态 1:正常 0:禁用',
    create_time timestamp default current_timestamp comment '创建时间',
    update_time timestamp default current_timestamp on update current_timestamp comment '更新时间'
    ) comment ='用户表';

-- 用户数据
INSERT INTO user (id, username, password, nickname, email, phone, status)
VALUES (1, 'admin', '$2a$12$t4P9MYX7l.aXIP5A1Xgd4OkMUFqNXcd7V7lS8tCQxMMYYFNeokoji', '超级管理员', 'admin@example.com', '13800000001', 1),
       (2, 'zhangsan', '$2a$12$t4P9MYX7l.aXIP5A1Xgd4OkMUFqNXcd7V7lS8tCQxMMYYFNeokoji', '张三', 'zhangsan@example.com', '13800000002', 1),
       (3, 'lisi', '$2a$12$t4P9MYX7l.aXIP5A1Xgd4OkMUFqNXcd7V7lS8tCQxMMYYFNeokoji', '李四', 'lisi@example.com', '13800000003', 1);


create table if not exists user_role
(
    id          bigint primary key comment '主键id',
    user_id     bigint not null comment '用户id',
    roles       JSON      DEFAULT NULL COMMENT '角色字段列表',
    permissions JSON      DEFAULT NULL COMMENT '权限字段列表',
    create_time timestamp default current_timestamp comment '创建时间',
    update_time timestamp default current_timestamp on update current_timestamp comment '更新时间'
) comment ='用户权限表';



-- 用户角色数据
INSERT INTO user_role (id,
                       user_id,
                       roles,
                       permissions)
VALUES (1, 1, JSON_ARRAY('ADMIN', 'USER'), JSON_ARRAY(
        'user:add',
        'user:update',
        'user:delete',
        'note:add',
        'note:update',
        'note:delete')),
       (2, 2, JSON_ARRAY('USER'), JSON_ARRAY('note:add', 'note:update')),
       (3, 3, JSON_ARRAY('GUEST'), JSON_ARRAY('note:view'));


create table if not exists user_profile(
                                           id          bigint primary key comment '主键id',
                                           user_id     bigint not null comment '用户id',
                                           avatar      varchar(255) DEFAULT NULL COMMENT '头像',
    bio         varchar(255) DEFAULT NULL COMMENT '个人简介',
    create_time timestamp default current_timestamp comment '创建时间',
    update_time timestamp default current_timestamp on update current_timestamp comment '更新时间'
    ) comment ='用户资料表';

INSERT INTO user_profile (id, user_id)
VALUES (1, 1),
       (2, 2),
       (3, 3);

create table if not exists user_restrict
(
    id bigint primary key comment '主键id',
    restrict_request bigint not null comment '请求限制次数',
    window_second bigint not null comment '一段限制时间'
) comment = '请求限流配置表';

INSERT INTO user_restrict (id, restrict_request,window_second)
    VALUE (1,100,60);