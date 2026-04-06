<template>
  <section class="site-page">
    <div class="page-hero">
      <p class="page-eyebrow">Resource Source</p>
      <h1>资源来源</h1>
      <p class="page-summary">
        本站展示的图片、音频、歌词与部分推荐结果，主要来自系统维护的本地资源库、
        业务数据以及用户在站内产生的互动记录。不同类型的内容，其整理和展示方式可能有所不同。
      </p>
    </div>

    <div class="resource-list">
      <article class="resource-card">
        <h2>本地媒体资源</h2>
        <p>
          站内歌曲封面、歌单封面、歌手图片、歌词文件与部分音频内容，
          主要来自系统本地资源目录，并由站点统一整理后用于页面展示与播放。
        </p>
      </article>

      <article class="resource-card">
        <h2>用户互动内容</h2>
        <p>
          收藏、评论、评分、播放历史和个人歌单等内容，来自用户在站内的实际操作记录。
          页面中的部分热门内容与推荐结果也会随着这些数据变化而更新。
        </p>
      </article>

      <article class="resource-card">
        <h2>推荐与排序结果</h2>
        <p>
          热门内容通常依据播放、收藏、评论、评分等指标进行综合排序；
          推荐内容则会结合用户历史行为动态生成，因此不同用户看到的结果可能并不完全一致。
        </p>
      </article>

      <article class="resource-card">
        <h2>使用提醒</h2>
        <p>
          若页面中的资源存在失效、缺失或显示异常的情况，欢迎通过反馈入口告知。
          我们会根据实际情况进行补充、修复或调整。
        </p>
      </article>
    </div>
  </section>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "ResourceSource",
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

.resource-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 1rem;
}

.resource-card {
  background: #fff;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 18px;
  padding: 1.25rem;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.04);
}

.resource-card h2 {
  margin: 0 0 0.85rem;
  font-size: 1.1rem;
  color: #111827;
}

.resource-card p {
  color: #5b6473;
  line-height: 1.85;
}
</style>
