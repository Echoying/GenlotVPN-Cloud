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

up(){
	docker-compose up -d --build
}

stop(){
	docker-compose stop
}

rm(){
	docker-compose rm
}

# 对 92/93 开放：MySQL / Redis / Nacos(含 gRPC 9848/9849)
port(){
	firewall-cmd --add-port=3306/tcp --permanent
	firewall-cmd --add-port=6379/tcp --permanent
	firewall-cmd --add-port=8848/tcp --permanent
	firewall-cmd --add-port=9848/tcp --permanent
	firewall-cmd --add-port=9849/tcp --permanent
	service firewalld restart
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
