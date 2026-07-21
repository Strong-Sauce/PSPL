export const environment = {
  production: false,
  // Empty string = relative URLs. The Angular dev proxy (proxy.conf.json) forwards
  // /api/**, /products/**, etc. to http://localhost:8080 automatically.
  // This eliminates CORS issues in development and matches the production setup.
  apiBaseUrl: 'http://localhost:8080/api',
};
