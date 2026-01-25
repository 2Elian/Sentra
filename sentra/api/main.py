try:
    from contextlib import asynccontextmanager
except ImportError:
    from contextlib2 import asynccontextmanager # type: ignore
import os
import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from datetime import datetime

from sentra.api.middleware import register_exception_handlers
from sentra import settings
from sentra.utils import setup_logger, logger
from sentra.api.core import get_factory
from sentra.api.router import knowledge_router, gqag_router

os.environ['HTTP_PROXY'] = ''
os.environ['HTTPS_PROXY'] = ''
os.environ['http_proxy'] = ''
os.environ['https_proxy'] = ''
os.environ['NO_PROXY'] = '*'
os.environ['no_proxy'] = '*'

@asynccontextmanager
async def lifespan(fastapi_app: FastAPI):
    logger.info("Starting the sentra API service...")
    await get_factory()
    logger.info("sentra API service started successfully")
    try:
        yield
    finally:
        logger.info("The sentra API service is shut down")

def create_app() -> FastAPI:
    app = FastAPI(
        title=settings.app.name,
        description="Welcome to sentra",
        version=settings.app.version,
        lifespan=lifespan,
        docs_url="/docs",
        redoc_url="/redoc",
        openapi_url="/openapi.json"
    )
    app.add_middleware(
        CORSMiddleware,
        allow_origins="*",
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )
    register_exception_handlers(app)
    
    # Include routers
    app.include_router(knowledge_router, prefix=settings.app.api_prefix)
    app.include_router(gqag_router, prefix=settings.app.api_prefix)

    @app.get("/", tags=["Root"])
    async def root():
        return {
            "message": "sentra API",
            "version": "1.0.0",
            "status": "running",
            "docs": "/docs"
        }

    @app.get("/health", tags=["Health"])
    async def health_check():
        return {
            "status": "healthy",
            "timestamp": datetime.now().isoformat(),
            "version": getattr(settings.app, "version", "1.0.0")
        }

    return app


app = create_app()

if __name__ == "__main__":
    uvicorn.run(
        "sentra.api.main:app", 
        host=settings.server.host, 
        port=settings.server.port, 
        reload=settings.app.debug
    )
