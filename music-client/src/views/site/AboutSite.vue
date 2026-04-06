<template>
  <section class="site-page">
    <div class="page-hero">
      <p class="page-eyebrow">About This Site</p>
      <h1>关于本站</h1>
      <p class="page-summary">
        本站提供歌曲浏览、在线播放、搜索发现、歌单查看、评论互动与个人内容管理等功能，
        希望帮助用户更方便地发现、整理和收听自己感兴趣的音乐内容。
      </p>
    </div>

    <div class="page-grid">
      <article class="page-card">
        <h2>系统定位</h2>
        <p>
          本站是一个集歌曲浏览、歌单展示、用户互动与内容管理于一体的音乐网站，
          支持用户在同一平台中完成内容发现、播放收听和个人偏好整理等常见操作。
        </p>
      </article>

      <article class="page-card">
        <h2>核心功能</h2>
        <ul>
          <li>浏览歌曲、歌单、歌手与详情信息</li>
          <li>支持播放、收藏、评论、评分和个人歌单管理</li>
          <li>支持关键词搜索、歌词查看与最近播放记录展示</li>
          <li>首页可查看热门内容与个性化推荐内容</li>
        </ul>
      </article>

      <article class="page-card">
        <h2>内容发现</h2>
        <p>
          首页会结合站内热门内容与用户使用记录展示推荐结果。你可以通过搜索、歌手页、
          歌单页和推荐模块，逐步找到自己喜欢的歌曲与相关内容。
        </p>
      </article>

      <article class="page-card">
        <h2>反馈与建议</h2>
        <p>
          如果你在使用过程中遇到页面异常、播放问题或内容显示不完整等情况，
          可以通过站内反馈入口提交建议，我们会持续优化使用体验。
        </p>
      </article>
    </div>
  </section>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "AboutSite",
});
</script>

<style lang="scss" scoped>
.site-page {
  max-width: 1120px;
  margin: 0 auto;
  padding: 2rem 1.25rem 3rem;
}

.page-hero {
  margin-bottom: 1.75rem;
}

.page-eyebrow {
  margin: 0 0 0.5rem;
  color: #66d9ef;
  font-size: 0.9rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.page-hero h1 {
  margin: 0 0 1rem;
  font-size: 2.25rem;
  color: #1f2937;
}

.page-summary {
  max-width: 880px;
  color: #5b6473;
  line-height: 1.9;
}

.page-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 1rem;
}

.page-card {
  background: #fff;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 18px;
  padding: 1.25rem;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.04);
}

.page-card h2 {
  margin: 0 0 0.85rem;
  font-size: 1.1rem;
  color: #111827;
}

.page-card p,
.page-card li {
  color: #5b6473;
  line-height: 1.85;
}

.page-card ul {
  margin: 0;
  padding-left: 1.1rem;
}
</style>
