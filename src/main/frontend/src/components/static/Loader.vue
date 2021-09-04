<template>
  <div class="lds-ellipsis">
    <div :style="{ backgroundColor: color }"></div>
    <div :style="{ backgroundColor: color }"></div>
    <div :style="{ backgroundColor: color }"></div>
    <div :style="{ backgroundColor: color }"></div>
  </div>
</template>

<script lang="ts">
import { Vue } from 'vue-class-component';
import { Prop } from 'vue-property-decorator';

export default class Loader extends Vue {
  @Prop({ type: String, default: '#ccc' }) readonly color!: string;
}
</script>

<style lang="scss" scoped>
.lds-ellipsis.grey div {
  background-color: #ccc;
}

.lds-ellipsis.black div {
  background-color: #000;
}

.lds-ellipsis.white div {
  background-color: #fff;
}

.lds-ellipsis {
  display: inline-block;
  position: relative;
  width: 80px;
  height: 80px;

  &.grey div {
    background-color: #ccc;
  }

  &.black div {
    background-color: #000;
  }

  &.white div {
    background-color: #fff;
  }

  div {
    position: absolute;
    top: 33px;
    width: 13px;
    height: 13px;
    border-radius: 50%;
    background: #ccc;
    animation-timing-function: cubic-bezier(0, 1, 1, 0);
  }

  div:nth-child(1) {
    left: 8px;
    animation: lds-ellipsis1 0.6s infinite;
  }

  div:nth-child(2) {
    left: 8px;
    animation: lds-ellipsis2 0.6s infinite;
  }

  div:nth-child(3) {
    left: 32px;
    animation: lds-ellipsis2 0.6s infinite;
  }

  div:nth-child(4) {
    left: 56px;
    animation: lds-ellipsis3 0.6s infinite;
  }
}

@keyframes lds-ellipsis1 {
  0% {
    transform: scale(0);
  }
  100% {
    transform: scale(1);
  }
}

@keyframes lds-ellipsis3 {
  0% {
    transform: scale(1);
  }
  100% {
    transform: scale(0);
  }
}

@keyframes lds-ellipsis2 {
  0% {
    transform: translate(0, 0);
  }
  100% {
    transform: translate(24px, 0);
  }
}

</style>
