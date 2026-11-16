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
if (!html.includes("let best=null") && !script.includes("let best=null")) throw new Error('Missing feature: '+ "let best=null");
if (!html.includes("support!==null") && !script.includes("support!==null")) throw new Error('Missing feature: '+ "support!==null");
if (!html.includes("nextBottom<=landingFloor") && !script.includes("nextBottom<=landingFloor")) throw new Error('Missing feature: '+ "nextBottom<=landingFloor");
if (!html.includes("name:'StarterBuilding',className:'Model'") && !script.includes("name:'StarterBuilding',className:'Model'")) throw new Error('Missing feature: '+ "name:'StarterBuilding',className:'Model'");
if (!html.includes("captureGeminiViewport") && !script.includes("captureGeminiViewport")) throw new Error('Missing feature: '+ "captureGeminiViewport");
if (!html.includes("gemini-3.8-flash") && !script.includes("gemini-3.8-flash")) throw new Error('Missing feature: '+ "gemini-3.8-flash");
if (!html.includes("id=\"geminiBtn\"") && !script.includes("id=\"geminiBtn\"")) throw new Error('Missing feature: '+ "id=\"geminiBtn\"");
if (!html.includes("askGemini(payload,context,image,id)") && !script.includes("askGemini(payload,context,image,id)")) throw new Error('Missing feature: '+ "askGemini(payload,context,image,id)");
console.log('STUDIO_MOBILE_UI_SMOKE_OK');
