/* ============================================
   顶红体育后台 — 公共 JS 模块
   包含: Toast通知、API封装、JWT认证守卫、工具函数
   ============================================ */

// ==================== Toast 通知系统 ====================

(function () {
  function ensureToastBox() {
    var box = document.getElementById('dhToastBox');
    if (!box) {
      box = document.createElement('div');
      box.id = 'dhToastBox';
      document.body.appendChild(box);
    }
    return box;
  }

  function looksLikeHtml(text) {
    text = String(text || '').trim().toLowerCase();
    return text.indexOf('<!doctype html') === 0 ||
      text.indexOf('<html') === 0 ||
      (text.indexOf('<head') >= 0 && text.indexOf('<body') >= 0);
  }

  function cleanMessage(msg) {
    msg = String(msg || '');
    if (looksLikeHtml(msg)) {
      return '接口返回了网页HTML，通常是登录失效或Nginx代理异常，请刷新后台后重试。';
    }
    msg = msg
      .replace(/<!DOCTYPE[\s\S]*$/i, '接口返回异常页面，请刷新后台后重试。')
      .replace(/<[^>]+>/g, '')
      .replace(/\s{3,}/g, ' ')
      .trim();
    if (msg.length > 260) {
      msg = msg.substring(0, 260) + '……';
    }
    return msg || '操作完成';
  }

  window.dhToast = function (msg, type) {
    var box = ensureToastBox();
    var div = document.createElement('div');
    div.className = 'dh-toast toast-' + (type || 'success');
    div.innerText = cleanMessage(msg);
    box.appendChild(div);

    setTimeout(function () {
      div.style.opacity = '0';
      div.style.transform = 'translateY(-6px)';
    }, 3200);

    setTimeout(function () {
      if (div.parentNode) div.parentNode.removeChild(div);
    }, 3700);
  };

  window.dhAlert = function (msg, type) {
    if (!type) {
      var raw = String(msg || '');
      type = /失败|错误|error|blocked|异常|html/i.test(raw) ? 'error' : 'success';
    }
    window.dhToast(msg, type);
  };

  window.dhConfirm = function (msg) {
    return window.confirm(msg);
  };
})();


// ==================== JWT Token 管理 ====================

(function () {
  window.dhGetToken = function () {
    return localStorage.getItem('dh_token') || '';
  };

  window.dhSetToken = function (token) {
    if (token) {
      localStorage.setItem('dh_token', token);
      localStorage.setItem('login', 'ok');
    }
  };

  window.dhClearToken = function () {
    localStorage.removeItem('dh_token');
    localStorage.removeItem('login');
  };
})();


// ==================== API 封装（自动注入 JWT Token） ====================

var DH_API = (function () {

  function authHeaders(extraHeaders) {
    var headers = extraHeaders || {};
    var token = window.dhGetToken();
    if (token) {
      headers['Authorization'] = 'Bearer ' + token;
    }
    return headers;
  }

  function fetchText(url, options) {
    options = options || {};
    options.cache = 'no-store';
    if (!options.headers) options.headers = {};
    options.headers = authHeaders(options.headers);

    return fetch(url, options).then(function (r) {
      return r.text().then(function (t) {
        if (r.status === 401) {
          window.dhClearToken();
          window.dhAlert('登录已过期，请重新登录', 'error');
          setTimeout(function () { location.href = 'login.html'; }, 1500);
          throw new Error('未登录或登录已过期');
        }
        if (!r.ok) {
          throw new Error('接口状态异常：HTTP ' + r.status + '，' + t);
        }
        var lower = String(t || '').trim().toLowerCase();
        if (lower.indexOf('<!doctype html') === 0 ||
          lower.indexOf('<html') === 0 ||
          (lower.indexOf('<head') >= 0 && lower.indexOf('<body') >= 0)) {
          throw new Error('接口返回了网页（可能登录过期），请刷新页面重试。');
        }
        return t;
      });
    });
  }

  function fetchJson(url) {
    var headers = authHeaders({});
    return fetch(url + '?_=' + Date.now(), { cache: 'no-store', headers: headers })
      .then(function (r) {
        return r.text().then(function (t) {
          if (r.status === 401) {
            window.dhClearToken();
            window.dhAlert('登录已过期，请重新登录', 'error');
            setTimeout(function () { location.href = 'login.html'; }, 1500);
            throw new Error('未登录或登录已过期');
          }
          if (!r.ok) {
            throw new Error('接口异常：HTTP ' + r.status);
          }
          var lower = String(t || '').trim().toLowerCase();
          if (lower.indexOf('<!doctype html') === 0 ||
            lower.indexOf('<html') === 0 ||
            (lower.indexOf('<head') >= 0 && lower.indexOf('<body') >= 0)) {
            throw new Error('接口返回了网页（可能登录过期），请刷新页面重试。');
          }
          return JSON.parse(t);
        });
      });
  }

  return {
    postForm: function (url, body) {
      return fetchText(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: body
      });
    },
    postJson: function (url, data) {
      return fetchText(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
      });
    },
    putJson: function (url, data) {
      return fetchText(url, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
      });
    },
    del: function (url) {
      return fetchText(url, { method: 'DELETE' });
    },
    getJson: fetchJson,
    getText: fetchText,
    checkId: function (id) {
      if (!/^\d+$/.test(String(id))) {
        dhAlert('参数错误，请刷新页面重试', 'error');
        return false;
      }
      return true;
    }
  };
})();


// ==================== 认证守卫（后台页面） ====================

(function () {
  var pageName = (location.pathname.split('/').pop() || '').toLowerCase();
  if (pageName === 'login.html' || pageName === 'play.html' || pageName === 'live_play.html' || pageName === 'live.html') {
    return;
  }
  if (localStorage.getItem('login') !== 'ok') {
    location.href = 'login.html';
  }
})();


// ==================== 通用登出 ====================

window.dhLogout = function () {
  window.dhClearToken();
  location.href = 'login.html';
};


// ==================== DOM 安全工具 ====================

window.val = function (id) {
  var el = document.getElementById(id);
  return el ? el.value.trim() : '';
};

window.setVal = function (id, v) {
  var el = document.getElementById(id);
  if (el) el.value = v || '';
};


// ==================== HTML 转义 ====================

window.escapeHtml = function (s) {
  return (s || '').split('&').join('&').split('"').join('"').split('<').join('<').split('>').join('>');
};


// ==================== 加载状态 ====================

window.showLoading = function (text) {
  var box = document.getElementById('loadingBox');
  if (!box) return;
  box.style.display = 'block';
  var p = box.querySelector('p');
  if (p && text) p.innerText = text;
};

window.hideLoading = function () {
  var box = document.getElementById('loadingBox');
  if (box) box.style.display = 'none';
};
