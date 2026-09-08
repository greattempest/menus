import { createRouter, createWebHistory } from 'vue-router'
import ListPage from '../views/list.vue'
import AddPage from '../views/add.vue'
import EditPage from '../views/edit.vue'
import DetailPage from '../views/detail.vue'
import DetailCardPage from '../views/detail-card.vue'

const routes = [
  {
    path: '/',
    name: 'List',
    component: ListPage,
  },
  {
    path: '/add',
    name: 'Add',
    component: AddPage,
  },
  {
    path: '/edit/:id',
    name: 'Edit',
    component: EditPage,
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
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
