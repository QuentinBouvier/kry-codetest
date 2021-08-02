<template>
  <Poller />
</template>

<script lang="ts">
import { Options, Vue } from 'vue-class-component';
import PollerComponent from './components/PollerComponent.vue';

@Options({
  components: {
    Poller: PollerComponent
  }
})
export default class App extends Vue {}
</script>

<style lang="scss">
@import '~bulma';
</style>
