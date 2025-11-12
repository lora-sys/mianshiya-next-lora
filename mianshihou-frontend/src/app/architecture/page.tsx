'use client';

import React, { useCallback, useState } from 'react';
import ReactFlow, {
  MiniMap,
  Controls,
  Background,
  useNodesState,
  useEdgesState,
  addEdge,
  Connection,
  Edge,
  Node,
  ControlsProps,
  NodeTypes,
  Position
} from 'reactflow';
import 'reactflow/dist/style.css';

// 定义自定义节点类型
const CustomNode = ({ data }: { data: any }) => {
  const { label, type, description } = data;
  
  const nodeStyle = {
    padding: '15px',
    border: '2px solid #555',
    borderRadius: '10px',
    backgroundColor: type === 'frontend' ? '#e3f2fd' : 
                    type === 'backend' ? '#f3e5f5' : 
                    type === 'database' ? '#e8f5e9' : 
                    type === 'cache' ? '#fff3e0' : 
                    type === 'search' ? '#fce4ec' : 
                    type === 'infrastructure' ? '#e0f7fa' : '#f5f5f5',
    boxShadow: '0 4px 8px rgba(0,0,0,0.1)',
    minWidth: '180px',
    textAlign: 'center' as const,
    fontWeight: 'bold',
    fontSize: '14px'
  };

  return (
    <div style={nodeStyle}>
      <div style={{ fontSize: '16px', marginBottom: '5px' }}>{label}</div>
      {description && (
        <div style={{ fontSize: '12px', color: '#666', marginTop: '5px' }}>
          {description}
        </div>
      )}
    </div>
  );
};

const nodeTypes: NodeTypes = {
  custom: CustomNode,
};

// 定义节点和边
const initialNodes: Node[] = [
  {
    id: '1',
    type: 'custom',
    position: { x: 0, y: 100 },
    data: { 
      label: '客户端层', 
      type: 'frontend',
      description: 'Web浏览器 / 移动应用'
    }
  },
  {
    id: '2',
    type: 'custom',
    position: { x: 250, y: 100 },
    data: { 
      label: '前端层', 
      type: 'frontend',
      description: 'Next.js + React + Ant Design'
    }
  },
  {
    id: '3',
    type: 'custom',
    position: { x: 500, y: 100 },
    data: { 
      label: 'API网关', 
      type: 'infrastructure',
      description: '反向代理 / 负载均衡'
    }
  },
  {
    id: '4',
    type: 'custom',
    position: { x: 500, y: 250 },
    data: { 
      label: '后端服务', 
      type: 'backend',
      description: 'Spring Boot APIs'
    }
  },
  {
    id: '5',
    type: 'custom',
    position: { x: 250, y: 250 },
    data: { 
      label: '认证服务', 
      type: 'backend',
      description: 'Sa-Token 权限认证'
    }
  },
  {
    id: '6',
    type: 'custom',
    position: { x: 250, y: 350 },
    data: { 
      label: '题库管理', 
      type: 'backend',
      description: '题目管理服务'
    }
  },
  {
    id: '7',
    type: 'custom',
    position: { x: 0, y: 250 },
    data: { 
      label: '用户管理', 
      type: 'backend',
      description: '用户服务'
    }
  },
  {
    id: '8',
    type: 'custom',
    position: { x: 0, y: 350 },
    data: { 
      label: '搜索服务', 
      type: 'backend',
      description: 'Elasticsearch'
    }
  },
  {
    id: '9',
    type: 'custom',
    position: { x: 750, y: 250 },
    data: { 
      label: '数据层', 
      type: 'database',
      description: 'MySQL 数据库'
    }
  },
  {
    id: '10',
    type: 'custom',
    position: { x: 750, y: 100 },
    data: { 
      label: '缓存层', 
      type: 'cache',
      description: 'Redis + Caffeine'
    }
  },
  {
    id: '11',
    type: 'custom',
    position: { x: 750, y: 350 },
    data: { 
      label: '搜索引擎', 
      type: 'search',
      description: 'Elasticsearch'
    }
  },
  {
    id: '12',
    type: 'custom',
    position: { x: 1000, y: 250 },
    data: { 
      label: '基础设施', 
      type: 'infrastructure',
      description: 'Nacos, Sentinel, Hotkey'
    }
  }
];

const initialEdges: Edge[] = [
  { id: 'e1-2', source: '1', target: '2', animated: true },
  { id: 'e2-3', source: '2', target: '3', animated: true },
  { id: 'e3-4', source: '3', target: '4', animated: true },
  { id: 'e4-5', source: '4', target: '5' },
  { id: 'e4-6', source: '4', target: '6' },
  { id: 'e4-7', source: '4', target: '7' },
  { id: 'e4-8', source: '4', target: '8' },
  { id: 'e5-9', source: '5', target: '9' },
  { id: 'e6-9', source: '6', target: '9' },
  { id: 'e7-9', source: '7', target: '9' },
  { id: 'e8-11', source: '8', target: '11' },
  { id: 'e4-10', source: '4', target: '10' },
  { id: 'e9-12', source: '9', target: '12' },
  { id: 'e10-12', source: '10', target: '12' },
  { id: 'e11-12', source: '11', target: '12' },
];

const ArchitecturePage = () => {
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);

  const onConnect = useCallback(
    (params: Connection) => setEdges((eds) => addEdge(params, eds)),
    [setEdges]
  );

  const controlsStyle: React.CSSProperties = {
    position: 'absolute',
    right: 10,
    bottom: 10,
    backgroundColor: 'rgba(255, 255, 255, 0.8)',
    borderRadius: '8px',
    padding: '8px',
  };

  return (
    <div style={{ width: '100vw', height: '100vh' }}>
      <div className="architecture-page">
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          nodeTypes={nodeTypes}
          fitView
          attributionPosition="bottom-left"
        >
        <Controls style={controlsStyle} />
        <MiniMap 
          nodeColor={(node) => {
            if (node.type === 'frontend') return '#e3f2fd';
            if (node.type === 'backend') return '#f3e5f5';
            if (node.type === 'database') return '#e8f5e9';
            if (node.type === 'cache') return '#fff3e0';
            if (node.type === 'search') return '#fce4ec';
            if (node.type === 'infrastructure') return '#e0f7fa';
            return '#f5f5f5';
          }}
        />
        <Background color="#aaa" gap={16} />
      </ReactFlow>
      </div>
    </div>
  );
};

export default ArchitecturePage;