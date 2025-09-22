const form = document.getElementById('hit-form');
const err  = document.getElementById('err');
const box  = document.getElementById('ajax-box');

const canvas = document.getElementById('plot');
const ctx = canvas.getContext('2d');

const allowedXs = [-5,-4,-3,-2,-1,0,1,2,3];

function getSelected(name){
  const el = document.querySelector(`input[name="${name}"]:checked`);
  return el ? Number(el.value) : NaN;
}
function parseY(text){ return Number(String(text ?? '').replace(',', '.')); }

const yEl = document.getElementById('y');
yEl.addEventListener('input', () => {
  yEl.value = yEl.value.replace(',', '.');

  if (yEl.value === '') { yEl.setCustomValidity(''); return; }
  const v = Number(yEl.value);
  if (Number.isNaN(v))        yEl.setCustomValidity('Введите число');
  else if (v < -5 || v > 5)   yEl.setCustomValidity('Y должен быть от -5 до 5');
  else                        yEl.setCustomValidity('');
});

// Программная проверка
function validateForm(){
  const x = getSelected('x');
  const y = parseY(yEl.value.trim());
  const r = getSelected('r');
  if (!allowedXs.includes(x)) return "Выберите X из {-5…3}";
  if (Number.isNaN(y))         return "Y должен быть числом";
  if (y < -5 || y > 5)         return "Y в диапазоне [-5; 5]";
  if (![1,2,3,4,5].includes(r))return "Выберите R ∈ {1..5}";
  return null;
}

/*  Canvas  */
function baseScale(){ return Math.min(canvas.width, canvas.height) * 0.4; }

function drawAxes(){
  const w=canvas.width,h=canvas.height,ox=w/2,oy=h/2;
  ctx.strokeStyle='#000'; ctx.lineWidth=1;

  // оси
  ctx.beginPath();
  ctx.moveTo(0,oy);   ctx.lineTo(w,oy);   // X
  ctx.moveTo(ox,0);   ctx.lineTo(ox,h);   // Y
  ctx.stroke();

  // стрелки
  ctx.beginPath();
  ctx.moveTo(w-10,oy-4); ctx.lineTo(w,oy); ctx.lineTo(w-10,oy+4); // →
  ctx.moveTo(ox-4,10);   ctx.lineTo(ox,0); ctx.lineTo(ox+4,10);   // ↑
  ctx.stroke();
}

function drawTicks(){
  const w=canvas.width,h=canvas.height,ox=w/2,oy=h/2,s=baseScale(),t=6;
  ctx.fillStyle='#000'; ctx.font='12px monospace';

  const vt=(x)=>ctx.fillRect(x,oy-t/2,1,t);
  vt(ox + 0.5*s); ctx.fillText('R/2',  ox + 0.5*s - 8,  oy-6);
  vt(ox + 1.0*s); ctx.fillText('R',    ox + 1.0*s - 3,  oy-6);
  vt(ox - 0.5*s); ctx.fillText('-R/2', ox - 0.5*s - 16, oy-6);
  vt(ox - 1.0*s); ctx.fillText('-R',   ox - 1.0*s - 12, oy-6);

  const ht=(y)=>ctx.fillRect(ox - t/2, y, t, 1);
  ht(oy - 0.5*s); ctx.fillText('R/2',  ox+6, oy - 0.5*s + 4);
  ht(oy - 1.0*s); ctx.fillText('R',    ox+6, oy - 1.0*s + 4);
  ht(oy + 0.5*s); ctx.fillText('-R/2', ox+6, oy + 0.5*s + 4);
  ht(oy + 1.0*s); ctx.fillText('-R',   ox+6, oy + 1.0*s + 4);
}

function drawArea(){
  const w=canvas.width,h=canvas.height,ox=w/2,oy=h/2,s=baseScale();
  ctx.clearRect(0,0,w,h);

  ctx.fillStyle='rgba(0,150,255,0.35)';
  ctx.fillRect(ox - 0.5*s, oy - 1.0*s, 0.5*s, 1.0*s);


  ctx.beginPath();
  ctx.moveTo(ox - 0.5*s, oy);
  ctx.lineTo(ox, oy);
  ctx.lineTo(ox, oy + 0.5*s);
  ctx.closePath();
  ctx.fill();

  ctx.beginPath();
  ctx.moveTo(ox, oy);
  ctx.arc(ox, oy, 0.5*s, 0, Math.PI/2, false);
  ctx.closePath();
  ctx.fill();

  drawAxes();
  drawTicks();
}

drawArea();

form.addEventListener('submit', async (e)=>{
  e.preventDefault();
  if (!form.reportValidity()) return;

  err.textContent = '';

  const fd = new FormData(form);
  fd.set('x', String(getSelected('x')));
  fd.set('r', String(getSelected('r')));
  fd.set('y', String(parseY(fd.get('y'))));

  const url = form.action + '?' + new URLSearchParams(fd);

  box.textContent = 'Загрузка…';
  try {
    const res  = await fetch(url, { method: 'GET', headers: { 'Accept': 'text/html' } });
    const html = await res.text();
    if (!res.ok) { box.innerHTML = html; return; }
    box.innerHTML = html;
  } catch {
    err.textContent = 'Сеть/сервер недоступны';
    box.textContent = '';
  }
});