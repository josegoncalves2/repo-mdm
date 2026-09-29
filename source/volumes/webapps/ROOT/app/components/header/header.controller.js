// Localization completed
angular.module( 'headwind-kiosk' )
.controller( 'HeaderController', function( $scope, $rootScope, $state, $uibModal, $timeout, $interval, $filter, $window,
                                           authService, localization, hintService, rebranding, alertService, Idle,
                                           themeService, settingsService ) {
    $scope.isControlPanel = false;
    $scope.authService = authService;
    $scope.showExitReportMode = false;
    $scope.themeService = themeService;

    $scope.toggleTheme = function () {
        themeService.toggle();
    };

    var body = angular.element($window.document.body);
    try {
        if ($window.localStorage.getItem('hwmdm.nav.collapsed') === '1') body.addClass('hwmdm-nav-collapsed');
    } catch (e) {}
    // Abaixo de 1025px o menu ja e' um drawer; acima, recolhe a coluna inteira.
    $scope.toggleMenu = function () {
        if ($window.innerWidth <= 1024) {
            $rootScope.$broadcast('HWMDM_TOGGLE_NAV');
            return;
        }
        body.toggleClass('hwmdm-nav-collapsed');
        try {
            $window.localStorage.setItem('hwmdm.nav.collapsed', body.hasClass('hwmdm-nav-collapsed') ? '1' : '0');
        } catch (e) {}
    };

    var loadWebBranding = function () {
        settingsService.getSettings(function (response) {
            if (response.status === 'OK' && response.data) {
                themeService.applyBrandColors(response.data);
                // Logos stored before the path became relative still carry the host they were
                // uploaded from; serve any /files/ logo from the origin the console is on now.
                var logo = response.data.webLogoUrl;
                var filesIdx = logo ? logo.indexOf('/files/') : -1;
                $scope.webLogoUrl = filesIdx > -1 && !/h-mdm\.com\//i.test(logo) ? logo.substring(filesIdx) : logo;
            }
        });
    };

    if (authService.isLoggedIn()) {
        // Covers a full page (re)load while already authenticated.
        loadWebBranding();
    }
    // Covers a fresh login within the same page load: HeaderController is
    // instantiated once at app bootstrap (on the login screen, before
    // authService.isLoggedIn() is true), so it needs this event to pick up
    // branding right after the user actually logs in.
    $rootScope.$on('aero_USER_AUTHENTICATED', loadWebBranding);

    $scope.$on( 'START_REPORT_MODE', function() {
        $scope.showExitReportMode = true;
    } );

    $scope.$on( 'HIDE_REPORT_MODE', function() {
        $scope.showExitReportMode = false;
    } );

    $scope.$on( 'HIDE_ADDRESS', function() {
        $scope.mapToolsConfig.showDeviceAddress = false;
    } );

    $scope.$on( 'SHOW_CHECKLIST_INFO', function( event, checklistId ) {
        showWorkResultsContent( checklistId );
    } );

    $scope.$on( 'SHOW_DATA_LOADING_MODAL', function() {
        $scope.dataLoadingWait = true;
    } );

    $scope.$on( 'HIDE_DATA_LOADING_MODAL', function() {
        $scope.dataLoadingWait = false;
    } );

    $scope.$on('IdleWarn', function(e, countdown) {
        if (!$scope.logoutAlert) {
            $scope.logoutAlert = alertService.showAlertMessage(
                localization.localize('idle.logout.message').replace('${sec}', countdown),
                function() {
                    $scope.logoutAlert = null;
                },
                localization.localize('idle.logout.resume'));
        } else {
            // How to update the logoutAlert contents??
        }
    });

    $scope.$on('IdleTimeout', function() {
        $scope.logoutAlert.close();
        $scope.logoutAlert = null;
        $scope.logout();
    });

    $rootScope.$on( 'SHOW_EXPIRY_WARNING', function() {
        $scope.expiryWarning = true;
    } );

    rebranding.query(function(value) {
        $scope.appName = value.appName;
    });
    $scope.portalHost = $window.location.host;

    updateDateTime = function() {
        $scope.dateTime = $filter( 'date' )( new Date(), localization.localize('format.date.header') );
    };
    updateDateTime();

    var interval = $interval( updateDateTime, 10000 );
    $scope.$on('$destroy', function() { $interval.cancel( interval ) } );

    $scope.getUserName = function() { return authService.getUserName(); };
    $scope.isAuth = function() { return authService.isLoggedIn() && document.URL.indexOf( 'invoice' ) === -1; };
    $scope.isHidden = function() {
        return $state.current.name === 'qr' || $state.current.name === 'passwordReset';
    };

    $scope.isSuperAdmin = function() {
        return authService.isSuperAdmin();
    };
    // updatesAllowed() and the "Updates" menu entry were removed along with the Updates
    // screen: its template and controller are gone and the /updates route only redirects.

    $scope.logout = function() {
        authService.logout();
        hintService.onLogout();
        $state.transitionTo( 'login' );
        $rootScope.$emit('aero_USER_LOGOUT');
    };

    $scope.isActive = function( state ) {
        return $state.$current.self.name === state;
    };

    $scope.controlPanel = function() {
        $state.transitionTo( 'control-panel' );
        $scope.isControlPanel = true;
    };

    $scope.mainPanel = function() {
        $state.transitionTo( 'main' );
        $scope.isControlPanel = false;
    };

    $scope.about = function () {
        $uibModal.open({
            templateUrl: 'app/components/about/about.html?v=hf71e2c5983',
            controller: 'AboutController'
        });
    };
} );
