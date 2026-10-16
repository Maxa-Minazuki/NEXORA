/*
 * Брондау формасы: қонақтарды қосу/жою, алдын ала баға есебі (сервердегі PricingService арқылы),
 * таңдалған күндегі бос емес уақыттар. Соңғы тексеру және соңғы баға — серверде.
 */
(function () {
  'use strict';

  document.addEventListener('DOMContentLoaded', function () {
    initGuests();
    initQuote();
  });

  /* ---------- Қонақтар ---------- */
  function initGuests() {
    var list = document.getElementById('guest-list');
    var tpl = document.getElementById('guest-template');
    var addBtn = document.getElementById('add-guest');
    if (!list || !tpl || !addBtn) return;
    var empty = document.getElementById('guest-empty');
    var counter = document.getElementById('guest-counter');
    var guestCount = document.getElementById('guestCount');
    var MAX = 50;

    function rows() { return list.querySelectorAll('.guest-row'); }

    function reindex() {
      rows().forEach(function (row, i) {
        row.querySelector('.guest-index').textContent = String(i + 1);
        row.querySelectorAll('[data-field]').forEach(function (input) {
          var f = input.getAttribute('data-field');
          input.name = 'guests[' + i + '].' + f;
          input.id = 'guests' + i + '.' + f;
          var label = input.closest('.field') && input.closest('.field').querySelector('label');
          if (label) label.setAttribute('for', input.id);
        });
        var rm = row.querySelector('[data-remove-guest]');
        if (rm) rm.setAttribute('aria-label', (i + 1) + '-қонақты жою');
      });
      var n = rows().length;
      if (empty) empty.hidden = n > 0;
      if (counter) counter.textContent = String(n);
      var limit = Math.min(MAX, guestCount && guestCount.value ? parseInt(guestCount.value, 10) || MAX : MAX);
      addBtn.disabled = n >= limit;
      addBtn.title = n >= limit ? 'Тізімдегі қонақтар саны жалпы қонақтар санынан аспауы керек' : '';
    }

    addBtn.addEventListener('click', function () {
      var node = tpl.content.firstElementChild.cloneNode(true);
      list.appendChild(node);
      reindex();
      var first = node.querySelector('input');
      if (first) first.focus();
    });

    list.addEventListener('click', function (e) {
      var btn = e.target.closest('[data-remove-guest]');
      if (!btn) return;
      var row = btn.closest('.guest-row');
      row.remove();
      reindex();
      addBtn.focus();
    });

    if (guestCount) guestCount.addEventListener('input', reindex);
    reindex();
  }

  /* ---------- Баға есебі және бос уақыт ---------- */
  function initQuote() {
    var form = document.getElementById('booking-form');
    var summary = document.getElementById('summary');
    if (!form || !summary) return;

    var date = document.getElementById('date');
    var start = document.getElementById('startTime');
    var end = document.getElementById('endTime');
    var guests = document.getElementById('guestCount');
    var status = document.getElementById('quote-status');
    var submit = document.getElementById('submit-booking');
    var timeline = document.getElementById('timeline');
    var busyList = document.getElementById('busy-list');
    var timer = null;
    var seq = 0;

    function roomInput() { return form.querySelector('input[name="roomId"]:checked'); }

    function set(name, value) {
      summary.querySelectorAll('[data-q="' + name + '"]').forEach(function (el) { el.textContent = value; });
    }

    function showStatus(kind, text) {
      if (!status) return;
      if (!text) { status.innerHTML = ''; return; }
      var icon = kind === 'ok' ? 'bi-check-circle' : 'bi-exclamation-triangle';
      var cls = kind === 'ok' ? 'alert-success' : 'alert-warning';
      status.innerHTML = '';
      var div = document.createElement('div');
      div.className = 'alert ' + cls;
      div.setAttribute('role', 'status');
      var i = document.createElement('i');
      i.className = 'bi ' + icon;
      i.setAttribute('aria-hidden', 'true');
      var span = document.createElement('span');
      span.textContent = text;
      div.appendChild(i); div.appendChild(span);
      status.appendChild(div);
    }

    function updateRoomPanel() {
      var r = roomInput();
      if (!r) return;
      set('room', r.dataset.name);
      var rl = r.closest('label').querySelector('[data-rate-label]');
      set('rate', rl ? rl.textContent : '—');
      set('capacity', r.dataset.capacity + ' адамға дейін');
      if (guests) guests.max = r.dataset.capacity;
      var eq = document.getElementById('room-equipment');
      if (eq) {
        eq.innerHTML = '';
        (r.dataset.equipment || '').split('|').filter(Boolean).forEach(function (item) {
          var c = document.createElement('span');
          c.className = 'chip';
          c.textContent = item;
          eq.appendChild(c);
        });
      }
      var cap = document.getElementById('capacity-hint');
      if (cap) cap.textContent = 'Бұл бөлмеге ' + r.dataset.capacity + ' адамға дейін';
    }

    function schedule() {
      clearTimeout(timer);
      timer = setTimeout(refresh, 250);
    }

    function refresh() {
      var r = roomInput();
      if (!r || !date.value || !start.value || !end.value) return;
      var mySeq = ++seq;
      summary.classList.add('is-loading');
      var params = new URLSearchParams({
        roomId: r.value, date: date.value, start: start.value, end: end.value
      });
      if (guests && guests.value) params.set('guests', guests.value);

      fetch('/api/pricing/quote?' + params.toString(), { headers: { 'Accept': 'application/json' } })
        .then(function (res) { return res.json(); })
        .then(function (q) {
          if (mySeq !== seq) return;
          set('rate', q.hourlyRate || '—');
          set('duration', q.duration ? q.duration + ' · ' + q.billableHours + ' сағат есептеледі' : '—');
          set('total', q.total || '—');
          set('deposit', q.deposit || '—');
          set('now', q.deposit || '—');
          set('balance', q.balance || '—');
          if (q.ok) showStatus('ok', 'Бұл уақыт бос. Брондауға болады');
          else showStatus('warn', q.message || 'Деректерді тексеріңіз');
          if (submit) submit.disabled = !q.ok;
        })
        .catch(function () {
          if (mySeq !== seq) return;
          showStatus('warn', 'Есептеу уақытша қолжетімсіз. Форманы жіберсеңіз, сервер бәрін тексереді');
          if (submit) submit.disabled = false;
        })
        .finally(function () { if (mySeq === seq) summary.classList.remove('is-loading'); });

      loadBusy(r.value, date.value);
    }

    function minutes(hhmm) {
      var p = hhmm.split(':');
      return parseInt(p[0], 10) * 60 + parseInt(p[1], 10);
    }

    function loadBusy(roomId, day) {
      if (!timeline) return;
      var open = minutes(timeline.dataset.open);
      var close = minutes(timeline.dataset.close);
      fetch('/api/rooms/' + encodeURIComponent(roomId) + '/busy?date=' + encodeURIComponent(day))
        .then(function (res) { return res.json(); })
        .then(function (slots) {
          timeline.querySelectorAll('.busy').forEach(function (b) { b.remove(); });
          busyList.innerHTML = '';
          if (!Array.isArray(slots) || slots.length === 0) {
            var s = document.createElement('span');
            s.className = 'slot free';
            s.textContent = 'Күні бойы бос';
            busyList.appendChild(s);
            return;
          }
          slots.forEach(function (slot) {
            var a = Math.max(open, minutes(slot.start));
            var b = Math.min(close, minutes(slot.end));
            if (b > a) {
              var bar = document.createElement('div');
              bar.className = 'busy';
              bar.style.left = ((a - open) / (close - open) * 100) + '%';
              bar.style.width = ((b - a) / (close - open) * 100) + '%';
              bar.title = slot.start + '–' + slot.end + ' бос емес';
              timeline.appendChild(bar);
            }
            var chip = document.createElement('span');
            chip.className = 'slot';
            chip.textContent = slot.start + '–' + slot.end;
            busyList.appendChild(chip);
          });
        })
        .catch(function () { busyList.textContent = 'Бос емес уақыттарды жүктеу мүмкін болмады'; });
    }

    form.querySelectorAll('input[name="roomId"]').forEach(function (r) {
      r.addEventListener('change', function () { updateRoomPanel(); schedule(); });
    });
    [date, start, end, guests].forEach(function (el) {
      if (el) { el.addEventListener('change', schedule); el.addEventListener('input', schedule); }
    });

    // Қақтығыс кезінде ұсынылған уақытты бір басумен таңдау.
    document.querySelectorAll('[data-suggest-start]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        start.value = btn.dataset.suggestStart;
        end.value = btn.dataset.suggestEnd;
        schedule();
        JYLY.toast('Уақыт өзгертілді: ' + btn.dataset.suggestStart + '–' + btn.dataset.suggestEnd, 'info');
      });
    });

    updateRoomPanel();
    refresh();
  }
})();
