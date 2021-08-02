<template>
  <div class="add-service-form box">
    <div class="box-header is-flex is-justify-content-space-between">
      <h2 class="title">Add a service</h2>
      <button class="delete" @click="closeBox"></button>
    </div>
    <div class="field">
      <div class="label">Name</div>
      <div class="control">
        <input type="text" :class="{ 'is-danger': hasError }" @input="resetError" class="input" v-model="nameValue" @keyup.enter="addService" placeholder="Service's identifier">
      </div>
    </div>
    <div class="field">
      <div class="label">Url</div>
      <div class="control">
        <input type="text" :class="{ 'is-danger': hasError }" @input="resetError" class="input" v-model="urlValue" @keyup.enter="addService" placeholder="https://example.com">
      </div>
    </div>
    <div class="field" :class="{ 'error-shift': !hasError }">
      <div class="control">
        <button class="button is-link" @click="addService">Add</button>
        <p class="help is-danger" v-show="hasError">{{error.message}}</p>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Vue } from 'vue-class-component';
import axios from 'axios';

interface AddServiceError {
  code: number;
  message: string;
}

export default class AddServiceComponent extends Vue {
  nameValue = '';
  urlValue = '';
  hasError = false;
  error: AddServiceError = {} as AddServiceError;

  async addService(): Promise<void> {
    try {
      const response = await axios({
        url: '/service',
        method: 'post',
        data: {
          name: this.nameValue,
          url: this.urlValue
        },
        responseType: 'text'
      });

      if (response.status === 201) {
        this.resetError();
        this.resetValues();
        this.emitter.emit('service-added');
      }
    } catch (err) {
      this.setError(err.response.status, err.response.data);
    }
  }

  resetError(): void {
    this.hasError = false;
    this.error = {} as AddServiceError;
  }

  resetValues(): void {
    this.nameValue = '';
    this.urlValue = '';
  }

  setError(code: number, message: string): void {
    this.error.code = code;
    this.error.message = message;
    this.hasError = true;
  }

  closeBox(): void {
    this.resetValues();
    this.resetError();
    this.emitter.emit('add-service-close');
  }
}
</script>

<style scoped lang="scss">
.add-service-form {
  width: 60%;
}

.error-shift {
  padding-bottom: 22px;
}
</style>
