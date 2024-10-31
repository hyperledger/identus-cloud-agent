const { build } = require('esbuild')

build({
  entryPoints: ['test-suite.ts'],
  bundle: true,
  minify: false,
  platform: 'node',
  target: ['esnext'],
  format: 'cjs',
  outfile: 'dist/suite.spec.js'
})
