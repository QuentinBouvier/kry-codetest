<template>
  <Poller />
</template>

<script lang="ts">
import { Options, Vue } from 'vue-class-component';
import PollerComponent from './components/PollerComponent.vue';
import { Provide } from 'vue-property-decorator';
import { StatusesService } from '@/service/StatusesService';

@Options({
  components: {
    Poller: PollerComponent
  }
})
export default class App extends Vue {
  @Provide() statusesService = new StatusesService();
}
</script>

<style lang="scss">
@import '~bulma';
</style>
