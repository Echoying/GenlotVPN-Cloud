-- ----------------------------
-- 部门授权表
-- ----------------------------
drop table if exists yal_dept_auth;
create table if not exists yal_dept_auth
(
    id                bigint(20)      not null auto_increment    comment '主键ID',
    dept_id           bigint(20)      default null               comment '部门ID',
    line_id           varchar(64)     default null               comment '线路ID',
    app_group_ids     varchar(1000)   default null               comment '应用组ID列表，逗号分隔',
    app_ids           varchar(1000)   default null               comment '应用ID列表，逗号分隔',
    create_by         varchar(64)     default ''                 comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         varchar(64)     default ''                 comment '更新者',
    update_time       datetime                                   comment '更新时间',
    primary key (id)
) engine=innodb auto_increment=1 comment = '部门授权表';
