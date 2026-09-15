import { createRouter, createWebHistory } from 'vue-router'
import ListPage from '../views/list.vue'
import AddPage from '../views/add.vue'
import EditPage from '../views/edit.vue'
import DetailPage from '../views/detail.vue'
import DetailCardPage from '../views/detail-card.vue'
import LoginPage from '../views/login.vue'
import NotePage from '../components/note.vue'
import { auth } from '../auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: LoginPage,
  },
  {
    path: '/',
    name: 'List',
    component: ListPage,
  },
  {
    path: '/add',
    name: 'Add',
    component: AddPage,
    meta: { requiresAuth: true, permission: 'menu:create' },
  },
  {
    path: '/edit/:id',
    name: 'Edit',
    component: EditPage,
    meta: { requiresAuth: true, permission: 'menu:update' },
  },
  {
    path: '/detail/:id',
    name: 'Detail',
    component: DetailPage,
  },
  {
    path: '/detail-card/:id',
    name: 'DetailCard',
    component: DetailCardPage,
  },
  {
    path: '/note',
    name: 'Note',
    component: NotePage,
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router

router.beforeEach(async (to) => {
  if (to.name === 'Login') {
    return true
  }

  const loggedIn = await auth.load()
  if (to.meta.requiresAuth && !loggedIn) {
    return { name: 'List' }
  }

  const permission = to.meta.permission
  if (permission && !auth.hasPermission(permission)) {
    return { name: 'List' }
  }
  return true
})
