#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""「研发+AI」全栈交付工作范式的探索与实践

基于 GenlotVPN-Cloud 项目的真实经验，展示：
1. 一人如何借助 AI 完成后端+前端+桌面端的全栈交付
2. 六阶段工作法如何管控 AI 协作质量
3. 架构决策中人机协作的实际模式

运行：python pipeline/work/_shared/build_share_pptx_fullstack.py
"""
from __future__ import annotations

import shutil
from datetime import datetime
from pathlib import Path

from pptx import Presentation
from pptx.enum.shapes import MSO_SHAPE_TYPE

HERE = Path(__file__).resolve().parent
OUT = HERE / "分享-研发AI全栈交付-40分钟.pptx"
TPL = HERE / "share-assets" / "tpl" / "追逐梦想-科技智享.pptx"
BAK = HERE / "share-ppt-bak"


def backup_current():
    """覆盖前把现片拷到 share-ppt-bak/，带时间戳。"""
    if not OUT.is_file():
        return None
    BAK.mkdir(parents=True, exist_ok=True)
    dest = BAK / "分享-研发AI全栈交付-40分钟-{}.pptx".format(
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
    for i, para in enumerate(paras):
        line = lines[i] if i < len(lines) else ""
        if para.runs:
            para.runs[0].text = line
            for run in para.runs[1:]:
                run.text = ""
        elif line:
            para.add_run().text = line
    if len(lines) > len(paras) and paras:
        paras[0].runs[0].text = "\n".join(lines) if paras[0].runs else text


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
    tmp = OUT.with_name("_tpl_fill_fullstack.pptx")
    shutil.copyfile(TPL, tmp)
    prs = Presentation(str(tmp))
    s = prs.slides

    # 1 封面：「研发+AI」全栈交付工作范式
    fill_slide(s[0], {
        2: "技术分享",
        6: "后端工程师如何借助 AI\n完成全栈交付",
        12: "GENLOT VPN",
        13: "全",
        14: "栈",
        15: "交",
        16: "付",
        17: "AI 协作",
    })
    notes(s[0], "核心：不是 AI 写了个项目，而是后端工程师通过人机协作范式，完成了原本需要跨团队的全栈工作。")

    # 2 目录
    fill_slide(s[1], {
        21: "问题与挑战",
        22: "一人要交后端+前端+桌面·Win+Mac·三级授权",
        9: "人机协作模式",
        10: "共创选型·六阶段轨道·质量门禁",
        13: "架构与实现",
        14: "微服务·双服务层·TLS协议·跨平台桌面",
        17: "实践与沉淀",
        18: "真实闭环·时间数据·可复用方法",
    })
    notes(s[1], "四大模块：问题定义 → 工作方法 → 技术架构 → 实践证据。")

    # 3 节首页：问题与挑战
    fill_slide(s[2], {
        4: "问题与挑战",
        5: "THE CHALLENGE",
        6: "一人交付\n全栈系统",
        7: "陌生技术域\nVue / Qt",
    })
    notes(s[2], "背景：统一 VPN 管理平台，需要管理端(Web)+桌面客户端(Win/Mac)+云端服务(Java微服务)。")

    # 4 需求与技术栈
    fill_slide(s[3], {
        14: "项目需求全景",
        15: "FULL-STACK REQUIREMENTS",
        7: "云端服务",
        6: "Spring Cloud 微服务·多线路同步·三级授权·审计日志",
        9: "管理端",
        8: "Vue 2 + Element UI·权限菜单·部门树·角色管理",
        11: "桌面客户端",
        10: "Qt 6 + QML·Win/Mac·TLS 1.3·选线登录·钉钉验证",
        13: "个人技术栈",
        12: "Java/C++ 熟练·Vue/Qt/QML 零基础·Spring Cloud 熟悉",
    })
    notes(s[3], "难点：1) Vue/Qt 完全陌生  2) 安全要求高(TLS必须)  3) 跨平台桌面(Win+Mac)  4) 多线路ID映射复杂")

    # 5 人机协作核心原则（四列）
    fill_slide(s[4], {
        21: "人机协作的四个原则",
        22: "HUMAN-AI COLLABORATION",
        7: "人定边界",
        17: "安全级·技术选型·架构约束由人确定",
        9: "AI 列方案",
        15: "AI 对比 2-3 个方案·列优缺点·不直接开写",
        8: "人拍板",
        16: "人基于约束做决策·结果落 ADR 文档",
        18: "门禁管控",
        20: "未批 spec 不写码·S0 只人验·门禁脚本可重入",
    })
    notes(s[4], "这四条是核心。AI 不是编码工具，而是架构助手+实现加速器。决策权始终在人。")

    # 6 共创决策实例（齿轮）
    fill_slide(s[5], {
        9: "三次关键共创决策",
        10: "AI PROPOSES, HUMAN DECIDES",
        12: "后台框架",
        11: "AI 提自研/若依/升Boot3。人选若依3.6.8(Java8约束)",
        14: "客户端技术",
        13: "AI 倾向 Electron。人否决(安全面大)·选 Qt(原生)",
        16: "传输协议",
        15: "AI 倾向复用 HTTP。人否决(抓包风险)·选 TLS+Protobuf",
        18: "决策依据",
        17: "一人·Java 8·必须 Win+Mac·必须过抓包·30303不可改",
        20: "落盘记录",
        19: "ADR-001(桌面技术栈)·ADR-002(云端协议)",
    })
    notes(s[5], "共创不是「AI 建议什么就用什么」。人给约束 → AI 列案 → 人基于安全/维护/能力边界决策。")

    # 7 节首页：六阶段工作法
    fill_slide(s[6], {
        4: "六阶段工作法",
        5: "SIX-PHASE WORKFLOW",
        6: "拆解·共创·突破\n生成·门禁·回修",
        7: "可重复的\n交付轨道",
    })
    notes(s[6], "这是方法的核心，让 AI 协作可管可控可复现。")

    # 8 六阶段详解（拼图/四块）
    fill_slide(s[7], {
        12: "六阶段轨道",
        13: "STRUCTURED AI WORKFLOW",
        8: "①②人工主导",
        9: "③④AI执行",
        10: "⑤⑥质量管控",
        11: "",
        15: "①拆解·②共创",
        14: "模块清单·熟悉度标注·spec 人批·未批不写码",
        17: "③突破·④生成",
        16: "陌生域先 Demo·熟悉域按参照·不另起炉灶",
        19: "⑤门禁·⑥回修",
        18: "gate→deploy→accept·钉钉反馈·回修≤2轮",
        21: "",
        20: "客户端 S0 只人验·不进自动 accept",
    })
    notes(s[7], "①②是设计阶段(人主导)·③④是实现阶段(AI 加速)·⑤⑥是验证阶段(脚本+人工)。")

    # 9 质量门禁机制（循环箭头）
    fill_slide(s[8], {
        4: "质量门禁三道关",
        5: "QUALITY GATES",
        9: "gate",
        8: "构建·单测·静态检查·环境预检",
        13: "deploy",
        12: "三节点部署·服务健康·日志无异常",
        11: "accept",
        10: "业务验收脚本·API 断言·端到端流程",
        7: "门禁记录",
        6: "质量门禁.md 记录 --run 输出·时间戳可追溯",
    })
    notes(s[8], "三命令可重入、可自动化。客户端因安全验收(TLS/Pin)不进 accept.py，人工对照 PHASE2 清单。")

    # 10 安全分级（灯泡+左右）
    fill_slide(s[9], {
        27: "安全验收分三级",
        28: "SECURITY TIERS",
        26: "S0 - 只人验",
        25: "TLS 配置·证书 Pin·密钥不进日志·抓包验密文",
        22: "S1 - 人审逻辑",
        21: "登录拦截(426)·多线路同步·权限校验",
        24: "S2 - 脚本断言",
        23: "只读查询·策略页·反馈列表·下载页文案",
    })
    notes(s[9], "S0 永远只人验。TLS 栈配好了≠每条新功能自动过 S0。")

    # 11 节首页：架构与实现
    fill_slide(s[10], {
        4: "架构与实现",
        5: "ARCHITECTURE",
        6: "微服务·双服务层\nTLS 协议·跨平台",
        7: "三层打通\n一人交付",
    })
    notes(s[10], "技术架构不是为了炫技，是为了满足「一人+AI」能维护的约束。")

    # 12 系统架构全景（树状/拼图）
    fill_slide(s[11], {
        1: "系统架构四层",
        2: "FOUR-LAYER ARCHITECTURE",
        8: "管理端",
        7: "Vue 2 + Element UI → Nginx :80 → Gateway :8080",
        4: "云端服务",
        3: "Spring Cloud·Nacos·yianlian 模块·双服务层",
        10: "桌面客户端",
        9: "Qt 6 + QML·Win/Mac·直连 vpn-auth :9443·TLS 1.3",
        6: "本地隧道",
        5: "易安联 Agent HTTP :30303(厂商不可改)",
    })
    notes(s[11], "管理端和桌面端是两张皮：管理端走 HTTP 网关·桌面端直连 9443 TLS。")

    # 13 双服务层设计（递减圆）
    fill_slide(s[12], {
        20: "易安联模块双服务层",
        21: "DUAL-SERVICE PATTERN",
        9: "本地库",
        8: "IVpnXxxService → Mapper → MySQL",
        10: "远程同步",
        11: "IYiAnLianXxxService → OpenApiClient → HTTP",
        12: "映射表管理",
        14: "*YianlianMapping 维护本地 ID ↔ 远程 ID",
        13: "多线路支持",
        15: "一条本地变更同步到多台易安联设备(LineApp)",
    })
    notes(s[12], "核心难点：一套本地数据 → 多个远程平台·每个平台独立 ID 空间·映射表是关键。")

    # 14 桌面端技术选型（四等圆时间轴风格展示技术点）
    fill_slide(s[13], {
        17: "桌面端关键技术",
        18: "DESKTOP CLIENT TECH",
        5: "Qt 6",
        6: "QML",
        7: "TLS 1.3",
        8: "Protobuf",
        9: "跨平台原生·C++ 熟悉度可迁移",
        10: "声明式 UI·学习曲线比 Qt Widgets 平缓",
        11: "TCP :9443·证书 Pin·抓包密文",
        12: "契约在 proto/vpn·Java/C++ 共用",
    })
    notes(s[13], "否决 Electron 的原因：安全面大·非原生·一人难维护。Qt 虽陌生，但 C++ 基础可迁移。")

    # 15 节首页：实践与沉淀
    fill_slide(s[14], {
        4: "实践与沉淀",
        5: "EVIDENCE & LESSONS",
        6: "真实闭环\n时间数据",
        7: "可复用方法\n可核验证据",
    })
    notes(s[14], "不写倍数·不写已投产·只讲对得上的数字和可打开的目录。")

    # 16 两个核心闭环（中心+四角）
    fill_slide(s[15], {
        20: "两个代表性闭环",
        21: "TWO KEY ITERATIONS",
        19: "08-15 线路权限",
        18: "首次跑通六阶段·回修 2 轮·ADR-003 记录踩坑",
        15: "08-16 版本拦截",
        14: "三层打通·11:14-13:21·管理端策略+云端拦截+桌面弹窗",
        17: "08-18 问题反馈",
        16: "桌面设置页→云端落库→管理端改状态·三层再验证",
        13: "08-19 下载页",
        12: "静态站部署·中英文·不改桌面源码·链接落实",
        11: "",
    })
    notes(s[15], "15 日轨道摔硬·16 日打穿三层·18/19 证明方法可复用。")

    # 17 时间数据与效率（四条展示）
    fill_slide(s[16], {
        11: "效率数据(不写倍数)",
        12: "EFFICIENCY METRICS",
        10: "编制对比",
        9: "传统：后端+前端+桌面+测试 ≥4人·现在：1人",
        6: "周期对比",
        5: "同类功能传统按周联调·现在当天 accept 通过",
        8: "08-16 节点",
        7: "11:14 gate → 12:03 deploy → 12:22 accept → 13:21 客户端对照",
        3: "方法复用",
        2: "同一套轨道：15权限·16拦截·18反馈·19下载",
        1: "",
    })
    notes(s[16], "不说快了几倍·只报时间戳和编制数。18/19 是方法复用，不是又做了两次客户端。")

    # 18 方法沉淀（四瓣+四卡片）
    fill_slide(s[17], {
        0: "可复用的方法沉淀",
        14: "REUSABLE METHODOLOGY",
        6: "01",
        7: "02",
        8: "03",
        9: "04",
        (12, 1): "SOP 文档",
        (12, 0): "六阶段工作法 SOP·已在本部门推广·换仓需适配",
        (10, 1): "ADR 决策记录",
        (10, 0): "ADR-001 到 006·桌面技术·协议选型·流水线踩坑",
        (11, 1): "门禁脚本",
        (11, 0): "gate/deploy/accept 可重入·质量门禁.md 记录",
        (13, 1): "当次目录",
        (13, 0): "清单·spec·门禁记录·截图·验收报告·可核验",
    })
    notes(s[17], "沉淀不是 PPT，是打开目录就能看到的文件：模块清单、spec、ADR、门禁记录。")

    # 19 边界与未来（中心装饰+四条）
    fill_slide(s[18], {
        11: "边界与不足",
        12: "BOUNDARIES",
        10: "客户端验收",
        9: "S0 只人验·不自动识别验证码·不用页面文案代替抓包",
        6: "投产范围",
        5: "内部使用·未规模化投产·下载页内网 accept",
        8: "方法推广",
        7: "本部门已用·换仓需调整·不是全公司自动化",
        3: "AI 局限",
        2: "决策仍需人·安全只人验·陌生域需先突破",
        1: "",
    })
    notes(s[18], "主动讲边界：不说无人审、不说已投产、不说客户端自动验收。")

    # 注：模板只有19页（索引0-18），最后一页使用s[18]作为结束页

    dest = OUT
    try:
        prs.save(str(OUT))
    except PermissionError:
        dest = OUT.with_name("分享-研发AI全栈交付-40分钟-新.pptx")
        prs.save(str(dest))
    tmp.unlink(missing_ok=True)
    print("wrote", dest, "slides", len(prs.slides))


if __name__ == "__main__":
    build()

