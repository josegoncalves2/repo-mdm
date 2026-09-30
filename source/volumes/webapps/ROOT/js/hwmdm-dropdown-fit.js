// Menus "..." anexados ao body (linha de dispositivo): depois de abrir, cabem na tela.
// Sem espaco embaixo do botao, abrem para cima; a altura acompanha o espaco disponivel.
(function () {
    var MARGIN = 8;
    function fit(toggle) {
        var menus = document.querySelectorAll('.dropdown-menu-devices-row');
        var menu = null;
        for (var i = 0; i < menus.length; i++) {
            if (menus[i].offsetParent !== null) { menu = menus[i]; }
        }
        if (!menu) { return; }
        menu.style.maxHeight = '';
        var t = toggle.getBoundingClientRect();
        var below = window.innerHeight - t.bottom - MARGIN;
        // A barra superior do painel e' fixa: o menu nao pode subir por baixo dela.
        var header = document.querySelector('.navbar-fixed-top, .hwmdm-topbar, header.navbar, .navbar');
        var headerBottom = header ? header.getBoundingClientRect().bottom : 0;
        var above = t.top - Math.max(headerBottom, 0) - MARGIN;
        var h = menu.scrollHeight;
        var up = h > below && above > below;
        var room = up ? above : below;
        var height = Math.min(h, room);
        menu.style.maxHeight = height + 'px';
        menu.style.overflowY = h > room ? 'auto' : '';
        var top = up ? (t.top - height - 2) : (t.bottom + 2);
        menu.style.top = (top + window.pageYOffset) + 'px';
    }
    document.addEventListener('click', function (e) {
        var toggle = e.target.closest && e.target.closest('[id^="single-button-"]');
        if (toggle) { setTimeout(function () { fit(toggle); }, 0); }
    }, true);
})();
