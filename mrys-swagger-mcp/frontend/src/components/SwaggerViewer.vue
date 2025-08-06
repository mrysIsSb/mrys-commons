<script setup lang="ts">
import { ref, onMounted } from 'vue'

// 定义接口类型
interface SwaggerSource {
  name: string
  url: string
}

interface ApiInfo {
  title: string
  version: string
  description: string
  baseUrl: string
  pathCount: number
  schemaCount: number
  httpMethods: Record<string, number>
}

// 响应式数据
const sources = ref<SwaggerSource[]>([])
const selectedSource = ref<string>('')
const apiInfo = ref<ApiInfo | null>(null)
const rawDoc = ref<string>('')
const loading = ref(false)
const error = ref<string>('')
const activeTab = ref<'info' | 'raw'>('info')

// 获取 Swagger 源列表
const fetchSources = async () => {
  try {
    loading.value = true
    error.value = ''

    const response = await fetch('/api/mcp', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        jsonrpc: '2.0',
        id: 1,
        method: 'tools/call',
        params: {
          name: 'list_swagger_sources',
          arguments: {}
        }
      })
    })

    const data = await response.json()
    if (data.result && data.result.content) {
      // 解析源列表文本
      const content = data.result.content[0].text
      const lines = content.split('\n').filter((line: string) => line.includes(':'))
      sources.value = lines.map((line: string) => {
        const [name, url] = line.split(': ')
        return {
          name: name.replace('- ', '').trim(),
          url: url.trim()
        }
      })
    }
  } catch (err) {
    error.value = '获取 Swagger 源列表失败: ' + (err as Error).message
  } finally {
    loading.value = false
  }
}

// 获取 Swagger 文档
const fetchSwaggerDoc = async (source: string) => {
  if (!source) return

  try {
    loading.value = true
    error.value = ''

    // 获取原始文档
    const docResponse = await fetch('/api/mcp', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        jsonrpc: '2.0',
        id: 2,
        method: 'tools/call',
        params: {
          name: 'get_swagger_doc',
          arguments: { source }
        }
      })
    })

    const docData = await docResponse.json()
    if (docData.result && docData.result.content) {
      rawDoc.value = docData.result.content[0].text
    }

    // 获取解析后的信息
    const parseResponse = await fetch('/api/mcp', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        jsonrpc: '2.0',
        id: 3,
        method: 'tools/call',
        params: {
          name: 'parse_swagger_doc',
          arguments: { source }
        }
      })
    })

    const parseData = await parseResponse.json()
    if (parseData.result && parseData.result.content) {
      const content = parseData.result.content[0].text
      // 简单解析 Markdown 格式的 API 信息
      apiInfo.value = parseMarkdownInfo(content)
    }
  } catch (err) {
    error.value = '获取 Swagger 文档失败: ' + (err as Error).message
  } finally {
    loading.value = false
  }
}

// 解析 Markdown 格式的 API 信息
const parseMarkdownInfo = (content: string): ApiInfo => {
  const lines = content.split('\n')
  const info: ApiInfo = {
    title: '',
    version: '',
    description: '',
    baseUrl: '',
    pathCount: 0,
    schemaCount: 0,
    httpMethods: {}
  }

  for (const line of lines) {
    if (line.startsWith('**标题:**')) {
      info.title = line.replace('**标题:**', '').trim()
    } else if (line.startsWith('**版本:**')) {
      info.version = line.replace('**版本:**', '').trim()
    } else if (line.startsWith('**基础URL:**')) {
      info.baseUrl = line.replace('**基础URL:**', '').trim()
    } else if (line.startsWith('**总路径数:**')) {
      info.pathCount = parseInt(line.replace('**总路径数:**', '').trim()) || 0
    } else if (line.startsWith('**Schema数量:**')) {
      info.schemaCount = parseInt(line.replace('**Schema数量:**', '').trim()) || 0
    } else if (line.startsWith('- **')) {
      const match = line.match(/- \*\*(\w+):\*\* (\d+)/)
      if (match) {
        info.httpMethods[match[1]] = parseInt(match[2])
      }
    }
  }

  return info
}

// 清除缓存
const clearCache = async () => {
  try {
    loading.value = true
    await fetch('/api/mcp', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        jsonrpc: '2.0',
        id: 4,
        method: 'tools/call',
        params: {
          name: 'clear_cache',
          arguments: {}
        }
      })
    })
    alert('缓存已清除')
  } catch (err) {
    error.value = '清除缓存失败: ' + (err as Error).message
  } finally {
    loading.value = false
  }
}

// 组件挂载时获取源列表
onMounted(() => {
  fetchSources()
})

// 监听选中的源变化
const onSourceChange = () => {
  if (selectedSource.value) {
    fetchSwaggerDoc(selectedSource.value)
  }
}
</script>

