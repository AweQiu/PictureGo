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
          path: 'user-manage',
          name: 'userManage',
          component: () => import('../pages/UserManagePage.vue'),
          meta: { title: '用户管理', requiresAdmin: true },
        },
        {
          path: 'picture-manage',
          name: 'pictureManage',
          component: () => import('../pages/PictureManagePage.vue'),
          meta: { title: '图片管理', requiresAdmin: true },
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
