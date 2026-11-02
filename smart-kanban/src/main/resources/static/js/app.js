// =====================================================================
// Smart Kanban - frontend. Conversa com o backend via fetch() (HTTP + JSON).
// Nunca usa innerHTML com dados do usuário (evita XSS): sempre textContent.
// =====================================================================
const STATUS = ['A_FAZER', 'EM_ANDAMENTO', 'CONCLUIDA'];
const ROTULO_PRIORIDADE = { BAIXA: 'Baixa', MEDIA: 'Média', ALTA: 'Alta' };

let tarefas = [];          // cópia local do que veio do banco (a verdade está no backend)
let editandoId = null;     // id da tarefa em edição (null = criando)
let detalheId = null;      // tarefa aberta no modal de detalhes
let iaTarefa = null;       // tarefa-pai do planejamento de IA
let iaSugestoes = [];      // sugestões recebidas da IA (ainda não salvas)

const $ = (id) => document.getElementById(id);

// ---------- comunicação com a API ----------
async function api(metodo, url, corpo) {
  const opcoes = { method: metodo, headers: {} };
  if (corpo !== undefined) {
    opcoes.headers['Content-Type'] = 'application/json';
    opcoes.body = JSON.stringify(corpo);
  }
  let resposta;
  try {
    resposta = await fetch(url, opcoes);
  } catch (e) {
    throw new Error('Não foi possível falar com o servidor. Ele está rodando?');
  }
  if (resposta.status === 204) return null;
  let dados = null;
  try { dados = await resposta.json(); } catch (e) { /* sem corpo JSON */ }
  if (!resposta.ok) throw new Error((dados && dados.erro) || ('Erro ' + resposta.status));
  return dados;
}

function toast(mensagem, erro = false) {
  const el = $('toast');
  el.textContent = mensagem;
  el.className = 'toast mostrar' + (erro ? ' erro' : '');
  clearTimeout(toast.t);
  toast.t = setTimeout(() => { el.className = 'toast'; }, 3500);
}

async function executar(promessa, mensagemOk) {
  try {
    const r = await promessa;
    if (mensagemOk) toast(mensagemOk);
    return r;
  } catch (e) {
    toast(e.message, true);
    return undefined;
  }
}

// ---------- carregar e renderizar ----------
async function carregar() {
  try {
    tarefas = await api('GET', '/api/tarefas');
  } catch (e) { toast(e.message, true); return; }
  renderizar();
  await atualizarDesfazer();
}

async function atualizarDesfazer() {
  try {
    const estado = await api('GET', '/api/desfazer/estado');
    $('btn-desfazer').disabled = !estado.podeDesfazer;
    $('contador-pilha').textContent = estado.quantidade;
    $('info-desfazer').textContent = estado.podeDesfazer ? 'Próxima ação a desfazer (topo da pilha): ' + estado.proximaAcao : '';
  } catch (e) { /* ignora */ }
}

function hojeISO() {
  const d = new Date();
  return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
}

function formatarData(iso) { return iso ? iso.split('-').reverse().join('/') : ''; }

function criar(tag, classe, texto) {
  const el = document.createElement(tag);
  if (classe) el.className = classe;
  if (texto !== undefined) el.textContent = texto;
  return el;
}

function renderizar() {
  for (const st of STATUS) {
    const lista = $('lista-' + st);
    lista.replaceChildren();
    const doStatus = tarefas.filter(t => t.status === st);
    $('cont-' + st).textContent = doStatus.length;
    if (doStatus.length === 0) lista.append(criar('div', 'vazio', 'Nenhuma tarefa'));
    doStatus.forEach(t => lista.append(criarCartao(t)));
  }
}

function criarCartao(t) {
  const c = criar('article', 'cartao ' + t.prioridade);
  c.draggable = true;
  c.dataset.id = t.id;
  c.addEventListener('dragstart', e => { e.dataTransfer.setData('text/plain', t.id); c.classList.add('arrastando'); });
  c.addEventListener('dragend', () => c.classList.remove('arrastando'));

  c.append(criar('h3', '', t.titulo));
  if (t.descricao) c.append(criar('p', '', t.descricao.length > 90 ? t.descricao.slice(0, 90) + '…' : t.descricao));

  const tags = criar('div', 'tags');
  tags.append(criar('span', 'tag ' + t.prioridade, 'Prioridade ' + ROTULO_PRIORIDADE[t.prioridade]));
  if (t.prazo) {
    const atrasada = t.status !== 'CONCLUIDA' && t.prazo < hojeISO();
    tags.append(criar('span', 'tag' + (atrasada ? ' atrasada' : ''), (atrasada ? '⚠ Atrasada · ' : '📅 ') + formatarData(t.prazo)));
  }
  if (t.estimativa) tags.append(criar('span', 'tag', '⏱ ' + t.estimativa));
  if (t.tarefaPaiId) tags.append(criar('span', 'tag sub', '↳ de: ' + t.tarefaPaiTitulo));
  if (t.totalSubtarefas > 0) tags.append(criar('span', 'tag sub', '☑ ' + t.subtarefasConcluidas + '/' + t.totalSubtarefas + ' subtarefas'));
  c.append(tags);

  const acoes = criar('div', 'cartao-acoes');
  const i = STATUS.indexOf(t.status);
  acoes.append(botao('◀', 'Mover para a coluna anterior', () => mover(t.id, STATUS[i - 1]), i === 0));
  acoes.append(botao('▶', 'Mover para a próxima coluna', () => mover(t.id, STATUS[i + 1]), i === STATUS.length - 1));
  acoes.append(botao('🔍', 'Detalhes', () => abrirDetalhes(t.id)));
  acoes.append(botao('✏️', 'Editar', () => abrirForm(t)));
  acoes.append(botao('✨', 'Auto-Planejamento com IA', () => abrirIA(t)));
  acoes.append(botao('🗑', 'Excluir', () => excluir(t)));
  c.append(acoes);
  return c;
}

