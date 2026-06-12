-- vpn_logininfor 增加登录用途字段（列长 100 预留扩展，当前业务校验上限 50 字）
ALTER TABLE vpn_logininfor
    ADD COLUMN login_purpose varchar(100) DEFAULT '' COMMENT '登录用途' AFTER user_name;
