import request from '@/utils/request'

export function getRoomList(username) {
  return request({
    url: '/chat/room/list',
    method: 'get',
    params: { username }
  })
}

export function createRoom(data) {
  return request({
    url: '/chat/room/create',
    method: 'post',
    data
  })
}

export function joinRoom(data) {
  return request({
    url: '/chat/room/join',
    method: 'post',
    data
  })
}

export function leaveRoom(data) {
  return request({
    url: '/chat/room/leave',
    method: 'post',
    data
  })
}

export function getRoomMembers(roomId) {
  return request({
    url: `/chat/room/${roomId}/members`,
    method: 'get'
  })
}

export function getRoomHistory(roomId, params) {
  return request({
    url: `/chat/room/${roomId}/history`,
    method: 'get',
    params
  })
}

export function getUserRoomIds(username) {
  return request({
    url: '/chat/room/user-rooms',
    method: 'get',
    params: { username }
  })
}

export function saveRoomMessage(data) {
  return request({
    url: '/chat/room/message',
    method: 'post',
    data
  })
}
