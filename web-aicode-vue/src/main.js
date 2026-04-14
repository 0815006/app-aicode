// src/main.js
import Vue from 'vue'
import App from './App.vue'
import router from './router'

// ✅ 必须引入 Element UI
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'

Vue.config.productionTip = false

// ✅ 必须注册
Vue.use(ElementUI)

new Vue({
  router,
  render: h => h(App)
}).$mount('#app')
