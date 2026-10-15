// Métricas visuales de BrowseMe. Se ejecuta en un mundo aislado de Chrome: ve el
// mismo DOM que la página, pero sus globales (RegExp, Math, getComputedStyle…) no
// pueden ser sustituidos por el JavaScript de la página. El resultado es dato no
// confiable igualmente y el backend lo valida.
// __CTA_PATTERN__ se sustituye por el patrón de CallToActionVocabulary (literal JSON).
(() => {
  const CTA = new RegExp(__CTA_PATTERN__);
  const MAX_TEXT_ELEMENTS = 1500;
  const MAX_TAP_TARGETS = 600;
  const MAX_SCANNED = 6000;

  window.scrollTo(0, 0);
  const root = document.documentElement;
  // Ventana de diseño (layout viewport): innerWidth crece si el móvil aleja la vista por desbordamiento.
  const vw = root.clientWidth || window.innerWidth;
  const vh = root.clientHeight || window.innerHeight;
  const body = document.body;
  const contentWidth = Math.max(root.scrollWidth, body ? body.scrollWidth : 0);
  const pageHeight = Math.max(root.scrollHeight, body ? body.scrollHeight : 0);

  const normalize = (text) => (text || '').normalize('NFD').replace(/[̀-ͯ]/g, '')
    .toLowerCase().replace(/\s+/g, ' ').trim();

  const isVisible = (el, cs) => {
    if (cs.display === 'none' || cs.visibility === 'hidden' || parseFloat(cs.opacity) === 0) return false;
    const r = el.getBoundingClientRect();
    return r.width > 0 && r.height > 0;
  };

  const ownText = (el) => {
    let text = '';
    for (const node of el.childNodes) {
      if (node.nodeType === 3) text += node.nodeValue;
    }
    return text.replace(/\s+/g, ' ').trim();
  };

  const parseColor = (value) => {
    const m = /^rgba?\(([^)]+)\)$/.exec(value || '');
    if (!m) return null;
    const p = m[1].split(/[\s,/]+/).filter(Boolean).map(Number);
    if (p.length < 3 || p.some(Number.isNaN)) return null;
    return { r: p[0], g: p[1], b: p[2], a: p.length > 3 ? p[3] : 1 };
  };

  const luminance = ({ r, g, b }) => {
    const f = (v) => {
      const c = v / 255;
      return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    };
    return 0.2126 * f(r) + 0.7152 * f(g) + 0.0722 * f(b);
  };

  // Primer fondo opaco hacia arriba. Imágenes, degradados o fondos semitransparentes: no medible.
  const backgroundOf = (el) => {
    for (let e = el; e && e.nodeType === 1; e = e.parentElement) {
      const cs = getComputedStyle(e);
      if (cs.backgroundImage && cs.backgroundImage !== 'none') return null;
      const c = parseColor(cs.backgroundColor);
      if (c && c.a >= 0.99) return c;
      if (c && c.a > 0) return null;
    }
    return { r: 255, g: 255, b: 255, a: 1 };
  };

  let textChecked = 0;
  let lowContrast = 0;
  let smallText = 0;
  const samples = [];
  const fonts = [];
  const all = body ? body.getElementsByTagName('*') : [];
  for (let i = 0; i < all.length && i < MAX_SCANNED && textChecked < MAX_TEXT_ELEMENTS; i++) {
    const el = all[i];
    const text = ownText(el);
    if (!/[\p{L}\p{N}]/u.test(text)) continue;
    const cs = getComputedStyle(el);
    if (!isVisible(el, cs)) continue;
    textChecked++;
    const size = parseFloat(cs.fontSize) || 16;
    if (size < 12) smallText++;
    const family = (cs.fontFamily || '').split(',')[0].replace(/["']/g, '').trim();
    if (family && !fonts.includes(family) && fonts.length < 10) fonts.push(family);
    const fg = parseColor(cs.color);
    const bg = backgroundOf(el);
    if (fg && bg && fg.a >= 0.99) {
      const l1 = luminance(fg);
      const l2 = luminance(bg);
      const ratio = (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05);
      const weight = parseInt(cs.fontWeight, 10) || 400;
      const large = size >= 24 || (size >= 18.66 && weight >= 700);
      if (ratio < (large ? 3 : 4.5)) {
        lowContrast++;
        if (samples.length < 5) samples.push(text.slice(0, 80));
      }
    }
  }

  // Enlaces dentro de un párrafo: WCAG 2.5.8 los exime del tamaño mínimo.
  const isInlineLink = (el, cs) => {
    if (el.tagName !== 'A' || !cs.display.startsWith('inline') || cs.display === 'inline-block') return false;
    const parent = el.parentElement;
    return !!parent && /[\p{L}\p{N}]/u.test(ownText(parent));
  };

  let tapChecked = 0;
  let tapSmall = 0;
  const targets = document.querySelectorAll(
    'a[href], button, input:not([type=hidden]), select, textarea, [role=button]');
  for (let i = 0; i < targets.length && tapChecked < MAX_TAP_TARGETS; i++) {
    const el = targets[i];
    const cs = getComputedStyle(el);
    if (!isVisible(el, cs) || isInlineLink(el, cs)) continue;
    tapChecked++;
    const r = el.getBoundingClientRect();
    if (r.width < 24 || r.height < 24) tapSmall++;
  }

  const ctaInFirstScreen = () => {
    const candidates = document.querySelectorAll(
      'a[href], button, [role=button], input[type=submit], input[type=button]');
    for (const el of candidates) {
      const label = el.tagName === 'INPUT' ? el.value : (el.innerText || el.textContent);
      const texts = [normalize(label), normalize(el.getAttribute('aria-label'))];
      if (!texts.some((t) => t && t.length <= 60 && CTA.test(t))) continue;
      const cs = getComputedStyle(el);
      if (!isVisible(el, cs)) continue;
      const r = el.getBoundingClientRect();
      if (r.bottom > 0 && r.top < vh && r.right > 0 && r.left < vw) return true;
    }
    return false;
  };

  return {
    viewportWidth: vw,
    contentWidth: contentWidth,
    textElementsChecked: textChecked,
    lowContrastTexts: lowContrast,
    lowContrastSamples: samples,
    smallTexts: smallText,
    tapTargetsChecked: tapChecked,
    smallTapTargets: tapSmall,
    fontFamilies: fonts,
    callToActionAboveFold: ctaInFirstScreen(),
    pageHeightScreens: vh > 0 ? Math.round(pageHeight / vh) : 0,
  };
})()
