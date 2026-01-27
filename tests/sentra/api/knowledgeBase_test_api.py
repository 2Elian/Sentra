import argparse
import json
import requests
import sys
from typing import Dict

BASE_URL = "http://localhost:8000"

HEADERS = {
    "Content-Type": "application/json"
}


def test_md_parse():
    """
    测试 /sentra/v1/knowledge/mdParse 接口
    """
    url = f"{BASE_URL}/sentra/v1/knowledge/mdParse"
    print(f"[TEST] POST {url}")
    md_content = """
    写入一个章节较为混乱的md格式的content
    """
    payload: Dict = {
        "md_content": md_content,
        "documentId": "docID123",
        "kbId": "KbID123"
    }
    resp = requests.post(
        url,
        headers=HEADERS,
        data=json.dumps(payload),
        timeout=30
    )

    print(f"[STATUS] {resp.status_code}")
    print("[RESPONSE]")
    try:
        print(json.dumps(resp.json(), indent=2, ensure_ascii=False))
    except Exception:
        print(resp.text)

def test_d2kg_api():
    """
    测试 /sentra/v1/knowledge/d2kg 接口
    """
    url = f"{BASE_URL}/sentra/v1/knowledge/d2kg"
    print(f"[TEST] POST {url}")
    content = """
        # 项目说明文档\n\n## 一、系统背景\n\n系统最初是为了提升效率而设计的，但是在实际使用中发现了一些问题。\n\n### 技术选型\n\n我们使用了很多技术，包括但不限于 Java、Python、Docker。\n\n#### 数据库\n\n数据库选型比
    较随意，前期用 MySQL，后期又加了 PostgreSQL。\n\n## 功能介绍\n\n系统功能很多，但没有很好地分类。\n\n### 用户模块\n\n用户可以登录、注册。\n\n## 日志系统\n\n日志系统其实属于基础设施，但这里先写一下。\n\n### 登录\n\n登录功能支持用 
    户名密码。\n\n#### 登录失败情况\n\n如果失败，会返回错误码。\n\n## 二、部署方式\n\n部署方式比较复杂。\n\n### Docker 部署\n\n使用 docker-compose 启动。\n\n## 功能介绍（补充）\n\n这里是前面功能介绍的补充，但是章节重复了。\n\n### 权
    限控制\n\n权限控制还没完全做好。\n\n# 附录\n\n一些额外的说明。\n\n## 三、未来规划\n\n未来可能会支持更多功能。\n\n### 功能介绍
        """
    from typing import Dict, Any, List, Optional, Tuple, Literal, get_args
    from enum import Enum
    class EntityTypeEnum(str, Enum):
        # 参与方
        ContractParty = "合同主体（甲乙方）"
        RelatedParty = "关联方（担保人、代理人）"
        Person = "自然人"
        Organization = "组织机构"

        # 标的物
        Contract = "合同本身"
        ProductService = "产品或服务"
        RightObligation = "权利或义务"
        IntellectualProperty = "知识产权"

        # 核心条款
        Amount = "金额"
        DateTerm = "日期或期限"
        Location = "地点"
        Condition = "条件"
        BreachClause = "违约条款"

        # 时空与度量
        SpecificTime = "具体时间"
        TimeSpan = "时间段"
        SpecificLocation = "具体地点"
        Currency = "货币"
        Unit = "度量单位"

    EntityType = Literal[
        "ContractParty",
        "RelatedParty",
        "Person",
        "Organization",
        "Contract",
        "ProductService",
        "RightObligation",
        "IntellectualProperty",
        "Amount",
        "DateTerm",
        "Location",
        "Condition",
        "BreachClause",
        "SpecificTime",
        "TimeSpan",
        "SpecificLocation",
        "Currency",
        "Unit"
    ]
    ENTITY_DES = {
        e.name: e.value
        for e in EntityTypeEnum
    }
    ENTITY_LIST = list(get_args(EntityType))
    payload: Dict = {
        "content": content,
        "docID": "docID123",
        "kbID": "KbID123",
        "entity_types": ENTITY_LIST,
        "entity_types_des": ENTITY_DES
    }
    resp = requests.post(
        url,
        headers=HEADERS,
        data=json.dumps(payload),
        timeout=300
    )
    print("[RESPONSE]")
    try:
        print(json.dumps(resp.json(), indent=2, ensure_ascii=False))
    except Exception:
        print(resp.text)

