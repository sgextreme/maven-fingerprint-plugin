require.config({
    paths: {
        'angular': '../lib/angular',
        'angularAMD': '../lib/angularAMD',
        'jquery' : '../lib/jquery',
    },
    shim: {
        'angularAMD': ['angular'],
    	'directives/DateValidationDirective': ['jquery'],
    	'directives/OutFocusClickDirective': ['jquery']
    },
    waitSeconds: 120,
    deps: ['app'],
    baseUrl: '/app'
});