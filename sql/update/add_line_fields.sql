-- 为 line_app 表添加新字段
ALTER TABLE line_app
  ADD COLUMN host VARCHAR(255) NOT NULL COMMENT '服务器域名或IP' AFTER url,
  ADD COLUMN srv_port INT NOT NULL COMMENT '服务器端口号' AFTER host,
  ADD COLUMN spa_port INT NOT NULL COMMENT '敲门端口' AFTER srv_port,
  ADD COLUMN spa_key VARCHAR(32) NOT NULL COMMENT '预共享秘钥（MD5加密32位小写）' AFTER spa_port;
