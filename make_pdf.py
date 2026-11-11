import os
import re
from pathlib import Path
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, Preformatted
from reportlab.lib import colors

project_dir = Path(__file__).resolve().parent
pdf_path = project_dir / 'IntelliStock_AI_Thesis_Report.pdf'
doc = SimpleDocTemplate(
    pdf_path,
    pagesize=letter,
    leftMargin=54,
    rightMargin=54,
    topMargin=54,
    bottomMargin=54
)

styles = getSampleStyleSheet()

title_style = ParagraphStyle(
    'DocTitle',
    parent=styles['Heading1'],
    fontName='Times-Bold',
    fontSize=18,
    leading=24,
    alignment=1,
    spaceAfter=15
)

subtitle_style = ParagraphStyle(
    'DocSubtitle',
    parent=styles['Normal'],
    fontName='Times-Roman',
    fontSize=13,
    leading=18,
    alignment=1,
    spaceAfter=15
)

author_style = ParagraphStyle(
    'AuthorStyle',
    parent=styles['Normal'],
    fontName='Times-Bold',
    fontSize=12,
    leading=16,
    alignment=1
)

center_roman = ParagraphStyle(
    'CenterRoman',
    parent=styles['Normal'],
    fontName='Times-Roman',
    fontSize=11,
    leading=15,
    alignment=1
)

h1_style = ParagraphStyle(
    'ChapH1',
    parent=styles['Heading1'],
    fontName='Times-Bold',
    fontSize=15,
    leading=20,
    alignment=1,
    spaceBefore=15,
    spaceAfter=15,
    keepWithNext=True
)

h2_style = ParagraphStyle(
    'ChapH2',
    parent=styles['Heading2'],
    fontName='Times-Bold',
    fontSize=12,
    leading=16,
    alignment=0,
    spaceBefore=12,
    spaceAfter=6,
    keepWithNext=True
)

h3_style = ParagraphStyle(
    'ChapH3',
    parent=styles['Heading3'],
    fontName='Times-Bold',
    fontSize=11,
    leading=15,
    alignment=0,
    spaceBefore=8,
    spaceAfter=4,
    keepWithNext=True
)

body_style = ParagraphStyle(
    'BodyJustified',
    parent=styles['Normal'],
    fontName='Times-Roman',
    fontSize=11,
    leading=16,
    alignment=4,
    spaceAfter=8
)

code_style = ParagraphStyle(
    'CodeStyle',
    parent=styles['Normal'],
    fontName='Courier',
    fontSize=7.5,
    leading=9.5,
    spaceAfter=6
)

story = []

# --- 1. TITLE PAGE (Exact SRM Format) ---
story.append(Spacer(1, 35))
story.append(Paragraph('<b>INTELLISTOCK — INVENTORY &amp; STOCK MANAGEMENT SYSTEM</b>', title_style))
story.append(Spacer(1, 20))
story.append(Paragraph('A PROJECT REPORT', subtitle_style))
story.append(Spacer(1, 20))
story.append(Paragraph('<i>Submitted by</i>', center_roman))
story.append(Spacer(1, 10))
story.append(Paragraph('<b>UDAY SURYA TEJ DANDA</b>', author_style))
story.append(Spacer(1, 25))
story.append(Paragraph('<i>Under the guidance of</i>', center_roman))
story.append(Spacer(1, 5))
story.append(Paragraph('<b>Dr. S. Ranjeet, Ph.D</b><br/>(Professor, Department of Computer Science &amp; Engineering)', author_style))
story.append(Spacer(1, 25))
story.append(Paragraph('<i>in partial fulfillment for the award of the degree of</i>', center_roman))
story.append(Spacer(1, 5))
story.append(Paragraph('<b>BACHELOR OF TECHNOLOGY</b><br/>in<br/><b>COMPUTER SCIENCE &amp; ENGINEERING</b>', author_style))
story.append(Spacer(1, 30))
story.append(Paragraph('of<br/><br/><b>FACULTY OF ENGINEERING AND TECHNOLOGY</b><br/><br/><b>S.R.M INSTITUTE OF SCIENCE AND TECHNOLOGY</b><br/>(Under Section 3 of UGC Act, 1956)<br/>S.R.M. Nagar, Kattankulathur, Kancheepuram District<br/><br/><b>NOVEMBER 2026</b>', center_roman))
story.append(PageBreak())

