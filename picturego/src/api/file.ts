import request from './request'

/** 上传文件，返回可访问的文件地址 */
export function uploadFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<unknown, string>('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
