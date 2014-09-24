var dummy = {
	object: {}
};
(function(){
	var app = angular.module('Site', []);
	app.controller("TestCtrl", function($scope){
		$scope.test = "AN ANGULAR STRING";
		return dummy.object;
	});
})();