-- ---------------
-- VPN应用表
-- ---------------
drop table if exists vpn_service;
create table vpn_service (
  id                    bigint(20)    not null auto_increment  comment '主键ID',
  app_id              varchar(64)   not null                 comment '关联线路ID',
  service_group_id      bigint(20)    not null            comment '关联应用组ID(本地)',
  yianlian_key          varchar(100)  default null          comment '易安联应用key',
  name              varchar(200)  not null                 comment '应用名称',
  type                varchar(10)   not null default 'web'   comment '应用类型 web/cs',
  url                   varchar(500)  not null              comment '应用地址',
  browser_type       varchar(10)   not null default '0'     comment '支持浏览器 0默认 1IE 2非IE 3国密',
  web_port              varchar(10)   not null              comment '应用服务端口',
  credit_level_id       varchar(50)   not null default '000000002' comment '等级Id',
  icon               varchar(200)  not null default '/diy/default-house.svg' comment '应用图标',
  if_show               tinyint(1)    not null default 1       comment '是否展示应用',
  second_auth_enable    varchar(2)    not null default '1'     comment '是否启用二次认证 0启用 1禁用',
  if_self_apply         tinyint(1)    not null default 1       comment '是否允许自助申请',
  if_s_alarm_tip        tinyint(1)    not null default 0       comment '是否开启告警',
  if_custom_alarm_content tinyint(1)  not null default 0       comment '是否自定义告警内容',
  custom_alarm_content  varchar(500)  default null             comment '自定义告警内容',
  create_type           varchar(10)   not null default '3'     comment '创建类型 3三方导入',
  description        varchar(500)  default null             comment '描述',
  status                char(1)       default '0'              comment '状态（0正常 1停用）',
  del_flag              char(1)       default '0'              comment '删除标志（0存在 2删除）',
  create_by             varchar(64)   default ''               comment '创建者',
  create_time           datetime                               comment '创建时间',
  update_by             varchar(64)   default ''               comment '更新者',
  update_time           datetime                       comment '更新时间',
  req_cs_server_vos     text          default null             comment 'CS服务器配置JSON',
  primary key (id)
) engine=innodb auto_increment=100 comment = 'VPN应用表';
