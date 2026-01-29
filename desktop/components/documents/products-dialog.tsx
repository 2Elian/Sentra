'use client';

import { useEffect, useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Button } from '@/components/ui/button';
import { documentApi } from '@/lib/api/documentApi';
import { Document } from '@/types/knowledge';
import { KnowledgeGraphView } from './knowledge-graph-view';
import { ChunksView } from './chunks-view';
import { QAPairsView } from './qa-pairs-view';

interface DocumentProductsDialogProps {
  document: Document;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function DocumentProductsDialog({
  document,
  open,
  onOpenChange,
}: DocumentProductsDialogProps) {
  const [productPaths, setProductPaths] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      fetchProductPaths();
    }
  }, [open, document.id]);

  const fetchProductPaths = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const paths = await documentApi.getProductPaths(document.id, document.kbId);
      console.log('Product paths:', paths);
      setProductPaths(paths);
    } catch (error: any) {
      console.error('Failed to fetch product paths:', error);
      setError(error.message || '获取产物路径失败');
    } finally {
      setIsLoading(false);
    }
  };

  // 解析产物路径，提取不同类型的产物
  const parseProductPaths = () => {
    const result = {
      graphml: '',
      chunks: '',
      aggregated: '',
      multiHop: '',
      cot: '',
    };

    productPaths.forEach((path) => {
      if (path.endsWith('.graphml')) {
        result.graphml = path;
      } else if (path.endsWith('chunks.json')) {
        result.chunks = path;
      } else if (path.endsWith('aggregated.json')) {
        result.aggregated = path;
      } else if (path.endsWith('multi_hop.json')) {
        result.multiHop = path;
      } else if (path.endsWith('cot.json')) {
        result.cot = path;
      }
    });

    return result;
  };

  const products = parseProductPaths();

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-6xl max-h-[90vh] overflow-hidden">
        <DialogHeader>
          <DialogTitle className="text-2xl">文档解析产物</DialogTitle>
          <DialogDescription>
            文档: {document.filename}
          </DialogDescription>
        </DialogHeader>

        {isLoading ? (
          <div className="flex h-96 items-center justify-center">
            <div className="flex flex-col items-center gap-4">
              <div className="h-12 w-12 animate-spin rounded-full border-4 border-primary border-t-transparent" />
              <p className="text-muted-foreground">加载产物中...</p>
            </div>
          </div>
        ) : error ? (
          <div className="flex h-96 items-center justify-center">
            <div className="text-center space-y-3">
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-destructive/10 mx-auto">
                <svg className="h-8 w-8 text-destructive" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <p className="text-destructive font-medium">加载失败</p>
              <p className="text-sm text-muted-foreground">{error}</p>
              <Button onClick={fetchProductPaths} variant="outline">
                重试
              </Button>
            </div>
          </div>
        ) : productPaths.length === 0 ? (
          <div className="flex h-96 items-center justify-center">
            <div className="text-center space-y-3">
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-muted/50 mx-auto">
                <svg className="h-8 w-8 text-muted-foreground/50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              </div>
              <p className="text-muted-foreground">暂无产物</p>
              <p className="text-sm text-muted-foreground/70">文档尚未完成解析</p>
            </div>
          </div>
        ) : (
          <Tabs defaultValue="graph" className="flex-1 overflow-hidden">
            <TabsList className="grid w-full grid-cols-3">
              <TabsTrigger value="graph">知识图谱</TabsTrigger>
              <TabsTrigger value="chunks">文档切块</TabsTrigger>
              <TabsTrigger value="qa">问答对</TabsTrigger>
            </TabsList>

            <TabsContent value="graph" className="mt-6 h-[calc(90vh-200px)] overflow-hidden">
              {products.graphml ? (
                <KnowledgeGraphView graphmlPath={products.graphml} />
              ) : (
                <div className="flex h-full items-center justify-center">
                  <p className="text-muted-foreground">暂无知识图谱产物</p>
                </div>
              )}
            </TabsContent>

            <TabsContent value="chunks" className="mt-6 h-[calc(90vh-200px)] overflow-hidden">
              {products.chunks ? (
                <ChunksView chunksPath={products.chunks} />
              ) : (
                <div className="flex h-full items-center justify-center">
                  <p className="text-muted-foreground">暂无切块产物</p>
                </div>
              )}
            </TabsContent>

            <TabsContent value="qa" className="mt-6 h-[calc(90vh-200px)] overflow-hidden">
              {(products.aggregated || products.multiHop || products.cot) ? (
                <QAPairsView
                  aggregatedPath={products.aggregated}
                  multiHopPath={products.multiHop}
                  cotPath={products.cot}
                />
              ) : (
                <div className="flex h-full items-center justify-center">
                  <p className="text-muted-foreground">暂无问答对产物</p>
                </div>
              )}
            </TabsContent>
          </Tabs>
        )}
      </DialogContent>
    </Dialog>
  );
}
