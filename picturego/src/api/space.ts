import request from './request'

export function addSpace(data: SpaceAddRequest) {
  return request.post<unknown, number | string>('/space/add', data)
}

export function deleteSpace(data: DeleteRequest) {
  return request.post<unknown, boolean>('/space/delete', data)
}

export function editSpace(data: SpaceEditRequest) {
  return request.post<unknown, boolean>('/space/edit', data)
}

export function updateSpace(data: SpaceUpdateRequest) {
  return request.post<unknown, boolean>('/space/update', data)
}

export function getSpaceById(id: number | string) {
  return request.get<unknown, Space>('/space/get', { params: { id } })
}

export function getSpaceVOById(id: number | string) {
  return request.get<unknown, SpaceVO>('/space/get/vo', { params: { id } })
}

export function listSpaceLevel() {
  return request.get<unknown, SpaceLevel[]>('/space/list/level')
}

export function listSpaceByPage(data: SpaceQueryRequest) {
  return request.post<unknown, Page<Space>>('/space/list/page', data)
}

export function listSpaceVOByPage(data: SpaceQueryRequest) {
  return request.post<unknown, Page<SpaceVO>>('/space/list/page/vo', data)
}
