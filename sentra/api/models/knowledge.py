from typing import Optional, Dict, Any, List, Tuple
from pydantic import BaseModel

from .common import StatusType
from sentra.core.pipeline.build_manager import BuildResult

class MdParseRequest(BaseModel):
    md_content: str
    documentId: str
    kbId: str

class MdParseReponse(BaseModel):
    status: StatusType
    new_md_content: str

class D2KgRequest(BaseModel):
    docID: str
    kbID: str # 这个传递过来 应该在前端让用户选一下是否入库 如果入库的话 这个值就应该是数据库里面唯一的 如果不入库的话 那么就给一个uuid
    content: str
    title: Optional[str] = None
    entity_types: Optional[List[str]] = None
    entity_types_des: Optional[Dict[str, str]] = None

class D2KgReponse(BaseModel):
    status: StatusType
    nodes: list[tuple[str, dict]]
    edges: list[tuple[str, str, dict]]
    namespace: str
    qap: Any

class KbPipelineRequest(BaseModel):
    docID: str
    kbID: str
    content: str
    title: Optional[str] = None
    entity_types: Optional[Any] = None
    entity_types_des: Optional[Any] = None

class KbPipelineReponse(BaseModel):
    status: StatusType
    total_chunks: int
    total_entities: int
    total_edges: int
    total_qac: int