import axios, { AxiosError, type AxiosResponse } from 'axios'

/** 业务错误码（以实际后端响应为准，文档示例中的 0 不准确） */
export const SUCCESS_CODE = 200
export const NOT_LOGIN_CODE = 40100

export class ApiError extends Error {
  code: number

  constructor(code: number, message: string) {
    super(message)
    this.code = code
  }
}

const request = axios.create({
  baseURL: '/picgo',
  timeout: 30000,
  withCredentials: true,
})

request.interceptors.response.use(
  (response: AxiosResponse<Result<unknown>>) => {
    // 文件下载等二进制响应直接放行
    if (response.config.responseType === 'blob') {
      return response
    }
    const result = response.data
    if (result && typeof result.code === 'number') {
      if (result.code === SUCCESS_CODE) {
        return result.data as never
      }
      return Promise.reject(new ApiError(result.code, result.message || '请求失败'))
    }
    return response
  },
  (error: AxiosError<Result<unknown>>) => {
    const result = error.response?.data
    if (result && typeof result.code === 'number') {
      return Promise.reject(new ApiError(result.code, result.message || '请求失败'))
    }
    return Promise.reject(new ApiError(-1, error.message || '网络异常'))
  },
)

export default request
