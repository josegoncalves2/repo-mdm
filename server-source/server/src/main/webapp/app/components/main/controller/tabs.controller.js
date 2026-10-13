angular.module('headwind-kiosk')
    .controller('TabController', function ($scope, $rootScope, openTab) {
        // activeTab, openTab, plugins, modules, maintenance — all inherited from ShellController.
        // This controller only exists because ui-router child states need a controller reference.
        // The openTab resolve confirms the correct tab is active after direct-URL entry.
        if ($scope.activeTab !== openTab) {
            $scope.activeTab = openTab;
        }
    });
