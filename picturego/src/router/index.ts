import { createRouter, createWebHistory } from 'vue-router'
import { message } from 'ant-design-vue'
import { useUserStore } from '../store/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../pages/LoginPage.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/',
      component: () => import('../layouts/BasicLayout.vue'),
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('../pages/HomePage.vue'),
          meta: { title: '首页' },
        },
        {
          path: 'upload',
          name: 'pictureUpload',
          component: () => import('../pages/PictureUploadPage.vue'),
          meta: { title: '上传图片' },
        },
        {
          // 图片管理 = 侧边栏「公共图库」+「我的空间」
          path: 'picture-manage',
          name: 'pictureManage',
          redirect: { name: 'picturePublic' },
          component: () => import('../layouts/PictureManageLayout.vue'),
          meta: { title: '图片管理', menuKey: 'pictureManage' },
          children: [
            {
              path: 'public',
              name: 'picturePublic',
              component: () => import('../pages/PicturePublicPage.vue'),
              meta: { title: '公共图库' },
            },
            {
              path: 'my-space',
              name: 'mySpace',
              component: () => import('../pages/MySpacePage.vue'),
              meta: { title: '我的空间' },
            },
          ],
        },
        {
          // 兼容旧的 /my-space 链接，统一进入图片管理页的「我的空间」
          path: 'my-space',
          redirect: { name: 'mySpace' },
        },
        {
          path: 'space/create',
          name: 'spaceCreate',
          component: () => import('../pages/SpaceCreatePage.vue'),
          meta: { title: '创建空间' },
        },
        {
          path: 'user-manage',
          name: 'userManage',
          component: () => import('../pages/UserManagePage.vue'),
          meta: { title: '用户管理', requiresAdmin: true },
        },
        {
          path: 'picture/:id',
          name: 'pictureDetail',
          component: () => import('../pages/PictureDetailPage.vue'),
          meta: { title: '图片详情' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

router.beforeEach(async (to) => {
  const userStore = useUserStore()
  if (!userStore.hasFetched) {
    await userStore.fetchLoginUser()
  }
  if (!userStore.isLogin && to.name !== 'login') {
    return { name: 'login' }
  }
  if (userStore.isLogin && to.name === 'login') {
    return { name: 'home' }
  }
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    message.warning('没有权限访问该页面')
    return { name: 'home' }
  }
  document.title = to.meta.title
    ? `${to.meta.title} - PicGo云图库`
    : 'PicGo云图库'
})

export default router
