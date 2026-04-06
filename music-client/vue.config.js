const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    client: {
      overlay: {
        runtimeErrors: false,  // 关闭运行时错误覆盖
        warnings: false,       // 可选：关闭警告覆盖
        errors: false         // 可选：关闭所有错误覆盖（慎用）
      }
    }
  },
  chainWebpack: config => {
    config.plugin('define').tap(definitions => {
      Object.assign(definitions[0]['process.env'], {
        // 使用环境变量，如果没有设置则使用默认值
        NODE_HOST: `"${process.env.VUE_APP_API_URL || 'http://localhost:8888'}"`,
        // src\api\request.ts文件中使用修改IP地址
      });
      return definitions;
    });
  }
}
)




