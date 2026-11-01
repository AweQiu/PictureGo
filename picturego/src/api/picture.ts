import request from './request'

export function listPictureByPage(data: PictureQueryRequest) {
  return request.post<unknown, Page<Picture>>('/picture/list/page', data)
}

export function listPictureVOByPage(data: PictureQueryRequest) {
  return request.post<unknown, Page<PictureVO>>('/picture/list/page/vo', data)
}

export function listPictureVOByPageWithCache(data: PictureQueryRequest) {
  return request.post<unknown, Page<PictureVO>>('/picture/list/page/vo/cache', data)
}

export function getPictureById(id: number | string) {
  return request.get<unknown, Picture>('/picture/get', { params: { id } })
}

export function getPictureVOById(id: number | string) {
  return request.get<unknown, PictureVO>('/picture/get/vo', { params: { id } })
}

/** 上传图片：不传 id 时新建，传 id 时替换该图片的文件；可选携带图片信息 */
export function uploadPicture(file: File, data: PictureUploadRequest = {}) {
  const formData = new FormData()
  formData.append('file', file)
  if (data.id !== undefined && data.id !== null) {
    formData.append('id', String(data.id))
  }
  if (data.name) formData.append('name', data.name)
  if (data.introduction) formData.append('introduction', data.introduction)
  if (data.category) formData.append('category', data.category)
  data.tags?.forEach((tag) => formData.append('tags', tag))
  if (data.spaceId !== undefined && data.spaceId !== null) {
    formData.append('spaceId', String(data.spaceId))
  }
  return request.post<unknown, PictureVO>('/picture/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 通过 URL 上传图片：不传 id 时新建，传 id 时替换该图片的文件；可选携带图片信息 */
export function uploadPictureByUrl(data: PictureUploadRequest) {
  return request.post<unknown, PictureVO>('/picture/upload/url', data)
}

export function uploadPictureByBatch(data: PictureUploadByBatchRequest) {
  return request.post<unknown, number>('/picture/upload/batch', data)
}

export function updatePicture(data: PictureUpdateRequest) {
  return request.post<unknown, boolean>('/picture/update', data)
}

export function editPicture(data: PictureEditRequest) {
  return request.post<unknown, boolean>('/picture/edit', data)
}

export function deletePicture(data: DeleteRequest) {
  return request.post<unknown, boolean>('/picture/delete', data)
}

export function reviewPicture(data: PictureReviewRequest) {
  return request.post<unknown, boolean>('/picture/review', data)
}

export function listPictureTagCategory() {
  return request.get<unknown, PictureTagCategory>('/picture/tag_category')
}
