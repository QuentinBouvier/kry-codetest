<template>
  <Poller />
</template>

<script lang="ts">
import { Options, Vue } from 'vue-class-component';
import Poller from './components/Poller.vue';

@Options({
  components: {
    Poller
  }
})
export default class App extends Vue {}
</script>

<style lang="scss">
@import '~bulma';
</style>
