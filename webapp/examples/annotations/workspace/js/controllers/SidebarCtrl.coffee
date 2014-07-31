Workspace.controller 'SidebarCtrl',
['$scope',
($scope) ->

    $scope.navigation = [
        { state: 'programs', name: 'Programs' }
        { state: 'projects', name: 'Projects' }
        { state: 'annotations', name: 'Annotations' }
        { state: 'libraries', name: 'Libraries' }
        { state: 'collections', name: 'Collections' }
    ]

    $scope.$on 'navigatedTo', (e, loc) ->
        $scope.currentLocationName = loc
        em.unit

    em.unit
]
