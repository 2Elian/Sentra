'use client';

import { useEffect, useState, useCallback } from 'react';
import ReactFlow, {
  Node,
  Edge,
  Background,
  Controls,
  MiniMap,
  useNodesState,
  useEdgesState,
  Position,
} from 'reactflow';
import 'reactflow/dist/style.css';

interface KnowledgeGraphViewProps {
  graphmlPath: string;
}

interface GraphMLNode {
  id: string;
  data: {
    d0: string; // entity_type
    d1: string; // description
    d2: string; // source_id
  };
}

interface GraphMLEdge {
  source: string;
  target: string;
  data: {
    d3: string; // source_id
    d4: string; // description
  };
}

export function KnowledgeGraphView({ graphmlPath }: KnowledgeGraphViewProps) {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // 根据实体类型获取节点颜色
  const getNodeColor = (entityType: string) => {
    const colorMap: Record<string, string> = {
      'ORGANIZATION': '#3b82f6', // blue
      'PERSON': '#10b981', // green
      'DATETERM': '#f59e0b', // amber
      'LOCATION': '#ef4444', // red
      'NONE': '#8b5cf6', // purple
    };
    return colorMap[entityType] || '#6b7280'; // gray
  };

  // 解析GraphML文件
  const parseGraphML = async (path: string) => {
    try {
      // 通过后端API访问文件
      // 使用URL参数传递文件路径，避免headers中的非ISO-8859-1字符问题
      // 将Windows路径的反斜杠转换为正斜杠，确保URL正确
      const normalizedPath = path.replace(/\\/g, '/');
      const response = await fetch(`/api/knowledge/v1/file?path=${encodeURIComponent(normalizedPath)}`, {
        method: 'GET',
      });

      if (!response.ok) {
        throw new Error('Failed to fetch GraphML file');
      }

      const xmlText = await response.text();
      const parser = new DOMParser();
      const xmlDoc = parser.parseFromString(xmlText, 'text/xml');

      const graphMLNodes: GraphMLNode[] = [];
      const graphMLEdges: GraphMLEdge[] = [];

      // 解析节点
      const nodeElements = xmlDoc.getElementsByTagName('node');
      for (let i = 0; i < nodeElements.length; i++) {
        const nodeEl = nodeElements[i];
        const nodeId = nodeEl.getAttribute('id');
        const dataElements = nodeEl.getElementsByTagName('data');

        let d0 = '', d1 = '', d2 = '';
        for (let j = 0; j < dataElements.length; j++) {
          const dataEl = dataElements[j];
          const key = dataEl.getAttribute('key');
          if (key === 'd0') d0 = dataEl.textContent || '';
          if (key === 'd1') d1 = dataEl.textContent || '';
          if (key === 'd2') d2 = dataEl.textContent || '';
        }

        if (nodeId) {
          graphMLNodes.push({ id: nodeId, data: { d0, d1, d2 } });
        }
      }

      // 解析边
      const edgeElements = xmlDoc.getElementsByTagName('edge');
      for (let i = 0; i < edgeElements.length; i++) {
        const edgeEl = edgeElements[i];
        const source = edgeEl.getAttribute('source');
        const target = edgeEl.getAttribute('target');
        const dataElements = edgeEl.getElementsByTagName('data');

        let d3 = '', d4 = '';
        for (let j = 0; j < dataElements.length; j++) {
          const dataEl = dataElements[j];
          const key = dataEl.getAttribute('key');
          if (key === 'd3') d3 = dataEl.textContent || '';
          if (key === 'd4') d4 = dataEl.textContent || '';
        }

        if (source && target) {
          graphMLEdges.push({ source, target, data: { d3, d4 } });
        }
      }

      // 转换为React Flow格式
      const flowNodes: Node[] = graphMLNodes.map((node) => {
        const color = getNodeColor(node.data.d0);

        return {
          id: node.id,
          position: { x: 0, y: 0 }, // 初始位置，将由布局算法计算
          data: {
            label: node.id,
            ...node.data,
            color,
          },
          style: {
            background: color,
            color: 'white',
            border: '2px solid #fff',
            borderRadius: '8px',
            padding: '10px 15px',
            fontSize: '12px',
            fontWeight: '500',
            boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
            transition: 'all 0.2s',
          },
          sourcePosition: Position.Right,
          targetPosition: Position.Left,
        };
      });

      const flowEdges: Edge[] = graphMLEdges.map((edge, index) => ({
        id: `e${index}`,
        source: edge.source,
        target: edge.target,
        data: edge.data,
        label: edge.data.d4,
        style: {
          stroke: '#94a3b8',
          strokeWidth: 2,
        },
        animated: true,
        markerEnd: {
          type: 'arrowclosed' as const,
          color: '#94a3b8',
        },
      }));

      // 简单的力导向布局模拟
      const layoutNodes = flowNodes.map((node, index) => {
        const angle = (index / flowNodes.length) * 2 * Math.PI;
        const radius = 300;
        return {
          ...node,
          position: {
            x: 400 + Math.cos(angle) * radius,
            y: 300 + Math.sin(angle) * radius,
          },
        };
      });

      setNodes(layoutNodes);
      setEdges(flowEdges);
    } catch (err: any) {
      console.error('Failed to parse GraphML:', err);
      setError(err.message || '解析GraphML文件失败');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (graphmlPath) {
      parseGraphML(graphmlPath);
    }
  }, [graphmlPath]);

  if (isLoading) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <div className="h-12 w-12 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          <p className="text-muted-foreground">加载知识图谱中...</p>
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
    <div className="h-full w-full bg-gradient-to-br from-slate-50 to-slate-100 dark:from-slate-900 dark:to-slate-800 rounded-lg">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        fitView
        className="bg-transparent"
      >
        <Background color="#cbd5e1" gap={16} />
        <Controls className="!bg-background !border-border" />
        <MiniMap
          className="!bg-background !border-border"
          nodeColor={(node) => (node.data as any).color}
          maskColor="rgba(0, 0, 0, 0.1)"
        />
      </ReactFlow>
    </div>
  );
}
