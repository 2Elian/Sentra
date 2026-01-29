/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  swcMinify: true,

  // 注释掉静态导出配置，使用开发模式
  // output: 'export',
  // 禁用图片优化（静态导出不支持）
  // images: {
  //   unoptimized: true,
  // },

  async rewrites() {
    return [
      {
        source: '/api/user/v1/:path*',
        destination: 'http://localhost:8081/v1/:path*',
      },
      {
        source: '/api/knowledge/v1/:path*',
        destination: 'http://localhost:8082/v1/:path*',
      },
      {
        source: '/api/python/:path*',
        destination: 'http://localhost:8000/api/:path*',
      },
    ];
  },

  // 配置环境变量
  env: {
    NEXT_PUBLIC_USER_API_URL: process.env.NEXT_PUBLIC_USER_API_URL || 'http://localhost:8081',
    NEXT_PUBLIC_KNOWLEDGE_API_URL: process.env.NEXT_PUBLIC_KNOWLEDGE_API_URL || 'http://localhost:8082',
    NEXT_PUBLIC_PYTHON_API_URL: process.env.NEXT_PUBLIC_PYTHON_API_URL || 'http://localhost:8000',
  },
};

export default nextConfig;
