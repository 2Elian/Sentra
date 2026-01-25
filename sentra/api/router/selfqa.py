import json
import os
import pandas as pd
from fastapi import APIRouter, Depends, HTTPException

from sentra import settings
from sentra.utils.common import normalize_result, serialize_item
from sentra.utils.logger import knowledgeBase_logger as logger
from sentra.api.core.dependencies import get_generatorSerivce_async
from sentra.api.models.gqag import gqagResponse
from sentra.api.models.gqag import gqagRequest, gqagSubgrapnResponse
from sentra.core.agents.gqag.sub_graph import SubGraphBuilder
from sentra.core.agents import GenerateService
gqag_router = APIRouter(prefix="/gqag", tags=["SELF-QA"])  # current contract knowledge graph


@gqag_router.post("/build", response_model=gqagResponse, tags=["SELF-QA"],
                  summary="Build a contract graph for current contract")
async def build_gqag(request: gqagRequest,
                               generatorService: GenerateService = Depends(get_generatorSerivce_async), ) -> gqagResponse:
    try:
        results, results_multihop, results_cot = await generatorService.build(namespace=request.namespace)
        save_dir = f"{settings.kg.working_dir}/gqag_data/{request.namespace}"
        os.makedirs(save_dir, exist_ok=True)
        save_path_aggregated = f"{save_dir}/aggregated.json"
        save_path_multihop = f"{save_dir}/multi_hop.json"
        save_path_cot = f"{save_dir}/cot.json"
        with open(save_path_aggregated, 'w', encoding='utf-8') as f:
            json.dump(results, f, ensure_ascii=False, indent=2)
        with open(save_path_multihop, 'w', encoding='utf-8') as f:
            json.dump(results_multihop, f, ensure_ascii=False, indent=2)
        with open(save_path_cot, 'w', encoding='utf-8') as f:
            json.dump(results_cot, f, ensure_ascii=False, indent=2)
        logger.info(f"{request.namespace} successfully generate self qa data")
        response = gqagResponse(
            status = "success",
            aggregated = results,
            multi_hop = results_multihop,
            cot = results_cot
        )
        return response

    except Exception as e:
        logger.error(f"Failed to build contract graph: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Failed to build contract graph: {str(e)}"
        )


@gqag_router.post("/subgraph", response_model=gqagSubgrapnResponse, tags=["SELF-QA"])
async def test_gqag_subgraph(request: gqagRequest) -> gqagSubgrapnResponse:
    try:
        builder = SubGraphBuilder()
        batches, batches_leiden = await builder(request.namespace)

        return gqagSubgrapnResponse(
            status = "success",
            batches = batches,
            batches_leiden = batches_leiden
        )
    except Exception as e:
        logger.error(f"Failed to build contract graph: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Failed to build contract graph: {str(e)}"
        )