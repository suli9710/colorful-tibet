from pathlib import Path

from docx import Document
from docx.enum.table import WD_CELL_VERTICAL_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor


OUT = Path(__file__).resolve().parents[1] / "docs" / "\u4e03\u5f69\u897f\u85cf\u7cfb\u7edf\u64cd\u4f5c\u624b\u518c.docx"
TODAY = "2026\u5e745\u670827\u65e5"

BLUE = RGBColor(46, 116, 181)
DARK_BLUE = RGBColor(31, 77, 120)
INK = RGBColor(32, 33, 36)
MUTED = RGBColor(91, 99, 115)
LIGHT_BLUE = "E8EEF5"
BORDER = "D9E2EC"
CALLOUT = "F4F6F9"

LATIN_FONT = "Calibri"
CJK_FONT = "Microsoft YaHei"
MONO_FONT = "Consolas"


def set_run_font(run, name=LATIN_FONT, east_asia=CJK_FONT, size=None, color=None, bold=None, italic=None):
    run.font.name = name
    rpr = run._element.get_or_add_rPr()
    rfonts = rpr.rFonts
    if rfonts is None:
        rfonts = OxmlElement("w:rFonts")
        rpr.append(rfonts)
    rfonts.set(qn("w:ascii"), name)
    rfonts.set(qn("w:hAnsi"), name)
    rfonts.set(qn("w:eastAsia"), east_asia)
    if size is not None:
        run.font.size = Pt(size)
    if color is not None:
        run.font.color.rgb = color
    if bold is not None:
        run.bold = bold
    if italic is not None:
        run.italic = italic


def set_paragraph_spacing(p, before=0, after=6, line=1.25):
    pf = p.paragraph_format
    pf.space_before = Pt(before)
    pf.space_after = Pt(after)
    pf.line_spacing = line


def shade_cell(cell, fill):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = tc_pr.find(qn("w:shd"))
    if shd is None:
        shd = OxmlElement("w:shd")
        tc_pr.append(shd)
    shd.set(qn("w:fill"), fill)


def set_cell_margins(cell, top=80, start=120, bottom=80, end=120):
    tc_pr = cell._tc.get_or_add_tcPr()
    tc_mar = tc_pr.first_child_found_in("w:tcMar")
    if tc_mar is None:
        tc_mar = OxmlElement("w:tcMar")
        tc_pr.append(tc_mar)
    for margin, value in [("top", top), ("start", start), ("bottom", bottom), ("end", end)]:
        node = tc_mar.find(qn(f"w:{margin}"))
        if node is None:
            node = OxmlElement(f"w:{margin}")
            tc_mar.append(node)
        node.set(qn("w:w"), str(value))
        node.set(qn("w:type"), "dxa")


def set_cell_width(cell, width_dxa):
    tc_pr = cell._tc.get_or_add_tcPr()
    tc_w = tc_pr.find(qn("w:tcW"))
    if tc_w is None:
        tc_w = OxmlElement("w:tcW")
        tc_pr.append(tc_w)
    tc_w.set(qn("w:w"), str(width_dxa))
    tc_w.set(qn("w:type"), "dxa")


def set_table_geometry(table, widths_dxa, indent_dxa=120):
    tbl = table._tbl
    tbl_pr = tbl.tblPr
    tbl_w = tbl_pr.find(qn("w:tblW"))
    if tbl_w is None:
        tbl_w = OxmlElement("w:tblW")
        tbl_pr.append(tbl_w)
    tbl_w.set(qn("w:w"), str(sum(widths_dxa)))
    tbl_w.set(qn("w:type"), "dxa")

    tbl_ind = tbl_pr.find(qn("w:tblInd"))
    if tbl_ind is None:
        tbl_ind = OxmlElement("w:tblInd")
        tbl_pr.append(tbl_ind)
    tbl_ind.set(qn("w:w"), str(indent_dxa))
    tbl_ind.set(qn("w:type"), "dxa")

    tbl_layout = tbl_pr.find(qn("w:tblLayout"))
    if tbl_layout is None:
        tbl_layout = OxmlElement("w:tblLayout")
        tbl_pr.append(tbl_layout)
    tbl_layout.set(qn("w:type"), "fixed")

    old_grid = tbl.tblGrid
    if old_grid is not None:
        tbl.remove(old_grid)
    grid = OxmlElement("w:tblGrid")
    for width in widths_dxa:
        col = OxmlElement("w:gridCol")
        col.set(qn("w:w"), str(width))
        grid.append(col)
    tbl.insert(1, grid)

    for row in table.rows:
        for index, cell in enumerate(row.cells):
            width = widths_dxa[min(index, len(widths_dxa) - 1)]
            set_cell_width(cell, width)
            set_cell_margins(cell)
            cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER


def set_table_borders(table, color=BORDER):
    tbl_pr = table._tbl.tblPr
    borders = tbl_pr.find(qn("w:tblBorders"))
    if borders is None:
        borders = OxmlElement("w:tblBorders")
        tbl_pr.append(borders)
    for edge in ["top", "left", "bottom", "right", "insideH", "insideV"]:
        tag = f"w:{edge}"
        element = borders.find(qn(tag))
        if element is None:
            element = OxmlElement(tag)
            borders.append(element)
        element.set(qn("w:val"), "single")
        element.set(qn("w:sz"), "4")
        element.set(qn("w:space"), "0")
        element.set(qn("w:color"), color)


def add_table(
    doc,
    headers,
    rows,
    widths_dxa,
    trailing_paragraph=True,
    header_size=10.5,
    body_size=10,
    line_spacing=1.15,
    cell_margins=None,
):
    table = doc.add_table(rows=1, cols=len(headers))
    table.style = "Table Grid"
    table.autofit = False
    set_table_geometry(table, widths_dxa)
    set_table_borders(table)
    for index, header in enumerate(headers):
        cell = table.rows[0].cells[index]
        if cell_margins is not None:
            set_cell_margins(cell, *cell_margins)
        shade_cell(cell, LIGHT_BLUE)
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        set_paragraph_spacing(p, after=0, line=line_spacing)
        run = p.add_run(header)
        set_run_font(run, size=header_size, color=INK, bold=True)

    for row in rows:
        cells = table.add_row().cells
        for index, value in enumerate(row):
            if cell_margins is not None:
                set_cell_width(cells[index], widths_dxa[min(index, len(widths_dxa) - 1)])
                set_cell_margins(cells[index], *cell_margins)
            p = cells[index].paragraphs[0]
            p.alignment = WD_ALIGN_PARAGRAPH.LEFT if len(str(value)) > 10 else WD_ALIGN_PARAGRAPH.CENTER
            set_paragraph_spacing(p, after=0, line=line_spacing)
            run = p.add_run(str(value))
            set_run_font(run, size=body_size, color=INK)
    if trailing_paragraph:
        doc.add_paragraph()
    return table


def add_field(paragraph, field_code):
    run = paragraph.add_run()
    fld_begin = OxmlElement("w:fldChar")
    fld_begin.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = field_code
    fld_sep = OxmlElement("w:fldChar")
    fld_sep.set(qn("w:fldCharType"), "separate")
    text = OxmlElement("w:t")
    text.text = "1"
    fld_end = OxmlElement("w:fldChar")
    fld_end.set(qn("w:fldCharType"), "end")
    run._r.append(fld_begin)
    run._r.append(instr)
    run._r.append(fld_sep)
    run._r.append(text)
    run._r.append(fld_end)


def configure_document(doc):
    section = doc.sections[0]
    section.page_width = Inches(8.5)
    section.page_height = Inches(11)
    section.top_margin = Inches(1)
    section.bottom_margin = Inches(1)
    section.left_margin = Inches(1)
    section.right_margin = Inches(1)
    section.header_distance = Inches(0.492)
    section.footer_distance = Inches(0.492)

    styles = doc.styles
    normal = styles["Normal"]
    normal.font.name = LATIN_FONT
    normal._element.rPr.rFonts.set(qn("w:eastAsia"), CJK_FONT)
    normal.font.size = Pt(11)
    normal.paragraph_format.space_after = Pt(6)
    normal.paragraph_format.line_spacing = 1.25

    for style_name in ["Heading 1", "Heading 2", "Heading 3"]:
        style = styles[style_name]
        style.font.name = LATIN_FONT
        style._element.rPr.rFonts.set(qn("w:eastAsia"), CJK_FONT)
        style.font.bold = True
        style.paragraph_format.keep_with_next = True

    styles["Heading 1"].font.size = Pt(16)
    styles["Heading 1"].font.color.rgb = BLUE
    styles["Heading 1"].paragraph_format.space_before = Pt(18)
    styles["Heading 1"].paragraph_format.space_after = Pt(10)
    styles["Heading 1"].paragraph_format.line_spacing = 1.25
    styles["Heading 2"].font.size = Pt(13)
    styles["Heading 2"].font.color.rgb = BLUE
    styles["Heading 2"].paragraph_format.space_before = Pt(14)
    styles["Heading 2"].paragraph_format.space_after = Pt(7)
    styles["Heading 2"].paragraph_format.line_spacing = 1.25
    styles["Heading 3"].font.size = Pt(12)
    styles["Heading 3"].font.color.rgb = DARK_BLUE
    styles["Heading 3"].paragraph_format.space_before = Pt(10)
    styles["Heading 3"].paragraph_format.space_after = Pt(5)
    styles["Heading 3"].paragraph_format.line_spacing = 1.25

    for list_style in ["List Bullet", "List Number"]:
        style = styles[list_style]
        style.font.name = LATIN_FONT
        style._element.rPr.rFonts.set(qn("w:eastAsia"), CJK_FONT)
        style.font.size = Pt(11)
        style.paragraph_format.left_indent = Inches(0.375)
        style.paragraph_format.first_line_indent = Inches(-0.188)
        style.paragraph_format.space_after = Pt(4)
        style.paragraph_format.line_spacing = 1.25

    header = section.header.paragraphs[0]
    header.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    run = header.add_run("\u4e03\u5f69\u897f\u85cf Colorful Tibet \u00b7 \u7cfb\u7edf\u64cd\u4f5c\u624b\u518c")
    set_run_font(run, size=9, color=MUTED)

    footer = section.footer.paragraphs[0]
    footer.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    run = footer.add_run("\u7b2c ")
    set_run_font(run, size=9, color=MUTED)
    add_field(footer, "PAGE")
    run = footer.add_run(" \u9875")
    set_run_font(run, size=9, color=MUTED)


def add_para(doc, text="", size=11, bold=False, color=INK, before=0, after=6, align=None, italic=False):
    p = doc.add_paragraph()
    set_paragraph_spacing(p, before=before, after=after, line=1.25)
    if align is not None:
        p.alignment = align
    if text:
        run = p.add_run(text)
        set_run_font(run, size=size, color=color, bold=bold, italic=italic)
    return p


def add_heading(doc, text, level=1):
    p = doc.add_paragraph(style=f"Heading {level}")
    run = p.add_run(text)
    if level == 1:
        set_run_font(run, size=16, color=BLUE, bold=True)
    elif level == 2:
        set_run_font(run, size=13, color=BLUE, bold=True)
    else:
        set_run_font(run, size=12, color=DARK_BLUE, bold=True)
    return p


