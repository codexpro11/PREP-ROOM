import config from '../../vite.config.ts';

export default {
  ...config,
  server: {
    ...config.server,
    host: '127.0.0.1', port: 3100, strictPort: true,
    proxy: { '/api': { target: 'http://127.0.0.1:18081', changeOrigin: true } },
  },
};
