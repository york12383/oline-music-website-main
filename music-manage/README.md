# music-manage

## Project setup
```
npm install
```

### Compiles and hot-reloads for development
```
npm run serve
```

### Compiles and minifies for production
```
npm run build
```

### Lints and fixes files
```
npm run lint
```

### Customize configuration
See [Configuration Reference](https://cli.vuejs.org/config/).


### 部署后修改API地址

构建完成后，如果需要修改API地址，可以直接编辑 `dist/index.html` 文件中的JavaScript部分：
window.VUE_APP_API_URL = 'http://后端的IP地址:端口';