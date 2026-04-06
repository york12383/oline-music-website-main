<template>
    <div class="music-page-shell my-song-sheet-page">
        <section class="hero-card music-panel">
            <span class="music-chip"><span class="music-dot"></span> My Playlist</span>
            <div class="hero-head">
                <div>
                    <h1>我的歌单管理</h1>
                    <p>把喜欢的旋律轻轻收进每一张歌单里，让心动、回忆与此刻的情绪，都能在这里慢慢被收藏。</p>
                </div>
                <el-button 
                    type="primary" 
                    size="small" 
                    :icon="Plus"
                    @click="handleCreate"
                >
                    创建新歌单
                </el-button>
            </div>
        </section>

        <section class="content-card music-panel">
        <div class="user-playlist-header">
            <h2>我的歌单管理 ({{ allPlayList.length }} 个)</h2>
        </div>
        
        <!-- 添加加载状态 -->
        <div v-if="loading" class="loading">加载中...</div>
        
        <!-- 未登录提示 -->
        <div v-else-if="!isLoggedIn" class="login-prompt">
            <el-empty description="请先登录查看您的歌单">
                <el-button type="primary" @click="goToLogin">立即登录</el-button>
            </el-empty>
        </div>
        
        <!-- 视图内容 -->
        <div v-else>
            <div class="view-toggle">
                <el-radio-group v-model="viewMode" size="small">
                    <el-radio-button label="table">表格视图</el-radio-button>
                    <el-radio-button label="grid">网格视图</el-radio-button>
                </el-radio-group>
            </div>

            <div v-if="showAsGrid" class="play-list-view">
                <play-list 
                    :playList="allPlayList" 
                    path="my-song-sheet-detail"
                    :title="`我的歌单 (${allPlayList.length} 个)`"
                ></play-list>
            </div>

            <template v-else>
                <el-table 
                    :data="tableData" 
                    stripe 
                    style="width: 100%"
                    empty-text="您还没有创建任何歌单"
                >
                    <el-table-column label="封面" width="100">
                        <template #default="scope">
                            <el-image 
                                :src="resolveSongListCover(scope.row)" 
                                class="sheet-cover-thumb"
                                fit="cover"
                                @click="goToSongListDetail(scope.row)"
                            />
                        </template>
                    </el-table-column>
                    
                    <el-table-column prop="title" label="标题" min-width="180">
                        <template #default="scope">
                            <span class="song-list-title" @click="goToSongListDetail(scope.row)">
                                {{ scope.row.title }}
                            </span>
                        </template>
                    </el-table-column>
                    
                    <el-table-column prop="style" label="风格" width="100"></el-table-column>
                    
                    <el-table-column label="状态" width="100">
                        <template #default="scope">
                            <el-tag :type="mapStatus(scope.row.type).type" effect="dark">
                                {{ mapStatus(scope.row.type).label }}
                            </el-tag>
                        </template>
                    </el-table-column>
          
                    <el-table-column label="操作" width="200" align="center">
                        <template #default="scope">
                            <el-button 
                                :icon="View" 
                                size="small" 
                                @click="goToSongListDetail(scope.row)"
                                text
                            >
                                查看
                            </el-button>
                            <el-button 
                                :icon="Edit" 
                                size="small" 
                                @click="handleEdit(scope.row)"
                                :disabled="scope.row.type !== 1"
                                text
                            >
                                修改
                            </el-button>
                            <el-button 
                                :icon="Delete" 
                                size="small" 
                                type="danger" 
                                @click="handleDelete(scope.row.id)"
                                text
                            >
                                删除
                            </el-button>
                        </template>
                    </el-table-column>
                </el-table>
          
                <el-pagination
                    v-if="allPlayList.length > pageSize"
                    class="pagination"
                    background
                    layout="total, prev, pager, next"
                    :current-page="currentPage"
                    :page-size="pageSize"
                    :total="allPlayList.length"
                    @current-change="handleCurrentChange"
                >
                </el-pagination>
            </template>
        </div>
        </section>
  
        <!-- 修改歌单弹窗 -->
        <el-dialog 
            :title="isCreating ? '创建新歌单' : '修改歌单信息'" 
            v-model="dialogVisible" 
            width="600px"
            @close="handleDialogClose"
        >
            <el-form :model="form" :rules="formRules" ref="formRef" label-width="80px">
                <el-form-item v-if="!isCreating" label="歌单ID">
                    <el-input v-model="form.id" disabled></el-input>
                </el-form-item>
                <el-form-item label="标题" prop="title">
                    <el-input v-model="form.title" placeholder="请输入歌单标题"></el-input>
                </el-form-item>
                <el-form-item label="介绍" prop="introduction">
                    <el-input 
                        type="textarea" 
                        :rows="3" 
                        v-model="form.introduction" 
                        placeholder="请输入歌单介绍"
                    ></el-input>
                </el-form-item>
                <el-form-item label="风格" prop="style">
                    <el-input 
                        v-model="form.style" 
                        placeholder="请输入歌单风格，如：流行,摇滚"
                    ></el-input>
                </el-form-item>
                
                <el-form-item label="封面">
                    <div class="upload-container">
                        <el-upload
                            class="avatar-uploader"
                            action="#"
                            :show-file-list="false"
                            :before-upload="beforeAvatarUpload"
                            :http-request="handleAvatarUpload"
                            :disabled="uploading"
                        >
                            <el-image 
                                v-if="!uploading" 
                                :src="resolveSongListCover(form)" 
                                style="width: 120px; height: 120px; border-radius: 4px;" 
                                fit="cover"
                            />
                            <div v-else class="uploading">
                                <el-icon class="is-loading"><Loading /></el-icon>
                                <div>上传中...</div>
                            </div>
                        </el-upload>
                        <div class="upload-info">
                            <div class="upload-tip">优先使用后端已有的歌单封面，也支持你手动上传新的歌单图。</div>
                            <div class="upload-tip">建议尺寸：300x300px，支持 JPG、PNG 格式</div>
                            <el-button 
                                v-if="!isCreating" 
                                type="text" 
                                size="small" 
                                @click="handleResetPic"
                            >
                                重置为后端默认封面
                            </el-button>
                        </div>
                    </div>
                </el-form-item>
            </el-form>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="dialogVisible = false" :disabled="uploading">取 消</el-button>
                    <el-button 
                        type="primary" 
                        @click="submitForm"
                        :loading="submitting"
                        :disabled="uploading"
                    >
                        {{ isCreating ? '创 建' : '保 存' }}
                    </el-button>
                </span>
            </template>
        </el-dialog>
        
    </div>
