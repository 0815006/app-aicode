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
        '其他公司': '#7F8C8D'
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
            // 右下角（东南侧）入口
            { type: 'entrance', name: '4号楼B1入口', x: 1050, y: 520, w: 80, h: 24 },
            { type: 'entrance', name: '5号楼B1入口', x: 1050, y: 545, w: 80, h: 24 }
          ],
          // ===== 环形车道系统（宽度60px）=====
          // 主车道从左侧双道闸进入，水平横穿中部，向东延伸
          // 外围有环形辅助车道（顶层、底层、右侧）
          lanes: [
            // 北车道（横向）：顶层车位下方，Y=55
            { x: 80, y: 55, w: 1005, h: 60 },
            // 南车道（横向）：底层车位上方，Y=345
            { x: 80, y: 345, w: 1005, h: 60 },
            // 东车道（纵向）：右侧车位左侧，X=1085
            { x: 1085, y: 55, w: 60, h: 350 },
            // 西车道（纵向）：左侧入口右侧，X=80
            { x: 80, y: 55, w: 60, h: 350 }
          ],
          blocks: [
            // ===== 【北侧墙】单排靠墙，车尾贴北墙，车头朝南（朝下）=====
            // 按PRD：A01-A08(其他公司8个) + B118-B119(综合管理部2个) + B114-B115(开发三部2个) + B110-B112(开发一部3个) + B116-B117(开发三部2个) + B081-B090(开发二部10个) + B091-B092(技术平台部2个) + B155(开发一部1个)
            // 其他公司 A01-A08（8个，灰色占位车位）
            { x: 80, y: 0, dir: 'vh', count: 8, dept: '其他公司', prefix: 'A', startNo: 1 },
            // 综合管理部 B118-B119（2个）
            { x: 280, y: 0, dir: 'vh', count: 2, dept: '综合管理部', prefix: 'B', startNo: 118 },
            // 开发三部 B114-B115（2个）
            { x: 330, y: 0, dir: 'vh', count: 2, dept: '开发三部', prefix: 'B', startNo: 114 },
            // 开发一部 B110-B112（3个）
            { x: 380, y: 0, dir: 'vh', count: 3, dept: '开发一部', prefix: 'B', startNo: 110 },
            // 开发三部 B116-B117（2个）
            { x: 455, y: 0, dir: 'vh', count: 2, dept: '开发三部', prefix: 'B', startNo: 116 },
            // 开发二部 B081-B090（10个）
            { x: 505, y: 0, dir: 'vh', count: 10, dept: '开发二部', prefix: 'B', startNo: 81 },
            // 技术平台研发部 B091-B092（2个）
            { x: 755, y: 0, dir: 'vh', count: 2, dept: '技术平台研发部', prefix: 'B', startNo: 91 },
            // 开发一部 B155（1个）
            { x: 805, y: 0, dir: 'vh', count: 1, dept: '开发一部', prefix: 'B', startNo: 155 },

            // ===== 【中部联动四排尾对尾区】=====
            // 上部双排岛：30个开发四部（上排15+下排15，尾对尾）
            { x: 140, y: 115, dir: 'vh', count: 15, dept: '开发四部', prefix: 'B', startNo: 48 },
            { x: 140, y: 170, dir: 'vh', count: 15, dept: '开发四部', prefix: 'B', startNo: 63 },
            // 下部双排岛左侧：3个测试部
            { x: 140, y: 280, dir: 'vh', count: 3, dept: '测试部', prefix: 'B', startNo: 122 },
            // 下部双排岛右侧：18个测试部（上排9+下排9，尾对尾）
            { x: 215, y: 280, dir: 'vh', count: 9, dept: '测试部', prefix: 'B', startNo: 125 },
            { x: 215, y: 335, dir: 'vh', count: 9, dept: '测试部', prefix: 'B', startNo: 134 },

            // ===== 【南侧墙左段】单排靠墙，车尾贴南墙，车头朝北（朝上）=====
            // A11-A18（8个，其他公司占位车位）
            { x: 80, y: 405, dir: 'vh', count: 8, dept: '其他公司', prefix: 'A', startNo: 11 },

            // ===== 【南侧墙右段】单排靠墙，车尾贴南墙，车头朝北（朝上）=====
            // A010-A017（8个，其他公司占位车位）+ 10个开发四部
            { x: 400, y: 405, dir: 'vh', count: 8, dept: '其他公司', prefix: 'A', startNo: 10 },
            { x: 600, y: 405, dir: 'vh', count: 10, dept: '开发四部', prefix: 'B', startNo: 97 },

            // ===== 【左侧墙中上部】A09-A10（2个，其他公司占位车位）=====
            { x: 80, y: 115, dir: 'vh', count: 2, dept: '其他公司', prefix: 'A', startNo: 9 },

            // ===== 【东侧墙】单排横向堆叠，车尾贴东墙，车头朝西（朝左）=====
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
            // 区块5：3个测试部 B149-B151
            { x: 1145, y: 450, dir: 'hv', count: 3, dept: '测试部', prefix: 'B', startNo: 149 }
          ],
          description: '上北下南、左西右东标准方位布局。3条主车道构成闭环双向双车道系统：主车道1（北车道-横向）位于顶层车位与中央岛之间；主车道2（南车道-横向）位于底层车位与中央岛之间，中间垂直向下连接5号楼连通口；主车道3（东车道-纵向）位于中央岛右侧末端与东侧墙壁之间。车位矩阵：矩阵1北侧靠墙直排22个（1×22，南北向）；矩阵2中央核心双岛48个（上排1×30+下排1×18，南北向）；矩阵3南侧靠墙分段20个（左1×10+右1×10，南北向）；矩阵4东侧靠墙纵向11个（11×1，东西向，含临1临2）；角隅补位2个（南北向）。总计103个车位。',
          allocations: [
            { dept: '开发四部', count: 46, spaces: 'B048-B075、B077-B079、B097-B104、B106-B108、B152-B154' },
            { dept: '测试部', count: 33, spaces: 'B093-B094、B096、B122-B151' },
            { dept: '开发二部', count: 10, spaces: 'B081-B090' },
            { dept: '开发三部', count: 4, spaces: 'B114-B115、B116-B117' },
            { dept: '开发一部', count: 4, spaces: 'B110-B112、B155' },
            { dept: '技术平台研发部', count: 2, spaces: 'B091-B092' },
            { dept: '应用维护部', count: 2, spaces: 'B120-B121' },
            { dept: '综合管理部', count: 2, spaces: 'B118-B119' }
          ]
        },

        // ==================== 5号楼 B1层 ====================
        {
          id: '5F-B1',
          name: '5号楼B1层',
          total: 124,
          width: 1280,
          height: 760,
          facilities: [
            { type: 'passage', name: '4号楼B1方向通道', x: 60, y: 10, w: 90, h: 24 },
            { type: 'elevator', name: '5号楼B1电梯间', x: 900, y: 80, w: 110, h: 50 },
            { type: 'passage', name: '下B2通道(双向)', x: 560, y: 360, w: 110, h: 50 },
            { type: 'entrance', name: '车库入口(地面)', x: 1180, y: 280, w: 90, h: 24 },
            { type: 'entrance', name: '中间出入口', x: 5, y: 200, w: 90, h: 24 },
            { type: 'entrance', name: '研修院入口', x: 5, y: 500, w: 90, h: 24 }
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
            // ===== 非我司占位车位（灰色）=====
            // 左侧边界排（西侧，"中间出入口"下侧）：9个竖向车位
            { x: 100, y: 220, dir: 'vh', count: 9, dept: '其他公司', prefix: 'C', startNo: 60 },
            // 中央第1双排岛（标"11"）：左排11个+右排11个，全部灰色占位
            { x: 480, y: 200, dir: 'vh', count: 11, dept: '其他公司', prefix: 'C', startNo: 70 },
            { x: 530, y: 200, dir: 'vh', count: 11, dept: '其他公司', prefix: 'C', startNo: 81 },
            // 下侧边界排（南侧，"研修院方向电梯间"右边）：除临17外8个灰色
            { x: 50, y: 430, dir: 'hv', count: 8, dept: '其他公司', prefix: 'C', startNo: 92 },
            // 右侧边界排（东侧，"研修院入口"下侧）：9个车位中部分灰色
            { x: 1100, y: 600, dir: 'vh', count: 9, dept: '其他公司', prefix: 'C', startNo: 100 },

            // ===== 南部大矩阵密集区（东西向停车，60-70个）=====
            // 8列垂直方向的条状区块，每列内部车位上下叠放
            { x: 100, y: 400, dir: 'hv', count: 8, dept: '开发三部', prefix: 'B', startNo: 1 },
            { x: 150, y: 400, dir: 'hv', count: 8, dept: '开发三部', prefix: 'B', startNo: 9 },
            { x: 200, y: 400, dir: 'hv', count: 8, dept: '开发三部', prefix: 'B', startNo: 17 },
            { x: 250, y: 400, dir: 'hv', count: 8, dept: '开发三部', prefix: 'B', startNo: 25 },
            { x: 300, y: 400, dir: 'hv', count: 8, dept: '开发三部', prefix: 'B', startNo: 33 },
            { x: 350, y: 400, dir: 'hv', count: 8, dept: '开发三部', prefix: 'B', startNo: 41 },
            { x: 400, y: 400, dir: 'hv', count: 8, dept: '开发三部', prefix: 'B', startNo: 49 },
            { x: 450, y: 400, dir: 'hv', count: 8, dept: '开发三部', prefix: 'B', startNo: 57 },

            // ===== 中西部横向阵列（35个，南北向停车）=====
            // 北排11个（含临5）
            { x: 100, y: 200, dir: 'vh', count: 11, dept: '综合管理部', prefix: 'B', startNo: 14 },
            // 南排24个（含临4）
            { x: 100, y: 250, dir: 'vh', count: 24, dept: '综合管理部', prefix: 'B', startNo: 20 },

            // ===== 东北部电梯间及通道周边（25个）=====
            // 电梯间西侧12个（东西向停车）
            { x: 800, y: 100, dir: 'hv', count: 12, dept: '技术平台研发部', prefix: 'C', startNo: 43 },
            // 电梯间正南侧11个（东西向停车）
            { x: 800, y: 150, dir: 'hv', count: 11, dept: '技术平台研发部', prefix: 'C', startNo: 55 },
            // 电梯间东侧墙边2个
            { x: 1020, y: 100, dir: 'v', count: 2, dept: '技术平台研发部', prefix: 'C', startNo: 105 },

            // ===== 东南角（4个纵向+9个横向）=====
            { x: 1100, y: 400, dir: 'v', count: 4, dept: '开发三部', prefix: 'C', startNo: 19 },
            { x: 1100, y: 500, dir: 'hv', count: 9, dept: '开发三部', prefix: 'C', startNo: 30 },

            // ===== 西南角（9个南北向+C001特殊车位）=====
            { x: 100, y: 650, dir: 'vh', count: 9, dept: '开发一部', prefix: 'C', startNo: 3 },
            // C001特殊车位（白色）
            { x: 200, y: 650, dir: 'vh', count: 1, dept: '特殊车位', prefix: 'C', startNo: 1 },

            // 临时车位（按PRD：临4、临5、临6、临17）
            // 临17：左侧墙（南墙）9个横车位最顶上
            { x: 50, y: 400, dir: 'hv', count: 1, dept: '临时车位', prefix: '临', startNo: 17 },
            // 临5、临6：中央第1岛（标"11"）左排和右排最顶上
            { x: 480, y: 200, dir: 'vh', count: 1, dept: '临时车位', prefix: '临', startNo: 5 },
            { x: 530, y: 200, dir: 'vh', count: 1, dept: '临时车位', prefix: '临', startNo: 6 },
            // 临4：中央第2岛（标"24"）右排最顶上
            { x: 630, y: 200, dir: 'vh', count: 1, dept: '临时车位', prefix: '临', startNo: 4 }
          ],
          description: '上北下南、左西右东标准方位布局。总体格局呈现"下半部高密度矩阵、上半部规整长排与零散交错"的特征。南部大矩阵密集区约60-70个车位（东西向停车），由8列垂直方向的条状区块并排组成。中西部横向阵列35个车位（南北向停车），分为北排11个和南排24个，最西侧端头设有临5和临4。东北部电梯间及通道周边约25个车位，电梯间西侧12个（东西向），电梯间正南侧11个（东西向），电梯间东侧墙边2个。东南角由4个纵向车位和9个横向车位拼成局部小交错区。西南角9个车位（南北向停车）及C001。',
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
            { type: 'exit', name: 'B2地库出口', x: 480, y: 760 },
            { type: 'exit', name: 'B2地库出口', x: 900, y: 760 },
            { type: 'elevator', name: 'B2电梯间', x: 680, y: 220 }
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
            // ===== 中央核心双岛区（61个，南北向停车）=====
            // 核心北排 - 西岛15个
            { x: 200, y: 150, dir: 'vh', count: 15, dept: '开发二部', prefix: 'A', startNo: 1 },
            // 核心北排 - 东岛15个
            { x: 650, y: 150, dir: 'vh', count: 15, dept: '开发二部', prefix: 'A', startNo: 37 },
            // 核心南排 - 西岛5个
            { x: 200, y: 250, dir: 'vh', count: 5, dept: '开发二部', prefix: 'A', startNo: 76 },
            // 核心南排 - 东岛26个（含A090开发二部 + A091-A117其他公司）
            { x: 650, y: 250, dir: 'vh', count: 1, dept: '开发二部', prefix: 'A', startNo: 90 },
            { x: 680, y: 250, dir: 'vh', count: 20, dept: '其他公司', prefix: 'A', startNo: 91 },

            // ===== 西侧靠墙直排（15个，东西向停车，含3个临时位）=====
            // 临8、临9（最北端）
            { x: 50, y: 100, dir: 'hv', count: 2, dept: '临时车位', prefix: '临', startNo: 8 },
            // 往南9个车位
            { x: 50, y: 150, dir: 'hv', count: 9, dept: '开发二部', prefix: 'A', startNo: 7 },
            // 临7 + 4个零散车位
            { x: 50, y: 400, dir: 'hv', count: 1, dept: '临时车位', prefix: '临', startNo: 7 },
            { x: 50, y: 450, dir: 'hv', count: 4, dept: '开发二部', prefix: 'A', startNo: 32 },

            // ===== 东侧与北侧边界区（21个，含2个临时位）=====
            // 电梯间正南方：临11和临10（南北向停车）
            { x: 620, y: 290, dir: 'vh', count: 1, dept: '临时车位', prefix: '临', startNo: 11 },
            { x: 660, y: 290, dir: 'vh', count: 1, dept: '临时车位', prefix: '临', startNo: 10 },
            // 东侧内圈直排：19个车位（东西向停车）
            { x: 1100, y: 100, dir: 'hv', count: 19, dept: '应用维护部', prefix: 'A', startNo: 14 },

            // ===== 非我司占位车位（灰色）=====
            // 最底端（南侧）大面积区域：约25个灰色占位车位
            { x: 200, y: 650, dir: 'vh', count: 25, dept: '其他公司', prefix: 'A', startNo: 118 },
            // 左下角（西南侧）出车口道闸附近：5个横向灰色占位车位
            { x: 50, y: 600, dir: 'hv', count: 5, dept: '其他公司', prefix: 'A', startNo: 143 }
          ],
          description: '上北下南、左西右东标准方位布局。该区域设有B2地库出口（正下方中部和右下角），中上偏右为5号楼B2电梯间。车道呈外环回路，环绕中央的核心大车位岛，中央有一条南北分流道将车位岛对称切分为"西岛"和"东岛"。中央核心双岛区共61个车位（南北向停车）：核心北排西岛15个、东岛15个；核心南排西岛5个、东岛26个（含A090开发二部+A091-A117其他公司）。西侧靠墙直排15个车位（东西向停车），最北端为临8、临9，往南9个车位，再往南为临7和4个零散车位。东侧内圈直排19个车位（东西向停车），电梯间正下方为临10、临11。',
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
            { type: 'entrance', name: 'B2地库出入口', x: 900, y: 60 }
          ],
          // 车道：倒L型
          lanes: [
            // 纵向车道（沿西墙）
            { x: 100, y: 60, w: 32, h: 300 },
            // 横向车道（底部）
            { x: 100, y: 320, w: 500, h: 32 }
          ],
          blocks: [
            // ===== 西侧双列大阵列（39个，东西向停车）=====
            // 左侧长排（紧贴西墙）30个
            { x: 150, y: 100, dir: 'h', count: 30, dept: '开发一部', prefix: 'B', startNo: 18 },
            // 右侧短排 - 北段6个
            { x: 500, y: 100, dir: 'h', count: 6, dept: '开发一部', prefix: 'B', startNo: 42 },
            // 右侧短排 - 南段3个
            { x: 500, y: 200, dir: 'h', count: 3, dept: '开发一部', prefix: 'B', startNo: 48 },

            // ===== 非我司占位车位（灰色）=====
            // 中下部及最底端大片区域：多排灰色占位车位
            { x: 150, y: 280, dir: 'h', count: 30, dept: '其他公司', prefix: 'B', startNo: 57 },
            { x: 500, y: 280, dir: 'h', count: 6, dept: '其他公司', prefix: 'B', startNo: 87 }
          ],
          description: '上北下南、左西右东标准方位布局。该区域格局相对独立，配有B2地库出入口（右侧偏上，东北侧）。车道呈倒L型围绕车位阵列。核心区域由两行双面相对的水平网格组成：最顶行一字排开共30个车位（东西向停车），第二行被分成两段（左侧6个、右侧3个），中间留空作为车道或建筑立柱区。全部39个车位均属于开发一部。',
          allocations: [
            { dept: '开发一部', count: 39, spaces: 'B018–B056（包揽该区域全部车位）' }
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