# --- 2. BONAFIDE CERTIFICATE (Exact SRM Format) ---
story.append(Spacer(1, 15))
story.append(Paragraph('<b>S.R.M INSTITUTE OF SCIENCE AND TECHNOLOGY</b><br/>(Under Section 3 of UGC Act, 1956)', center_roman))
story.append(Spacer(1, 25))
story.append(Paragraph('<b>BONAFIDE CERTIFICATE</b>', title_style))
story.append(Spacer(1, 15))
cert_text = (
    'Certified that this project report titled <b>&ldquo;INTELLISTOCK — INVENTORY '
    '&amp; STOCK MANAGEMENT SYSTEM&rdquo;</b> is the bonafide work of <b>UDAY SURYA TEJ DANDA</b>, '
    'who carried out the project work under my supervision. Certified further, that to the best of my knowledge the work '
    'reported herein does not form any other project report or dissertation on the basis of which a degree or award was '
    'conferred on an earlier occasion on this or any other candidate.'
)
story.append(Paragraph(cert_text, body_style))
story.append(Spacer(1, 70))

cert_table_data = [
    [
        Paragraph('<b>SIGNATURE</b><br/><br/><br/><b>Dr. S. Ranjeet, Ph.D</b><br/><b>GUIDE</b><br/>Professor<br/>Dept. of Computer Science &amp; Engineering', center_roman),
        Paragraph('<b>SIGNATURE</b><br/><br/><br/><b>Dr. R. Balaji Ganesh, Ph.D</b><br/><b>HEAD OF THE DEPARTMENT</b><br/>Dept. of Computer Science &amp; Engineering', center_roman)
    ],
    [
        Paragraph('<br/><br/><br/>Signature of the Internal Examiner', center_roman),
        Paragraph('<br/><br/><br/>Signature of the External Examiner', center_roman)
    ]
]
cert_table = Table(cert_table_data, colWidths=[250, 250])
cert_table.setStyle(TableStyle([
    ('VALIGN', (0,0), (-1,-1), 'TOP'),
    ('ALIGN', (0,0), (-1,-1), 'CENTER'),
]))
story.append(cert_table)
story.append(PageBreak())

# --- 3. ABSTRACT ---
story.append(Spacer(1, 20))
story.append(Paragraph('<b>ABSTRACT</b>', title_style))
story.append(Spacer(1, 15))
story.append(Paragraph(
    'Inventory management is a critical business function for warehouses, wholesalers, and retail distributors. '
    'Manual record-keeping or disconnected spreadsheet tracking frequently causes stock discrepancies, undetected shortages, '
    'delayed procurement, and inventory depreciation through expiration. These challenges necessitate a reliable, automated, '
    'and audit-traceable desktop management system.', body_style))
story.append(Spacer(1, 8))
story.append(Paragraph(
    '<b>IntelliStock</b> is an enterprise-grade desktop inventory and stock management system developed in Java 21, '
    'leveraging Java Swing for a responsive GUI and SQLite via pure Java Database Connectivity (JDBC) for ACID-compliant '
    'relational persistence. The application strictly adheres to a 3-tier layered architecture separating presentation, '
    'business service logic, and repository access layers.', body_style))
story.append(Spacer(1, 8))
story.append(Paragraph(
    'Key capabilities include atomic product registration with unique SKU validation, real-time inventory catalog viewing '
    'with dynamic status indicators (NORMAL, LOW, CRITICAL, OUT_OF_STOCK, ARCHIVED), transaction-safe stock movements '
    '(receipts, dispatches, damages, returns, adjustments) enforcing non-negative constraints (CHECK quantity &gt;= 0), and an '
    'automated rule-based Smart Stock Alert Engine. In addition, transparent moving-average demand analytics provide inventory '
    'velocity classification and automated reorder suggestions. The system also supports end-to-end procurement with partial '
    'goods receipt, PBKDF2 with HMAC-SHA256 password security, zero-downtime WAL online backups, and single-click UTF-8 BOM CSV exports. '
    'Comprehensive automated verification via JUnit 5 demonstrates 100% test pass rate across 20 unit and end-to-end integration test cases.', body_style))
story.append(PageBreak())

# --- 4. ACKNOWLEDGEMENTS ---
story.append(Spacer(1, 20))
story.append(Paragraph('<b>ACKNOWLEDGEMENTS</b>', title_style))
story.append(Spacer(1, 15))
ack_text = (
    'I would like to express my deepest gratitude to my project guide, <b>Dr. S. Ranjeet</b>, Professor, Department of '
    'Computer Science &amp; Engineering, for his valuable guidance, consistent encouragement, timely help, and for '
    'providing an excellent atmosphere for conducting this project work. All through the project, in spite of his busy schedule, '
    'he has extended cheerful and cordial support towards completing this work.<br/><br/>'
    'I also extend my sincere thanks to <b>Dr. R. Balaji Ganesh</b>, Head of the Department of Computer Science &amp; Engineering, '
    'and the faculty of <b>S.R.M Institute of Science and Technology</b> for their continuous encouragement and institutional support.<br/><br/>'
    'Lastly, I thank my family and peers for their unwavering cooperation and moral support throughout the duration of this project.'
)
story.append(Paragraph(ack_text, body_style))
story.append(Spacer(1, 40))
story.append(Paragraph('<b>UDAY SURYA TEJ DANDA</b>', ParagraphStyle('AuthorRight', parent=styles['Normal'], fontName='Times-Bold', alignment=2)))
story.append(PageBreak())

