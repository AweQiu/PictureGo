/** 后端统一响应包装 */
interface Result<T> {
  code: number
  data: T
  message: string
}

/** 分页结果 */
interface Page<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/** 登录用户信息（后端 Long 序列化为字符串，id 用 string） */
interface LoginUserVO {
  id: string
  userAccount: string
  userName: string
  userAvatar: string
  userProfile: string
  userRole: string
  editTime: string
  createTime: string
  updateTime: string
}

/** 用户实体（管理端 getUserById 返回，含编辑时间） */
interface User {
  id: string
  userAccount: string
  userName: string
  userAvatar: string
  userProfile: string
  userRole: string
  editTime: string
  createTime: string
  updateTime: string
  isDelete: number
}

/** 用户视图（脱敏） */
interface UserVO {
  id: string
  userAccount: string
  userName: string
  userAvatar: string
  userProfile: string
  userRole: string
  createTime: string
}

interface UserLoginRequest {
  userAccount: string
  userPassword: string
}

interface UserRegisterRequest {
  userAccount: string
  userPassword: string
  checkPassword: string
}

interface UserAddRequest {
  userName?: string
  userAccount?: string
  userAvatar?: string
  userProfile?: string
  userRole?: string
}

interface UserUpdateRequest {
  id?: number | string
  userName?: string
  userAccount?: string
  userAvatar?: string
  userProfile?: string
  userRole?: string
}

interface UserQueryRequest {
  current?: number
  pageSize?: number
  sortField?: string
  sortOrder?: string
  id?: number | string
  userName?: string
  userAccount?: string
  userProfile?: string
  userRole?: string
}

interface DeleteRequest {
  id?: number | string
}

/** 图片（管理端，含审核字段） */
interface Picture {
  id: string
  url: string
  thumbnailUrl: string
  name: string
  introduction: string
  category: string
  tags: string
  reviewStatus: number
  reviewMessage: string
  reviewerId: string
  reviewTime: string
  picSize: number
  picWidth: number
  picHeight: number
  picScale: number
  picFormat: string
  userId: string
  spaceId: string
  createTime: string
  editTime: string
  updateTime: string
  isDelete: number
}

/** 图片视图（展示用） */
interface PictureVO {
  id: string
  url: string
  thumbnailUrl: string
  name: string
  introduction: string
  category: string
  tags: string[]
  picSize: number
  picWidth: number
  picHeight: number
  picScale: number
  picFormat: string
  userId: string
  spaceId: string
  createTime: string
  editTime: string
  updateTime: string
  userVO: UserVO
}

interface PictureQueryRequest {
  current?: number
  pageSize?: number
  sortField?: string
  sortOrder?: string
  id?: number | string
  name?: string
  introduction?: string
  category?: string
  tags?: string[]
  reviewStatus?: number
  reviewMessage?: string
  reviewerId?: number | string
  picSize?: number
  picWidth?: number
  picHeight?: number
  picScale?: number
  picFormat?: string
  searchText?: string
  userId?: number | string
  spaceId?: number | string
  startEditTime?: string
  endEditTime?: string
}

interface PictureEditRequest {
  id?: number | string
  name?: string
  introduction?: string
  category?: string
  tags?: string[]
}

interface PictureUpdateRequest {
  id?: number | string
  name?: string
  introduction?: string
  category?: string
  tags?: string[]
}

interface PictureUploadRequest {
  id?: number | string
  url?: string
  name?: string
  introduction?: string
  category?: string
  tags?: string[]
  spaceId?: number | string
}

interface PictureUploadByBatchRequest {
  searchText?: string
  count?: number
  namePrefix?: string
}

interface PictureReviewRequest {
  id?: number | string
  reviewStatus?: number
  reviewMessage?: string
}

interface PictureTagCategory {
  tags: string[]
  categories: string[]
}

/** 空间实体（管理端） */
interface Space {
  id: string
  spaceName: string
  spaceLevel: number
  maxSize: number
  maxCount: number
  totalSize: number
  totalCount: number
  userId: string
  createTime: string
  editTime: string
  updateTime: string
  isDelete: number
}

/** 空间视图（展示用） */
interface SpaceVO {
  id: string
  spaceName: string
  spaceLevel: number
  maxSize: number
  maxCount: number
  totalSize: number
  totalCount: number
  userId: string
  createTime: string
  editTime: string
  updateTime: string
  userVO: UserVO
}

/** 空间级别说明 */
interface SpaceLevel {
  value: number
  text: string
  maxCount: number
  maxSize: number
}

interface SpaceAddRequest {
  spaceName?: string
  spaceLevel?: number
  /** 管理员为指定用户创建空间时携带 */
  userId?: number | string
}

interface SpaceEditRequest {
  id?: number | string
  spaceName?: string
}

interface SpaceUpdateRequest {
  id?: number | string
  spaceName?: string
  spaceLevel?: number
  maxSize?: number
  maxCount?: number
}

interface SpaceQueryRequest {
  current?: number
  pageSize?: number
  sortField?: string
  sortOrder?: string
  id?: number | string
  userId?: number | string
  spaceName?: string
  spaceLevel?: number
}
