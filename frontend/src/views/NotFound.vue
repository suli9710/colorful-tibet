<template>
  <section class="tibet-page-shell min-h-[calc(100vh-6rem)] px-4 py-20">
    <div class="mx-auto max-w-3xl text-center">
      <div class="tibet-panel rounded-[2rem] px-8 py-14 md:px-14">
        <p class="mb-4 text-sm font-semibold uppercase tracking-[0.35em] text-tibet-gold">404</p>
        <h1 class="tibet-heading justify-center text-4xl font-black text-tibet-dark md:text-5xl">
          页面走进了云雾里
        </h1>
        <p class="mx-auto mt-6 max-w-xl text-base leading-8 text-tibet-brown/75">
          这个地址暂时没有对应的内容。你可以回到首页，或继续探索景点、非遗和路线规划。
        </p>
        <div class="mt-10 flex flex-col justify-center gap-3 sm:flex-row">
          <RouterLink class="tibet-btn" to="/">返回首页</RouterLink>
          <RouterLink class="tibet-btn-outline" to="/route-planner">规划路线</RouterLink>
        </div>
      </div>
    </div>
  </section>
</template>
