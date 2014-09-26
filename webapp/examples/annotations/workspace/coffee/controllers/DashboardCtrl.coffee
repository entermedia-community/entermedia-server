Workspace.controller 'DashboardCtrl', 
['$scope', ($scope) ->
    $scope.testChangeButton = (text) ->
        if not text
            text = 'now test is this!'
        $scope.test = text
        em.unit
    em.unit;
]