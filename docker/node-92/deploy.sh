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

up(){
	docker-compose up -d --build
}

stop(){
	docker-compose stop
}

rm(){
	docker-compose rm
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
