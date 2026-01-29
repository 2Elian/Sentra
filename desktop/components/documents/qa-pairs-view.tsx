'use client';

import { useEffect, useState } from 'react';
import { Card } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Badge } from '@/components/ui/badge';

interface QAPairsViewProps {
  aggregatedPath?: string;
  multiHopPath?: string;
  cotPath?: string;
}

interface QAPair {
  messages: Array<{
    role: 'user' | 'assistant';
    content: string;
  }>;
}

type QAType = 'aggregated' | 'multiHop' | 'cot';

const QATypeLabels: Record<QAType, string> = {
  aggregated: '聚合类',
  multiHop: '多跳类',
  cot: '推理类',
};

const QATypeColors: Record<QAType, string> = {
  aggregated: 'bg-blue-500/10 text-blue-600 dark:text-blue-400',
  multiHop: 'bg-green-500/10 text-green-600 dark:text-green-400',
  cot: 'bg-purple-500/10 text-purple-600 dark:text-purple-400',
};

export function QAPairsView({
  aggregatedPath,
  multiHopPath,
  cotPath,
}: QAPairsViewProps) {
  const [qaData, setQaData] = useState<Record<QAType, QAPair[]>>({
    aggregated: [],
    multiHop: [],
    cot: [],
  });
  const [isLoading, setIsLoading] = useState(true);
  const [errors, setErrors] = useState<Record<QAType, string | null>>({
    aggregated: null,
    multiHop: null,
    cot: null,
  });

  useEffect(() => {
    const fetchQAData = async () => {
      const results: Record<QAType, { data: QAPair[]; error: string | null }> = {
        aggregated: { data: [], error: null },
        multiHop: { data: [], error: null },
        cot: { data: [], error: null },
      };

      const promises: Promise<void>[] = [];

      const fetchFile = async (type: QAType, path?: string) => {
        if (!path) return;

        try {
          // 通过后端API访问文件，使用URL参数避免headers编码问题
          // 将Windows路径的反斜杠转换为正斜杠，确保URL正确
          const normalizedPath = path.replace(/\\/g, '/');
          const response = await fetch(`/api/knowledge/v1/file?path=${encodeURIComponent(normalizedPath)}`, {
            method: 'GET',
          });

          if (!response.ok) {
            throw new Error('Failed to fetch QA pairs file');
          }

          const data: QAPair[] = await response.json();
          results[type].data = data;
        } catch (err: any) {
          console.error(`Failed to fetch ${type} QA pairs:`, err);
          results[type].error = err.message || '加载失败';
        }
      };

      promises.push(fetchFile('aggregated', aggregatedPath));
      promises.push(fetchFile('multiHop', multiHopPath));
      promises.push(fetchFile('cot', cotPath));

      await Promise.all(promises);

      setQaData({
        aggregated: results.aggregated.data,
        multiHop: results.multiHop.data,
        cot: results.cot.data,
      });

      setErrors({
        aggregated: results.aggregated.error,
        multiHop: results.multiHop.error,
        cot: results.cot.error,
      });

      setIsLoading(false);
    };

    fetchQAData();
  }, [aggregatedPath, multiHopPath, cotPath]);

  if (isLoading) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <div className="h-12 w-12 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          <p className="text-muted-foreground">加载问答对中...</p>
        </div>
      </div>
    );
  }

  const availableTypes = Object.entries(qaData)
    .filter(([_, data]) => data.length > 0)
    .map(([type]) => type as QAType);

  if (availableTypes.length === 0) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="text-center space-y-3">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-muted/50 mx-auto">
            <svg className="h-8 w-8 text-muted-foreground/50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <p className="text-muted-foreground">暂无问答对数据</p>
        </div>
      </div>
    );
  }

  return (
    <Tabs defaultValue={availableTypes[0]} className="h-full flex flex-col">
      <TabsList className="grid w-full grid-cols-3">
        {availableTypes.map((type) => (
          <TabsTrigger key={type} value={type}>
            {QATypeLabels[type]} ({qaData[type].length})
          </TabsTrigger>
        ))}
      </TabsList>

      {availableTypes.map((type) => (
        <TabsContent key={type} value={type} className="flex-1 overflow-y-auto mt-4">
          <div className="space-y-4">
            {qaData[type].map((qa, index) => (
              <Card
                key={index}
                className="hover:shadow-md transition-shadow duration-200"
              >
                <div className="p-4 space-y-4">
                  {/* 问题 */}
                  <div className="flex gap-3">
                    <div className="flex-shrink-0 w-8 h-8 rounded-full bg-primary flex items-center justify-center">
                      <svg className="w-4 h-4 text-primary-foreground" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
                      </svg>
                    </div>
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        <Badge variant="outline" className="text-xs">
                          Question
                        </Badge>
                        <Badge className={QATypeColors[type]}>
                          {QATypeLabels[type]}
                        </Badge>
                      </div>
                      <p className="text-sm font-medium">{qa.messages[0].content}</p>
                    </div>
                  </div>

                  {/* 答案 */}
                  <div className="flex gap-3">
                    <div className="flex-shrink-0 w-8 h-8 rounded-full bg-green-500 flex items-center justify-center">
                      <svg className="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                    </div>
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        <Badge variant="outline" className="text-xs">
                          Answer
                        </Badge>
                      </div>
                      <p className="text-sm text-muted-foreground leading-relaxed">
                        {qa.messages[1].content}
                      </p>
                    </div>
                  </div>
                </div>
              </Card>
            ))}
          </div>
        </TabsContent>
      ))}
    </Tabs>
  );
}
