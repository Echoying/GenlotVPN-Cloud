-- ---------------
-- VPN应用组表
-- ------------------
drop table if exists vpn_service_group;
create table vpn_service_group (
  id              bigint(20)    not null auto_increment  comment '主键ID',
  parent_id       bigint(20)    default 0          comment '父应用组ID',
  ancestors       varchar(200)  default ''               comment '祖级列表',
  app_id        varchar(64)   not null          comment '关联线路ID',
  yianlian_key    varchar(100)  default null             comment '易安联应用组key',
  group_name      varchar(100)  not null             comment '应用组名称',
  order_num       int(4)        default 0                comment '显示顺序',
  description     varchar(500)  default null             comment '描述',
  status          char(1)    default '0'           comment '状态（0正常 1停用）',
  del_flag        char(1)       default '0'              comment '删除标志（0存在 2删除）',
  create_by       varchar(64)   default ''             comment '创建者',
  create_time     datetime                       comment '创建时间',
  update_by       varchar(64)   default ''               comment '更新者',
  update_time     datetime                             comment '更新时间',
  primary key (id)
) engine=innodb auto_increment=100 comment = 'VPN应用组表';
