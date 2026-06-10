#!/bin/sh

# 管理端机（10.27.0.92）：gateway/auth/system/gen/job/file/monitor/yianlian/nginx
# 在本目录执行：sh deploy.sh [up|stop|rm|port]
# 注意：需先确保中间件机（91）已就绪。

usage() {
	echo "Usage: sh deploy.sh [up|stop|rm|port]"
	echo "  up     启动管理端全部服务（host 网络）"
	echo "  stop   停止"
	echo "  rm     删除容器"
	echo "  port   开放本机防火墙端口"
	exit 1
}

# 创建日志/上传目录（Java 容器默认 root 可写；nginx 非 root 须放宽权限）
prep(){
	mkdir -p nginx/logs
	mkdir -p ruoyi/uploadPath
	mkdir -p ruoyi/gateway/logs ruoyi/auth/logs
	mkdir -p ruoyi/modules/system/logs ruoyi/modules/gen/logs
	mkdir -p ruoyi/modules/job/logs ruoyi/modules/file/logs
	mkdir -p ruoyi/modules/yianlian/logs
	mkdir -p ruoyi/visual/monitor/logs
	chmod -R 777 nginx/logs ruoyi/uploadPath 2>/dev/null || true
	find ruoyi -type d -name logs -exec chmod 777 {} \; 2>/dev/null || true
}

up(){
	prep
	docker-compose up -d --build
}

stop(){
	docker-compose stop
}

rm(){
	docker-compose rm -f
}

port(){
	firewall-cmd --add-port=80/tcp --permanent
	firewall-cmd --add-port=8080/tcp --permanent
	firewall-cmd --add-port=9100/tcp --permanent
	firewall-cmd --add-port=9200/tcp --permanent
	firewall-cmd --add-port=9201/tcp --permanent
	firewall-cmd --add-port=9202/tcp --permanent
	firewall-cmd --add-port=9203/tcp --permanent
	firewall-cmd --add-port=9205/tcp --permanent
	firewall-cmd --add-port=9300/tcp --permanent
	firewall-cmd --reload
}

case "$1" in
"up")
	up
;;
"stop")
	stop
;;
"rm")
	rm
;;
"port")
	port
;;
*)
	usage
;;
esac
