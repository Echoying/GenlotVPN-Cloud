#!/bin/sh

# 复制项目的文件到对应 docker 路径（三机拆分后产物分发到 node-9x 子目录），便于一键生成镜像。
usage() {
	echo "Usage: sh copy.sh"
	exit 1
}

# 校验 JAR 是否为 Java 8 编译（class major version 52）
check_jar_java8() {
	jar_path="$1"
	if [ ! -f "$jar_path" ]; then
		echo "错误: 找不到 JAR: $jar_path"
		exit 1
	fi
	python - "$jar_path" <<'PY' || exit 1
import struct, sys, zipfile
path = sys.argv[1]
with zipfile.ZipFile(path) as z:
    names = [n for n in z.namelist() if n.endswith("Application.class") and "BOOT-INF/classes" in n]
    if not names:
        print(f"错误: {path} 中未找到 Application.class")
        sys.exit(1)
    major = struct.unpack(">H", z.read(names[0])[6:8])[0]
    if major != 52:
        print(f"错误: {path} 为 Java class {major}，需要 Java 8（52）。请用 JDK 8 执行 mvn package。")
        sys.exit(1)
    print(f"OK Java8: {path}")
PY
}

# 复制前端 dist：删除旧 dist.tar → 同步 dist 目录 → 打包为 dist.tar
copy_html_dist() {
	src_dir="$1"
	html_dir="$2"
	dist_dir="$html_dir/dist"
	tar_path="$html_dir/dist.tar"

	if [ ! -d "$src_dir" ]; then
		echo "错误: 源目录不存在: $src_dir"
		exit 1
	fi
	if [ -f "$tar_path" ]; then
		echo "remove old $tar_path"
		rm -f "$tar_path"
	fi
	rm -rf "$dist_dir"
	mkdir -p "$dist_dir"
	cp -r "$src_dir"/. "$dist_dir"/
	echo "pack $tar_path"
	tar -cf "$tar_path" -C "$html_dir" dist
}

# copy sql -> node-91
echo "begin copy sql "
cp ../sql/ry-cloud.sql ./node-91/mysql/db
cp ../sql/ry-config.sql ./node-91/mysql/db
cp ../sql/quartz.sql ./node-91/mysql/db

# copy html
echo "begin copy html "
copy_html_dist "../ruoyi-ui/dist" "./node-92/nginx/html"
echo "begin copy vpn html "
copy_html_dist "../ruoyi-vpn-ui/dist" "./node-93/ruoyi/vpn/nginx/html"

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

echo "begin check jar java version (must be 52 / Java 8)"
check_jar_java8 ./node-92/ruoyi/gateway/jar/ruoyi-gateway.jar
check_jar_java8 ./node-92/ruoyi/auth/jar/ruoyi-auth.jar
check_jar_java8 ./node-92/ruoyi/visual/monitor/jar/ruoyi-visual-monitor.jar
check_jar_java8 ./node-92/ruoyi/modules/system/jar/ruoyi-modules-system.jar
check_jar_java8 ./node-92/ruoyi/modules/file/jar/ruoyi-modules-file.jar
check_jar_java8 ./node-92/ruoyi/modules/job/jar/ruoyi-modules-job.jar
check_jar_java8 ./node-92/ruoyi/modules/gen/jar/ruoyi-modules-gen.jar
check_jar_java8 ./node-92/ruoyi/modules/yianlian/jar/ruoyi-modules-yianlian.jar
check_jar_java8 ./node-93/ruoyi/vpn/gateway/jar/ruoyi-vpn-gateway.jar
check_jar_java8 ./node-93/ruoyi/vpn/auth/jar/ruoyi-vpn-auth.jar
echo "all jars are Java 8"
