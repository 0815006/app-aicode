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

            <!-- 整体偏移包裹层（xOffset/yOffset用于微调使车位边缘对齐虚线边框） -->
            <g :transform="'translate(' + (currentFloorData.xOffset || 0) + ',' + (currentFloorData.yOffset || 0) + ')'">

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

            <!-- 墙体（四角及岛内间隔墙体） -->
            <g v-for="(wall, i) in currentWalls" :key="'wall-' + i">
              <rect
                :x="wall.x"
                :y="wall.y"
                :width="wall.w"
                :height="wall.h"
                fill="#9aa5b1"
                opacity="0.85"
                rx="2"
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
                :stroke="space.tandem ? '#FF8C00' : '#fff'"
                :stroke-width="space.tandem ? 1.6 : 0.8"
                :stroke-dasharray="space.tandem ? '3 2' : 'none'"
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
              <!-- 子母车位标记 -->
              <text
                v-if="space.tandem"
                :x="space.x + space.w - 3"
                :y="space.y + 8"
                text-anchor="end"
                fill="#FF8C00"
                font-size="7"
                font-weight="700"
              >子母</text>
            </g>

            </g><!-- 整体偏移包裹层结束 -->
          </svg>
        </div>
      </div>

      <!-- 右侧：文字信息 -->
      <div class="info-area">
        <div class="info-content">
          <h3>{{ currentFloorData.name }}（共{{ currentFloorData.total }}个车位）</h3>


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
          xOffset: -20, // 整体左移20px使最右车位右边缘对齐虚线右边缘
          yOffset: 30, // 整体下移30px使第6排底部对齐虚线底部
          // ===== 设施（出入口）=====
          // 左侧（西侧）双道闸：出口在上、入口在下
          // 出口：车辆先往左（西）开出，再往北（上）转驶离
          // 入口：车辆从左下角进入，先往北（上）开，然后转东（右）接入中部车道
          // 下方中间偏右（最下一行中间偏右）：通往5号楼的通道，其右侧紧邻有色车位
          facilities: [
            { type: 'exit', name: '出口道闸', x: 55, y: 110, w: 70, h: 24 },
            { type: 'entrance', name: '入口道闸', x: 55, y: 225, w: 70, h: 24 },
            // 4号楼/5号楼B1入口上下放置于A024右侧，4号楼入口顶部低于A024底部+半车位高度
            { type: 'entrance', name: '4号楼B1入口', x: 755, y: 538, w: 80, h: 28 },
            { type: 'entrance', name: '5号楼B1入口', x: 755, y: 570, w: 80, h: 28 }
          ],
          // ===== 环形闭环车道系统（5条车道，宽60px）=====
          // 北/中部/南3条横向 + 西/东2条纵向，首尾相连构成外环闭环
          // 中部车道（第二行车道）西侧开口对准左侧入口道闸
          // 南车道（最下一行）自西车道延伸至东车道，与左右南北向车道连通
          // 中部双排岛（第2~5排）被车道四面包围，岛与车道紧挨不留白
          lanes: [
            // 北车道（横向，最上一行）：第1排与第2排之间，左侧对齐西车道右侧x=185
            { x: 185, y: 55, w: 960, h: 60 },
            // 中部车道（横向，第二行车道）：第3排与第4排之间，左侧对齐西车道右侧x=185
            { x: 185, y: 225, w: 960, h: 60 },
            // 南车道（横向，最下一行）：第5排与第6排之间，左侧对齐西车道右侧x=185
            { x: 185, y: 395, w: 960, h: 60 },
            // 西车道（纵向，左侧南北向）：纵贯北车道~南车道（右侧对齐A014左侧x=185）
            { x: 125, y: 55, w: 60, h: 400 },
            // 东车道（纵向，右侧南北向）：纵贯北车道~南车道
            { x: 1085, y: 55, w: 60, h: 400 },
            // A024右侧南北向通道：从中部车道(y=285)向南直通至底部虚线(y=510)，右侧对齐B097左侧(x=825)
            { x: 765, y: 285, w: 60, h: 225 }
          ],
          // ===== 墙体（四角 + 第1排占位与我司间隔 + 左子岛第4-5行间隔）=====
          walls: [
            // 四角墙体（西北/东北/西南/东南角，无车位）
            { x: 70, y: 0, w: 55, h: 55 },     // 西北角（正方形55×55，右边界对齐西车道左侧x=125）
            { x: 1140, y: 0, w: 60, h: 55 },  // 东北角（右移+55对齐虚线右边界）
            { x: 70, y: 455, w: 55, h: 55 },   // 西南角（正方形55×55，右边界对齐西车道左侧x=125）
            { x: 1140, y: 455, w: 60, h: 55 },// 东南角（右移+55对齐虚线右边界）
            // 第1排：外部公司占位(A01-A13)与我司有色之间墙体间隔（随A组右移+100）
            { x: 510, y: 0, w: 155, h: 55 },
            // 左子岛第4行：从左数第7~9位置墙体（位置7-9，x=365~455，整体右移+95对齐A043）
            { x: 365, y: 285, w: 90, h: 55 },
            // 左子岛第5行：从左数第7~9位置墙体
            { x: 365, y: 340, w: 90, h: 55 }
          ],
          blocks: [
            // ============================================================
            // 【第1排·北墙顶排】y=0，vh水平排列
            // 左侧13个外部公司占位(A01-A13) + 墙体间隔 + 右侧16个我司有色
            // 有色部分从右往左：8个单独车位 + 8个子母车位格子(可停16辆)
            // 8单独: B155(1黄) + B091-B092(2绿) + B081-B088(8粉中前8) ...
            // 简化按PRD图例从右往左：B155, B091-B092, B081-B090, B116-B117, B110-B113, B114-B115, B118-B119
            // 右侧8单独(非子母): B155(1) + B091-B092(2) + B081-B087(5) = 8个
            // 左侧8子母: B088-B090(3) + B116-B117(2) + B110-B113(4) ... 调整为8个
            // ============================================================
            // 左侧13个外部公司占位（A01-A13，右移+100使A013对齐A026）
            { x: 125, y: 0, dir: 'vh', count: 13, dept: '其他公司', prefix: 'A', startNo: 1 },
            // 右侧8个单独车位（从右往左第1~8个，非子母）
            { x: 1085, y: 0, dir: 'vh', count: 1, dept: '开发一部', prefix: 'B', startNo: 155 },
            { x: 1055, y: 0, dir: 'vh', count: 2, dept: '技术平台研发部', prefix: 'B', startNo: 91 },
            { x: 995, y: 0, dir: 'vh', count: 5, dept: '开发二部', prefix: 'B', startNo: 81 },
            // 左侧8个子母车位格子（从右往左第9~16个，子母，可停16辆）
            { x: 905, y: 0, dir: 'vh', count: 3, dept: '开发二部', prefix: 'B', startNo: 86, tandem: true },
            { x: 815, y: 0, dir: 'vh', count: 2, dept: '开发三部', prefix: 'B', startNo: 116, tandem: true },
            { x: 755, y: 0, dir: 'vh', count: 4, dept: '开发一部', prefix: 'B', startNo: 110, tandem: true },
            { x: 695, y: 0, dir: 'vh', count: 2, dept: '开发三部', prefix: 'B', startNo: 114, tandem: true },
            { x: 665, y: 0, dir: 'vh', count: 2, dept: '综合管理部', prefix: 'B', startNo: 118, tandem: true },

            // ============================================================
            // 【第2排·上部双排岛上排】y=115，车头朝北面向北车道
            // 岛内我司靠后（右侧贴东车道），从右侧起放置。每排30个=15占位+15我司
            // 我司15个开发四部从右(x=1060)往左到x=640；占位15个从x=635往左到x=185
            // ============================================================
            { x: 185, y: 115, dir: 'vh', count: 15, dept: '其他公司', prefix: 'A', startNo: 14 },
            { x: 640, y: 115, dir: 'vh', count: 15, dept: '开发四部', prefix: 'B', startNo: 48 },

            // ============================================================
            // 【第3排·上部双排岛下排】y=170，车头朝南面向中部车道，与第2排尾对尾
            // 从右侧起放置：我司15个靠右 + 占位15个靠左
            // ============================================================
            { x: 185, y: 170, dir: 'vh', count: 15, dept: '其他公司', prefix: 'A', startNo: 29 },
            { x: 640, y: 170, dir: 'vh', count: 15, dept: '开发四部', prefix: 'B', startNo: 63 },

            // ============================================================
            // 【第4排·下部双排岛上排】y=285，车头朝北面向中部车道
            // 下部双排岛被中间南北向通道(x=535~580)分左右两个子岛
            // 左子岛(标3)整体右移+95使B108对齐A043：位置1-6占位(x=185) + 墙体7-9(x=365) + 位置10-12占位(x=455) + 位置13-15我司(x=545)
            // 右子岛(标18)整体右移+30：9个我司从x=825往右到x=1065
            // ============================================================
            // 左子岛第4行：位置1-6占位（右移+95）
            { x: 185, y: 285, dir: 'vh', count: 6, dept: '其他公司', prefix: 'A', startNo: 44 },
            // 左子岛第4行：位置10-12占位（位置7-9为墙体，已在walls定义，右移+95）
            { x: 455, y: 285, dir: 'vh', count: 3, dept: '其他公司', prefix: 'A', startNo: 50 },
            // 左子岛第4行：墙体右侧3个外部公司占位（与有色车位互换位置）
            { x: 545, y: 285, dir: 'vh', count: 3, dept: '其他公司', prefix: 'A', startNo: 65 },
            // 左子岛第4行：3个我司开发四部（B108右边界对齐通道左侧x=765）
            { x: 680, y: 285, dir: 'vh', count: 3, dept: '开发四部', prefix: 'B', startNo: 106 },
            // 右子岛第4行：9个我司开发四部（右移+30，x=825~1065）
            { x: 825, y: 285, dir: 'vh', count: 9, dept: '开发四部', prefix: 'B', startNo: 97 },

            // ============================================================
            // 【第5排·下部双排岛下排】y=340，车头朝南面向南车道，与第4排尾对尾
            // 左子岛全占位(位置7-9墙体)整体右移+95；右子岛9我司右移+30
            //   位置10-15占位(x=455~605) + 墙体7-9(x=365~455) + 位置1-6占位(x=185~335)
            // ============================================================
            // 左子岛第5行：位置1-6占位（右移+95）
            { x: 185, y: 340, dir: 'vh', count: 6, dept: '其他公司', prefix: 'A', startNo: 53 },
            // 左子岛第5行：位置10-15占位（位置7-9为墙体，右移+95）
            { x: 455, y: 340, dir: 'vh', count: 6, dept: '其他公司', prefix: 'A', startNo: 59 },
            // 左子岛第5行：墙体右侧3个外部公司占位（与第4行对齐）
            { x: 545, y: 340, dir: 'vh', count: 3, dept: '其他公司', prefix: 'A', startNo: 68 },
            // 左子岛第5行：B106-B108下方补3个外部公司占位
            { x: 680, y: 340, dir: 'vh', count: 3, dept: '其他公司', prefix: 'A', startNo: 71 },
            // 右子岛第5行：9个我司开发四部（右移+30，x=825~1065）
            { x: 825, y: 340, dir: 'vh', count: 9, dept: '开发四部', prefix: 'B', startNo: 77 },

            // ============================================================
            // 【第6排·南墙底排】y=455，vh水平排列
            // 中间南北向通道(x=535~580)纵贯本排，分左右两段
            // 通道右侧10个我司开发四部整体右移+30：从x=795往右到x=1065
            // 通道左侧8个外部公司占位从x=525往左到x=290
            // ============================================================
            // 通道左侧：8个外部公司占位（右移使A024右边界与南北向通道左侧x=765对齐）
            { x: 530, y: 455, dir: 'vh', count: 8, dept: '其他公司', prefix: 'A', startNo: 17 },
            // 通道右侧：10个我司开发四部（标示数字10，右移+30，x=795~1065）
            { x: 825, y: 455, dir: 'vh', count: 10, dept: '开发四部', prefix: 'B', startNo: 152 },

            // ============================================================
            // 【西墙靠边列】x=0，贴西墙（vh宽25，不侵入西车道x=25~85）
            // 出口道闸上方3个占位(A14-A16)对准北车道行
            // 入口道闸下方8个子母车位(A25-A32)，2个一行分四行，东西停放
            // ============================================================
            // 出口道闸上方：3个占位，vh水平排列，上边界对齐西北角墙体下边界(y=55)
            { x: 40, y: 55, dir: 'vh', count: 3, dept: '其他公司', prefix: 'A', startNo: 14 },
            // 入口道闸下方：8个子母车位，2个一行分四行，东西停放（hv横向，右边界对齐西车道左侧x=125）
            { x: 70, y: 340, dir: 'hv', count: 2, dept: '其他公司', prefix: 'A', startNo: 29, tandem: true },
            { x: 70, y: 400, dir: 'hv', count: 2, dept: '其他公司', prefix: 'A', startNo: 31, tandem: true },

            // ============================================================
            // 【东墙靠边列】x=1145，hv垂直堆叠，贴东墙（东车道以东）
            // 自北向南共11行：第1行1个单独 + 第2~8行7行子母 + 第9~11行3个单独
            // 整体上移-60（一个车道宽度），第1行对齐北墙体(y=55)
            // ============================================================
            // 第1行：1个单独车位（紫色B120，应用维护部，非子母）
            { x: 1145, y: 55, dir: 'hv', count: 1, dept: '应用维护部', prefix: 'B', startNo: 120 },
            // 第2~8行：7行子母车位（含我司有色与外部公司灰色，均子母）
            { x: 1145, y: 85, dir: 'hv', count: 1, dept: '应用维护部', prefix: 'B', startNo: 121, tandem: true },
            { x: 1145, y: 115, dir: 'hv', count: 1, dept: '测试部', prefix: 'B', startNo: 122, tandem: true },
            { x: 1145, y: 145, dir: 'hv', count: 1, dept: '测试部', prefix: 'B', startNo: 123, tandem: true },
            { x: 1145, y: 175, dir: 'hv', count: 1, dept: '测试部', prefix: 'B', startNo: 124, tandem: true },
            { x: 1145, y: 205, dir: 'hv', count: 1, dept: '测试部', prefix: 'B', startNo: 125, tandem: true },
            { x: 1145, y: 235, dir: 'hv', count: 1, dept: '测试部', prefix: 'B', startNo: 126, tandem: true },
            { x: 1145, y: 265, dir: 'hv', count: 1, dept: '测试部', prefix: 'B', startNo: 127, tandem: true },
            // 第9~11行：3个单独车位（非子母）
            { x: 1145, y: 295, dir: 'hv', count: 1, dept: '测试部', prefix: 'B', startNo: 149 },
            { x: 1145, y: 325, dir: 'hv', count: 1, dept: '测试部', prefix: 'B', startNo: 150 },
            { x: 1145, y: 355, dir: 'hv', count: 1, dept: '测试部', prefix: 'B', startNo: 151 },

            // ============================================================
            // 【临时车位·东车道上】临1、临2位于中部停车岛右侧的东车道上
            // 临1：上部双排岛右侧，纵向居中于第2、3行之间(y≈142)
            // 临2：下部双排岛右侧，纵向居中于第4、5行之间(y≈312)
            // ============================================================
            { x: 1090, y: 142, dir: 'vh', count: 1, dept: '临时车位', prefix: '临', startNo: 1 },
            { x: 1090, y: 312, dir: 'vh', count: 1, dept: '临时车位', prefix: '临', startNo: 2 }
          ],
          description: '',
          allocations: [
            { dept: '开发四部', count: 61, spaces: '中部双排岛51个(上岛30+下岛左3+右18)+南墙底排10个(B152-B161)' },
            { dept: '测试部', count: 8, spaces: 'B122-B127、B149-B151（东墙车位）' },
            { dept: '开发二部', count: 10, spaces: 'B081-B090（北墙顶排，8单独+部分子母）' },
            { dept: '开发一部', count: 5, spaces: 'B110-B113(子母)、B155（北墙顶排）' },
            { dept: '开发三部', count: 4, spaces: 'B114-B115、B116-B117（北墙顶排子母）' },
            { dept: '技术平台研发部', count: 2, spaces: 'B091-B092（北墙顶排单独）' },
            { dept: '应用维护部', count: 2, spaces: 'B120-B121（东墙，B120单独+B121子母）' },
            { dept: '综合管理部', count: 2, spaces: 'B118-B119（北墙顶排子母）' },
            { dept: '临时车位', count: 2, spaces: '临1、临2（东车道上各岛右侧）' }
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
    // 墙体列表（来自 floor.walls，四角及岛内间隔墙体）
    currentWalls() {
      const floor = this.currentFloorData
      return (floor && floor.walls) ? floor.walls : []
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
          spaces.push({ x, y, w, h, dept: block.dept, label, vertical, tandem: !!block.tandem })
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
