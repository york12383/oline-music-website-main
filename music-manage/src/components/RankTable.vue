<template>
    <div class="rank-table-wrapper">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item :label="type === 0 ? '歌曲/歌手' : '歌单名称'">
          <el-input v-model="searchForm.name" :placeholder="type === 0 ? '歌曲名/歌手名' : '歌单名'" clearable />
        </el-form-item>
        <el-form-item label="评分用户">
          <el-input v-model="searchForm.consumerName" placeholder="用户昵称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
  
      <el-table :data="data" v-loading="loading" border stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="id" label="评分ID" width="100" align="center" />
        
        <el-table-column :label="type === 0 ? '歌曲信息' : '歌单名称'" min-width="180">
          <template #default="{ row }">
              {{ row.name }}
              <span v-if="type === 0 && row.singerName"> ({{ row.singerName }})</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="consumerName" label="评分用户" width="150" align="center" />
        
        <el-table-column label="评分" width="180" align="center">
  <template #default="{ row }">
    <el-rate 
      :model-value="row.score / 2" 
      disabled 
      show-score 
      text-color="#ff9900" 
      score-template="{value}"
      allow-half
    />
  </template>
</el-table-column>
        
        <!-- <el-table-column prop="createTime" label="评分时间" width="180" align="center" /> -->
        
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button class="delete-rank-button" type="danger" size="small" @click="$emit('delete', row.id, type)">
              删除评分
            </el-button>
          </template>
        </el-table-column>
      </el-table>
  
      <el-pagination
        class="pagination-container"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="currentPage"
        :page-sizes="[10, 20, 50]"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        background
      />
    </div>
  </template>
  
  <script lang="ts">
  import { defineComponent, ref, watch, PropType } from 'vue';
  
  export default defineComponent({
    props: {
      // 0: 歌曲评分, 1: 歌单评分
      type: {
        type: Number as PropType<0 | 1>,
        required: true,
      },
      data: {
        type: Array as PropType<any[]>,
        default: () => [],
      },
      total: {
        type: Number,
        default: 0,
      },
      loading: {
        type: Boolean,
        default: false,
      },
      currentPageValue: {
        type: Number,
        default: 1,
      },
      pageSizeValue: {
        type: Number,
        default: 10,
      },
      searchName: {
        type: String,
        default: '',
      },
      searchConsumerName: {
        type: String,
        default: '',
      },
    },
    emits: ['search', 'delete', 'page-change'],
    setup(props, { emit }) {
      const currentPage = ref(props.currentPageValue);
      const pageSize = ref(props.pageSizeValue);
      const searchForm = ref({
        name: props.searchName,
        consumerName: props.searchConsumerName,
      });
  
      // 搜索按钮
      const handleSearch = () => {
        currentPage.value = 1;
        emit('search', props.type, {
          currentPage: currentPage.value,
          pageSize: pageSize.value,
          ...searchForm.value,
        });
      };
  
      // 重置按钮
      const handleReset = () => {
        searchForm.value = { name: '', consumerName: '' };
        currentPage.value = 1;
        emit('search', props.type, {
          currentPage: currentPage.value,
          pageSize: pageSize.value,
          ...searchForm.value,
        });
      };
  
      // 分页大小变化
      const handleSizeChange = (val: number) => {
        pageSize.value = val;
        emit('page-change', props.type, {
          currentPage: currentPage.value,
          pageSize: pageSize.value,
          ...searchForm.value,
        });
      };
  
      // 当前页变化
      const handleCurrentChange = (val: number) => {
        currentPage.value = val;
        emit('page-change', props.type, {
          currentPage: currentPage.value,
          pageSize: pageSize.value,
          ...searchForm.value,
        });
      };
      
      const applyPropsState = () => {
        currentPage.value = props.currentPageValue;
        pageSize.value = props.pageSizeValue;
        searchForm.value = {
          name: props.searchName,
          consumerName: props.searchConsumerName,
        };
      };

      watch(
        () => [
          props.type,
          props.currentPageValue,
          props.pageSizeValue,
          props.searchName,
          props.searchConsumerName,
        ],
        applyPropsState
      );
      
      return {
        currentPage,
        pageSize,
        searchForm,
        handleSearch,
        handleReset,
        handleSizeChange,
        handleCurrentChange,
      };
    },
  });
  </script>
  
  <style scoped>
  .rank-table-wrapper {
    padding: 4px 0 0;
  }
  .search-form {
    margin-bottom: 22px;
  }
  .pagination-container {
    margin-top: 20px;
    justify-content: flex-end;
    display: flex;
  }

  :deep(.el-rate) {
    gap: 2px;
  }

  .delete-rank-button {
    min-width: 0;
    height: 28px;
    padding: 0 10px;
    border-radius: 10px;
    font-size: 12px;
    font-weight: 700;
    color: #ffffff !important;
    border-color: rgba(248, 113, 125, 0.96) !important;
    background: linear-gradient(135deg, #fb7381 0%, #f35b66 100%) !important;
    box-shadow: 0 10px 18px rgba(243, 91, 102, 0.16);
  }

  .delete-rank-button:hover,
  .delete-rank-button:focus-visible {
    color: #ffffff !important;
    border-color: rgba(252, 132, 142, 0.98) !important;
    background: linear-gradient(135deg, #fc8792 0%, #f56a75 100%) !important;
  }
  </style>
