-- 新增AES加密密码字段，用于同步易安联时传递旧密码
ALTER TABLE `vpn_user` ADD COLUMN `encrypted_pwd` varchar(255) DEFAULT NULL COMMENT 'AES加密密码' AFTER `password`;
