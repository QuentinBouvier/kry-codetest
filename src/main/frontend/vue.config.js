/*
Webpack configs. See https://cli.vuejs.org/config/#vue-config-js
 */

/**
 * @type {import('@vue/cli-service').ProjectOptions}
 */
module.exports = {
  devServer: {
    port: 8081,
    proxy: 'http://localhost:8080'
  }
};
