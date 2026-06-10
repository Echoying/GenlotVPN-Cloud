#!/bin/sh

# 复制项目的文件到对应 docker 路径（三机拆分后产物分发到 node-9x 子目录），便于一键生成镜像。
usage() {
	echo "Usage: sh copy.sh"
	exit 1
}


# copy sql -> node-91
echo "begin copy sql "
cp ../sql/ry-cloud.sql ./node-91/mysql/db
cp ../sql/ry-config.sql ./node-91/mysql/db
cp ../sql/quartz.sql ./node-91/mysql/db

# copy html
echo "begin copy html "
cp -r ../ruoyi-ui/dist/** ./node-92/nginx/html/dist
cp -r ../ruoyi-vpn-ui/dist/** ./node-93/ruoyi/vpn/nginx/html/dist

# copy jar -> node-92（管理端）
echo "begin copy ruoyi-gateway "
cp ../ruoyi-gateway/target/ruoyi-gateway.jar ./node-92/ruoyi/gateway/jar

echo "begin copy ruoyi-auth "
cp ../ruoyi-auth/target/ruoyi-auth.jar ./node-92/ruoyi/auth/jar

echo "begin copy ruoyi-visual "
cp ../ruoyi-visual/ruoyi-monitor/target/ruoyi-visual-monitor.jar  ./node-92/ruoyi/visual/monitor/jar

echo "begin copy ruoyi-modules-system "
cp ../ruoyi-modules/ruoyi-system/target/ruoyi-modules-system.jar ./node-92/ruoyi/modules/system/jar

echo "begin copy ruoyi-modules-file "
cp ../ruoyi-modules/ruoyi-file/target/ruoyi-modules-file.jar ./node-92/ruoyi/modules/file/jar

echo "begin copy ruoyi-modules-job "
cp ../ruoyi-modules/ruoyi-job/target/ruoyi-modules-job.jar ./node-92/ruoyi/modules/job/jar

echo "begin copy ruoyi-modules-gen "
cp ../ruoyi-modules/ruoyi-gen/target/ruoyi-modules-gen.jar ./node-92/ruoyi/modules/gen/jar

echo "begin copy ruoyi-modules-yianlian "
cp ../ruoyi-modules/ruoyi-yianlian/target/ruoyi-modules-yianlian.jar ./node-92/ruoyi/modules/yianlian/jar

# copy jar -> node-93（VPN）
echo "begin copy ruoyi-vpn-gateway "
cp ../ruoyi-vpn-gateway/target/ruoyi-vpn-gateway.jar ./node-93/ruoyi/vpn/gateway/jar

echo "begin copy ruoyi-vpn-auth "
cp ../ruoyi-vpn-auth/target/ruoyi-vpn-auth.jar ./node-93/ruoyi/vpn/auth/jar
