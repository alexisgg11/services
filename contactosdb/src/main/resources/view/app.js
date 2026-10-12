'use strict';
const $ = id => document.getElementById(id);
const state = { products: [], catalogs: null, deleteId: null, pending: false, loadVersion: 0, page: 1, pageSize: 10, total: 0, pages: 1, loading: false, service: null, servicePending: false };
const money = value => new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(value);
const escapeHtml = value => String(value).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
async function api(path, options = {}) {
  const response = await fetch('/api' + path, options);
  const data = await response.json();
  if (!response.ok) throw new Error(response.status >= 500 ? 'El servicio no está disponible. Intenta nuevamente.' : data.error || 'No se pudo completar la solicitud.');
  return data;
}
function notice(message) { $('notice').textContent = message; $('notice').hidden = !message; }
function fillSelect(id, items, key, label, placeholder) {
  $(id).replaceChildren();
  if (placeholder) $(id).add(new Option(placeholder, ''));
  for (const item of items) $(id).add(new Option(item[label], item[key]));
}
function renderCatalogs() {
  const { tipos, proveedores, unidades } = state.catalogs;
  fillSelect('filter-type', tipos, 'idTipo', 'descripcion', 'Todos los tipos');
  fillSelect('filter-supplier', proveedores, 'idProveedor', 'nombre', 'Todos los proveedores');
  fillSelect('form-type', tipos, 'idTipo', 'descripcion', 'Selecciona un tipo');
  fillSelect('form-supplier', proveedores, 'idProveedor', 'nombre', 'Selecciona un proveedor');
  fillSelect('form-unit', unidades, 'idUnidad', 'descripcion', 'Selecciona una unidad');
  fillSelect('service-type', tipos, 'idTipo', 'descripcion', 'Selecciona un tipo');
  fillSelect('service-supplier', proveedores, 'idProveedor', 'nombre', 'Selecciona un proveedor');
  $('stat-suppliers').textContent = proveedores.length;
  $('stat-types').textContent = tipos.length;
  $('type-list').innerHTML = tipos.map(t => `<div class="catalog-item"><span>${escapeHtml(t.descripcion)}</span><small>ID ${t.idTipo}</small></div>`).join('');
  $('unit-list').innerHTML = unidades.map(u => `<div class="catalog-item"><span>${escapeHtml(u.descripcion)}</span><small>${escapeHtml(u.simbolo)}</small></div>`).join('');
}
const icons = {
  edit: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m16 3 5 5-12 12-6 1 1-6Z"/><path d="m14 5 5 5"/></svg>',
  delete: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7M14 10v7"/></svg>'
};
function productRow(p, actions) {
  return `<tr><td>${String(p.idProducto).padStart(3, '0')}</td><td>${escapeHtml(p.descripcion)}</td><td><span class="type-tag">${escapeHtml(p.tipo)}</span></td><td>${escapeHtml(p.proveedor)}</td><td>${escapeHtml(p.simbolo)}</td><td class="number">${money(p.precio)}</td>${actions ? `<td><div class="row-actions"><button class="row-action" data-action="edit" data-id="${p.idProducto}" aria-label="Editar ${escapeHtml(p.descripcion)}" title="Editar">${icons.edit}</button><button class="row-action" data-action="delete" data-id="${p.idProducto}" aria-label="Eliminar ${escapeHtml(p.descripcion)}" title="Eliminar">${icons.delete}</button></div></td>` : ''}</tr>`;
}
function renderProducts() {
  const total = state.total;
  const pages = state.pages;
  const start = (state.page - 1) * state.pageSize;
  $('product-rows').innerHTML = total ? state.products.map(p => productRow(p, true)).join('') : '<tr><td colspan="7" class="empty">No se encontraron productos.</td></tr>';
  $('count').textContent = `${total} ${total === 1 ? 'producto' : 'productos'}`;
  $('table-summary').textContent = `${total ? start + 1 : 0}–${Math.min(start + state.pageSize, total)} de ${total} ${total === 1 ? 'producto' : 'productos'}`;
  $('page-label').textContent = `Página ${state.page} de ${pages}`;
  $('first-page').disabled = $('previous-page').disabled = state.loading || state.page === 1;
  $('next-page').disabled = $('last-page').disabled = state.loading || state.page === pages;
}
async function loadProducts() {
  const version = ++state.loadVersion;
  state.loading = true;
  for (const id of ['first-page', 'previous-page', 'next-page', 'last-page']) $(id).disabled = true;
  const query = new URLSearchParams({ buscar: $('search').value, tipo: $('filter-type').value, proveedor: $('filter-supplier').value, pagina: state.page, tamano: state.pageSize });
  try {
    const data = await api('/productos?' + query);
    if (version !== state.loadVersion) return;
    state.products = data.items; state.total = data.total; state.page = data.pagina; state.pages = data.paginas;
    state.loading = false;
    renderProducts();
  } catch (error) {
    if (version !== state.loadVersion) return;
    state.products = [];
    state.total = 0; state.page = 1; state.pages = 1; state.loading = false;
    renderProducts();
    notice('No se pudo cargar el catálogo. Intenta nuevamente.');
    $('product-rows').innerHTML = '<tr><td colspan="7" class="empty">No se pudo cargar el catálogo. Revisa la conexión e intenta actualizar la página.</td></tr>';
    $('table-summary').textContent = 'Catálogo no disponible';
  }
}
async function refreshSummary() {
  const summary = await api('/resumen');
  $('stat-products').textContent = summary.productos;
  const max = summary.masCaro;
  $('stat-max').textContent = max ? money(max.precio) : '—';
  $('stat-max-name').textContent = max ? `${max.descripcion} / ${max.simbolo}` : 'Sin productos registrados';
  $('supplier-rows').innerHTML = state.catalogs.proveedores.map(p => `<tr><td>${p.idProveedor}</td><td>${escapeHtml(p.nombre)}</td><td>${escapeHtml(p.ruc)}</td><td>${summary.productosPorProveedor[p.idProveedor] || 0}</td></tr>`).join('');
}
const pages = {
  productos: 'Productos', proveedores: 'Proveedores', catalogos: 'Tipos y unidades', servicios: 'Servicios'
};
for (const button of document.querySelectorAll('[data-page]')) button.addEventListener('click', () => {
  const page = button.dataset.page;
  for (const section of document.querySelectorAll('.page-section')) section.hidden = section.id !== page;
  for (const nav of document.querySelectorAll('[data-page]')) nav.classList.toggle('active', nav === button);
  $('page-title').textContent = $('breadcrumb').textContent = pages[page];
  $('new-product').hidden = page !== 'productos';
});
function openForm(product) {
  if (!state.catalogs) { notice('El catálogo aún no está disponible. Revisa la conexión.'); return; }
  const form = $('product-form'); form.reset(); $('form-error').hidden = true;
  $('product-id').value = product?.idProducto || '';
  $('dialog-title').textContent = product ? 'Editar producto' : 'Nuevo producto';
  if (product) {
    for (const key of ['descripcion', 'precio', 'idTipo', 'idProveedor', 'idUnidad']) form.elements[key].value = product[key];
  }
  $('product-dialog').showModal();
}
$('new-product').addEventListener('click', () => openForm());
for (const id of ['close-dialog', 'cancel-dialog']) $(id).addEventListener('click', () => $('product-dialog').close());
$('product-rows').addEventListener('click', event => {
  const button = event.target.closest('[data-action]'); if (!button) return;
  const product = state.products.find(p => p.idProducto === Number(button.dataset.id)); if (!product) return;
  if (button.dataset.action === 'edit') openForm(product);
  else {
    state.deleteId = product.idProducto;
    $('delete-description').textContent = `¿Eliminar “${product.descripcion}”?`;
    $('delete-error').hidden = true; $('delete-dialog').showModal();
  }
});
$('product-form').addEventListener('submit', async event => {
  event.preventDefault(); if (state.pending) return;
  state.pending = true; $('save-product').disabled = true; $('form-error').hidden = true;
  const id = $('product-id').value;
  try {
    await api('/productos' + (id ? '/' + id : ''), { method: id ? 'PUT' : 'POST', body: new URLSearchParams(new FormData(event.target)) });
    $('product-dialog').close(); notice(id ? 'Producto actualizado.' : 'Producto registrado.');
    await loadProducts(); await refreshSummary();
  } catch (error) {
    if ($('product-dialog').open) { $('form-error').textContent = error.message; $('form-error').hidden = false; }
    else notice(error.message);
  } finally { state.pending = false; $('save-product').disabled = false; }
});
$('cancel-delete').addEventListener('click', () => $('delete-dialog').close());
$('confirm-delete').addEventListener('click', async () => {
  if (state.pending) return;
  state.pending = true; $('confirm-delete').disabled = true;
  try {
    await api('/productos/' + state.deleteId, { method: 'DELETE' });
    $('delete-dialog').close(); notice('Producto eliminado.');
    await loadProducts(); await refreshSummary();
  } catch (error) {
    if ($('delete-dialog').open) { $('delete-error').textContent = error.message; $('delete-error').hidden = false; }
    else notice(error.message);
  } finally { state.pending = false; $('confirm-delete').disabled = false; }
});
let debounce;
function resetQuery() { clearTimeout(debounce); state.page = 1; return loadProducts(); }
$('search').addEventListener('input', () => { clearTimeout(debounce); state.page = 1; debounce = setTimeout(loadProducts, 200); });
for (const id of ['filter-type', 'filter-supplier']) $(id).addEventListener('change', resetQuery);
$('clear-filters').addEventListener('click', () => { $('search').value = $('filter-type').value = $('filter-supplier').value = ''; resetQuery(); });
$('page-size').addEventListener('change', () => { state.pageSize = Number($('page-size').value); state.page = 1; loadProducts(); });
for (const [id, move] of Object.entries({ 'first-page': () => 1, 'previous-page': () => state.page - 1, 'next-page': () => state.page + 1, 'last-page': () => state.pages })) {
  $(id).addEventListener('click', () => { state.page = move(); loadProducts(); });
}
async function runService(path, title, button, page = 1) {
  if (state.servicePending) return;
  state.servicePending = true;
  for (const id of ['service-max', 'service-by-supplier', 'service-by-type', 'service-previous', 'service-next']) $(id).disabled = true;
  try {
    const [name, query] = path.split('?');
    const params = new URLSearchParams(query); params.set('pagina', page); params.set('tamano', '10');
    const data = await api('/servicios/' + name + '?' + params);
    const paginated = Array.isArray(data.items);
    const rows = paginated ? data.items : [data];
    const total = paginated ? data.total : rows.length;
    state.service = paginated ? { path, title, page: data.pagina, pages: data.paginas } : null;
    $('result-footer').hidden = !paginated;
    $('service-page-label').textContent = paginated ? `Página ${data.pagina} de ${data.paginas}` : '';
    $('result-title').textContent = title; $('result-description').textContent = `${total} ${total === 1 ? 'producto' : 'productos'}`;
    $('result-content').innerHTML = rows.length ? `<table><thead><tr><th>ID</th><th>Producto</th><th>Tipo</th><th>Proveedor</th><th>Unidad</th><th class="number">Precio</th></tr></thead><tbody>${rows.map(p => productRow(p, false)).join('')}</tbody></table>` : 'No hay productos para esta consulta.';
    notice('');
  } catch (error) { notice(error.message); }
  finally {
    state.servicePending = false;
    for (const id of ['service-max', 'service-by-supplier', 'service-by-type']) $(id).disabled = false;
    $('service-previous').disabled = !state.service || state.service.page === 1;
    $('service-next').disabled = !state.service || state.service.page === state.service.pages;
  }
}
$('service-previous').addEventListener('click', event => { if (state.service) runService(state.service.path, state.service.title, event.currentTarget, state.service.page - 1); });
$('service-next').addEventListener('click', event => { if (state.service) runService(state.service.path, state.service.title, event.currentTarget, state.service.page + 1); });
$('service-max').addEventListener('click', event => runService('producto-mas-caro', 'Producto de mayor costo unitario', event.currentTarget));
$('service-by-supplier').addEventListener('click', event => {
  const id = $('service-supplier').value;
  if (!id) { notice('Selecciona un proveedor para consultar.'); return; }
  runService('por-proveedor?id=' + id, 'Productos por proveedor', event.currentTarget);
});
$('service-by-type').addEventListener('click', event => {
  const id = $('service-type').value;
  if (!id) { notice('Selecciona un tipo para consultar.'); return; }
  runService('por-tipo?id=' + id, 'Productos por tipo', event.currentTarget);
});
async function start() {
  try {
    state.catalogs = await api('/catalogos'); renderCatalogs();
    await loadProducts(); await refreshSummary();
  } catch (error) {
    notice('No se pudo cargar el catálogo. Intenta nuevamente.'); $('new-product').disabled = true;
    $('product-rows').innerHTML = '<tr><td colspan="7" class="empty">Catálogo no disponible.</td></tr>';
    $('table-summary').textContent = 'Catálogo no disponible';
  }
}
start();
