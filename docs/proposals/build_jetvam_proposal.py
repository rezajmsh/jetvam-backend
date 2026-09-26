from pathlib import Path
from datetime import date

from PIL import Image, ImageDraw, ImageFont
from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.style import WD_STYLE_TYPE
from docx.enum.table import WD_CELL_VERTICAL_ALIGNMENT, WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor


ROOT = Path(__file__).resolve().parent
ASSETS = ROOT / "assets"
QA = ROOT / "qa"
OUTPUT = ROOT / "Jetvam_Implementation_Proposal_FA.docx"

NAVY = "102A43"
TEAL = "007F86"
CYAN = "2CB1BC"
GOLD = "C99A3D"
PALE_BLUE = "EAF4F7"
PALE_GRAY = "F4F6F8"
MID_GRAY = "66788A"
LIGHT_BORDER = "D9D9D9"
BLACK = "000000"
WHITE = "FFFFFF"
FONT = "IRANSansXFaNum"
FONT_FALLBACK = "Tahoma"


def font(size, bold=False):
    path = Path("C:/Windows/Fonts/IRANSansXFaNum-Regular.ttf")
    bold_path = Path("C:/Windows/Fonts/tahomabd.ttf")
    regular_path = Path("C:/Windows/Fonts/tahoma.ttf")
    selected = bold_path if bold and bold_path.exists() else path if path.exists() else regular_path
    return ImageFont.truetype(str(selected), size=size)


def draw_centered(draw, box, text, fill, fnt):
    if isinstance(fill, str) and len(fill) == 6 and not fill.startswith("#"):
        fill = f"#{fill}"
    x1, y1, x2, y2 = box
    bbox = draw.textbbox((0, 0), text, font=fnt)
    x = x1 + (x2 - x1 - (bbox[2] - bbox[0])) / 2
    y = y1 + (y2 - y1 - (bbox[3] - bbox[1])) / 2 - bbox[1]
    draw.text((x, y), text, fill=fill, font=fnt)


def rounded_box(draw, box, fill, outline, label, label_font, label_color=WHITE, radius=24):
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=3)
    draw_centered(draw, box, label, label_color, label_font)


def arrow(draw, start, end, fill=CYAN, width=6):
    draw.line([start, end], fill=f"#{fill}", width=width)
    ex, ey = end
    sx, sy = start
    if abs(ex - sx) >= abs(ey - sy):
        direction = 1 if ex > sx else -1
        pts = [(ex, ey), (ex - 18 * direction, ey - 11), (ex - 18 * direction, ey + 11)]
    else:
        direction = 1 if ey > sy else -1
        pts = [(ex, ey), (ex - 11, ey - 18 * direction), (ex + 11, ey - 18 * direction)]
    draw.polygon(pts, fill=f"#{fill}")