def add_numbered(doc, items):
    for index, item in enumerate(items, start=1):
        p = doc.add_paragraph()
        p.paragraph_format.left_indent = Inches(0.25)
        p.paragraph_format.first_line_indent = Inches(-0.25)
        set_paragraph_spacing(p, after=4, line=1.25)
        prefix = p.add_run(f"{index}. ")
        set_run_font(prefix, size=11, color=INK)
        run = p.add_run(item)
        set_run_font(run, size=11, color=INK)


def add_command_block(doc, lines):
    table = doc.add_table(rows=1, cols=1)
    table.autofit = False
    set_table_geometry(table, [9360], indent_dxa=120)
    set_table_borders(table, color="E5E7EB")
    cell = table.cell(0, 0)
    shade_cell(cell, "F8FAFC")
    p = cell.paragraphs[0]
    set_paragraph_spacing(p, after=0, line=1.15)
    for index, line in enumerate(lines):
        if index:
            p.add_run().add_break()
        run = p.add_run(line)
        set_run_font(run, name=MONO_FONT, east_asia=CJK_FONT, size=9.5, color=RGBColor(31, 41, 55))
    doc.add_paragraph()


def add_note(doc, title, body):
    table = doc.add_table(rows=1, cols=1)
    table.autofit = False
    set_table_geometry(table, [9360], indent_dxa=120)
    set_table_borders(table, color="CBD5E1")
    cell = table.cell(0, 0)
    shade_cell(cell, CALLOUT)
    p = cell.paragraphs[0]
    set_paragraph_spacing(p, after=2, line=1.20)
    run = p.add_run(title)
    set_run_font(run, size=10.5, color=DARK_BLUE, bold=True)
    p2 = cell.add_paragraph()
    set_paragraph_spacing(p2, after=0, line=1.20)
    run2 = p2.add_run(body)
    set_run_font(run2, size=10.5, color=INK)
    doc.add_paragraph()


def add_cover(doc):
    add_para(doc, "Colorful Tibet", size=12, bold=True, color=MUTED, after=8, align=WD_ALIGN_PARAGRAPH.CENTER)
    add_para(doc, "\u4e03\u5f69\u897f\u85cf\u65c5\u6e38\u7cfb\u7edf", size=26, bold=True, color=RGBColor(11, 37, 69), after=4, align=WD_ALIGN_PARAGRAPH.CENTER)
    add_para(doc, "\u7cfb\u7edf\u64cd\u4f5c\u624b\u518c", size=20, bold=True, color=BLUE, after=14, align=WD_ALIGN_PARAGRAPH.CENTER)
    add_para(doc, "\u9002\u7528\u4e8e\u524d\u53f0\u6e38\u5ba2\u3001\u6ce8\u518c\u7528\u6237\u4e0e\u540e\u53f0\u7ba1\u7406\u5458\u7684\u90e8\u7f72\u3001\u4f7f\u7528\u548c\u7ef4\u62a4\u64cd\u4f5c\u8bf4\u660e", size=11.5, color=MUTED, after=26, align=WD_ALIGN_PARAGRAPH.CENTER)

    table = doc.add_table(rows=0, cols=2)
    table.autofit = False
    set_table_geometry(table, [2100, 7260], indent_dxa=120)
    set_table_borders(table)
    for label, value in [
        ("\u9879\u76ee\u540d\u79f0", "\u4e03\u5f69\u897f\u85cf Colorful Tibet"),
        ("\u7cfb\u7edf\u7c7b\u578b", "\u897f\u85cf\u65c5\u6e38\u4fe1\u606f\u3001\u8def\u7ebf\u89c4\u5212\u3001\u9152\u5e97\u9884\u8ba2\u4e0e\u5185\u5bb9\u7ba1\u7406 Web \u7cfb\u7edf"),
        ("\u6280\u672f\u67b6\u6784", "Spring Boot 3.5.12 + Vue 3 + MySQL + Redis + Docker Compose"),
        ("\u9002\u7528\u5bf9\u8c61", "\u6e38\u5ba2\u3001\u6ce8\u518c\u7528\u6237\u3001\u7ba1\u7406\u5458\u3001\u90e8\u7f72\u4e0e\u8fd0\u7ef4\u4eba\u5458"),
        ("\u7f16\u5199\u65e5\u671f", TODAY),
        ("\u6587\u6863\u7248\u672c", "V1.0"),
    ]:
        cells = table.add_row().cells
        shade_cell(cells[0], LIGHT_BLUE)
        p = cells[0].paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        set_paragraph_spacing(p, after=0, line=1.15)
        run = p.add_run(label)
        set_run_font(run, size=10.5, bold=True, color=INK)
        p = cells[1].paragraphs[0]
        set_paragraph_spacing(p, after=0, line=1.15)
        run = p.add_run(value)
        set_run_font(run, size=10.5, color=INK)
    doc.add_paragraph()
    add_note(doc, "\u4f7f\u7528\u8bf4\u660e", "\u672c\u624b\u518c\u6839\u636e\u9879\u76ee\u5f53\u524d\u4ee3\u7801\u7ed3\u6784\u3001\u8def\u7531\u5165\u53e3\u3001\u63a5\u53e3\u5b9a\u4e49\u548c\u90e8\u7f72\u6587\u6863\u7f16\u5199\u3002\u5b9e\u9645\u8d26\u53f7\u3001\u5bc6\u94a5\u3001\u57df\u540d\u548c\u7aef\u53e3\u4ee5\u90e8\u7f72\u73af\u5883\u4e2d\u7684 .env \u914d\u7f6e\u548c\u7ba1\u7406\u5458\u5206\u914d\u4fe1\u606f\u4e3a\u51c6\u3002")
    doc.add_page_break()


