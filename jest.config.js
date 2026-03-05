export default {
  testEnvironment: 'jsdom',
  transform: {},
  moduleNameMapper: {
    '^@/managementScripts\\.js$':
      '<rootDir>/src/main/resources/static/js/pages/admin/managementScripts.js',

    // Default catch-all
    '^@/(.*)$': '<rootDir>/src/main/resources/static/js/$1'
  },
  testMatch: [
    '**/src/test/webapp/js/**/*.test.js'
  ],
  collectCoverageFrom: [
    'src/main/resources/static/js/**/*.js',
    '!src/main/resources/static/js/**/*.test.js'
  ]
};