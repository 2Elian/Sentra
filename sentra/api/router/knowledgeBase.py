from fastapi import APIRouter, Depends, HTTPException

from sentra.api.models.common import CommonResponse
from sentra.core.knowledge_graph import KgBuilder
from sentra.core.agents import OCRAgent
from sentra.api.core.dependencies import get_kgBuilder_async, get_ocrService_async
from sentra.api.models.knowledge_graph import BuildReturnModel
from sentra.api.models.knowledge import MdParseRequest, MdParseReponse
from sentra.api.models.common import ContractGraphRequest
from sentra.utils.logger import knowledgeBase_logger as logger

knowledge_router = APIRouter(prefix="/knowledge", tags=["Knowledge Service"])

@knowledge_router.post("/build", response_model=CommonResponse, tags=["Knowledge Service"], summary="完整的知识库构建pipeline接口")
def build_knowledge_base():
    pass

# TODO 完成入参 出参 loger内容的适配
@knowledge_router.post("/d2kg", response_model=BuildReturnModel, tags=["Knowledge Service"],
                  summary="Document2KnowledgeGraph")
async def build_contract_graph(request: ContractGraphRequest,
                               kg_builder: KgBuilder = Depends(get_kgBuilder_async), ) -> BuildReturnModel:
    try:
        logger.info(f"build contract graph: id={request.contract_id}")
        # Extract entities and relationships
        nodes, edges, namespaces = await kg_builder.build_graph(
            md_content=request.contract_text,
            contract_id=request.contract_id
        )
        # Prepare response
        response = BuildReturnModel(
            status="success",
            nodes=nodes,
            edges=edges,
            graph_namespace=namespaces
        )

        logger.info(f"{request.contract_id} successfully built contract graph")
        return response

    except Exception as e:
        logger.error(f"Failed to build contract graph: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Failed to build contract graph: {str(e)}"
    )

@knowledge_router.post("/mdParse", response_model=MdParseReponse, tags=["Knowledge Service"],
                  summary="OCR下层服务-md格式的文本进行章节重构")
async def parse_knowledge_service(request: MdParseRequest, service: OCRAgent = Depends(get_ocrService_async), ) -> MdParseReponse:
    try:
        logger.info(f"process document_id: {request.documentId}, kbId: {request.kbId}")
        new_md_content = await service.run(request.md_content)
        reponse = MdParseReponse(new_md_content=new_md_content)
        return reponse
    except Exception as e:
        logger.error(f"Failed to parse md content: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Failed to parse md content: {str(e)}"
    )