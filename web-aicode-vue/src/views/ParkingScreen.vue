<!-- src/views/ParkingScreen.vue -->
<template>
  <div class="parking-screen-container">
    <div class="page-layout">
      <!-- 左侧：车位图 -->
      <div class="map-area">
        <div class="map-header">
          <h2 class="page-title">停车大屏</h2>
          <el-radio-group v-model="currentFloor" size="small">
            <el-radio-button
              v-for="f in floors"
              :key="f.id"
              :label="f.id"
            >{{ f.name }}</el-radio-button>
          </el-radio-group>
        </div>

        <!-- 图例 -->
        <div class="legend">
          <div
            v-for="(color, dept) in deptColors"
            :key="dept"
            class="legend-item"
          >
            <span class="legend-color" :style="{ background: color }"></span>
            <span>{{ dept }}</span>
          </div>
        </div>

        <!-- SVG 车位图 -->
        <div class="svg-wrapper">
          <svg
            :viewBox="`0 0 ${currentFloorData.width} ${currentFloorData.height}`"
            preserveAspectRatio="xMidYMid meet"
            class="parking-svg"
          >
            <!-- 地面背景 -->
            <rect
              x="0"
              y="0"
              :width="currentFloorData.width"
              :height="currentFloorData.height"
              fill="#eef0f3"
            />

            <!-- 车道装饰线（外环） -->
            <rect
              x="20"
              y="20"
              :width="currentFloorData.width - 40"
              :height="currentFloorData.height - 40"
              fill="none"
              stroke="#cfd6e0"
              stroke-width="2"
              stroke-dasharray="10 6"
              rx="12"
            />

            <!-- 车道带（浅色背景，体现车位行+车道行交替） -->
            <g v-for="(lane, i) in currentFloorData.lanes" :key="'lane-' + i">
              <rect
                :x="lane.x"
                :y="lane.y"
                :width="lane.w"
                :height="lane.h"
                fill="#dfe4ea"
                opacity="0.55"
                rx="3"
              />
            </g>

            <!-- 设施标注 -->
            <g v-for="(fac, i) in currentFloorData.facilities" :key="'fac-' + i">
              <rect
                :x="fac.x"
                :y="fac.y"
                :width="fac.w || 100"
                :height="fac.h || 28"
                :fill="facilityColor(fac.type)"
                rx="4"
                opacity="0.9"
              />
              <text
                :x="fac.x + (fac.w || 100) / 2"
                :y="fac.y + (fac.h || 28) / 2 + 4"
                text-anchor="middle"
                fill="#fff"
                font-size="12"
                font-weight="bold"
              >{{ fac.name }}</text>
            </g>

            <!-- 车位 -->
            <g v-for="(space, i) in currentSpaces" :key="'space-' + i">
              <rect
                :x="space.x"
                :y="space.y"
                :width="space.w"
                :height="space.h"
                :fill="deptColors[space.dept]"
                stroke="#fff"
                stroke-width="0.8"
                rx="2"
              />
              <text
                :x="space.x + space.w / 2"
                :y="space.y + space.h / 2 + 3"
                text-anchor="middle"
                :transform="space.vertical ? `rotate(-90 ${space.x + space.w / 2} ${space.y + space.h / 2})` : ''"
                fill="#fff"
                font-size="8"
                font-weight="600"
              >{{ space.label }}</text>
            </g>
          </svg>
        </div>
      </div>

      <!-- 右侧：文字信息 -->
      <div class="info-area">
        <div class="info-content">
          <h3>{{ currentFloorData.name }}（共{{ currentFloorData.total }}个车位）</h3>

          <el-divider content-position="left">空间布局</el-divider>
          <div class="layout-desc">{{ currentFloorData.description }}</div>

          <el-divider content-position="left">部门车位分配</el-divider>
          <div
            v-for="(alloc, i) in currentFloorData.allocations"
            :key="i"
            class="alloc-item"
          >
            <div class="alloc-dept">
              <span class="alloc-color" :style="{ background: deptColors[alloc.dept] || '#ccc' }"></span>
              {{ alloc.dept }}（{{ alloc.count }}个）
            </div>
            <div class="alloc-spaces">{{ alloc.spaces }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ParkingScreen',
  data() {
    return {
      currentFloor: '4F-B1',
      deptColors: {
        '综合管理部': '#F4B183',
        '开发一部': '#FFF2CC',
        '开发二部': '#FCE4D6',
        '开发三部': '#D9E1F2',
        '开发四部': '#BDD7EE',
        '技术平台研发部': '#E2EFDA',
        '测试部': '#E7E6E6',
        '应用维护部': '#E1D5E7',
        '临时车位': '#FF0000',
        '特殊车位': '#FFFFFF',
        '其他公司': '#D3D3D3'
      },
      floors: [
        // ==================== 4号楼 B1层 ====================
        // 地图方向：标准方向（上北、下南、左西、右东）
        // 车位方向定义（按PRD"尾对尾"逻辑）：
        // - 竖向车位(vh)：长边垂直（形如|），车位左右并排分布
        // - 横向车位(hv)：长边水平（形如—），车位上下堆叠分布
        // 车头朝向说明：
        // - 北墙车位：车尾贴北墙，车头朝南（朝下）
        // - 南墙车位：车尾贴南墙，车头朝北（朝上）
        // - 东墙车位：车尾贴东墙，车头朝西（朝左）
        // - 中部双排岛：上排车头朝北，下排车头朝南，尾对尾
        {
          id: '4F-B1',
          name: '4号楼B1层',
          total: 103,
          width: 1200,
          height: 560,
          facilities: [
            // 左侧（西侧）出入口
            { type: 'exit', name: '出口道闸', x: 5, y: 200, w: 70, h: 24 },
            { type: 'entrance', name: '入口道闸', x: 5, y: 240, w: 70, h: 24 },
            // 下方中间偏右（南侧中偏东位置）入口 - 按PRD修正版调整
            { type: 'entrance', name: '4号楼B1入口', x: 900, y: 520, w: 80, h: 24 },
            { type: 'entrance', name: '5号楼B1入口', x: 900, y: 545, w: 80, h: 24 }
          ],
          // ===== 环形车道系统（宽度60px）=====
          // 主车道从左侧双道闸进入，水平横穿中部，向东延伸
          // 外围有环形辅助车道（顶层、底层、右侧）
          lanes: [
            // 北车道（横向）：第1排(北墙)与第2排(中部上排岛)之间，Y=55
            { x: 80, y: 55, w: 1005, h: 60 },
            // 中部车道（横向）：第3排与第4排之间贯穿东西，对接左侧入口，Y=225
            { x: 80, y: 225, w: 1005, h: 55 },
            // 南车道（横向）：底层车位上方，Y=450（为贴墙车位留出空间）
            { x: 280, y: 450, w: 650, h: 60 },
            // 东车道（纵向）：右侧车位左侧，X=1085
            { x: 1085, y: 55, w: 60, h: 395 },
            // 西车道（纵向）：左侧入口道闸右侧，X=80，从第3排上方到南车道
            { x: 80, y: 170, w: 60, h: 280 }
          ],
          blocks: [
            // ===== 【北侧墙】单排靠墙，车尾贴北墙，车头朝南（朝下）=====
            // 按PRD修正版：A01-A08(其他公司8个) + B118-B119(综合管理部2个) + B114-B115(开发三部2个) + B110-B113(开发一部4个) + B116-B117(开发三部2个) + B081-B090(开发二部10个) + B091-B092(技术平台部2个) + B155(开发一部1个)
            // 其他公司 A01-A08（8个，灰色占位车位）
            { x: 80, y: 0, dir: 'vh', count: 8, dept: '其他公司', prefix: 'A', startNo: 1 },
            // 综合管理部 B118-B119（2个）
            { x: 280, y: 0, dir: 'vh', count: 2, dept: '综合管理部', prefix: 'B', startNo: 118 },
            // 开发三部 B114-B115（2个）
            { x: 330, y: 0, dir: 'vh', count: 2, dept: '开发三部', prefix: 'B', startNo: 114 },
            // 开发一部 B110-B113（4个）- 按PRD修正版调整
            { x: 380, y: 0, dir: 'vh', count: 4, dept: '开发一部', prefix: 'B', startNo: 110 },
            // 开发三部 B116-B117（2个）
            { x: 480, y: 0, dir: 'vh', count: 2, dept: '开发三部', prefix: 'B', startNo: 116 },
            // 开发二部 B081-B090（10个）
            { x: 530, y: 0, dir: 'vh', count: 10, dept: '开发二部', prefix: 'B', startNo: 81 },
            // 技术平台研发部 B091-B092（2个）
            { x: 780, y: 0, dir: 'vh', count: 2, dept: '技术平台研发部', prefix: 'B', startNo: 91 },
            // 开发一部 B155（1个）
            { x: 830, y: 0, dir: 'vh', count: 1, dept: '开发一部', prefix: 'B', startNo: 155 },

            // ===== 【中部联动四排尾对尾区】=====
            // ★新增：中部左侧占位车位（A19-A26，8个）- 按PRD修正版新增
            { x: 140, y: 115, dir: 'vh', count: 8, dept: '其他公司', prefix: 'A', startNo: 19 },
            // 上部双排岛：30个开发四部（上排15+下排15，尾对尾）
            { x: 340, y: 115, dir: 'vh', count: 15, dept: '开发四部', prefix: 'B', startNo: 48 },
            { x: 340, y: 170, dir: 'vh', count: 15, dept: '开发四部', prefix: 'B', startNo: 63 },
            // 下部双排岛左侧：3个开发四部 - 按PRD修正版调整（原为测试部）
            { x: 340, y: 280, dir: 'vh', count: 3, dept: '开发四部', prefix: 'B', startNo: 106 },
            // 下部双排岛右侧：18个开发四部（上排9+下排9，尾对尾）- 按PRD修正版调整（原为测试部）
            { x: 415, y: 280, dir: 'vh', count: 9, dept: '开发四部', prefix: 'B', startNo: 97 },
            { x: 415, y: 335, dir: 'vh', count: 9, dept: '开发四部', prefix: 'B', startNo: 77 },

            // ===== 【南侧墙左段】单排靠墙，车尾贴南墙，车头朝北（朝上）=====
            // A11-A18（8个，其他公司占位车位）- 贴南墙(y=510，墙在y=560)
            { x: 80, y: 510, dir: 'vh', count: 8, dept: '其他公司', prefix: 'A', startNo: 11 },

            // ===== 【南侧墙右段】单排靠墙，车尾贴南墙，车头朝北（朝上）=====
            // A010-A017（8个，其他公司占位车位）+ 10个开发四部 - 贴南墙
            { x: 400, y: 510, dir: 'vh', count: 8, dept: '其他公司', prefix: 'A', startNo: 10 },
            { x: 600, y: 510, dir: 'vh', count: 10, dept: '开发四部', prefix: 'B', startNo: 152 },

            // ===== 【左侧墙中上部】A09-A11（3个，其他公司占位车位）- 出口道闸上方，与中部上排岛同一行 =====
            { x: 80, y: 115, dir: 'vh', count: 3, dept: '其他公司', prefix: 'A', startNo: 9 },

            // ===== 【东侧墙】单排横向堆叠，车尾贴东墙(x=1200)，车头朝西（朝左）=====
            // 车位为hv类型，w=55,h=25，x=1145时占x=1145~1200，贴东墙
            // 区块1：1个应用维护部 B120
            { x: 1145, y: 0, dir: 'hv', count: 1, dept: '应用维护部', prefix: 'B', startNo: 120 },
            // 区块2：2个（B121应用维护部 + B122测试部）
            { x: 1145, y: 30, dir: 'hv', count: 1, dept: '应用维护部', prefix: 'B', startNo: 121 },
            { x: 1145, y: 55, dir: 'hv', count: 1, dept: '测试部', prefix: 'B', startNo: 122 },
            // 区块3：2个测试部 B123-B124
            { x: 1145, y: 90, dir: 'hv', count: 2, dept: '测试部', prefix: 'B', startNo: 123 },
            // 区块4：10个（含临1、临2，测试部为主）
            { x: 1145, y: 150, dir: 'hv', count: 2, dept: '临时车位', prefix: '临', startNo: 1 },
            { x: 1145, y: 210, dir: 'hv', count: 8, dept: '测试部', prefix: 'B', startNo: 125 },
            // 区块5：3个测试部 B149-B151 - 调整y=420，避开东车道(y=115~450)
            { x: 1145, y: 420, dir: 'hv', count: 3, dept: '测试部', prefix: 'B', startNo: 149 }
          ],
          description: '上北下南、左西右东标准方位布局。3条主车道构成闭环双向双车道系统：主车道1（北车道-横向）位于顶层车位与中央岛之间；主车道2（南车道-横向）位于底层车位与中央岛之间，中间垂直向下连接5号楼连通口（位于下方中间偏右位置）；主车道3（东车道-纵向）位于中央岛右侧末端与东侧墙壁之间。车位矩阵：矩阵1北侧靠墙直排23个（含8个占位车位）；矩阵2中央核心双岛51个（上排1×30+下排1×3+1×18，南北向，全部开发四部）；矩阵3南侧靠墙分段18个（左1×8占位+右1×10开发四部）；矩阵4东侧靠墙纵向18个（含临1临2）；中部左侧新增8个占位车位。总计103个我司车位+多组占位车位。',
          allocations: [
            { dept: '开发四部', count: 61, spaces: '中部双排岛51个(B048-B075、B077-B079、B097-B104、B106-B108)+底排10个(B152-B154)' },
            { dept: '测试部', count: 33, spaces: 'B093-B094、B096、B122-B151' },
            { dept: '开发二部', count: 10, spaces: 'B081-B090' },
            { dept: '开发三部', count: 4, spaces: 'B114-B115、B116-B117' },
            { dept: '开发一部', count: 5, spaces: 'B110-B113、B155' },
            { dept: '技术平台研发部', count: 2, spaces: 'B091-B092' },
            { dept: '应用维护部', count: 2, spaces: 'B120-B121' },
            { dept: '综合管理部', count: 2, spaces: 'B118-B119' }
          ]
        },

        // ==================== 5号楼 B1层 ====================
        // 按PRD修正版：统一按"上北下南"方向校正描述
        {
          id: '5F-B1',
          name: '5号楼B1层',
          total: 124,
          width: 1280,
          height: 760,
          facilities: [
            // 按PRD修正版调整设施位置
            { type: 'passage', name: '4号楼B1方向', x: 60, y: 10, w: 90, h: 24 },
            { type: 'elevator', name: '5号楼B1电房/电梯间', x: 900, y: 80, w: 110, h: 50 },
            { type: 'passage', name: 'B2入', x: 1100, y: 100, w: 80, h: 24 },
            { type: 'entrance', name: '中间出入口', x: 5, y: 380, w: 90, h: 24 },
            { type: 'entrance', name: '研修院入口', x: 1180, y: 380, w: 90, h: 24 },
            { type: 'elevator', name: '研修院方向电梯间', x: 400, y: 700, w: 110, h: 50 }
          ],
          // 车道带
          lanes: [
            // 左侧L形车道：第二列（从4号楼通过来），纵向
            { x: 80, y: 40, w: 40, h: 660 },
            // L形底部横向车道（右转）
            { x: 80, y: 660, w: 470, h: 40 },
            // 中间段各车道行
            { x: 300, y: 90, w: 540, h: 28 },   // 电梯下方车道
            { x: 300, y: 220, w: 540, h: 28 },  // 屁股对屁股①下方车道
            { x: 300, y: 310, w: 540, h: 28 },  // 单行车位下方车道
            { x: 300, y: 420, w: 540, h: 28 },  // 下B2通道下方车道
            { x: 300, y: 510, w: 540, h: 28 },  // 车位排下方车道
            { x: 300, y: 600, w: 540, h: 28 },  // 屁股对屁股②下方车道
            { x: 300, y: 690, w: 540, h: 28 },  // 屁股对屁股③下方车道
            // 中间段右侧南北向车道
            { x: 850, y: 30, w: 40, h: 700 }
          ],
          blocks: [
            // ===== 非我司占位车位（灰色）- 按PRD修正版调整 =====
            // 左侧边界排（西侧，"中间出入口"两侧）
            // "中间出入口"下侧（南侧）：9个竖向车位
            { x: 100, y: 400, dir: 'vh', count: 9, dept: '其他公司', prefix: 'C', startNo: 60 },
            // "中间出入口"上侧（北侧）：部分灰色占位车位
            { x: 100, y: 200, dir: 'vh', count: 13, dept: '其他公司', prefix: 'C', startNo: 70 },
            // 中央第1双排岛（标"11"）：左排11个+右排11个，全部灰色占位（含临5、临6）
            { x: 480, y: 200, dir: 'vh', count: 11, dept: '其他公司', prefix: 'C', startNo: 83 },
            { x: 530, y: 200, dir: 'vh', count: 11, dept: '其他公司', prefix: 'C', startNo: 94 },
            // 下侧边界排（南侧，"研修院方向电梯间"右边）：除临17外8个灰色
            { x: 520, y: 600, dir: 'hv', count: 8, dept: '其他公司', prefix: 'C', startNo: 105 },
            // 右侧边界排（东侧，"研修院入口"下侧）：9个车位中部分灰色
            { x: 1100, y: 400, dir: 'vh', count: 9, dept: '其他公司', prefix: 'C', startNo: 113 },

            // ===== 第二双排岛（标示数字"24"）- 按PRD修正版 =====
            // 左半排12个 + 右半排12个竖向车位尾对尾，右排顶端为临4
            { x: 630, y: 200, dir: 'vh', count: 12, dept: '开发三部', prefix: 'B', startNo: 1 },
            { x: 680, y: 200, dir: 'vh', count: 12, dept: '开发三部', prefix: 'B', startNo: 13 },

            // ===== 第三双排岛（标示数字"12"）=====
            { x: 780, y: 200, dir: 'vh', count: 6, dept: '开发三部', prefix: 'B', startNo: 25 },
            { x: 810, y: 200, dir: 'vh', count: 6, dept: '开发三部', prefix: 'B', startNo: 31 },

            // ===== 第四双排岛（标示数字"11"）=====
            { x: 880, y: 200, dir: 'vh', count: 6, dept: '开发三部', prefix: 'B', startNo: 37 },
            { x: 910, y: 200, dir: 'vh', count: 5, dept: '开发三部', prefix: 'B', startNo: 43 },

            // ===== 第五双排岛（标示数字"2"和"16"）=====
            // 上半截：2个绿色竖向车位（技术平台研发部）
            { x: 980, y: 200, dir: 'vh', count: 2, dept: '技术平台研发部', prefix: 'B', startNo: 156 },
            // 下半截：16个橙色竖向车位（综合管理部），两排各8个尾对尾
            { x: 980, y: 260, dir: 'vh', count: 8, dept: '综合管理部', prefix: 'B', startNo: 14 },
            { x: 980, y: 315, dir: 'vh', count: 8, dept: '综合管理部', prefix: 'B', startNo: 22 },

            // ===== 右侧边缘堆叠排（最右侧偏上）=====
            // 紧邻上侧"5号楼B1电房/梯间"下方：4个纵向堆叠的横向车位（开发一部）
            { x: 1050, y: 150, dir: 'hv', count: 4, dept: '开发一部', prefix: 'C', startNo: 45 },

            // ===== 右侧边界排（最右侧，"研修院入口"左侧）=====
            // C001特殊车位（白色）
            { x: 1100, y: 650, dir: 'vh', count: 1, dept: '特殊车位', prefix: 'C', startNo: 1 },

            // ===== 左侧边界排（西侧）=====
            // 综合管理部车位
            { x: 100, y: 100, dir: 'vh', count: 6, dept: '综合管理部', prefix: 'B', startNo: 38 },
            { x: 100, y: 160, dir: 'vh', count: 6, dept: '综合管理部', prefix: 'B', startNo: 44 },

            // ===== 下侧边界排（南侧）=====
            // 综合管理部车位
            { x: 200, y: 600, dir: 'vh', count: 10, dept: '综合管理部', prefix: 'B', startNo: 20 },

            // ===== 上侧边界排（北侧）=====
            // 技术平台研发部车位（B156-B170）
            { x: 300, y: 100, dir: 'vh', count: 15, dept: '技术平台研发部', prefix: 'B', startNo: 156 },

            // 临时车位（按PRD修正版：临4、临5、临6、临17）
            // 临17：下侧边界排（南侧）标示为"9"的纵向横车位中最顶上
            { x: 520, y: 570, dir: 'hv', count: 1, dept: '临时车位', prefix: '临', startNo: 17 },
            // 临5：中央第1岛（标"11"，占位岛）左排最顶上
            { x: 480, y: 200, dir: 'vh', count: 1, dept: '临时车位', prefix: '临', startNo: 5 },
            // 临6：中央第1岛（标"11"，占位岛）右排最顶上
            { x: 530, y: 200, dir: 'vh', count: 1, dept: '临时车位', prefix: '临', startNo: 6 },
            // 临4：中央第2岛（标"24"）右排最顶上
            { x: 680, y: 200, dir: 'vh', count: 1, dept: '临时车位', prefix: '临', startNo: 4 }
          ],
          description: '上北下南、左西右东标准方位布局（已将原图旋转90度校正）。左侧边界正中设有"中间出入口"，右侧边界正中设有"研修院入口"，下侧边界中偏左设有"研修院方向电梯间"，上侧边界中偏右设有"5号楼B1电房/电梯间"。中央区域由多个双排岛组成：第1岛（标"11"）为灰色占位岛，第2岛（标"24"）为开发三部车位，第3岛（标"12"）、第4岛（标"11"）为开发三部车位，第5岛上半截为技术平台研发部2个车位、下半截为综合管理部16个车位。右侧边缘有4个开发一部横向车位。总计124个我司车位+多组占位车位。',
          allocations: [
            { dept: '开发三部', count: 58, spaces: 'B001-B011、C003-C014、C019-C042、C049-C059' },
            { dept: '综合管理部', count: 29, spaces: 'B014-B019、B102-B103、B038-B043、B020-B029、B044-B047、C002' },
            { dept: '技术平台研发部', count: 24, spaces: 'B156-B170、C043-C044、C105-C108、B030-B031' },
            { dept: '开发一部', count: 12, spaces: 'C045-C048、C072-C079' },
            { dept: '特殊车位', count: 1, spaces: 'C001' },
            { dept: '临时车位', count: 4, spaces: '临4、临5、临6、临17' }
          ]
        },

        // ==================== 5号楼 B2层 A区 ====================
        {
          id: '5F-B2A',
          name: '5号楼B2层A区',
          total: 97,
          width: 1200,
          height: 800,
          facilities: [
            { type: 'exit', name: 'B2地库出口', x: 100, y: 760 },
            { type: 'exit', name: 'B2地库出口', x: 1000, y: 760 },
            { type: 'elevator', name: '5号楼B2电梯间', x: 900, y: 150, w: 110, h: 50 }
          ],
          // 车道：外环回路 + 中央南北分流道
          lanes: [
            // 外环车道
            { x: 60, y: 60, w: 1080, h: 32 },    // 上车道
            { x: 60, y: 700, w: 1080, h: 32 },   // 下车道
            { x: 60, y: 92, w: 32, h: 608 },     // 左车道
            { x: 1108, y: 92, w: 32, h: 608 },   // 右车道
            // 中央南北分流道
            { x: 580, y: 92, w: 40, h: 608 }
          ],
          blocks: [
            // ===== 北墙靠边排（最顶排）9个竖向车位 =====
            // 最右侧两车位为临8和临9
            { x: 200, y: 100, dir: 'vh', count: 7, dept: '开发二部', prefix: 'A', startNo: 1 },
            { x: 375, y: 100, dir: 'vh', count: 1, dept: '临时车位', prefix: '临', startNo: 8 },
            { x: 400, y: 100, dir: 'vh', count: 1, dept: '临时车位', prefix: '临', startNo: 9 },

            // ===== 西墙靠边排（最左排）横向车位 =====
            // 上方1个独立红框车位（临7）
            { x: 100, y: 200, dir: 'hv', count: 1, dept: '临时车位', prefix: '临', startNo: 7 },
            // 下方5个纵向堆叠车位
            { x: 100, y: 230, dir: 'hv', count: 5, dept: '开发二部', prefix: 'A', startNo: 7 },

            // ===== 左侧偏内单独纵排 =====
            // 6个竖向并排车位（A001-A006，开发二部）
            { x: 200, y: 200, dir: 'vh', count: 6, dept: '开发二部', prefix: 'A', startNo: 1 },

            // ===== 中部双排岛（尾对尾）=====
            // 位于电梯间下方，两排竖向车位尾对尾，每排各15个
            { x: 300, y: 200, dir: 'vh', count: 15, dept: '开发二部', prefix: 'A', startNo: 37 },
            { x: 300, y: 255, dir: 'vh', count: 15, dept: '开发二部', prefix: 'A', startNo: 52 },

            // ===== 中部单排对望排 =====
            // 在"临11/临10"正下方：26个并排竖向车位
            { x: 300, y: 350, dir: 'vh', count: 24, dept: '开发二部', prefix: 'A', startNo: 76 },
            { x: 780, y: 350, dir: 'vh', count: 2, dept: '开发二部', prefix: 'A', startNo: 11 },

            // ===== 临11和临10 =====
            { x: 300, y: 310, dir: 'vh', count: 1, dept: '临时车位', prefix: '临', startNo: 11 },
            { x: 750, y: 310, dir: 'vh', count: 1, dept: '临时车位', prefix: '临', startNo: 10 },

            // ===== 东墙靠边排（最右排）=====
            // 紧贴右侧墙：19个横向车位（应用维护部）
            { x: 1050, y: 100, dir: 'hv', count: 19, dept: '应用维护部', prefix: 'A', startNo: 14 },

            // ===== 电梯间旁特殊车位 =====
            { x: 1020, y: 200, dir: 'hv', count: 1, dept: '应用维护部', prefix: 'A', startNo: 36 },

            // ===== 非我司占位车位（灰色）=====
            // 最底端（南侧）大面积区域：约25个灰色占位车位
            { x: 200, y: 650, dir: 'vh', count: 25, dept: '其他公司', prefix: 'A', startNo: 118 },
            // 左下角（西南侧）出车口道闸附近：5个横向灰色占位车位
            { x: 100, y: 600, dir: 'hv', count: 5, dept: '其他公司', prefix: 'A', startNo: 143 }
          ],
          description: '上北下南、左西右东标准方位布局。该区域设有B2地库出口（左下角和右下角），右侧中上为5号楼B2电梯间。北墙靠边排9个车位（最右侧为临8、临9），西墙靠边排6个横向车位（最上方为临7），左侧偏内纵排6个车位（A001-A006），中部双排岛30个车位（尾对尾布局），中部单排对望排26个车位，东墙靠边排19个横向车位（应用维护部）。最底端有约25个灰色占位车位，左下角有5个横向灰色占位车位。总计97个我司车位+多组占位车位。',
          allocations: [
            { dept: '开发二部', count: 62, spaces: 'A001-A013、A037-A060、A076-A083、A084-A097' },
            { dept: '应用维护部', count: 35, spaces: 'A014-A032、A061-A075、A036' }
          ]
        },

        // ==================== 5号楼 B2层 B区 ====================
        {
          id: '5F-B2B',
          name: '5号楼B2层B区',
          total: 39,
          width: 1200,
          height: 400,
          facilities: [
            { type: 'exit', name: 'B2地库出口', x: 900, y: 300, w: 90, h: 24 }
          ],
          // 车道：倒L型
          lanes: [
            // 纵向车道（沿西墙）
            { x: 100, y: 60, w: 32, h: 300 },
            // 横向车道（底部）
            { x: 100, y: 320, w: 500, h: 32 }
          ],
          blocks: [
            // ===== 北墙靠边排（最顶排）=====
            // 30个竖向黄色车位（B018-B047，开发一部）
            { x: 150, y: 100, dir: 'vh', count: 30, dept: '开发一部', prefix: 'B', startNo: 18 },

            // ===== 西墙靠边排（最左排）=====
            // 6个横向黄色车位（B048-B053，开发一部）
            { x: 100, y: 150, dir: 'hv', count: 6, dept: '开发一部', prefix: 'B', startNo: 48 },

            // ===== 中部单排悬空岛 =====
            // 3个竖向黄色车位（B054-B056，开发一部）
            { x: 500, y: 200, dir: 'vh', count: 3, dept: '开发一部', prefix: 'B', startNo: 54 },

            // ===== 非我司占位车位（灰色）=====
            // 中下部及最底端大片区域：多排灰色占位车位
            { x: 150, y: 280, dir: 'vh', count: 30, dept: '其他公司', prefix: 'B', startNo: 57 },
            { x: 500, y: 280, dir: 'vh', count: 9, dept: '其他公司', prefix: 'B', startNo: 87 }
          ],
          description: '上北下南、左西右东标准方位布局。该区域格局相对独立，右侧中偏下设有B2地库出口及半弧形出库行车坡道。北墙靠边排30个竖向黄色车位（B018-B047），西墙靠边排6个横向黄色车位（B048-B053），中部单排悬空岛3个竖向黄色车位（B054-B056）。中下部及最底端有大面积灰色占位车位阵列。全部39个我司车位均属于开发一部。',
          allocations: [
            { dept: '开发一部', count: 39, spaces: 'B018-B056（包揽该区域全部车位）' }
          ]
        }
      ]
    }
  },
  computed: {
    currentFloorData() {
      return this.floors.find(f => f.id === this.currentFloor) || this.floors[0]
    },
    currentSpaces() {
      const floor = this.currentFloorData
      if (!floor) return []
      const spaces = []
      const hW = 38, hH = 24, hGap = 3
      const vW = 24, vH = 38, vGap = 3
      // vh：纵向车位（车头南北），沿水平方向排列 - 符合用户规格25×55
      const vhW = 25, vhH = 55, vhGap = 5
      // hv：横向车位（车头东西），沿垂直方向排列 - 符合用户规格55×25
      const hvW = 55, hvH = 25, hvGap = 5

      floor.blocks.forEach(block => {
        for (let i = 0; i < block.count; i++) {
          let x, y, w, h, vertical
          if (block.dir === 'h') {
            // 横向车位，水平排列
            w = hW; h = hH; vertical = false
            x = block.x + i * (hW + hGap)
            y = block.y
          } else if (block.dir === 'vh') {
            // 纵向车位（车头南北），水平排列
            w = vhW; h = vhH; vertical = true
            x = block.x + i * (vhW + vhGap)
            y = block.y
          } else if (block.dir === 'hv') {
            // 横向车位（车头东西），垂直排列
            w = hvW; h = hvH; vertical = false
            x = block.x
            y = block.y + i * (hvH + hvGap)
          } else {
            // 纵向车位，垂直排列
            w = vW; h = vH; vertical = true
            x = block.x
            y = block.y + i * (vH + vGap)
          }
          const no = block.startNo + i
          const label = block.prefix === '临'
            ? '临' + no
            : block.prefix + String(no).padStart(3, '0')
          spaces.push({ x, y, w, h, dept: block.dept, label, vertical })
        }
      })
      return spaces
    }
  },
  methods: {
    facilityColor(type) {
      const map = {
        entrance: '#67C23A',
        exit: '#F56C6C',
        elevator: '#409EFF',
        passage: '#909399'
      }
      return map[type] || '#909399'
    }
  }
}
</script>

