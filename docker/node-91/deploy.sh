#!/bin/sh

# 中间件机（10.27.0.91）：MySQL / Redis / Nacos
# 在本目录执行：sh deploy.sh [up|stop|rm|port]

usage() {
	echo "Usage: sh deploy.sh [up|stop|rm|port]"
	echo "  up     启动 mysql/redis/nacos（host 网络）"
	echo "  stop   停止"
	echo "  rm     删除容器"
	echo "  port   开放本机防火墙端口"
	exit 1
}

# 创建数据/日志目录并修正权限（非 root 容器用户对挂载目录须可写）
prep(){
	mkdir -p redis/data redis/logs mysql/data mysql/logs nacos/logs
	# redis 镜像运行用户 redis(999)
	chmod 777 redis/data redis/logs 2>/dev/null || true
	chown 999:999 redis/data redis/logs 2>/dev/null || true
	# nacos 镜像运行用户 nacos(1000)
	chmod 777 nacos/logs 2>/dev/null || true
	chown 1000:1000 nacos/logs 2>/dev/null || true
	# mysql 数据目录（entrypoint 会初始化，预先放宽避免首次启动异常）
	chmod 777 mysql/data mysql/logs 2>/dev/null || true
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

# 对 92/93 开放：MySQL / Redis / Nacos(含 gRPC 9848/9849)
port(){
	firewall-cmd --add-port=3306/tcp --permanent
	firewall-cmd --add-port=6379/tcp --permanent
	firewall-cmd --add-port=8848/tcp --permanent
	firewall-cmd --add-port=9848/tcp --permanent
	firewall-cmd --add-port=9849/tcp --permanent
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