</template>
  
<script lang="ts">
import { defineComponent, ref, computed, onMounted, reactive, watch } from "vue";
import { useStore } from "vuex";
import { useRoute, useRouter } from "vue-router";
import { HttpManager } from "@/api";
import { ElMessage, ElMessageBox, type FormRules, type UploadProps } from "element-plus";
import { Edit, Delete, Plus, View, Loading } from "@element-plus/icons-vue";
import { getSongListCoverUrl } from "@/utils/song-list-cover";
import type { UploadRequestHandler } from 'element-plus';

// 导入 PlayList 组件
import PlayList from "@/components/PlayList.vue";

// 响应数据类型定义
interface SongListItem {
    id: number;
    title: string;
    pic: string;
    style: string;
    introduction: string;
    type: number;
    consumer: number;
}

interface ResponseBody {
    code: number;
    message: string;
    type: string;
    success: boolean;
    data: any;
}

interface SongListStatus {
    label: string;
    type: 'success' | 'warning' | 'danger' | 'info';
}

interface SongListForm {
    id?: number | null;
    title: string;
    introduction: string;
    style: string;
    pic?: string;
    type?: number;
    consumer?: number;
}

export default defineComponent({
    name: 'UserPlaylist',
    components: {
        PlayList
    },
    setup() {
        const store = useStore();
        const route = useRoute();
        const router = useRouter();
        const userId = computed(() => store.getters.userId); 
        const isLoggedIn = computed(() => !!userId.value);
        
        const formRef = ref();
        
        const pageSize = ref(15); 
        const currentPage = ref(1); 
        const allPlayList = ref<SongListItem[]>([]);
        const loading = ref(false);
        const dialogVisible = ref(false);
        const isCreating = ref(false);
        const uploading = ref(false);
        const submitting = ref(false);
        const viewMode = ref('table'); // 视图模式：table 或 grid

        function normalizeQueryValue(value?: string | string[]) {
            return Array.isArray(value) ? value[0] : value;
        }

        function parsePositivePage(value?: string | string[]) {
            const page = Number(normalizeQueryValue(value));
            return Number.isInteger(page) && page > 0 ? page : 1;
        }

        const routePage = computed(() => parsePositivePage(route.query.page));

        function getMaxPage() {
            return Math.max(1, Math.ceil(allPlayList.value.length / pageSize.value));
        }

        async function syncPageQuery(page: number) {
            const query = { ...route.query, page: String(page) } as Record<string, string>;
            await router.replace({
                path: route.path,
                query
            });
        }

        async function applyBoundedRoutePage() {
            if (!allPlayList.value.length) {
                currentPage.value = routePage.value;
                return;
            }

            const boundedPage = Math.min(routePage.value, getMaxPage());
            currentPage.value = boundedPage;

            if (route.query.page && boundedPage !== routePage.value) {
                await syncPageQuery(boundedPage);
            }
        }
        
        // 计算属性
        const showAsGrid = computed(() => viewMode.value === 'grid');
        
        // 表单数据
        const form = reactive<SongListForm>({
            id: null,
            title: '',
            introduction: '',
            style: '',
            pic: '/img/songListPic/123.jpg' // 默认封面
        });

        // 表单验证规则
        const formRules: FormRules = {
            title: [
                { required: true, message: '请输入歌单标题', trigger: 'blur' },
                { min: 1, max: 30, message: '标题长度在 1 到 30 个字符', trigger: 'blur' }
            ],
            introduction: [
                { required: true, message: '请输入歌单介绍', trigger: 'blur' },
                { min: 1, max: 200, message: '介绍长度在 1 到 200 个字符', trigger: 'blur' }
            ],
            style: [
                { required: true, message: '请输入歌单风格', trigger: 'blur' }
            ]
        };

        // 计算属性 - 分页数据
        const tableData = computed(() => {
            if (!allPlayList.value.length) return [];
            
            const startIndex = (currentPage.value - 1) * pageSize.value;
            const endIndex = currentPage.value * pageSize.value;
            return allPlayList.value.slice(startIndex, endIndex);
        });

        // 检查登录状态
        function checkLoginStatus() {
            if (!isLoggedIn.value) {
                ElMessage.warning('请先登录');
                router.push('/sign-in');
                return false;
            }
            return true;
        }

        // 跳转到登录页
        function goToLogin() {
            router.push('/sign-in');
        }

        // 跳转到歌单详情页 - 修复版本
        function goToSongListDetail(songList: SongListItem) {
            // 设置歌单详情到 store
            store.commit("setSongDetails", songList);
            
            // 使用路由跳转
            router.push({
                path: `/my-song-sheet-detail/${songList.id}`
            });
        }

        function resolveSongListCover(source: Partial<SongListItem> | SongListForm) {
            return getSongListCoverUrl(source);
        }

        function mapStatus(type: number): SongListStatus {
            switch (type) {
                case 1:
                    return { label: '启用', type: 'success' };
                case 2:
                    return { label: '审核中', type: 'warning' };
                case 3:
                    return { label: '禁用', type: 'danger' };
                default:
                    return { label: '未知', type: 'info' };
            }
        }

        // 获取用户歌单方法
        async function getUserSongList() {
            if (!isLoggedIn.value) {
                return;
            }
            
            loading.value = true;
            try {
                const result = await HttpManager.getSongListByConsumerId(userId.value) as ResponseBody;
                
                if (result.success && result.data) {
                    allPlayList.value = result.data;
                    await applyBoundedRoutePage();
                } else {
                    allPlayList.value = [];
                    await applyBoundedRoutePage();
                    ElMessage.warning(result.message || "获取歌单失败");
                }
            } catch (error) {
                ElMessage.error("获取歌单失败，请检查网络连接");
                allPlayList.value = [];
                await applyBoundedRoutePage();
            } finally {
                loading.value = false;
            }
        }
        
        // 创建歌单
        function handleCreate() {
            if (!checkLoginStatus()) return;
            
            isCreating.value = true;
            dialogVisible.value = true;
            
            // 重置表单
            Object.assign(form, {
                id: null,
                title: '',
                introduction: '',
                style: '',
                pic: '/img/songListPic/123.jpg',
                type: 1,
                consumer: userId.value
            });
        }

        // 修改歌单
        function handleEdit(row: SongListItem) {
            if (!checkLoginStatus()) return;
            
            isCreating.value = false;
            dialogVisible.value = true;
            
            Object.assign(form, {
                id: row.id,
                title: row.title,
                introduction: row.introduction,
                style: row.style,
                pic: row.pic
            });
        }

        function handleResetPic() {
            form.pic = '/img/songListPic/123.jpg';
            ElMessage.info('已重置为默认封面');
        }

        // 提交表单（创建或修改）
        async function submitForm() {
            if (!formRef.value || !checkLoginStatus()) return;
            
            submitting.value = true;
            try {
                // 验证表单
                await formRef.value.validate();
                
                if (isCreating.value) {
                    // 创建歌单
                    await createSongList();
                } else {
                    // 修改歌单
                    await updateSongList();
                }
            } catch (error) {
                if (error !== false) {
                    ElMessage.error("请先完善歌单信息后再提交");
                }
            } finally {
                submitting.value = false;
            }
        }

        // 创建歌单的具体逻辑
        async function createSongList() {
            try {
                const createData = {
                    title: form.title,
                    introduction: form.introduction,
                    style: form.style,
                    pic: form.pic,
                    type: 1, // 默认启用状态
                    consumer: userId.value
                };
                
                const result = await HttpManager.addSongList(createData) as ResponseBody;
                if (result.success) {
                    ElMessage.success("创建歌单成功");
                    dialogVisible.value = false;
                    await getUserSongList(); // 刷新列表
                } else {
                    ElMessage.error(result.message || "创建歌单失败");
                }
            } catch (error) {
                ElMessage.error("创建歌单失败");
            }
        }

        // 修改歌单的具体逻辑
        async function updateSongList() {
            try {
                const updateData = {
                    id: form.id,
                    title: form.title,
                    introduction: form.introduction,
                    style: form.style,
                    pic: form.pic // 包含图片
                };
                
                const result = await HttpManager.updateSongList(updateData) as ResponseBody;
                if (result.success) {
                    ElMessage.success("修改成功");
                    dialogVisible.value = false;
                    await getUserSongList();
                } else {
                    ElMessage.error(result.message || "修改失败");
                }
            } catch (error) {
                ElMessage.error("修改歌单失败");
            }
        }

        const beforeAvatarUpload: UploadProps['beforeUpload'] = (rawFile) => {
            const isJPGOrPNG = rawFile.type === 'image/jpeg' || rawFile.type === 'image/png';
            const isLt2M = rawFile.size / 1024 / 1024 < 2;

            if (!isJPGOrPNG) {
                ElMessage.error('封面图片必须是 JPG 或 PNG 格式!');
                return false;
            }
            if (!isLt2M) {
                ElMessage.error('封面图片大小不能超过 2MB!');
                return false;
            }
            return true;
        };

        const handleAvatarUpload: UploadRequestHandler = async (options) => {
            if (!form.id && !isCreating.value) {
                ElMessage.warning('请先保存歌单基本信息再上传图片');
                return;
            }

            uploading.value = true;
            try {
                const formData = new FormData();
                formData.append('file', options.file);
                formData.append('id', String(isCreating.value ? 0 : form.id));

                const result = await HttpManager.updateSongListPic(formData) as ResponseBody;
                
                if (result.success) {
                    form.pic = result.data;
                    ElMessage.success('图片上传成功');
                } else {
                    ElMessage.error(result.message || '图片上传失败');
                }
            } catch (error) {
                ElMessage.error('图片上传失败');
            } finally {
                uploading.value = false;
            }
        };

        // 弹窗关闭处理
        function handleDialogClose() {
            formRef.value?.clearValidate();
            uploading.value = false;
        }

        // 删除歌单
        function handleDelete(id: number) {
            if (!checkLoginStatus()) return;
            
            ElMessageBox.confirm("确定要删除这个歌单吗？此操作不可逆！", "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning",
            }).then(async () => {
                try {
                    const result = await HttpManager.deleteSongList(id) as ResponseBody;
                    if (result.success) {
                        ElMessage.success("删除成功");
                        await getUserSongList();
                    } else {
                        ElMessage.error(result.message || "删除失败");
                    }
                } catch (error) {
                    ElMessage.error("删除歌单失败");
                }
            }).catch(() => {
                ElMessage.info("已取消删除");
            });
        }

        async function handleCurrentChange(val: number) {
            currentPage.value = val;
            await syncPageQuery(val);
        }
        
        onMounted(() => {
            if (!isLoggedIn.value) {
                ElMessage.warning('请先登录查看歌单');
                return;
            }
            
            getUserSongList();
        });

        watch(userId, (newId) => {
            if (newId) {
                getUserSongList();
            } else {
                allPlayList.value = [];
                currentPage.value = routePage.value;
            }
        });

        watch(
            () => route.query.page,
            () => {
                void applyBoundedRoutePage();
            },
            { immediate: true }
        );

        return {
            pageSize,
            currentPage,
            allPlayList,
            tableData,
            loading,
            dialogVisible,
            isCreating,
            isLoggedIn,
            form,
            formRef,
            formRules,
            uploading,
            submitting,
            viewMode,
            showAsGrid,
            Plus,
            Delete,
            Edit,
            View,
            Loading,
            handleCurrentChange,
            handleCreate,
            handleEdit,
            handleDelete,
            submitForm,
            mapStatus,
            resolveSongListCover,
            beforeAvatarUpload,
            handleAvatarUpload,
            handleDialogClose,
            goToLogin,
            goToSongListDetail,
            handleResetPic,
        };
    },
});
</script>
  
