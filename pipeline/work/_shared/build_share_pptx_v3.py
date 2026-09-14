#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""「研发+AI」全栈交付工作范式 V3 优化版

优化重点：
1. 故事线更清晰：痛点→决策→方法→验证
2. 重点更突出：从0到若依的token驱动、Web到客户端的安全驱动
3. 表达更简洁：每页信息密度适中
4. 适合现场：标注演示节点

运行：python pipeline/work/_shared/build_share_pptx_v3.py
"""
from __future__ import annotations

import shutil
from datetime import datetime
from pathlib import Path

from pptx import Presentation
from pptx.enum.shapes import MSO_SHAPE_TYPE

HERE = Path(__file__).resolve().parent
OUT = HERE / "分享-研发AI全栈交付范式-40分钟.pptx"
TPL = HERE / "share-assets" / "tpl" / "追逐梦想-科技智享.pptx"
BAK = HERE / "share-ppt-bak"


def backup_current():
    """覆盖前把现片拷到 share-ppt-bak/，带时间戳。"""
    if not OUT.is_file():
        return None
    BAK.mkdir(parents=True, exist_ok=True)
    dest = BAK / "分享-研发AI全栈交付范式-40分钟-{}.pptx".format(
        datetime.now().strftime("%Y%m%d-%H%M%S")
    )
    shutil.copyfile(OUT, dest)
    print("backup", dest)
    return dest


def set_text(shape, text):
    if shape is None or not shape.has_text_frame:
        return
    tf = shape.text_frame
    tf.word_wrap = True
    lines = text.split("\n") if isinstance(text, str) else list(text)
    paras = list(tf.paragraphs)

    # 清空所有段落的所有runs
    for para in paras:
        if para.runs:
            for run in para.runs:
                run.text = ""

    # 总是把所有内容放到第一段（用换行符连接多行）
    if paras:
        if paras[0].runs:
            paras[0].runs[0].text = "\n".join(lines)
        else:
            paras[0].add_run().text = text


def sh(slide, *idx):
    cur = slide.shapes
    obj = None
    for i in idx:
        obj = cur[i]
        cur = obj.shapes if obj.shape_type == MSO_SHAPE_TYPE.GROUP else None
    return obj


def fill_slide(slide, mapping):
    for key, text in mapping.items():
        if isinstance(key, int):
            set_text(slide.shapes[key], text)
        else:
            set_text(sh(slide, *key), text)


def notes(slide, text):
    slide.notes_slide.notes_text_frame.text = text


def build():
    if not TPL.is_file():
        raise SystemExit("缺少模板：{}".format(TPL))
    backup_current()
    tmp = OUT.with_name("_tpl_fill_v3.pptx")
    shutil.copyfile(TPL, tmp)
    prs = Presentation(str(tmp))
    s = prs.slides

    # 1 封面
    fill_slide(s[0], {
        2: "技术分享",
        6: "后端+AI完成全栈交付\n架构决策·工作方法·多项目验证",
        12: "GENLOT VPN",
        13: "全",
        14: "栈",
        15: "交",
        16: "付",
        17: "AI协作范式",
    })
    notes(s[0], "一句话：后端工程师借助AI，通过结构化协作方法，完成了云端+管理端+桌面客户端的全栈交付，并沉淀出可推广的六阶段工作法。")

    # 2 目录
    fill_slide(s[1], {
        21: "01 架构演进",
        22: "token成本·从0到若依·Web到客户端",
        9: "02 共创决策",
        10: "人给约束·AI列方案·四选一对比",
        13: "03 工作方法",
        14: "六阶段·质量门禁·可复用流程",
        17: "04 多项目验证",
        18: "4个系统·部门推广·证据可核验",
    })
    notes(s[1], "四段式：问题和决策(10分钟) → 人机协作模式(10分钟) → 工作方法(10分钟) → 验证和推广(8分钟)。")

    # 3 节首页：架构演进
    fill_slide(s[2], {
        4: "01 架构演进历程",
        5: "WHY NOT FROM SCRATCH",
        6: "从0开始\ntoken消耗大",
        7: "Web端MVP\n安全驱动升级",
    })
    notes(s[2], "这部分讲清楚两个关键决策：为什么选若依(token成本)、为什么从Web升级到客户端(安全)。")

    # 4 问题：从0构建的成本
    fill_slide(s[3], {
        14: "最初方案：从0构建",
        15: "TOKEN COST PROBLEM",
        7: "让AI全生成",
        6: "Spring Boot微服务从0搭·权限菜单从0写",
        9: "遇到的问题",
        8: "token消耗巨大·基础设施不是核心·调试周期长",
        11: "关键认知",
        10: "应聚焦VPN业务(多线路同步)·不是重造轮子",
        13: "决策转向",
        12: "寻找成熟管理系统底座",
    })
    notes(s[3], "真实经历：一开始想让AI全生成，发现基础框架消耗token大且不是核心价值。这是转向若依的原因。")

    # 5 决策：若依作为底座
    fill_slide(s[4], {
        21: "选择若依作为底座",
        22: "AI ASSISTED DECISION",
        7: "AI列方案",
        17: "自研完整后台·升Boot3·用若依3.6.8(Boot2+Java8)",
        9: "人的约束",
        15: "一人维护·Java8环境·必须Win+Mac·30303不可改",
        8: "决策理由",
        16: "权限菜单现成·微服务架构·token聚焦业务逻辑",
        18: "核心模块",
        20: "yianlian模块·双服务层·多线路ID映射",
    })
    notes(s[4], "若依不是「套壳」，而是经过AI对比后的理性决策。核心业务(yianlian模块)是AI辅助实现的。")

    # 6 演进：Web端到客户端
    fill_slide(s[5], {
        9: "从Web端到客户端",
        10: "SECURITY DRIVEN",
        12: "第一版Web",
        11: "MVP验证·统一登录·选线功能·跨平台快",
        14: "安全问题",
        13: "HTTP明文·抓包可见密码·与零信任矛盾",
        16: "第二版客户端",
        15: "TLS 1.3加密·Qt跨平台·Win/Mac原生",
        18: "关键转折",
        17: "安全把第一解否决了·不是失败是演进",
        20: "Web下线",
        19: "用户端Web入口已下线·只保留管理端",
    })
    notes(s[5], "Web端不是失败，是MVP。安全问题驱动升级到客户端。这是架构演进，不是推倒重来。")

    # 7 节首页：共创决策
    fill_slide(s[6], {
        4: "02 人机协作决策",
        5: "AI LISTS, HUMAN DECIDES",
        6: "Qt四选一\n跨平台设计",
        7: "人给约束\nAI列方案",
    })
    notes(s[6], "人机协作的核心：不是AI说什么就做什么，而是人给约束、AI列方案、人决策、ADR记录。")

    # 8 共创三步法
    fill_slide(s[7], {
        12: "共创决策的固定三步",
        13: "STRUCTURED COLLABORATION",
        8: "①人给约束",
        9: "②AI列方案",
        10: "③人决策落盘",
        11: "",
        15: "约束示例",
        14: "一人·Java8·Win+Mac·过抓包·30303不改",
        17: "AI职责",
        16: "列2-3个方案·对比优缺点·风险分析·不直接开写",
        19: "决策记录",
        18: "选哪个·为何不选其他·落ADR文档·可追溯",
        21: "三次共创",
        20: "后台(若依)·客户端(Qt)·协议(TLS)",
    })
    notes(s[7], "固定三步是可复用的。后面三个实例分别展开。现场可打开ADR-001/002。")

    # 9 实例：客户端四选一
    fill_slide(s[8], {
        4: "客户端技术栈四选一",
        5: "QT VS ELECTRON VS WPF",
        9: "Electron",
        8: "AI建议：出包快·前端熟·跨平台 → 人否决：安全面大",
        13: "Qt 6+QML",
        12: "AI建议：原生·C++可迁移 → 人选择：Win/Mac一套",
        11: "WPF",
        10: "AI提示：只Win → 人否决：没Mac",
        7: "ADR记录",
        6: "ADR-001·四选一对比表·决策理由·可打开核验",
    })
    notes(s[8], "现场演示：打开ADR-001，展示四选一对比表。强调人基于约束否决，不是AI推荐什么就用什么。")

    # 10 实例：协议与安全
    fill_slide(s[9], {
        27: "传输协议与安全分级",
        28: "TLS NOT HTTP",
        26: "AI倾向",
        25: "复用HTTP网关·JSON格式·出活快 → 人否决：抓包风险",
        22: "人决策",
        21: "TLS 1.3 + Protobuf :9443·证书Pin·管理端仍HTTP",
        24: "安全分级",
        23: "S0只人验(TLS/Pin)·S1人审逻辑·S2脚本断言",
    })
    notes(s[9], "协议决策：AI倾向快(HTTP)，人基于安全否决。S0永远只人验，栈配好≠功能自动过。")

    # 11 节首页：工作方法
    fill_slide(s[10], {
        4: "03 六阶段工作法",
        5: "REUSABLE METHOD",
        6: "可重复流程\n质量门禁",
        7: "已沉淀SOP\n可推广",
    })
    notes(s[10], "从实践中提炼出的方法，已沉淀SOP文档，在4个项目验证。")

    # 12 六阶段流程
    fill_slide(s[11], {
        1: "六阶段全景",
        2: "SIX PHASES",
        8: "①拆解·②共创",
        7: "人主导·模块清单·spec人批·未批不写码",
        4: "③突破·④生成",
        3: "陌生域先Demo·熟悉域按规范·不另起炉灶",
        10: "⑤门禁·⑥回修",
        9: "gate→deploy→accept·质量门禁.md·回修≤2轮",
        6: "为什么六阶段",
        5: "①②防跑偏·③④按熟悉度·⑤⑥可管控",
    })
    notes(s[11], "为什么是六阶段：①②保证方向(人主导)·③④加速实现(分流)·⑤⑥控制质量(门禁)。")

    # 13 人与AI配合
    fill_slide(s[12], {
        20: "人与AI如何配合",
        21: "ROLES AND BOUNDARIES",
        9: "人的职责",
        8: "拆解边界·批准spec·S0验收·架构决策",
        10: "AI的职责",
        11: "列方案·生成代码·排错·即时导师·不承担责任",
        12: "硬门槛",
        14: "未批spec不写码·S0只人验·回修≤2轮·超轮停下",
        13: "可重复性",
        15: "同一套命令·可搬迁到新项目·证据可核验",
    })
    notes(s[12], "职责边界清晰是关键。三个硬门槛保证质量可控。")

    # 14 质量门禁
    fill_slide(s[13], {
        17: "质量门禁三命令",
        18: "GATE-DEPLOY-ACCEPT",
        5: "gate",
        6: "deploy",
        7: "accept",
        8: "--run",
        9: "构建·单测·静态检查",
        10: "三节点部署·服务健康·日志无异常",
        11: "业务验收·API断言·端到端流程",
        12: "写入质量门禁.md·时间戳可追溯·证据可核验",
    })
    notes(s[13], "三命令可重入、可自动化。--run参数强制记录到当次目录，否则不算过门禁。")

    # 15 节首页：多项目验证
    fill_slide(s[14], {
        4: "04 多项目验证",
        5: "VERIFIED METHODOLOGY",
        6: "4个系统验证\n部门已推广",
        7: "证据可核验\n时间戳真实",
    })
    notes(s[14], "方法不是纸上谈兵，已在4个项目验证、部门内推广使用。")

    # 16 验证项目
    fill_slide(s[15], {
        20: "4个已验证项目",
        21: "REAL PROJECTS",
        19: "GenlotVPN",
        18: "统一VPN·后端+Web+Qt·首个完整验证·2个月",
        15: "山西热线",
        14: "迁移pipeline·验证可搬迁·适配新栈",
        17: "山西统一系统",
        16: "再次验证·不同技术栈·流程可复用",
        13: "验票系统",
        12: "第四个项目·部门内推广·SOP已分享",
        11: "",
    })
    notes(s[15], "每个项目都有pipeline/work目录可核验。不是做了一个项目，而是验证了方法的可推广性。")

    # 17 时间数据
    fill_slide(s[16], {
        11: "关键数据(可核验)",
        12: "REAL METRICS",
        10: "08-16三层打通",
        9: "11:14 gate → 12:03 deploy → 12:22 accept → 13:21客户端",
        6: "编制对比",
        5: "传统≥4人(后端+前端+桌面+测试) vs 1人+AI",
        8: "方法复用",
        7: "同一套六阶段：15权限查询·16版本拦截·18反馈·19下载",
        3: "推广范围",
        2: "部门内推广·4个项目验证·非全公司自动化",
        1: "",
    })
    notes(s[16], "不写倍数，只报时间戳和编制数。现场可打开08-16当次目录，展示质量门禁.md的时间戳。")

    # 18 边界说明
    fill_slide(s[17], {
        0: "边界与不足",
        14: "WHAT WE DO NOT CLAIM",
        6: "01",
        7: "02",
        8: "03",
        9: "04",
        (12, 1): "S0只人验",
        (12, 0): "客户端不自动验收·TLS/Pin人工确认·抓包验密文",
        (10, 1): "未投产规模化",
        (10, 0): "内部使用·不说已大规模投产·下载页内网accept",
        (11, 1): "非全自动",
        (11, 0): "生产验证码保持开·不自动识图回填·决策权在人",
        (13, 1): "推广需适配",
        (13, 0): "部门内推广·换仓需调整·非一键复制",
    })
    notes(s[17], "主动讲边界：不说无人审、不说已投产、不说全自动、不说一键复制。")

    # 19 总结
    fill_slide(s[18], {
        2: "技术分享",
        6: "架构决策可追溯(ADR)\n工作方法可推广(SOP)\n多项目已验证(证据)",
        12: "GENLOT VPN",
        13: "全",
        14: "栈",
        15: "交",
        16: "付",
        17: "欢迎提问",
    })
    notes(s[18], "三个核心价值：1)每个决策有ADR文档 2)六阶段有SOP文档 3)4个项目有证据。欢迎提问。")

    dest = OUT
    try:
        prs.save(str(OUT))
    except PermissionError:
        dest = OUT.with_name("分享-研发AI全栈交付范式-40分钟-新.pptx")
        prs.save(str(dest))
    tmp.unlink(missing_ok=True)
    print("wrote", dest, "slides", len(prs.slides))


if __name__ == "__main__":
    build()
