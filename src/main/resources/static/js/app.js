/* JYLY — ортақ интерфейс мінез-құлқы. Бизнес-логика серверде; мұнда тек UX. */
(function () {
  'use strict';

  var PLACEHOLDER = '/images/room-placeholder.svg';

  /* ---------- Сынған суреттерді алмастыру ---------- */
  function fixImage(img) {
    if (img.dataset.fallbackApplied) return;
    img.dataset.fallbackApplied = '1';
    img.src = PLACEHOLDER;
  }
  document.addEventListener('error', function (e) {
    if (e.target && e.target.tagName === 'IMG') fixImage(e.target);
  }, true);

  document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('img').forEach(function (img) {
      if (img.complete && img.naturalWidth === 0 && img.getAttribute('src')) fixImage(img);
    });

    /* ---------- Навигация ---------- */
    var header = document.querySelector('.site-header');
    if (header) {
      var onScroll = function () { header.classList.toggle('is-scrolled', window.scrollY > 8); };
      onScroll();
      window.addEventListener('scroll', onScroll, { passive: true });
    }
    document.querySelectorAll('[data-nav-toggle]').forEach(function (btn) {
      var target = document.getElementById(btn.getAttribute('aria-controls'));
      if (!target) return;
      btn.addEventListener('click', function () {
        var open = target.classList.toggle('is-open');
        btn.setAttribute('aria-expanded', open ? 'true' : 'false');
      });
      target.querySelectorAll('a').forEach(function (a) {
        a.addEventListener('click', function () {
          target.classList.remove('is-open');
          btn.setAttribute('aria-expanded', 'false');
        });
      });
    });

    /* ---------- Toast хабарламалары ---------- */
    document.querySelectorAll('.toast').forEach(function (t) { scheduleToast(t); });

    /* ---------- Пайда болу анимациясы ---------- */
    var reveals = document.querySelectorAll('.reveal');
    if ('IntersectionObserver' in window) {
      var io = new IntersectionObserver(function (entries) {
        entries.forEach(function (en) {
          if (en.isIntersecting) { en.target.classList.add('is-visible'); io.unobserve(en.target); }
        });
      }, { threshold: 0.12 });
      reveals.forEach(function (el) { io.observe(el); });
    } else {
      reveals.forEach(function (el) { el.classList.add('is-visible'); });
    }

    /* ---------- Қауіпті әрекеттерді растау ---------- */
    var dialog = document.getElementById('confirm-dialog');
    var pendingForm = null;
    document.querySelectorAll('form[data-confirm]').forEach(function (form) {
      form.addEventListener('submit', function (e) {
        if (form.dataset.confirmed === '1' || !dialog || typeof dialog.showModal !== 'function') {
          if (!dialog && !window.confirm(form.dataset.confirm)) e.preventDefault();
          return;
        }
        e.preventDefault();
        pendingForm = form;
        dialog.querySelector('[data-confirm-title]').textContent = form.dataset.confirmTitle || 'Растаңыз';
        var extra = form.querySelector('[data-confirm-extra]');
        dialog.querySelector('[data-confirm-text]').textContent = form.dataset.confirm + (extra ? ' ' + extra.textContent : '');
        dialog.querySelector('[data-confirm-ok]').textContent = form.dataset.confirmOk || 'Иә, жалғастыру';
        dialog.showModal();
      });
    });
    if (dialog) {
      dialog.querySelector('[data-confirm-cancel]').addEventListener('click', function () { dialog.close(); pendingForm = null; });
      dialog.querySelector('[data-confirm-ok]').addEventListener('click', function () {
        if (!pendingForm) return;
        pendingForm.dataset.confirmed = '1';
        dialog.close();
        if (pendingForm.requestSubmit) pendingForm.requestSubmit(); else pendingForm.submit();
      });
    }

    /* ---------- Форманы екі рет жіберуден қорғау + жүктелу күйі ---------- */
    document.querySelectorAll('form').forEach(function (form) {
      form.addEventListener('submit', function (e) {
        if (e.defaultPrevented) return;
        if (form.method.toLowerCase() !== 'post') return;
        var btn = e.submitter || form.querySelector('button[type="submit"]');
        if (form.dataset.submitting === '1') { e.preventDefault(); return; }
        form.dataset.submitting = '1';
        if (btn) btn.classList.add('is-loading');
      });
    });
    // Артқа қайтқанда (bfcache) батырмаларды қалпына келтіру.
    window.addEventListener('pageshow', function () {
      document.querySelectorAll('form').forEach(function (f) { delete f.dataset.submitting; delete f.dataset.confirmed; });
      document.querySelectorAll('.is-loading').forEach(function (b) { b.classList.remove('is-loading'); });
    });

    /* ---------- Әкімші: бүйір мәзірі ---------- */
    var side = document.getElementById('admin-side');
    document.querySelectorAll('[data-admin-toggle]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var open = side.classList.toggle('is-open');
        btn.setAttribute('aria-expanded', open ? 'true' : 'false');
      });
    });
  });

  function scheduleToast(t) {
    var close = function () {
      t.classList.add('is-leaving');
      setTimeout(function () { t.remove(); }, 300);
    };
    var btn = t.querySelector('button');
    if (btn) btn.addEventListener('click', close);
    setTimeout(close, 6000);
  }

  /** Басқа скрипттер үшін: JYLY.toast('Мәтін', 'success'|'error'|'info'). */
  window.JYLY = window.JYLY || {};
  window.JYLY.toast = function (message, type) {
    var box = document.querySelector('.toasts');
    if (!box) return;
    var icons = { success: 'bi-check-circle-fill', error: 'bi-exclamation-octagon-fill', info: 'bi-info-circle-fill' };
    var t = document.createElement('div');
    t.className = 'toast ' + (type || 'info');
    t.setAttribute('role', 'status');
    var i = document.createElement('i');
    i.className = 'bi ' + (icons[type] || icons.info);
    i.setAttribute('aria-hidden', 'true');
    var span = document.createElement('span');
    span.textContent = message;
    var b = document.createElement('button');
    b.type = 'button';
    b.setAttribute('aria-label', 'Жабу');
    b.innerHTML = '<i class="bi bi-x-lg" aria-hidden="true"></i>';
    t.appendChild(i); t.appendChild(span); t.appendChild(b);
    box.appendChild(t);
    scheduleToast(t);
  };
})();