<style scoped lang="scss">
@import "@/assets/css/global.scss";

.my-song-sheet-page {
    display: flex;
    flex-direction: column;
    gap: 22px;
}

.hero-card,
.content-card {
    padding: clamp(20px, 3vw, 32px);
}

.hero-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 20px;
    margin-top: 16px;
}

.hero-head h1 {
    margin: 0;
    font-size: clamp(34px, 4vw, 56px);
    line-height: 1;
    color: var(--app-title);
}

.hero-head p {
    max-width: 720px;
    margin: 12px 0 0;
    color: var(--app-text-muted);
    line-height: 1.8;
}

.content-card {
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.user-playlist-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 0;
    padding: 0 4px;
    border-bottom: 1px solid var(--app-divider);
}
.user-playlist-header h2 {
    font-size: 24px;
    font-weight: bold;
    color: var(--app-title);
}
.pagination {
    margin-top: 20px;
    display: flex;
    justify-content: center;
}
.loading {
    text-align: center;
    padding: 50px;
    color: var(--app-text-muted);
}
.login-prompt {
    text-align: center;
    padding: 50px 0;
}
.song-list-title {
    cursor: pointer;
    color: var(--app-title);
    transition: color 0.3s;
}
.song-list-title:hover {
    color: var(--app-accent-strong);
}

