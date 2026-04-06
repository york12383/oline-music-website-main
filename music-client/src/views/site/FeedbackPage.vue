<template>
  <section class="feedback-page">
    <div class="page-hero">
      <p class="page-eyebrow">Feedback</p>
      <h1>反馈意见</h1>
      <p class="page-summary">
        如果你在使用过程中遇到功能异常、页面显示问题，或者想给推荐逻辑、交互体验提建议，都可以在这里提交。
        不登录也能反馈；登录后，我们会自动关联你的账号，方便后续排查。
      </p>
    </div>

    <div class="feedback-layout">
      <el-card class="feedback-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>提交反馈</span>
            <span class="header-tip">{{ submitTip }}</span>
          </div>
        </template>

        <el-form ref="feedbackFormRef" :model="feedbackForm" :rules="rules" label-position="top">
          <el-form-item label="反馈类型" prop="feedbackType">
            <el-select v-model="feedbackForm.feedbackType" placeholder="请选择反馈类型">
              <el-option label="功能问题" value="bug" />
              <el-option label="体验建议" value="suggestion" />
              <el-option label="内容纠错" value="content" />
              <el-option label="其他" value="other" />
            </el-select>
          </el-form-item>

          <el-form-item label="反馈标题" prop="title">
            <el-input v-model="feedbackForm.title" maxlength="80" show-word-limit placeholder="例如：热门歌曲查看全部为空" />
          </el-form-item>

          <el-form-item label="反馈内容" prop="content">
            <el-input
              v-model="feedbackForm.content"
              type="textarea"
              :rows="6"
              maxlength="600"
              show-word-limit
              placeholder="请尽量描述清楚出现问题的页面、操作步骤、预期结果和实际结果。"
            />
          </el-form-item>

          <el-form-item label="联系方式（可选）" prop="contact">
            <el-input v-model="feedbackForm.contact" maxlength="120" placeholder="可填写邮箱、QQ 或其他联系方式" />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="submitFeedback">提交反馈</el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card class="feedback-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>我的最近反馈</span>
            <span class="header-tip">{{ recentFeedbackTip }}</span>
          </div>
        </template>

        <template v-if="feedbackList.length">
          <div v-for="item in feedbackList" :key="item.id" class="feedback-item">
            <div class="feedback-item-head">
              <strong>{{ item.title }}</strong>
              <span class="feedback-tag">{{ feedbackTypeText(item.feedbackType) }}</span>
            </div>
            <p class="feedback-content">{{ item.content }}</p>
            <div class="feedback-meta">
              <span>提交时间：{{ formatDate(item.createTime) }}</span>
              <span>状态：{{ item.status === 1 ? "已处理" : "待处理" }}</span>
            </div>
          </div>
        </template>

        <el-empty v-else :description="emptyDescription" />
      </el-card>
    </div>
  </section>
</template>

<script lang="ts" setup>
import { computed, getCurrentInstance, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { HttpManager } from "@/api";

const { proxy } = getCurrentInstance() as any;
const feedbackFormRef = ref();
const submitting = ref(false);
const feedbackList = ref<any[]>([]);

const feedbackForm = reactive({
  feedbackType: "bug",
  title: "",
  content: "",
  contact: "",
});

const rules = {
  feedbackType: [{ required: true, message: "请选择反馈类型", trigger: "change" }],
  title: [{ required: true, message: "请输入反馈标题", trigger: "blur" }],
  content: [{ required: true, message: "请输入反馈内容", trigger: "blur" }],
};

const isLoggedIn = computed(() => Boolean(proxy?.$store?.getters?.userId));

const submitTip = computed(() =>
  isLoggedIn.value ? `当前将关联账号：${proxy.$store.getters.username || "已登录用户"}` : "当前将以匿名方式提交"
);

const recentFeedbackTip = computed(() =>
  isLoggedIn.value ? "展示最近 10 条提交记录" : "登录后可查看自己的最近反馈记录"
);

const emptyDescription = computed(() =>
  isLoggedIn.value ? "你还没有提交过反馈" : "登录后可查看自己的反馈记录"
);

function feedbackTypeText(type: string) {
  const typeMap: Record<string, string> = {
    bug: "功能问题",
    suggestion: "体验建议",
    content: "内容纠错",
    other: "其他",
  };
  return typeMap[type] || "其他";
}

function formatDate(value: string | number | Date) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  const hour = `${date.getHours()}`.padStart(2, "0");
  const minute = `${date.getMinutes()}`.padStart(2, "0");
  return `${year}-${month}-${day} ${hour}:${minute}`;
}

function resetForm() {
  feedbackForm.feedbackType = "bug";
  feedbackForm.title = "";
  feedbackForm.content = "";
  feedbackForm.contact = "";
  feedbackFormRef.value?.clearValidate?.();
}

async function loadMyFeedback() {
  if (!isLoggedIn.value) {
    feedbackList.value = [];
    return;
  }
  try {
    const result = (await HttpManager.getMyFeedback()) as ResponseBody;
    feedbackList.value = Array.isArray(result?.data) ? result.data : [];
  } catch (error) {
    feedbackList.value = [];
  }
}

async function submitFeedback() {
  if (!feedbackFormRef.value) return;

  let valid = false;
  await feedbackFormRef.value.validate((result: boolean) => {
    valid = result;
  });
  if (!valid) return;

  submitting.value = true;
  try {
    const result = (await HttpManager.addFeedback({
      feedbackType: feedbackForm.feedbackType,
      title: feedbackForm.title.trim(),
      content: feedbackForm.content.trim(),
      contact: feedbackForm.contact.trim(),
      pagePath: window.location.pathname,
    })) as ResponseBody;

    ElMessage({
      message: result?.message || "反馈已提交",
      type: (result?.type as any) || "success",
    });

    if (result?.success) {
      resetForm();
      await loadMyFeedback();
    }
  } catch (error) {
    ElMessage.error("反馈提交失败，请稍后重试");
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  loadMyFeedback();
});
</script>

<style lang="scss" scoped>
.feedback-page {
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

.feedback-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr);
  gap: 1rem;
}

.feedback-card {
  border-radius: 20px;
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.card-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.header-tip {
  font-size: 0.9rem;
  color: #7b8794;
}

.feedback-item + .feedback-item {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid rgba(148, 163, 184, 0.18);
}

.feedback-item-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.feedback-tag {
  background: rgba(102, 217, 239, 0.12);
  color: #0f766e;
  border-radius: 999px;
  padding: 0.2rem 0.65rem;
  font-size: 0.85rem;
}

.feedback-content {
  margin: 0.75rem 0;
  color: #4b5563;
  line-height: 1.75;
  white-space: pre-wrap;
}

.feedback-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  color: #8a94a6;
  font-size: 0.9rem;
}

@media (max-width: 960px) {
  .feedback-layout {
    grid-template-columns: 1fr;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