<style scoped>
.parking-screen-container {
  height: calc(100% - 32px);
  box-sizing: border-box;
  overflow: hidden;
  padding: 20px;
  margin: 16px;
  border-radius: 14px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border: 1px solid #dbe7f7;
  box-shadow: 0 10px 22px rgba(21, 52, 105, 0.08);
}

.page-layout {
  display: flex;
  height: 100%;
}

/* ============ 左侧：车位图 ============ */
.map-area {
  flex: 3;
  padding-right: 20px;
  border-right: 1px solid #ebeef5;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.map-area::-webkit-scrollbar {
  width: 6px;
}
.map-area::-webkit-scrollbar-thumb {
  background: #c8d8f0;
  border-radius: 8px;
}

.map-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  flex-wrap: wrap;
  gap: 10px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  color: #1f2d3d;
}

.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
  padding: 10px 14px;
  margin-bottom: 14px;
  background: #f5f7fa;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.legend-item {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #606266;
}

.legend-color {
  display: inline-block;
  width: 14px;
  height: 14px;
  border-radius: 3px;
  margin-right: 6px;
  border: 1px solid rgba(0, 0, 0, 0.1);
}

.svg-wrapper {
  flex: 1;
  min-height: 400px;
  background: #eef0f3;
  border-radius: 10px;
  border: 1px solid #dcdfe6;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px;
}

