#!/usr/bin/env python3
"""项目数据库初始化脚本导出工具。

连接测试库，导出各库的完整初始化脚本（表结构 + 框架种子数据）：
  - ry-cloud  -> ry-cloud.sql  （排除 qrtz_* 表，由 quartz.sql 单独管理；业务/日志表清空）
  - ry-config -> ry-config.sql （Nacos 配置，结构 + 全部数据）
  - 若存在独立 seata 库 -> seata.sql（仅结构）

用法：python export_init_db.py
"""
from pathlib import Path
import sys
import pymysql

DB_HOST = "10.9.2.177"
DB_PORT = 3306
DB_USER = "root"
DB_PASSWORD = "root123456"

OUT_DIR = Path(__file__).resolve().parent

# 目标库 -> 输出文件名
TARGET_DBS = {
    "ry-cloud": "ry-cloud.sql",
    "ry-config": "ry-config.sql",
    "seata": "seata.sql",
    "ry-seata": "seata.sql",
}

# 仅导出结构、清空数据的表（业务 / 授权 / 映射 / 日志 / 代码生成临时）
EMPTY_TABLES = {
    # VPN 业务
    "vpn_user", "vpn_dept", "vpn_role", "vpn_user_role",
    "vpn_service", "vpn_service_group",
    # 易安联授权与映射
    "yal_dept_auth", "yal_role_auth", "yal_user_auth",
    "vpn_dept_yianlian_mapping", "vpn_role_yianlian_mapping", "vpn_user_yianlian_mapping",
    # 线路配置（含密钥）
    "line_app",
    # 日志
    "sys_logininfor", "sys_oper_log", "vpn_logininfor", "sys_job_log",
    # 代码生成临时
    "gen_table", "gen_table_column",
}

# 整库仅导出结构（不导任何数据）
STRUCTURE_ONLY_DBS = {"seata", "ry-seata"}

INSERT_BATCH = 200


def is_excluded_table(db: str, table: str) -> bool:
    """ry-cloud 中的 qrtz_* 表由 quartz.sql 单独管理，整表排除。"""
    if db == "ry-cloud" and table.lower().startswith("qrtz_"):
        return True
    return False


def escape_value(conn, v):
    """生成 SQL 字面量。

    blob/二进制列（bytes/bytearray）改用 0x... 十六进制字面量输出：
    pymysql 对 bytes 的 escape 会经 surrogateescape 解码，导致原始字节落入
    单引号字符串，遇到含特殊字节的大字段（如 sys_notice.notice_content 的 HTML）
    时容易丢行/截断；十六进制字面量为纯 ASCII 单行，安全可重入。
    """
    if isinstance(v, (bytes, bytearray)):
        if len(v) == 0:
            return "''"  # MySQL 不接受空的 0x 字面量，空 blob 用空串
        return "0x" + bytes(v).hex()
    return conn.escape(v)


def dump_database(conn, db: str, out_path: Path) -> None:
    cur = conn.cursor()
    cur.execute("USE `%s`" % db)
    cur.execute("SHOW FULL TABLES WHERE Table_type = 'BASE TABLE'")
    tables = [r[0] for r in cur.fetchall()]

    structure_only = db in STRUCTURE_ONLY_DBS
    lines = []
    lines.append("-- ----------------------------------------------------")
    lines.append("-- 数据库初始化脚本: %s" % db)
    lines.append("-- 由 sql/export_init_db.py 从测试库导出" )
    lines.append("-- ----------------------------------------------------")
    lines.append("SET NAMES utf8mb4;")
    lines.append("SET FOREIGN_KEY_CHECKS = 0;")
    lines.append("")
    lines.append("CREATE DATABASE IF NOT EXISTS `%s` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" % db)
    lines.append("USE `%s`;" % db)
    lines.append("")

    summary = []
    for t in tables:
        if is_excluded_table(db, t):
            summary.append((t, "skip(qrtz)"))
            continue

        cur.execute("SHOW CREATE TABLE `%s`" % t)
        ddl = cur.fetchone()[1]
        lines.append("-- ---------------------------- 表结构: %s ----------------------------" % t)
        lines.append("DROP TABLE IF EXISTS `%s`;" % t)
        lines.append(ddl + ";")
        lines.append("")

        if structure_only or t in EMPTY_TABLES:
            summary.append((t, "structure-only"))
            continue

        cur.execute("SELECT COUNT(*) FROM `%s`" % t)
        total = cur.fetchone()[0]
        if total == 0:
            summary.append((t, "empty"))
            continue

        # 列名
        cur.execute("SELECT * FROM `%s`" % t)
        cols = [d[0] for d in cur.description]
        col_sql = ", ".join("`%s`" % c for c in cols)

        lines.append("-- 数据: %s (%d 行)" % (t, total))
        rows = cur.fetchall()
        for i in range(0, len(rows), INSERT_BATCH):
            chunk = rows[i:i + INSERT_BATCH]
            values = []
            for row in chunk:
                vals = ", ".join(escape_value(conn, v) for v in row)
                values.append("(%s)" % vals)
            stmt = "INSERT INTO `%s` (%s) VALUES\n%s;" % (t, col_sql, ",\n".join(values))
            lines.append(stmt)
        lines.append("")
        summary.append((t, "%d rows" % total))

    lines.append("SET FOREIGN_KEY_CHECKS = 1;")
    lines.append("")

    # 部分列可能含被 pymysql 以 surrogateescape 解码的原始字节，
    # 写回时同样用 surrogateescape 以保持字节级一致，避免 lone surrogate 编码报错。
    with open(out_path, "w", encoding="utf-8", errors="surrogateescape", newline="\n") as f:
        f.write("\n".join(lines))
    print("[OK] %s -> %s (%d 表)" % (db, out_path.name, len(tables)), file=sys.stderr)
    for t, info in summary:
        print("     - %-40s %s" % (t, info), file=sys.stderr)


def main() -> int:
    conn = pymysql.connect(
        host=DB_HOST, port=DB_PORT, user=DB_USER, password=DB_PASSWORD,
        charset="utf8mb4", connect_timeout=10,
    )
    try:
        cur = conn.cursor()
        cur.execute("SHOW DATABASES")
        existing = {r[0] for r in cur.fetchall()}

        done_files = set()
        for db, fname in TARGET_DBS.items():
            if db not in existing or fname in done_files:
                continue
            dump_database(conn, db, OUT_DIR / fname)
            done_files.add(fname)

        if not done_files:
            print("未找到任何目标库，请检查连接与库名。", file=sys.stderr)
            return 1
    finally:
        conn.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