def build_doc():
    doc = Document()
    configure_document(doc)
    doc.core_properties.title = "\u4e03\u5f69\u897f\u85cf\u7cfb\u7edf\u64cd\u4f5c\u624b\u518c"
    doc.core_properties.subject = "\u7cfb\u7edf\u90e8\u7f72\u4e0e\u529f\u80fd\u64cd\u4f5c\u8bf4\u660e"
    doc.core_properties.author = "Codex"
    add_cover(doc)

    add_heading(doc, "1 \u7cfb\u7edf\u90e8\u7f72\u6d41\u7a0b", 1)
    add_para(doc, "\u4e03\u5f69\u897f\u85cf\u91c7\u7528\u524d\u540e\u7aef\u5206\u79bb\u67b6\u6784\u3002\u524d\u7aef\u7531 Vue 3 \u548c Vite \u6784\u5efa\uff0c\u540e\u7aef\u7531 Spring Boot \u63d0\u4f9b REST API\uff0cMySQL \u4fdd\u5b58\u4e1a\u52a1\u6570\u636e\uff0cRedis \u7528\u4e8e\u7f13\u5b58\u3001\u4f1a\u8bdd\u8f85\u52a9\u548c\u9650\u6d41\uff0cScrapling \u5fae\u670d\u52a1\u7528\u4e8e\u7968\u4ef7\u6293\u53d6\u3002\u751f\u4ea7\u6216\u8054\u8c03\u73af\u5883\u63a8\u8350\u4f7f\u7528 Docker Compose \u7edf\u4e00\u542f\u52a8\u3002")
    add_heading(doc, "1.1 \u73af\u5883\u51c6\u5907", 2)
    add_para(doc, "\u90e8\u7f72\u6216\u5f00\u53d1\u524d\uff0c\u9700\u8981\u5148\u51c6\u5907\u8fd0\u884c\u73af\u5883\u3002Windows \u672c\u5730\u5f00\u53d1\u5efa\u8bae\u4f7f\u7528 PowerShell\uff0c\u5e76\u786e\u4fdd Docker\u3001JDK\u3001Maven\u3001Node.js \u7b49\u5de5\u5177\u53ef\u7528\u3002")
    add_table(doc, ["\u7c7b\u522b", "\u8981\u6c42", "\u7528\u9014"], [
        ("\u57fa\u7840\u73af\u5883", "Windows 10/11\u3001PowerShell\u3001Git", "\u62c9\u53d6\u4ee3\u7801\u3001\u6267\u884c\u811a\u672c\u548c\u7ef4\u62a4\u9879\u76ee\u6587\u4ef6"),
        ("\u5bb9\u5668\u73af\u5883", "Docker / Docker Compose\uff0c\u5efa\u8bae\u914d\u5408 WSL2", "\u4e00\u952e\u542f\u52a8\u524d\u7aef\u3001\u540e\u7aef\u3001MySQL\u3001Redis \u548c Scrapling"),
        ("\u540e\u7aef\u5f00\u53d1", "JDK 17\u3001Maven 3.9+", "\u7f16\u8bd1\u548c\u8fd0\u884c Spring Boot \u540e\u7aef"),
        ("\u524d\u7aef\u5f00\u53d1", "Node.js 20+ \u6216 22+\u3001npm", "\u5b89\u88c5\u4f9d\u8d56\u3001\u542f\u52a8 Vite\u3001\u672c\u5730\u6784\u5efa\u524d\u7aef"),
        ("\u6570\u636e\u5e93", "MySQL 8.x\u3001Redis 7.x", "\u4e1a\u52a1\u6570\u636e\u5b58\u50a8\u3001\u7f13\u5b58\u4e0e\u9650\u6d41\u652f\u6301"),
    ], [1600, 3560, 4200])

    add_heading(doc, "1.2 \u83b7\u53d6\u9879\u76ee\u5e76\u914d\u7f6e\u73af\u5883\u53d8\u91cf", 2)
    add_numbered(doc, [
        "\u5c06\u9879\u76ee\u4ee3\u7801\u653e\u7f6e\u5230\u672c\u5730\u76ee\u5f55\uff0c\u4f8b\u5982 C:\\\\Users\\\\Suli\\\\Desktop\\\\colorful-tibet\u3002",
        "\u8fdb\u5165\u9879\u76ee\u6839\u76ee\u5f55\uff0c\u590d\u5236 .env.example \u4e3a .env\u3002",
        "\u7f16\u8f91 .env\uff0c\u81f3\u5c11\u66ff\u6362\u6240\u6709 change-me\u3001replace-with-* \u7b49\u5360\u4f4d\u503c\u3002",
        "\u786e\u8ba4 SPRING_PROFILES_ACTIVE=local\uff0cVITE_API_BASE_URL=/api\uff0c\u5e76\u6839\u636e\u672c\u673a\u60c5\u51b5\u8bbe\u7f6e\u7aef\u53e3\u3002",
        "\u751f\u4ea7\u73af\u5883\u5fc5\u987b\u4f7f\u7528\u5f3a\u968f\u673a\u5bc6\u94a5\uff0c\u4e0d\u80fd\u6cbf\u7528\u793a\u4f8b\u5bc6\u94a5\u6216\u6f14\u793a\u53e3\u4ee4\u3002",
    ])
    add_command_block(doc, ["cd C:\\\\Users\\\\Suli\\\\Desktop\\\\colorful-tibet", "Copy-Item .env.example .env"])
    add_table(doc, ["\u53d8\u91cf", "\u8bf4\u660e"], [
        ("SPRING_PROFILES_ACTIVE", "\u8fd0\u884c\u73af\u5883\uff0c\u672c\u5730\u5e38\u7528 local\uff0c\u751f\u4ea7\u4f7f\u7528 prod"),
        ("VITE_API_BASE_URL", "\u524d\u7aef API \u57fa\u7840\u8def\u5f84\uff0c\u9ed8\u8ba4 /api"),
        ("MYSQL_* / DB_*", "MySQL \u6570\u636e\u5e93\u540d\u79f0\u3001\u8d26\u53f7\u3001\u5bc6\u7801\u548c\u7aef\u53e3\u914d\u7f6e"),
        ("REDIS_*", "Redis \u4e3b\u673a\u3001\u7aef\u53e3\u548c\u5bc6\u7801\u914d\u7f6e"),
        ("JWT_SECRET / CSRF_SIGNING_SECRET", "\u767b\u5f55\u4ee4\u724c\u7b7e\u540d\u548c CSRF \u9632\u62a4\u5bc6\u94a5"),
        ("PII_KEYS / PII_ACTIVE_KID", "\u654f\u611f\u4fe1\u606f\u52a0\u5bc6\u5bc6\u94a5\u96c6\u5408\u548c\u5f53\u524d key id"),
        ("SUPER_ADMIN_USERNAME", "\u8d85\u7ea7\u7ba1\u7406\u5458\u7528\u6237\u540d\uff0c\u9ed8\u8ba4\u914d\u7f6e\u4e2d\u5e38\u89c1\u4e3a lzh"),
        ("DOUBAO_API_KEY / ARK_API_KEY", "AI \u8def\u7ebf\u751f\u6210\u6a21\u578b\u63a5\u53e3 Key\uff0c\u9700\u8981\u771f\u5b9e AI \u80fd\u529b\u65f6\u914d\u7f6e"),
        ("VITE_AMAP_KEY / VITE_AMAP_SECURITY_CODE", "\u9ad8\u5fb7\u5730\u56fe\u524d\u7aef\u914d\u7f6e"),
        ("FRONTEND_HOST_PORT / BACKEND_HOST_PORT", "\u524d\u7aef\u548c\u540e\u7aef\u5bf9\u5916\u8bbf\u95ee\u7aef\u53e3"),
    ], [3000, 6360])

    add_heading(doc, "1.3 Docker Compose \u4e00\u952e\u542f\u52a8", 2)
    add_para(doc, "\u63a8\u8350\u4f7f\u7528\u9879\u76ee\u6839\u76ee\u5f55\u4e0b\u7684 start.ps1 \u542f\u52a8\u672c\u5730\u5b8c\u6574\u73af\u5883\u3002\u8be5\u811a\u672c\u4f1a\u68c0\u67e5 WSL \u4e0e Docker \u72b6\u6001\uff0c\u5fc5\u8981\u65f6\u6784\u5efa\u955c\u50cf\uff0c\u5e76\u7b49\u5f85\u524d\u7aef\u3001\u540e\u7aef\u3001MySQL\u3001Redis\u3001Scrapling \u670d\u52a1\u5c31\u7eea\u3002")
    add_numbered(doc, [
        "\u6253\u5f00 PowerShell\uff0c\u8fdb\u5165\u9879\u76ee\u6839\u76ee\u5f55\u3002",
        "\u786e\u8ba4 .env \u5df2\u914d\u7f6e\u5b8c\u6210\u3002",
        "\u6267\u884c .\\\\start.ps1\uff0c\u7b49\u5f85\u811a\u672c\u5b8c\u6210\u6784\u5efa\u548c\u5065\u5eb7\u68c0\u67e5\u3002",
        "\u6d4f\u89c8\u5668\u8bbf\u95ee http://localhost\uff0c\u8fdb\u5165\u4e03\u5f69\u897f\u85cf\u7cfb\u7edf\u9996\u9875\u3002",
    ])
    add_command_block(doc, ["cd C:\\\\Users\\\\Suli\\\\Desktop\\\\colorful-tibet", ".\\\\start.ps1"])
    add_para(doc, "\u4e5f\u53ef\u4ee5\u4f7f\u7528 Docker Compose \u624b\u52a8\u542f\u52a8\uff1a")
    add_command_block(doc, ["docker compose config", "docker compose up -d --build", "docker compose ps"])
    add_table(doc, ["\u670d\u52a1", "\u9ed8\u8ba4\u8bbf\u95ee\u5730\u5740", "\u7528\u9014"], [
        ("\u524d\u7aef", "http://localhost", "\u7528\u6237\u8bbf\u95ee\u5165\u53e3\uff0cNginx \u6258\u7ba1\u9759\u6001\u8d44\u6e90\u5e76\u4ee3\u7406 /api"),
        ("\u540e\u7aef\u5065\u5eb7\u68c0\u67e5", "http://localhost:8080/actuator/health/readiness", "\u68c0\u67e5 Spring Boot \u540e\u7aef\u662f\u5426\u5c31\u7eea"),
        ("Swagger", "http://localhost:8080/swagger-ui.html", "\u63a5\u53e3\u6587\u6863\uff0c\u9700 PUBLIC_DOCS_ENABLED=true"),
        ("Scrapling", "http://localhost:8000/health", "\u7968\u4ef7\u6293\u53d6\u5fae\u670d\u52a1\u5065\u5eb7\u68c0\u67e5"),
        ("MySQL", "127.0.0.1:3307", "\u672c\u5730\u6570\u636e\u5e93\u8fde\u63a5\u5730\u5740"),
        ("Redis", "127.0.0.1:6380", "\u672c\u5730 Redis \u8fde\u63a5\u5730\u5740"),
    ], [2100, 3760, 3500])

    add_heading(doc, "1.4 \u524d\u540e\u7aef\u5206\u5f00\u8c03\u8bd5", 2)
    add_para(doc, "\u65e5\u5e38\u5f00\u53d1\u65f6\uff0c\u53ef\u4ee5\u53ea\u7528 Docker \u542f\u52a8 MySQL\u3001Redis \u548c Scrapling\uff0c\u524d\u7aef\u4e0e\u540e\u7aef\u5728\u672c\u673a\u76f4\u63a5\u8fd0\u884c\uff0c\u4fbf\u4e8e\u70ed\u66f4\u65b0\u548c\u65ad\u70b9\u8c03\u8bd5\u3002")
    add_numbered(doc, [
        "\u542f\u52a8\u57fa\u7840\u4f9d\u8d56\u670d\u52a1\uff1adocker compose up -d mysql redis scrapling\u3002",
        "\u8fdb\u5165 backend \u76ee\u5f55\uff0c\u914d\u7f6e local profile \u548c\u6570\u636e\u5e93\u3001Redis\u3001\u5bc6\u94a5\u7b49\u73af\u5883\u53d8\u91cf\u3002",
        "\u6267\u884c mvn spring-boot:run \u542f\u52a8\u540e\u7aef\uff0c\u9ed8\u8ba4\u7aef\u53e3\u4e3a 8080\u3002",
        "\u8fdb\u5165 frontend \u76ee\u5f55\uff0c\u6267\u884c npm install \u5b89\u88c5\u4f9d\u8d56\u3002",
        "\u6267\u884c npm run dev \u542f\u52a8 Vite\uff0c\u9ed8\u8ba4\u8bbf\u95ee\u5730\u5740\u4e3a http://localhost:5173\u3002",
    ])
    add_command_block(doc, [
        "docker compose up -d mysql redis scrapling",
        "cd backend",
        "mvn -q -DskipTests compile",
        "mvn spring-boot:run",
        "",
        "cd ..\\\\frontend",
        "npm install",
        "npm run dev",
    ])

    add_heading(doc, "1.5 \u505c\u6b62\u3001\u91cd\u542f\u4e0e\u9a8c\u8bc1", 2)
    add_numbered(doc, [
        "\u67e5\u770b\u5bb9\u5668\u72b6\u6001\uff1adocker compose ps\u3002",
        "\u67e5\u770b\u540e\u7aef\u65e5\u5fd7\uff1adocker compose logs -f backend\u3002",
        "\u67e5\u770b\u524d\u7aef\u65e5\u5fd7\uff1adocker compose logs -f frontend\u3002",
        "\u505c\u6b62\u6240\u6709\u670d\u52a1\uff1adocker compose down\u3002",
        "\u9700\u8981\u91cd\u7f6e\u672c\u5730\u6570\u636e\u5e93\u548c Redis \u6570\u636e\u65f6\u6267\u884c docker compose down -v\uff0c\u4f46\u8be5\u547d\u4ee4\u4f1a\u5220\u9664\u672c\u5730\u6570\u636e\u5377\u3002",
    ])
    add_command_block(doc, [
        "docker compose logs -f backend",
        "curl.exe http://localhost/health",
        "curl.exe http://localhost:8080/actuator/health/readiness",
        "docker compose down",
    ])

    add_heading(doc, "1.6 \u767b\u5f55\u8d26\u53f7\u4e0e\u521d\u59cb\u5bc6\u7801", 2)
    add_para(doc, "\u9a8c\u6536\u6f14\u793a\u73af\u5883\u53ef\u4f7f\u7528\u4e0b\u8868\u8d26\u53f7\u767b\u5f55\u3002\u8fd9\u4e9b\u8d26\u53f7\u9700\u5148\u5728 .env \u4e2d\u5f00\u542f SEED_DEMO_USERS=true \u5e76\u914d\u7f6e\u5bf9\u5e94\u521d\u59cb\u5bc6\u7801\uff0c\u7136\u540e\u7531\u540e\u7aef DataSeeder \u5728\u7a7a\u6570\u636e\u5e93\u9996\u6b21\u542f\u52a8\u65f6\u521b\u5efa\u3002")
    add_table(doc, ["\u89d2\u8272", "\u767b\u5f55\u5165\u53e3", "\u8d26\u53f7", "\u521d\u59cb\u5bc6\u7801 / \u4e8c\u6b21\u8ba4\u8bc1"], [
        ("\u6e38\u5ba2", "\u65e0\u9700\u767b\u5f55", "\u65e0", "\u65e0\u5bc6\u7801\uff0c\u53ef\u76f4\u63a5\u6d4f\u89c8\u9996\u9875\u3001\u666f\u70b9\u3001\u9152\u5e97\u3001\u975e\u9057\u548c\u65b0\u95fb\u7b49\u516c\u5f00\u5185\u5bb9\u3002"),
        ("\u666e\u901a\u7528\u6237", "/login", "user1", "User@Colorful2026\uff0c\u5bf9\u5e94 .env \u53d8\u91cf SEED_DEMO_USER_PASSWORD\u3002"),
        ("\u7ba1\u7406\u5458", "/login \u767b\u5f55\u540e\u8bbf\u95ee /admin", "admin", "Admin@Colorful2026\uff0c\u5bf9\u5e94 .env \u53d8\u91cf SEED_DEMO_ADMIN_PASSWORD\u3002\u9996\u6b21\u767b\u5f55\u540e\u5982\u63d0\u793a\u4fee\u6539\u521d\u59cb\u5bc6\u7801\uff0c\u9700\u5148\u5728\u4e2a\u4eba\u4e2d\u5fc3\u5b8c\u6210\u4fee\u6539\u3002"),
        ("\u8d85\u7ea7\u7ba1\u7406\u5458", "/login \u767b\u5f55\u540e\u8bbf\u95ee /admin", "lzh", "Lzh@Colorful2026\uff0c\u5bf9\u5e94 .env \u53d8\u91cf SEED_DEMO_SUPER_ADMIN_PASSWORD\u3002\u8be5\u8d26\u53f7\u767b\u5f55\u65f6\u9700\u8f93\u5165\u57fa\u4e8e SUPER_ADMIN_TOTP_SECRET \u751f\u6210\u7684 TOTP \u52a8\u6001\u9a8c\u8bc1\u7801\u3002"),
        ("\u8fd0\u7ef4\u4eba\u5458", "\u670d\u52a1\u5668\u6587\u4ef6", "/root/colorful-tibet-first-login.txt", "\u4f7f\u7528 deploy-new-server-http.ps1 \u90e8\u7f72\u65f6\uff0c\u811a\u672c\u4f1a\u751f\u6210 lzh\u3001admin\u3001user1 \u7684\u9996\u6b21\u767b\u5f55\u5bc6\u7801\u548c TOTP \u5bc6\u94a5\uff0c\u5e76\u4fdd\u5b58\u5728\u8be5\u6587\u4ef6\u4e2d\u3002"),
    ], [1300, 2000, 2260, 3800])
    add_para(doc, "\u5982\u9700\u5728\u672c\u5730\u9a8c\u6536\u73af\u5883\u542f\u7528\u4e0a\u8868\u56fa\u5b9a\u8d26\u53f7\uff0c\u53ef\u5728 .env \u4e2d\u52a0\u5165\u6216\u4fee\u6539\u4ee5\u4e0b\u914d\u7f6e\uff1a")
    add_command_block(doc, [
        "SEED_DEMO_USERS=true",
        "SUPER_ADMIN_USERNAME=lzh",
        "SUPER_ADMIN_TOTP_SECRET=JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP",
        "SEED_DEMO_ADMIN_PASSWORD=Admin@Colorful2026",
        "SEED_DEMO_SUPER_ADMIN_PASSWORD=Lzh@Colorful2026",
        "SEED_DEMO_USER_PASSWORD=User@Colorful2026",
    ])
    add_note(doc, "\u8d26\u53f7\u5b89\u5168\u8bf4\u660e", "\u6f14\u793a\u8d26\u53f7\u4ec5\u5efa\u8bae\u7528\u4e8e\u672c\u5730\u6216\u9a8c\u6536\u73af\u5883\u3002DataSeeder \u4e0d\u4f1a\u8986\u76d6\u5df2\u5b58\u5728\u7528\u6237\u7684\u5bc6\u7801\uff1b\u82e5\u6570\u636e\u5e93\u4e2d\u5df2\u6709\u540c\u540d\u8d26\u53f7\uff0c\u5b9e\u9645\u5bc6\u7801\u4ee5\u6570\u636e\u5e93\u5df2\u4fdd\u5b58\u7684\u8d26\u53f7\u5bc6\u7801\u4e3a\u51c6\u3002\u751f\u4ea7\u73af\u5883\u5e94\u5173\u95ed SEED_DEMO_USERS\uff0c\u5e76\u5728\u9996\u6b21\u767b\u5f55\u540e\u7acb\u5373\u4fee\u6539\u521d\u59cb\u5bc6\u7801\u3002")

    add_heading(doc, "2 \u6e38\u5ba2\u529f\u80fd\u6a21\u5757", 1)
    add_para(doc, "\u6e38\u5ba2\u65e0\u9700\u767b\u5f55\u5373\u53ef\u6d4f\u89c8\u516c\u5f00\u5185\u5bb9\uff0c\u5305\u62ec\u9996\u9875\u3001\u666f\u70b9\u5217\u8868\u3001\u666f\u70b9\u8be6\u60c5\u3001\u975e\u9057\u6587\u5316\u3001\u65b0\u95fb\u8d44\u8baf\u3001\u9152\u5e97\u5217\u8868\u3001\u8def\u7ebf\u793e\u533a\u516c\u5f00\u5185\u5bb9\u7b49\u3002\u6d89\u53ca\u9884\u8ba2\u3001\u8bc4\u8bba\u3001\u6536\u85cf\u3001\u53d1\u5e03\u548c\u4e2a\u4eba\u4e2d\u5fc3\u7684\u529f\u80fd\u9700\u8981\u5148\u6ce8\u518c\u5e76\u767b\u5f55\u3002")
    sections = [
        ("2.1 \u9996\u9875\u6d4f\u89c8\u6a21\u5757", [
            "\u8bbf\u95ee\u7cfb\u7edf\u9996\u9875 http://localhost \u6216\u751f\u4ea7\u73af\u5883\u57df\u540d\u3002",
            "\u67e5\u770b\u9876\u90e8\u5bfc\u822a\u680f\uff0c\u53ef\u8fdb\u5165\u666f\u70b9\u3001AI \u8def\u7ebf\u3001\u793e\u533a\u3001\u9152\u5e97\u3001\u975e\u9057\u3001\u65b0\u95fb\u7b49\u9875\u9762\u3002",
            "\u5728\u9996\u9875\u8f6e\u64ad\u56fe\u533a\u57df\u67e5\u770b\u63a8\u8350\u5185\u5bb9\uff0c\u70b9\u51fb\u56fe\u7247\u6216\u6309\u94ae\u53ef\u8df3\u8f6c\u5230\u5bf9\u5e94\u9875\u9762\u3002",
            "\u6d4f\u89c8\u9996\u9875\u63a8\u8350\u666f\u70b9\u3001\u70ed\u95e8\u5185\u5bb9\u548c\u7f51\u7ad9\u5e95\u90e8\u8054\u7cfb\u65b9\u5f0f\u3002",
        ]),
        ("2.2 \u666f\u70b9\u6d4f\u89c8\u6a21\u5757", [
            "\u70b9\u51fb\u5bfc\u822a\u680f\u201c\u666f\u70b9\u201d\u8fdb\u5165 /spots \u9875\u9762\u3002",
            "\u901a\u8fc7\u5206\u7c7b\u6309\u94ae\u5207\u6362\u5168\u90e8\u3001\u81ea\u7136\u3001\u4eba\u6587\u7b49\u666f\u70b9\u7c7b\u522b\u3002",
            "\u5728\u666f\u70b9\u5361\u7247\u4e2d\u67e5\u770b\u666f\u70b9\u540d\u79f0\u3001\u56fe\u7247\u3001\u7968\u4ef7\u3001\u6807\u7b7e\u548c\u7b80\u8981\u4ecb\u7ecd\u3002",
            "\u70b9\u51fb\u666f\u70b9\u5361\u7247\u6216\u201c\u67e5\u770b\u8be6\u60c5\u201d\u8fdb\u5165\u666f\u70b9\u8be6\u60c5\u9875\u3002",
        ]),
        ("2.3 \u666f\u70b9\u8be6\u60c5\u4e0e\u8bc4\u8bba\u6d4f\u89c8\u6a21\u5757", [
            "\u8fdb\u5165\u666f\u70b9\u8be6\u60c5\u9875\u540e\uff0c\u67e5\u770b\u666f\u70b9\u56fe\u7247\u3001\u540d\u79f0\u3001\u85cf\u6587\u540d\u79f0\u3001\u63cf\u8ff0\u3001\u6807\u7b7e\u3001\u7968\u4ef7\u548c\u5730\u56fe\u4fe1\u606f\u3002",
            "\u5411\u4e0b\u6d4f\u89c8\u7528\u6237\u8bc4\u8bba\uff0c\u67e5\u770b\u8bc4\u5206\u3001\u8bc4\u8bba\u5185\u5bb9\u3001\u56fe\u7247\u548c\u70b9\u8d5e\u6570\u91cf\u3002",
            "\u5982\u9700\u53d1\u8868\u8bc4\u8bba\u3001\u70b9\u8d5e\u8bc4\u8bba\u6216\u9884\u8ba2\u95e8\u7968\uff0c\u7cfb\u7edf\u4f1a\u8981\u6c42\u5148\u767b\u5f55\u3002",
            "\u70b9\u51fb\u5730\u56fe\u6216\u5bfc\u822a\u76f8\u5173\u5165\u53e3\uff0c\u53ef\u7ed3\u5408\u9875\u9762\u63d0\u4f9b\u7684\u4f4d\u7f6e\u4fe1\u606f\u89c4\u5212\u884c\u7a0b\u3002",
        ]),
        ("2.4 \u975e\u9057\u6587\u5316\u6d4f\u89c8\u6a21\u5757", [
            "\u70b9\u51fb\u5bfc\u822a\u680f\u201c\u975e\u9057\u201d\u8fdb\u5165 /heritage \u9875\u9762\u3002",
            "\u4f7f\u7528\u641c\u7d22\u6846\u8f93\u5165\u975e\u9057\u9879\u76ee\u540d\u79f0\u3001\u7c7b\u522b\u6216\u5730\u533a\u5173\u952e\u8bcd\u3002",
            "\u6309\u7c7b\u522b\u67e5\u770b\u6c11\u95f4\u6587\u5b66\u3001\u4f20\u7edf\u97f3\u4e50\u3001\u4f20\u7edf\u821e\u8e48\u3001\u4f20\u7edf\u6280\u827a\u3001\u6c11\u4fd7\u7b49\u5185\u5bb9\u3002",
            "\u70b9\u51fb\u9879\u76ee\u5361\u7247\u67e5\u770b\u57fa\u7840\u4ecb\u7ecd\u3001\u8d77\u6e90\u6545\u4e8b\u3001\u6587\u5316\u4ef7\u503c\u3001\u7ebf\u4e0b\u4f53\u9a8c\u70b9\u3001\u4f20\u627f\u4eba\u548c\u6d3b\u52a8\u4fe1\u606f\u3002",
            "\u5b8c\u6574\u6545\u4e8b\u3001\u8bc4\u8bba\u548c\u4e92\u52a8\u529f\u80fd\u53ef\u80fd\u9700\u8981\u767b\u5f55\u540e\u67e5\u770b\u6216\u64cd\u4f5c\u3002",
        ]),
        ("2.5 \u65b0\u95fb\u8d44\u8baf\u6a21\u5757", [
            "\u70b9\u51fb\u5bfc\u822a\u680f\u201c\u65b0\u95fb\u201d\u8fdb\u5165 /news \u9875\u9762\u3002",
            "\u6d4f\u89c8\u653f\u7b56\u3001\u6d3b\u52a8\u3001\u901a\u77e5\u7b49\u65b0\u95fb\u5185\u5bb9\u3002",
            "\u70b9\u51fb\u65b0\u95fb\u6761\u76ee\u67e5\u770b\u8be6\u60c5\u4fe1\u606f\uff0c\u4e86\u89e3\u65c5\u6e38\u516c\u544a\u3001\u6d3b\u52a8\u5b89\u6392\u548c\u5e73\u53f0\u52a8\u6001\u3002",
        ]),
        ("2.6 \u9152\u5e97\u6d4f\u89c8\u6a21\u5757", [
            "\u70b9\u51fb\u5bfc\u822a\u680f\u201c\u9152\u5e97\u201d\u8fdb\u5165 /hotels \u9875\u9762\u3002",
            "\u901a\u8fc7\u5173\u952e\u8bcd\u3001\u57ce\u5e02\u548c\u661f\u7ea7\u7b5b\u9009\u9152\u5e97\u3002",
            "\u5728\u9152\u5e97\u5217\u8868\u4e2d\u67e5\u770b\u9152\u5e97\u56fe\u7247\u3001\u5730\u5740\u3001\u4ef7\u683c\u533a\u95f4\u3001\u8bc4\u5206\u548c\u8bbe\u65bd\u3002",
            "\u70b9\u51fb\u9152\u5e97\u8fdb\u5165\u8be6\u60c5\u9875\uff0c\u67e5\u770b\u623f\u578b\u3001\u4ef7\u683c\u3001\u53ef\u4f4f\u4eba\u6570\u548c\u8bbe\u65bd\u8bf4\u660e\u3002",
            "\u70b9\u51fb\u201c\u9884\u8ba2\u623f\u578b\u201d\u4f1a\u8fdb\u5165\u9884\u8ba2\u9875\u9762\uff0c\u672a\u767b\u5f55\u65f6\u9700\u5148\u5b8c\u6210\u767b\u5f55\u3002",
        ]),
    ]
    for title, items in sections:
        add_heading(doc, title, 2)
        add_numbered(doc, items)

    add_heading(doc, "3 \u6ce8\u518c\u7528\u6237\u529f\u80fd\u6a21\u5757", 1)
    add_para(doc, "\u6ce8\u518c\u7528\u6237\u767b\u5f55\u540e\uff0c\u53ef\u4ee5\u4f7f\u7528\u8def\u7ebf\u751f\u6210\u3001\u8def\u7ebf\u5206\u4eab\u3001\u666f\u70b9\u95e8\u7968\u9884\u8ba2\u3001\u9152\u5e97\u9884\u8ba2\u3001\u6536\u85cf\u3001\u8ba2\u5355\u4e2d\u5fc3\u3001\u4e2a\u4eba\u8d44\u6599\u7ef4\u62a4\u3001\u8bc4\u8bba\u548c\u793e\u533a\u95ee\u7b54\u7b49\u529f\u80fd\u3002")
    user_sections = [
        ("3.1 \u6ce8\u518c\u6a21\u5757", [
            "\u70b9\u51fb\u5bfc\u822a\u680f\u201c\u6ce8\u518c\u201d\u6216\u767b\u5f55\u9875\u4e2d\u7684\u201c\u7acb\u5373\u6ce8\u518c\u201d\u3002",
            "\u586b\u5199\u7528\u6237\u540d\u3001\u6635\u79f0\u3001\u5bc6\u7801\u548c\u786e\u8ba4\u5bc6\u7801\u3002",
            "\u786e\u8ba4\u4e24\u6b21\u8f93\u5165\u7684\u5bc6\u7801\u4e00\u81f4\u540e\u70b9\u51fb\u6ce8\u518c\u6309\u94ae\u3002",
            "\u6ce8\u518c\u6210\u529f\u540e\u8df3\u8f6c\u5230\u767b\u5f55\u9875\uff0c\u4f7f\u7528\u65b0\u8d26\u53f7\u767b\u5f55\u7cfb\u7edf\u3002",
        ]),
        ("3.2 \u767b\u5f55\u6a21\u5757", [
            "\u8fdb\u5165 /login \u9875\u9762\u3002",
            "\u8f93\u5165\u7528\u6237\u540d\u548c\u5bc6\u7801\uff0c\u70b9\u51fb\u767b\u5f55\u3002",
            "\u5982\u7cfb\u7edf\u63d0\u793a\u9700\u8981\u4e8c\u6b21\u8ba4\u8bc1\u6216\u4e8c\u7ea7\u5bc6\u7801\uff0c\u6309\u7ba1\u7406\u5458\u63d0\u4f9b\u7684\u4fe1\u606f\u586b\u5199\u3002",
            "\u666e\u901a\u7528\u6237\u767b\u5f55\u6210\u529f\u540e\u8fdb\u5165\u9996\u9875\uff1b\u7ba1\u7406\u5458\u767b\u5f55\u6210\u529f\u540e\u53ef\u8fdb\u5165\u540e\u53f0\u7ba1\u7406\u9875\u3002",
            "\u82e5\u8d26\u53f7\u88ab\u8981\u6c42\u4fee\u6539\u521d\u59cb\u5bc6\u7801\uff0c\u7cfb\u7edf\u4f1a\u5f15\u5bfc\u8fdb\u5165\u4e2a\u4eba\u4e2d\u5fc3\u5b8c\u6210\u4fee\u6539\u3002",
        ]),
        ("3.3 AI \u8def\u7ebf\u89c4\u5212\u6a21\u5757", [
            "\u70b9\u51fb\u5bfc\u822a\u680f\u201cAI \u8def\u7ebf\u201d\u6216\u8bbf\u95ee /route-planner\u3002",
            "\u8bbe\u7f6e\u8ba1\u5212\u5929\u6570\uff0c\u7cfb\u7edf\u652f\u6301\u901a\u8fc7\u52a0\u51cf\u6309\u94ae\u8c03\u6574\u5929\u6570\u3002",
            "\u9009\u62e9\u9884\u7b97\u8303\u56f4\uff0c\u5305\u62ec\u7ecf\u6d4e\u3001\u8212\u9002\u3001\u8c6a\u534e\u7b49\u9009\u9879\u3002",
            "\u9009\u62e9\u65c5\u884c\u504f\u597d\uff0c\u5305\u62ec\u81ea\u7136\u98ce\u5149\u3001\u4eba\u6587\u5386\u53f2\u3001\u6444\u5f71\u6253\u5361\u3001\u4f11\u95f2\u7597\u6108\u7b49\u9009\u9879\u3002",
            "\u4e5f\u53ef\u4ee5\u4f7f\u7528\u5feb\u901f\u9884\u8bbe\uff0c\u4f8b\u5982\u81ea\u7136\u98ce\u5149\u3001\u6587\u5316\u6df1\u5ea6\u3001\u6444\u5f71\u8def\u7ebf\u3001\u4f11\u95f2\u8def\u7ebf\u3002",
            "\u70b9\u51fb\u201c\u751f\u6210\u8def\u7ebf\u201d\uff0c\u7b49\u5f85 AI \u8f93\u51fa\u8def\u7ebf\u5185\u5bb9\u3002",
            "\u751f\u6210\u5b8c\u6210\u540e\uff0c\u53ef\u6267\u884c\u4fdd\u5b58\u8def\u7ebf\u3001\u4e0b\u8f7d Markdown\u3001\u5206\u4eab\u5230\u8def\u7ebf\u793e\u533a\u7b49\u64cd\u4f5c\u3002",
        ]),
        ("3.4 \u624b\u52a8\u53d1\u5e03\u8def\u7ebf\u6a21\u5757", [
            "\u8fdb\u5165\u8def\u7ebf\u793e\u533a\u9875\u9762\uff0c\u70b9\u51fb\u201c\u521b\u5efa\u6211\u7684\u8def\u7ebf\u201d\uff0c\u6216\u8bbf\u95ee /create-route\u3002",
            "\u586b\u5199\u8def\u7ebf\u6807\u9898\uff0c\u6807\u9898\u6700\u591a 200 \u4e2a\u5b57\u7b26\u3002",
            "\u9009\u62e9\u8ba1\u5212\u5929\u6570\u3001\u9884\u7b97\u8303\u56f4\u548c\u504f\u597d\u7c7b\u578b\u3002",
            "\u5728\u8def\u7ebf\u5185\u5bb9\u6587\u672c\u6846\u4e2d\u586b\u5199\u8be6\u7ec6\u884c\u7a0b\u5b89\u6392\u3002",
            "\u70b9\u51fb\u53d1\u5e03\u6309\u94ae\uff0c\u53d1\u5e03\u6210\u529f\u540e\u8def\u7ebf\u4f1a\u8fdb\u5165\u793e\u533a\u5217\u8868\u3002",
        ]),
        ("3.5 \u8def\u7ebf\u793e\u533a\u6a21\u5757", [
            "\u8bbf\u95ee /community \u9875\u9762\u3002",
            "\u5728\u201c\u5171\u4eab\u8def\u7ebf\u201d\u9875\u7b7e\u4e2d\uff0c\u53ef\u6309\u5929\u6570\u3001\u9884\u7b97\u548c\u504f\u597d\u7b5b\u9009\u8def\u7ebf\u3002",
            "\u70b9\u51fb\u8def\u7ebf\u5361\u7247\u8fdb\u5165\u8be6\u60c5\u9875\uff0c\u67e5\u770b\u8def\u7ebf\u5185\u5bb9\u3001\u6d4f\u89c8\u91cf\u3001\u70b9\u8d5e\u6570\u548c\u8bc4\u8bba\u3002",
            "\u767b\u5f55\u540e\u53ef\u4ee5\u70b9\u8d5e\u3001\u8bc4\u8bba\uff0c\u4e5f\u53ef\u4ee5\u5220\u9664\u81ea\u5df1\u53d1\u5e03\u7684\u8def\u7ebf\u6216\u8bc4\u8bba\u3002",
            "\u5728\u201c\u65c5\u884c\u95ee\u7b54\u201d\u9875\u7b7e\u4e2d\uff0c\u53ef\u6309\u6807\u7b7e\u548c\u6392\u5e8f\u65b9\u5f0f\u7b5b\u9009\u95ee\u9898\u3002",
            "\u70b9\u51fb\u201c\u6211\u8981\u63d0\u95ee\u201d\u53d1\u5e03\u95ee\u9898\uff0c\u586b\u5199\u6807\u9898\u3001\u5185\u5bb9\u548c\u6807\u7b7e\u3002",
            "\u8fdb\u5165\u95ee\u9898\u8be6\u60c5\u540e\u53ef\u4ee5\u56de\u7b54\u95ee\u9898\uff0c\u95ee\u9898\u4f5c\u8005\u53ef\u91c7\u7eb3\u6ee1\u610f\u7b54\u6848\u3002",
        ]),
        ("3.6 \u666f\u70b9\u95e8\u7968\u9884\u8ba2\u6a21\u5757", [
            "\u5728\u666f\u70b9\u8be6\u60c5\u9875\u53f3\u4fa7\u6216\u79fb\u52a8\u7aef\u5e95\u90e8\u64cd\u4f5c\u680f\u4e2d\u627e\u5230\u9884\u8ba2\u533a\u57df\u3002",
            "\u9009\u62e9\u6e38\u73a9\u65e5\u671f\u3002",
            "\u901a\u8fc7\u52a0\u51cf\u6309\u94ae\u9009\u62e9\u95e8\u7968\u6570\u91cf\u3002",
            "\u786e\u8ba4\u603b\u91d1\u989d\u540e\u70b9\u51fb\u201c\u786e\u8ba4\u652f\u4ed8\u201d\u3002",
            "\u5b8c\u6210\u652f\u4ed8\u5f39\u7a97\u4e2d\u7684\u652f\u4ed8\u786e\u8ba4\u540e\uff0c\u7cfb\u7edf\u521b\u5efa\u666f\u70b9\u95e8\u7968\u8ba2\u5355\u3002",
            "\u8ba2\u5355\u53ef\u5728\u4e2a\u4eba\u4e2d\u5fc3\u6216\u8ba2\u5355\u4e2d\u5fc3\u4e2d\u67e5\u770b\u3001\u53d6\u6d88\u6216\u5220\u9664\u3002",
        ]),
        ("3.7 \u9152\u5e97\u9884\u8ba2\u6a21\u5757", [
            "\u5728\u9152\u5e97\u8be6\u60c5\u9875\u9009\u62e9\u623f\u578b\uff0c\u70b9\u51fb\u201c\u9884\u8ba2\u6b64\u623f\u578b\u201d\u3002",
            "\u8fdb\u5165\u9152\u5e97\u9884\u8ba2\u9875\u540e\uff0c\u9009\u62e9\u5165\u4f4f\u65e5\u671f\u548c\u79bb\u5e97\u65e5\u671f\u3002",
            "\u9009\u62e9\u5165\u4f4f\u4eba\u6570\u3002",
            "\u586b\u5199\u9884\u8ba2\u4eba\u59d3\u540d\u3001\u624b\u673a\u53f7\u548c\u5907\u6ce8\u4fe1\u606f\u3002",
            "\u786e\u8ba4\u623f\u578b\u3001\u5165\u4f4f\u665a\u6570\u548c\u603b\u91d1\u989d\u3002",
            "\u70b9\u51fb\u786e\u8ba4\u9884\u8ba2\u5e76\u5b8c\u6210\u652f\u4ed8\u786e\u8ba4\u3002",
            "\u9884\u8ba2\u8bb0\u5f55\u53ef\u5728\u201c\u9152\u5e97\u8ba2\u5355\u201d\u6216\u4e2a\u4eba\u4e2d\u5fc3\u4e2d\u67e5\u770b\u3002",
        ]),
        ("3.8 \u6536\u85cf\u6a21\u5757", [
            "\u5728\u8def\u7ebf\u8be6\u60c5\u9875\u70b9\u51fb\u6536\u85cf\u5165\u53e3\uff0c\u5c06\u8def\u7ebf\u52a0\u5165\u6536\u85cf\u3002",
            "\u8bbf\u95ee /favorites \u9875\u9762\u67e5\u770b\u5df2\u6536\u85cf\u8def\u7ebf\u3002",
            "\u70b9\u51fb\u6536\u85cf\u5361\u7247\u53ef\u8fdb\u5165\u8def\u7ebf\u8be6\u60c5\u3002",
            "\u70b9\u51fb\u201c\u53d6\u6d88\u6536\u85cf\u201d\u53ef\u4ece\u6536\u85cf\u5217\u8868\u4e2d\u79fb\u9664\u8be5\u8def\u7ebf\u3002",
        ]),
        ("3.9 \u8ba2\u5355\u4e2d\u5fc3\u6a21\u5757", [
            "\u8bbf\u95ee /orders \u9875\u9762\u8fdb\u5165\u7edf\u4e00\u8ba2\u5355\u4e2d\u5fc3\u3002",
            "\u901a\u8fc7\u72b6\u6001\u9875\u7b7e\u67e5\u770b\u5168\u90e8\u3001\u5f85\u652f\u4ed8\u3001\u5df2\u786e\u8ba4\u3001\u9000\u6b3e\u4e2d\u3001\u5df2\u5173\u95ed\u7b49\u8ba2\u5355\u3002",
            "\u4f7f\u7528\u641c\u7d22\u6846\u6309\u8ba2\u5355\u53f7\u3001\u5546\u54c1\u6216\u51ed\u8bc1\u8fdb\u884c\u67e5\u8be2\u3002",
            "\u70b9\u51fb\u8ba2\u5355\u67e5\u770b\u8be6\u60c5\uff0c\u5305\u62ec\u5546\u54c1\u3001\u91d1\u989d\u3001\u652f\u4ed8\u72b6\u6001\u3001\u51ed\u8bc1\u3001\u9000\u6b3e\u548c\u53d1\u7968\u4fe1\u606f\u3002",
            "\u5bf9\u7b26\u5408\u6761\u4ef6\u7684\u8ba2\u5355\u53ef\u6267\u884c\u53d6\u6d88\u3001\u7533\u8bf7\u9000\u6b3e\u3001\u7533\u8bf7\u53d1\u7968\u6216\u5220\u9664\u64cd\u4f5c\u3002",
            "\u9152\u5e97\u8ba2\u5355\u4e5f\u53ef\u4ee5\u901a\u8fc7 /hotel-orders \u6216\u4e2a\u4eba\u4e2d\u5fc3\u4e2d\u7684\u9152\u5e97\u9884\u8ba2\u8bb0\u5f55\u67e5\u770b\u3002",
        ]),
        ("3.10 \u4e2a\u4eba\u4e2d\u5fc3\u6a21\u5757", [
            "\u767b\u5f55\u540e\u70b9\u51fb\u201c\u4e2a\u4eba\u4e2d\u5fc3\u201d\u6216\u8bbf\u95ee /profile\u3002",
            "\u67e5\u770b\u4e2a\u4eba\u7edf\u8ba1\u4fe1\u606f\uff0c\u5305\u62ec\u8def\u7ebf\u3001\u8bc4\u8bba\u3001\u9884\u8ba2\u7b49\u6570\u91cf\u3002",
            "\u5728\u8d44\u6599\u533a\u57df\u4fee\u6539\u6635\u79f0\u3001\u4e0a\u4f20\u5934\u50cf\u3002",
            "\u5728\u5bc6\u7801\u533a\u57df\u8f93\u5165\u65e7\u5bc6\u7801\u3001\u65b0\u5bc6\u7801\u548c\u786e\u8ba4\u5bc6\u7801\uff0c\u5b8c\u6210\u5bc6\u7801\u4fee\u6539\u3002",
            "\u5728\u201c\u6211\u7684\u8def\u7ebf\u201d\u4e2d\u67e5\u770b\u6216\u5220\u9664\u81ea\u5df1\u53d1\u5e03\u7684\u8def\u7ebf\u3002",
            "\u5728\u201c\u6211\u7684\u9884\u8ba2\u201d\u548c\u201c\u9152\u5e97\u9884\u8ba2\u201d\u4e2d\u67e5\u770b\u3001\u53d6\u6d88\u6216\u5220\u9664\u8ba2\u5355\u3002",
            "\u5728\u201c\u6211\u7684\u8bc4\u8bba\u201d\u4e2d\u67e5\u770b\u666f\u70b9\u8bc4\u8bba\u548c\u8def\u7ebf\u8bc4\u8bba\uff0c\u5e76\u5220\u9664\u81ea\u5df1\u7684\u8bc4\u8bba\u3002",
        ]),
    ]
    for title, items in user_sections:
        add_heading(doc, title, 2)
        add_numbered(doc, items)
        if title.startswith("3.2"):
            add_note(doc, "\u8d26\u53f7\u8bf4\u660e", "\u672c\u9879\u76ee\u4e0d\u5728\u624b\u518c\u4e2d\u56fa\u5b9a\u5199\u6b7b\u9ed8\u8ba4\u5bc6\u7801\u3002\u6f14\u793a\u8d26\u53f7\u662f\u5426\u521b\u5efa\u3001\u7ba1\u7406\u5458\u8d26\u53f7\u540d\u79f0\u548c\u521d\u59cb\u5bc6\u7801\uff0c\u5747\u4ee5 .env \u4e2d\u7684\u79cd\u5b50\u6570\u636e\u914d\u7f6e\u3001\u6570\u636e\u5e93\u8bb0\u5f55\u548c\u7ba1\u7406\u5458\u5b9e\u9645\u5206\u914d\u4fe1\u606f\u4e3a\u51c6\u3002")
        if title.startswith("3.3"):
            add_para(doc, "\u5982\u679c\u914d\u7f6e\u4e86\u771f\u5b9e\u6a21\u578b Key\uff0c\u7cfb\u7edf\u4f1a\u8c03\u7528 AI \u751f\u6210\u670d\u52a1\uff1b\u5982\u679c\u672a\u914d\u7f6e\uff0c\u9875\u9762\u4f1a\u6839\u636e\u540e\u7aef\u515c\u5e95\u903b\u8f91\u8fd4\u56de\u63d0\u793a\u6216\u793a\u4f8b\u5185\u5bb9\u3002")

    add_heading(doc, "4 \u7ba1\u7406\u5458\u529f\u80fd\u6a21\u5757", 1)
    add_para(doc, "\u7ba1\u7406\u5458\u529f\u80fd\u96c6\u4e2d\u5728 /admin \u9875\u9762\u3002\u53ea\u6709\u62e5\u6709 ADMIN \u89d2\u8272\u7684\u8d26\u53f7\u53ef\u4ee5\u8bbf\u95ee\u540e\u53f0\uff1b\u672a\u767b\u5f55\u7528\u6237\u8bbf\u95ee\u540e\u53f0\u4f1a\u8df3\u8f6c\u767b\u5f55\u9875\uff0c\u975e\u7ba1\u7406\u5458\u8d26\u53f7\u4f1a\u88ab\u91cd\u5b9a\u5411\u5230\u9996\u9875\u3002")
    admin_sections = [
        ("4.1 \u540e\u53f0\u767b\u5f55\u4e0e\u603b\u89c8\u6a21\u5757", [
            "\u4f7f\u7528\u7ba1\u7406\u5458\u8d26\u53f7\u767b\u5f55\u7cfb\u7edf\u3002",
            "\u5982\u7cfb\u7edf\u63d0\u793a\u4e8c\u6b21\u8ba4\u8bc1\u6216\u4e8c\u7ea7\u5bc6\u7801\uff0c\u6309\u7ba1\u7406\u5458\u5b89\u5168\u914d\u7f6e\u586b\u5199\u3002",
            "\u8bbf\u95ee /admin \u8fdb\u5165\u540e\u53f0\u7ba1\u7406\u9875\u9762\u3002",
            "\u67e5\u770b\u603b\u7528\u6237\u6570\u3001\u8ba2\u5355\u6570\u3001\u603b\u6536\u5165\u3001\u666f\u70b9\u6570\u91cf\u7b49\u7edf\u8ba1\u5361\u7247\u3002",
            "\u67e5\u770b\u8ba2\u5355\u6536\u5165\u8d8b\u52bf\u3001\u666f\u70b9\u7c7b\u522b\u5360\u6bd4\u3001\u6e38\u5ba2\u57ce\u5e02\u5206\u5e03\u3001\u70ed\u95e8\u666f\u70b9\u8bbf\u95ee\u91cf\u3001\u7528\u6237\u589e\u957f\u548c\u65b0\u95fb\u53d1\u5e03\u8d8b\u52bf\u7b49\u56fe\u8868\u3002",
        ]),
        ("4.2 \u7528\u6237\u7ba1\u7406\u6a21\u5757", [
            "\u5c55\u5f00\u201c\u7528\u6237\u7ba1\u7406\u201d\u533a\u57df\u3002",
            "\u67e5\u770b\u7528\u6237 ID\u3001\u7528\u6237\u540d\u3001\u6635\u79f0\u3001\u6ce8\u518c\u65f6\u95f4\u3001\u89d2\u8272\u548c\u9501\u5b9a\u72b6\u6001\u3002",
            "\u5bf9\u666e\u901a\u7528\u6237\u53ef\u6267\u884c\u8bbe\u4e3a\u7ba1\u7406\u5458\u64cd\u4f5c\u3002",
            "\u5bf9\u7ba1\u7406\u5458\u7528\u6237\u53ef\u6267\u884c\u53d6\u6d88\u7ba1\u7406\u5458\u64cd\u4f5c\uff0c\u4f46\u7cfb\u7edf\u4fdd\u62a4\u8d26\u53f7\u4e0d\u53ef\u968f\u610f\u964d\u6743\u3002",
            "\u5bf9\u88ab\u9501\u5b9a\u7528\u6237\u53ef\u6267\u884c\u89e3\u9501\u64cd\u4f5c\u3002",
            "\u5bf9\u5141\u8bb8\u5220\u9664\u7684\u7528\u6237\u53ef\u6267\u884c\u5220\u9664\u64cd\u4f5c\u3002",
        ]),
        ("4.3 \u666f\u70b9\u7ba1\u7406\u6a21\u5757", [
            "\u5c55\u5f00\u201c\u666f\u70b9\u7ba1\u7406\u201d\u533a\u57df\u3002",
            "\u67e5\u770b\u666f\u70b9\u5361\u7247\uff0c\u5305\u62ec\u666f\u70b9\u56fe\u7247\u3001\u540d\u79f0\u3001\u57ce\u5e02\u3001\u4ecb\u7ecd\u3001\u7968\u4ef7\u548c\u8bbf\u95ee\u91cf\u3002",
            "\u70b9\u51fb\u666f\u70b9\u5361\u7247\u8fdb\u5165\u7f16\u8f91\u5f39\u7a97\uff0c\u4fee\u6539\u666f\u70b9\u57fa\u7840\u4fe1\u606f\u548c\u56fe\u7247\u3002",
            "\u70b9\u51fb\u5355\u4e2a\u666f\u70b9\u7684\u7968\u4ef7\u5237\u65b0\u6309\u94ae\uff0c\u53ef\u6293\u53d6\u5e76\u66f4\u65b0\u8be5\u666f\u70b9\u7968\u4ef7\u3002",
            "\u70b9\u51fb\u201c\u6279\u91cf\u83b7\u53d6\u7968\u4ef7\u201d\u53ef\u542f\u52a8\u6279\u91cf\u7968\u4ef7\u66f4\u65b0\u4efb\u52a1\uff0c\u5e76\u5728\u8fdb\u5ea6\u6761\u4e2d\u67e5\u770b\u5df2\u5904\u7406\u3001\u6210\u529f\u3001\u5931\u8d25\u548c\u8df3\u8fc7\u6570\u91cf\u3002",
            "\u4fdd\u5b58\u4fee\u6539\u540e\uff0c\u524d\u53f0\u666f\u70b9\u5217\u8868\u548c\u8be6\u60c5\u9875\u4f1a\u5c55\u793a\u6700\u65b0\u5185\u5bb9\u3002",
        ]),
        ("4.4 \u65b0\u95fb\u7ba1\u7406\u6a21\u5757", [
            "\u5c55\u5f00\u201c\u65b0\u95fb\u7ba1\u7406\u201d\u533a\u57df\u3002",
            "\u70b9\u51fb\u201c\u65b0\u589e\u65b0\u95fb\u201d\u6253\u5f00\u65b0\u95fb\u7f16\u8f91\u5f39\u7a97\u3002",
            "\u586b\u5199\u6807\u9898\u3001\u5185\u5bb9\u3001\u7c7b\u522b\u3001\u56fe\u7247\u548c\u6d4f\u89c8\u91cf\u3002",
            "\u70b9\u51fb\u4fdd\u5b58\u540e\uff0c\u65b0\u95fb\u8fdb\u5165\u524d\u53f0\u65b0\u95fb\u5217\u8868\u3002",
            "\u5bf9\u5df2\u6709\u65b0\u95fb\u53ef\u6267\u884c\u7f16\u8f91\u6216\u5220\u9664\u64cd\u4f5c\u3002",
        ]),
        ("4.5 \u8f6e\u64ad\u56fe\u7ba1\u7406\u6a21\u5757", [
            "\u5c55\u5f00\u201c\u8f6e\u64ad\u56fe\u7ba1\u7406\u201d\u533a\u57df\u3002",
            "\u70b9\u51fb\u201c\u65b0\u589e\u8f6e\u64ad\u56fe\u201d\u6253\u5f00\u7f16\u8f91\u5f39\u7a97\u3002",
            "\u586b\u5199\u6807\u9898\u3001\u526f\u6807\u9898\u3001\u6807\u7b7e\u3001\u56fe\u7247\u3001\u8df3\u8f6c\u94fe\u63a5\u3001\u6392\u5e8f\u503c\u548c\u542f\u7528\u72b6\u6001\u3002",
            "\u4fdd\u5b58\u540e\uff0c\u542f\u7528\u7684\u8f6e\u64ad\u56fe\u4f1a\u6309\u6392\u5e8f\u5728\u9996\u9875\u5c55\u793a\u3002",
            "\u53ef\u5bf9\u5df2\u6709\u8f6e\u64ad\u56fe\u6267\u884c\u7f16\u8f91\u3001\u7981\u7528\u6216\u5220\u9664\u64cd\u4f5c\u3002",
        ]),
        ("4.6 \u8def\u7ebf\u7ba1\u7406\u6a21\u5757", [
            "\u5c55\u5f00\u201c\u8def\u7ebf\u7ba1\u7406\u201d\u533a\u57df\u3002",
            "\u67e5\u770b\u5b98\u65b9\u8def\u7ebf\u6216\u540e\u53f0\u7ef4\u62a4\u7684\u8def\u7ebf\u6570\u636e\u3002",
            "\u70b9\u51fb\u65b0\u589e\u6216\u7f16\u8f91\uff0c\u586b\u5199\u6807\u9898\u3001\u5929\u6570\u3001\u9884\u7b97\u3001\u504f\u597d\u3001\u4ef7\u683c\u3001\u96be\u5ea6\u3001\u6e29\u5ea6\u3001\u5730\u7406\u4fe1\u606f\u548c\u8def\u7ebf\u5185\u5bb9\u3002",
            "\u4fdd\u5b58\u540e\uff0c\u8def\u7ebf\u53ef\u4f5c\u4e3a\u5b98\u65b9\u5185\u5bb9\u5c55\u793a\u6216\u540c\u6b65\u5230\u793e\u533a\u3002",
            "\u5bf9\u4e0d\u518d\u4f7f\u7528\u7684\u8def\u7ebf\u53ef\u6267\u884c\u5220\u9664\u64cd\u4f5c\u3002",
        ]),
        ("4.7 \u9152\u5e97\u4e0e\u623f\u578b\u7ba1\u7406\u6a21\u5757", [
            "\u5c55\u5f00\u201c\u9152\u5e97\u7ba1\u7406\u201d\u533a\u57df\u3002",
            "\u70b9\u51fb\u201c\u65b0\u589e\u9152\u5e97\u201d\u586b\u5199\u9152\u5e97\u540d\u79f0\u3001\u4f4d\u7f6e\u3001\u7535\u8bdd\u3001\u4ef7\u683c\u533a\u95f4\u3001\u8bc4\u5206\u3001\u56fe\u7247\u548c\u8bbe\u65bd\u3002",
            "\u70b9\u51fb\u5df2\u6709\u9152\u5e97\u53ef\u7f16\u8f91\u57fa\u7840\u4fe1\u606f\u3002",
            "\u5728\u9152\u5e97\u5361\u7247\u4e2d\u5c55\u5f00\u623f\u578b\u7ba1\u7406\uff0c\u7ef4\u62a4\u623f\u578b\u540d\u79f0\u3001\u4ef7\u683c\u3001\u5bb9\u91cf\u3001\u8bbe\u65bd\u548c\u6392\u5e8f\u3002",
            "\u5220\u9664\u623f\u578b\u6216\u9152\u5e97\u524d\u5e94\u786e\u8ba4\u4e0d\u5b58\u5728\u9700\u8981\u4fdd\u7559\u7684\u8ba2\u5355\u5173\u8054\u3002",
        ]),
        ("4.8 \u9152\u5e97\u8ba2\u5355\u7ba1\u7406\u6a21\u5757", [
            "\u5c55\u5f00\u201c\u9152\u5e97\u8ba2\u5355\u201d\u533a\u57df\u3002",
            "\u67e5\u770b\u8ba2\u5355\u53f7\u3001\u7528\u6237\u3001\u9152\u5e97\u623f\u578b\u3001\u5165\u4f4f\u79bb\u5e97\u65e5\u671f\u3001\u9884\u8ba2\u4eba\u3001\u91d1\u989d\u3001\u72b6\u6001\u548c\u4e0b\u5355\u65f6\u95f4\u3002",
            "\u901a\u8fc7\u72b6\u6001\u4e0b\u62c9\u6846\u5c06\u8ba2\u5355\u66f4\u65b0\u4e3a\u5f85\u5904\u7406\u3001\u5df2\u786e\u8ba4\u6216\u5df2\u53d6\u6d88\u3002",
            "\u5bf9\u786e\u8ba4\u65e0\u6548\u6216\u6d4b\u8bd5\u6570\u636e\u7684\u8ba2\u5355\u53ef\u6267\u884c\u5220\u9664\u64cd\u4f5c\u3002",
        ]),
        ("4.9 \u975e\u9057\u7ba1\u7406\u6a21\u5757", [
            "\u5c55\u5f00\u201c\u975e\u9057\u7ba1\u7406\u201d\u533a\u57df\u3002",
            "\u4f7f\u7528\u641c\u7d22\u6846\u6309\u9879\u76ee\u540d\u79f0\u3001\u7c7b\u522b\u6216\u5730\u533a\u7b5b\u9009\u975e\u9057\u9879\u76ee\u3002",
            "\u70b9\u51fb\u201c\u65b0\u589e\u975e\u9057\u201d\u586b\u5199\u540d\u79f0\u3001\u85cf\u6587\u540d\u79f0\u3001\u7c7b\u522b\u3001\u5730\u533a\u3001\u4fdd\u62a4\u7ea7\u522b\u3001\u56fe\u7247\u3001\u89c6\u9891\u3001\u767e\u79d1\u94fe\u63a5\u3001\u4ecb\u7ecd\u3001\u85cf\u6587\u63cf\u8ff0\u3001\u8d77\u6e90\u6545\u4e8b\u548c\u6587\u5316\u4ef7\u503c\u3002",
            "\u5bf9\u5df2\u6709\u9879\u76ee\u53ef\u6267\u884c\u7f16\u8f91\u6216\u5220\u9664\u64cd\u4f5c\u3002",
            "\u70b9\u51fb\u201c\u4f20\u627f\u4eba/\u6d3b\u52a8\u201d\u5c55\u5f00\u5173\u8054\u7ba1\u7406\u3002",
            "\u5728\u4f20\u627f\u4eba\u533a\u57df\u7ef4\u62a4\u59d3\u540d\u3001\u7ea7\u522b\u3001\u5730\u533a\u3001\u5934\u50cf\u3001\u7b80\u4ecb\u548c\u4f20\u627f\u6545\u4e8b\u3002",
            "\u5728\u6d3b\u52a8\u533a\u57df\u7ef4\u62a4\u6807\u9898\u3001\u5f00\u59cb\u65e5\u671f\u3001\u7ed3\u675f\u65e5\u671f\u3001\u5730\u70b9\u3001\u8054\u7cfb\u65b9\u5f0f\u3001\u56fe\u7247\u548c\u6d3b\u52a8\u5185\u5bb9\u3002",
        ]),
        ("4.10 \u793e\u533a\u5185\u5bb9\u7ba1\u7406\u6a21\u5757", [
            "\u5c55\u5f00\u201c\u793e\u533a\u7ba1\u7406\u201d\u533a\u57df\u3002",
            "\u901a\u8fc7\u9875\u7b7e\u5207\u6362\u5171\u4eab\u8def\u7ebf\u3001\u65c5\u884c\u95ee\u9898\u3001\u8def\u7ebf\u8bc4\u8bba\u3001\u666f\u70b9\u8bc4\u8bba\u548c\u95ee\u7b54\u56de\u7b54\u3002",
            "\u67e5\u770b\u5185\u5bb9\u4f5c\u8005\u3001\u6765\u6e90\u3001\u72b6\u6001\u3001\u6b63\u6587\u6458\u8981\u3001\u521b\u5efa\u65f6\u95f4\u548c\u4e92\u52a8\u6570\u636e\u3002",
            "\u70b9\u51fb\u7f16\u8f91\u6309\u94ae\u53ef\u4fee\u6539\u4e0d\u89c4\u8303\u6807\u9898\u6216\u6b63\u6587\u3002",
            "\u70b9\u51fb\u5220\u9664\u6309\u94ae\u53ef\u79fb\u9664\u8fdd\u89c4\u3001\u91cd\u590d\u6216\u6d4b\u8bd5\u5185\u5bb9\u3002",
            "\u5904\u7406\u95ee\u7b54\u5185\u5bb9\u65f6\uff0c\u5e94\u4f18\u5148\u4fdd\u7559\u5df2\u91c7\u7eb3\u7b54\u6848\u548c\u6709\u4ef7\u503c\u7684\u7528\u6237\u7ecf\u9a8c\u3002",
        ]),
    ]
    for title, items in admin_sections:
        add_heading(doc, title, 2)
        add_numbered(doc, items)

    add_heading(doc, "5 \u7cfb\u7edf\u7ef4\u62a4\u4e0e\u5e38\u89c1\u95ee\u9898", 1)
    add_heading(doc, "5.1 \u5e38\u7528\u7ef4\u62a4\u547d\u4ee4", 2)
    add_table(doc, ["\u573a\u666f", "\u547d\u4ee4"], [
        ("\u67e5\u770b\u670d\u52a1\u72b6\u6001", "docker compose ps"),
        ("\u67e5\u770b\u540e\u7aef\u65e5\u5fd7", "docker compose logs -f backend"),
        ("\u67e5\u770b\u524d\u7aef\u65e5\u5fd7", "docker compose logs -f frontend"),
        ("\u91cd\u5efa\u524d\u7aef\u955c\u50cf", "docker compose build frontend && docker compose up -d frontend"),
        ("\u91cd\u5efa\u540e\u7aef\u955c\u50cf", "docker compose build backend && docker compose up -d backend"),
        ("\u505c\u6b62\u670d\u52a1", "docker compose down"),
        ("\u91cd\u7f6e\u672c\u5730\u6570\u636e\u5377", "docker compose down -v"),
    ], [2800, 6560])
    add_heading(doc, "5.2 \u5e38\u89c1\u95ee\u9898\u5904\u7406", 2)
    add_table(doc, ["\u95ee\u9898", "\u5904\u7406\u65b9\u6cd5"], [
        ("\u524d\u7aef\u65e0\u6cd5\u8bbf\u95ee", "\u68c0\u67e5 frontend \u5bb9\u5668\u662f\u5426\u542f\u52a8\uff0c\u786e\u8ba4 FRONTEND_HOST_PORT \u672a\u88ab\u5360\u7528\uff0c\u5e76\u67e5\u770b frontend \u65e5\u5fd7\u3002"),
        ("\u63a5\u53e3\u8bf7\u6c42 404 \u6216\u4ee3\u7406\u5931\u8d25", "\u786e\u8ba4 VITE_API_BASE_URL=/api\uff0cNginx \u6216 Vite \u4ee3\u7406\u914d\u7f6e\u6307\u5411\u540e\u7aef\u670d\u52a1\u3002"),
        ("\u767b\u5f55\u5931\u8d25\u6216\u9891\u7e41 401", "\u68c0\u67e5\u8d26\u53f7\u5bc6\u7801\u3001Cookie/CSRF \u914d\u7f6e\u3001JWT_SECRET \u662f\u5426\u4e00\u81f4\uff0c\u5fc5\u8981\u65f6\u91cd\u65b0\u767b\u5f55\u3002"),
        ("\u7ba1\u7406\u5458\u65e0\u6cd5\u8fdb\u5165\u540e\u53f0", "\u786e\u8ba4\u7528\u6237\u89d2\u8272\u4e3a ADMIN\uff0c\u5e76\u68c0\u67e5\u662f\u5426\u9700\u8981\u4e8c\u6b21\u8ba4\u8bc1\u6216\u4fee\u6539\u521d\u59cb\u5bc6\u7801\u3002"),
        ("AI \u8def\u7ebf\u751f\u6210\u5931\u8d25", "\u68c0\u67e5 DOUBAO_API_KEY \u6216 ARK_API_KEY \u662f\u5426\u914d\u7f6e\uff0c\u67e5\u770b backend \u65e5\u5fd7\u4e2d\u7684 AI \u670d\u52a1\u9519\u8bef\u3002"),
        ("\u7968\u4ef7\u6293\u53d6\u5931\u8d25", "\u68c0\u67e5 scrapling \u670d\u52a1\u5065\u5eb7\u72b6\u6001\u3001SCRAPLING_SERVICE_URL \u914d\u7f6e\u548c\u5916\u90e8\u76ee\u6807\u7ad9\u70b9\u53ef\u7528\u6027\u3002"),
        ("\u56fe\u7247\u4e0d\u663e\u793a", "\u68c0\u67e5\u56fe\u7247 URL\u3001/images \u9759\u6001\u8d44\u6e90\u3001/uploads \u4e0a\u4f20\u76ee\u5f55\u548c Nginx \u4ee3\u7406\u914d\u7f6e\u3002"),
        ("\u6570\u636e\u5e93\u8fde\u63a5\u5931\u8d25", "\u68c0\u67e5 MySQL \u5bb9\u5668\u3001\u7aef\u53e3\u3001\u8d26\u53f7\u5bc6\u7801\u548c MYSQL_ALLOW_PUBLIC_KEY_RETRIEVAL \u914d\u7f6e\u3002"),
    ], [2600, 6760])
    add_heading(doc, "5.3 \u4e0a\u7ebf\u524d\u68c0\u67e5", 2)
    add_numbered(doc, [
        "\u6267\u884c\u524d\u7aef\u7c7b\u578b\u68c0\u67e5\uff1acd frontend && npm run typecheck\u3002",
        "\u6267\u884c\u524d\u7aef\u751f\u4ea7\u6784\u5efa\uff1acd frontend && npm run build\u3002",
        "\u6267\u884c\u540e\u7aef\u6d4b\u8bd5\uff1acd backend && mvn -q test\u3002",
        "\u68c0\u67e5 backend/Dockerfile\u3001frontend/Dockerfile\u3001docker-compose.yml\u3001docker-compose.prod.yml\u3002",
        "\u786e\u8ba4\u751f\u4ea7\u73af\u5883\u6240\u6709\u5bc6\u94a5\u4e3a\u5f3a\u968f\u673a\u503c\uff0cCOOKIE_SECURE=true\uff0cPUBLIC_DOCS_ENABLED \u6309\u9700\u5173\u95ed\u3002",
        "\u786e\u8ba4\u6570\u636e\u5e93\u3001Redis\u3001\u4e0a\u4f20\u76ee\u5f55\u3001\u65e5\u5fd7\u76ee\u5f55\u548c\u8bc1\u4e66\u76ee\u5f55\u5df2\u7ecf\u6309\u751f\u4ea7\u73af\u5883\u8981\u6c42\u6302\u8f7d\u548c\u5907\u4efd\u3002",
    ])

    add_heading(doc, "\u9644\u5f55 A \u7cfb\u7edf\u89d2\u8272\u4e0e\u4e3b\u8981\u5165\u53e3", 1)
    add_table(doc, ["\u89d2\u8272", "\u4e3b\u8981\u5165\u53e3", "\u53ef\u7528\u529f\u80fd"], [
        ("\u6e38\u5ba2", "/", "\u9996\u9875\u3001\u666f\u70b9\u3001\u9152\u5e97\u3001\u975e\u9057\u3001\u65b0\u95fb\u548c\u516c\u5f00\u793e\u533a\u5185\u5bb9\u6d4f\u89c8"),
        ("\u6ce8\u518c\u7528\u6237", "/login\u3001/profile", "\u8def\u7ebf\u751f\u6210\u3001\u8def\u7ebf\u53d1\u5e03\u3001\u8bc4\u8bba\u3001\u6536\u85cf\u3001\u95e8\u7968\u9884\u8ba2\u3001\u9152\u5e97\u9884\u8ba2\u3001\u8ba2\u5355\u4e2d\u5fc3"),
        ("\u7ba1\u7406\u5458", "/admin", "\u6570\u636e\u7edf\u8ba1\u3001\u7528\u6237\u3001\u666f\u70b9\u3001\u65b0\u95fb\u3001\u8f6e\u64ad\u56fe\u3001\u8def\u7ebf\u3001\u9152\u5e97\u3001\u8ba2\u5355\u3001\u975e\u9057\u548c\u793e\u533a\u5185\u5bb9\u7ba1\u7406"),
        ("\u8fd0\u7ef4\u4eba\u5458", "\u9879\u76ee\u6839\u76ee\u5f55\u4e0e Docker \u73af\u5883", "\u73af\u5883\u53d8\u91cf\u914d\u7f6e\u3001\u670d\u52a1\u542f\u52a8\u3001\u65e5\u5fd7\u67e5\u770b\u3001\u5065\u5eb7\u68c0\u67e5\u3001\u5907\u4efd\u548c\u53d1\u5e03"),
    ], [1600, 2500, 5260])
    doc.add_page_break()
    add_heading(doc, "\u9644\u5f55 B \u524d\u7aef\u8def\u7531\u901f\u67e5", 1)
    add_table(doc, ["\u8def\u7531", "\u9875\u9762\u8bf4\u660e"], [
        ("/", "\u9996\u9875"),
        ("/login\u3001/register", "\u767b\u5f55\u4e0e\u6ce8\u518c"),
        ("/spots\u3001/spots/:id", "\u666f\u70b9\u5217\u8868\u4e0e\u666f\u70b9\u8be6\u60c5"),
        ("/route-planner", "AI \u8def\u7ebf\u89c4\u5212"),
        ("/community\u3001/community/:id", "\u8def\u7ebf\u793e\u533a\u4e0e\u8def\u7ebf\u8be6\u60c5"),
        ("/community/question/:id", "\u65c5\u884c\u95ee\u7b54\u8be6\u60c5"),
        ("/create-route", "\u624b\u52a8\u53d1\u5e03\u8def\u7ebf"),
        ("/hotels\u3001/hotels/:id\u3001/hotel-booking/:id", "\u9152\u5e97\u5217\u8868\u3001\u9152\u5e97\u8be6\u60c5\u4e0e\u9152\u5e97\u9884\u8ba2"),
        ("/orders\u3001/hotel-orders\u3001/favorites", "\u8ba2\u5355\u4e2d\u5fc3\u3001\u9152\u5e97\u8ba2\u5355\u4e0e\u6211\u7684\u6536\u85cf"),
        ("/heritage\u3001/news", "\u975e\u9057\u6587\u5316\u4e0e\u65b0\u95fb\u8d44\u8baf"),
        ("/profile", "\u4e2a\u4eba\u4e2d\u5fc3"),
        ("/admin", "\u7ba1\u7406\u5458\u540e\u53f0"),
    ], [2800, 6560], trailing_paragraph=False, header_size=9.5, body_size=9, line_spacing=1.0, cell_margins=(40, 80, 40, 80))

    OUT.parent.mkdir(parents=True, exist_ok=True)
    doc.save(str(OUT))
    print(OUT)


if __name__ == "__main__":
    build_doc()
