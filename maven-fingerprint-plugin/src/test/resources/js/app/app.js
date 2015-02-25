// Define needs to be on the 1st line for requirejs optimizer to work correctly
define([
        'angularAMD',
        'ui-bootstrap-tpls',
        'angular-route',
        'angular-cookies',
        'angular-animate',
        'ng-tags-input',
        'app-templates',
        'services/StorageService',
        'services/HttpService',
        'services/CollectionService',
        'services/DatePickerInitService',
        'services/DataSourceService',
        'services/TenantService',
        'services/SessionTimeoutService',
        'collect/CollectionController',
        'collect/StartDateController',
        'collect/EndDateController',
        'collect/QueryController',
        'collect/DataSourceController',
        'collect/DataSourceModalInstanceController',
        'collect/EngagementSettingsController',
        'common/ModalInstanceController',
        'common/TimeoutModalInstanceController',
        'login/LoginController',
        'MainController',
        'HeaderController',
        'LandingController',
        'TenantController',
        'TenantMainController',
        'LogoutController',
        'directives/DateValidationDirective',
        'directives/OutFocusClickDirective'
        ], function (angularAMD, uiBootstrap, angularRoute, angularCookies, angularAnimate, ngTagsInput, StorageService, HttpService, CollectionService, DatePickerInitService
		,DataSourceService, TenantService, SessionTimeoutService, CollectionController, StartDateController, EndDateController, QueryController, DataSourceController, DataSourceModalInstanceController, EngagementSettingsController 
		,ModalInstanceController, TimeoutModalInstanceController, LoginController, MainController, HeaderController, LandingController, TenantController, TenantMainController, LogoutController 
		,DateValidationDirective,OutFocusClickDirective) {

	var app = angular.module('empath-v2', ['ui.bootstrap','ngRoute','ngCookies','ngAnimate','ngTagsInput']);

	app.constant('contextPath', document.contextPath);
	app.constant('sessionTimeout', document.sessionTimeout);

	app.service('StorageService', StorageService)
	.service('HttpService', HttpService)
	.service('CollectionService', CollectionService)
	.service('DataSourceService', DataSourceService)
	.service('DatePickerInitService', DatePickerInitService)
	.service('SessionTimeoutService', SessionTimeoutService)
	.service('TenantService', TenantService);

	app.controller('collect/CollectionController', CollectionController)
	.controller('collect/StartDateController', StartDateController)
	.controller('collect/EndDateController', EndDateController)
	.controller('collect/QueryController', QueryController)
	.controller('collect/DataSourceController', DataSourceController)
	.controller('collect/DataSourceModalInstanceController', DataSourceModalInstanceController)
	.controller('collect/EngagementSettingsController', EngagementSettingsController)
	.controller('common/ModalInstanceController', ModalInstanceController)
	.controller('common/TimeoutModalInstanceController', TimeoutModalInstanceController)
	.controller('login/LoginController', LoginController)
	.controller('MainController', MainController)
	.controller('HeaderController', HeaderController)
	.controller('LandingController', LandingController)
	.controller('TenantController', TenantController)
	.controller('TenantMainController', TenantMainController)
	.controller('LogoutController', LogoutController),

	app.config(['$routeProvider', '$locationProvider', 'contextPath', function($routeProvider, $locationProvider, contextPath) {
		$routeProvider
		.when(contextPath + '/app/collections', angularAMD.route({
			templateUrl : contextPath + '/templates/collection.html'
		}))
		.when(contextPath + '/app/collections/:collectionId', angularAMD.route({
			templateUrl : contextPath + '/templates/collection.html'
		}))
		.when(contextPath + '/api/v1/auth/login', angularAMD.route({
			templateUrl : contextPath + '/templates/login.html'
		}))
		.when(contextPath + '/api/v1/auth/logout', angularAMD.route({
			templateUrl : contextPath + '/templates/login.html'
		}))
		.when(contextPath + '/app/', angularAMD.route({
			templateUrl : contextPath + '/templates/landing_page.html'
		}))
		.when(contextPath + '/api/v1/twitter/addUser', {
			redirectTo : function(params, path) {
				return path;
			}
		})
		.when(contextPath + '/tenant/', angularAMD.route({
			templateUrl : contextPath + '/templates/tenant_administration/tenant_landing_page.html',
			reloadOnSearch: true
		}))
		.when(contextPath + '/tenant/new', angularAMD.route({
			templateUrl : contextPath + '/templates/tenant_administration/tenant_details.html',
			reloadOnSearch: true
		}))
		.when(contextPath + '/tenant/:tenantId', angularAMD.route({
			templateUrl : contextPath + '/templates/tenant_administration/tenant_details.html',
			reloadOnSearch: true
		}))
		.otherwise(angularAMD.route({
			templateUrl : contextPath + '/templates/landing_page.html',
		}));

		$locationProvider.html5Mode(true);
	}]);


	/**
	 * Validation Directives
	 */

	app.directive('dateValidation', DateValidationDirective)
	.directive('outFocusClick', OutFocusClickDirective);
	
    return angularAMD.bootstrap(app);
});