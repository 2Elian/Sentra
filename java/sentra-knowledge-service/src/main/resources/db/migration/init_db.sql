-- 初始化 sentra_knowledge 数据库表结构 这个.sql用于初始化模板实体类型和描述 分别是合同和论文
-- 执行命令: docker exec -i postgres-sentra psql -U postgres -d sentra_knowledge < init_db.sql

-- 实体类型模板表
CREATE TABLE IF NOT EXISTS t_entity_type_template (
    id VARCHAR(32) PRIMARY KEY,
    tenant_id VARCHAR(32) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_system BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(32),
    updated_by VARCHAR(32)
);

CREATE INDEX IF NOT EXISTS idx_template_tenant ON t_entity_type_template(tenant_id);
CREATE INDEX IF NOT EXISTS idx_template_system ON t_entity_type_template(is_system);
CREATE INDEX IF NOT EXISTS idx_template_active ON t_entity_type_template(is_active);

-- 实体类型定义表
CREATE TABLE IF NOT EXISTS t_entity_type_definition (
    id VARCHAR(32) PRIMARY KEY,
    template_id VARCHAR(32) NOT NULL,
    entity_code VARCHAR(50) NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    entity_description TEXT NOT NULL,
    display_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_template_entity_code UNIQUE (template_id, entity_code)
);

CREATE INDEX IF NOT EXISTS idx_definition_template ON t_entity_type_definition(template_id);
CREATE INDEX IF NOT EXISTS idx_definition_order ON t_entity_type_definition(display_order);

-- 知识库表 (基础字段,根据Entity类推断)
CREATE TABLE IF NOT EXISTS t_knowledge_base (
    id BIGSERIAL PRIMARY KEY,
    kb_id VARCHAR(100) NOT NULL UNIQUE,
    tenant_id VARCHAR(32) NOT NULL,
    name VARCHAR(255) NOT NULL,
    owner_user_id VARCHAR(32),
    scope VARCHAR(50),
    description TEXT,
    entity_template_id VARCHAR(32),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(32),
    updated_by VARCHAR(32),
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_kb_tenant ON t_knowledge_base(tenant_id);
CREATE INDEX IF NOT EXISTS idx_kb_owner ON t_knowledge_base(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_kb_entity_template ON t_knowledge_base(entity_template_id);

-- 文档表 (基础字段)
CREATE TABLE IF NOT EXISTS t_document (
    id BIGSERIAL PRIMARY KEY,
    kb_id VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(32) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    file_size BIGINT,
    file_type VARCHAR(50),
    remote_filepath TEXT,
    ocr_result_path TEXT,
    document_unique_id VARCHAR(100),
    status VARCHAR(50) DEFAULT 'UPLOADED',
    progress INT DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(32),
    updated_by VARCHAR(32),
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_doc_kb ON t_document(kb_id);
CREATE INDEX IF NOT EXISTS idx_doc_tenant ON t_document(tenant_id);
CREATE INDEX IF NOT EXISTS idx_doc_status ON t_document(status);

-- 插入系统预置的合同领域实体类型模板
INSERT INTO t_entity_type_template (
    id, tenant_id, name, description, is_system, is_active, created_by
) VALUES (
    'sys_template_contract',
    'system',
    '合同领域',
    '适用于合同、协议类文档的实体类型定义',
    TRUE,
    TRUE,
    'system'
) ON CONFLICT (id) DO NOTHING;

-- 插入合同领域的实体类型定义
INSERT INTO t_entity_type_definition (
    id, template_id, entity_code, entity_name, entity_description, display_order
) VALUES
('entity_001', 'sys_template_contract', 'ContractParty', '合同主体', '合同主体(甲乙方)', 1),
('entity_002', 'sys_template_contract', 'RelatedParty', '关联方', '关联方(担保人、代理人)', 2),
('entity_003', 'sys_template_contract', 'Person', '自然人', '自然人', 3),
('entity_004', 'sys_template_contract', 'Organization', '组织机构', '组织机构', 4),
('entity_005', 'sys_template_contract', 'Contract', '合同本身', '合同本身', 5),
('entity_006', 'sys_template_contract', 'ProductService', '产品或服务', '产品或服务', 6),
('entity_007', 'sys_template_contract', 'RightObligation', '权利或义务', '权利或义务', 7),
('entity_008', 'sys_template_contract', 'IntellectualProperty', '知识产权', '知识产权', 8),
('entity_009', 'sys_template_contract', 'Amount', '金额', '金额', 9),
('entity_010', 'sys_template_contract', 'DateTerm', '日期或期限', '日期或期限', 10),
('entity_011', 'sys_template_contract', 'Location', '地点', '地点', 11),
('entity_012', 'sys_template_contract', 'Condition', '条件', '条件', 12),
('entity_013', 'sys_template_contract', 'BreachClause', '违约条款', '违约条款', 13),
('entity_014', 'sys_template_contract', 'SpecificTime', '具体时间', '具体时间', 14),
('entity_015', 'sys_template_contract', 'TimeSpan', '时间段', '时间段', 15),
('entity_016', 'sys_template_contract', 'SpecificLocation', '具体地点', '具体地点', 16),
('entity_017', 'sys_template_contract', 'Currency', '货币', '货币', 17),
('entity_018', 'sys_template_contract', 'Unit', '度量单位', '度量单位', 18)
ON CONFLICT (id) DO NOTHING;

-- 插入论文领域实体类型模板
INSERT INTO t_entity_type_template (
    id, tenant_id, name, description, is_system, is_active, created_by
) VALUES (
    'sys_template_paper',
    'system',
    '论文领域',
    '适用于学术论文、研究报告的实体类型定义',
    TRUE,
    TRUE,
    'system'
) ON CONFLICT (id) DO NOTHING;

-- 插入论文领域的实体类型定义
INSERT INTO t_entity_type_definition (
    id, template_id, entity_code, entity_name, entity_description, display_order
) VALUES
('entity_101', 'sys_template_paper', 'Author', '作者', '论文作者', 1),
('entity_102', 'sys_template_paper', 'Institution', '机构', '所属机构', 2),
('entity_103', 'sys_template_paper', 'Publication', '出版物', '发表的期刊或会议', 3),
('entity_104', 'sys_template_paper', 'Keyword', '关键词', '论文关键词', 4),
('entity_105', 'sys_template_paper', 'Method', '研究方法', '研究方法或算法', 5),
('entity_106', 'sys_template_paper', 'Dataset', '数据集', '使用的数据集', 6),
('entity_107', 'sys_template_paper', 'Metric', '评估指标', '评估指标', 7),
('entity_108', 'sys_template_paper', 'Result', '研究结果', '主要研究结果', 8)
ON CONFLICT (id) DO NOTHING;
