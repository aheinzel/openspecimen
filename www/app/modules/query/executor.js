
angular.module('os.query.executor', [])
  .factory('QueryExecutor', function($http, $document, Util, ApiUrls) {
    var queryUrl = ApiUrls.getBaseUrl() + 'query';

    return {
      getCount: function(queryId, cp, aql) {
        if (typeof cp == 'number') {
          cp = {id: cp};
        }

        var req = {
          savedQueryId: queryId, 
          cpId: !cp.$$cpGroup ? cp.id : undefined,
          cpGroupId: cp.$$cpGroup ? cp.id : undefined,
          drivingForm: 'Participant',
          runType: 'Count',
          aql: aql
        };

        return $http.post(queryUrl, req).then(
          function(resp) {
            var data = resp.data;

            var result = {cprCnt: 0, specimenCnt: 0};
            result.cprCnt  = data.rows[0][0];
            result.visitCnt = data.rows[0][1];
            for (var i = 2; i < data.rows[0].length; ++i) {
              result.specimenCnt += parseInt(data.rows[0][i]);
            }
            return result;
          }
        );
      },

      getRecords: function(queryId, cp, aql, wideRowMode, outputIsoFmt, opts) {
        if (typeof cp == 'number') {
          cp = {id: cp};
        }

        opts = opts || {};

        var req = {
          savedQueryId: queryId, 
          cpId: !cp.$$cpGroup ? cp.id : undefined,
          cpGroupId: cp.$$cpGroup ? cp.id : undefined,
          drivingForm: 'Participant',
          runType: 'Data', 
          aql: aql, 
          wideRowMode: wideRowMode || "OFF",
          outputIsoDateTime: (outputIsoFmt || false),
          outputColumnExprs: opts.outputColumnExprs || false
        };
        return $http.post(queryUrl, req).then(
          function(resp) {
            return resp.data;
          }
        );
      },

      exportQueryResultsData: function(queryId, cp, aql, wideRowMode, opts) {
        if (typeof cp == 'number') {
          cp = {id: cp};
        }

        opts = opts || {};

        var req = {
          savedQueryId: queryId,
          cpId: !cp.$$cpGroup ? cp.id : undefined,
          cpGroupId: cp.$$cpGroup ? cp.id : undefined,
          drivingForm: 'Participant',
          runType: 'Export',
          aql: aql,
          indexOf: 'Specimen.label',
          wideRowMode: wideRowMode || "OFF",
          outputColumnExprs: opts.outputColumnExprs || false
        };

        return $http.post(queryUrl + '/export', req).then(
          function(resp) {
            return resp.data;
          }
        );
      },

      downloadDataFile: function(fileId, filename) {
        filename = !!filename ? filename : 'QueryResults.csv';
        Util.downloadFile(queryUrl + '/export?fileId=' + fileId + '&filename=' + filename);
      },

      getFacetValues: function(cp, facetExprs, searchTerm, restriction) {
        if (typeof cp == 'number') {
          cp = {id: cp};
        }

        var op = {
          cpId: !cp.$$cpGroup ? cp.id : undefined,
          cpGroupId: cp.$$cpGroup ? cp.id : undefined,
          facets: facetExprs,
          searchTerm: searchTerm,
          restriction: restriction
        };
        return $http.post(queryUrl + '/facet-values', op).then(
          function(result) {
            return result.data;
          }
        );
      }
    };
  });
