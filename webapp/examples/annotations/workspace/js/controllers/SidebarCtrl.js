Workspace.controller('SidebarCtrl', [
  '$scope', function($scope) {
    $scope.navigation = [
      {
        state: 'programs',
        name: 'Programs'
      }, {
        state: 'projects',
        name: 'Projects'
      }, {
        state: 'annotations',
        name: 'Annotations'
      }, {
        state: 'libraries',
        name: 'Libraries'
      }, {
        state: 'collections',
        name: 'Collections'
      }
    ];
    $scope.$on('navigatedTo', function(e, loc) {
      $scope.currentLocationName = loc;
      return em.unit;
    });
    return em.unit;
  }
]);