def make_architecture_image(path):
    canvas = Image.new("RGB", (1800, 1050), "#F7FAFC")
    draw = ImageDraw.Draw(canvas)
    title_font = font(42, True)
    box_font = font(27, True)
    small_font = font(22)
    draw.text((70, 45), "JETVAM TARGET ARCHITECTURE", fill=f"#{NAVY}", font=title_font)

    personas = [(90, 190, 380, 300, "Customer"), (90, 380, 380, 490, "Merchant"),
                (90, 570, 380, 680, "Operations"), (90, 760, 380, 870, "Finance & Admin")]
    for x1, y1, x2, y2, text in personas:
        rounded_box(draw, (x1, y1, x2, y2), f"#{NAVY}", f"#{NAVY}", text, box_font)

    rounded_box(draw, (560, 145, 1240, 260), f"#{TEAL}", f"#{TEAL}", "API & Identity Access Layer", box_font)
    modules = [
        (520, 350, 820, 460, "Identity & eKYC"),
        (850, 350, 1150, 460, "Product & Rules"),
        (1180, 350, 1480, 460, "Origination"),
        (520, 520, 820, 630, "Inquiry & Scoring"),
        (850, 520, 1150, 630, "Contract & Sign"),
        (1180, 520, 1480, 630, "Facility & Credit"),
        (520, 690, 820, 800, "Merchant"),
        (850, 690, 1150, 800, "Transaction"),
        (1180, 690, 1480, 800, "Settlement"),
    ]
    for x1, y1, x2, y2, text in modules:
        rounded_box(draw, (x1, y1, x2, y2), "#FFFFFF", f"#{TEAL}", text, small_font, f"#{NAVY}")

    rounded_box(draw, (550, 890, 1450, 990), f"#{NAVY}", f"#{NAVY}",
                "Jobs  Audit  Observability  Security  Data", box_font)
    rounded_box(draw, (1550, 250, 1740, 820), "#E8F3F5", f"#{CYAN}", "Providers", box_font, f"#{NAVY}")

    for _, y1, x2, y2, _ in personas:
        arrow(draw, (x2 + 10, (y1 + y2) // 2), (550, 205))
    arrow(draw, (900, 270), (900, 335))
    arrow(draw, (1490, 575), (1535, 575))
    arrow(draw, (1000, 815), (1000, 875))
    canvas.save(path, quality=95)


def make_journey_image(path):
    canvas = Image.new("RGB", (1800, 850), "#FFFFFF")
    draw = ImageDraw.Draw(canvas)
    title_font = font(42, True)
    box_font = font(24, True)
    small_font = font(20)
    draw.text((70, 45), "CUSTOMER CREDIT JOURNEY", fill=f"#{NAVY}", font=title_font)
    steps = [
        ("1", "Registration\nOTP & eKYC"),
        ("2", "Plan\nSelection"),
        ("3", "Prioritized\nControls"),
        ("4", "Fees &\nPayments"),
        ("5", "Profile &\nGuarantees"),
        ("6", "Digital\nContract"),
        ("7", "Credit\nAllocation"),
        ("8", "Purchase &\nSettlement"),
    ]
    xs = [80, 300, 520, 740, 960, 1180, 1400, 1580]
    widths = [180, 180, 180, 180, 180, 180, 180, 180]
    for idx, ((number, label), x, w) in enumerate(zip(steps, xs, widths)):
        box = (x, 260, x + w, 480)
        fill = f"#{NAVY}" if idx in (0, 7) else f"#{TEAL}" if idx in (2, 5, 6) else "#EAF4F7"
        color = f"#{WHITE}" if idx in (0, 2, 5, 6, 7) else f"#{NAVY}"
        draw.rounded_rectangle(box, radius=28, fill=fill, outline=f"#{TEAL}", width=3)
        draw.ellipse((x + 57, 195, x + 123, 261), fill=f"#{GOLD}")
        draw_centered(draw, (x + 57, 195, x + 123, 261), number, WHITE, box_font)
        lines = label.split("\n")
        for line_idx, line in enumerate(lines):
            bbox = draw.textbbox((0, 0), line, font=box_font)
            tx = x + (w - (bbox[2] - bbox[0])) / 2
            draw.text((tx, 320 + line_idx * 48), line, fill=color, font=box_font)
        if idx < len(steps) - 1:
            arrow(draw, (x + w + 8, 370), (xs[idx + 1] - 8, 370), GOLD, 5)

    draw.rounded_rectangle((410, 590, 1380, 760), radius=28, fill="#F4F6F8", outline="#D9D9D9", width=3)
    draw.text((470, 625), "Async inquiries  provider routing  callback  retry  audit history",
              fill=f"#{MID_GRAY}", font=small_font)
    draw.text((615, 690), "Fail fast prevents unnecessary provider cost",
              fill=f"#{NAVY}", font=box_font)
    canvas.save(path, quality=95)


def make_roadmap_image(path):
    canvas = Image.new("RGB", (1800, 1000), "#FFFFFF")
    draw = ImageDraw.Draw(canvas)
    title_font = font(42, True)
    label_font = font(23, True)
    small_font = font(18)
    draw.text((70, 45), "DELIVERY ROADMAP  48 WEEKS", fill=f"#{NAVY}", font=title_font)
    left = 410
    top = 160
    col_w = 105
    row_h = 115
    phases = [
        ("Discovery & scope", 0, 2, NAVY),
        ("Platform foundation", 1, 4, TEAL),
        ("Origination & providers", 3, 8, CYAN),
        ("Merchant & settlement", 6, 10, GOLD),
        ("Hardening & UAT", 9, 11, NAVY),
        ("Go live & warranty", 11, 12, TEAL),
    ]
    for q in range(12):
        x = left + q * col_w
        draw.rectangle((x, top, x + col_w, top + 60), fill="#EAF4F7", outline="#D9D9D9", width=2)
        draw_centered(draw, (x, top, x + col_w, top + 60), f"M{q + 1}", f"#{NAVY}", small_font)
    for idx, (label, start, end, color) in enumerate(phases):
        y = top + 95 + idx * row_h
        draw.text((70, y + 25), label, fill=f"#{NAVY}", font=label_font)
        for q in range(12):
            x = left + q * col_w
            draw.rectangle((x, y, x + col_w, y + 72), fill="#FFFFFF", outline="#E3E8EC", width=2)
        draw.rounded_rectangle((left + start * col_w + 8, y + 10, left + end * col_w - 8, y + 62),
                               radius=18, fill=f"#{color}")
    canvas.save(path, quality=95)


def set_cell_shading(cell, fill):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = tc_pr.find(qn("w:shd"))
    if shd is None:
        shd = OxmlElement("w:shd")
        tc_pr.append(shd)
    shd.set(qn("w:fill"), fill)


def set_cell_margins(cell, top=120, start=140, bottom=120, end=140):
    tc = cell._tc
    tc_pr = tc.get_or_add_tcPr()
    tc_mar = tc_pr.first_child_found_in("w:tcMar")
    if tc_mar is None:
        tc_mar = OxmlElement("w:tcMar")
        tc_pr.append(tc_mar)
    for name, value in (("top", top), ("start", start), ("bottom", bottom), ("end", end)):
        node = tc_mar.find(qn(f"w:{name}"))
        if node is None:
            node = OxmlElement(f"w:{name}")
            tc_mar.append(node)
        node.set(qn("w:w"), str(value))
        node.set(qn("w:type"), "dxa")


def set_table_borders(table, color=LIGHT_BORDER, size="6"):
    tbl_pr = table._tbl.tblPr
    borders = tbl_pr.find(qn("w:tblBorders"))
    if borders is None:
        borders = OxmlElement("w:tblBorders")
        tbl_pr.append(borders)
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        el = borders.find(qn(f"w:{edge}"))
        if el is None:
            el = OxmlElement(f"w:{edge}")
            borders.append(el)
        el.set(qn("w:val"), "single")
        el.set(qn("w:sz"), size)
        el.set(qn("w:color"), color)


def set_table_rtl(table):
    tbl_pr = table._tbl.tblPr
    bidi = tbl_pr.find(qn("w:bidiVisual"))
    if bidi is None:
        bidi = OxmlElement("w:bidiVisual")
        tbl_pr.append(bidi)


def repeat_header(row):
    tr_pr = row._tr.get_or_add_trPr()
    tbl_header = OxmlElement("w:tblHeader")
    tbl_header.set(qn("w:val"), "true")
    tr_pr.append(tbl_header)


def prevent_row_split(row):
    tr_pr = row._tr.get_or_add_trPr()
    cant_split = OxmlElement("w:cantSplit")
    cant_split.set(qn("w:val"), "true")
    tr_pr.append(cant_split)


def set_repeat_table_header(row):
    repeat_header(row)


def set_run_font(run, size=None, bold=None, color=None):
    run.font.name = FONT
    run._element.get_or_add_rPr()
    fonts = run._element.rPr.rFonts
    if fonts is None:
        fonts = OxmlElement("w:rFonts")
        run._element.rPr.insert(0, fonts)
    for attr in ("ascii", "hAnsi", "cs", "eastAsia"):
        fonts.set(qn(f"w:{attr}"), FONT)
    rtl = run._element.rPr.find(qn("w:rtl"))
    if rtl is None:
        rtl = OxmlElement("w:rtl")
        run._element.rPr.append(rtl)
    rtl.set(qn("w:val"), "1")
    if size is not None:
        run.font.size = Pt(size)
    if bold is not None:
        run.bold = bold
    if color is not None:
        run.font.color.rgb = RGBColor.from_string(color)


def set_paragraph_rtl(paragraph, alignment=WD_ALIGN_PARAGRAPH.RIGHT):
    paragraph.alignment = alignment
    p_pr = paragraph._p.get_or_add_pPr()
    bidi = p_pr.find(qn("w:bidi"))
    if bidi is None:
        bidi = OxmlElement("w:bidi")
        p_pr.append(bidi)
    bidi.set(qn("w:val"), "1")


def keep_with_next(paragraph):
    p_pr = paragraph._p.get_or_add_pPr()
    node = p_pr.find(qn("w:keepNext"))
    if node is None:
        node = OxmlElement("w:keepNext")
        p_pr.append(node)


def add_text(doc, text, bold=False, size=10.8, color=BLACK, align=WD_ALIGN_PARAGRAPH.JUSTIFY,
             before=0, after=7, line_spacing=1.3, keep=False):
    p = doc.add_paragraph()
    set_paragraph_rtl(p, align)
    p.paragraph_format.space_before = Pt(before)
    p.paragraph_format.space_after = Pt(after)
    p.paragraph_format.line_spacing = line_spacing
    if keep:
        keep_with_next(p)
    r = p.add_run(text)
    set_run_font(r, size, bold, color)
    return p


def add_heading(doc, text, level=1):
    p = doc.add_paragraph(style=f"Heading {level}")
    set_paragraph_rtl(p, WD_ALIGN_PARAGRAPH.RIGHT)
    keep_with_next(p)
    p.paragraph_format.space_before = Pt(12 if level == 1 else 8)
    p.paragraph_format.space_after = Pt(7)
    r = p.add_run(text)
    set_run_font(r, 17 if level == 1 else 13, True, BLACK)
    return p


def add_bullets(doc, items, level=0):
    for item in items:
        p = doc.add_paragraph(style="List Bullet" if level == 0 else "List Bullet 2")
        set_paragraph_rtl(p, WD_ALIGN_PARAGRAPH.RIGHT)
        p.paragraph_format.space_after = Pt(4)
        p.paragraph_format.line_spacing = 1.25
        r = p.add_run(item)
        set_run_font(r, 10.6, False, BLACK)


def add_numbered(doc, items):
    persian_digits = str.maketrans("0123456789", "۰۱۲۳۴۵۶۷۸۹")
    for index, item in enumerate(items, 1):
        p = doc.add_paragraph()
        set_paragraph_rtl(p, WD_ALIGN_PARAGRAPH.RIGHT)
        p.paragraph_format.space_after = Pt(5)
        r = p.add_run(f"{str(index).translate(persian_digits)}  {item}")
        set_run_font(r, 10.6, False, BLACK)


def add_table(doc, headers, rows, widths=None, font_size=9.6):
    table = doc.add_table(rows=1, cols=len(headers))
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    set_table_rtl(table)
    set_table_borders(table)
    hdr = table.rows[0]
    set_repeat_table_header(hdr)
    for i, header in enumerate(headers):
        cell = hdr.cells[i]
        set_cell_shading(cell, NAVY)
        set_cell_margins(cell)
        cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
        p = cell.paragraphs[0]
        set_paragraph_rtl(p, WD_ALIGN_PARAGRAPH.CENTER)
        r = p.add_run(str(header))
        set_run_font(r, font_size, True, WHITE)
        if widths:
            cell.width = Inches(widths[i])
    for row_idx, values in enumerate(rows):
        row = table.add_row()
        prevent_row_split(row)
        cells = row.cells
        for i, value in enumerate(values):
            cell = cells[i]
            set_cell_shading(cell, PALE_BLUE if row_idx % 2 else WHITE)
            set_cell_margins(cell)
            cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
            p = cell.paragraphs[0]
            align = WD_ALIGN_PARAGRAPH.CENTER if i == 0 or len(str(value)) < 18 else WD_ALIGN_PARAGRAPH.RIGHT
            set_paragraph_rtl(p, align)
            p.paragraph_format.line_spacing = 1.15
            r = p.add_run(str(value))
            set_run_font(r, font_size, False, BLACK)
            if widths:
                cell.width = Inches(widths[i])
    doc.add_paragraph().paragraph_format.space_after = Pt(2)
    return table


def set_picture_alt(inline_shape, alt_text):
    """Adds meaningful alternative text to an inline Word image."""
    doc_pr = inline_shape._inline.docPr
    doc_pr.set("descr", alt_text)
    doc_pr.set("title", alt_text)


def add_picture(doc, path, width=Inches(7.0), caption=None, alt_text=None):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    keep_with_next(p)
    run = p.add_run()
    picture = run.add_picture(str(path), width=width)
    set_picture_alt(picture, alt_text or caption or path.stem)
    if caption:
        cp = doc.add_paragraph()
        set_paragraph_rtl(cp, WD_ALIGN_PARAGRAPH.CENTER)
        cp.paragraph_format.space_after = Pt(10)
        cr = cp.add_run(caption)
        set_run_font(cr, 9.2, False, MID_GRAY)


def add_page_break(doc):
    doc.add_page_break()


def add_page_number(paragraph):
    set_paragraph_rtl(paragraph, WD_ALIGN_PARAGRAPH.CENTER)
    r = paragraph.add_run("محرمانه  |  صفحه ")
    set_run_font(r, 8.5, False, MID_GRAY)
    run = paragraph.add_run()
    fld_begin = OxmlElement("w:fldChar")
    fld_begin.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = " PAGE "
    fld_end = OxmlElement("w:fldChar")
    fld_end.set(qn("w:fldCharType"), "end")
    run._r.extend([fld_begin, instr, fld_end])


def remove_paragraph_borders(paragraph):
    p_pr = paragraph._p.get_or_add_pPr()
    borders = p_pr.find(qn("w:pBdr"))
    if borders is not None:
        p_pr.remove(borders)


def configure_styles(doc):
    normal = doc.styles["Normal"]
    normal.font.name = FONT
    normal._element.rPr.rFonts.set(qn("w:ascii"), FONT)
    normal._element.rPr.rFonts.set(qn("w:hAnsi"), FONT)
    normal._element.rPr.rFonts.set(qn("w:cs"), FONT)
    normal.font.size = Pt(10.8)
    for style_name, size in (("Title", 25), ("Heading 1", 17), ("Heading 2", 13)):
        style = doc.styles[style_name]
        style.font.name = FONT
        style._element.rPr.rFonts.set(qn("w:ascii"), FONT)
        style._element.rPr.rFonts.set(qn("w:hAnsi"), FONT)
        style._element.rPr.rFonts.set(qn("w:cs"), FONT)
        style.font.color.rgb = RGBColor(0, 0, 0)
        style.font.size = Pt(size)
        style.font.bold = True
        if style_name == "Title":
            p_pr = style._element.get_or_add_pPr()
            borders = p_pr.find(qn("w:pBdr"))
            if borders is not None:
                p_pr.remove(borders)
    for style_name in ("List Bullet", "List Bullet 2", "List Number"):
        style = doc.styles[style_name]
        style.font.name = FONT
        style._element.rPr.rFonts.set(qn("w:cs"), FONT)
        style.font.size = Pt(10.6)


def cover_page(doc, cover_path):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_after = Pt(16)
    picture = p.add_run().add_picture(str(cover_path), width=Inches(7.05))
    set_picture_alt(
        picture,
        "تصویر مفهومی سامانه جامع جت وام با هسته بانکی، هویت، قرارداد، پذیرنده و تسویه",
    )

    title = doc.add_paragraph(style="Title")
    set_paragraph_rtl(title, WD_ALIGN_PARAGRAPH.CENTER)
    remove_paragraph_borders(title)
    title.paragraph_format.space_before = Pt(10)
    title.paragraph_format.space_after = Pt(10)
    r = title.add_run("پروپوزال طراحی توسعه استقرار و پشتیبانی سامانه جامع جت وام")
    set_run_font(r, 25, True, BLACK)

    subtitle = doc.add_paragraph()
    set_paragraph_rtl(subtitle, WD_ALIGN_PARAGRAPH.CENTER)
    sr = subtitle.add_run("پلتفرم تأمین مالی دیجیتال مدیریت درخواست تسهیلات پذیرندگان تراکنش و تسویه")
    set_run_font(sr, 12.5, False, MID_GRAY)

    doc.add_paragraph().paragraph_format.space_after = Pt(14)
    meta = add_table(doc, ["مقدار", "مشخصات"], [
        ["[نام شرکت پیشنهاددهنده]", "تهیه کننده"],
        ["[نام شرکت کارفرما]", "کارفرما"],
        ["نسخه ۱  |  ۳ مهر ۱۴۰۵", "نسخه و تاریخ"],
        ["۳۰ روز تقویمی", "اعتبار پیشنهاد"],
    ], widths=[4.5, 1.5], font_size=9.7)
    doc.add_paragraph()
    p = add_text(doc, "این سند محرمانه است و صرفاً برای ارزیابی پیشنهاد و مذاکره قراردادی در اختیار کارفرما قرار می‌گیرد.",
                 size=9.2, color=MID_GRAY, align=WD_ALIGN_PARAGRAPH.CENTER)
    p.paragraph_format.space_before = Pt(10)


def build_document():
    QA.mkdir(parents=True, exist_ok=True)
    arch = ASSETS / "jetvam-target-architecture.png"
    journey = ASSETS / "jetvam-customer-journey.png"
    roadmap = ASSETS / "jetvam-delivery-roadmap.png"
    make_architecture_image(arch)
    make_journey_image(journey)
    make_roadmap_image(roadmap)

    doc = Document()
    section = doc.sections[0]
    section.page_width = Inches(8.5)
    section.page_height = Inches(11)
    section.top_margin = Inches(0.65)
    section.bottom_margin = Inches(0.65)
    section.left_margin = Inches(0.72)
    section.right_margin = Inches(0.72)
    section.header_distance = Inches(0.28)
    section.footer_distance = Inches(0.30)
    section.different_first_page_header_footer = True
    configure_styles(doc)

    header = section.header
    hp = header.paragraphs[0]
    set_paragraph_rtl(hp, WD_ALIGN_PARAGRAPH.RIGHT)
    hr = hp.add_run("پروپوزال سامانه جامع جت وام")
    set_run_font(hr, 8.5, False, MID_GRAY)
    add_page_number(section.footer.paragraphs[0])

    cover_page(doc, ASSETS / "jetvam-proposal-cover.png")
    add_page_break(doc)

    add_heading(doc, "اطلاعات و کنترل سند", 1)
    add_table(doc, ["مقدار", "عنوان"], [
        ["پروپوزال طراحی توسعه استقرار و پشتیبانی سامانه جامع جت وام", "نام سند"],
        ["[نام شرکت پیشنهاددهنده]", "مالک پیشنهاد"],
        ["[نام شرکت کارفرما]", "مخاطب"],
        ["نسخه ۱", "نسخه"],
        ["۳ مهر ۱۴۰۵", "تاریخ"],
        ["محرمانه", "طبقه بندی"],
        ["در انتظار تکمیل اطلاعات ثبتی طرفین", "وضعیت"],
    ], widths=[4.7, 1.4])
    add_heading(doc, "هدف سند", 2)
    add_text(doc, "این پروپوزال محدوده پیشنهادی، معماری، روش اجرا، برنامه زمان‌بندی، مدل پشتیبانی و شرایط تجاری طراحی و راه‌اندازی نسخه عملیاتی سامانه جت وام را مشخص می‌کند. هدف، رسیدن به یک توافق قابل سنجش برای تولید و بهره‌برداری سامانه در محیط شرکت زیرمجموعه بانک است.")
    add_heading(doc, "مبنای تدوین", 2)
    add_bullets(doc, [
        "راهنمای فعلی سامانه و ویدئوی سفر مشتری به عنوان توصیف وضعیت جاری",
        "سند نیازمندی‌های جت وام به عنوان نقشه راه و نیازمندی‌های نسخه‌های آینده",
        "معماری و کد موجود پروژه شامل ماژول‌های هویت، محصول، درخواست، استعلام، پرداخت، پذیرنده، تراکنش و تسویه",
        "مذاکرات تکمیلی تحلیل کسب و کار و خروجی فاز شناخت که پس از شروع قرارداد نهایی می‌شود",
    ])

    add_heading(doc, "فهرست مطالب", 1)
    toc_items = [
        "خلاصه مدیریتی", "شناخت مسئله و اهداف", "محدوده پیشنهادی نسخه عملیاتی",
        "سفر مشتری و فرایندهای اصلی", "معماری راهکار", "امنیت و الزامات غیرعملکردی",
        "روش اجرا و برنامه زمان‌بندی", "تیم و حاکمیت پروژه", "تضمین کیفیت و پذیرش",
        "استقرار و بهره‌برداری", "پشتیبانی و سطح خدمت", "پیشنهاد مالی",
        "مفروضات و موارد خارج از محدوده", "ریسک‌ها و کنترل تغییرات", "شرایط قراردادی پیشنهادی",
        "گام‌های شروع پروژه", "پیوست الف تحویل‌دادنی‌ها", "پیوست ب فرهنگ واژگان",
    ]
    add_numbered(doc, toc_items)

    add_page_break(doc)
    add_heading(doc, "۱ خلاصه مدیریتی", 1)
    add_text(doc, "پیشنهاد حاضر، طراحی و پیاده‌سازی یک پلتفرم تأمین مالی دیجیتال برای مدیریت چرخه درخواست تسهیلات، احراز هویت مشتری، کنترل‌های اعتباری، قرارداد و امضای دیجیتال، تخصیص اعتبار، مدیریت پذیرندگان و تسویه تراکنش‌ها را پوشش می‌دهد. راهکار به صورت ماژولار طراحی می‌شود تا افزودن محصول، طرح، کنترل یا سرویس‌دهنده بیرونی بدون بازنویسی هسته امکان‌پذیر باشد.")
    add_text(doc, "نسخه عملیاتی پیشنهادی در یک برنامه ۴۸ هفته‌ای تحویل می‌شود. شرایط تجاری در دو مدل قابل انتخاب ارائه شده است: پرداخت ثابت به مبلغ ۲۴٫۵ میلیارد تومان یا همکاری مشارکتی مبتنی بر کارمزد با پرداخت بخشی از مبلغ اجرا، سهم کارمزدی و حداقل پرداخت تضمین‌شده. سه ماه ضمانت رفع اشکال پس از راه‌اندازی در هر دو مدل منظور شده و در مدل مشارکتی، پشتیبانی استاندارد از محل سهم کارمزدی و کف تضمین‌شده پوشش داده می‌شود.")
    add_table(doc, ["پیشنهاد", "موضوع"], [
        ["۴۸ هفته", "مدت اجرای مبنا"],
        ["۲۴٫۵ میلیارد تومان", "مبلغ اجرای پیشنهادی"],
        ["۳ ماه پس از راه‌اندازی", "دوره ضمانت"],
        ["ماژولار مونولیت با مرزبندی دامنه", "الگوی معماری پایه"],
        ["محیط توسعه آزمون پذیرش و تولید", "محیط‌های استقرار"],
        ["۸ در ۵ یا ۲۴ در ۷", "گزینه‌های پشتیبانی"],
    ], widths=[2.4, 3.8])
    add_heading(doc, "تصمیم مورد درخواست", 2)
    add_text(doc, "پیشنهاد می‌شود کارفرما ابتدا شروع فاز شناخت و تثبیت محدوده را تصویب کند. خروجی این فاز شامل فهرست نهایی فرایندها، APIهای بیرونی، معیارهای پذیرش، برنامه انتشار و نسخه نهایی برنامه هزینه خواهد بود. هر تعهد ثابت زمانی یا مالی برای قابلیت‌هایی که API یا مقررات آن‌ها هنوز مشخص نیست، پس از این فاز قطعی می‌شود.")

    add_page_break(doc)
    add_heading(doc, "۲ شناخت مسئله و اهداف", 1)
    add_heading(doc, "وضعیت کسب و کار", 2)
    add_text(doc, "سامانه جت وام چرخه‌ای را مدیریت می‌کند که از ثبت‌نام مشتری و انتخاب طرح شروع می‌شود، با استعلام‌ها و کنترل‌های اعتباری ادامه پیدا می‌کند و پس از تکمیل مدارک، پرداخت هزینه‌ها و امضای قرارداد به تخصیص اعتبار می‌رسد. مصرف اعتبار در شبکه پذیرندگان انجام می‌شود و نتیجه تراکنش‌ها باید در فرایند تطبیق و تسویه مالی پذیرندگان منعکس شود.")
    add_heading(doc, "اهداف نسخه عملیاتی", 2)
    add_bullets(doc, [
        "یکپارچه‌سازی سفر مشتری از ثبت‌نام تا تخصیص و مصرف اعتبار",
        "تعریف محصول و طرح بدون وابستگی قواعد تجاری به کد هسته",
        "اجرای کنترل‌های اعتباری با اولویت و توقف در اولین عدم احراز",
        "مدیریت چند سرویس‌دهنده بیرونی با مسیریابی، failover، TLS و احراز هویت قابل تنظیم",
        "مدیریت متمرکز پذیرنده، شعب، کاربران، ابزارهای پرداخت و قراردادها",
        "ثبت قابل حسابرسی تراکنش‌ها، مغایرت‌ها و تسویه با پذیرندگان",
        "تفکیک نقش و مجوز برای مشتری، پذیرنده، عملیات، مالی و مدیر سیستم",
        "فراهم‌کردن زیرساخت توسعه بعدی کیف پول، BNPL، جت کارت و باشگاه مشتریان",
    ])
    add_heading(doc, "شاخص‌های موفقیت پیشنهادی", 2)
    add_table(doc, ["معیار پذیرش پیشنهادی", "شاخص"], [
        ["ثبت و پیگیری انتها به انتهای درخواست در محیط UAT", "سفر درخواست"],
        ["ثبت تاریخچه، نتیجه و callback قابل بازیابی", "استعلام"],
        ["عدم اجرای کنترل و هزینه مرحله بعد پس از رد کنترل جاری", "کنترل هزینه"],
        ["تطبیق روزانه با گزارش مغایرت و قابلیت اجرای مجدد", "تراکنش"],
        ["محاسبه قابل تکرار و تأییدپذیر برای هر دوره", "تسویه"],
        ["ثبت Audit برای عملیات حساس و تغییر تنظیمات", "حسابرسی"],
    ], widths=[4.2, 2.0])

    add_heading(doc, "۳ محدوده پیشنهادی نسخه عملیاتی", 1)
    add_text(doc, "محدوده زیر نسخه قابل بهره‌برداری اولیه را تعریف می‌کند. قابلیت‌های نقشه راه تنها زمانی جزو تعهد این قرارداد هستند که در همین بخش یا پیوست نهایی محدوده ذکر شوند.")
    scope_rows = [
        ["هویت و دسترسی", "ثبت‌نام مشتری با موبایل و OTP، شاهکار، کاربران پنل، نقش‌ها، مجوزها، 2FA قابل تنظیم"],
        ["پروفایل مشتری", "اطلاعات هویتی، تماس، سکونت، شغلی، بانکی و نگهداری نسخه اطلاعات مصرف‌شده در درخواست"],
        ["محصول و طرح", "تعریف محصول، طرح، مبلغ، مدت، نرخ، کنترل، استعلام، هزینه، ضامن و وثیقه"],
        ["درخواست تسهیلات", "ایجاد درخواست، ادامه فرایند نیمه‌تمام، وضعیت‌ها، تاریخچه و عملیات پشتیبانی"],
        ["کنترل و استعلام", "سن، رتبه اعتباری، چک برگشتی و سایر کنترل‌ها با اولویت، اجرای async و callback"],
        ["پرداخت کارمزد", "ایجاد تعهد هزینه، پرداخت امن، تأیید معتبر و جلوگیری از پرداخت هزینه مراحل لغوشده"],
        ["ضمانت", "اطلاعات ضامن، چک صیادی، وثیقه و کنترل تکمیل الزامات طرح"],
        ["قرارداد", "تولید نسخه قرارداد، بایگانی، ثبت رضایت و اتصال به سرویس امضای دیجیتال"],
        ["تخصیص اعتبار", "ارسال درخواست به سرویس بیرونی، پیگیری نتیجه، retry و ثبت شناسه مرجع"],
        ["پذیرنده", "پذیرنده، شعبه، کاربر، حساب بانکی، قرارداد و ابزار پرداخت"],
        ["تراکنش", "دریافت فایل یا API تراکنش، یکتاسازی، تطبیق، برگشت و مغایرت"],
        ["تسویه", "تعریف چرخه، محاسبه سهم، تأیید مالی، ارسال تسویه و گزارش نتیجه"],
        ["اعلان", "پیامک و اعلان فرایندی با providerهای قابل جایگزینی و outbox"],
        ["پنل مدیریت", "مدیریت درخواست، تنظیمات، پذیرنده، عملیات مالی، jobها، گزارش و Audit"],
    ]
    add_table(doc, ["شرح", "حوزه"], scope_rows, widths=[4.9, 1.4], font_size=9.1)

    add_heading(doc, "تفکیک وضعیت جاری و نقشه راه", 2)
    add_table(doc, ["نحوه برخورد در پیشنهاد", "طبقه", "نمونه قابلیت"], [
        ["جزو محدوده نسخه عملیاتی", "هسته", "ثبت‌نام، درخواست، استعلام، قرارداد، تخصیص اعتبار، پذیرنده و تسویه"],
        ["آماده‌سازی معماری و قرارداد توسعه جداگانه", "نقشه راه", "کیف پول، BNPL، بازپرداخت منعطف، وصول مطالبات و CRM"],
        ["مطالعه و طراحی رابط آینده", "راهبردی", "جت کارت، اتصال شتاب و شاپرک، دفتر کل جامع و Loyalty Platform"],
    ], widths=[3.1, 1.1, 2.1], font_size=9.0)

    add_page_break(doc)
    add_heading(doc, "۴ سفر مشتری و فرایندهای اصلی", 1)
    add_picture(doc, journey, Inches(7.0), "تصویر ۱  نمای سطح بالا از سفر مشتری تا تراکنش و تسویه")
    add_heading(doc, "رفتار کنترل‌ها و استعلام‌ها", 2)
    add_text(doc, "هنگام ایجاد درخواست، تمام کنترل‌های فعال طرح با وضعیت اولیه و اولویت مصوب در درخواست ثبت می‌شوند. کنترل محلی مانند سن همان لحظه ارزیابی می‌شود. اگر کنترل به استعلام بیرونی نیاز داشته باشد، هزینه همان کنترل فعال می‌شود و پس از پرداخت، درخواست استعلام به صورت پایدار ثبت خواهد شد.")
    add_text(doc, "Job مستقل درخواست را به provider ارسال یا نتیجه را با tracking code پیگیری می‌کند. Inquiry پس از رسیدن به نتیجه نهایی callback ثبت‌شده را فراخوانی می‌کند. Origination نتیجه را روی همان کنترل اعمال می‌کند و فقط در صورت موفقیت، کنترل بعدی آزاد می‌شود. رد یک کنترل، کنترل‌ها و هزینه‌های پرداخت‌نشده بعدی را لغو می‌کند.")
    add_heading(doc, "سناریوهای مهم", 2)
    add_bullets(doc, [
        "استعلام دو مرحله‌ای که در ارسال اولیه tracking code می‌دهد و نتیجه در polling بعدی دریافت می‌شود",
        "تعویض خودکار provider براساس سلامت، اولویت و سیاست مسیریابی تعریف‌شده",
        "اجرای مجدد امن job بدون ایجاد درخواست یا تراکنش تکراری",
        "ادامه درخواست مشتری از آخرین مرحله معتبر پس از خروج یا قطع ارتباط",
        "ورود پرونده به بررسی دستی پس از پایان retryهای خطای فنی",
    ])

    add_page_break(doc)
    add_heading(doc, "۵ معماری راهکار", 1)
    add_picture(doc, arch, Inches(7.0), "تصویر ۲  معماری هدف نسخه عملیاتی")
    add_heading(doc, "الگوی معماری", 2)
    add_text(doc, "نسخه اولیه به صورت modular monolith با مرزهای روشن دامنه پیاده می‌شود. این انتخاب هزینه عملیاتی و پیچیدگی توزیع‌شده را کنترل می‌کند، در عین حال قراردادهای ماژول‌ها و مالکیت داده به گونه‌ای تعریف می‌شوند که در صورت رشد بار یا تیم، جداسازی سرویس‌های منتخب ممکن باشد.")
    add_table(doc, ["مسئولیت", "لایه"], [
        ["APIهای مشتری، پذیرنده و پنل‌های عملیاتی", "Services Application"],
        ["OAuth2 و OIDC، OTP، کاربر، نقش و مجوز", "UAA Application"],
        ["زمان‌بندی، اجرای دستی، retry و تاریخچه اجرا", "Jobs Application"],
        ["هویت، محصول، درخواست، استعلام، پرداخت، قرارداد، پذیرنده و تسویه", "Business Modules"],
        ["دیتابیس، HTTP Client، TLS، Cache، Observability و Web", "Infrastructure"],
        ["Routing چند provider و تنظیمات runtime بدون وابستگی به ماژول مصرف‌کننده", "Integration"],
    ], widths=[4.3, 2.0])
    add_heading(doc, "اصول یکپارچه‌سازی بیرونی", 2)
    add_bullets(doc, [
        "Adapter اختصاصی هر capability در ماژول مالک همان کسب و کار قرار می‌گیرد",
        "Routing عمومی، سلامت provider، احراز هویت و TLS در زیرساخت Integration باقی می‌ماند",
        "تغییر endpoint، credential، certificate و اولویت provider از تنظیمات امن runtime انجام می‌شود",
        "فراخوانی شبکه داخل transaction دیتابیس انجام نمی‌شود",
        "تمام عملیات مالی و استعلامی دارای idempotency key، correlation id و Audit قابل جست‌وجو هستند",
    ])

    add_page_break(doc)
    add_heading(doc, "۶ امنیت و الزامات غیرعملکردی", 1)
    add_heading(doc, "امنیت و کنترل دسترسی", 2)
    add_table(doc, ["کنترل پیشنهادی", "حوزه"], [
        ["OIDC و OAuth2، توکن کوتاه‌عمر، refresh token کنترل‌شده و ابطال نشست", "احراز هویت"],
        ["نقش به همراه مجوز جزئی و کنترل مالکیت مشتری یا محدوده پذیرنده", "مجوزدهی"],
        ["TLS برای همه ارتباطات و mTLS برای providerهایی که الزام دارند", "ارتباط امن"],
        ["Secret Manager یا Vault و ممنوعیت نگهداری credential در سورس", "مدیریت راز"],
        ["ثبت actor، زمان، عملیات، نتیجه و correlation id برای عملیات حساس", "Audit"],
        ["رمزنگاری داده حساس در انتقال و در صورت نیاز در سطح ستون یا دیسک", "حفاظت داده"],
        ["SAST، dependency scan، تست نفوذ و رفع موارد بحرانی پیش از Go Live", "امنیت نرم‌افزار"],
    ], widths=[4.6, 1.7], font_size=9.3)
    add_heading(doc, "اهداف غیرعملکردی", 2)
    add_table(doc, ["توضیح و شرط سنجش", "هدف"], [
        ["هدف SRS برای سرویس‌های داخل سامانه و بدون احتساب زمان provider بیرونی، پس از تثبیت سناریوی بار", "P95 کمتر از ۳۰۰ میلی‌ثانیه"],
        ["با معماری High Availability، مانیتورینگ و فرایند عملیاتی توافق‌شده", "دسترس‌پذیری ۹۹٫۹۵ درصد"],
        ["پشتیبانی از رشد تا ۵۰۰ هزار کاربر فعال و ۲۰۰ کاربر همزمان براساس الگوی بار مورد توافق", "ظرفیت"],
        ["پشتیبان‌گیری رمزگذاری‌شده، تست بازیابی و اهداف RPO و RTO مصوب کارفرما", "تداوم خدمت"],
        ["Metrics، Trace، Log ساخت‌یافته، dashboard و alert برای جریان‌های اصلی", "مشاهده‌پذیری"],
    ], widths=[4.7, 1.6], font_size=9.2)
    add_text(doc, "تحقق اعداد فوق وابسته به ظرفیت زیرساخت، کیفیت شبکه، رفتار providerها و داده واقعی آزمون است. معیار نهایی در سند Non Functional Requirements و برنامه آزمون کارایی تصویب می‌شود.", size=9.5, color=MID_GRAY)

    add_page_break(doc)
    add_heading(doc, "۷ روش اجرا و برنامه زمان‌بندی", 1)
    add_picture(doc, roadmap, Inches(6.4), "تصویر ۳  برنامه مبنای اجرای ۴۸ هفته‌ای با امکان هم‌پوشانی جریان‌ها")
    add_table(doc, ["خروجی اصلی", "مدت", "فاز"], [
        ["Scope baseline، نقشه فرایند، API inventory، NFR و برنامه انتشار", "۴ تا ۶ هفته", "شناخت و تثبیت محدوده"],
        ["UAA، امنیت، زیرساخت داده، Integration، CI/CD و Observability", "۸ تا ۱۰ هفته", "زیرساخت پلتفرم"],
        ["محصول و طرح، Origination، Inquiry، Payment، Guarantee و Contract", "۱۴ تا ۱۸ هفته", "هسته تسهیلات"],
        ["Merchant، Transaction، Reconciliation و Settlement", "۱۰ تا ۱۴ هفته", "پذیرنده و مالی"],
        ["کارایی، امنیت، UAT، مهاجرت، آموزش و Cutover", "۸ هفته", "آماده‌سازی تولید"],
        ["پایش نزدیک و رفع اشکال", "۳ ماه", "ضمانت پس از راه‌اندازی"],
    ], widths=[3.8, 1.2, 1.5], font_size=8.7)
    add_heading(doc, "چرخه تحویل", 2)
    add_text(doc, "توسعه در iterationهای دو هفته‌ای انجام می‌شود. در پایان هر iteration، قابلیت تکمیل‌شده در محیط آزمون نمایش داده می‌شود. تصمیم‌های دامنه، تغییر Scope و وابستگی‌های provider در backlog و صورت‌جلسه کنترل تغییر ثبت می‌شوند. نسخه‌های قابل انتشار پس از عبور از تست خودکار، بازبینی امنیت و تأیید Product Owner به UAT منتقل خواهند شد.")

    add_page_break(doc)
    add_heading(doc, "۸ تیم و حاکمیت پروژه", 1)
    add_heading(doc, "ترکیب تیم پیشنهادی", 2)
    add_table(doc, ["مسئولیت", "ظرفیت مبنا", "نقش"], [
        ["مالک معماری، تصمیم‌های فنی و بازبینی کیفیت", "۱ نفر", "معمار و Tech Lead"],
        ["مدل‌سازی فرایند، backlog و معیار پذیرش", "۱ نفر", "تحلیلگر کسب و کار یا Product"],
        ["ماژول‌های دامنه، API، داده و Integration", "۳ تا ۴ نفر", "توسعه Backend"],
        ["پرتال مشتری، پذیرنده و Backoffice", "۲ نفر", "توسعه Frontend"],
        ["برنامه آزمون، Automation و UAT support", "۱ تا ۲ نفر", "QA"],
        ["CI/CD، محیط‌ها، مانیتورینگ و امنیت استقرار", "نیم تا یک نفر", "DevOps و SRE"],
        ["هماهنگی، گزارش پیشرفت، ریسک و صورت‌جلسه", "نیم تا یک نفر", "مدیر پروژه"],
        ["بازبینی امنیت و تست نفوذ", "پاره وقت یا پیمانکار", "کارشناس امنیت"],
    ], widths=[3.6, 1.1, 1.7], font_size=9.2)
    add_heading(doc, "نقش‌های کارفرما", 2)
    add_bullets(doc, [
        "معرفی Product Owner دارای اختیار تصمیم‌گیری درباره فرایند و اولویت",
        "معرفی نمایندگان عملیات، مالی، امنیت، زیرساخت و حقوقی",
        "تأمین دسترسی به محیط، API، مستندات provider و داده آزمون",
        "بررسی و اعلام نظر درباره تحویل‌ها در بازه زمانی توافق‌شده",
        "هماهنگی مجوزها، قراردادهای سرویس‌دهندگان و تأییدهای سازمانی",
    ])
    add_heading(doc, "جلسات و گزارش‌ها", 2)
    add_table(doc, ["خروجی", "تناوب", "جلسه"], [
        ["برنامه iteration و تعهد دو هفته", "هر دو هفته", "Sprint Planning"],
        ["نمایش قابلیت و ثبت بازخورد", "هر دو هفته", "Demo"],
        ["وضعیت زمان هزینه ریسک و تصمیم", "هفتگی", "گزارش مدیریت پروژه"],
        ["تصمیم Scope و ریسک‌های سطح بالا", "ماهانه", "کمیته راهبری"],
        ["تأیید تغییر و اثر زمان و مبلغ", "حسب نیاز", "Change Control Board"],
    ], widths=[3.4, 1.2, 1.8], font_size=9.3)

    add_page_break(doc)
    add_heading(doc, "۹ تضمین کیفیت و پذیرش", 1)
    add_heading(doc, "لایه‌های آزمون", 2)
    add_bullets(doc, [
        "Unit Test برای قواعد دامنه، state machine و محاسبات مالی",
        "Integration Test برای دیتابیس، migration، callback و idempotency",
        "Contract Test برای APIهای provider و پاسخ‌های خطا",
        "End to End Test برای سفرهای مشتری، عملیات و پذیرنده",
        "Performance Test براساس workload مصوب و تفکیک زمان provider",
        "Security Test شامل SAST، dependency scan، تست دسترسی و تست نفوذ",
        "UAT با سناریو، داده و نتیجه مورد انتظار تأییدشده توسط کارفرما",
    ])
    add_heading(doc, "معیار پذیرش هر قابلیت", 2)
    add_table(doc, ["مدرک پذیرش", "معیار"], [
        ["Traceability بین نیازمندی و story", "نیازمندی مصوب"],
        ["گزارش pipeline و test suite", "موفقیت تست خودکار"],
        ["خروجی بررسی امنیت", "نبود آسیب‌پذیری Critical و High باز"],
        ["نسخه API و راهنمای عملیاتی", "مستندات"],
        ["صورت‌جلسه Demo یا UAT", "تأیید Product Owner"],
        ["ثبت release note و rollback plan", "آمادگی انتشار"],
    ], widths=[3.8, 2.4])
    add_heading(doc, "پذیرش نهایی", 2)
    add_text(doc, "پس از استقرار Production، یک دوره پایش اولیه برای اجرای سناریوهای حیاتی برگزار می‌شود. کارفرما حداکثر ده روز کاری برای اعلام مغایرت مستند با معیار پذیرش فرصت دارد. عدم اعلام مغایرت در این بازه به معنی پذیرش همان تحویل است؛ ایرادهای دوره ضمانت طبق اولویت رفع خواهند شد.")

    add_page_break(doc)
    add_heading(doc, "۱۰ استقرار و بهره‌برداری", 1)
    add_heading(doc, "محیط‌ها", 2)
    add_table(doc, ["کاربرد", "محیط"], [
        ["توسعه و تست روزانه تیم", "Development"],
        ["تست یکپارچه و تست خودکار", "Test"],
        ["آزمون پذیرش با داده کنترل‌شده", "UAT"],
        ["خدمت واقعی با کنترل تغییر", "Production"],
        ["در صورت الزام HA و DR کارفرما", "Disaster Recovery"],
    ], widths=[4.6, 1.7])
    add_heading(doc, "تحویل عملیاتی", 2)
    add_bullets(doc, [
        "Pipeline ساخت و استقرار و سیاست approval برای Production",
        "تنظیمات محیطی و مدیریت Secret بدون وابستگی به نسخه نرم‌افزار",
        "Migration دیتابیس با امکان پایش و Rollback برنامه‌ریزی‌شده",
        "Dashboardهای سلامت سرویس، دیتابیس، queue، job و provider",
        "Runbook رخداد، بازیابی، failover provider و عملیات دستی مجاز",
        "آموزش تیم عملیات، مالی، پشتیبانی و مدیر سامانه",
    ])
    add_heading(doc, "مهاجرت داده", 2)
    add_text(doc, "دامنه و کیفیت داده سامانه فعلی باید در فاز شناخت ارزیابی شود. پیشنهاد شامل طراحی mapping، اجرای dry run، گزارش مغایرت و یک اجرای نهایی است. پاک‌سازی گسترده داده، اصلاح سوابق ناقص و مهاجرت از منابعی که ساختار آن‌ها در زمان پیشنهاد مشخص نیست، نیازمند برآورد جداگانه خواهد بود.")

    add_page_break(doc)
    add_heading(doc, "۱۱ پشتیبانی و سطح خدمت", 1)
    add_heading(doc, "دوره ضمانت", 2)
    add_text(doc, "سه ماه رفع اشکال پس از راه‌اندازی Production در مبلغ پیاده‌سازی منظور شده است. ضمانت شامل رفع مغایرت با نیازمندی و معیار پذیرش مصوب است و توسعه قابلیت جدید، تغییر مقررات، تغییر API provider یا افزایش ظرفیت زیرساخت را شامل نمی‌شود.")
    add_heading(doc, "بسته‌های پشتیبانی پس از ضمانت", 2)
    add_table(doc, ["هزینه ماهانه", "پوشش", "بسته"], [
        ["۵۰۰ میلیون تومان", "روزهای کاری ۸ تا ۱۷، مانیتورینگ و رسیدگی به رخداد", "استاندارد ۸ در ۵"],
        ["۸۵۰ میلیون تومان", "On Call شبانه‌روزی برای رخداد بحرانی و پوشش عملیاتی", "حیاتی ۲۴ در ۷"],
        ["برآورد جداگانه", "تیم ظرفیت رزروشده برای roadmap و بهبود مستمر", "توسعه مستمر"],
    ], widths=[1.5, 3.3, 1.5], font_size=9.2)
    add_heading(doc, "اهداف پاسخ‌گویی بسته استاندارد", 2)
    add_table(doc, ["هدف رفع یا راهکار موقت", "زمان پاسخ", "تعریف", "اولویت"], [
        ["۴ ساعت کاری", "۳۰ دقیقه", "توقف سرویس یا ریسک مالی فعال", "P1"],
        ["۱ روز کاری", "۲ ساعت کاری", "اختلال جدی بدون توقف کامل", "P2"],
        ["۳ روز کاری", "۱ روز کاری", "اختلال محدود با راهکار جایگزین", "P3"],
        ["نسخه برنامه‌ریزی‌شده", "۲ روز کاری", "درخواست کم‌اثر یا بهبود", "P4"],
    ], widths=[1.5, 1.2, 2.7, 0.8], font_size=8.9)
    add_text(doc, "SLA نهایی باید ساعات خدمت، پنجره نگهداری، مسئولیت زیرساخت و providerهای ثالث، نحوه اندازه‌گیری و service credit را مشخص کند.", size=9.5, color=MID_GRAY)
    add_heading(doc, "پشتیبانی در مدل مشارکت در کارمزد", 2)
    add_text(doc, "در صورت انتخاب مدل مشارکتی بخش ۱۲، پشتیبانی استاندارد ۸ در ۵ در کل دوره مشارکت جزو تعهد پیمانکار است و هزینه جداگانه‌ای ندارد. کارفرما در هر ماه مکلف است مبلغ بیشتر از میان سهم کارمزدی پیمانکار و حداقل پرداخت تضمین‌شده را بپردازد؛ بنابراین نبود فروش، کاهش تراکنش یا عدم وصول کارمزد از مشتریان، تعهد پرداخت حداقل تضمین‌شده را ساقط یا معلق نمی‌کند. ارتقای خدمت به پوشش حیاتی ۲۴ در ۷ با افزایش کف تضمین‌شده یا پرداخت مابه‌التفاوت بسته حیاتی انجام می‌شود.")

    add_heading(doc, "۱۲ پیشنهاد مالی", 1)
    add_heading(doc, "مدل اول پرداخت ثابت", 2)
    add_table(doc, ["مبلغ تومان", "شرح"], [
        ["۲۶٬۸۰۰٬۰۰۰٬۰۰۰", "قیمت پایه طراحی توسعه استقرار و انتقال دانش"],
        ["۲٬۳۰۰٬۰۰۰٬۰۰۰-", "تخفیف آغاز همکاری"],
        ["۲۴٬۵۰۰٬۰۰۰٬۰۰۰", "مبلغ خالص پیشنهادی اجرای نسخه عملیاتی"],
    ], widths=[2.2, 4.1], font_size=10.0)
    add_text(doc, "مالیات و عوارض قانونی به مبلغ فوق افزوده می‌شود. هزینه زیرساخت، تجهیزات، لایسنس، پیامک، eKYC، امضای دیجیتال، سرویس‌های استعلامی، HSM و تست نفوذ شرکت ثالث در مبلغ فوق منظور نشده است.", size=9.6, color=MID_GRAY)
    add_heading(doc, "برنامه پرداخت", 2)
    add_table(doc, ["مبلغ تومان", "درصد", "رویداد پرداخت"], [
        ["۴٬۹۰۰٬۰۰۰٬۰۰۰", "۲۰ درصد", "امضای قرارداد و ابلاغ شروع"],
        ["۲٬۴۵۰٬۰۰۰٬۰۰۰", "۱۰ درصد", "تأیید Scope baseline و معماری تفصیلی"],
        ["۴٬۹۰۰٬۰۰۰٬۰۰۰", "۲۰ درصد", "تحویل زیرساخت پلتفرم و هویت"],
        ["۶٬۱۲۵٬۰۰۰٬۰۰۰", "۲۵ درصد", "تحویل هسته درخواست تسهیلات و Integration"],
        ["۳٬۶۷۵٬۰۰۰٬۰۰۰", "۱۵ درصد", "تحویل Merchant Transaction Settlement"],
        ["۱٬۲۲۵٬۰۰۰٬۰۰۰", "۵ درصد", "استقرار Production و شروع پایش"],
        ["۱٬۲۲۵٬۰۰۰٬۰۰۰", "۵ درصد", "پذیرش نهایی و انتقال دانش"],
    ], widths=[1.8, 1.0, 3.5], font_size=9.2)
    add_heading(doc, "مدل دوم مشارکت در کارمزد با حداقل تضمین‌شده", 2)
    add_text(doc, "این مدل با هدف هم‌راستا کردن منافع طرفین و تأمین پایدار هزینه نگهداری پیشنهاد می‌شود. ارقام زیر پیشنهاد اولیه تجاری است و پس از دریافت پیش‌بینی حجم فروش، نرخ کارمزد و سناریوی رشد، در قرارداد نهایی تثبیت خواهد شد.")
    add_table(doc, ["شرط پیشنهادی", "موضوع"], [
        ["۱۲٬۲۵۰٬۰۰۰٬۰۰۰ تومان معادل ۵۰ درصد مبلغ خالص اجرا که متناسب با milestoneهای برنامه پرداخت مدل ثابت پرداخت می‌شود", "پرداخت اجرای اولیه"],
        ["۱۵ درصد از خالص کارمزد قابل تقسیم که در هر ماه واقعاً توسط کارفرما یا شرکت‌های وابسته و عاملان وصول دریافت شده است", "سهم کارمزدی پیمانکار"],
        ["۸۵۰ میلیون تومان در ماه؛ کارفرما مکلف است مبلغ بیشتر از میان سهم کارمزدی محاسبه‌شده و این کف را بپردازد", "حداقل پرداخت تضمین‌شده"],
        ["۳۶ ماه از نخستین ماه کامل پس از راه‌اندازی Production", "دوره مشارکت"],
        ["پشتیبانی استاندارد ۸ در ۵، مانیتورینگ و رسیدگی به رخداد در دوره مشارکت بدون صورتحساب ماهانه جداگانه", "خدمت مشمول مشارکت"],
        ["تسویه ماهانه حداکثر تا دهمین روز کاری ماه بعد همراه با گزارش فروش، کارمزد، کسورات، برگشت‌ها و مغایرت‌ها", "تسویه و گزارش‌دهی"],
    ], widths=[4.4, 1.9], font_size=8.8)
    add_text(doc, "خالص کارمزد قابل تقسیم عبارت است از کارمزد خدمات و تراکنش‌های موفق سامانه که وصول قطعی شده، پس از کسر مالیات و عوارض، برگشت وجه، chargeback، سهم بانک یا PSP، هزینه providerهای ثالث و کسورات الزامی قانونی. اصل تسهیلات، سود و وجه تسویه پذیرنده جزو مبنای کارمزد نیست. حداقل تضمین‌شده مستقل از میزان فروش و وصول کارمزد است و در ماه بدون فروش نیز به طور کامل پرداخت می‌شود.", size=9.4)
    add_text(doc, "کارفرما باید دسترسی برخط یا گزارش قابل حسابرسی از تراکنش‌ها، کارمزدها و اسناد تسویه را در اختیار پیمانکار بگذارد. در صورت تأخیر در ارائه گزارش، مبلغ حداقل تضمین‌شده در سررسید پرداخت می‌شود و تعدیل سهم واقعی پس از دریافت گزارش در صورتحساب بعدی انجام خواهد شد. مدل ثابت و مدل مشارکتی جایگزین یکدیگرند و انتخاب هر مدل باید صریحاً در قرارداد درج شود.", size=9.4, color=MID_GRAY)
    add_heading(doc, "اعتبار و تعدیل", 2)
    add_bullets(doc, [
        "اعتبار این پیشنهاد ۳۰ روز تقویمی از تاریخ ارائه است",
        "تأخیر کارفرما بیش از ۳۰ روز در دسترسی، تصمیم یا پرداخت موجب بازبرنامه‌ریزی می‌شود",
        "عبور پروژه از سال مالی یا تأخیر خارج از کنترل پیمانکار، مشمول تعدیل مورد توافق خواهد بود",
        "هر تغییر خارج از Scope baseline با Change Request و برآورد زمان و مبلغ اجرا می‌شود",
    ])

    add_page_break(doc)
    add_heading(doc, "۱۳ مفروضات و موارد خارج از محدوده", 1)
    add_heading(doc, "مفروضات اصلی", 2)
    add_bullets(doc, [
        "کارفرما قرارداد و دسترسی Sandbox یا Test همه providerهای لازم را فراهم می‌کند",
        "APIهای بانک، eKYC، امضای دیجیتال و پرداخت دارای مستندات و پشتیبانی فنی قابل دسترس هستند",
        "تصمیم‌های Product Owner حداکثر ظرف سه روز کاری اعلام می‌شود",
        "زیرساخت اجرا، شبکه، دامنه، گواهی و دسترسی‌های امنیتی طبق برنامه تأمین می‌شود",
        "محتوا و متن حقوقی قراردادها و رضایت‌نامه‌ها توسط کارفرما تأیید می‌شود",
        "یک زبان رابط کاربری و یک واحد پول اصلی در نسخه مبنا پشتیبانی می‌شود",
    ])
    add_heading(doc, "تعهدات کارفرما برای محیط و زیرساخت", 2)
    add_bullets(doc, [
        "کارفرما باید محیط‌های Development، Test، UAT و Production و در صورت توافق محیط Pre Production و Disaster Recovery را مطابق برنامه پروژه ایجاد و در اختیار تیم پیمانکار قرار دهد.",
        "سرورها یا منابع پردازشی، فضای ذخیره‌سازی، پایگاه داده، cache، queue، فضای فایل و object storage و زیرساخت پشتیبان‌گیری مورد نیاز هر محیط توسط کارفرما تأمین می‌شود و باید با ظرفیت و معماری مصوب سازگار باشد.",
        "دسترسی امن و پایدار اعضای معرفی‌شده پیمانکار به محیط‌ها، مخازن کد و artifact، pipeline، لاگ‌ها، مانیتورینگ و ابزارهای عیب‌یابی از طریق VPN، bastion یا سازوکار امنیتی مورد توافق فراهم می‌شود.",
        "شبکه، IP و allowlist، DNS، دامنه، گواهی TLS، firewall، WAF، load balancer، Secret، کلیدها و مجوزهای اتصال به بانک و providerهای ثالث توسط کارفرما یا با همکاری واحدهای ذی‌ربط آن تأمین و تمدید می‌شود.",
        "کارفرما مسئول تأمین قرارداد، حساب کاربری، اعتبار مصرفی و دسترسی Sandbox، Test و Production سرویس‌های بیرونی و معرفی مسئول فنی پاسخ‌گو برای هر provider است.",
        "داده آزمون مجاز، سناریوهای UAT و نمایندگان کسب و کار، عملیات، مالی، امنیت و زیرساخت در زمان‌های مورد نیاز پروژه توسط کارفرما فراهم می‌شوند.",
        "تأخیر یا محدودیت در تأمین هر یک از محیط‌ها، سرورها، دسترسی‌ها، داده‌ها یا سرویس‌های فوق که مانع کار تیم شود، تأخیر مجاز محسوب و موجب بازبرنامه‌ریزی زمان و در صورت تحمیل هزینه، صدور Change Request خواهد شد.",
        "پیمانکار مسئول نصب، پیکربندی و استقرار اقلام داخل محدوده روی زیرساخت تحویلی است؛ مالکیت، هزینه و عملیات پایه زیرساخت و سرویس‌های ثالث بر عهده کارفرما باقی می‌ماند مگر آنکه در پیوست جداگانه خلاف آن توافق شود.",
    ])
    add_heading(doc, "موارد خارج از مبلغ پایه", 2)
    add_bullets(doc, [
        "هزینه مصرف یا راه‌اندازی سرویس‌های بیرونی و کارمزد تراکنش",
        "خرید سرور، تجهیزات شبکه، HSM، WAF و لایسنس محصولات تجاری",
        "تولید اپلیکیشن Native مستقل Android و iOS مگر آنکه در Scope نهایی افزوده شود",
        "پیاده‌سازی کامل کیف پول، BNPL، جت کارت، Loyalty، CRM و وصول مطالبات",
        "اخذ مجوز، تأییدیه حقوقی یا امنیتی از نهادهای بیرونی",
        "مهاجرت یا پاک‌سازی داده خارج از mapping تأییدشده",
        "تغییرات ناشی از مقررات جدید پس از تثبیت Scope",
    ])

    add_page_break(doc)
    add_heading(doc, "۱۴ ریسک‌ها و کنترل تغییرات", 1)
    add_table(doc, ["اقدام کنترلی", "اثر", "ریسک"], [
        ["API inventory در فاز شناخت، mock و contract test", "تأخیر و بازکاری", "نامشخص بودن API provider"],
        ["تعریف substitute provider و circuit breaker", "اختلال سفر مشتری", "عدم پایداری سرویس بیرونی"],
        ["کمیته تغییر و برآورد پیش از اجرا", "افزایش زمان و هزینه", "رشد Scope"],
        ["تحلیل داده، dry run و reconciliation", "خطای مالی یا عملیاتی", "کیفیت پایین داده مهاجرت"],
        ["Security gate و تست نفوذ پیش از Go Live", "رخداد امنیتی", "ضعف تنظیمات یا کد"],
        ["Idempotency، Ledger trail و کنترل چهارچشمی", "زیان مالی", "تکرار تراکنش یا تسویه"],
        ["ظرفیت‌سنجی و تست بار تدریجی", "افت کارایی", "برآورد نادرست بار"],
        ["تصمیم مکتوب Product Owner و SLA پاسخ", "توقف تیم", "تأخیر تصمیم کارفرما"],
    ], widths=[3.2, 1.4, 1.8], font_size=8.8)
    add_heading(doc, "فرایند تغییر", 2)
    add_numbered(doc, [
        "ثبت درخواست تغییر و دلیل کسب و کاری",
        "تحلیل اثر روی معماری، امنیت، زمان، هزینه و پذیرش",
        "ارائه پیشنهاد تغییر و اولویت اجرا",
        "تأیید کتبی نمایندگان مجاز طرفین",
        "به‌روزرسانی backlog، برنامه انتشار و مبنای مالی",
    ])

    add_page_break(doc)
    add_heading(doc, "۱۵ شرایط قراردادی پیشنهادی", 1)
    add_bullets(doc, [
        "مالکیت سورس اختصاصی پروژه پس از تسویه کامل مطابق قرارداد به کارفرما منتقل می‌شود. اجزای عمومی و ابزارهای از پیش موجود پیمانکار با مجوز استفاده دائمی در اختیار پروژه قرار می‌گیرند.",
        "طرفین تعهد محرمانگی و ضوابط دسترسی به داده‌های واقعی را پیش از دسترسی تیم امضا می‌کنند.",
        "تعهدات امنیتی، محل نگهداری داده، سیاست لاگ و نحوه استفاده از داده Production در پیوست امنیت تعیین می‌شود.",
        "مسئولیت عملکرد سرویس‌های ثالث، شبکه و زیرساختی که تحت کنترل پیمانکار نیستند از SLA پیمانکار تفکیک خواهد شد.",
        "سقف مسئولیت، موارد استثنا، خسارت مستقیم و نحوه رسیدگی به رخداد مالی باید صریحاً در قرارداد تعیین شود.",
        "فسخ، تعلیق، تأخیر پرداخت، تحویل سورس و انتقال دانش براساس milestone تکمیل‌شده تسویه می‌شود.",
        "رزومه افراد کلیدی، فهرست پیمانکاران فرعی احتمالی و سطح دسترسی هر نقش پیش از شروع تأیید می‌شود.",
        "در مدل مشارکت در کارمزد، تعهد کارفرما به پرداخت حداقل تضمین‌شده تعهد مستقل قراردادی است و به میزان فروش، عملکرد بازاریابی، تعداد تراکنش یا وصول کارمزد از اشخاص ثالث مشروط نیست.",
        "کارفرما حق دسترسی پیمانکار به گزارش‌ها و اسناد لازم برای محاسبه و حسابرسی سهم کارمزدی را فراهم می‌کند و سازوکار رسیدگی به مغایرت، مهلت تسویه و ضمانت اجرای تأخیر در قرارداد درج می‌شود.",
    ])
    add_text(doc, "این بخش جایگزین بررسی حقوقی قرارداد نیست. متن نهایی باید توسط مشاوران حقوقی و امنیتی طرفین بازبینی شود.", size=9.5, color=MID_GRAY)

    add_page_break(doc)
    add_heading(doc, "۱۶ گام‌های شروع پروژه", 1)
    add_numbered(doc, [
        "تکمیل نام و اطلاعات ثبتی طرفین و تعیین نمایندگان مجاز",
        "امضای NDA و تبادل اسناد فنی و قراردادهای providerها",
        "برگزاری کارگاه Scope با نمایندگان کسب و کار، عملیات، مالی، امنیت و فناوری",
        "تأیید Scope baseline، اولویت انتشار و معیارهای پذیرش",
        "تأمین محیط‌ها، دسترسی‌ها و داده آزمون",
        "امضای قرارداد اجرایی و واریز پیش‌پرداخت",
        "آغاز iteration اول و ارائه گزارش هفتگی",
    ])
    add_heading(doc, "اقلام مورد نیاز برای نهایی‌سازی", 2)
    add_table(doc, ["مالک تهیه", "وضعیت", "قلم"], [
        ["کارفرما", "نیازمند تکمیل", "نام رسمی و اطلاعات ثبتی کارفرما"],
        ["پیشنهاددهنده", "نیازمند تکمیل", "نام رسمی شرکت پیشنهاددهنده و صاحبان امضا"],
        ["کارفرما", "نیازمند دریافت", "فهرست providerها و قراردادهای فعال"],
        ["مشترک", "فاز شناخت", "Scope نسخه اول و اولویت roadmap"],
        ["مشترک", "فاز شناخت", "معیارهای NFR و SLA نهایی"],
        ["پیشنهاددهنده", "پیش از قرارداد", "رزومه اعضای کلیدی و برنامه تخصیص"],
    ], widths=[1.4, 1.5, 3.5], font_size=9.2)

    add_page_break(doc)
    add_heading(doc, "پیوست الف فهرست تحویل‌دادنی‌ها", 1)
    deliverables = [
        ["سند Scope baseline و Requirements Traceability Matrix", "تحلیل"],
        ["مدل دامنه، Context Map و Architecture Decision Records", "معماری"],
        ["سورس‌کد، migrationها، تست‌ها و pipelineها", "نرم‌افزار"],
        ["OpenAPI تفکیک‌شده برای دامنه‌ها و نمونه فراخوانی", "API"],
        ["تنظیمات runtime، راهنمای Secret و certificate", "Integration"],
        ["Test plan، گزارش تست و فهرست ایرادهای رفع‌شده", "کیفیت"],
        ["Deployment guide، rollback plan و runbook", "عملیات"],
        ["Dashboard و alertهای توافق‌شده", "مانیتورینگ"],
        ["راهنمای کاربر پنل و محتوای آموزش", "آموزش"],
        ["Release note، صورت‌جلسه UAT و تحویل نهایی", "پذیرش"],
    ]
    add_table(doc, ["شرح", "گروه"], deliverables, widths=[5.0, 1.3])
    add_heading(doc, "پیوست ب فرهنگ واژگان", 1)
    add_table(doc, ["تعریف در این پیشنهاد", "واژه"], [
        ["خانواده تجاری محصولات اعتباری", "Product"],
        ["پیشنهاد قابل انتخاب مشتری با قواعد و هزینه مشخص", "Plan"],
        ["قاعده احراز طرح مانند سن یا حداقل رتبه اعتباری", "Control"],
        ["دریافت Fact از سرویس بیرونی بدون تصمیم‌گیری درباره طرح", "Inquiry"],
        ["چرخه درخواست مشتری از انتخاب طرح تا تخصیص اعتبار", "Origination"],
        ["ثبت الزام پرداخت یک هزینه مشخص", "Fee Obligation"],
        ["اختصاص اعتبار تأییدشده در سرویس مالی بیرونی", "Facility Allocation"],
        ["مقایسه و تطبیق سوابق تراکنش داخلی و provider", "Reconciliation"],
        ["محاسبه و پرداخت سهم پذیرنده در یک دوره", "Settlement"],
        ["هدف نقطه بازیابی داده", "RPO"],
        ["هدف زمان بازیابی خدمت", "RTO"],
    ], widths=[4.8, 1.5], font_size=9.2)

    add_page_break(doc)
    add_heading(doc, "منابع", 1)
    add_bullets(doc, [
        "راهنمای مراحل درخواست تسهیلات در سامانه جت وام به عنوان وضعیت جاری",
        "ویدئوی سفر مشتری جت وام مرداد ۱۴۰۵ به عنوان وضعیت جاری",
        "سند نیازمندی‌های سامانه جت وام خرداد ۱۴۰۵ به عنوان نقشه راه",
        "مستندات معماری و Context Map کدبیس جت وام",
        "تعرفه پایه خدمات فنی تخصصی انفورماتیک سال ۱۴۰۴ برای مبنای روش قیمت‌گذاری و پشتیبانی",
        "گزارش‌های حقوق و دستمزد ۱۴۰۵ برای کنترل معقول بودن هزینه تیم",
    ])
    add_text(doc, "پایان سند", bold=True, size=12, align=WD_ALIGN_PARAGRAPH.CENTER, before=30)

    doc.core_properties.title = "پروپوزال طراحی توسعه استقرار و پشتیبانی سامانه جامع جت وام"
    doc.core_properties.subject = "پیشنهاد فنی و تجاری سامانه جت وام"
    doc.core_properties.author = "[نام شرکت پیشنهاددهنده]"
    doc.core_properties.keywords = "Jetvam, proposal, digital lending, merchant settlement"
    doc.core_properties.comments = "نسخه قابل ویرایش برای تکمیل اطلاعات طرفین"
    doc.save(OUTPUT)
    print(OUTPUT)


if __name__ == "__main__":
    build_document()
