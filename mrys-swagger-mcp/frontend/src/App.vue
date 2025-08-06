<script setup lang="ts">
import { ref } from 'vue'
import SwaggerViewer from './components/SwaggerViewer.vue'

const title = ref('Swagger MCP 文档查看器')
</script>

<template>
  <div class="min-h-screen min-w-1xl bg-gray-50">
    <!-- 头部导航 -->
    <header class="bg-green-300 shadow-sm border-b">
      <div class="max-w-full mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex flex-wrap justify-between items-center h-12">
          <div class="flex items-center">
            <h1 class="text-xl font-semibold text-gray-900">{{ title }}</h1>
          </div>
          <div class="flex items-center space-x-4">
            <span class="text-sm text-gray-500">基于 MCP 协议的 Swagger 文档服务</span>
          </div>
        </div>
      </div>
    </header>

    <!-- 主要内容区域 -->
    <main class="max-w-full mx-auto py-6 px-4 sm:px-6 lg:px-8">
      <SwaggerViewer />
    </main>
  </div>
</template>

<style scoped>
/* 自定义样式 */
</style>
