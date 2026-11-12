import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const cli = fileURLToPath(new URL('../node_modules/@angular/cli/bin/ng.js', import.meta.url));
const args = [cli, 'build', ...process.argv.slice(2)];
const apiUrl = process.env.API_URL?.trim();

if (apiUrl) {
  const url = URL.parse(apiUrl);
  if (!url || !['http:', 'https:'].includes(url.protocol) || url.username || url.password || url.search || url.hash) {
    console.error('API_URL debe ser una URL HTTP o HTTPS sin credenciales, parámetros ni fragmento.');
    process.exit(1);
  }
  args.push('--define', 'CONECTA_API_URL=' + JSON.stringify(url.href.replace(/\/+$/, '')));
}

const build = spawnSync(process.execPath, args, { stdio: 'inherit' });
if (build.error) {
  console.error(build.error.message);
}
process.exit(build.status ?? 1);
