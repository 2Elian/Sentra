from fastapi import APIRouter, Depends, HTTPException

from sentra.core import (
    MarkdownParser, SplitterFactory, BuildConfiguration
)
from sentra.api.core.dependencies import (
    get_factory,
    ProductionWorkflowFactory,
)
from sentra.api.models.knowledge import (
    MdParseRequest, MdParseReponse,
    D2KgRequest, D2KgReponse,
    KbPipelineRequest, KbPipelineReponse
)
from sentra.api.models.common import StatusType
from sentra import settings
from sentra.utils.save_json import save_qa_pair
from sentra.utils.logger import knowledgeBase_logger as logger

knowledge_router = APIRouter(prefix="/knowledge", tags=["Knowledge Service"])

@knowledge_router.post("/build", response_model=KbPipelineReponse, tags=["Knowledge Service"], summary="完整的知识库构建pipeline接口")
async def build_knowledge_base(request: KbPipelineRequest,
                                factory: ProductionWorkflowFactory = Depends(get_factory),) -> KbPipelineReponse:
    try:
        # 初始化Pipeline服务
        kb_pipeline = factory.create_kb_pipeline_workflow(vector_store=None) # 可以传递vector_store
        result = await kb_pipeline.build_knowledge_base(
            markdown_content=request.content,
            kb_id=request.kbID,
            doc_id=request.docID,
            entity_types=request.entity_types,
            entity_types_des=request.entity_types_des
        )
        return KbPipelineReponse(
            status=StatusType.SUCCESS,
            total_chunks=result.total_chunks,
            total_entities=result.total_entities,
            total_edges=result.total_edges,
            total_qac=result.total_qac
        )
    except Exception as e:
        logger.error(f"Failed to build knowledge: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Failed to build knowledge: {str(e)}"
        )

@knowledge_router.post("/d2kg", response_model=D2KgReponse, tags=["Knowledge Service"], summary="单个文档build图和qap接口 没有embedding")
async def build_contract_graph(request: D2KgRequest,
                                factory: ProductionWorkflowFactory = Depends(get_factory),) -> D2KgReponse:
    try:
        kg_builder = factory.create_current_contract_graphBuild_workflow()
        logger.info(f"build graph: doc_id={request.docID}")
        # Step 1: Parse markdown
        sentra_kg_config = BuildConfiguration()
        logger.info("[1/4] Parsing markdown document...")
        document = MarkdownParser.parse(request.content, kb_id=request.kbID, doc_id=request.docID, title=request.title)
        logger.info(f"  - Parsed {len(document.sections)} sections")

        # Step 2: Chunk document
        logger.info("[2/4] Chunking document...")
        splitter = SplitterFactory.create(
            strategy=sentra_kg_config.chunk_strategy,
            chunk_size=sentra_kg_config.chunk_size,
            chunk_overlap=sentra_kg_config.chunk_overlap,
            tokenizer=kg_builder.llm_sentra.tokenizer
        )
        chunks = splitter.split(document, request.kbID)
        logger.info(f"  - Created {len(chunks)} chunks")
        # Step 3: BuildGraph
        logger.info("[3/4] Building knowledge graph...")
        nodes, edges, namespaces = await kg_builder.build_graph(
            chunks=chunks,
            doc_id=request.docID,
            kb_id=request.kbID,
            entity_types=request.entity_types,
            entity_types_des=request.entity_types_des
        )
        # Step 4: build qap
        logger.info("[4/4] Building Question Answer Pairs...")
        gqag_agent = factory.create_gqag_agent_workflow()
        results_aggregated, results_multihop, results_cot = await gqag_agent.build(namespaces, request.kbID)
        save_dir = f"{settings.kg.working_dir}/{request.kbID}/{namespaces}"
        save_qa_pair(save_dir, results_aggregated, results_multihop, results_cot)

        # Prepare response
        response = D2KgReponse(
            status=StatusType.SUCCESS,
            nodes=nodes,
            edges=edges,
            namespace=namespaces,
            qap=results_aggregated+results_multihop+results_cot
        )

        logger.info(f"{request.docID} successfully built contract graph")
        return response

    except Exception as e:
        logger.error(f"Failed to build graph: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Failed to build graph: {str(e)}"
    )

@knowledge_router.post("/mdParse", response_model=MdParseReponse, tags=["Knowledge Service"],
                  summary="OCR下层服务-md格式的文本进行章节重构")
async def parse_knowledge_service(request: MdParseRequest, factory: ProductionWorkflowFactory = Depends(get_factory),) -> MdParseReponse:
    try:
        ocr_agent = factory.create_md_parser_workflow()
        logger.info(f"process document_id: {request.documentId}, kbId: {request.kbId}")
        new_md_content = await ocr_agent.run(request.md_content)
        reponse = MdParseReponse(
            status=StatusType.SUCCESS,
            new_md_content=new_md_content)
        return reponse
    except Exception as e:
        logger.error(f"Failed to parse md content: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Failed to parse md content: {str(e)}"
    )