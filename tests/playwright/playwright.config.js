module.exports = {
  timeout: 30000,
  use: {
    channel: 'chrome',
    headless: true,
    ignoreHTTPSErrors: true
  },
  reporter: [['list']]
};
