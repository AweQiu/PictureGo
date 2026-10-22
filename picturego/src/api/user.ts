import request from './request'

export function userLogin(data: UserLoginRequest) {
  return request.post<unknown, LoginUserVO>('/user/login', data)
}

export function userRegister(data: UserRegisterRequest) {
  return request.post<unknown, number>('/user/register', data)
}

export function userLogout() {
  return request.post<unknown, boolean>('/user/logout')
}

export function getLoginUser() {
  return request.get<unknown, LoginUserVO>('/user/get/login')
}

export function addUser(data: UserAddRequest) {
  return request.post<unknown, number>('/user/add', data)
}

export function updateUser(data: UserUpdateRequest) {
  return request.post<unknown, boolean>('/user/update', data)
}

export function deleteUser(data: DeleteRequest) {
  return request.post<unknown, boolean>('/user/delete', null, { params: data })
}

export function listUserVOByPage(data: UserQueryRequest) {
  return request.post<unknown, Page<UserVO>>('/user/list/page/vo', data)
}

export function getUserVOById(id: number | string) {
  return request.get<unknown, UserVO>('/user/get/vo', { params: { id } })
}
