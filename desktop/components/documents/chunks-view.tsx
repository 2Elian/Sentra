'use client';

import { useEffect, useState } from 'react';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';

interface ChunksViewProps {
  chunksPath: string;
}

interface Chunk {
  chunk_id: string;
  doc_id: string;
  kb_id: string;
  section_id: string;
  content_text: string;
  token_count: number;
  embedding: number[];
  strategy: string;
  metadata: Record<string, any>;
}

export function ChunksView({ chunksPath }: ChunksViewProps) {
  const [chunks, setChunks] = useState<Chunk[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedChunks, setExpandedChunks] = useState<Set<string>>(new Set());

  useEffect(() => {
    const fetchChunks = async () => {
      try {
        // 通过后端API访问文件，使用URL参数避免headers编码问题
        // 将Windows路径的反斜杠转换为正斜杠，确保URL正确
        const normalizedPath = chunksPath.replace(/\\/g, '/');
        const response = await fetch(`/api/knowledge/v1/file?path=${encodeURIComponent(normalizedPath)}`, {
          method: 'GET',
        });

        if (!response.ok) {
          throw new Error('Failed to fetch chunks file');
        }

        const data: Chunk[] = await response.json();
        setChunks(data);
      } catch (err: any) {
        console.error('Failed to fetch chunks:', err);
        setError(err.message || '加载切块失败');
      } finally {
        setIsLoading(false);
      }
    };

    if (chunksPath) {
      fetchChunks();
    }
  }, [chunksPath]);

  const toggleExpand = (chunkId: string) => {
    setExpandedChunks((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(chunkId)) {
        newSet.delete(chunkId);
      } else {
        newSet.add(chunkId);
      }
      return newSet;
    });
  };

  const toggleAll = () => {
    if (expandedChunks.size === chunks.length) {
      setExpandedChunks(new Set());
    } else {
      setExpandedChunks(new Set(chunks.map((c) => c.chunk_id)));
    }
  };

  const filteredChunks = chunks.filter((chunk) =>
    chunk.content_text.toLowerCase().includes(searchQuery.toLowerCase())
  );

  if (isLoading) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <div className="h-12 w-12 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          <p className="text-muted-foreground">加载切块中...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="text-center space-y-3">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-destructive/10 mx-auto">
            <svg className="h-8 w-8 text-destructive" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <p className="text-destructive font-medium">加载失败</p>
          <p className="text-sm text-muted-foreground">{error}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col space-y-4">
      {/* 工具栏 */}
      <div className="flex items-center gap-4">
        <div className="flex-1">
          <Input
            placeholder="搜索切块内容..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="max-w-md"
          />
        </div>
        <Button variant="outline" onClick={toggleAll}>
          {expandedChunks.size === chunks.length ? '收起全部' : '展开全部'}
        </Button>
        <div className="text-sm text-muted-foreground">
          共 {filteredChunks.length} 个切块
        </div>
      </div>

      {/* 切块列表 */}
      <div className="flex-1 overflow-y-auto space-y-3 pr-2">
        {filteredChunks.length === 0 ? (
          <div className="flex h-full items-center justify-center">
            <div className="text-center space-y-3">
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-muted/50 mx-auto">
                <svg className="h-8 w-8 text-muted-foreground/50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </div>
              <p className="text-muted-foreground">未找到匹配的切块</p>
            </div>
          </div>
        ) : (
          filteredChunks.map((chunk) => (
            <Card
              key={chunk.chunk_id}
              className="hover:shadow-md transition-shadow duration-200"
            >
              <div className="p-4">
                {/* 切块头部 */}
                <div
                  className="flex items-center justify-between cursor-pointer"
                  onClick={() => toggleExpand(chunk.chunk_id)}
                >
                  <div className="flex items-center gap-3">
                    <div className="flex items-center justify-center h-8 w-8 rounded-full bg-primary/10">
                      <svg
                        className={`h-4 w-4 text-primary transition-transform ${
                          expandedChunks.has(chunk.chunk_id) ? 'rotate-90' : ''
                        }`}
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                      >
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                      </svg>
                    </div>
                    <div>
                      <div className="font-medium text-sm">
                        Chunk ID: {chunk.chunk_id.slice(0, 16)}...
                      </div>
                      <div className="text-xs text-muted-foreground mt-0.5">
                        Section: {chunk.section_id}
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center gap-4">
                    <div className="text-right">
                      <div className="text-xs text-muted-foreground">Token Count</div>
                      <div className="text-sm font-medium">{chunk.token_count}</div>
                    </div>
                    <div className="px-2 py-1 rounded bg-blue-500/10 text-blue-600 dark:text-blue-400 text-xs font-medium">
                      {chunk.strategy}
                    </div>
                  </div>
                </div>

                {/* 切块内容 */}
                {expandedChunks.has(chunk.chunk_id) && (
                  <div className="mt-4 pt-4 border-t border-border/50">
                    <div className="prose prose-sm max-w-none dark:prose-invert">
                      <pre className="whitespace-pre-wrap break-words text-sm bg-muted/30 p-4 rounded-lg">
                        {chunk.content_text}
                      </pre>
                    </div>
                    {chunk.embedding && (
                      <div className="mt-3 text-xs text-muted-foreground">
                        Embedding维度: {chunk.embedding.length}
                      </div>
                    )}
                  </div>
                )}
              </div>
            </Card>
          ))
        )}
      </div>
    </div>
  );
}