<template>
  <div class="space-y-6">
    <!-- 控制面板 -->
    <div class="bg-white rounded-lg hover:shadow-lg p-6">
      <div class="flex flex-col gap-4 items-center sm:items-stretch justify-start">
        <div class="flex-1">
          <label for="source-select" class="block text-sm font-medium text-gray-700 mb-2">
            选择 Swagger 源
          </label>
          <select id="source-select" v-model="selectedSource" @change="onSourceChange"
            class="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500">
            <option value="">请选择一个 Swagger 源</option>
            <option v-for="source in sources" :key="source.name" :value="source.name">
              {{ source.name }} ({{ source.url }})
            </option>
          </select>
        </div>

        <div class="flex gap-2">
          <button @click="fetchSources" :disabled="loading"
            class="px-4 py-2 leading-0 h-8 bg-blue-600 text-white rounded-md hover:bg-blue-700 hover:cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed">
            刷新源列表
          </button>
          <button @click="clearCache" :disabled="loading"
            class="px-4 py-2 bg-gray-600 text-white rounded-md hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed">
            清除缓存
          </button>
        </div>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="bg-red-50 border border-red-200 rounded-md p-4">
      <div class="flex">
        <div class="flex-shrink-0">
          <svg class="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd"
              d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
              clip-rule="evenodd" />
          </svg>
        </div>
        <div class="ml-3">
          <h3 class="text-sm font-medium text-red-800">错误</h3>
          <div class="mt-2 text-sm text-red-700">
            {{ error }}
          </div>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="flex justify-center py-8">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
    </div>

    <!-- API 信息展示 -->
    <div v-if="apiInfo && !loading" class="bg-white rounded-lg shadow">
      <!-- 标签页 -->
      <div class="border-b border-gray-200">
        <nav class="-mb-px flex">
          <button @click="activeTab = 'info'" :class="[
            'py-2 px-4 border-b-2 font-medium text-sm',
            activeTab === 'info'
              ? 'border-blue-500 text-blue-600'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          ]">
            API 信息
          </button>
          <button @click="activeTab = 'raw'" :class="[
            'py-2 px-4 border-b-2 font-medium text-sm',
            activeTab === 'raw'
              ? 'border-blue-500 text-blue-600'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          ]">
            原始文档
          </button>
        </nav>
      </div>

      <!-- API 信息标签页 -->
      <div v-if="activeTab === 'info'" class="p-6">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <!-- 基本信息 -->
          <div>
            <h3 class="text-lg font-medium text-gray-900 mb-4">基本信息</h3>
            <dl class="space-y-3">
              <div>
                <dt class="text-sm font-medium text-gray-500">标题</dt>
                <dd class="text-sm text-gray-900">{{ apiInfo.title || '未知' }}</dd>
              </div>
              <div>
                <dt class="text-sm font-medium text-gray-500">版本</dt>
                <dd class="text-sm text-gray-900">{{ apiInfo.version || '未知' }}</dd>
              </div>
              <div>
                <dt class="text-sm font-medium text-gray-500">基础 URL</dt>
                <dd class="text-sm text-gray-900 font-mono">{{ apiInfo.baseUrl || '未知' }}</dd>
              </div>
            </dl>
          </div>

          <!-- 统计信息 -->
          <div>
            <h3 class="text-lg font-medium text-gray-900 mb-4">统计信息</h3>
            <dl class="space-y-3">
              <div>
                <dt class="text-sm font-medium text-gray-500">API 路径数</dt>
                <dd class="text-sm text-gray-900">{{ apiInfo.pathCount }}</dd>
              </div>
              <div>
                <dt class="text-sm font-medium text-gray-500">Schema 数量</dt>
                <dd class="text-sm text-gray-900">{{ apiInfo.schemaCount }}</dd>
              </div>
            </dl>
          </div>
        </div>

        <!-- HTTP 方法统计 -->
        <div v-if="Object.keys(apiInfo.httpMethods).length > 0" class="mt-6">
          <h3 class="text-lg font-medium text-gray-900 mb-4">HTTP 方法统计</h3>
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div v-for="(count, method) in apiInfo.httpMethods" :key="method"
              class="bg-gray-50 rounded-lg p-4 text-center">
              <div class="text-2xl font-bold text-gray-900">{{ count }}</div>
              <div class="text-sm text-gray-500 uppercase">{{ method }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 原始文档标签页 -->
      <div v-if="activeTab === 'raw'" class="p-6">
        <div class="mb-4">
          <h3 class="text-lg font-medium text-gray-900">原始 Swagger 文档</h3>
          <p class="text-sm text-gray-500 mt-1">完整的 OpenAPI/Swagger JSON 文档</p>
        </div>
        <pre class="bg-gray-50 rounded-lg p-4 overflow-auto text-xs"><code>{{ rawDoc }}</code></pre>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!apiInfo && !loading && !error" class="text-center py-12">
      <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
      </svg>
      <h3 class="mt-2 text-sm font-medium text-gray-900">暂无文档</h3>
      <p class="mt-1 text-sm text-gray-500">请选择一个 Swagger 源来查看文档</p>
    </div>
  </div>
</template>

<style scoped>
/* 自定义样式 */
code {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
}
</style>