Workspace.controller 'ProjectCtrl', 
['$scope', '$stateParams', 'annotationService',
($scope, $stateParams, annotationService) ->
    $scope.currentProject = _.find annotationService.mockData, 
    (item) ->
    	item.project.id == $stateParams.projectID;
    
    em.unit;
]