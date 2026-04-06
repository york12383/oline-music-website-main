import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { clearStoredAdminInfo, hasValidStoredAdminInfo } from '@/utils/admin-auth'
const routes: Array<RouteRecordRaw> = [
  {
    path: '/Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '????' },
    children: [
      {
        path: '/Info',
        component: () => import('@/views/InfoPage.vue'),
        meta: { title: 'Info', requiresAuth: true }
      },
      {
        path: '/song',
        component: () => import('@/views/SongPage.vue'),
        meta: { title: 'Song' }
      },
      {
        path: '/songs',
        component: () => import('@/views/SongsPage.vue'),
        meta: { title: 'Songs' }
      },
      {
        path: '/singer',
        component: () => import('@/views/SingerPage.vue'),
        meta: { title: 'Singer' }
      },
      {
        path: '/SongList',
        component: () => import('@/views/SongListPage.vue'),
        meta: { title: 'SongList' }
      },
      {
        path: '/ListSong',
        component: () => import('@/views/ListSongPage.vue'),
        meta: { title: 'ListSong' }
      },
      {
        path: '/Comment',
        component: () => import('@/views/CommentPage.vue'),
        meta: { title: 'Comment' }
      },
      {
        path: '/Consumer',
        component: () => import('@/views/ConsumerPage.vue'),
        meta: { title: 'Consumer' }
      },
      {
        path: '/Collect',
        component: () => import('@/views/CollectPage.vue'),
        meta: { title: 'Collect' }
      },
      {
        path: '/banner',
        component: () => import('@/views/BannerPage.vue'),
        meta: { title: 'Banner' }
      },
      {
        path: '/admin',
        component: () => import('@/views/AdminPage.vue'),
        meta: { title: 'Admin' }
      },
      {
        path: '/rank',
        component: () => import('@/views/RankPage.vue'),
        meta: { title: 'Rank' }
      },
      {
        path: '/feedback-manage',
        component: () => import('@/views/FeedbackManagePage.vue'),
        meta: { title: 'FeedbackManage' }
      },
    ]
  },
  {
    path: '/',
    component: () => import('@/views/Login.vue')
  }
]
const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})
router.beforeEach((to, from, next) => {
  const hasLocalAdmin = hasValidStoredAdminInfo()
  if (to.path === '/') {
    next()
    return
  }
  if (hasLocalAdmin) {
    next()
    return
  }
  clearStoredAdminInfo()
  next('/')
})
export default router