# Read markdown chapters
with open(project_dir / 'docs' / 'IntelliStock_AI_Thesis.md', 'r', encoding='utf-8') as f:
    lines = f.readlines()

in_content = False
current_code = []
in_code = False
in_table = False
table_lines = []

def flush_table(tbl_lines):
    if not tbl_lines:
        return None
    rows = []
    for l in tbl_lines:
        l = l.strip()
        if not l.startswith('|'):
            continue
        parts = [p.strip() for p in l.split('|')[1:-1]]
        if all(set(p) <= set(':- ') for p in parts if p):
            continue
        row_cells = []
        for p in parts:
            p_clean = re.sub(r'\*\*(.*?)\*\*', r'<b>\1</b>', p)
            p_clean = re.sub(r'`(.*?)`', r'\1', p_clean)
            p_clean = p_clean.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
            p_clean = p_clean.replace('&lt;b&gt;', '<b>').replace('&lt;/b&gt;', '</b>')
            row_cells.append(Paragraph(p_clean, ParagraphStyle('TCell', parent=styles['Normal'], fontName='Times-Roman', fontSize=8, leading=10)))
        rows.append(row_cells)
    if not rows:
        return None
    col_count = len(rows[0])
    avail_width = 504
    w = avail_width / col_count
    t = Table(rows, colWidths=[w]*col_count)
    t.setStyle(TableStyle([
        ('BOX', (0,0), (-1,-1), 0.5, colors.grey),
        ('INNERGRID', (0,0), (-1,-1), 0.5, colors.lightgrey),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
        ('BACKGROUND', (0,0), (-1,0), colors.whitesmoke),
        ('BOTTOMPADDING', (0,0), (-1,-1), 3),
        ('TOPPADDING', (0,0), (-1,-1), 3),
    ]))
    return t

for raw_line in lines:
    line = raw_line.strip()
    
    if '# CHAPTER 1' in raw_line:
        in_content = True
        
    if not in_content:
        continue
        
    if line.startswith('```'):
        if in_code:
            in_code = False
            code_str = '\n'.join(current_code)
            current_code = []
            story.append(Preformatted(code_str, code_style))
            story.append(Spacer(1, 4))
        else:
            in_code = True
            current_code = []
        continue
        
    if in_code:
        current_code.append(raw_line.rstrip())
        continue
        
    if line.startswith('|'):
        if not in_table:
            in_table = True
            table_lines = []
        table_lines.append(line)
        continue
    else:
        if in_table:
            in_table = False
            tbl_obj = flush_table(table_lines)
            table_lines = []
            if tbl_obj:
                story.append(Spacer(1, 4))
                story.append(tbl_obj)
                story.append(Spacer(1, 4))
                
    if not line:
        continue
        
    if line.startswith('# CHAPTER'):
        story.append(PageBreak())
        title_text = line.replace('# ', '')
        story.append(Paragraph('<b>' + title_text + '</b>', h1_style))
        story.append(Spacer(1, 8))
    elif line.startswith('## '):
        sub_text = line.replace('## ', '')
        story.append(Paragraph('<b>' + sub_text + '</b>', h2_style))
        story.append(Spacer(1, 4))
    elif line.startswith('### '):
        sub_text = line.replace('### ', '')
        story.append(Paragraph('<b>' + sub_text + '</b>', h3_style))
        story.append(Spacer(1, 3))
    elif line.startswith('---'):
        continue
    else:
        clean_p = line.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
        clean_p = clean_p.replace('&lt;b&gt;', '<b>').replace('&lt;/b&gt;', '</b>')
        clean_p = clean_p.replace('&lt;i&gt;', '<i>').replace('&lt;/i&gt;', '</i>')
        clean_p = re.sub(r'\*\*(.*?)\*\*', r'<b>\1</b>', clean_p)
        clean_p = re.sub(r'`(.*?)`', r'<font face="Courier"><b>\1</b></font>', clean_p)
        story.append(Paragraph(clean_p, body_style))

if in_table and table_lines:
    tbl_obj = flush_table(table_lines)
    if tbl_obj:
        story.append(tbl_obj)

doc.build(story)
print('SUCCESS_PDF_GENERATED')
