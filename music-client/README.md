# 音乐网站前端

## 环境配置说明

### 构建时配置API地址

可以通过以下方式配置API地址：

1. 使用环境变量：
   ```
   VUE_APP_API_URL=http://your-api-server:port
   ```

2. 构建命令：
   ```bash
   # 开发环境构建
   npm run serve
   
   # 生产环境构建
   VUE_APP_API_URL=http://your-production-api:port npm run build
   
   # 或者在Windows CMD中
   set VUE_APP_API_URL=http://your-production-api:port && npm run build
   ```

### 部署后修改API地址

构建完成后，如果需要修改API地址，可以直接编辑 `dist/index.html` 文件中的JavaScript部分：

```javascript
// 修改这里的地址即可
window.VUE_APP_API_URL = 'http://your-new-api-server:port';
```

这样就不需要重新构建整个项目，只需要修改这个配置值就可以切换API地址。