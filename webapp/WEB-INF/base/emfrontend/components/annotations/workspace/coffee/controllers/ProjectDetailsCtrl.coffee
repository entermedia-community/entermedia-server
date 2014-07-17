Workspace.controller 'ProjectDetailsCtrl', 
['$scope', '$stateParams', 'annotationService',
($scope, $stateParams, annotationService) ->
    $scope.currentAnnotation = _.find annotationService.mockData,
    (item) ->
    	item.project.id is parseInt $stateParams.projectID
    em.unit
]