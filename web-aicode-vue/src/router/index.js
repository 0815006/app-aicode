// src/router/index.js
import Vue from 'vue'
import Router from 'vue-router'
import Home from '@/views/Home.vue'
import ResourceCheck from '@/views/ResourceCheck.vue'
import ParkingBookList from '@/views/ParkingBookList.vue' // 新增
import ChatHall from '@/views/ChatHall.vue' // 新增
import DataMigration from '@/views/DataMigration.vue'
import VoteManagement from '@/views/VoteManagement.vue'
import XmindConverter from '@/views/XmindConverter.vue'
import PerformanceTest from '@/views/PerformanceTest.vue'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/',
      component: Home,
      redirect: '/chat',
      children: [
        {
          path: 'vote',
          name: 'VoteManagement',
          component: VoteManagement,
          meta: { title: '投票功能' }
        },
        {
          path: 'resource-check',
          name: 'ResourceCheck',
          component: ResourceCheck,
          meta: { title: '资源核查' }
        },
        {
          path: 'parking-list',
          name: 'ParkingBookList',
          component: ParkingBookList,
          meta: { title: '车位预约查询' }
        },
        {
          path: 'chat',
          name: 'ChatHall',
          component: ChatHall,
          meta: { title: '聊天大厅' }
        },
        {
          path: 'data-migration',
          name: 'DataMigration',
          component: DataMigration,
          meta: { title: '数据迁移' }
        },
        {
          path: 'xmind-converter',
          name: 'XmindConverter',
          component: XmindConverter,
          meta: { title: 'XMind 转换' }
        },
        {
          path: 'performance',
          name: 'PerformanceTest',
          component: PerformanceTest,
          meta: { title: '性能测试方案' }
        }
      ]
    }
  ]
})
