<template>
  <div v-if="show" class="modal-mask" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal-head">
        <div>{{ title }}</div>
        <button class="btn secondary small" @click="$emit('close')">关闭</button>
      </div>
      <div class="modal-body">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  show: { type: Boolean, default: false },
  title: { type: String, default: '' }
})

defineEmits(['close'])
</script>

<style scoped>
</style>