.parking-svg {
  width: 100%;
  height: 100%;
  max-height: 70vh;
}

/* ============ 右侧：文字信息 ============ */
.info-area {
  flex: 1;
  padding-left: 20px;
  overflow-y: auto;
  background-color: #fafafa;
  min-width: 260px;
}

.info-area::-webkit-scrollbar {
  width: 6px;
}
.info-area::-webkit-scrollbar-thumb {
  background: #c8d8f0;
  border-radius: 8px;
}

.info-content h3 {
  font-size: 16px;
  color: #303133;
  margin-top: 0;
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 2px solid #409EFF;
}

.info-content ::v-deep .el-divider__text {
  font-size: 13px;
  font-weight: 600;
  color: #409EFF;
  background-color: #fafafa;
}

.layout-desc {
  color: #606266;
  font-size: 12px;
  line-height: 1.8;
  padding: 0 4px;
  margin-bottom: 10px;
}

.alloc-item {
  margin-bottom: 14px;
  line-height: 1.6;
}

.alloc-dept {
  font-weight: bold;
  color: #303133;
  font-size: 13px;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
}

.alloc-color {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 3px;
  margin-right: 6px;
  border: 1px solid rgba(0, 0, 0, 0.1);
}

.alloc-spaces {
  color: #606266;
  font-size: 12px;
  padding-left: 18px;
  word-break: break-all;
}

::v-deep .el-radio-button__inner {
  border-radius: 0;
}
</style>