def test_pipeline_api():
    """
    测试 /sentra/v1/knowledge/build 接口
    """
    url = f"{BASE_URL}/sentra/v1/knowledge/build"
    print(f"[TEST] POST {url}")
    content = """
        # 项目说明文档\n\n## 一、系统背景\n\n系统最初是为了提升效率而设计的，但是在实际使用中发现了一些问题。\n\n### 技术选型\n\n我们使用了很多技术，包括但不限于 Java、Python、Docker。\n\n#### 数据库\n\n数据库选型比
    较随意，前期用 MySQL，后期又加了 PostgreSQL。\n\n## 功能介绍\n\n系统功能很多，但没有很好地分类。\n\n### 用户模块\n\n用户可以登录、注册。\n\n## 日志系统\n\n日志系统其实属于基础设施，但这里先写一下。\n\n### 登录\n\n登录功能支持用 
    户名密码。\n\n#### 登录失败情况\n\n如果失败，会返回错误码。\n\n## 二、部署方式\n\n部署方式比较复杂。\n\n### Docker 部署\n\n使用 docker-compose 启动。\n\n## 功能介绍（补充）\n\n这里是前面功能介绍的补充，但是章节重复了。\n\n### 权
    限控制\n\n权限控制还没完全做好。\n\n# 附录\n\n一些额外的说明。\n\n## 三、未来规划\n\n未来可能会支持更多功能。\n\n### 功能介绍
        """
    from typing import Dict, Any, List, Optional, Tuple, Literal, get_args
    from enum import Enum
    class EntityTypeEnum(str, Enum):
        # 参与方
        ContractParty = "合同主体（甲乙方）"
        RelatedParty = "关联方（担保人、代理人）"
        Person = "自然人"
        Organization = "组织机构"

        # 标的物
        Contract = "合同本身"
        ProductService = "产品或服务"
        RightObligation = "权利或义务"
        IntellectualProperty = "知识产权"

        # 核心条款
        Amount = "金额"
        DateTerm = "日期或期限"
        Location = "地点"
        Condition = "条件"
        BreachClause = "违约条款"

        # 时空与度量
        SpecificTime = "具体时间"
        TimeSpan = "时间段"
        SpecificLocation = "具体地点"
        Currency = "货币"
        Unit = "度量单位"

    EntityType = Literal[
        "ContractParty",
        "RelatedParty",
        "Person",
        "Organization",
        "Contract",
        "ProductService",
        "RightObligation",
        "IntellectualProperty",
        "Amount",
        "DateTerm",
        "Location",
        "Condition",
        "BreachClause",
        "SpecificTime",
        "TimeSpan",
        "SpecificLocation",
        "Currency",
        "Unit"
    ]
    ENTITY_DES = {
        e.name: e.value
        for e in EntityTypeEnum
    }
    ENTITY_LIST = list(get_args(EntityType))
    payload: Dict = {
        "content": content,
        "docID": "docID123",
        "kbID": "KbID123",
        "entity_types": ENTITY_LIST,
        "entity_types_des": ENTITY_DES
    }
    resp = requests.post(
        url,
        headers=HEADERS,
        data=json.dumps(payload),
        timeout=300
    )
    print("[RESPONSE]")
    try:
        print(json.dumps(resp.json(), indent=2, ensure_ascii=False))
    except Exception:
        print(resp.text)



def main():
    parser = argparse.ArgumentParser(
        description="Sentra MD Parse API 测试工具"
    )
    parser.add_argument(
        "-v",
        "--version",
        required=True,
        choices=["mdParse", "d2kg", "pipeline"],
        help="选择要测试的接口函数"
    )

    args = parser.parse_args()

    if args.version == "mdParse":
        test_md_parse()
    elif args.version == "d2kg":
        test_d2kg_api()
    elif args.version == "pipeline":
        test_pipeline_api()
    else:
        print("未知的测试版本")
        sys.exit(1)


if __name__ == "__main__":
    main()
