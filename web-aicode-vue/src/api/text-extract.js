import request from '@/utils/request';

/**
 * 上传 Word/Excel 文件
 */
export function uploadFile(file) {
  const formData = new FormData();
  formData.append('file', file);
  return request({
    url: '/text-extract/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}

/**
 * 获取已上传的文件列表
 */
export function getFileList() {
  return request({
    url: '/text-extract/files',
    method: 'get'
  });
}

/**
 * 删除指定文件
 */
export function deleteFile(fileName) {
  return request({
    url: '/text-extract/file',
    method: 'delete',
    params: { fileName }
  });
}
