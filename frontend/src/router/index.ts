/**
 * 路由实例与路由表
 */
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import AppLayout from '@/layout/AppLayout.vue'
import { Role } from '@/types/enums'
import { setupRouterGuards } from '@/router/guards'

export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { requiresAuth: false, title: '登录' }
  },
  {
    path: '/change-password',
    name: 'ChangePassword',
    component: () => import('@/views/login/ChangePassword.vue'),
    meta: { title: '修改密码' }
  },
  {
    path: '/',
    component: AppLayout,
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/home/Home.vue'),
        meta: { title: '工作台' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/Profile.vue'),
        meta: { title: '个人中心' }
      },
      {
        path: 'me/assessment',
        name: 'MyAssessment',
        component: () => import('@/views/me/MyAssessment.vue'),
        meta: { title: '我的考核表', roles: [Role.EMP], adminDenied: true }
      },
      {
        path: 'me/assessment/:id',
        name: 'MyAssessmentDetail',
        component: () => import('@/views/common/AssessmentDetail.vue'),
        meta: { title: '我的考核表详情', roles: [Role.EMP], adminDenied: true }
      },
      {
        path: 'dept/review',
        name: 'DeptReview',
        component: () => import('@/views/dept-lead/DeptReview.vue'),
        meta: { title: '部门审核', roles: [Role.DEPT_LEAD], adminDenied: true }
      },
      {
        path: 'dept/review/:id',
        name: 'DeptReviewDetail',
        component: () => import('@/views/common/AssessmentDetail.vue'),
        meta: { title: '部门审核详情', roles: [Role.DEPT_LEAD], adminDenied: true }
      },
      {
        path: 'lead/score',
        name: 'LeadScore',
        component: () => import('@/views/lead/LeadScore.vue'),
        meta: { title: '领导评分', roles: [Role.LEAD], adminDenied: true }
      },
      {
        path: 'lead/score/:id',
        name: 'LeadScoreDetail',
        component: () => import('@/views/common/AssessmentDetail.vue'),
        meta: { title: '领导评分详情', roles: [Role.LEAD], adminDenied: true }
      },
      {
        path: 'hr/period',
        name: 'PeriodManage',
        component: () => import('@/views/hr/PeriodManage.vue'),
        // 注意：后端拦截器禁止 ADMIN 访问 /periods/**，故仅 HR 可见
        meta: { title: '周期管理', roles: [Role.HR], adminDenied: true }
      },
      {
        path: 'hr/list',
        name: 'AssessmentList',
        component: () => import('@/views/hr/AssessmentList.vue'),
        meta: { title: '考核列表', roles: [Role.LEAD, Role.HR], adminDenied: true }
      },
      {
        path: 'hr/table/:id',
        name: 'AssessmentTableDetail',
        component: () => import('@/views/common/AssessmentDetail.vue'),
        meta: { title: '考核详情', roles: [Role.LEAD, Role.HR], adminDenied: true }
      },
      {
        path: 'admin/users',
        name: 'UserManage',
        component: () => import('@/views/admin/UserManage.vue'),
        meta: { title: '用户管理', roles: [Role.ADMIN] }
      },
      {
        path: 'admin/depts',
        name: 'DeptManage',
        component: () => import('@/views/admin/DeptManage.vue'),
        meta: { title: '部门管理', roles: [Role.ADMIN] }
      }
    ]
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/Forbidden.vue'),
    meta: { requiresAuth: false, title: '无权限' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
    meta: { requiresAuth: false, title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

setupRouterGuards(router)

export default router