function botao(texto, titulo, aoClicar, desabilitado = false) {
  const b = criar('button', '', texto);
  b.type = 'button';
  b.title = titulo;
  b.disabled = desabilitado;
  b.addEventListener('click', aoClicar);
  return b;
}

// ---------- ações do CRUD ----------
async function mover(id, novoStatus) {
  if (!novoStatus) return;
  const atual = tarefas.find(t => t.id === Number(id));
  if (atual && atual.status === novoStatus) return;
  await executar(api('PATCH', '/api/tarefas/' + id + '/status', { status: novoStatus }));
  await carregar();
}

async function excluir(t) {
  const aviso = t.totalSubtarefas > 0 ? '\nAs ' + t.totalSubtarefas + ' subtarefas também serão excluídas.' : '';
  if (!confirm('Excluir a tarefa "' + t.titulo + '"?' + aviso + '\n(Você poderá desfazer com Ctrl+Z.)')) return;
  await executar(api('DELETE', '/api/tarefas/' + t.id), 'Tarefa excluída. Ctrl+Z para desfazer.');
  await carregar();
}

async function desfazer() {
  if ($('btn-desfazer').disabled) return;
  const r = await executar(api('POST', '/api/desfazer'));
  if (r) toast(r.mensagem);
  await carregar();
}

// ---------- formulário criar/editar ----------
function abrirForm(t) {
  editandoId = t ? t.id : null;
  $('form-titulo').textContent = t ? 'Editar tarefa' : 'Nova tarefa';
  $('f-titulo').value = t ? t.titulo : '';
  $('f-descricao').value = t && t.descricao ? t.descricao : '';
  $('f-prioridade').value = t ? t.prioridade : 'MEDIA';
  $('f-prazo').value = t && t.prazo ? t.prazo : '';
  $('f-ia-apos').checked = false;
  $('lbl-ia-apos').hidden = !!t;
  $('dlg-form').showModal();
  $('f-titulo').focus();
}

$('form-tarefa').addEventListener('submit', async (e) => {
  e.preventDefault();
  const corpo = {
    titulo: $('f-titulo').value,
    descricao: $('f-descricao').value,
    prioridade: $('f-prioridade').value,
    prazo: $('f-prazo').value || null
  };
  const planejar = $('f-ia-apos').checked;
  const r = editandoId
    ? await executar(api('PUT', '/api/tarefas/' + editandoId, corpo), 'Tarefa atualizada.')
    : await executar(api('POST', '/api/tarefas', corpo), 'Tarefa criada.');
  if (!r) return; // erro: mantém o formulário aberto
  $('dlg-form').close();
  await carregar();
  if (planejar && !editandoId) abrirIA(r);
});
$('f-cancelar').addEventListener('click', () => $('dlg-form').close());

