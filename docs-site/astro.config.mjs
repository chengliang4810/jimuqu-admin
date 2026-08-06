import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

export default defineConfig({
  site: 'https://doc.jimuqu.com',
  integrations: [
    starlight({
      title: 'Jimuqu Admin',
      description: '基于 Solon 的轻量级企业管理后台开发文档',
      logo: {
        src: './src/assets/logo.svg',
        replacesTitle: true,
      },
      favicon: '/favicon.svg',
      defaultLocale: 'root',
      locales: {
        root: { label: '简体中文', lang: 'zh-CN' },
      },
      social: [
        { icon: 'github', label: 'GitHub', href: 'https://github.com/chengliang4810/jimuqu-admin' },
        { icon: 'code-branch', label: 'Gitee', href: 'https://gitee.com/chengliang4810/jimuqu-admin' },
      ],
      editLink: {
        baseUrl: 'https://github.com/chengliang4810/jimuqu-admin/edit/main/docs-site/',
      },
      lastUpdated: true,
      customCss: ['./src/styles/custom.css'],
      sidebar: [
        {
          label: '开始使用',
          items: [
            { label: '项目介绍', slug: 'guide/introduction' },
            { label: '快速开始', slug: 'guide/getting-started' },
            { label: '配置说明', slug: 'guide/configuration' },
            { label: '生产部署', slug: 'guide/deployment' },
            { label: '前后端联调', slug: 'guide/frontend-integration' },
          ],
        },
        {
          label: '开发指南',
          items: [
            { label: '项目结构', slug: 'development/architecture' },
            { label: 'CRUD 开发', slug: 'development/crud' },
            { label: 'Xbatis 查询', slug: 'development/xbatis' },
            { label: '数据权限', slug: 'development/data-permission' },
          ],
        },
        {
          label: '系统功能',
          items: [
            { label: '功能总览', slug: 'system/overview' },
            { label: '认证与客户端', slug: 'system/auth-client' },
            { label: '用户与组织', slug: 'system/users-organization' },
            { label: '角色与菜单', slug: 'system/roles-menus' },
            { label: '字典与参数', slug: 'system/dict-config' },
            { label: '公告与消息', slug: 'system/notices-messages' },
            { label: '日志与在线用户', slug: 'system/audit-online' },
          ],
        },
        {
          label: '基础能力',
          items: [
            { label: '模块总览', slug: 'modules/overview' },
            { label: '数据访问与建表', slug: 'modules/data-access' },
            { label: '缓存与 Redis', slug: 'modules/cache-redis' },
            { label: '安全与 Web', slug: 'modules/security-web' },
            { label: '文件存储', slug: 'modules/file-storage' },
            { label: 'Excel 导入导出', slug: 'modules/excel' },
            { label: '接口限流', slug: 'modules/rate-limit' },
            { label: '幂等与操作日志', slug: 'modules/idempotent-log' },
            { label: '字段翻译', slug: 'modules/translation' },
            { label: 'SSE 与 WebSocket', slug: 'modules/realtime-messaging' },
            { label: '短信与邮件', slug: 'modules/sms-mail' },
            { label: '第三方登录', slug: 'modules/social-auth' },
            { label: '定时任务', slug: 'modules/scheduled-jobs' },
            { label: '接口文档状态', slug: 'modules/api-docs' },
          ],
        },
        {
          label: '深入参考',
          items: [
            { label: '完整 CRUD 示例', slug: 'reference/complete-crud' },
            { label: '完整 Xbatis 手册', slug: 'reference/complete-xbatis' },
            { label: '数据权限使用示例', slug: 'reference/data-permission-guide' },
            { label: '数据权限测试指南', slug: 'reference/data-permission-testing' },
            { label: '文件上传示例', slug: 'reference/file-upload-examples' },
            { label: '翻译模块完整指南', slug: 'reference/translation-guide' },
            { label: '限流模块完整指南', slug: 'reference/rate-limit-guide' },
            { label: '定时任务完整说明', slug: 'reference/scheduled-jobs-complete' },
          ],
        },
      ],
    }),
  ],
});
