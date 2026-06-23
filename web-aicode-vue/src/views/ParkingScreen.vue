<!-- src/views/ParkingScreen.vue -->
<template>
  <div class="parking-screen-container">
    <div class="page-layout">
      <!-- 左侧：车位图 -->
      <div class="map-area" :class="{ 'map-area--tall': isTallFloor, 'map-area--full': !showInfo }">
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
            :viewBox="currentViewBox"
            preserveAspectRatio="xMidYMid meet"
            class="parking-svg"
          >
            <!-- 地面背景 -->
            <rect
              x="0"
              y="0"
              :width="currentSvgWidth"
              :height="currentSvgHeight"
              fill="#eef0f3"
            />

            <!-- SVG箭头标记定义 -->
            <defs>
              <marker id="arrowUp" markerWidth="8" markerHeight="8" refX="4" refY="4" orient="auto">
                <polygon points="0,8 4,0 8,8" fill="#67C23A" />
              </marker>
              <marker id="arrowDown" markerWidth="8" markerHeight="8" refX="4" refY="4" orient="auto">
                <polygon points="0,8 4,0 8,8" fill="#F56C6C" />
              </marker>
            </defs>

            <!-- 车道装饰线（外环） -->
            <rect
              x="20"
              y="20"
              :width="currentSvgWidth - 40"
              :height="currentSvgHeight - 40"
              fill="none"
              stroke="#cfd6e0"
              stroke-width="2"
              stroke-dasharray="10 6"
              rx="12"
            />

            <!-- 整体偏移/旋转包裹层 -->
            <g :transform="currentFloorTransform">

            <!-- 车道带（浅色背景，体现车位行+车道行交替） -->
            <g v-for="(lane, i) in currentFloorData.lanes" :key="'lane-' + i">
              <!-- 普通车道 -->
              <rect
                v-if="!lane.dir"
                :x="lane.x"
                :y="lane.y"
                :width="lane.w"
                :height="lane.h"
                fill="#dfe4ea"
                opacity="0.55"
                rx="3"
              />
              <!-- 方向车道（出口/入口标记） -->
              <template v-if="lane.dir">
                <!-- 方向车道背景 -->
                <rect
                  v-if="lane.dir !== 'both'"
                  :x="lane.x"
                  :y="lane.y"
                  :width="lane.w"
                  :height="lane.h"
                  :fill="lane.dir === 'up' ? 'rgba(103,194,58,0.25)' : 'rgba(245,108,108,0.25)'"
                  rx="3"
                />
                <!-- 双向车道：45°斜线一分为二，上方出口 + 下方入口 -->
                <template v-if="lane.dir === 'both'">
                  <!-- 车道背景 -->
                  <rect
                    v-if="lane.labelDown == null || lane.labelDown !== ''"
                    :x="lane.x"
                    :y="lane.y"
                    :width="lane.w"
                    :height="lane.h"
                    fill="rgba(64,158,255,0.08)"
                    rx="3"
                  />
                  <!-- 上方三角：出口（红色调） -->
                  <polygon
                    :points="`${lane.x},${lane.y} ${lane.x+lane.w},${lane.y} ${lane.x+lane.w},${lane.y+lane.h/2+lane.w/2} ${lane.x},${lane.y+lane.h/2-lane.w/2}`"
                    fill="rgba(245,108,108,0.12)"
                    stroke="none"
                  />
                  <!-- 下方三角：入口（绿色调） -->
                  <polygon
                    v-if="lane.labelDown == null || lane.labelDown !== ''"
                    :points="`${lane.x},${lane.y+lane.h/2-lane.w/2} ${lane.x+lane.w},${lane.y+lane.h/2+lane.w/2} ${lane.x+lane.w},${lane.y+lane.h} ${lane.x},${lane.y+lane.h}`"
                    fill="rgba(103,194,58,0.12)"
                    stroke="none"
                  />
                  <!-- 45° 分界线 -->
                  <line
                    :x1="lane.x"
                    :y1="lane.y + lane.h / 2 - lane.w / 2"
                    :x2="lane.x + lane.w"
                    :y2="lane.y + lane.h / 2 + lane.w / 2"
                    stroke="#909399"
                    stroke-width="2.5"
                  />
                  <!-- 出口车道中线（红色虚线） -->
                  <line
                    :x1="lane.x + lane.w / 2"
                    :y1="lane.y + 10"
                    :x2="lane.x + lane.w / 2"
                    :y2="lane.y + lane.h / 2 - 6"
                    stroke="#F56C6C"
                    stroke-width="1.5"
                    stroke-dasharray="6 4"
                    opacity="0.7"
                  />
                  <!-- 入口车道中线（绿色虚线） -->
                  <line
                    v-if="lane.labelDown == null || lane.labelDown !== ''"
                    :x1="lane.x + lane.w / 2"
                    :y1="lane.y + lane.h / 2 + 6"
                    :x2="lane.x + lane.w / 2"
                    :y2="lane.y + lane.h - 10"
                    stroke="#67C23A"
                    stroke-width="1.5"
                    stroke-dasharray="6 4"
                    opacity="0.7"
                  />
                  <!-- 出口标签（居中） -->
                  <rect
                    :x="lane.x + lane.w / 2 - 18"
                    :y="lane.y + 12"
                    :width="36"
                    :height="22"
                    fill="#F56C6C"
                    rx="4"
                    opacity="0.9"
                  />
                  <text
                    :x="lane.x + lane.w / 2"
                    :y="lane.y + 27"
                    text-anchor="middle"
                    fill="#fff"
                    font-size="12"
                    font-weight="bold"
                  >{{ lane.labelUp != null ? lane.labelUp : '出口' }}</text>
                  <!-- 入口标签（居中） -->
                  <template v-if="lane.labelDown == null || lane.labelDown">
                  <rect
                    :x="lane.x + lane.w / 2 - 26"
                    :y="lane.y + lane.h - 30"
                    :width="52"
                    :height="22"
                    fill="#67C23A"
                    rx="4"
                    opacity="0.9"
                  />
                  <text
                    :x="lane.x + lane.w / 2"
                    :y="lane.y + lane.h - 15"
                    text-anchor="middle"
                    fill="#fff"
                    font-size="11"
                    font-weight="bold"
                  >{{ lane.labelDown || 'B2入口' }}</text>
                  </template>
                </template>
                <!-- 单方向箭头中线 -->
                <line
                  v-if="lane.dir !== 'both'"
                  :x1="lane.x + lane.w / 2"
                  :y1="lane.y + 12"
                  :x2="lane.x + lane.w / 2"
                  :y2="lane.y + lane.h - 12"
                  :stroke="lane.dir === 'up' ? '#67C23A' : '#F56C6C'"
                  stroke-width="2"
                  stroke-dasharray="8 6"
                />
                <!-- 单方向标签 -->
                <template v-if="lane.dir !== 'both'">
                  <rect
                    :x="lane.x + 2"
                    :y="lane.y + lane.h / 2 - 14"
                    :width="lane.w - 4"
                    :height="28"
                    :fill="lane.dir === 'up' ? '#67C23A' : '#F56C6C'"
                    rx="4"
                    opacity="0.9"
                  />
                  <text
                    :x="lane.x + lane.w / 2"
                    :y="lane.y + lane.h / 2 + 4"
                    text-anchor="middle"
                    fill="#fff"
                    font-size="11"
                    font-weight="bold"
                  >{{ lane.label }}</text>
                </template>
              </template>
              <!-- 弧线转角（车道90°转弯） -->
              <path
                v-if="lane.arc"
                :d="arcPath(lane)"
                fill="#dfe4ea"
                opacity="0.55"
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
            <g v-for="(fac, i) in currentFloorData.facilities" :key="'fac-' + i"
              :transform="fac.rotate ? 'rotate(' + fac.rotate + ' ' + (fac.x + (fac.w || 100) / 2) + ' ' + (fac.y + (fac.h || 28) / 2) + ')' : undefined">
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

            <!-- 子母车位组合虚线框（橙色虚线框同时圈住子母车位与相邻铺位） -->
            <g v-for="(grp, i) in tandemGroupRects" :key="'tgrp-' + i">
              <rect
                :x="grp.x"
                :y="grp.y"
                :width="grp.w"
                :height="grp.h"
                fill="none"
                stroke="#FF8C00"
                stroke-width="1.5"
                stroke-dasharray="5 3"
                opacity="0.9"
                rx="4"
              />
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
                font-size="13"
                font-weight="600"
              >{{ space.label }}</text>
              <!-- 禁止停车X标记 -->
              <g v-if="space.disabled">
                <line :x1="space.x+3" :y1="space.y+3" :x2="space.x+space.w-3" :y2="space.y+space.h-3" stroke="#E6A23C" stroke-width="2.5"/>
                <line :x1="space.x+space.w-3" :y1="space.y+3" :x2="space.x+3" :y2="space.y+space.h-3" stroke="#E6A23C" stroke-width="2.5"/>
              </g>
            </g>

            </g><!-- 整体偏移包裹层结束 -->
          </svg>
        </div>
      </div>

      <!-- 折叠按钮（居中于分割线） -->
      <div
        class="toggle-btn"
        :class="{ 'toggle-btn--collapsed': !showInfo }"
        @click="showInfo = !showInfo"
        :title="showInfo ? '折叠右侧面板' : '展开右侧面板'"
      >
        <i :class="showInfo ? 'el-icon-d-arrow-right' : 'el-icon-d-arrow-left'"></i>
      </div>

      <!-- 右侧：文字信息 -->
      <div class="info-area" v-show="showInfo">
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
      showInfo: true,
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
        '禁止停车': '#999999',
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
          total: 127,
          width: 1380,
          height: 720,
          xOffset: 25, // 整体右移25px（使车位图居中于虚线框内，左边界x=15→40，右边界x≈1255→1280）
          yOffset: 85, // 整体下移85px（原30+55，为第1排子母车位上方的铺位腾空间）
          // ===== 设施（出入口）=====
          // 左侧（西侧）双道闸：出口在上、入口在下
          // 出口：车辆先往左（西）开出，再往北（上）转驶离
          // 入口：车辆从左下角进入，先往北（上）开，然后转东（右）接入中部车道
          // 下方中间偏右（最下一行中间偏右）：通往5号楼的通道，其右侧紧邻有色车位
          facilities: [
            { type: 'exit', name: '出口道闸', x: 40, y: 176, w: 55, h: 26 },
            { type: 'entrance', name: '入口道闸', x: 40, y: 256, w: 55, h: 26 },
            { type: 'entrance', name: '4号楼B1入口', x: 751, y: 588, w: 80, h: 24 },
            { type: 'entrance', name: '5号楼B1入口', x: 751, y: 621, w: 80, h: 24 },
            // 4号楼/5号楼B1入口上下放置于A024右侧，紧挨底部虚线边框
          ],
          // ===== 环形闭环车道系统（宽60px）=====
          // 北/中部/南3条横向 + 西/东2条纵向 + 出入口通道，首尾相连构成外环闭环
          // 3条横向车道右侧缩至东车道左边界x=1085，避免与东车道交叠颜色变深
          lanes: [
            // 北车道（横向，右边界挨东车道左边界1107）
            { x: 206, y: 55, w: 901, h: 81 },
            // 中部车道（横向）
            { x: 206, y: 246, w: 901, h: 81 },
            // 南车道（横向）
            { x: 206, y: 437, w: 901, h: 81 },
            // 西车道（纵向，左侧南北向）：下边界对齐南车道下边界518
            { x: 125, y: 55, w: 81, h: 463 },
            // 东车道（纵向，B058右边界1099+柱子8=1107，下边界挨B057上边界518）
            { x: 1107, y: 55, w: 81, h: 463 },
            // A024右侧南北向通道上半段（B152右边界743+柱子8=751）
            { x: 751, y: 327, w: 81, h: 110 },
            // A024右侧南北向通道下半段（底对齐虚线框下边界700）
            { x: 751, y: 518, w: 81, h: 182 },
            // 入口通道：左边延伸到虚线，右边接西车道
            { x: 0, y: 246, w: 125, h: 81 },
            // 出口通道
            { x: 0, y: 161, w: 125, h: 81 }
          ],
          // ===== 墙体（四角 + 第1排占位与我司间隔 + 左子岛第4-5行间隔）=====
          walls: [
            // 四角墙体（西北/东北/西南/东南角，无车位）
            { x: 15, y: 0, w: 107, h: 55 },     // 西北角（左对齐A091=15，右保持122）
            { x: 1132, y: 0, w: 166, h: 55 },  // 东北角（B97右1129+3=1132，右对齐B080右1298）
            { x: 15, y: 518, w: 494, h: 55 },   // 西南角（左A008左15，右A010左512-3=509）
            { x: 1135, y: 518, w: 163, h: 55 },// 东南角（右边界对齐B080右边界1298）
            // 第1排：中间墙体（左A075右506+3=509，右对齐A074右边界651）
            { x: 509, y: 0, w: 142, h: 55 },
            // 左子岛第4行：墙体（x=206+2组*89+柱子8=392，宽=476-392=84）
            { x: 392, y: 327, w: 84, h: 55 },
            // 左子岛第5行：墙体（同上）
            { x: 392, y: 382, w: 84, h: 55 }
          ],
          blocks: [
            // ============================================================
            // ★【第0排·子母车位上铺位】y=-55（每个子母车位正上方铺一个单独车位）
            // 13个单独车位，与下方第1排子母车位一一对齐，同部门颜色
            // ============================================================
            // 铺位 S形对齐 row1 tandem（x同步）
            { x: 885, y: -55, dir: 'vh', count: 1, dept: '禁止停车', prefix: 'B', startNo: 105, disabled: true },
            { x: 857, y: -55, dir: 'vh', count: 1, dept: '开发四部', prefix: 'B', startNo: 106 },
            { x: 826, y: -55, dir: 'vh', count: 1, dept: '开发四部', prefix: 'B', startNo: 109 },
            { x: 796, y: -55, dir: 'vh', count: 1, dept: '开发一部', prefix: 'B', startNo: 110 },
            { x: 768, y: -55, dir: 'vh', count: 1, dept: '开发一部', prefix: 'B', startNo: 113 },
            { x: 740, y: -55, dir: 'vh', count: 1, dept: '开发三部', prefix: 'B', startNo: 114 },
            { x: 707, y: -55, dir: 'vh', count: 1, dept: '开发三部', prefix: 'B', startNo: 117 },
            { x: 679, y: -55, dir: 'vh', count: 1, dept: '综合管理部', prefix: 'B', startNo: 118 },
            { x: 651, y: -55, dir: 'vh', count: 1, dept: '应用维护部', prefix: 'B', startNo: 120 },
            // A076：A075上方铺位（子母车位）
            { x: 476, y: -55, dir: 'vh', count: 1, dept: '其他公司', prefix: 'A', startNo: 76 },

            // ============================================================
            // 【第1排·北墙顶排】y=0，vh水平排列
            // 左侧13个外部公司占位(A01-A13) + 墙体间隔 + 右侧16个我司有色
            // 有色部分从右往左：8个单独车位 + 8个子母车位格子(可停16辆)
            // 8单独: B155(1黄) + B091-B092(2绿) + B081-B088(8粉中前8) ...
            // 左侧8子母: B088-B090(3) + B116-B117(2) + B110-B113(4) ...
            // ============================================================
            // 左侧12个外部公司占位，编号A088-A077（从左往右降序）
            { x: 120, y: 0, dir: 'vh', count: 12, dept: '其他公司', prefix: 'A', startNo: 88, numberStep: -1 },
            { x: 476, y: 0, dir: 'vh', count: 1, dept: '其他公司', prefix: 'A', startNo: 75, tandem: true },
            // Row1 B区：墙右651→柱→3→柱（G1:659,687,715 G2:748,776,804 G3:837,865,893 G4:926,954,982 G5:1015,1043,1071 G6:1104,1132,1160 G7:1197,1225,1253）
            { x: 651, y: 0, dir: 'vh', count: 1, dept: '应用维护部', prefix: 'B', startNo: 121, tandem: true },
            { x: 679, y: 0, dir: 'vh', count: 1, dept: '综合管理部', prefix: 'B', startNo: 119, tandem: true },
            { x: 707, y: 0, dir: 'vh', count: 1, dept: '开发三部', prefix: 'B', startNo: 116, tandem: true },
            { x: 740, y: 0, dir: 'vh', count: 1, dept: '开发三部', prefix: 'B', startNo: 115, tandem: true },
            { x: 768, y: 0, dir: 'vh', count: 1, dept: '开发一部', prefix: 'B', startNo: 112, tandem: true },
            { x: 796, y: 0, dir: 'vh', count: 1, dept: '开发一部', prefix: 'B', startNo: 111, tandem: true },
            { x: 826, y: 0, dir: 'vh', count: 1, dept: '开发四部', prefix: 'B', startNo: 108, tandem: true },
            { x: 857, y: 0, dir: 'vh', count: 1, dept: '开发四部', prefix: 'B', startNo: 107, tandem: true },
            { x: 885, y: 0, dir: 'vh', count: 1, dept: '开发四部', prefix: 'B', startNo: 104, tandem: true },
            { x: 918, y: 0, dir: 'vh', count: 1, dept: '开发四部', prefix: 'B', startNo: 103 },
            { x: 946, y: 0, dir: 'vh', count: 1, dept: '开发四部', prefix: 'B', startNo: 102 },
            { x: 974, y: 0, dir: 'vh', count: 1, dept: '开发四部', prefix: 'B', startNo: 101 },
            { x: 1007, y: 0, dir: 'vh', count: 1, dept: '开发四部', prefix: 'B', startNo: 100 },
            { x: 1035, y: 0, dir: 'vh', count: 1, dept: '开发四部', prefix: 'B', startNo: 99 },
            { x: 1063, y: 0, dir: 'vh', count: 1, dept: '开发四部', prefix: 'B', startNo: 98 },
            { x: 1096, y: 0, dir: 'vh', count: 1, dept: '开发四部', prefix: 'B', startNo: 97 },

            // ============================================================
            // 【第2排·上部双排岛上排】y=115，车头朝北面向北车道
            // 岛内我司靠后（右侧贴东车道），从右侧起放置。每排30个=15占位+15我司
            // 我司15个开发四部从右(x=1060)往左到x=640；占位15个从x=635往左到x=185
            // ============================================================
            { x: 206, y: 136, dir: 'vh', count: 15, dept: '其他公司', prefix: 'A', startNo: 60 },
            { x: 651, y: 136, dir: 'vh', count: 15, dept: '测试部', prefix: 'B', startNo: 122 },

            // ============================================================
            // 【第3排·上部双排岛下排】y=170，车头朝南面向中部车道，与第2排尾对尾
            // 从右侧起放置：我司15个靠右 + 占位15个靠左
            // ============================================================
            // A043往左→A029，编号59-45（从左往右降序）
            { x: 206, y: 191, dir: 'vh', count: 15, dept: '其他公司', prefix: 'A', startNo: 59, numberStep: -1 },
            { x: 651, y: 191, dir: 'vh', count: 15, dept: '测试部', prefix: 'B', startNo: 151, numberStep: -1 },

            // ============================================================
            // 【第4排·下部双排岛上排】y=285，车头朝北面向中部车道
            // 下部双排岛被中间南北向通道(x=535~580)分左右两个子岛
            // 左子岛(标3)整体右移+95使B108对齐A043：位置1-6占位(x=185) + 墙体7-9(x=365) + 位置10-12占位(x=455) + 位置13-15我司(x=545)
            // 右子岛(标18)整体右移+30：9个我司从x=825往右到x=1065
            // ============================================================
            // 左子岛第4行：位置1-6占位（右移+95），编号A033-A038
            { x: 206, y: 327, dir: 'vh', count: 6, dept: '其他公司', prefix: 'A', startNo: 33 },
            // 左子岛第4行：位置10-12占位（位置7-9为墙体，已在walls定义，右移+95），编号A039-A041
            { x: 476, y: 327, dir: 'vh', count: 3, dept: '其他公司', prefix: 'A', startNo: 39 },
            // 左子岛第4行：墙体右侧3个外部公司占位（与有色车位互换位置），编号A042-A044
            { x: 565, y: 327, dir: 'vh', count: 3, dept: '其他公司', prefix: 'A', startNo: 42 },
            // 左子岛第4行：3个我司开发四部（B108右边界对齐通道左侧x=765）
            { x: 654, y: 327, dir: 'vh', count: 3, dept: '开发四部', prefix: 'B', startNo: 154, numberStep: -1 },
            // 右子岛第4行：9个我司开发四部（右移+30，x=825~1065）
            { x: 832, y: 327, dir: 'vh', count: 9, dept: '开发四部', prefix: 'B', startNo: 67 },

            // ============================================================
            // 【第5排·下部双排岛下排】y=340，车头朝南面向南车道，与第4排尾对尾
            // 左子岛全占位(位置7-9墙体)整体右移+95；右子岛9我司右移+30
            //   位置10-15占位(x=455~605) + 墙体7-9(x=365~455) + 位置1-6占位(x=185~335)
            // ============================================================
            // 左子岛第5行：位置1-6占位（右移+95），编号A032-A027（从左往右降序）
            { x: 206, y: 382, dir: 'vh', count: 6, dept: '其他公司', prefix: 'A', startNo: 32, numberStep: -1 },
            // 左子岛第5行：位置10-15占位（位置7-9为墙体，右移+95），编号A026-A021
            { x: 476, y: 382, dir: 'vh', count: 6, dept: '其他公司', prefix: 'A', startNo: 26, numberStep: -1 },
            // 左子岛第5行：B106-B108下方补3个外部公司占位，编号A020-A018
            { x: 654, y: 382, dir: 'vh', count: 3, dept: '其他公司', prefix: 'A', startNo: 20, numberStep: -1 },
            // 右子岛第5行：9个我司开发四部（右移+30），编号B066-B058（从左往右降序）
            { x: 832, y: 382, dir: 'vh', count: 9, dept: '开发四部', prefix: 'B', startNo: 66, numberStep: -1 },

            // ============================================================
            // 【第6排·南墙底排】y=455，vh水平排列
            // 中间南北向通道(x=535~580)纵贯本排，分左右两段
            // 通道右侧10个我司开发四部整体右移+30：从x=795往右到x=1065
            // 通道左侧8个外部公司占位从x=525往左到x=290
            // ============================================================
            // 通道左侧：8个外部公司占位（右移使A024右边界与南北向通道左侧x=765对齐）
            { x: 504, y: 518, dir: 'vh', count: 1, dept: '其他公司', prefix: 'A', startNo: 10 },
            { x: 532, y: 518, dir: 'vh', count: 1, dept: '其他公司', prefix: 'A', startNo: 11 },
            { x: 565, y: 518, dir: 'vh', count: 1, dept: '其他公司', prefix: 'A', startNo: 12 },
            { x: 593, y: 518, dir: 'vh', count: 1, dept: '其他公司', prefix: 'A', startNo: 13 },
            { x: 621, y: 518, dir: 'vh', count: 1, dept: '其他公司', prefix: 'A', startNo: 14 },
            { x: 654, y: 518, dir: 'vh', count: 1, dept: '其他公司', prefix: 'A', startNo: 15 },
            { x: 682, y: 518, dir: 'vh', count: 1, dept: '其他公司', prefix: 'A', startNo: 16 },
            { x: 710, y: 518, dir: 'vh', count: 1, dept: '其他公司', prefix: 'A', startNo: 17 },
            // 通道右侧：10个我司开发四部（标示数字10，右移+30，x=795~1065）
            { x: 832, y: 518, dir: 'vh', count: 10, dept: '开发四部', prefix: 'B', startNo: 48 },

            // ============================================================
            // 【西墙靠边列】x=0，贴西墙（vh宽25，不侵入西车道x=25~85）
            // 出口道闸上方3个占位(A14-A16)对准北车道行
            // 入口道闸下方8个子母车位(A25-A32)，2个一行分四行，东西停放
            // ============================================================
            // ★【西墙左侧铺位·子母车位左邻车位】x=15（往左铺），非子母，同部门颜色
            // A001：A033上方南北向车位，靠近A033左边界
            { x: 7, y: 327, dir: 'vh', count: 1, dept: '其他公司', prefix: 'A', startNo: 1 },
            // A001下方8个车位，A009底距SW墙顶1柱(8px)
            { x: 15, y: 388, dir: 'hv', count: 1, dept: '其他公司', prefix: 'A', startNo: 3 },
            { x: 70, y: 388, dir: 'hv', count: 1, dept: '其他公司', prefix: 'A', startNo: 2, tandem: true },
            { x: 15, y: 416, dir: 'hv', count: 1, dept: '其他公司', prefix: 'A', startNo: 4 },
            { x: 70, y: 416, dir: 'hv', count: 1, dept: '其他公司', prefix: 'A', startNo: 5, tandem: true },
            { x: 15, y: 449, dir: 'hv', count: 1, dept: '其他公司', prefix: 'A', startNo: 7 },
            { x: 70, y: 449, dir: 'hv', count: 1, dept: '其他公司', prefix: 'A', startNo: 6, tandem: true },
            { x: 15, y: 477, dir: 'hv', count: 1, dept: '其他公司', prefix: 'A', startNo: 8 },
            { x: 70, y: 477, dir: 'hv', count: 1, dept: '其他公司', prefix: 'A', startNo: 9, tandem: true },

            // 出口道闸上方：3个占位，vh水平排列，上边界对齐西北角墙体下边界(y=55)
            // 编号 A91/A90/A89（从西往东降序）
            { x: 7, y: 58, dir: 'vh', count: 1, dept: '其他公司', prefix: 'A', startNo: 91 },
            { x: 35, y: 58, dir: 'vh', count: 1, dept: '其他公司', prefix: 'A', startNo: 90 },
            { x: 63, y: 58, dir: 'vh', count: 1, dept: '其他公司', prefix: 'A', startNo: 89 },

            // ============================================================
            // 【东墙靠边列】x=1145，hv垂直堆叠，贴东墙（东车道以东）
            // 自北向南共11行：第1行1个单独 + 第2~8行7行子母 + 第9~11行3个单独
            // 整体上移-60（一个车道宽度），第1行对齐北墙体(y=55)
            // ============================================================
            // 东墙：柱→3→柱→3→柱→3→柱→3（G1:71,99,127|G2:160,188,216|G3:249,277,305|G4:338,366,394）
            { x: 1188, y: 63, dir: 'hv', count: 1, dept: '测试部', prefix: 'B', startNo: 96, tandem: true },
            { x: 1188, y: 91, dir: 'hv', count: 1, dept: '测试部', prefix: 'B', startNo: 94, tandem: true },
            { x: 1188, y: 119, dir: 'hv', count: 1, dept: '技术平台研发部', prefix: 'B', startNo: 91, tandem: true },
            { x: 1188, y: 152, dir: 'hv', count: 1, dept: '开发二部', prefix: 'B', startNo: 90, tandem: true },
            { x: 1188, y: 180, dir: 'hv', count: 1, dept: '开发二部', prefix: 'B', startNo: 87, tandem: true },
            { x: 1188, y: 208, dir: 'hv', count: 1, dept: '开发二部', prefix: 'B', startNo: 86, tandem: true },
            { x: 1188, y: 241, dir: 'hv', count: 1, dept: '开发二部', prefix: 'B', startNo: 83, tandem: true },
            { x: 1188, y: 269, dir: 'hv', count: 1, dept: '开发二部', prefix: 'B', startNo: 82, tandem: true },
            { x: 1188, y: 297, dir: 'hv', count: 1, dept: '开发四部', prefix: 'B', startNo: 79, tandem: true },
            { x: 1188, y: 330, dir: 'hv', count: 1, dept: '开发四部', prefix: 'B', startNo: 78 },
            { x: 1188, y: 358, dir: 'hv', count: 1, dept: '开发四部', prefix: 'B', startNo: 77 },
            { x: 1188, y: 386, dir: 'hv', count: 1, dept: '禁止停车', prefix: 'B', startNo: 76, disabled: true },

            // 东墙铺位（同 y）
            { x: 1243, y: 63, dir: 'hv', count: 1, dept: '禁止停车', prefix: 'B', startNo: 95, disabled: true },
            { x: 1243, y: 91, dir: 'hv', count: 1, dept: '测试部', prefix: 'B', startNo: 93 },
            { x: 1243, y: 119, dir: 'hv', count: 1, dept: '技术平台研发部', prefix: 'B', startNo: 92 },
            { x: 1243, y: 152, dir: 'hv', count: 1, dept: '开发二部', prefix: 'B', startNo: 89 },
            { x: 1243, y: 180, dir: 'hv', count: 1, dept: '开发二部', prefix: 'B', startNo: 88 },
            { x: 1243, y: 208, dir: 'hv', count: 1, dept: '开发二部', prefix: 'B', startNo: 85 },
            { x: 1243, y: 241, dir: 'hv', count: 1, dept: '开发二部', prefix: 'B', startNo: 84 },
            { x: 1243, y: 269, dir: 'hv', count: 1, dept: '开发二部', prefix: 'B', startNo: 81 },
            { x: 1243, y: 297, dir: 'hv', count: 1, dept: '禁止停车', prefix: 'B', startNo: 80, disabled: true },
            // ============================================================
            // 【临时车位·东车道上】临1、临2位于中部停车岛右侧的东车道上
            // 临1：上部双排岛右侧，纵向居中于第2、3行之间(y≈142)
            // 临2：下部双排岛右侧，纵向居中于第4、5行之间(y≈312)
            // ============================================================
            { x: 1099, y: 163, dir: 'vh', count: 1, dept: '临时车位', prefix: '临', startNo: 1 },
            { x: 1099, y: 354, dir: 'vh', count: 1, dept: '临时车位', prefix: '临', startNo: 2 }
          ],
          description: '',
          allocations: [
            { dept: '综合管理部', count: 2, spaces: 'B118-B119' },
            { dept: '开发一部', count: 4, spaces: 'B110-B113' },
            { dept: '开发二部', count: 10, spaces: 'B081-B090' },
            { dept: '开发三部', count: 4, spaces: 'B114-B117' },
            { dept: '开发四部', count: 46, spaces: 'B097-B104、B106-B108、B048-B075、B077-B079、B152-B154' },
            { dept: '技术平台研发部', count: 2, spaces: 'B091-B092' },
            { dept: '测试部', count: 33, spaces: 'B093-B094、B096、B122-B151' },
            { dept: '应用维护部', count: 2, spaces: 'B120-B121' },
            { dept: '禁止停车', count: 4, spaces: 'B076、B080、B095、B105（防控物资）' },
            { dept: '临时车位', count: 2, spaces: '临1、临2（东车道上各岛右侧）' }
          ]
        },

        // ==================== 5号楼 B1层 ====================
        // 按PRD修正版：PDF原始方向为上西下东，顺时针旋转90度校正为上北下南
        // 车位朝向：竖向(vh)=南北朝向(左右并排)，横向(hv)=东西朝向(上下堆叠)
        // 页面须顺时针旋转90°使上方与地理北一致（PDF原方向上西下东→网页上北下南）
        {
          id: '5F-B1',
          name: '5号楼B1层',
          total: 103,
          svgTransform: 'translate(210, 60)', // 整体右移5车位宽
          width: 1400,
          height: 1100,
          displayWidth: 1400,
          displayHeight: 1100,
          facilities: [
            // 按PRD修正版：
            // B2地库入口在东侧(右侧偏中)，靠近C001
            // 5号楼B1电梯间在北侧(上方偏右)，紧邻往4号楼通道
            // 往4号楼B1方向通道在东北角
            // 往研修院方向电梯间在西南角(左下角)
            { type: 'elevator', name: '5号楼B1电梯间', x: 1026, y: 350, w: 135, h: 45, rotate: 90 },
            { type: 'entrance', name: '往4号楼B1方向', x: 1026, y: 138, w: 110, h: 24 },
            { type: 'entrance', name: 'B2入口', x: 1310, y: 138, w: 60, h: 24 },
            { type: 'elevator', name: '研修院方向电梯间', x: -60, y: 0, w: 140, h: 50, rotate: 360 },
            { type: 'entrance', name: '地面入口', x: 170, y: 745, w: 80, h: 24 }
          ],
          // 车道：网格状分布
          // 北部车道(横向)：北侧靠墙行与各岛之间
          // 南部车道(横向)：各岛与南侧车位之间
          // 西侧车道(纵向)：左侧靠墙与岛1之间
          // 岛间纵向支车道：各岛之间穿插
          lanes: [
            // 北部车道（横向）y=110-191
            { x: 95, y: 110, w: 1135, h: 81 },
            // 南部车道（横向）y=555-636，左延到C94右边界
            { x: -87, y: 555, w: 1042, h: 81 },
            // 左1车道（西侧纵向）右贴临5左
            { x: 95, y: 191, w: 81, h: 364 },
            // 左2车道（岛1-岛2间）
            { x: 291, y: 191, w: 81, h: 364 },
            // 车道3（岛2-岛3间）
            { x: 487, y: 191, w: 81, h: 364 },
            // 车道4（岛3-岛4间）→ 错层双向：45°斜线分割，上方出口+下方B2入口
            { x: 623, y: 191, w: 81, h: 364, dir: 'both' },
            // 车道5（岛4-岛5间）
            { x: 759, y: 191, w: 81, h: 364 },
            // 车道6（岛5右侧，上起北车道下边界，下至南车道底部）
            { x: 955, y: 191, w: 81, h: 445 },
            // 地面入口：整体绕小长方形左下角(291,636)逆时针旋转90°
            { x: 291, y: 636, w: 81, h: 81, arc: { dir: 'sw-es', r: 81 } },
            { x: 129, y: 717, w: 162, h: 81 }
          ],
          // 墙体
          walls: [
            // 右1车道右侧墙体（下边界对齐B042居中）
            { x: 1036, y: 191, w: 115, h: 316 }
          ],
          blocks: [
            // ===== 北侧靠墙车位行（y=55, vh，从右往左组:3,3,3,3,1,3,1,3,3,3,3,2）=====
            { x: 134, y: 55, dir: 'vh', count: 2, dept: '开发一部', prefix: 'C', startNo: 76, numberStep: -1 },
            { x: 195, y: 55, dir: 'vh', count: 3, dept: '开发一部', prefix: 'C', startNo: 74, numberStep: -1 },
            { x: 284, y: 55, dir: 'vh', count: 4, dept: '开发一部', prefix: 'C', startNo: 48, numberStep: -1 },
            { x: 406, y: 55, dir: 'vh', count: 2, dept: '技术平台研发部', prefix: 'C', startNo: 44, numberStep: -1 },
            { x: 467, y: 55, dir: 'vh', count: 4, dept: '技术平台研发部', prefix: 'C', startNo: 108, numberStep: -1 },
            { x: 609, y: 55, dir: 'vh', count: 3, dept: '技术平台研发部', prefix: 'B', startNo: 170, numberStep: -1 },
            { x: 723, y: 55, dir: 'vh', count: 1, dept: '技术平台研发部', prefix: 'B', startNo: 167 },
            { x: 756, y: 55, dir: 'vh', count: 12, dept: '技术平台研发部', prefix: 'B', startNo: 166, numberStep: -1 },

            // ===== 左侧靠墙列（左1车道左侧，测试部15个，柱→3→柱→3）=====
            // C077-C091，hv方向东西停放，5组×3，整体上移89px
            { x: 35, y: 102, dir: 'hv', count: 3, dept: '开发一部', prefix: 'C', startNo: 77 },
            { x: 35, y: 191, dir: 'hv', count: 3, dept: '其他公司', prefix: 'C', startNo: 80 },
            { x: 35, y: 280, dir: 'hv', count: 3, dept: '其他公司', prefix: 'C', startNo: 83 },
            { x: 35, y: 369, dir: 'hv', count: 3, dept: '其他公司', prefix: 'C', startNo: 86 },
            { x: 35, y: 458, dir: 'hv', count: 3, dept: '其他公司', prefix: 'C', startNo: 89 },
            // 叉左侧 hv上下放: C94(上),C95(下)，居中于下方车道
            { x: -142, y: 561, dir: 'hv', count: 2, dept: '其他公司', prefix: 'C', startNo: 94 },
            // C091左侧(vh上下停): 叉,C96,C93,C92(左→右)
            { x: -95, y: 492, dir: 'vh', count: 1, dept: '禁止停车', prefix: '', startNo: 0, disabled: true },
            { x: -67, y: 492, dir: 'vh', count: 1, dept: '其他公司', prefix: 'C', startNo: 96 },
            { x: -39, y: 492, dir: 'vh', count: 1, dept: '其他公司', prefix: 'C', startNo: 93 },
            { x: -6, y: 492, dir: 'vh', count: 1, dept: '其他公司', prefix: 'C', startNo: 92 },

            // ===== 南车道下方 hv: C097-C099 + C001 =====
            { x: 37, y: 636, dir: 'hv', count: 1, dept: '其他公司', prefix: 'C', startNo: 97 },
            { x: 108, y: 636, dir: 'hv', count: 1, dept: '其他公司', prefix: 'C', startNo: 98 },
            { x: 179, y: 636, dir: 'hv', count: 1, dept: '其他公司', prefix: 'C', startNo: 99 },
            // C001(测试) 右侧对齐车道3正中，柱+C002(综合,vh)
            { x: 473, y: 636, dir: 'hv', count: 1, dept: '测试部', prefix: 'C', startNo: 1 },
            { x: 536, y: 636, dir: 'vh', count: 1, dept: '综合管理部', prefix: 'C', startNo: 2 },
            // B102-B103(综合)+B14(综合,vh)，B103居中车道4
            { x: 615, y: 636, dir: 'vh', count: 2, dept: '综合管理部', prefix: 'B', startNo: 102 },
            { x: 671, y: 636, dir: 'vh', count: 1, dept: '综合管理部', prefix: 'B', startNo: 14 },
            // B15-B18(vh,综合管理部 B014-B019)
            { x: 718, y: 636, dir: 'vh', count: 1, dept: '综合管理部', prefix: 'B', startNo: 15 },
            { x: 751, y: 636, dir: 'vh', count: 3, dept: '综合管理部', prefix: 'B', startNo: 16 },
            // B019(hv,综合管理部)，居中岛5
            { x: 870, y: 636, dir: 'hv', count: 1, dept: '综合管理部', prefix: 'B', startNo: 19 },
            // ===== 右下角 hv: B47-B44(综合)，右墙下方 =====
            { x: 1036, y: 507, dir: 'hv', count: 1, dept: '综合管理部', prefix: 'B', startNo: 47, numberStep: -1 },
            { x: 1036, y: 540, dir: 'hv', count: 3, dept: '综合管理部', prefix: 'B', startNo: 46, numberStep: -1 },

            // 停车道上 临5(hv贴岛1顶)、临4(hv贴岛2顶)
            { x: 206, y: 166, dir: 'hv', count: 1, dept: '临时车位', prefix: '临', startNo: 5 },
            { x: 402, y: 166, dir: 'hv', count: 1, dept: '临时车位', prefix: '临', startNo: 4 },

            // ===== 岛1·左1岛（双排，柱→3→柱→3）=====
            // 左列 G0(191): C71(其他)|C070-C069(其他)
            { x: 176, y: 191, dir: 'hv', count: 1, dept: '其他公司', prefix: 'C', startNo: 71 },
            { x: 176, y: 219, dir: 'hv', count: 2, dept: '其他公司', prefix: 'C', startNo: 70, numberStep: -1 },
            { x: 176, y: 280, dir: 'hv', count: 3, dept: '其他公司', prefix: 'C', startNo: 68, numberStep: -1 },
            { x: 176, y: 369, dir: 'hv', count: 3, dept: '其他公司', prefix: 'C', startNo: 65, numberStep: -1 },
            { x: 176, y: 458, dir: 'hv', count: 3, dept: '其他公司', prefix: 'C', startNo: 62, numberStep: -1 },
            // 右列 G0(191): 临6 | C049-C050(2)  G1(280): C051-C053(3)  G2(369): C054-C056(3)  G3(458): C057-C059(3)
            { x: 236, y: 191, dir: 'hv', count: 1, dept: '临时车位', prefix: '临', startNo: 6 },
            { x: 236, y: 219, dir: 'hv', count: 2, dept: '开发三部', prefix: 'C', startNo: 49 },
            { x: 236, y: 280, dir: 'hv', count: 3, dept: '开发三部', prefix: 'C', startNo: 51 },
            { x: 236, y: 369, dir: 'hv', count: 3, dept: '开发三部', prefix: 'C', startNo: 54 },
            { x: 236, y: 458, dir: 'hv', count: 3, dept: '开发三部', prefix: 'C', startNo: 57 },

            // ===== 岛2·左2岛（双排，柱→3→柱→3）=====
            // 左列 G0(191): C42(开发三部)|C41(开发三部)|C40(开发三部)
            { x: 372, y: 191, dir: 'hv', count: 1, dept: '开发三部', prefix: 'C', startNo: 42 },
            { x: 372, y: 219, dir: 'hv', count: 1, dept: '开发三部', prefix: 'C', startNo: 41 },
            { x: 372, y: 247, dir: 'hv', count: 1, dept: '开发三部', prefix: 'C', startNo: 40 },
            // G1(280): C039-C037(3)  G2(369): C036-C034(3)  G3(458): C033-C031(3)
            { x: 372, y: 280, dir: 'hv', count: 3, dept: '开发三部', prefix: 'C', startNo: 39, numberStep: -1 },
            { x: 372, y: 369, dir: 'hv', count: 3, dept: '开发三部', prefix: 'C', startNo: 36, numberStep: -1 },
            { x: 372, y: 458, dir: 'hv', count: 3, dept: '开发三部', prefix: 'C', startNo: 33, numberStep: -1 },
            // 右列 编号C19-C30(上→下) G0(191): C19-C21 G1(280): C22-C24 G2(369): C25-C27 G3(458): C28-C30
            { x: 432, y: 191, dir: 'hv', count: 3, dept: '开发三部', prefix: 'C', startNo: 19 },
            { x: 432, y: 280, dir: 'hv', count: 3, dept: '开发三部', prefix: 'C', startNo: 22 },
            { x: 432, y: 369, dir: 'hv', count: 3, dept: '开发三部', prefix: 'C', startNo: 25 },
            { x: 432, y: 458, dir: 'hv', count: 3, dept: '开发三部', prefix: 'C', startNo: 28 },

            // ===== 岛3·中岛（单排，柱→3→柱→3）=====
            // 编号C14-C03(上→下) G0(191): C14-C13(其他)+C12(开发三部)
            { x: 568, y: 191, dir: 'hv', count: 2, dept: '其他公司', prefix: 'C', startNo: 14, numberStep: -1 },
            { x: 568, y: 247, dir: 'hv', count: 1, dept: '开发三部', prefix: 'C', startNo: 12 },
            { x: 568, y: 280, dir: 'hv', count: 3, dept: '开发三部', prefix: 'C', startNo: 11, numberStep: -1 },
            { x: 568, y: 369, dir: 'hv', count: 3, dept: '开发三部', prefix: 'C', startNo: 8, numberStep: -1 },
            { x: 568, y: 458, dir: 'hv', count: 3, dept: '开发三部', prefix: 'C', startNo: 5, numberStep: -1 },

            // ===== 岛4·右2岛（单排，柱→3→柱→3，B01-B02下移1位）=====
            { x: 704, y: 219, dir: 'hv', count: 2, dept: '开发三部', prefix: 'B', startNo: 1 },
            { x: 704, y: 280, dir: 'hv', count: 3, dept: '开发三部', prefix: 'B', startNo: 3 },
            { x: 704, y: 369, dir: 'hv', count: 3, dept: '开发三部', prefix: 'B', startNo: 6 },
            { x: 704, y: 458, dir: 'hv', count: 3, dept: '开发三部', prefix: 'B', startNo: 9 },

            // ===== 岛5·右1岛（双排，柱→3→柱→3）=====
            // 左列 编号31-20(上→下) G0(191): B031(其他)|B030(其他)|B029(综合)  G1(280): B028-B026(3综合倒序)  G2(369): B025-B023(3综合倒序)  G3(458): B022-B021(2综合倒序)+B020(1测试)
            { x: 840, y: 191, dir: 'hv', count: 1, dept: '技术平台研发部', prefix: 'B', startNo: 31 },
            { x: 840, y: 219, dir: 'hv', count: 1, dept: '技术平台研发部', prefix: 'B', startNo: 30 },
            { x: 840, y: 247, dir: 'hv', count: 1, dept: '综合管理部', prefix: 'B', startNo: 29 },
            { x: 840, y: 280, dir: 'hv', count: 3, dept: '综合管理部', prefix: 'B', startNo: 28, numberStep: -1 },
            { x: 840, y: 369, dir: 'hv', count: 3, dept: '综合管理部', prefix: 'B', startNo: 25, numberStep: -1 },
            { x: 840, y: 458, dir: 'hv', count: 2, dept: '综合管理部', prefix: 'B', startNo: 22, numberStep: -1 },
            { x: 840, y: 514, dir: 'hv', count: 1, dept: '综合管理部', prefix: 'B', startNo: 20 },
            // 右列 G0(191): B032-B034(其他)  G1(280): B035-B037(其他)  G2(369): B038-B040(综合)  G3(458): B041-B042(综合)+B043(其他)
            { x: 900, y: 191, dir: 'hv', count: 1, dept: '其他公司', prefix: 'B', startNo: 32 },
            { x: 900, y: 219, dir: 'hv', count: 1, dept: '其他公司', prefix: 'B', startNo: 33 },
            { x: 900, y: 247, dir: 'hv', count: 1, dept: '其他公司', prefix: 'B', startNo: 34 },
            { x: 900, y: 280, dir: 'hv', count: 3, dept: '其他公司', prefix: 'B', startNo: 35 },
            { x: 900, y: 369, dir: 'hv', count: 3, dept: '综合管理部', prefix: 'B', startNo: 38 },
            { x: 900, y: 458, dir: 'hv', count: 2, dept: '综合管理部', prefix: 'B', startNo: 41 },
            { x: 900, y: 514, dir: 'hv', count: 1, dept: '综合管理部', prefix: 'B', startNo: 43 },

          ],
          description: '5号楼B1层，上北下南。车道宽81px，柱→3→柱→3网格。横向：北车道y=110-191、南车道y=555-636。纵向6条车道(y=191-636)紧贴5岛互不重叠：左1(95-176)→岛1(176/236)→左2(291-372)→岛2(372/432)→车道3(487-568)→岛3(568)→车道4→出口/入口(623-704)→岛4(704)→车道5(759-840)→岛5(840/900)→车道6(955-1036)。车道4为错层双向通道：45°斜线分割，上方出口(红色)南行→地面，下方B2入口(绿色)北行→地库。地面入口：整体绕小长方形左下角(291,636)逆时针旋转90°→西行2车道长(162px, x:129~291)。岛1左临5+占位11/右临6+开发三部11；岛2左临4+C002+开发三部10/右开发三部12；岛3技术平台2+开发三部10；岛4开发三部12；岛5左占位2+B014-B022综合9(从右墙挪入)/右B030技术1+B025-B035综合11。北侧靠墙行贴左：开发一部8+占位12+技术平台4+占位5+技术平台15。右侧靠墙列仅B012-B013占位2。南墙行(C080-C096/C097-C099)、右墙B044-B065/B097-B105均已删除。',
          allocations: [
            { dept: '综合管理部', count: 29, spaces: 'B014-B019、B102-B103、B038-B043、B020-B029、B044-B047、C002' },
            { dept: '开发一部', count: 12, spaces: 'C045-C048、C072-C079' },
            { dept: '开发三部', count: 58, spaces: 'B001-B011、C003-C014、C019-C042、C049-C059' },
            { dept: '技术平台研发部', count: 24, spaces: 'B156-B170、C043-C044、C105-C108、B030-B031' },
            { dept: '测试部', count: 1, spaces: 'C001' }
          ]
        },

        // ==================== 5号楼 B2层 ====================
        // 复制自B1层，待修改
        {
          id: '5F-B2',
          name: '5号楼B2层',
          total: 136,
          svgTransform: 'translate(160, 60)',
          width: 1400,
          height: 1100,
          displayWidth: 1400,
          displayHeight: 1100,
          facilities: [
            { type: 'elevator', name: '5号楼B2电梯间', x: 1026, y: 350, w: 135, h: 45, rotate: 90 }
          ],
          lanes: [
            { x: 95, y: 110, w: 1135, h: 81 },
            { x: -87, y: 644, w: 1317, h: 81 },
            { x: 95, y: 191, w: 81, h: 453 },
            { x: 291, y: 191, w: 81, h: 453 },
            { x: 487, y: 191, w: 81, h: 453 },
            { x: 623, y: 191, w: 81, h: 453, dir: 'both', labelUp: '通往B1', labelDown: '' },
            { x: 759, y: 191, w: 81, h: 453 },
            { x: 955, y: 191, w: 81, h: 453 },
            { x: 291, y: 725, w: 81, h: 81, dir: 'down', label: 'B2出口' }
          ],
          walls: [
            { x: 1036, y: 288, w: 115, h: 259 }
          ],
          blocks: [
            { x: 139, y: 55, dir: 'vh', count: 5, dept: '其他公司', prefix: 'B', startNo: 88, numberStep: -1 },
            { x: 289, y: 55, dir: 'vh', count: 6, dept: '开发一部', prefix: 'B', startNo: 53, numberStep: -1 },
            { x: 467, y: 55, dir: 'vh', count: 3, dept: '开发二部', prefix: 'B', startNo: 17, numberStep: -1 },
            { x: 556, y: 55, dir: 'vh', count: 1, dept: '开发二部', prefix: 'B', startNo: 14 },
            { x: 636, y: 55, dir: 'hv', count: 1, dept: '开发二部', prefix: 'A', startNo: 83 },
            { x: 807, y: 55, dir: 'hv', count: 1, dept: '开发二部', prefix: 'A', startNo: 82 },
            { x: 870, y: 55, dir: 'hv', count: 1, dept: '临时车位', prefix: '临', startNo: 7 },
            { x: 928, y: 55, dir: 'vh', count: 1, dept: '开发二部', prefix: 'A', startNo: 51 },
            { x: 956, y: 55, dir: 'vh', count: 1, dept: '开发二部', prefix: 'A', startNo: 50 },
            { x: 984, y: 55, dir: 'vh', count: 1, dept: '开发二部', prefix: 'A', startNo: 49 },
            { x: 1017, y: 55, dir: 'vh', count: 1, dept: '开发二部', prefix: 'A', startNo: 48 },
            { x: 1045, y: 55, dir: 'vh', count: 1, dept: '开发二部', prefix: 'A', startNo: 47 },
            { x: 1073, y: 55, dir: 'vh', count: 1, dept: '开发二部', prefix: 'A', startNo: 46 },
            { x: 1106, y: 55, dir: 'vh', count: 1, dept: '开发二部', prefix: 'A', startNo: 45 },
            { x: 1134, y: 55, dir: 'vh', count: 1, dept: '开发二部', prefix: 'A', startNo: 44 },
            { x: 1162, y: 55, dir: 'vh', count: 1, dept: '开发二部', prefix: 'A', startNo: 43 },
            { x: 1132, y: 110, dir: 'hv', count: 2, dept: '临时车位', prefix: '临', startNo: 8 },
            // 各岛底部新增：柱+3车位+柱
            { x: 176, y: 547, dir: 'hv', count: 3, dept: '其他公司', prefix: 'B', startNo: 71, numberStep: -1 },
            { x: 236, y: 547, dir: 'hv', count: 3, dept: '其他公司', prefix: 'B', startNo: 66 },
            { x: 372, y: 547, dir: 'hv', count: 3, dept: '开发一部', prefix: 'B', startNo: 35, numberStep: -1 },
            { x: 432, y: 547, dir: 'hv', count: 3, dept: '开发一部', prefix: 'B', startNo: 30 },
            { x: 568, y: 547, dir: 'hv', count: 2, dept: '开发二部', prefix: 'B', startNo: 2, numberStep: -1 },
            { x: 704, y: 547, dir: 'hv', count: 3, dept: '开发二部', prefix: 'A', startNo: 8 },
            { x: 840, y: 547, dir: 'hv', count: 3, dept: '应用维护部', prefix: 'A', startNo: 69, numberStep: -1 },
            { x: 900, y: 547, dir: 'hv', count: 3, dept: '应用维护部', prefix: 'A', startNo: 64 },
            { x: 35, y: 130, dir: 'hv', count: 2, dept: '其他公司', prefix: 'B', startNo: 89 },
            { x: 35, y: 191, dir: 'hv', count: 3, dept: '其他公司', prefix: 'B', startNo: 91 },
            { x: 35, y: 280, dir: 'hv', count: 3, dept: '其他公司', prefix: 'B', startNo: 94 },
            { x: 35, y: 369, dir: 'hv', count: 3, dept: '其他公司', prefix: 'B', startNo: 97 },
            { x: 35, y: 458, dir: 'hv', count: 3, dept: '其他公司', prefix: 'B', startNo: 100 },
            { x: 35, y: 547, dir: 'hv', count: 4, dept: '其他公司', prefix: 'B', startNo: 103 },
            // 下1车道右侧上方贴边 A033-A035
            { x: 1162, y: 589, dir: 'vh', count: 1, dept: '应用维护部', prefix: 'A', startNo: 33 },
            { x: 1134, y: 589, dir: 'vh', count: 1, dept: '应用维护部', prefix: 'A', startNo: 34 },
            { x: 1106, y: 589, dir: 'vh', count: 1, dept: '应用维护部', prefix: 'A', startNo: 35 },
            { x: 671, y: 589, dir: 'vh', count: 1, dept: '开发二部', prefix: 'A', startNo: 11 },
            { x: 643, y: 589, dir: 'vh', count: 1, dept: '开发二部', prefix: 'A', startNo: 12 },
            { x: 615, y: 589, dir: 'vh', count: 1, dept: '开发二部', prefix: 'A', startNo: 13 },
            // 下方车道下侧：右→左 A032-A014, 3|柱|3|柱|3|柱|3|柱|3|柱|1|柱|3
            { x: 1162, y: 725, dir: 'vh', count: 1, dept: '应用维护部', prefix: 'A', startNo: 32 },
            { x: 1134, y: 725, dir: 'vh', count: 1, dept: '应用维护部', prefix: 'A', startNo: 31 },
            { x: 1106, y: 725, dir: 'vh', count: 1, dept: '应用维护部', prefix: 'A', startNo: 30 },
            { x: 1073, y: 725, dir: 'vh', count: 1, dept: '应用维护部', prefix: 'A', startNo: 29 },
            { x: 1045, y: 725, dir: 'vh', count: 1, dept: '应用维护部', prefix: 'A', startNo: 28 },
            { x: 1017, y: 725, dir: 'vh', count: 1, dept: '应用维护部', prefix: 'A', startNo: 27 },
            { x: 984, y: 725, dir: 'vh', count: 1, dept: '应用维护部', prefix: 'A', startNo: 26 },
            { x: 956, y: 725, dir: 'vh', count: 1, dept: '应用维护部', prefix: 'A', startNo: 25 },
            { x: 928, y: 725, dir: 'vh', count: 1, dept: '应用维护部', prefix: 'A', startNo: 24 },
            { x: 895, y: 725, dir: 'vh', count: 1, dept: '应用维护部', prefix: 'A', startNo: 23 },
            { x: 867, y: 725, dir: 'vh', count: 1, dept: '应用维护部', prefix: 'A', startNo: 22 },
            { x: 839, y: 725, dir: 'vh', count: 1, dept: '应用维护部', prefix: 'A', startNo: 21 },
            { x: 806, y: 725, dir: 'vh', count: 1, dept: '应用维护部', prefix: 'A', startNo: 20 },
            { x: 778, y: 725, dir: 'vh', count: 1, dept: '应用维护部', prefix: 'A', startNo: 19 },
            { x: 750, y: 725, dir: 'vh', count: 1, dept: '应用维护部', prefix: 'A', startNo: 18 },
            { x: 717, y: 725, dir: 'vh', count: 1, dept: '应用维护部', prefix: 'A', startNo: 17 },
            { x: 684, y: 725, dir: 'vh', count: 1, dept: '应用维护部', prefix: 'A', startNo: 16 },
            { x: 656, y: 725, dir: 'vh', count: 1, dept: '应用维护部', prefix: 'A', startNo: 15 },
            { x: 628, y: 725, dir: 'vh', count: 1, dept: '应用维护部', prefix: 'A', startNo: 14 },
            { x: 1036, y: 247, dir: 'hv', count: 1, dept: '开发二部', prefix: 'A', startNo: 37 },
            { x: 1036, y: 191, dir: 'vh', count: 3, dept: '应用维护部', prefix: 'A', startNo: 38 },
            { x: 1133, y: 191, dir: 'vh', count: 2, dept: '应用维护部', prefix: 'A', startNo: 41 },
            { x: 1003, y: 435, dir: 'vh', count: 1, dept: '临时车位', prefix: '临', startNo: 10 },
            { x: 1003, y: 346, dir: 'vh', count: 1, dept: '临时车位', prefix: '临', startNo: 11 },
            { x: 1036, y: 596, dir: 'hv', count: 1, dept: '应用维护部', prefix: 'A', startNo: 36 },
            { x: 176, y: 191, dir: 'hv', count: 1, dept: '其他公司', prefix: 'B', startNo: 83 },
            { x: 176, y: 219, dir: 'hv', count: 2, dept: '其他公司', prefix: 'B', startNo: 82, numberStep: -1 },
            { x: 176, y: 280, dir: 'hv', count: 3, dept: '其他公司', prefix: 'B', startNo: 80, numberStep: -1 },
            { x: 176, y: 369, dir: 'hv', count: 3, dept: '其他公司', prefix: 'B', startNo: 77, numberStep: -1 },
            { x: 176, y: 458, dir: 'hv', count: 3, dept: '其他公司', prefix: 'B', startNo: 74, numberStep: -1 },
            { x: 236, y: 191, dir: 'hv', count: 1, dept: '开发一部', prefix: 'B', startNo: 54 },
            { x: 236, y: 219, dir: 'hv', count: 2, dept: '开发一部', prefix: 'B', startNo: 55 },
            { x: 236, y: 280, dir: 'hv', count: 3, dept: '其他公司', prefix: 'B', startNo: 57 },
            { x: 236, y: 369, dir: 'hv', count: 3, dept: '其他公司', prefix: 'B', startNo: 60 },
            { x: 236, y: 458, dir: 'hv', count: 3, dept: '其他公司', prefix: 'B', startNo: 63 },
            { x: 372, y: 191, dir: 'hv', count: 1, dept: '开发一部', prefix: 'B', startNo: 47 },
            { x: 372, y: 219, dir: 'hv', count: 1, dept: '开发一部', prefix: 'B', startNo: 46 },
            { x: 372, y: 247, dir: 'hv', count: 1, dept: '开发一部', prefix: 'B', startNo: 45 },
            { x: 372, y: 280, dir: 'hv', count: 3, dept: '开发一部', prefix: 'B', startNo: 44, numberStep: -1 },
            { x: 372, y: 369, dir: 'hv', count: 3, dept: '开发一部', prefix: 'B', startNo: 41, numberStep: -1 },
            { x: 372, y: 458, dir: 'hv', count: 3, dept: '开发一部', prefix: 'B', startNo: 38, numberStep: -1 },
            { x: 432, y: 191, dir: 'hv', count: 3, dept: '开发一部', prefix: 'B', startNo: 18 },
            { x: 432, y: 280, dir: 'hv', count: 3, dept: '开发一部', prefix: 'B', startNo: 21 },
            { x: 432, y: 369, dir: 'hv', count: 3, dept: '开发一部', prefix: 'B', startNo: 24 },
            { x: 432, y: 458, dir: 'hv', count: 3, dept: '开发一部', prefix: 'B', startNo: 27 },
            { x: 568, y: 219, dir: 'hv', count: 1, dept: '开发二部', prefix: 'B', startNo: 13 },
            { x: 568, y: 247, dir: 'hv', count: 1, dept: '开发二部', prefix: 'B', startNo: 12 },
            { x: 568, y: 280, dir: 'hv', count: 3, dept: '开发二部', prefix: 'B', startNo: 11, numberStep: -1 },
            { x: 568, y: 369, dir: 'hv', count: 3, dept: '开发二部', prefix: 'B', startNo: 8, numberStep: -1 },
            { x: 568, y: 458, dir: 'hv', count: 3, dept: '开发二部', prefix: 'B', startNo: 5, numberStep: -1 },
            { x: 704, y: 336, dir: 'hv', count: 1, dept: '开发二部', prefix: 'A', startNo: 1 },
            { x: 704, y: 369, dir: 'hv', count: 3, dept: '开发二部', prefix: 'A', startNo: 2 },
            { x: 704, y: 458, dir: 'hv', count: 3, dept: '开发二部', prefix: 'A', startNo: 5 },
            { x: 840, y: 191, dir: 'hv', count: 1, dept: '开发二部', prefix: 'A', startNo: 81 },
            { x: 840, y: 219, dir: 'hv', count: 1, dept: '开发二部', prefix: 'A', startNo: 80 },
            { x: 840, y: 247, dir: 'hv', count: 1, dept: '开发二部', prefix: 'A', startNo: 79 },
            { x: 840, y: 280, dir: 'hv', count: 3, dept: '开发二部', prefix: 'A', startNo: 78, numberStep: -1 },
            { x: 840, y: 369, dir: 'hv', count: 3, dept: '应用维护部', prefix: 'A', startNo: 75, numberStep: -1 },
            { x: 840, y: 458, dir: 'hv', count: 2, dept: '应用维护部', prefix: 'A', startNo: 72, numberStep: -1 },
            { x: 840, y: 514, dir: 'hv', count: 1, dept: '应用维护部', prefix: 'A', startNo: 70 },
            { x: 900, y: 191, dir: 'hv', count: 1, dept: '开发二部', prefix: 'A', startNo: 52 },
            { x: 900, y: 219, dir: 'hv', count: 1, dept: '开发二部', prefix: 'A', startNo: 53 },
            { x: 900, y: 247, dir: 'hv', count: 1, dept: '开发二部', prefix: 'A', startNo: 54 },
            { x: 900, y: 280, dir: 'hv', count: 3, dept: '开发二部', prefix: 'A', startNo: 55 },
            { x: 900, y: 369, dir: 'hv', count: 3, dept: '开发二部', prefix: 'A', startNo: 58 },
            { x: 900, y: 458, dir: 'hv', count: 2, dept: '应用维护部', prefix: 'A', startNo: 61 },
            { x: 900, y: 514, dir: 'hv', count: 1, dept: '应用维护部', prefix: 'A', startNo: 63 },
          ],
          description: '5号楼B2层，上北下南。车道宽81px，柱→3→柱→3网格。横向：北车道y=110-191、南车道y=555-636。纵向6条车道(y=191-636)紧贴5岛互不重叠。车道4为错层双向通道：45°斜线分割，上方通往B1(红色)。',
          allocations: [
            { dept: '开发二部', count: 62, spaces: 'A001-A013、A037-A060、A076-A083、B001-B017' },
            { dept: '应用维护部', count: 35, spaces: 'A014-A032、A061-A075、A036' },
            { dept: '开发一部', count: 39, spaces: 'B018-B056' }
          ]
        }
      ]
    }
  },
  computed: {
    currentFloorData() {
      return this.floors.find(f => f.id === this.currentFloor) || this.floors[0]
    },
    // SVG整体transform：5号楼B1需要旋转90°使北朝上
    currentFloorTransform() {
      const floor = this.currentFloorData
      if (floor && floor.svgTransform) {
        return floor.svgTransform
      }
      const xOff = (floor && floor.xOffset) || 0
      const yOff = (floor && floor.yOffset) || 0
      return 'translate(' + xOff + ',' + yOff + ')'
    },
    // SVG viewBox：旋转后的楼层统一用displayWidth/displayHeight，与4号楼车位等大
    currentViewBox() {
      const floor = this.currentFloorData
      if (floor && floor.displayWidth) {
        return '0 0 ' + floor.displayWidth + ' ' + floor.displayHeight
      }
      return '0 0 ' + (floor.width || 1200) + ' ' + (floor.height || 560)
    },
    // SVG 视图宽：优先使用displayWidth
    currentSvgWidth() {
      const floor = this.currentFloorData
      if (floor && floor.displayWidth) {
        return floor.displayWidth
      }
      return floor.width || 1200
    },
    // SVG 视图高：优先使用displayHeight
    currentSvgHeight() {
      const floor = this.currentFloorData
      if (floor && floor.displayHeight) {
        return floor.displayHeight
      }
      return floor.height || 560
    },
    // 是否为超长楼层（需要滚动查看）
    isTallFloor() {
      const floor = this.currentFloorData
      return !!(floor && floor.displayWidth)
    },
    // 墙体列表（来自 floor.walls，四角及岛内间隔墙体）
    currentWalls() {
      const floor = this.currentFloorData
      if (!floor || !floor.walls) return []
      const walls = floor.walls.map(w => ({ ...w }))
      const spaces = this.currentSpaces
      const pg = 8 // pillar gap
      walls.forEach(w => {
        if (!w.dynamic) return
        const rowSpaces = spaces.filter(s => s.y === w.y).sort((a, b) => a.x - b.x)
        if (!rowSpaces.length) return
        if (w.dynamic === 'fill') {
          // 北墙：填满外部车位和内部车位之间的空隙
          const extSp = rowSpaces.filter(s => s.dept === '其他公司')
          const intSp = rowSpaces.filter(s => s.dept !== '其他公司')
          if (extSp.length && intSp.length) {
            w.x = Math.max(...extSp.map(s => s.x + s.w))
            w.w = Math.min(...intSp.map(s => s.x)) - w.x
          }
        } else {
          // 岛内墙：左边界=前一个车位右边界+柱子间隙
          const idx = rowSpaces.findIndex(s => s.x > w.x)
          if (idx > 0) {
            const prev = rowSpaces[idx - 1]
            w.x = prev.x + prev.w + pg
          }
          if (w.w === 0) {
            // 宽度填到下一个车位左边界-柱子间隙
            const idx2 = rowSpaces.findIndex(s => s.x >= w.x)
            if (idx2 >= 0 && idx2 < rowSpaces.length) {
              w.w = rowSpaces[idx2].x - pg - w.x
            }
          }
        }
      })
      return walls
    },
    // 子母车位组合矩形：将每个子母车位与其附属铺位框在一起
    tandemGroupRects() {
      const spaces = this.currentSpaces
      if (!spaces || spaces.length === 0) return []
      const groups = []
      const used铺位Keys = new Set()
      spaces.forEach(space => {
        if (!space.tandem) return
        // 寻找与本子母车位紧邻的铺位（同x差55y=vh，同y差55x=hv）
        const 铺位 = spaces.find(s =>
          !s.tandem &&
          ((space.vertical && s.x === space.x && Math.abs(s.y - space.y) === 55) ||
           (!space.vertical && s.y === space.y && Math.abs(s.x - space.x) === 55))
        )
        if (!铺位) return
        const key = 铺位.x + ',' + 铺位.y
        if (used铺位Keys.has(key)) return
        used铺位Keys.add(key)
        const pad = 3
        const minX = Math.min(space.x, 铺位.x) - pad
        const minY = Math.min(space.y, 铺位.y) - pad
        const maxX = Math.max(space.x + space.w, 铺位.x + 铺位.w) + pad
        const maxY = Math.max(space.y + space.h, 铺位.y + 铺位.h) + pad
        groups.push({ x: minX, y: minY, w: maxX - minX, h: maxY - minY, dept: space.dept })
      })
      return groups
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
            // 4号楼B1：柱子→3车位→柱子→3车位（柱子先于第一组，跨行对齐）
            if (floor.id === '4F-B1' || floor.id === '5F-B1' || floor.id === '5F-B2') {
              const closeGap = 3, pillarGap = 8
              const g = Math.floor(i / 3)
              x = block.x + pillarGap + i * vhW + g * pillarGap + (i - g) * closeGap
            } else {
              x = block.x + i * (vhW + vhGap)
            }
            y = block.y
          } else if (block.dir === 'hv') {
            // 横向车位（车头东西），垂直排列
            w = hvW; h = hvH; vertical = false
            x = block.x
            if (floor.id === '4F-B1' || floor.id === '5F-B1' || floor.id === '5F-B2') {
              const closeGap = 3, pillarGap = 8
              const g = Math.floor(i / 3)
              y = block.y + pillarGap + i * hvH + g * pillarGap + (i - g) * closeGap
            } else {
              y = block.y + i * (hvH + hvGap)
            }
          } else if (block.dir === 'v') {
            // 纵向车位（车头南北），垂直排列（用于5号楼岛内列）
            // 压缩高度以适配SVG可用空间：25×(30+3)=33px/行
            const vStepH = 30, vStepGap = 3
            w = vhW; h = vStepH; vertical = true
            x = block.x
            y = block.y + i * (vStepH + vStepGap)
          } else {
            // 纵向车位，垂直排列（兼容旧格式）
            w = vW; h = vH; vertical = true
            x = block.x
            y = block.y + i * (vH + vGap)
          }
          const step = block.numberStep || 1
          const no = block.startNo + i * step
          const label = block.prefix === '临'
            ? '临' + no
            : block.prefix + String(no).padStart(3, '0')
          spaces.push({ x, y, w, h, dept: block.dept, label, vertical, tandem: !!block.tandem, disabled: !!block.disabled })
        }
      })
      return spaces
    }
  },
  methods: {
    arcPath(lane) {
      const { x, y, w, h, arc: { dir, r } } = lane
      const centers = {
        'se': [x + w, y + h],      // SE角：北→东 CW
        'sw': [x,     y + h],      // SW角：西→北 CW
        'ne': [x + w, y],          // NE角：东→南 CW
        'nw': [x,     y],          // NW角：南→西 CW
        'sw-es': [x, y + h]        // SW角：东→南 CW
      }
      const [cx, cy] = centers[dir] || centers['se']
      const paths = {
        'se': `M ${cx} ${cy-r} A ${r} ${r} 0 0 1 ${cx+r} ${cy} L ${cx} ${cy} Z`,
        'sw': `M ${cx-r} ${cy} A ${r} ${r} 0 0 1 ${cx} ${cy-r} L ${cx} ${cy} Z`,
        'ne': `M ${cx+r} ${cy} A ${r} ${r} 0 0 1 ${cx} ${cy+r} L ${cx} ${cy} Z`,
        'nw': `M ${cx} ${cy+r} A ${r} ${r} 0 0 1 ${cx-r} ${cy} L ${cx} ${cy} Z`,
        'sw-es': `M ${cx+r} ${cy} A ${r} ${r} 0 0 1 ${cx} ${cy+r} L ${cx} ${cy} Z`
      }
      return paths[dir] || paths['se']
    },
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
  position: relative;
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

/* 超长楼层（如5F-B1旋转后）：高度由SVG自身的viewBox宽高比决定 */
.map-area--tall .svg-wrapper {
  flex: none;
  overflow: visible;
}
.map-area--tall .parking-svg {
  width: 100%;
  height: auto;
  max-height: none;
}

/* ============ 右侧：文字信息 ============ */
.info-area {
  flex: 1;
  padding-left: 20px;
  overflow-y: auto;
  background-color: #fafafa;
  min-width: 260px;
}

/* 折叠按钮 */
.toggle-btn {
  position: absolute;
  left: calc(75% - 14px);
  top: 50%;
  transform: translateY(-50%);
  z-index: 10;
  width: 28px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  cursor: pointer;
  color: #909399;
  font-size: 14px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.toggle-btn:hover {
  color: #409EFF;
  border-color: #409EFF;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.2);
}

.toggle-btn--collapsed {
  left: calc(100% - 30px);
}

/* 右侧折叠时，左侧全宽 */
.map-area--full {
  border-right: none;
  padding-right: 0;
  flex: 1;
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