// ---------- detalhes ----------
function abrirDetalhes(id) {
  const t = tarefas.find(x => x.id === id);
  if (!t) return;
  detalheId = id;
  const corpo = $('detalhes-corpo');
  corpo.replaceChildren(criar('h3', '', t.titulo));
  const linha = (rotulo, valor) => {
    const p = criar('div', 'detalhe-linha');
    p.append(criar('b', '', rotulo), document.createTextNode(valor));
    corpo.append(p);
  };
  const nomes = { A_FAZER: 'A Fazer', EM_ANDAMENTO: 'Em andamento', CONCLUIDA: 'Concluída' };
  linha('Descrição:', t.descricao || '—');
  linha('Status:', nomes[t.status]);
  linha('Prioridade:', ROTULO_PRIORIDADE[t.prioridade]);
  linha('Prazo:', t.prazo ? formatarData(t.prazo) : '—');
  linha('Criada em:', new Date(t.dataCriacao).toLocaleString('pt-BR'));
  linha('Concluída em:', t.dataConclusao ? new Date(t.dataConclusao).toLocaleString('pt-BR') : '—');
  if (t.estimativa) linha('Estimativa (IA):', t.estimativa);
  if (t.tarefaPaiId) linha('Subtarefa de:', t.tarefaPaiTitulo);
  const subs = tarefas.filter(x => x.tarefaPaiId === id);
  if (subs.length) {
    corpo.append(criar('div', 'detalhe-linha', 'Subtarefas:'));
    const ul = criar('ul', 'sub-lista');
    subs.forEach(s => ul.append(criar('li', '', (s.status === 'CONCLUIDA' ? '✅ ' : '⬜ ') + s.titulo + (s.estimativa ? ' (' + s.estimativa + ')' : ''))));
    corpo.append(ul);
  }
  $('dlg-detalhes').showModal();
}
$('d-fechar').addEventListener('click', () => $('dlg-detalhes').close());
$('d-ia').addEventListener('click', () => {
  const t = tarefas.find(x => x.id === detalheId);
  $('dlg-detalhes').close();
  if (t) abrirIA(t);
});

// ---------- Auto-Planejamento com IA ----------
async function abrirIA(t) {
  iaTarefa = t;
  iaSugestoes = [];
  $('ia-tarefa').textContent = 'Tarefa: ' + t.titulo;
  $('ia-salvar').hidden = true;
  $('ia-descartar').textContent = 'Fechar';
  $('ia-conteudo').replaceChildren(criar('div', 'carregando', '⏳ A IA está analisando a tarefa…'));
  if (!$('dlg-ia').open) $('dlg-ia').showModal();
  try {
    iaSugestoes = await api('POST', '/api/ia/planejar', { titulo: t.titulo, descricao: t.descricao || '' });
    renderizarSugestoes();
  } catch (e) {
    // Chave ausente, sem internet, cota excedida, etc.: mostramos a mensagem real, sem fingir sucesso
    $('ia-conteudo').replaceChildren(criar('div', 'ia-erro', '⚠ ' + e.message));
  }
}

function renderizarSugestoes() {
  const area = $('ia-conteudo');
  area.replaceChildren(criar('p', 'muted', 'Marque as subtarefas que deseja salvar. Elas serão criadas como tarefas individuais vinculadas à principal.'));
  iaSugestoes.forEach((s, idx) => {
    const rotulo = criar('label', 'ia-item');
    const cb = document.createElement('input');
    cb.type = 'checkbox';
    cb.checked = true;
    cb.dataset.idx = idx;
    const texto = criar('span');
    texto.append(criar('strong', '', s.titulo));
    if (s.descricao) texto.append(criar('small', '', s.descricao));
    texto.append(criar('small', '', '⏱ Estimativa: ' + (s.estimativa || 'não informada')));
    rotulo.append(cb, texto);
    area.append(rotulo);
  });
  $('ia-salvar').hidden = false;
  $('ia-descartar').textContent = 'Descartar tudo';
}

$('ia-salvar').addEventListener('click', async () => {
  const marcadas = [...document.querySelectorAll('#ia-conteudo input[type=checkbox]:checked')]
    .map(cb => iaSugestoes[Number(cb.dataset.idx)]);
  if (marcadas.length === 0) { toast('Selecione ao menos uma subtarefa ou clique em Descartar.', true); return; }
  const r = await executar(api('POST', '/api/ia/aceitar', { tarefaPaiId: iaTarefa.id, subtarefas: marcadas }),
    marcadas.length + ' subtarefa(s) salva(s).');
  if (!r) return;
  $('dlg-ia').close();
  await carregar();
});
$('ia-descartar').addEventListener('click', () => $('dlg-ia').close());

// ---------- eventos globais ----------
$('btn-nova').addEventListener('click', () => abrirForm(null));
$('btn-desfazer').addEventListener('click', desfazer);

// Atalho CTRL + Z (Cmd + Z no Mac). Ignorado enquanto o usuário digita num campo de texto,
// para não atrapalhar o "desfazer" nativo da digitação.
document.addEventListener('keydown', (e) => {
  const digitando = ['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement.tagName);
  if ((e.ctrlKey || e.metaKey) && !e.shiftKey && e.key.toLowerCase() === 'z' && !digitando) {
    e.preventDefault();
    desfazer();
  }
});

// Arrastar e soltar entre colunas
document.querySelectorAll('.coluna').forEach(col => {
  col.addEventListener('dragover', e => { e.preventDefault(); col.classList.add('alvo'); });
  col.addEventListener('dragleave', () => col.classList.remove('alvo'));
  col.addEventListener('drop', e => {
    e.preventDefault();
    col.classList.remove('alvo');
    mover(e.dataTransfer.getData('text/plain'), col.dataset.status);
  });
});

carregar();
