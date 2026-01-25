from typing import Optional, Dict, Any, List, Tuple
from pydantic import BaseModel

class MdParseRequest(BaseModel):
    md_content: str
    documentId: str
    kbId: str

class MdParseReponse(BaseModel):
    new_md_content: str