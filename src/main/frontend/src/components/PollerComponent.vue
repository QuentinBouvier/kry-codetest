<template>
  <div class="columns">
    <div class="column is-half is-offset-one-quarter">
      <div class="is-flex is-flex-direction-column is-align-items-center">
        <h1 class="title">Kry Status Poller</h1>
        <button @click="showAddForm = true" v-show="!showAddForm" class="button mb-4">Add Service</button>
        <AddService v-show="showAddForm"/>
        <div class="services-container">
          <div v-for="service in services" :key="service.name" class="box">
            <div class="columns">
              <div class="column is-flex is-align-items-center">
                <span :class="[tagClass[service.status]]" class="tag is-large">{{ service.status }}</span>
              </div>
              <div class="column">
                <p class="title is-3">{{ service.name }}</p>
                <p class="subtitle is-5"><a :href=service.url target="_blank" rel="noreferrer">{{ service.url }}</a></p>
              </div>
              <div class="column is-flex">
                <button class="button" @click="deleteService(service.name)">Delete</button>
                <button class="ml-1 button is-disabled" disabled>Update</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Options, Vue } from 'vue-class-component';
import AddServiceComponent from './AddServiceComponent.vue';
import { ServiceStatusService } from '@/service/ServiceStatusService';
import { Inject } from 'vue-property-decorator';
import { ServiceStatus } from '@/model/ServiceStatus';

Vue.registerHooks(['mounted']);

@Options({
  components: { AddService: AddServiceComponent }
})
export default class PollerComponent extends Vue {
  @Inject('serviceStatusService') readonly serviceStatusService!: ServiceStatusService;

  services: ServiceStatus[] = [];
  showAddForm = false;
  tagClass = {
    OK: 'is-success',
    FAIL: 'is-danger',
    UNKNOWN: 'is-info'
  };

  async mounted(): Promise<void> {
    this.services = await this.getServices();
    this.addEvents();
  }

  addEvents(): void {
    this.emitter.on('service-added', async () => {
      this.showAddForm = false;
      this.services = await this.getServices();
    });
    this.emitter.on('add-service-close', () => {
      this.showAddForm = false;
    });
  }

  async getServices(): Promise<ServiceStatus[]> {
    return this.serviceStatusService.getAll();
  }

  async deleteService(name: string): Promise<void> {
    const success = await this.serviceStatusService.delete(name);

    if (success) {
      this.services = await this.getServices();
    }
  }
}
</script>

<style scoped lang="scss">
.services-container {
  width: 100%;
}
</style>
