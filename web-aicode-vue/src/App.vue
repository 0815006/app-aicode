<!-- src/App.vue -->
<template>
  <div id="app">
    <Layout />
  </div>
</template>

<script>
import Layout from '@/components/Layout'

export default {
  name: 'App',
  components: {
    Layout
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
body, html {
  height: 100%;
  font-family: Arial, sans-serif;
}
#app {
  height: 100%;
}
</style>
