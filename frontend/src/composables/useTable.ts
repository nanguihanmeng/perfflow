/**
 * 分页查询组合式函数：封装 加载/搜索/分页 通用范式
 */
import { onMounted, reactive, ref, watch } from 'vue'
import type { PageResult } from '@/types/result'

export interface UseTableOptions<T, Q> {
  /** 列表查询函数，返回分页结构 */
  fetcher: (query: Q) => Promise<PageResult<T>>
  /** 默认查询参数（不含分页） */
  defaultQuery?: () => Partial<Omit<Q, 'pageNo' | 'pageSize'>>
  /** 默认每页条数 */
  pageSize?: number
  /** 是否自动加载（默认 true） */
  autoLoad?: boolean
}

export const useTable = <T, Q>(options: UseTableOptions<T, Q>) => {
  const { fetcher, defaultQuery, pageSize = 20, autoLoad = true } = options

  const loading = ref(false)
  const list = ref<T[]>([])

  /** 分页参数 */
  const pagination = reactive({
    pageNo: 1,
    pageSize,
    total: 0
  })

  /** 搜索参数（不含分页） */
  const query = reactive<Partial<Omit<Q, 'pageNo' | 'pageSize'>>>({})

  const load = async (): Promise<void> => {
    loading.value = true
    try {
      const params = {
        ...query,
        pageNo: pagination.pageNo,
        pageSize: pagination.pageSize
      } as Q
      const result = await fetcher(params)
      list.value = result.records
      pagination.total = result.total
    } finally {
      loading.value = false
    }
  }

  /** 搜索：重置页码后重新加载 */
  const search = (): void => {
    if (pagination.pageNo !== 1) {
      pagination.pageNo = 1
    } else {
      void load()
    }
  }

  /** 重置查询条件并搜索 */
  const reset = (): void => {
    const q = query as Record<string, unknown>
    Object.keys(q).forEach((key) => {
      delete q[key]
    })
    if (defaultQuery) {
      Object.assign(query, defaultQuery())
    }
    search()
  }

  watch(
    () => [pagination.pageNo, pagination.pageSize],
    () => {
      void load()
    }
  )

  if (autoLoad) {
    onMounted(() => {
      if (defaultQuery) {
        Object.assign(query, defaultQuery())
      }
      void load()
    })
  }

  return { loading, list, pagination, query, load, search, reset }
}
