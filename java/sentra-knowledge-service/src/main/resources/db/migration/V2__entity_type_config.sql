-- ========================================
-- 实体类型配置表
-- 用于存储不同领域文档的实体类型定义
-- ========================================

-- 主表：实体类型模板
-- 每个模板代表一个领域的实体配置（如"合同领域"、"论文领域"）
CREATE TABLE t_entity_type_template (
    id VARCHAR(32) PRIMARY KEY,
    tenant_id VARCHAR(32) NOT NULL,
    name VARCHAR(100) NOT NULL COMMENT '模板名称（如：合同领域、论文领域）',
    description TEXT COMMENT '模板描述',
    is_system BOOLEAN DEFAULT FALSE COMMENT '是否为系统预置模板',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(32) COMMENT '创建人ID',
    updated_by VARCHAR(32) COMMENT '更新人ID',

    CONSTRAINT uk_tenant_template_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_template_tenant ON t_entity_type_template(tenant_id);
CREATE INDEX idx_template_system ON t_entity_type_template(is_system);
CREATE INDEX idx_template_active ON t_entity_type_template(is_active);

-- 从表：实体类型定义
-- 存储每个模板下的具体实体类型
CREATE TABLE t_entity_type_definition (
    id VARCHAR(32) PRIMARY KEY,
    template_id VARCHAR(32) NOT NULL,
    entity_code VARCHAR(50) NOT NULL COMMENT '实体编码（如：ContractParty）',
    entity_name VARCHAR(100) NOT NULL COMMENT '实体名称（如：合同主体）',
    entity_description TEXT NOT NULL COMMENT '实体描述（如：合同主体（甲乙方））',
    display_order INT DEFAULT 0 COMMENT '显示顺序',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_template_entity_code UNIQUE (template_id, entity_code),
    CONSTRAINT fk_template FOREIGN KEY (template_id)
        REFERENCES t_entity_type_template(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_definition_template ON t_entity_type_definition(template_id);
CREATE INDEX idx_definition_order ON t_entity_type_definition(display_order);

-- 知识库表新增字段：关联实体类型模板
ALTER TABLE t_knowledge_base ADD COLUMN entity_template_id VARCHAR(32);
ALTER TABLE t_knowledge_base ADD CONSTRAINT fk_entity_template
    FOREIGN KEY (entity_template_id)
    REFERENCES t_entity_type_template(id)
    ON DELETE SET NULL;

CREATE INDEX idx_kb_entity_template ON t_knowledge_base(entity_template_id);

-- ========================================
-- 初始化系统预置的合同领域实体类型模板
-- ========================================

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
);

-- 插入合同领域的实体类型定义
INSERT INTO t_entity_type_definition (
    id, template_id, entity_code, entity_name, entity_description, display_order
) VALUES
-- 参与方
('entity_001', 'sys_template_contract', 'ContractParty', '合同主体', '合同主体（甲乙方）', 1),
('entity_002', 'sys_template_contract', 'RelatedParty', '关联方', '关联方（担保人、代理人）', 2),
('entity_003', 'sys_template_contract', 'Person', '自然人', '自然人', 3),
('entity_004', 'sys_template_contract', 'Organization', '组织机构', '组织机构', 4),

-- 标的物
('entity_005', 'sys_template_contract', 'Contract', '合同本身', '合同本身', 5),
('entity_006', 'sys_template_contract', 'ProductService', '产品或服务', '产品或服务', 6),
('entity_007', 'sys_template_contract', 'RightObligation', '权利或义务', '权利或义务', 7),
('entity_008', 'sys_template_contract', 'IntellectualProperty', '知识产权', '知识产权', 8),

-- 核心条款
('entity_009', 'sys_template_contract', 'Amount', '金额', '金额', 9),
('entity_010', 'sys_template_contract', 'DateTerm', '日期或期限', '日期或期限', 10),
('entity_011', 'sys_template_contract', 'Location', '地点', '地点', 11),
('entity_012', 'sys_template_contract', 'Condition', '条件', '条件', 12),
('entity_013', 'sys_template_contract', 'BreachClause', '违约条款', '违约条款', 13),

-- 时空与度量
('entity_014', 'sys_template_contract', 'SpecificTime', '具体时间', '具体时间', 14),
('entity_015', 'sys_template_contract', 'TimeSpan', '时间段', '时间段', 15),
('entity_016', 'sys_template_contract', 'SpecificLocation', '具体地点', '具体地点', 16),
('entity_017', 'sys_template_contract', 'Currency', '货币', '货币', 17),
('entity_018', 'sys_template_contract', 'Unit', '度量单位', '度量单位', 18);

-- ========================================
-- 初始化论文领域实体类型模板（示例）
-- ========================================

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
);

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
('entity_108', 'sys_template_paper', 'Result', '研究结果', '主要研究结果', 8);
