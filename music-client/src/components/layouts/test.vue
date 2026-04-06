

<template></template>

  <title>现代化页脚设计</title>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
  


  <div class="content">
    <h1>现代化页脚设计示例</h1>
    <p>此页面展示了修改后的页脚效果，所有链接都已更新为跳转到百度首页。您可以向下滚动查看页脚部分。</p>
  </div>

  

  <script>

  </script>


</template>


<style>
    
  </style>

//------------------------------------------------------------------------------------------------------------------------------------------------
  <template>
  <div class="footer">
    <p> </p>
    <p v-for="(item, index) in footerList" :key="index" v-html="item"></p>
  </div>
</template>

<script lang="ts">
import { defineComponent, readonly } from "vue";

export default defineComponent({
  setup() {
    const footerList = readonly([
      '<a href="/about">关于</a> | <a href="/help">帮助</a> | <a href="/terms">条款</a> | <a href="/feedback">反馈</a>', // [!code ++]
      "Modification © 2025 Actording",
      "Copyright © 2019 Yin-Hongwei",
    ]);

    return { footerList };
  },
});
</script>

<style lang="scss" scoped>
@import "@/assets/css/var.scss";
@import "@/assets/css/global.scss";

.footer {
  width: 100%;
  height: $footer-height;
  padding: 20px 0;
  box-sizing: border-box;
  font-size: 14px;
  @include layout(center, center, column);
}

p {
  height: 30px;

  a {
    color: inherit;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }
}
</style>

