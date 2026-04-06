<template>
  <section class="site-page">
    <div class="page-hero">
      <p class="page-eyebrow">Terms Of Use</p>
      <h1>使用条款</h1>
      <p class="page-summary">
        为了保证本站的正常运行与良好使用体验，请在使用前阅读以下说明。
        继续访问或使用本站，视为你已知悉并接受这些基本约定。
      </p>
    </div>

    <div class="terms-list">
      <article class="term-card">
        <h2>1. 使用目的</h2>
        <p>
          本站用于提供音乐内容浏览、播放、搜索、互动与相关信息展示服务。
          请勿将本站用于违法、侵权、恶意攻击或影响系统正常运行的用途。
        </p>
      </article>

      <article class="term-card">
        <h2>2. 账号与行为</h2>
        <p>
          用户注册、评论、评分、收藏和歌单创建等行为应保持真实、文明与适度。
          禁止恶意刷分、批量灌水、冒用他人身份或破坏他人正常使用体验。
        </p>
      </article>

      <article class="term-card">
        <h2>3. 内容与资源</h2>
        <p>
          本站中的图片、音频、歌词和其他展示内容仅用于站内浏览、播放与功能体验。
          未经允许，请勿将站内内容用于与本站无关的传播、抓取或批量搬运。
        </p>
      </article>

      <article class="term-card">
        <h2>4. 数据与反馈</h2>
        <p>
          你提交的评分、收藏、评论、播放记录与反馈内容，可能被系统用于统计展示、
          个性化推荐、内容优化或问题排查。请避免在反馈中填写不必要的敏感隐私信息。
        </p>
      </article>

      <article class="term-card">
        <h2>5. 服务变更</h2>
        <p>
          为了修复问题、补充功能或优化展示效果，本站页面、功能与部分内容可能会不定期更新。
          若推荐结果、样式布局或展示内容发生变化，通常属于正常维护范围。
        </p>
      </article>

      <article class="term-card">
        <h2>6. 联系与建议</h2>
        <p>
          如果你在使用过程中发现异常、播放问题、页面显示错误或功能不符合预期，
          欢迎通过“反馈意见”页面提交建议，我们会尽快跟进处理。
        </p>
      </article>
    </div>
  </section>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "TermsOfUse",
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

.terms-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1rem;
}

.term-card {
  background: #fff;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 18px;
  padding: 1.25rem;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.04);
}

.term-card h2 {
  margin: 0 0 0.85rem;
  font-size: 1.1rem;
  color: #111827;
}

.term-card p {
  color: #5b6473;
  line-height: 1.85;
}
</style>
