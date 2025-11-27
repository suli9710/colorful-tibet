<script setup lang="ts">
import NavBar from './components/NavBar.vue'
import Footer from './components/Footer.vue'
</script>

<template>
  <div class="flex flex-col min-h-screen">
    <NavBar />
    <main class="flex-grow pt-24 md:pt-28">
      <router-view></router-view>
    </main>
    <Footer />
  </div>
</template>
