import JSZip from 'jszip';

/**
 * 解析 Xmind 文件并转换为 mind-map 组件需要的结构
 * @param {File} file 
 * @returns {Promise<Object>}
 */
export async function parseXmindToMap(file) {
  const zip = new JSZip();
  const zipContent = await zip.loadAsync(file);
  const jsonFile = zipContent.file("content.json");
  
  if (!jsonFile) throw new Error("不支持旧版 Xmind (XML)，请使用新版文件");
  
  const rawData = JSON.parse(await jsonFile.async("string"));
  const rootTopic = rawData[0].rootTopic;

  // 递归转换：将 Xmind 结构转为 mind-map 组件需要的 data + children 结构
  const transform = (node) => ({
    data: {
      text: node.title,
      expand: true,
      note: (node.notes && node.notes.plain && node.notes.plain.content) || ""
    },
    children: (node.children && node.children.attached ? node.children.attached : []).map(transform)
  });

  return transform(rootTopic);
}
