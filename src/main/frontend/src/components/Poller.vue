<template>
  <div class="columns">
    <div class="column is-half is-offset-one-quarter">
      <div class="is-flex is-flex-direction-column is-align-items-center">
        <h1 class="title">Kry Status Poller</h1>
        <div class="services-container">
          <div v-for="service in services" :key="service.name" class="box">
            <div class="columns">
              <div class="column is-flex is-align-items-center">
                <span :class="[tagClass[service.status]]" class="tag is-large">{{service.status}}</span>
              </div>
              <div class="column">
                <p class="title is-3">{{service.name}}</p>
                <p class="subtitle is-5">{{ service.url }}</p>
              </div>
              <div class="column is-flex">
                <button class="button">supprimer</button>
                <button class="ml-1 button">éditer</button>
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
import axios from 'axios';

Vue.registerHooks(['mounted']);

interface ServiceStatusDto {
  url: string;
  name: string;
  // eslint-disable-next-line camelcase
  created_at: number;
  status: string;
}

interface ServiceStatus {
  url: string;
  name: string;
  createdAt: Date;
  status: string;
}

@Options({})
export default class Poller extends Vue {
  services: ServiceStatus[] = [];
  tagClass = {
    OK: 'is-success',
    FAIL: 'is-danger',
    UNKNOWN: 'is-info'
  }

  async mounted (): Promise<void> {
    this.services = await this.getServices();
  }

  async getServices (): Promise<ServiceStatus[]> {
    const response = await axios({
      method: 'get',
      url: 'http://localhost:8080/service'
    });
    return response.data.map((x: ServiceStatusDto) => {
      return {
        url: x.url,
        name: x.name,
        createdAt: new Date(x.created_at),
        status: x.status
      };
    });
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="scss">
.services-container {
  width: 100%;
}
</style>
