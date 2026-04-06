import { createRouter, createWebHistory, RouteRecordRaw } from "vue-router";

const routes: Array<RouteRecordRaw> = [
  {
    path: "/:pathMatch(.*)*",
    redirect: "/404",
  },
  {
    path: "/404",
    component: () => import("@/views/error/404.vue"),
  },
  {
    path: "/",
    name: "main-container",
    component: () => import("@/views/MainContainer.vue"),
    children: [
      {
        path: "/",
        name: "home",
        component: () => import("@/views/Home.vue"),
      },
      {
        path: "/sign-in",
        name: "sign-in",
        component: () => import("@/views/SignIn.vue"),
      },
      {
        path: "/sign-up",
        name: "sign-up",
        component: () => import("@/views/SignUp.vue"),
      },
      {
        path: "/personal",
        name: "personal",
        meta: {
          requireAuth: true,
        },
        component: () => import("@/views/personal/Personal.vue"),
      },
      {//跳转到歌单页面路径
        path: "/song-sheet",
        name: "song-sheet",
        component: () => import("@/views/song-sheet/SongSheet.vue"),
      },
      {
        path: "/song-sheet-detail/:id",
        name: "song-sheet-detail",
        component: () => import("@/views/song-sheet/SongSheetDetail.vue"),
        
      },

      {
        path: "/my-song-sheet-detail/:id",
        name: "my-song-sheet-detail",
        component: () => import("@/views/personal/MySongSheetDetail.vue"),
      },

      {
        path: "/singer",
        name: "singer",
        component: () => import("@/views/singer/Singer.vue"),
      },
      {
        path: "/rank",
        name: "rank",
        component: () => import("@/views/rank/Rank.vue"),
      },

      {
        path: "/singer-detail/:id",
        name: "singer-detail",
        component: () => import("@/views/singer/SingerDetail.vue"),
      },
      {
        path: "/collection",
        name: "collection",
        meta: {
          requireAuth: true,
        },
        component: () => import("@/views/collection/Collection.vue"),
      },
      {
        path: "/lyric/:id",
        name: "lyric",
        component: () => import("@/views/Lyric.vue"),
      },
      {
        path: "/search",
        name: "search",
        component: () => import("@/views/search/Search.vue"),
      },
      {
        path: "/about",
        name: "about",
        component: () => import("@/views/site/AboutSite.vue"),
      },
      {
        path: "/resources",
        name: "resources",
        component: () => import("@/views/site/ResourceSource.vue"),
      },
      {
        path: "/terms",
        name: "terms",
        component: () => import("@/views/site/TermsOfUse.vue"),
      },
      {
        path: "/feedback",
        name: "feedback",
        component: () => import("@/views/site/FeedbackPage.vue"),
      },
      {
        path: "/personal-data",
        name: "personal-data",
        component: () => import("@/views/setting/PersonalData.vue"),
      },
      {
        path: "/FPassword",
        name: "FPassword",
        component: ()=> import("@/views/FPassword.vue"),
      },
      {
        path: "/loginByemail",
        name: "loginByemail",
        component: ()=> import("@/views/loginByemail.vue"),
      },

      {
        path: "/MySongSheet",
        name: "MySongSheet",
        meta: {
          requireAuth: true,
        },
        component: () => import("@/views/personal/MySongSheet.vue"),
      },

      {
        path: "/setting",
        name: "setting",
        meta: {
          requireAuth: true,
        },
        component: () => import("@/views/setting/Setting.vue"),
        children: [
          {
            path: "/setting/PersonalData",
            name: "personalData",
            meta: {
              requireAuth: true,
            },
            component: () => import("@/views/setting/PersonalData.vue"),
          }
        ]
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition;
    }
    return { top: 0, left: 0 };
  },
});

export default router;
