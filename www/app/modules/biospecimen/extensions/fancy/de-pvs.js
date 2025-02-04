var openspecimen = openspecimen || {}
openspecimen.ui = openspecimen.ui || {};
openspecimen.ui.fancy = openspecimen.ui.fancy || {};

openspecimen.ui.fancy.Pvs = edu.common.de.LookupSvc.extend({
  getApiUrl: function() {
    var apiUrls = angular.element(document).injector().get('ApiUrls');
    return apiUrls.getBaseUrl() + 'permissible-values/v/';
  },

  searchRequest: function(searchTerm, searchFilters, field) {
    var filters = {searchString: searchTerm};
    if (field.attribute) {
      filters.attribute = field.attribute;
    }

    filters.includeOnlyLeafValue = field.leafValue;
    return $.extend(filters, searchFilters);
  },

  formatResults: function(pvs) {
    var result = [];
    for (var i = 0; i < pvs.length; ++i) {
      result.push({id: pvs[i].id, text: pvs[i].value});
    }

    return result;
  },

  formatResult: function(data) {
    return !data ? "" : {id: data.id, text: data.value};
  },

  getDefaultValue: function() {
    var deferred = $.Deferred();
    deferred.resolve(undefined);
    return deferred.promise();
  },

  getHeaders: function() {
    var $http = angular.element(document).injector().get('$http');
    return {'X-OS-API-TOKEN': $http.defaults.headers.common['X-OS-API-TOKEN']};
  }
});

openspecimen.ui.fancy.PvField = edu.common.de.LookupField.extend({
  svc: new openspecimen.ui.fancy.Pvs()
});

edu.common.de.FieldManager.getInstance()
  .register({
    name: "pvField", 
    displayName: "PV Dropdown",
    fieldCtor: openspecimen.ui.fancy.PvField
  }); 
