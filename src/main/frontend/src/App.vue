<template>
  <Poller />
</template>

<script lang="ts">
import { Options, Vue } from 'vue-class-component';
import PollerComponent from './components/PollerComponent.vue';
import { Provide } from 'vue-property-decorator';
import { ServiceStatusService } from '@/service/ServiceStatusService';

@Options({
  components: {
    Poller: PollerComponent
  }
})
export default class App extends Vue {
  @Provide() serviceStatusService = new ServiceStatusService();
}
</script>

<style lang="scss">
@import '~bulma';
</style>
