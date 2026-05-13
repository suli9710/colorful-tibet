import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET


src = [p for p in Path(r"C:\Users\Suli\Desktop").glob("202201150514-*.docx") if "SpringBoot+Vue" in p.name][0]
out = src.with_name(src.stem + "-algorithm-revised.docx")

ns_uri = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
ns = {"w": ns_uri}
ET.register_namespace("w", ns_uri)

replacements = {
    55: "本文的研究特色主要包括以下两方面内容：（1）基于混合协同过滤与内容过滤机制为游客提供智能化景点推荐提醒，其中融合用户行为相似度、景点相似度和标签偏好等信息；（2）使用Echarts进行展示，对出行人数对于景点发展的影响进行了分析。",
    147: "5.1.4 混合协同过滤推荐算法流程44",
    1190: "5.1.4 混合协同过滤推荐算法流程",
    1191: "本系统的景区推荐算法以混合协同过滤为核心，整体流程包括数据采集及清洗、建立用户-景点交互矩阵、计算用户相似度和景点相似度、融合内容标签得分，最后进行推荐排序[ 8 ]。其中，基于用户的协同过滤用于发现兴趣相近的游客群体，基于物品的协同过滤用于发现与已浏览景点相似的候选景点，两者共同构成推荐候选集。",
    1194: "用户相似度计算采用多维混合方式衡量用户之间的兴趣接近程度，主要结合调整后的余弦相似度、Jaccard相似度和时间加权相似度。调整后的余弦相似度用于降低用户评分习惯差异带来的影响，Jaccard相似度用于衡量共同访问景点集合的重合程度，时间加权相似度则体现用户近期兴趣对推荐结果的影响。",
    1195: "在完成相似用户和相似景点候选集构建之后，系统会从近邻用户的正向交互数据以及已访问景点的相似景点中获取目标用户尚未浏览的景区作为备选集，并结合景点标签、类别和用户兴趣画像计算内容匹配得分。最终通过加权融合方式生成个性化推荐列表，同时保留点击、浏览、评价等行为收集机制，实现“推荐生成-用户互动-反馈评价-改进模型”的循环过程。",
    1225: "在西藏旅游网站中，当用户对景点进行搜索或者给景点评分后，系统会对用户行为数据进行抓取和分析，并通过混合协同过滤算法实现个性化景点推荐。该算法既利用用户之间的行为相似性，也利用景点之间的相似性，并进一步结合景点标签、类别、热度和用户上下文信息进行综合排序。",
    1228: "（2）相似度网络建立，在经过前一步骤处理之后得到用户-项目交互矩阵上，系统分别计算用户之间的兴趣相似度和景点之间的物品相似度。用户相似度综合采用调整余弦相似度、Jaccard相似度和时间加权相似度，景点相似度则根据共同访问用户和评分行为计算，从而形成候选推荐网络。",
    1230: "（4）推荐结果生成，系统基于协同过滤得分、内容标签得分和上下文匹配得分对候选景点进行综合排序，选取得分较高的前N个景点作为个性化推荐结果输出，在一定程度上利用群体智慧和景点属性信息缓解用户行为稀疏问题，并提升长尾景点的曝光机会。",
    1240: "通过对混合协同过滤计算出的景点推荐总体框架设计，在此之前需要选择合理的评价标准，根据用户行为以及对景点评分，之后给出相应的权重并采用数据挖掘的方法，设计包含User-Based CF、Item-Based CF和内容过滤的推荐算法实现过程。系统构建用户-景点矩阵，分别计算用户相似度和景点相似度，再结合标签匹配、上下文过滤和多样性重排序，最终实现景点个性化推荐，部分源码见图5.4。",
    1332: "本节介绍了基于用户行为分析的混合协同过滤推荐方法在旅游景区个性化推荐中的应用。其基本原理是在用户行为路径比较的基础上，进一步结合景点之间的相似度、景点标签和类别等内容特征，找到最契合用户兴趣和旅游资源特征的推荐结果。系统获取游客在网站上的操作信息，如搜索次数、对服务评价、点击次数和浏览时间等，形成用户兴趣画像，并依据该画像发现与目标用户旅游喜好接近的人群以及与已浏览景点相似的候选景点。由于该方法同时利用人与人之间的行为关系以及景点之间的内容关系，可以在一定程度上弥补单个用户行为稀疏造成的偏差。本章还介绍了基于ECharts实现旅游景点数据可视化的方法，在ECharts中绘制用户近7天对景点、酒店的预定数量以及预订人数变化情况折线图，便于景区、酒店进行运营数据分析。网站设有热力图，根据当前的人流量以及景点环境温度的不同用不同颜色表示，显示景点热度。本章主要介绍了系统相关技术的选择及其应用。",
    1667: "根据以上情况，本人利用SpringBoot+Vue开发了一款西藏旅游网站，应用混合协同过滤算法以及景区相关属性信息为游客提供定制化的景点推荐，并通过数据可视化的方式呈现给用户。",
    1673: "（5）本文以西藏旅游系统为例，对其所采用关键技术进行剖析。主要是基于用户行为模式的混合协同过滤算法设计、使用ECharts框架配合大数据分析技术实现的数据可视化平台建设，以及通过热力图显示不同地区受欢迎程度的方法。而在应用上，则是从最初的初始数据获取、预处理一直到最终模型训练上线整个过程进行了阐述，并重点介绍了各个关键技术的作用和发展趋势等。",
    1677: "（1）本论文所研究旅游平台使用了混合协同过滤推荐算法，在收集游客对各景点搜索次数以及服务质量评分的基础上，计算用户之间的兴趣相似性和景点之间的相似性，并结合景点标签、类别和热度等内容特征，最后向每个游客提供个性化的景点推荐列表。",
    1684: "（2）本论文利用用户行为协同过滤、物品相似度计算和内容过滤方法设计一种个性化景点推荐系统，根据用户的喜好和景点特征为其提供合适的景点，提升用户体验。",
}

with zipfile.ZipFile(src, "r") as zin:
    files = {name: zin.read(name) for name in zin.namelist()}

root = ET.fromstring(files["word/document.xml"])
paragraphs = root.findall(".//w:p", ns)
changed = []

for idx, new_text in replacements.items():
    paragraph = paragraphs[idx - 1]
    old_text = "".join(t.text or "" for t in paragraph.findall(".//w:t", ns))
    if not old_text.strip():
        raise RuntimeError(f"Paragraph {idx} is empty; refusing to patch")
    text_nodes = paragraph.findall(".//w:t", ns)
    if not text_nodes:
        raise RuntimeError(f"Paragraph {idx} has no text nodes")
    text_nodes[0].text = new_text
    for text_node in text_nodes[1:]:
        text_node.text = ""
    changed.append((idx, old_text, new_text))

files["word/document.xml"] = ET.tostring(root, encoding="utf-8", xml_declaration=True)

with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as zout:
    for name, data in files.items():
        zout.writestr(name, data)

report = out.with_name(out.stem + "-changes.txt")
with report.open("w", encoding="utf-8") as handle:
    handle.write(f"修改文件: {out}\n")
    handle.write(f"原文件: {src}\n\n")
    for idx, old, new in changed:
        handle.write(f"【段落 {idx}】\n")
        handle.write("原文：" + old + "\n")
        handle.write("修改后：" + new + "\n\n")

print(out)
print(report)
print(len(changed))