.sheet-cover-thumb {
    width: 76px;
    height: 76px;
    border-radius: 18px;
    cursor: pointer;
    box-shadow: var(--app-panel-shadow-soft);
}
.upload-container {
    display: flex;
    align-items: flex-start;
    gap: 16px;
}
.avatar-uploader {
    border: 1px dashed var(--app-panel-border-strong);
    border-radius: 18px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    width: 120px;
    height: 120px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: border-color 0.3s;
}
.avatar-uploader:hover {
    border-color: var(--app-accent-strong);
}
.uploading {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: var(--app-accent-strong);
}
.upload-info {
    flex: 1;
}
.upload-tip {
    font-size: 12px;
    color: var(--app-text-soft);
    margin-bottom: 8px;
}
.view-toggle {
    margin-bottom: 6px;
    text-align: right;
}
.play-list-view {
    margin-top: 8px;
}

:deep(.el-radio-button__inner) {
    border-radius: 999px !important;
    background: var(--app-chip-bg-muted);
    border: 1px solid var(--app-panel-border) !important;
    color: var(--app-text-muted);
    box-shadow: none !important;
}

:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
    background: linear-gradient(135deg, var(--app-accent-strong), var(--app-accent));
    color: #08101f;
    border-color: transparent !important;
}

:deep(.el-table) {
    --el-table-bg-color: transparent;
    --el-table-tr-bg-color: var(--app-table-row);
    --el-table-row-hover-bg-color: var(--app-table-row-hover);
    --el-table-header-bg-color: transparent;
    --el-table-border-color: var(--app-divider);
    color: var(--app-text);
}

:deep(.el-table tr),
:deep(.el-table td.el-table__cell),
:deep(.el-table th.el-table__cell) {
    background: transparent;
}

:deep(.el-table .cell) {
    padding-top: 4px;
    padding-bottom: 4px;
    line-height: 1.4;
}

:deep(.el-table th.el-table__cell .cell) {
    color: var(--app-text-muted);
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0.04em;
}

@media (max-width: 768px) {
    .hero-head {
        flex-direction: column;
    }
}
</style>
