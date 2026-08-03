#!/bin/sh

# VPN 机（10.27.0.93）：vpn-auth（桌面端 TCP 9443 / HTTP 9400）
# 在本目录执行：sh deploy.sh [up|stop|rm|port]
# 注意：需先确保中间件机（91）已就绪。

usage() {
	echo "Usage: sh deploy.sh [up|stop|rm|port]"
	echo "  up     启动 VPN 全部服务（host 网络）"
	echo "  stop   停止"
	echo "  rm     删除容器"
	echo "  port   开放本机防火墙端口"
	exit 1
}

# 创建日志目录
prep(){
	mkdir -p ruoyi/vpn/auth/logs
	find ruoyi/vpn -type d -name logs -exec chmod 777 {} \; 2>/dev/null || true
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
	firewall-cmd --add-port=9400/tcp --permanent
	firewall-cmd --add-port=9443/tcp --permanent
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
