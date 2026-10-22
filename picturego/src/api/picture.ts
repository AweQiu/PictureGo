import request from './request'

export function listPictureByPage(data: PictureQueryRequest) {
  return request.post<unknown, Page<Picture>>('/picture/list/page', data)
}

export function listPictureVOByPage(data: PictureQueryRequest) {
  return request.post<unknown, Page<PictureVO>>('/picture/list/page/vo', data)
}

export function getPictureById(id: number) {
  return request.get<unknown, Picture>('/picture/get', { params: { id } })
}

export function updatePicture(data: PictureUpdateRequest) {
  return request.post<unknown, boolean>('/picture/update', data)
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
