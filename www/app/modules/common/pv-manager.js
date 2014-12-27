
/**
 * TODO: The PvManager will actually do following
 * 1. make REST API calls to get PVs for the input attribute
 * 2. Cache the PVs so that frequent calls are not needed
 */
angular.module('openspecimen')
  .factory('PvManager', function($http, $q, ApiUrls, ApiUtil, SiteService) {
    var url = ApiUrls.getBaseUrl() + 'permissible-values/attribute=';

    var genders = [
      'Female Gender', 
      'Male Gender', 
      'Unknown', 
      'Unspecified'
    ];

    var ethnicity = [
      'Hispanic or Latino', 
      'Not Hispanic or Latino', 
      'Not Reported', 
      'Unknown'
    ];

    var vitalStatuses = [
      'Alive', 
      'Dead', 
      'Unknown', 
      'Unspecified'
    ];

    var races = [
      'White', 
      'Black or African American', 
      'American Indian or Alaska Native',
      'Asian', 
      'Native Hawaiian or Other Pacific Islander', 
      'Not Reported',
      'Unknown'
    ];

    var anatomicSites = [
      'DIGESTIVE ORGANS',
      'SKIN',
      'MALE GENITAL ORGANS',
      'UNKNOWN PRIMARY SITE',
      'PERIPHERAL NERVES AND AUTONOMIC NERVOUS SYSTEM',
      'FEMALE GENITAL ORGANS',                       
      'OTHER AND ILL-DEFINED SITES',
      'HEMATOPOIETIC AND RETICULOENDOTHELIAL SYSTEMS',
      'RETROPERITONEUM AND PERITONEUM',
      'RESPIRATORY SYSTEM AND INTRATHORACIC ORGANS',
      'BONES, JOINTS AND ARTICULAR CARTILAGE',
      'THYROID AND OTHER ENDOCRINE GLANDS',
      'MENINGES',
      'CONNECTIVE, SUBCUTANEOUS AND OTHER SOFT TISSUES',
      'BREAST',
      'LIP, ORAL CAVITY AND PHARYNX',
      'LYMPH NODES',
      'URINARY TRACT',
      'BRAIN',
      'SPINAL CORD, CRANIAL NERVES, AND OTHER PARTS OF CENTRAL NERVOUS SYSTEM',
      'EYE, BRAIN AND OTHER PARTS OF CENTRAL NERVOUS SYSTEM',
      'Not Specified'
    ];

    var pvMap = {
      gender: genders, 
      ethnicity: ethnicity, 
      vitalStatus: vitalStatuses, 
      race: races,
      anatomicSite: anatomicSites
    };

    var pvIdMap = {
      'clinical-status': '2003988'
    };

    return {
      _getPvs: function(attr) {
        var deferred = $q.defer();
        var result = undefined;
        if (pvMap[attr]) {
          result = {status: 'ok', data: pvMap[attr]};
        } else {
          result = {status: 'error'};
        }

        deferred.resolve(result);
        return deferred.promise;
      },

      getPvs: function(attr, srchTerm) {
        var pvId = pvIdMap[attr];
        if (!pvId) {
          return [];
        }

        var pvs = [];
        $http.get(url + pvId, {params: {searchString: srchTerm}}).then(
          function(result) {
            angular.forEach(result.data, function(pv) {
              pvs.push(pv.value);
            });
          }
        );

        return pvs;
      },

      /** loadPvs will be deprecated soon **/
      loadPvs: function(scope, attr) {
        this._getPvs(attr).then(
          function(result) {
            if (result.status != 'ok') {
              alert("Failed to load PVs of attribute: " + attr);
              return;
            }

            scope[attr + '_pvs'] = result.data;
          }
        );
      },

      loadSites: function(scope, attr) {
        return SiteService.getSites().then(
          function(result) {
            if (result.status != "ok") {
              alert("Failed to load sites information");
            }

            if (attr) {
              scope[attr] = result.data;
            }
            return result.data;
          }
        );
      },

      getClinicalDiagnoses: function(params, cb) {
        var url = ApiUrls.getBaseUrl() + '/clinical-diagnoses';
        var diagnoses = [];
        $http.get(url, {params: params}).then(
          function(result) {
            angular.forEach(result.data, function(diagnosis) {
              diagnoses.push(diagnosis);
            });

            if (typeof cb == 'function') {
              cb(result.data);
            }
          }
        );

        return diagnoses;
      }
    };
  });
