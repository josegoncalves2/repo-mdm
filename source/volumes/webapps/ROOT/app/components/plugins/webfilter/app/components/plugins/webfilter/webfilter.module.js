(function () {
    'use strict';
    angular.module('hwdm.webfilter', ['ui.router', 'hwdm'])
        .config(function ($stateProvider) {
            $stateProvider.state('webfilter', {
                url: '/plugins/webfilter',
                templateUrl: 'app/components/plugins/webfilter/views/policy.html',
                controller: 'WebFilterController',
                controllerAs: 'ctrl',
                data: { pageTitle: 'Web Filter' }
            });
        });
})();