
angular.module('os.biospecimen.cp.specimens', ['os.biospecimen.models'])
  .controller('CpSpecimensCtrl', function(
    $scope, $state, $stateParams, $timeout, $modal,
    cp, events, specimenRequirements, aliquotQtyReq,
    Specimen, SpecimenRequirement, PvManager, Alerts, Util) {

    if (!$stateParams.eventId && !!events && events.length > 0) {
      $state.go('cp-detail.specimen-requirements', {eventId: events[0].id});
      return;
    }

    function init() {
      $scope.cp = cp;
      $scope.events = events;
      $scope.eventId = $stateParams.eventId;
      $scope.selectEvent({id: $stateParams.eventId});
      $scope.selectedEvent = events.find(function(evt) { return evt.id == $stateParams.eventId; });

      $scope.specimenRequirements = Specimen.flatten(specimenRequirements);
      $scope.aliquotQtyReq = aliquotQtyReq;

      $scope.view = 'list_sr';
      $scope.sr = {};
      $scope.childReq = {};
      $scope.poolReq = {};
      $scope.errorCode = '';
    }

    function sortReqs(reqs) {
      reqs.sort(
        function(r1, r2) {
          if (r1.sortOrder != null && r2.sortOrder != null) {
            return r1.sortOrder - r2.sortOrder;
          } else if (r1.sortOrder != null) {
            return -1;
          } else if (r2.sortOrder != null) {
            return 1;
          } else {
            return r1.id - r2.id;
          }
        }
      );

      return reqs;
    }

    function addToSrList(sr) {
      specimenRequirements.push(sr);
      $scope.specimenRequirements = Specimen.flatten(sortReqs(specimenRequirements));
    }

    function updateSrList(sr) {
      var result = findSr(specimenRequirements, sr.id);
      angular.extend(result.sr, sr);
      $scope.specimenRequirements = Specimen.flatten(sortReqs(specimenRequirements));
    }

    function deleteFromSrList(sr) {
      var result = findSr(specimenRequirements, sr.id);
      result.list.splice(result.idx, 1);
      $scope.specimenRequirements = Specimen.flatten(sortReqs(specimenRequirements));
    }

    function findSr(srList, srId) {
      if (!srList) {
        return undefined;
      }

      for (var i = 0; i < srList.length; ++i) {
        if (srList[i].id == srId) {
          return {list: srList, sr: srList[i], idx: i};
        }
        var result = findSr(srList[i].children, srId);
        if (!!result) {
          return result;
        }

        result = findSr(srList[i].specimensPool, srId);
        if (!!result) {
          return result;
        }
      }

      return undefined;
    }

    function addChildren(parent, children) {
      if (!parent.children) {
        parent.children = [];
      }

      angular.forEach(children, function(child) {
        parent.children.push(child);
      });

      $scope.specimenRequirements = Specimen.flatten(specimenRequirements);
    };

    var pvsLoaded = false;
    function loadPvs() {
      if (pvsLoaded) {
        return;
      }

      $scope.storageTypes = PvManager.getPvs('storage-type');

      loadLabelAutoPrintModes();
      pvsLoaded = true;
    };
    
    function loadLabelAutoPrintModes() {
      $scope.spmnLabelAutoPrintModes = [];
      PvManager.loadPvs('specimen-label-auto-print-modes').then(
        function(pvs) {
          if ($scope.cp.spmnLabelPrePrintMode != 'NONE') {
            $scope.spmnLabelAutoPrintModes = pvs;
          } else {
            $scope.spmnLabelAutoPrintModes = pvs.filter(
      	      function(pv) {
      	        return pv.name != 'PRE_PRINT';
      	      }
      	    );
          }
        }
      );
    }

    function removeUiProps(sr) {
      delete sr.labelFmtSpecified;
      angular.forEach(sr.specimensPool,
        function(poolSpmn) {
          delete poolSpmn.labelFmtSpecified;
        }
      );

      return sr;
    }

    function cloneSr(sr) {
      var delAttrs = [
        'depth', 'hasChildren', 'children', 'isOpened', 'parent',
        'pooledSpecimen', 'specimensPool'
      ];

      var result = angular.copy(sr);
      angular.forEach(delAttrs, function(attr) { delete result[attr]; });
      return result;
    }

    $scope.openSpecimenNode = function(sr) {
      sr.isOpened = true;
    };

    $scope.closeSpecimenNode = function(sr) {
      sr.isOpened = false;
    };

    $scope.showAddSr = function() {
      $scope.view = 'addedit_sr';
      $scope.sr = new SpecimenRequirement({eventId: $scope.eventId});
      loadPvs();
    };

    $scope.onCreatePooledSpmnsClick = function(createPooledSpmn) {
      if (createPooledSpmn) {
        $scope.sr.specimensPool = [
          new SpecimenRequirement({eventId: $scope.eventId})
        ];
      } else {
        $scope.sr.specimensPool = undefined;
      }
    }

    $scope.addSpecimenPoolReq = function() {
      $scope.sr.specimensPool.push(new SpecimenRequirement({eventId: $scope.eventId}));
    }

    $scope.removeSpecimenPoolReq = function(poolSpmn) {
      var idx = $scope.sr.specimensPool.indexOf(poolSpmn);
      $scope.sr.specimensPool.splice(idx, 1);
      if ($scope.sr.specimensPool.length == 0) {
        $scope.addSpecimenPoolReq();
      }
    }

    $scope.showEditSr = function(sr) {
      $scope.specimensCount = 0;
      $scope.sr = cloneSr(sr);
      $scope.sr.$$storedInRepo = (sr.storageType != 'Virtual');

      if (sr.isAliquot()) {
        $scope.view = 'addedit_aliquot';
        $scope.parentSr = sr.parent;
        $scope.childReq = $scope.sr;
      } else if (sr.isDerivative()) {
        $scope.view = 'addedit_derived';
        $scope.parentSr = sr.parent;
        $scope.childReq = $scope.sr;
      } else if (!!sr.pooledSpecimen) {
        $scope.view = 'addedit_pool';
        $scope.parentSr = sr.pooledSpecimen;
        $scope.poolReq = $scope.sr;
      } else {
        $scope.view = 'addedit_sr';
      }

      $scope.sr.getSpecimensCount().then(
        function(count) {
          $scope.specimensCount = count;
        }
      );
      $scope.sr.initialQty = Util.getNumberInScientificNotation($scope.sr.initialQty);
      loadPvs();
    };

    $scope.viewSr = function(sr) {
      $scope.view = 'view_sr';
      $scope.parentSr = sr.parent;
      $scope.childReq = sr;
    };

    $scope.revertEdit = function() {
      $scope.view = 'list_sr';
      $scope.parentSr = null;
      $scope.childReq = {};
      $scope.poolReq= {};
      $scope.sr = {};
    };

    $scope.createSr = function() {
      $scope.sr.storageType = ($scope.sr.$$storedInRepo ? 'Manual' : 'Virtual');
      removeUiProps($scope.sr).$saveOrUpdate().then(
        function(result) {
          addToSrList(result);
          $scope.view = 'list_sr';
        }
      );
    };

    $scope.updateSr = function() {
      $scope.sr.storageType = ($scope.sr.$$storedInRepo ? 'Manual' : 'Virtual');
      removeUiProps($scope.sr).$saveOrUpdate().then(
        function(result) {
          updateSrList(result);
          $scope.revertEdit();
        }
      );
    };

    $scope.ensureLabelFmtSpecified = function(sr, lineage) {
      sr.labelFmtSpecified = true;

      if (sr.labelAutoPrintMode != 'PRE_PRINT' || !!sr.labelFmt) {
        return;
      }

      if (lineage == 'Aliquot') {
        sr.labelFmtSpecified = !!$scope.cp.aliquotLabelFmt;
      } else if (lineage == 'Derivative') {
        sr.labelFmtSpecified = !!$scope.cp.derivativeLabelFmt;
      } else {
        sr.labelFmtSpecified = !!$scope.cp.specimenLabelFmt;
      }
    }

    ////////////////////////////////////////////////
    //
    //  Aliquot logic
    //
    ////////////////////////////////////////////////
    $scope.showCreateAliquots = function(sr) {
      if (sr.availableQty() == 0) {
        Alerts.error('srs.errors.insufficient_qty');
        return;
      }

      $scope.childReq = { $$storedInRepo: true, storageType: 'Manual' };
      $scope.parentSr = sr;
      $scope.view = 'addedit_aliquot';
      loadPvs();
    };

    $scope.createAliquots = function() {
      var spec = $scope.childReq;
      var availableQty = $scope.parentSr.availableQty();

      if (!!spec.qtyPerAliquot && !!spec.noOfAliquots) {
        var requiredQty = spec.qtyPerAliquot * spec.noOfAliquots;
        if ((requiredQty - availableQty) > 0.000001) {
          Alerts.error("srs.errors.insufficient_qty");
          return;
        }
      } else if (!!spec.qtyPerAliquot) {
        spec.noOfAliquots = Math.floor(availableQty / spec.qtyPerAliquot);
      } else if (!!spec.noOfAliquots && availableQty != null && availableQty != undefined) {
        spec.qtyPerAliquot = Math.round(availableQty / spec.noOfAliquots * 10000) / 10000;
      }

      $scope.parentSr.createAliquots(removeUiProps(spec)).then(
        function(aliquots) {
          addChildren($scope.parentSr, aliquots);
          $scope.parentSr.isOpened = true;

          $scope.childReq = {};
          $scope.parentSr = undefined;
          $scope.view = 'list_sr';
        }
      );
    };

    ////////////////////////////////////////////////
    //
    //  Derivative logic
    //
    ////////////////////////////////////////////////
    $scope.showCreateDerived = function(sr) {
      $scope.parentSr = sr;
      $scope.view = 'addedit_derived';
      $scope.childReq = {
        pathology: sr.pathology,
        anatomicSite: sr.anatomicSite,
        laterality: sr.laterality,
        storageType: 'Virtual'
      };
      loadPvs();
    };

    $scope.createDerivative = function() {
      $scope.parentSr.createDerived(removeUiProps($scope.childReq)).then(
        function(derived) {
          addChildren($scope.parentSr, [derived]);
          $scope.parentSr.isOpened = true;

          $scope.childReq = {};
          $scope.parentSr = undefined;
          $scope.view = 'list_sr';
        }
      );
    };

    $scope.showCreatePoolSpecimen = function(sr) {
      $scope.parentSr = sr;
      $scope.view = 'addedit_pool';
      $scope.poolReq = new SpecimenRequirement({eventId: $scope.eventId});
    };

    $scope.showEditPooledSpmn = function(sr) {
      $scope.parentSr = sr.parent;
      $scope.view = 'addedit_pool';
      $scope.pooledReq.spmn = sr;
    };

    $scope.addToSpmnPool = function() {
      $scope.parentSr.addPoolSpecimens([removeUiProps($scope.poolReq)]).then(
        function(poolSpmns) {
          $scope.parentSr.specimensPool = $scope.parentSr.specimensPool.concat(poolSpmns);
          $scope.parentSr.isOpened = true;

          $scope.poolReq = {};
          $scope.parentSr = undefined;
          $scope.view = 'list_sr';
          $scope.specimenRequirements = Specimen.flatten(specimenRequirements);
        }
      );
    };
        
    $scope.copySr = function(sr) {
      var aliquotReq = {noOfAliquots: 1, qtyPerAliquot: sr.initialQty};
      if (sr.isAliquot() && !sr.parent.hasSufficientQty(aliquotReq)) {
        Alerts.error('srs.errors.insufficient_qty');
        return;
      }
      
      sr.copy().then(
        function(result) {
          if (sr.pooledSpecimen) {
            sr.pooledSpecimen.specimensPool.push(result);
            $scope.specimenRequirements = Specimen.flatten(specimenRequirements);
          } else if (sr.parent) {
            addChildren(sr.parent, [result]);
          } else {
            addToSrList(result);
          }
        }
      );
    };

    $scope.deleteSr = function(sr) {
      Util.showConfirm({
        templateUrl: 'modules/biospecimen/cp/delete_sr.html',
        ok: function() {
          sr.delete().then(
            function() {
              deleteFromSrList(sr);
            }
          );
        }
      });
    }

    $scope.closeSr = function(sr) {
      Util.showConfirm({
        templateUrl: 'modules/biospecimen/cp/close_sr.html',
        ok: function() {
          var toClose = cloneSr(sr);
          toClose.activityStatus = 'Closed';

          removeUiProps(toClose).$saveOrUpdate().then(
            function(result) {
              updateSrList(result);
            }
          );
        }
      });
    }

    init();
  });
