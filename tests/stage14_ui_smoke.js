const fs = require('fs');
const html = fs.readFileSync('app/src/main/assets/index.html', 'utf8');
const script = html.match(/<script>([\s\S]*?)<\/script>/)[1];
// Parse the entire embedded application script, not just this test file.
new Function(script);
const required = [
  'HOME','MODEL','AVATAR','UI','SCRIPT','TEST','VIEW','PLUGINS',
  'Toolbox','Explorer','Properties','Output','codeHighlight','lineNumbers',
  'data-tool="move"','data-tool="scale"','data-tool="rotate"',
  'function runSelectedScript','function setActiveTool','data-panel="explorer"',
  'function makeRuntimeBootstrap','function applyRuntimeScene',
  'function mergeRuntimeNodes','function startPlay','function stopPlay',
  'function playFrame','function bindPlayControls',
  '__STUDIO_MOBILE_SCENE__','playStick','playJump',
  "playBtn').onclick=togglePlay",
  "const src=String($('codeArea').value||'')",
  "n.source=src;currentScript=n.id;markDirty()",
  "name:'Baseplate',className:'Part'",
  "selected:'spawn'"
];
for (const item of required) {
  if (!html.includes(item)) throw new Error('Missing Studio UI feature: ' + item);
}
const ids = [...html.matchAll(/\bid="([^"]+)"/g)].map(m => m[1]);
const seen = new Set();
for (const id of ids) {
  if (seen.has(id)) throw new Error('Duplicate HTML id: ' + id);
  seen.add(id);
}
if ((script.match(/querySelectorAll\('\[data-panel\]'\)/g) || []).length !== 1) {
  throw new Error('Mobile panel handler must be bound exactly once');
}
console.log('STUDIO_MOBILE_UI_SMOKE_OK');
