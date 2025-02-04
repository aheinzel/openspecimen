
package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.events.SiteSummary;
import com.krishagni.catissueplus.core.audit.services.impl.DeleteLogUtil;
import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.WorkflowUtil;
import com.krishagni.catissueplus.core.biospecimen.domain.AliquotSpecimensRequirement;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolSite;
import com.krishagni.catissueplus.core.biospecimen.domain.ConsentStatement;
import com.krishagni.catissueplus.core.biospecimen.domain.CpConsentTier;
import com.krishagni.catissueplus.core.biospecimen.domain.CpReportSettings;
import com.krishagni.catissueplus.core.biospecimen.domain.CpWorkflowConfig;
import com.krishagni.catissueplus.core.biospecimen.domain.CpWorkflowConfig.Workflow;
import com.krishagni.catissueplus.core.biospecimen.domain.DerivedSpecimenRequirement;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenRequirement;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CollectionProtocolFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ConsentStatementErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpReportSettingsFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpeErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpeFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CprErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenRequirementFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolEventDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolSummary;
import com.krishagni.catissueplus.core.biospecimen.events.ConsentTierDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ConsentTierOp;
import com.krishagni.catissueplus.core.biospecimen.events.ConsentTierOp.OP;
import com.krishagni.catissueplus.core.biospecimen.events.CopyCpOpDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CopyCpeOpDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CpQueryCriteria;
import com.krishagni.catissueplus.core.biospecimen.events.CpReportSettingsDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CpWorkflowCfgDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CprSummary;
import com.krishagni.catissueplus.core.biospecimen.events.FileDetail;
import com.krishagni.catissueplus.core.biospecimen.events.MergeCpDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenPoolRequirements;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenRequirementDetail;
import com.krishagni.catissueplus.core.biospecimen.events.WorkflowDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.CollectionProtocolDao;
import com.krishagni.catissueplus.core.biospecimen.repository.CpListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.CprListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.impl.BiospecimenDaoHelper;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.Tuple;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr.ParticipantReadAccess;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.domain.Notification;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityResp;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.ObjectAccessor;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.NotifUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.query.Column;
import com.krishagni.catissueplus.core.query.ListConfig;
import com.krishagni.catissueplus.core.query.ListDetail;
import com.krishagni.catissueplus.core.query.ListService;
import com.krishagni.rbac.common.errors.RbacErrorCode;
import com.krishagni.rbac.events.SubjectRoleOpNotif;
import com.krishagni.rbac.service.RbacService;

public class CollectionProtocolServiceImpl implements CollectionProtocolService, ObjectAccessor, InitializingBean {

	private ThreadPoolTaskExecutor taskExecutor;

	private CollectionProtocolFactory cpFactory;
	
	private CpeFactory cpeFactory;
	
	private SpecimenRequirementFactory srFactory;

	private DaoFactory daoFactory;
	
	private RbacService rbacSvc;

	private ListService listSvc;

	private CpReportSettingsFactory rptSettingsFactory;

	public void setTaskExecutor(ThreadPoolTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public void setCpFactory(CollectionProtocolFactory cpFactory) {
		this.cpFactory = cpFactory;
	}

	public void setCpeFactory(CpeFactory cpeFactory) {
		this.cpeFactory = cpeFactory;
	}
	
	public void setSrFactory(SpecimenRequirementFactory srFactory) {
		this.srFactory = srFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setRbacSvc(RbacService rbacSvc) {
		this.rbacSvc = rbacSvc;
	}

	public void setListSvc(ListService listSvc) {
		this.listSvc = listSvc;
	}

	public void setRptSettingsFactory(CpReportSettingsFactory rptSettingsFactory) {
		this.rptSettingsFactory = rptSettingsFactory;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<CollectionProtocolSummary>> getProtocols(RequestEvent<CpListCriteria> req) {
		try {
			CpListCriteria crit = addCpListCriteria(req.getPayload());
			if (crit == null) {
				return ResponseEvent.response(Collections.emptyList());
			}

			return ResponseEvent.response(daoFactory.getCollectionProtocolDao().getCollectionProtocols(crit));
		} catch (OpenSpecimenException oce) {
			return ResponseEvent.error(oce);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<Long> getProtocolsCount(RequestEvent<CpListCriteria> req) {
		try {
			CpListCriteria crit = addCpListCriteria(req.getPayload());
			if (crit == null) {
				return ResponseEvent.response(0L);
			}
			
			return ResponseEvent.response(daoFactory.getCollectionProtocolDao().getCpCount(crit));
		} catch (OpenSpecimenException oce) {
			return ResponseEvent.error(oce);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolDetail> getCollectionProtocol(RequestEvent<CpQueryCriteria> req) {
		try {
			CpQueryCriteria crit = req.getPayload();
			CollectionProtocol cp = getCollectionProtocol(crit.getId(), crit.getTitle(), crit.getShortTitle());
			AccessCtrlMgr.getInstance().ensureReadCpRights(cp);

			return ResponseEvent.response(CollectionProtocolDetail.from(cp, crit.isFullObject()));
		} catch (OpenSpecimenException oce) {
			return ResponseEvent.error(oce);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<SiteSummary>> getSites(RequestEvent<CpQueryCriteria> req) {
		try {
			CpQueryCriteria crit = req.getPayload();
			CollectionProtocol cp = getCollectionProtocol(crit.getId(), crit.getTitle(), crit.getShortTitle());
			AccessCtrlMgr.getInstance().ensureReadCpRights(cp);

			List<Site> sites = cp.getSites().stream().map(CollectionProtocolSite::getSite).collect(Collectors.toList());
			return ResponseEvent.response(SiteSummary.from(sites));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<CprSummary>> getRegisteredParticipants(RequestEvent<CprListCriteria> req) {
		try { 
			CprListCriteria listCrit = addCprListCriteria(req.getPayload());
			if (listCrit == null) {
				return ResponseEvent.response(Collections.emptyList());
			}

			return ResponseEvent.response(daoFactory.getCprDao().getCprList(listCrit));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> getRegisteredParticipantsCount(RequestEvent<CprListCriteria> req) {
		try {
			CprListCriteria listCrit = addCprListCriteria(req.getPayload());
			if (listCrit == null) {
				return ResponseEvent.response(0L);
			}

			return ResponseEvent.response(daoFactory.getCprDao().getCprCount(listCrit));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolDetail> createCollectionProtocol(RequestEvent<CollectionProtocolDetail> req) {
		try {
			CollectionProtocol cp = createCollectionProtocol(req.getPayload(), null, false);
			notifyUsersOnCpCreate(cp);
			return ResponseEvent.response(CollectionProtocolDetail.from(cp));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolDetail> updateCollectionProtocol(RequestEvent<CollectionProtocolDetail> req) {
		try {
			CollectionProtocolDetail detail = req.getPayload();
			CollectionProtocol existingCp = daoFactory.getCollectionProtocolDao().getById(detail.getId());
			if (existingCp == null) {
				return ResponseEvent.userError(CpErrorCode.NOT_FOUND);
			}

			AccessCtrlMgr.getInstance().ensureUpdateCpRights(existingCp);
			CollectionProtocol cp = cpFactory.createCollectionProtocol(detail);
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(cp);
			ensureUsersBelongtoCpSites(cp);
			
			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			ensureUniqueTitle(existingCp, cp, ose);
			ensureUniqueShortTitle(existingCp, cp, ose);
			ensureUniqueCode(existingCp, cp, ose);
			ensureUniqueCpSiteCode(cp, ose);
			if (existingCp.isConsentsWaived() != cp.isConsentsWaived()) {
			  ensureConsentTierIsEmpty(existingCp, ose);
			}

			ose.checkAndThrow();
			
			User oldPi = existingCp.getPrincipalInvestigator();
			Collection<User> addedCoord = Utility.subtract(cp.getCoordinators(), existingCp.getCoordinators());
			Collection<User> removedCoord = Utility.subtract(existingCp.getCoordinators(), cp.getCoordinators());

			Set<Site> addedSites = Utility.subtract(cp.getRepositories(), existingCp.getRepositories());
			Set<Site> removedSites = Utility.subtract(existingCp.getRepositories(), cp.getRepositories());
			ensureSitesAreNotInUse(existingCp, removedSites);

			existingCp.update(cp);
			existingCp.addOrUpdateExtension();
			
			fixSopDocumentName(existingCp);

			// PI and coordinators role handling
			addOrRemovePiCoordinatorRoles(cp, "UPDATE", oldPi, cp.getPrincipalInvestigator(), addedCoord, removedCoord);
			notifyUsersOnCpUpdate(existingCp, addedSites, removedSites);
			return ResponseEvent.response(CollectionProtocolDetail.from(existingCp));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolDetail> copyCollectionProtocol(RequestEvent<CopyCpOpDetail> req) {
		try {
			CopyCpOpDetail opDetail = req.getPayload();
			Long cpId = opDetail.getCpId();
			CollectionProtocol existing = daoFactory.getCollectionProtocolDao().getById(cpId);
			if (existing == null) {
				throw OpenSpecimenException.userError(CpErrorCode.NOT_FOUND);
			}
			
			AccessCtrlMgr.getInstance().ensureReadCpRights(existing);
			
			CollectionProtocol cp = createCollectionProtocol(opDetail.getCp(), existing, true);
			notifyUsersOnCpCreate(cp);
			return ResponseEvent.response(CollectionProtocolDetail.from(cp));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<MergeCpDetail> mergeCollectionProtocols(RequestEvent<MergeCpDetail> req) {
		AccessCtrlMgr.getInstance().ensureUserIsAdmin();

		CollectionProtocol srcCp = getCollectionProtocol(req.getPayload().getSrcCpShortTitle());
		CollectionProtocol tgtCp = getCollectionProtocol(req.getPayload().getTgtCpShortTitle());

		ensureMergeableCps(srcCp, tgtCp);

		int maxRecords = 30;
		boolean moreRecords = true;
		while (moreRecords) {
			List<CollectionProtocolRegistration> cprs = daoFactory.getCprDao().getCprsByCpId(srcCp.getId(), 0, maxRecords);
			for (CollectionProtocolRegistration srcCpr: cprs) {
				mergeCprIntoCp(srcCpr, tgtCp);
			}

			if (cprs.size() < maxRecords) {
				moreRecords = false;
			}
		}

		return ResponseEvent.response(req.getPayload());
	}

	@PlusTransactional
	public ResponseEvent<CollectionProtocolDetail> updateConsentsWaived(RequestEvent<CollectionProtocolDetail> req) {
		try {
			CollectionProtocolDetail detail = req.getPayload();
			CollectionProtocol existingCp = daoFactory.getCollectionProtocolDao().getById(detail.getId());
			if (existingCp == null) {
				return ResponseEvent.userError(CpErrorCode.NOT_FOUND);
			}
			
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(existingCp);
			
			if (CollectionUtils.isNotEmpty(existingCp.getConsentTier())) {
				return ResponseEvent.userError(CpErrorCode.CONSENT_TIER_FOUND, existingCp.getShortTitle());
			}

			existingCp.setConsentsWaived(detail.getConsentsWaived());
			return ResponseEvent.response(CollectionProtocolDetail.from(existingCp));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@PlusTransactional
	public ResponseEvent<List<DependentEntityDetail>> getCpDependentEntities(RequestEvent<Long> req) {
		try {
			CollectionProtocol existingCp = daoFactory.getCollectionProtocolDao().getById(req.getPayload());
			if (existingCp == null) {
				return ResponseEvent.userError(CpErrorCode.NOT_FOUND);
			}
			
			return ResponseEvent.response(existingCp.getDependentEntities());
 		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<BulkDeleteEntityResp<CollectionProtocolDetail>> deleteCollectionProtocols(RequestEvent<BulkDeleteEntityOp> req) {
		try {
			BulkDeleteEntityOp crit = req.getPayload();

			Set<Long> cpIds = crit.getIds();
			List<CollectionProtocol> cps = daoFactory.getCollectionProtocolDao().getByIds(cpIds);
			if (crit.getIds().size() != cps.size()) {
				cps.forEach(cp -> cpIds.remove(cp.getId()));
				throw OpenSpecimenException.userError(CpErrorCode.DOES_NOT_EXIST, cpIds);
			}

			for (CollectionProtocol cp : cps) {
				AccessCtrlMgr.getInstance().ensureDeleteCpRights(cp);
			}

			boolean completed = crit.isForceDelete() ? forceDeleteCps(cps, crit.getReason()) : deleteCps(cps, crit.getReason());
			BulkDeleteEntityResp<CollectionProtocolDetail> resp = new BulkDeleteEntityResp<>();
			resp.setCompleted(completed);
			resp.setEntities(CollectionProtocolDetail.from(cps));
			return ResponseEvent.response(resp);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<File> getSopDocument(RequestEvent<Long> req) {
		try {
			Long cpId = req.getPayload();
			CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getById(cpId);
			if (cp == null) {
				return ResponseEvent.userError(CprErrorCode.NOT_FOUND);
			}

			AccessCtrlMgr.getInstance().ensureReadCpRights(cp);

			String filename = cp.getSopDocumentName();
			File file = null;
			if (StringUtils.isBlank(filename)) {
				file = ConfigUtil.getInstance().getFileSetting(ConfigParams.MODULE, ConfigParams.CP_SOP_DOC, null);
			} else {
				file = new File(getSopDocDir() + filename);
				if (!file.exists()) {
					filename = filename.split("_", 2)[1];
					return ResponseEvent.userError(CpErrorCode.SOP_DOC_MOVED_OR_DELETED, cp.getShortTitle(), filename);
				}
			}

			if (file == null) {
				return ResponseEvent.userError(CpErrorCode.SOP_DOC_NOT_FOUND, cp.getShortTitle());
			}

			return ResponseEvent.response(file);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public ResponseEvent<String> uploadSopDocument(RequestEvent<FileDetail> req) {
		OutputStream out = null;

		try {
			FileDetail detail = req.getPayload();
			String filename = UUID.randomUUID() + "_" + detail.getFilename();

			File file = new File(getSopDocDir() + filename);
			out = new FileOutputStream(file);
			IOUtils.copy(detail.getFileIn(), out);

			return ResponseEvent.response(filename);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		} finally {
			IOUtils.closeQuietly(out);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolDetail> importCollectionProtocol(RequestEvent<CollectionProtocolDetail> req) {
		try {
			CollectionProtocolDetail cpDetail = req.getPayload();
			
			ResponseEvent<CollectionProtocolDetail> resp = createCollectionProtocol(req);
			resp.throwErrorIfUnsuccessful();

			Long cpId = resp.getPayload().getId();
			importConsents(cpId, cpDetail.getConsents());
			importEvents(cpDetail.getTitle(), cpDetail.getEvents());
			importWorkflows(cpId, cpDetail.getWorkflows());
			
			return resp;
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> isSpecimenBarcodingEnabled() {
		try {
			boolean isEnabled = ConfigUtil.getInstance().getBoolSetting(
					ConfigParams.MODULE, ConfigParams.ENABLE_SPMN_BARCODING, false);

			if (!isEnabled) {
				isEnabled = daoFactory.getCollectionProtocolDao().anyBarcodingEnabledCpExists();
			}

			return ResponseEvent.response(isEnabled);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<ConsentTierDetail>> getConsentTiers(RequestEvent<Long> req) {
		Long cpId = req.getPayload();

		try {
			CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getById(cpId);
			if (cp == null) {
				return ResponseEvent.userError(CpErrorCode.NOT_FOUND);
			}
			
			AccessCtrlMgr.getInstance().ensureReadCpRights(cp);
			return ResponseEvent.response(ConsentTierDetail.from(cp.getConsentTier()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<ConsentTierDetail> updateConsentTier(RequestEvent<ConsentTierOp> req) {
		try {
			ConsentTierOp opDetail = req.getPayload();
			CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getById(opDetail.getCpId());
			if (cp == null) {
				return ResponseEvent.userError(CpErrorCode.NOT_FOUND);
			}
			
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(cp);
			
			if (cp.isConsentsWaived()) {
				return ResponseEvent.userError(CpErrorCode.CONSENTS_WAIVED, cp.getShortTitle());
			}
			
			ConsentTierDetail input = opDetail.getConsentTier();
			CpConsentTier resp = null;			
			ConsentStatement stmt = null;
			switch (opDetail.getOp()) {
				case ADD:
					ensureUniqueConsentStatement(input, cp);
					stmt = getStatement(input.getStatementId(), input.getStatementCode(), input.getStatement());
					resp = cp.addConsentTier(getConsentTierObj(input.getId(), stmt));
					break;
					
				case UPDATE:
					ensureUniqueConsentStatement(input, cp);
					stmt = getStatement(input.getStatementId(), input.getStatementCode(), input.getStatement());
					resp = cp.updateConsentTier(getConsentTierObj(input.getId(), stmt));
					break;
					
				case REMOVE:
					resp = cp.removeConsentTier(input.getId());
					break;			    
			}
			
			if (resp != null) {
				daoFactory.getCollectionProtocolDao().saveOrUpdate(cp, true);
			}
						
			return ResponseEvent.response(ConsentTierDetail.from(resp));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}		
	}	
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<DependentEntityDetail>> getConsentDependentEntities(RequestEvent<ConsentTierDetail> req) {
		try {
			ConsentTierDetail consentTierDetail = req.getPayload();
			CpConsentTier consentTier = getConsentTier(consentTierDetail);
			return ResponseEvent.response(consentTier.getDependentEntities());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch(Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<CollectionProtocolEventDetail>> getProtocolEvents(RequestEvent<Long> req) {
		Long cpId = req.getPayload();
		
		try {
			CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getById(cpId);
			if (cp == null) {
				return ResponseEvent.userError(CpErrorCode.NOT_FOUND);
			}

			AccessCtrlMgr.getInstance().ensureReadCpRights(cp);
			return ResponseEvent.response(CollectionProtocolEventDetail.from(cp.getOrderedCpeList()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}		
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolEventDetail> getProtocolEvent(RequestEvent<Long> req) {
		Long cpeId = req.getPayload();
		
		try {
			CollectionProtocolEvent cpe = daoFactory.getCollectionProtocolDao().getCpe(cpeId);
			if (cpe == null) {
				return ResponseEvent.userError(CpeErrorCode.NOT_FOUND, cpeId, 1);
			}

			CollectionProtocol cp = cpe.getCollectionProtocol();
			AccessCtrlMgr.getInstance().ensureReadCpRights(cp);

			if (cpe.getEventPoint() != null) {
				CollectionProtocolEvent firstEvent = cp.firstEvent();
				if (firstEvent.getEventPoint() != null) {
					cpe.setOffset(firstEvent.getEventPoint());
					cpe.setOffsetUnit(firstEvent.getEventPointUnit());
				}
			}

			return ResponseEvent.response(CollectionProtocolEventDetail.from(cpe));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		}  catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
		
	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolEventDetail> addEvent(RequestEvent<CollectionProtocolEventDetail> req) {
		try {
			CollectionProtocolEvent cpe = cpeFactory.createCpe(req.getPayload());			
			CollectionProtocol cp = cpe.getCollectionProtocol();
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(cp);
			
			cp.addCpe(cpe);			
			daoFactory.getCollectionProtocolDao().saveOrUpdate(cp, true);			
			return ResponseEvent.response(CollectionProtocolEventDetail.from(cpe));			
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolEventDetail> updateEvent(RequestEvent<CollectionProtocolEventDetail> req) {
		try {
			CollectionProtocolEvent cpe = cpeFactory.createCpe(req.getPayload());			
			CollectionProtocol cp = cpe.getCollectionProtocol();
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(cp);
			
			cp.updateCpe(cpe);
			return ResponseEvent.response(CollectionProtocolEventDetail.from(cpe));			
		} catch (OpenSpecimenException ose) {		
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolEventDetail> copyEvent(RequestEvent<CopyCpeOpDetail> req) {
		try {
			CollectionProtocolDao cpDao = daoFactory.getCollectionProtocolDao();
			
			CopyCpeOpDetail opDetail = req.getPayload();
			String cpTitle = opDetail.getCollectionProtocol();
			String eventLabel = opDetail.getEventLabel();
			
			CollectionProtocolEvent existing = null;
			Object key = null;
			if (opDetail.getEventId() != null) {
				existing = cpDao.getCpe(opDetail.getEventId());
				key = opDetail.getEventId();
			} else if (!StringUtils.isBlank(eventLabel) && !StringUtils.isBlank(cpTitle)) {
				existing = cpDao.getCpeByEventLabel(cpTitle, eventLabel);
				key = eventLabel;
			}
			
			if (existing == null) {
				throw OpenSpecimenException.userError(CpeErrorCode.NOT_FOUND, key, 1);
			}
			
			CollectionProtocol cp = existing.getCollectionProtocol();
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(cp);
			
			CollectionProtocolEvent cpe = cpeFactory.createCpeCopy(opDetail.getCpe(), existing);
			existing.copySpecimenRequirementsTo(cpe);			
			
			cp.addCpe(cpe);			
			cpDao.saveOrUpdate(cp, true);			
			return ResponseEvent.response(CollectionProtocolEventDetail.from(cpe));			
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolEventDetail> deleteEvent(RequestEvent<Long> req) {
		try {
			Long cpeId = req.getPayload();
			CollectionProtocolEvent cpe = daoFactory.getCollectionProtocolDao().getCpe(cpeId);
			if (cpe == null) {
				throw OpenSpecimenException.userError(CpeErrorCode.NOT_FOUND, cpeId, 1);
			}
			
			CollectionProtocol cp = cpe.getCollectionProtocol();
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(cp);
			
			cpe.delete();
			daoFactory.getCollectionProtocolDao().saveCpe(cpe);
			return ResponseEvent.response(CollectionProtocolEventDetail.from(cpe));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
		
	@Override
	@PlusTransactional
	public ResponseEvent<List<SpecimenRequirementDetail>> getSpecimenRequirments(RequestEvent<Tuple> req) {
		try {
			Tuple tuple = req.getPayload();
			Long cpId = tuple.element(0);
			Long cpeId = tuple.element(1);
			String cpeLabel = tuple.element(2);
			boolean includeChildren = tuple.element(3) == null || (Boolean) tuple.element(3);

			CollectionProtocolEvent cpe = null;
			Object key = null;
			if (cpeId != null) {
				cpe = daoFactory.getCollectionProtocolDao().getCpe(cpeId);
				key = cpeId;
			} else if (StringUtils.isNotBlank(cpeLabel)) {
				if (cpId == null) {
					throw OpenSpecimenException.userError(CpErrorCode.REQUIRED);
				}

				cpe = daoFactory.getCollectionProtocolDao().getCpeByEventLabel(cpId, cpeLabel);
				key = cpeLabel;
			}

			if (key == null) {
				return ResponseEvent.response(Collections.emptyList());
			} else if (cpe == null) {
				return ResponseEvent.userError(CpeErrorCode.NOT_FOUND, key, 1);
			}
			
			AccessCtrlMgr.getInstance().ensureReadCpRights(cpe.getCollectionProtocol());
			return ResponseEvent.response(SpecimenRequirementDetail.from(cpe.getTopLevelAnticipatedSpecimens(), includeChildren));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		}  catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenRequirementDetail> getSpecimenRequirement(RequestEvent<Long> req) {
		Long reqId = req.getPayload();
		try {
			SpecimenRequirement sr = daoFactory.getSpecimenRequirementDao().getById(reqId);
			if (sr == null) {
				return ResponseEvent.userError(SrErrorCode.NOT_FOUND);
			}
			
			AccessCtrlMgr.getInstance().ensureReadCpRights(sr.getCollectionProtocol());
			return ResponseEvent.response(SpecimenRequirementDetail.from(sr));				
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	
	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenRequirementDetail> addSpecimenRequirement(RequestEvent<SpecimenRequirementDetail> req) {
		try {
			SpecimenRequirement requirement = srFactory.createSpecimenRequirement(req.getPayload());			
			CollectionProtocolEvent cpe = requirement.getCollectionProtocolEvent();
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(cpe.getCollectionProtocol());
			
			cpe.addSpecimenRequirement(requirement);
			daoFactory.getCollectionProtocolDao().saveCpe(cpe, true);
			return ResponseEvent.response(SpecimenRequirementDetail.from(requirement));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<SpecimenRequirementDetail>> addSpecimenPoolReqs(RequestEvent<SpecimenPoolRequirements> req) {
		try {
			List<SpecimenRequirement> spmnPoolReqs = srFactory.createSpecimenPoolReqs(req.getPayload());

			SpecimenRequirement pooledReq = spmnPoolReqs.iterator().next().getPooledSpecimenRequirement();
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(pooledReq.getCollectionProtocol());

			pooledReq.getCollectionProtocolEvent().ensureUniqueSrCodes(spmnPoolReqs);
			pooledReq.addSpecimenPoolReqs(spmnPoolReqs);
			daoFactory.getSpecimenRequirementDao().saveOrUpdate(pooledReq, true);
			return ResponseEvent.response(SpecimenRequirementDetail.from(spmnPoolReqs));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<SpecimenRequirementDetail>> createAliquots(RequestEvent<AliquotSpecimensRequirement> req) {
		try {
			return ResponseEvent.response(SpecimenRequirementDetail.from(createAliquots(req.getPayload())));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenRequirementDetail> createDerived(RequestEvent<DerivedSpecimenRequirement> req) {
		try {
			DerivedSpecimenRequirement requirement = req.getPayload();
			SpecimenRequirement derived = srFactory.createDerived(requirement);						
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(derived.getCollectionProtocol());
			ensureSrIsNotClosed(derived.getParentSpecimenRequirement());

			if (StringUtils.isNotBlank(derived.getCode())) {
				if (derived.getCollectionProtocolEvent().getSrByCode(derived.getCode()) != null) {
					return ResponseEvent.userError(SrErrorCode.DUP_CODE, derived.getCode());
				}
			}
			
			daoFactory.getSpecimenRequirementDao().saveOrUpdate(derived, true);
			return ResponseEvent.response(SpecimenRequirementDetail.from(derived));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenRequirementDetail> updateSpecimenRequirement(RequestEvent<SpecimenRequirementDetail> req) {
		try {
			SpecimenRequirementDetail detail = req.getPayload();
			Long srId = detail.getId();
			SpecimenRequirement sr = daoFactory.getSpecimenRequirementDao().getById(srId);
			if (sr == null) {
				throw OpenSpecimenException.userError(SrErrorCode.NOT_FOUND);
			}
			
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(sr.getCollectionProtocol());
			SpecimenRequirement partial = srFactory.createForUpdate(sr.getLineage(), detail);
			if (isSpecimenClassOrTypeChanged(sr, partial)) {
				ensureSpecimensNotCollected(sr);
			}
			
			sr.update(partial);
			return ResponseEvent.response(SpecimenRequirementDetail.from(sr));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenRequirementDetail> copySpecimenRequirement(RequestEvent<Long> req) {
		try {
			Long srId = req.getPayload();
			
			SpecimenRequirement sr = daoFactory.getSpecimenRequirementDao().getById(srId);
			if (sr == null) {
				throw OpenSpecimenException.userError(SrErrorCode.NOT_FOUND);
			}
			
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(sr.getCollectionProtocol());
			SpecimenRequirement copy = sr.deepCopy(null);
			daoFactory.getSpecimenRequirementDao().saveOrUpdate(copy, true);
			return ResponseEvent.response(SpecimenRequirementDetail.from(copy));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenRequirementDetail> deleteSpecimenRequirement(RequestEvent<Long> req) {
		try {
			Long srId = req.getPayload();
			SpecimenRequirement sr = daoFactory.getSpecimenRequirementDao().getById(srId);
			if (sr == null) {
				throw OpenSpecimenException.userError(SrErrorCode.NOT_FOUND);
			}
			
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(sr.getCollectionProtocol());
			sr.delete();
			daoFactory.getSpecimenRequirementDao().saveOrUpdate(sr);
			return ResponseEvent.response(SpecimenRequirementDetail.from(sr));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<Integer> getSrSpecimensCount(RequestEvent<Long> req) {
		try {
			Long srId = req.getPayload();
			SpecimenRequirement sr = daoFactory.getSpecimenRequirementDao().getById(srId);
			if (sr == null) {
				throw OpenSpecimenException.userError(SrErrorCode.NOT_FOUND);
			}
			
			return ResponseEvent.response(
					daoFactory.getSpecimenRequirementDao().getSpecimensCount(srId));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CpReportSettingsDetail> getReportSettings(RequestEvent<CpQueryCriteria> req) {
		try {
			CpReportSettings settings = getReportSetting(req.getPayload());
			if (settings == null) {
				return ResponseEvent.response(null);
			}

			AccessCtrlMgr.getInstance().ensureReadCpRights(settings.getCp());
			return ResponseEvent.response(CpReportSettingsDetail.from(settings));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CpReportSettingsDetail> saveReportSettings(RequestEvent<CpReportSettingsDetail> req) {
		try {
			CpReportSettings settings = rptSettingsFactory.createSettings(req.getPayload());
			CollectionProtocol cp = settings.getCp();
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(cp);

			CpReportSettings existing = daoFactory.getCpReportSettingsDao().getByCp(cp.getId());
			if (existing == null) {
				existing = settings;
			} else {
				existing.update(settings);
			}

			daoFactory.getCpReportSettingsDao().saveOrUpdate(existing);
			return ResponseEvent.response(CpReportSettingsDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CpReportSettingsDetail> deleteReportSettings(RequestEvent<CpQueryCriteria> req) {
		try {
			CpReportSettings settings = getReportSetting(req.getPayload());
			if (settings == null) {
				return ResponseEvent.response(null);
			}

			AccessCtrlMgr.getInstance().ensureUpdateCpRights(settings.getCp());
			settings.delete();
			return ResponseEvent.response(CpReportSettingsDetail.from(settings));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> generateReport(RequestEvent<CpQueryCriteria> req) {
		try {
			CpQueryCriteria crit = req.getPayload();
			CollectionProtocol cp = getCollectionProtocol(crit.getId(), crit.getTitle(), crit.getShortTitle());
			AccessCtrlMgr.getInstance().ensureReadCpRights(cp);

			CpReportSettings cpSettings = daoFactory.getCpReportSettingsDao().getByCp(cp.getId());
			if (cpSettings != null && !cpSettings.isEnabled()) {
				return ResponseEvent.userError(CpErrorCode.RPT_DISABLED, cp.getShortTitle());
			}

			taskExecutor.execute(new CpReportTask(cp.getId()));
			return ResponseEvent.response(true);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<File> getReportFile(Long cpId, String fileId) {
		try {
			CollectionProtocol cp = getCollectionProtocol(cpId, null, null);
			AccessCtrlMgr.getInstance().ensureReadCpRights(cp);

			File file = new CpReportGenerator().getDataFile(cpId, fileId);
			if (file == null) {
				return ResponseEvent.userError(CpErrorCode.RPT_FILE_NOT_FOUND);
			}

			return ResponseEvent.response(file);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CpWorkflowCfgDetail> getWorkflows(RequestEvent<Long> req) {
		Long cpId = req.getPayload();
		CollectionProtocol cp = null;

		CpWorkflowConfig cfg;
		if (cpId == null || cpId == -1L) {
			cfg = WorkflowUtil.getInstance().getSysWorkflows();
		} else {
			cp = daoFactory.getCollectionProtocolDao().getById(cpId);
			if (cp == null) {
				return ResponseEvent.userError(CpErrorCode.NOT_FOUND);
			}

			cfg = daoFactory.getCollectionProtocolDao().getCpWorkflows(cpId);
		}

		if (cfg == null) {
			cfg = new CpWorkflowConfig();
			cfg.setCp(cp);
		}
		
		return ResponseEvent.response(CpWorkflowCfgDetail.from(cfg));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CpWorkflowCfgDetail> saveWorkflows(RequestEvent<CpWorkflowCfgDetail> req) {
		try {
			CpWorkflowCfgDetail input = req.getPayload();
			CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getById(input.getCpId());
			if (cp == null) {
				return ResponseEvent.userError(CpErrorCode.NOT_FOUND);
			}

			AccessCtrlMgr.getInstance().ensureUpdateCpRights(cp);
			CpWorkflowConfig cfg = saveWorkflows(cp, input);
			return ResponseEvent.response(CpWorkflowCfgDetail.from(cfg));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<CollectionProtocolSummary>> getRegisterEnabledCps(
			List<String> siteNames, String searchTitle, int maxResults) {
		try {
			Set<Long> cpIds = AccessCtrlMgr.getInstance().getRegisterEnabledCpIds(siteNames);
			
			CpListCriteria crit = new CpListCriteria().title(searchTitle).maxResults(maxResults);
			if (cpIds != null && cpIds.isEmpty()) {
				return ResponseEvent.response(Collections.<CollectionProtocolSummary>emptyList());
			} else if (cpIds != null) {
				crit.ids(new ArrayList<>(cpIds));
			}

			return ResponseEvent.response(daoFactory.getCollectionProtocolDao().getCollectionProtocols(crit));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ListConfig> getCpListCfg(RequestEvent<Map<String, Object>> req) {
		return listSvc.getListCfg(req);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ListDetail> getList(RequestEvent<Map<String, Object>> req) {
		return listSvc.getList(req);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Integer> getListSize(RequestEvent<Map<String, Object>> req) {
		return listSvc.getListSize(req);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Collection<Object>> getListExprValues(RequestEvent<Map<String, Object>> req) {
		return listSvc.getListExprValues(req);
	}

	@Override
	public String getObjectName() {
		return CollectionProtocol.getEntityName();
	}

	@Override
	@PlusTransactional
	public Map<String, Object> resolveUrl(String key, Object value) {
		if (key.equals("id")) {
			value = Long.valueOf(value.toString());
		}

		return daoFactory.getCollectionProtocolDao().getCpIds(key, value);
	}

	@Override
	public String getAuditTable() {
		return "CAT_COLLECTION_PROTOCOL_AUD";
	}

	@Override
	public void ensureReadAllowed(Long id) {
		CollectionProtocol cp = getCollectionProtocol(id, null, null);
		AccessCtrlMgr.getInstance().ensureReadCpRights(cp);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		listSvc.registerListConfigurator("participant-list-view", this::getParticipantsListConfig);
		listSvc.registerListConfigurator("specimen-list-view", this::getSpecimenListConfig);
	}


	@Override
	public CpWorkflowConfig saveWorkflows(CollectionProtocol cp, CpWorkflowCfgDetail input) {
		CpWorkflowConfig cfg = daoFactory.getCollectionProtocolDao().getCpWorkflows(cp.getId());
		if (cfg == null) {
			cfg = new CpWorkflowConfig();
			cfg.setCp(cp);
		}

		if (!input.isPatch()) {
			cfg.getWorkflows().clear();
		}

		if (input.getWorkflows() != null) {
			for (WorkflowDetail detail : input.getWorkflows().values()) {
				Workflow wf = new Workflow();
				BeanUtils.copyProperties(detail, wf);
				cfg.getWorkflows().put(wf.getName(), wf);
			}
		}

		daoFactory.getCollectionProtocolDao().saveCpWorkflows(cfg);
		return cfg;
	}

	private CpListCriteria addCpListCriteria(CpListCriteria crit) {
		Set<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadableSiteCps();
		return siteCps != null && siteCps.isEmpty() ? null : crit.siteCps(siteCps);
	}

	private CprListCriteria addCprListCriteria(CprListCriteria listCrit) {
		ParticipantReadAccess access = AccessCtrlMgr.getInstance().getParticipantReadAccess(listCrit.cpId());
		if (!access.admin) {
			if (access.noAccessibleSites() || (!access.phiAccess && listCrit.hasPhiFields())) {
				return null;
			}
		}

		return listCrit.includePhi(access.phiAccess)
			.phiSiteCps(access.phiSiteCps)
			.siteCps(access.siteCps)
			.useMrnSites(AccessCtrlMgr.getInstance().isAccessRestrictedBasedOnMrn());
	}

	private CollectionProtocol createCollectionProtocol(CollectionProtocolDetail detail, CollectionProtocol existing, boolean createCopy) {
		CollectionProtocol cp = null;
		if (!createCopy) {
			cp = cpFactory.createCollectionProtocol(detail);
		} else {
			cp = cpFactory.createCpCopy(detail, existing);
		}
		
		AccessCtrlMgr.getInstance().ensureCreateCpRights(cp);
		ensureUsersBelongtoCpSites(cp);
		
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		ensureUniqueTitle(null, cp, ose);
		ensureUniqueShortTitle(null, cp, ose);
		ensureUniqueCode(null, cp, ose);
		ensureUniqueCpSiteCode(cp, ose);
		ose.checkAndThrow();

		daoFactory.getCollectionProtocolDao().saveOrUpdate(cp, true);
		cp.addOrUpdateExtension();

		//Assign default roles to PI and Coordinators
		addOrRemovePiCoordinatorRoles(cp, "CREATE", null, cp.getPrincipalInvestigator(), cp.getCoordinators(), null);
		fixSopDocumentName(cp);
		copyWorkflows(existing, cp);
		return cp;
	}

	private void ensureUsersBelongtoCpSites(CollectionProtocol cp) {
		ensureCreatorBelongToCpSites(cp);
	}
	
	private void ensureCreatorBelongToCpSites(CollectionProtocol cp) {
		User user = AuthUtil.getCurrentUser();
		if (user.isAdmin()) {
			return;
		}

		Set<Site> cpSites = cp.getRepositories();
		if (cpSites.stream().anyMatch(cpSite -> !cpSite.getInstitute().equals(AuthUtil.getCurrentUserInstitute()))) {
			throw OpenSpecimenException.userError(CpErrorCode.CREATOR_DOES_NOT_BELONG_CP_REPOS);
		}
	}
	
	private void ensureUniqueTitle(CollectionProtocol existingCp, CollectionProtocol cp, OpenSpecimenException ose) {
		String title = cp.getTitle();
		if (existingCp != null && existingCp.getTitle().equals(title)) {
			return;
		}
		
		CollectionProtocol dbCp = daoFactory.getCollectionProtocolDao().getCollectionProtocol(title);
		if (dbCp != null) {
			ose.addError(CpErrorCode.DUP_TITLE, title);
		}		
	}
	
	private void ensureUniqueShortTitle(CollectionProtocol existingCp, CollectionProtocol cp, OpenSpecimenException ose) {
		String shortTitle = cp.getShortTitle();
		if (existingCp != null && existingCp.getShortTitle().equals(shortTitle)) {
			return;
		}
		
		CollectionProtocol dbCp = daoFactory.getCollectionProtocolDao().getCpByShortTitle(shortTitle);
		if (dbCp != null) {
			ose.addError(CpErrorCode.DUP_SHORT_TITLE, shortTitle);
		}
	}
	
	private void ensureUniqueCode(CollectionProtocol existingCp, CollectionProtocol cp, OpenSpecimenException ose) {
		String code = cp.getCode();
		if (StringUtils.isBlank(code)) {
			return;
		}
		
		if (existingCp != null && code.equals(existingCp.getCode())) {
			return;
		}
		
		CollectionProtocol dbCp = daoFactory.getCollectionProtocolDao().getCpByCode(code);
		if (dbCp != null) {
			ose.addError(CpErrorCode.DUP_CODE, code);
		}
	}
	
	private void ensureUniqueCpSiteCode(CollectionProtocol cp, OpenSpecimenException ose) {
		List<String> codes = Utility.<List<String>>collect(cp.getSites(), "code");
		codes.removeAll(Arrays.asList(new String[] {null, ""}));
		
		Set<String> uniqueCodes = new HashSet<String>(codes);
		if (codes.size() != uniqueCodes.size()) {
			ose.addError(CpErrorCode.DUP_CP_SITE_CODES, codes);
		}
	}

	private void ensureSitesAreNotInUse(CollectionProtocol cp, Collection<Site> sites) {
		if (sites.isEmpty()) {
			return;
		}

		List<Long> siteIds = sites.stream().map(Site::getId).collect(Collectors.toList());
		Map<String, Integer> counts = daoFactory.getCprDao().getParticipantsBySite(cp.getId(), siteIds);
		if (!counts.isEmpty()) {
			String siteLabels = counts.keySet().stream().collect(Collectors.joining(", "));
			throw OpenSpecimenException.userError(CpErrorCode.USED_SITES, siteLabels, counts.size());
		}
	}

	private void fixSopDocumentName(CollectionProtocol cp) {
		if (StringUtils.isBlank(cp.getSopDocumentName())) {
			return;
		}

		String[] nameParts = cp.getSopDocumentName().split("_", 2);
		if (nameParts[0].equals(cp.getId().toString())) {
			return;
		}

		try {
			UUID uuid = UUID.fromString(nameParts[0]);
		} catch (Exception e) {
			throw OpenSpecimenException.userError(CpErrorCode.INVALID_SOP_DOC, cp.getSopDocumentName());
		}

		if (StringUtils.isBlank(nameParts[1])) {
			throw OpenSpecimenException.userError(CpErrorCode.INVALID_SOP_DOC, cp.getSopDocumentName());
		}

		File src = new File(getSopDocDir() + File.separator + cp.getSopDocumentName());
		if (!src.exists()) {
			throw OpenSpecimenException.userError(CpErrorCode.SOP_DOC_MOVED_OR_DELETED, cp.getSopDocumentName(), cp.getShortTitle());
		}

		cp.setSopDocumentName(cp.getId() + "_" + nameParts[1]);

		File dest = new File(getSopDocDir() + File.separator + cp.getSopDocumentName());
		src.renameTo(dest);
	}

	private void ensureConsentTierIsEmpty(CollectionProtocol existingCp, OpenSpecimenException ose) {
		if (CollectionUtils.isNotEmpty(existingCp.getConsentTier())) {
			ose.addError(CpErrorCode.CONSENT_TIER_FOUND, existingCp.getShortTitle());
		}
	}
	
	private void importConsents(Long cpId, List<ConsentTierDetail> consents) {
		if (CollectionUtils.isEmpty(consents)) {
			return;			
		}
		
		for (ConsentTierDetail consent : consents) {
			ConsentTierOp addOp = new ConsentTierOp();
			addOp.setConsentTier(consent);
			addOp.setCpId(cpId);
			addOp.setOp(OP.ADD);

			ResponseEvent<ConsentTierDetail> resp = updateConsentTier(new RequestEvent<>(addOp));
			resp.throwErrorIfUnsuccessful();
		}
	}
	
	private void importEvents(String cpTitle, List<CollectionProtocolEventDetail> events) {
		if (CollectionUtils.isEmpty(events)) {
			return;
		}
		
		for (CollectionProtocolEventDetail event : events) {
			if (Status.isClosedOrDisabledStatus(event.getActivityStatus())) {
				continue;
			}

			event.setCollectionProtocol(cpTitle);
			ResponseEvent<CollectionProtocolEventDetail> resp = addEvent(new RequestEvent<>(event));
			resp.throwErrorIfUnsuccessful();
			
			Long eventId = resp.getPayload().getId();
			importSpecimenReqs(eventId, null, event.getSpecimenRequirements());
		}
	}
	
	private void importSpecimenReqs(Long eventId, Long parentSrId, List<SpecimenRequirementDetail> srs) {
		if (CollectionUtils.isEmpty(srs)) {
			return;
		}
		
		for (SpecimenRequirementDetail sr : srs) {
			if (Status.isClosedOrDisabledStatus(sr.getActivityStatus())) {
				continue;
			}

			sr.setEventId(eventId);
			
			if (sr.getLineage().equals(Specimen.NEW)) {
				ResponseEvent<SpecimenRequirementDetail> resp = addSpecimenRequirement(new RequestEvent<>(sr));
				resp.throwErrorIfUnsuccessful();
				
				importSpecimenReqs(eventId, resp.getPayload().getId(), sr.getChildren());
			} else if (parentSrId != null && sr.getLineage().equals(Specimen.ALIQUOT)) {				
				AliquotSpecimensRequirement aliquotReq = sr.toAliquotRequirement(parentSrId, 1);
				List<SpecimenRequirement> aliquots = createAliquots(aliquotReq);

				if (StringUtils.isNotBlank(sr.getCode())) {
					aliquots.get(0).setCode(sr.getCode());
				}
				
				importSpecimenReqs(eventId, aliquots.get(0).getId(), sr.getChildren());
			} else if (parentSrId != null && sr.getLineage().equals(Specimen.DERIVED)) {
				DerivedSpecimenRequirement derivedReq = sr.toDerivedRequirement(parentSrId);
				ResponseEvent<SpecimenRequirementDetail> resp = createDerived(new RequestEvent<DerivedSpecimenRequirement>(derivedReq));
				resp.throwErrorIfUnsuccessful();
				
				importSpecimenReqs(eventId, resp.getPayload().getId(), sr.getChildren());
			}			
		}
	}

	private void ensureSrIsNotClosed(SpecimenRequirement sr) {
		if (!sr.isClosed()) {
			return;
		}

		String key = sr.getCode();
		if (StringUtils.isBlank(key)) {
			key = sr.getName();
		}

		if (StringUtils.isBlank(key)) {
			key = sr.getId().toString();
		}

		throw OpenSpecimenException.userError(SrErrorCode.CLOSED, key);
	}

	private List<SpecimenRequirement> createAliquots(AliquotSpecimensRequirement requirement) {
		List<SpecimenRequirement> aliquots = srFactory.createAliquots(requirement);
		SpecimenRequirement aliquot = aliquots.iterator().next();
		AccessCtrlMgr.getInstance().ensureUpdateCpRights(aliquot.getCollectionProtocol());

		SpecimenRequirement parent = aliquot.getParentSpecimenRequirement();
		if (StringUtils.isNotBlank(requirement.getCode())) {
			setAliquotCode(parent, aliquots, requirement.getCode());
		}

		parent.addChildRequirements(aliquots);
		daoFactory.getSpecimenRequirementDao().saveOrUpdate(parent, true);
		return aliquots;
	}

	private void importWorkflows(Long cpId, Map<String, WorkflowDetail> workflows) {
		CpWorkflowCfgDetail input = new CpWorkflowCfgDetail();
		input.setCpId(cpId);
		input.setWorkflows(workflows);

		ResponseEvent<CpWorkflowCfgDetail> resp = saveWorkflows(new RequestEvent<>(input));
		resp.throwErrorIfUnsuccessful();
	}

	private void copyWorkflows(CollectionProtocol srcCp, CollectionProtocol dstCp) {
		if (srcCp == null) {
			return;
		}

		CpWorkflowConfig srcWfCfg = daoFactory.getCollectionProtocolDao().getCpWorkflows(srcCp.getId());
		if (srcWfCfg != null) {
			CpWorkflowConfig newConfig = new CpWorkflowConfig();
			newConfig.setCp(dstCp);
			newConfig.setWorkflows(srcWfCfg.getWorkflows());
			daoFactory.getCollectionProtocolDao().saveCpWorkflows(newConfig);
		}
	}

	private void addOrRemovePiCoordinatorRoles(CollectionProtocol cp, String cpOp, User oldPi, User newPi, Collection<User> newCoord, Collection<User> removedCoord) {
		List<User> notifUsers = null;
		if (!Objects.equals(oldPi, newPi)) {
			notifUsers = AccessCtrlMgr.getInstance().getSiteAdmins(null, cp);

			if (newPi != null) {
				addDefaultPiRoles(cp, notifUsers, newPi, cpOp);
			}

			if (oldPi != null) {
				removeDefaultPiRoles(cp, notifUsers, oldPi, cpOp);
			}
		}

		if (CollectionUtils.isNotEmpty(newCoord)) {
			if (notifUsers == null) {
				notifUsers = AccessCtrlMgr.getInstance().getSiteAdmins(null, cp);
			}

			addDefaultCoordinatorRoles(cp, notifUsers, newCoord, cpOp);
		}

		if (CollectionUtils.isNotEmpty(removedCoord)) {
			if (notifUsers == null) {
				notifUsers = AccessCtrlMgr.getInstance().getSiteAdmins(null, cp);
			}

			removeDefaultCoordinatorRoles(cp, notifUsers, removedCoord, cpOp);
		}
	}

	private void addDefaultPiRoles(CollectionProtocol cp, List<User> notifUsers, User user, String cpOp) {
		try {
			if (user.isContact()) {
				return;
			}

			for (String role : getDefaultPiRoles()) {
				addRole(cp, notifUsers, user, role, cpOp);
			}
		} catch (OpenSpecimenException ose) {
			ose.rethrow(RbacErrorCode.ACCESS_DENIED, CpErrorCode.USER_UPDATE_RIGHTS_REQUIRED);
			throw ose;
		}
	}

	private void removeDefaultPiRoles(CollectionProtocol cp, List<User> notifUsers, User user, String cpOp) {
		try {
			for (String role : getDefaultPiRoles()) {
				removeRole(cp, notifUsers, user, role, cpOp);
			}
		} catch (OpenSpecimenException ose) {
			ose.rethrow(RbacErrorCode.ACCESS_DENIED, CpErrorCode.USER_UPDATE_RIGHTS_REQUIRED);
		}
	}
	
	private void addDefaultCoordinatorRoles(CollectionProtocol cp, List<User> notifUsers, Collection<User> coordinators, String cpOp) {
		try {
			for (User user : coordinators) {
				if (user.isContact()) {
					continue;
				}

				for (String role : getDefaultCoordinatorRoles()) {
					addRole(cp, notifUsers, user, role, cpOp);
				}
			}
		} catch (OpenSpecimenException ose) {
			ose.rethrow(RbacErrorCode.ACCESS_DENIED, CpErrorCode.USER_UPDATE_RIGHTS_REQUIRED);
		}
	}
	
	private void removeDefaultCoordinatorRoles(CollectionProtocol cp, List<User> notifUsers, Collection<User> coordinators, String cpOp) {
		try {
			for (User user : coordinators) {
				for (String role : getDefaultCoordinatorRoles()) {
					removeRole(cp, notifUsers, user, role, cpOp);
				}
			}
		} catch (OpenSpecimenException ose) {
			ose.rethrow(RbacErrorCode.ACCESS_DENIED, CpErrorCode.USER_UPDATE_RIGHTS_REQUIRED);
		}
	}

	private void addRole(CollectionProtocol cp, List<User> admins, User user, String role, String cpOp) {
		SubjectRoleOpNotif notifReq = getNotifReq(cp, role, admins, user, cpOp, "ADD");
		rbacSvc.addSubjectRole(null, cp, user, new String[] { role }, notifReq);
	}

	private void removeRole(CollectionProtocol cp, List<User> admins, User user, String role, String cpOp) {
		SubjectRoleOpNotif notifReq = getNotifReq(cp, role, admins, user, cpOp, "REMOVE");
		rbacSvc.removeSubjectRole(null, cp, user, new String[] { role }, notifReq);
	}
	
	private String[] getDefaultPiRoles() {
		return new String[] {"Principal Investigator"};
	}
	
	private String[] getDefaultCoordinatorRoles() {
		return new String[] {"Coordinator"};
	}
	
	private CpConsentTier getConsentTier(ConsentTierDetail consentTierDetail) {
		CollectionProtocolDao cpDao = daoFactory.getCollectionProtocolDao();
		
		CpConsentTier consentTier = null;
		if (consentTierDetail.getId() != null) {
			consentTier = cpDao.getConsentTier(consentTierDetail.getId());
		} else if (StringUtils.isNotBlank(consentTierDetail.getStatement()) && consentTierDetail.getCpId() != null ) {
			consentTier = cpDao.getConsentTierByStatement(consentTierDetail.getCpId(), consentTierDetail.getStatement());
		}
		
		if (consentTier == null) {
			throw OpenSpecimenException.userError(CpErrorCode.CONSENT_TIER_NOT_FOUND);
		}
		
		return consentTier;
	}

	private void ensureUniqueConsentStatement(ConsentTierDetail consentTierDetail, CollectionProtocol cp) {
		Predicate<CpConsentTier> findFn;
		if (consentTierDetail.getStatementId() != null) {
			findFn = (t) -> t.getStatement().getId().equals(consentTierDetail.getStatementId());
		} else if (StringUtils.isNotBlank(consentTierDetail.getStatementCode())) {
			findFn = (t) -> t.getStatement().getCode().equals(consentTierDetail.getStatementCode());
		} else if (StringUtils.isNotBlank(consentTierDetail.getStatement())) {
			findFn = (t) -> t.getStatement().getStatement().equals(consentTierDetail.getStatement());
		} else {
			throw OpenSpecimenException.userError(ConsentStatementErrorCode.CODE_REQUIRED);
		}

		CpConsentTier tier = cp.getConsentTier().stream().filter(findFn).findFirst().orElse(null);
		if (tier != null && !tier.getId().equals(consentTierDetail.getId())) {
			throw OpenSpecimenException.userError(CpErrorCode.DUP_CONSENT, tier.getStatement().getCode(), cp.getShortTitle());
		}
	}

	private CpConsentTier getConsentTierObj(Long id, ConsentStatement stmt) {
		CpConsentTier tier = new CpConsentTier();
		tier.setId(id);
		tier.setStatement(stmt);
		return tier;
	}

	private void ensureSpecimensNotCollected(SpecimenRequirement sr) {
		int count = daoFactory.getSpecimenRequirementDao().getSpecimensCount(sr.getId());
		if (count > 0) {
			throw OpenSpecimenException.userError(SrErrorCode.CANNOT_CHANGE_CLASS_OR_TYPE);
		}
	}
	
	private boolean isSpecimenClassOrTypeChanged(SpecimenRequirement existingSr, SpecimenRequirement sr) {
		return !existingSr.getSpecimenClass().equals(sr.getSpecimenClass()) || 
				!existingSr.getSpecimenType().equals(sr.getSpecimenType());
	}
	
	private void setAliquotCode(SpecimenRequirement parent, List<SpecimenRequirement> aliquots, String code) {
		Set<String> codes = new HashSet<String>();
		CollectionProtocolEvent cpe = parent.getCollectionProtocolEvent();
		for (SpecimenRequirement sr : cpe.getSpecimenRequirements()) {
			if (StringUtils.isNotBlank(sr.getCode())) {
				codes.add(sr.getCode());
			}
		}

		int count = 1;
		for (SpecimenRequirement sr : aliquots) {
			while (!codes.add(code + count)) {
				count++;
			}

			sr.setCode(code + count++);
		}
	}

	private CollectionProtocol getCollectionProtocol(String shortTitle) {
		return getCollectionProtocol(null, null, shortTitle);
	}

	private CollectionProtocol getCollectionProtocol(Long id, String title, String shortTitle) {
		CollectionProtocol cp = null;
		if (id != null) {
			cp = daoFactory.getCollectionProtocolDao().getById(id);
		} else if (StringUtils.isNotBlank(title)) {
			cp = daoFactory.getCollectionProtocolDao().getCollectionProtocol(title);
		} else if (StringUtils.isNoneBlank(shortTitle)) {
			cp = daoFactory.getCollectionProtocolDao().getCpByShortTitle(shortTitle);
		}

		if (cp == null) {
			throw OpenSpecimenException.userError(CpErrorCode.NOT_FOUND);
		}

		return cp;
	}

	private void mergeCprIntoCp(CollectionProtocolRegistration srcCpr, CollectionProtocol tgtCp) {
		//
		// Step 1: Get a matching CPR either by PPID or participant ID
		//
		CollectionProtocolRegistration tgtCpr = daoFactory.getCprDao().getCprByPpid(tgtCp.getId(), srcCpr.getPpid());
		if (tgtCpr == null) {
			tgtCpr = srcCpr.getParticipant().getCpr(tgtCp);
		}

		//
		// Step 2: Map all visits of source CP registrations to first event of target CP
		// Further mark all created specimens as unplanned
		//
		CollectionProtocolEvent firstCpe = tgtCp.firstEvent();
		for (Visit visit : srcCpr.getVisits()) {
			visit.setCpEvent(firstCpe);
			visit.getSpecimens().forEach(s -> s.setSpecimenRequirement(null));
		}

		//
		// Step 3: Attach registrations to target CP
		//
		if (tgtCpr == null) {
			//
			// case 1: No matching registration was found in target CP; therefore make source
			// registration as part of target CP
			//
			srcCpr.setCollectionProtocol(tgtCp);
		} else {
			//
			// case 2: Matching registration was found in target CP; therefore do following
			// 2.1 Move all visits of source CP registration to target CP registration
			// 2.2 Finally delete source CP registration
			//
			tgtCpr.addVisits(srcCpr.getVisits());
			srcCpr.getVisits().clear();
			srcCpr.delete();
		}
	}

	private void ensureMergeableCps(CollectionProtocol srcCp, CollectionProtocol tgtCp) {
		ArrayList<String> notSameLabels = new ArrayList<>();

		ensureBlankOrSame(srcCp.getPpidFormat(), tgtCp.getPpidFormat(), PPID_MSG, notSameLabels);
		ensureBlankOrSame(srcCp.getVisitNameFormat(), tgtCp.getVisitNameFormat(), VISIT_NAME_MSG, notSameLabels);
		ensureBlankOrSame(srcCp.getSpecimenLabelFormat(), tgtCp.getSpecimenLabelFormat(), SPECIMEN_LABEL_MSG, notSameLabels);
		ensureBlankOrSame(srcCp.getDerivativeLabelFormat(), tgtCp.getDerivativeLabelFormat(), DERIVATIVE_LABEL_MSG, notSameLabels);
		ensureBlankOrSame(srcCp.getAliquotLabelFormat(), tgtCp.getAliquotLabelFormat(), ALIQUOT_LABEL_MSG, notSameLabels);

		if (!notSameLabels.isEmpty()) {
			throw OpenSpecimenException.userError(
				CpErrorCode.CANNOT_MERGE_FMT_DIFFERS,
				srcCp.getShortTitle(),
				tgtCp.getShortTitle(),
				notSameLabels);
		}
	}

	private void ensureBlankOrSame(String srcLabelFmt, String tgtLabelFmt, String labelKey, List<String> notSameLabels) {
		if (!StringUtils.isBlank(tgtLabelFmt) && !tgtLabelFmt.equals(srcLabelFmt)) {
			notSameLabels.add(getMsg(labelKey));
		}
	}

	private boolean forceDeleteCps(final List<CollectionProtocol> cps, final String reason)
	throws Exception {
		final Authentication auth = AuthUtil.getAuth();

		Future<Boolean> result = taskExecutor.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				SecurityContextHolder.getContext().setAuthentication(auth);
				cps.forEach(cp -> forceDeleteCp(cp, reason));
				return true;
			}
		});

		boolean completed = false;
		try {
			completed = result.get(30, TimeUnit.SECONDS);
		} catch (TimeoutException ex) {
			completed = false;
		}

		return completed;
	}

	private void forceDeleteCp(CollectionProtocol cp, String reason) {
		while (deleteRegistrations(cp));
		deleteCp(cp, reason);
	}

	private boolean deleteCps(List<CollectionProtocol> cps, String reason) {
		cps.forEach(cp -> deleteCp(cp, reason));
		return true;
	}

	@PlusTransactional
	private boolean deleteCp(CollectionProtocol cp, String reason) {
		boolean success = false;
		String stackTrace = null;
		CollectionProtocol deletedCp = new CollectionProtocol();
		try {
			//
			// refresh cp, as it could have been fetched in another transaction
			// if in same transaction, then it will be obtained from session
			//
			cp = daoFactory.getCollectionProtocolDao().getById(cp.getId());

			removeContainerRestrictions(cp);
			addOrRemovePiCoordinatorRoles(cp, "DELETE", cp.getPrincipalInvestigator(), null, null, cp.getCoordinators());
			removeCpRoles(cp);
			BeanUtils.copyProperties(cp, deletedCp);

			cp.setOpComments(reason);
			cp.delete();

			DeleteLogUtil.getInstance().log(cp);
			success = true;
		} catch (Exception ex) {
			stackTrace = ExceptionUtils.getStackTrace(ex);

			if (ex instanceof OpenSpecimenException) {
				throw ex;
			} else {
				throw OpenSpecimenException.serverError(ex);
			}
		} finally {
			notifyUsersOnCpDelete(deletedCp, success, stackTrace);
		}

		return true;
	}
	
	@PlusTransactional
	private boolean deleteRegistrations(CollectionProtocol cp) {
		List<CollectionProtocolRegistration> cprs = daoFactory.getCprDao().getCprsByCpId(cp.getId(), 0, 10);
		cprs.forEach(cpr -> cpr.delete(false));
		return cprs.size() == 10;
	}

	private void removeContainerRestrictions(CollectionProtocol cp) {
		Set<StorageContainer> containers = cp.getStorageContainers();
		for (StorageContainer container : containers) {
			container.removeCpRestriction(cp);
		}
		
		cp.setStorageContainers(Collections.EMPTY_SET);
	}

	private void removeCpRoles(CollectionProtocol cp) {
		rbacSvc.removeCpRoles(cp.getId());
	}

	private void notifyUsersOnCpCreate(CollectionProtocol cp) {
		notifyUsersOnCpOp(cp, cp.getRepositories(), OP_CP_CREATED);
	}

	private void notifyUsersOnCpUpdate(CollectionProtocol cp, Collection<Site> addedSites, Collection<Site> removedSites) {
		notifyUsersOnCpOp(cp, removedSites, OP_CP_SITE_REMOVED);
		notifyUsersOnCpOp(cp, addedSites, OP_CP_SITE_ADDED);
	}

	private void notifyUsersOnCpDelete(CollectionProtocol cp, boolean success, String stackTrace) {
		if (success) {
			notifyUsersOnCpOp(cp, cp.getRepositories(), OP_CP_DELETED);
		} else {
			User currentUser = AuthUtil.getCurrentUser();
			String[] rcpts = {currentUser.getEmailAddress(), cp.getPrincipalInvestigator().getEmailAddress()};
			String[] subjParams = new String[] {cp.getShortTitle()};

			Map<String, Object> props = new HashMap<>();
			props.put("cp", cp);
			props.put("$subject", subjParams);
			props.put("user", currentUser);
			props.put("error", stackTrace);
			EmailUtil.getInstance().sendEmail(CP_DELETE_FAILED_EMAIL_TMPL, rcpts, null, props);
		}
	}

	private void notifyUsersOnCpOp(CollectionProtocol cp, Collection<Site> sites, int op) {
		Map<String, Object> emailProps = new HashMap<>();
		emailProps.put("$subject", new Object[] {cp.getShortTitle(), op});
		emailProps.put("cp", cp);
		emailProps.put("op", op);
		emailProps.put("currentUser", AuthUtil.getCurrentUser());
		emailProps.put("ccAdmin", false);

		if (op == OP_CP_CREATED || op == OP_CP_DELETED) {
			List<User> superAdmins = AccessCtrlMgr.getInstance().getSuperAdmins();
			notifyUsers(superAdmins, CP_OP_EMAIL_TMPL, emailProps, (op == OP_CP_CREATED) ? "CREATE" : "DELETE");
		}

		for (Site site : sites) {
			String siteName = site.getName();
			emailProps.put("siteName", siteName);
			emailProps.put("$subject", new Object[] {siteName, op, cp.getShortTitle()});
			notifyUsers(site.getCoordinators(), CP_SITE_UPDATED_EMAIL_TMPL, emailProps, "UPDATE");
		}
	}

	private void notifyUsers(Collection<User> users, String template, Map<String, Object> emailProps, String notifOp) {
		for (User rcpt : users) {
			emailProps.put("rcpt", rcpt);
			EmailUtil.getInstance().sendEmail(template, new String[] {rcpt.getEmailAddress()}, null, emailProps);
		}

		CollectionProtocol cp = (CollectionProtocol)emailProps.get("cp");
		Object[] subjParams = (Object[])emailProps.get("$subject");

		Notification notif = new Notification();
		notif.setEntityType(CollectionProtocol.getEntityName());
		notif.setEntityId(cp.getId());
		notif.setOperation(notifOp);
		notif.setCreatedBy(AuthUtil.getCurrentUser());
		notif.setCreationTime(Calendar.getInstance().getTime());
		notif.setMessage(MessageUtil.getInstance().getMessage(template + "_subj", subjParams));
		NotifUtil.getInstance().notify(notif, Collections.singletonMap("cp-overview", users));
	}

	private SubjectRoleOpNotif getNotifReq(CollectionProtocol cp, String role, List<User> notifUsers, User user, String cpOp, String roleOp) {
		SubjectRoleOpNotif notifReq = new SubjectRoleOpNotif();
		notifReq.setAdmins(notifUsers);
		notifReq.setAdminNotifMsg("cp_admin_notif_role_" + roleOp.toLowerCase());
		notifReq.setAdminNotifParams(new Object[] { user.getFirstName(), user.getLastName(), role, cp.getShortTitle(), user.getInstitute().getName() });
		notifReq.setUser(user);

		if (cpOp.equals("DELETE")) {
			notifReq.setSubjectNotifMsg("cp_delete_user_notif_role");
			notifReq.setSubjectNotifParams(new Object[] { cp.getShortTitle(), role });
		} else {
			notifReq.setSubjectNotifMsg("cp_user_notif_role_" + roleOp.toLowerCase());
			notifReq.setSubjectNotifParams(new Object[] { role, cp.getShortTitle(), user.getInstitute().getName() });
		}

		notifReq.setEndUserOp(cpOp);
		return notifReq;
	}

	private String getMsg(String code) {
		return MessageUtil.getInstance().getMessage(code);
	}

	private String getSopDocDir() {
		String defDir = ConfigUtil.getInstance().getDataDir() + File.separator + "cp-sop-documents";
		String dir = ConfigUtil.getInstance().getStrSetting(ConfigParams.MODULE, ConfigParams.CP_SOP_DOCS_DIR, defDir);
		new File(dir).mkdirs();
		return dir + File.separator;
	}

	private CpReportSettings getReportSetting(CpQueryCriteria crit) {
		CpReportSettings settings = null;
		if (crit.getId() != null) {
			settings = daoFactory.getCpReportSettingsDao().getByCp(crit.getId());
		} else if (StringUtils.isNotBlank(crit.getShortTitle())) {
			settings = daoFactory.getCpReportSettingsDao().getByCp(crit.getShortTitle());
		}

		return settings;
	}

	private ListConfig getSpecimenListConfig(Map<String, Object> listReq) {
		ListConfig cfg = getListConfig(listReq, "specimen-list-view", "Specimen");
		if (cfg == null) {
			return null;
		}

		Column id = new Column();
		id.setExpr("Specimen.id");
		id.setCaption("specimenId");
		cfg.setPrimaryColumn(id);

		Column type = new Column();
		type.setExpr("Specimen.type");
		type.setCaption("specimenType");

		Column specimenClass = new Column();
		specimenClass.setExpr("Specimen.class");
		specimenClass.setCaption("specimenClass");

		List<Column> hiddenColumns = new ArrayList<>();
		hiddenColumns.add(id);
		hiddenColumns.add(type);
		hiddenColumns.add(specimenClass);
		cfg.setHiddenColumns(hiddenColumns);

		Long cpId = (Long)listReq.get("cpId");
		List<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps(cpId);
		if (siteCps == null) {
			//
			// Admin; hence no additional restrictions
			//
			return cfg;
		}

		if (siteCps.isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		boolean useMrnSites = AccessCtrlMgr.getInstance().isAccessRestrictedBasedOnMrn();
		String restrictions = BiospecimenDaoHelper.getInstance().getSiteCpsCondAql(siteCps, useMrnSites);
		cfg.setRestriction(restrictions);
		cfg.setDistinct(true);
		return cfg;
	}

	private ListConfig getParticipantsListConfig(Map<String, Object> listReq) {
		ListConfig cfg = getListConfig(listReq, "participant-list-view", "Participant");
		if (cfg == null) {
			return null;
		}

		Column id = new Column();
		id.setExpr("Participant.id");
		id.setCaption("cprId");
		cfg.setPrimaryColumn(id);
		cfg.setHiddenColumns(Collections.singletonList(id));

		Long cpId = (Long)listReq.get("cpId");
		ParticipantReadAccess access = AccessCtrlMgr.getInstance().getParticipantReadAccess(cpId);
		if (access.admin) {
			return cfg;
		}

		if (access.siteCps == null || access.siteCps.isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		boolean useMrnSites = AccessCtrlMgr.getInstance().isAccessRestrictedBasedOnMrn();
		cfg.setRestriction(BiospecimenDaoHelper.getInstance().getSiteCpsCondAql(access.siteCps, useMrnSites));
		cfg.setDistinct(true);
		return cfg;
	}

	private ListConfig getListConfig(Map<String, Object> listReq, String listName, String drivingForm) {
		Long cpId = (Long) listReq.get("cpId");
		if (cpId == null) {
			cpId = (Long) listReq.get("objectId");
		}

		Workflow workflow = getWorkFlow(cpId, listName);
		if (workflow == null) {
			return null;
		}

		ListConfig listCfg = new ObjectMapper().convertValue(workflow.getData(), ListConfig.class);
		listCfg.setCpId(cpId);
		listCfg.setDrivingForm(drivingForm);
		setListLimit(listReq, listCfg);

		Boolean includeCount = (Boolean)listReq.get("includeCount");
		listCfg.setIncludeCount(includeCount == null ? false : includeCount);
		return listCfg;
	}

	private Workflow getWorkFlow(Long cpId, String name) {
		Workflow workflow = null;

		CpWorkflowConfig cfg = daoFactory.getCollectionProtocolDao().getCpWorkflows(cpId);
		if (cfg != null) {
			workflow = cfg.getWorkflows().get(name);
		}

		if (workflow == null) {
			workflow = getSysWorkflow(name);
		}

		return workflow;
	}

	private Workflow getSysWorkflow(String name) {
		return WorkflowUtil.getInstance().getSysWorkflow(name);
	}

	private void setListLimit(Map<String, Object> listReq, ListConfig cfg) {
		Integer startAt = (Integer)listReq.get("startAt");
		if (startAt == null) {
			startAt = 0;
		}

		Integer maxResults = (Integer)listReq.get("maxResults");
		if (maxResults == null) {
			maxResults = 100;
		}

		cfg.setStartAt(startAt);
		cfg.setMaxResults(maxResults);
	}

	private ConsentStatement getStatement(Long id, String code, String statement) {
		ConsentStatement stmt = null;
		Object key = null;

		if (id != null) {
			key = id;
			stmt = daoFactory.getConsentStatementDao().getById(id);
		} else if (StringUtils.isNotBlank(code)) {
			key = code;
			stmt = daoFactory.getConsentStatementDao().getByCode(code);
		} else if (StringUtils.isNotBlank(statement)) {
			key = statement;
			stmt = daoFactory.getConsentStatementDao().getByStatement(statement);
		}

		if (key == null) {
			throw OpenSpecimenException.userError(ConsentStatementErrorCode.CODE_REQUIRED);
		} else if (stmt == null) {
			throw OpenSpecimenException.userError(ConsentStatementErrorCode.NOT_FOUND, key);
		}

		return stmt;
	}

	private static final String PPID_MSG                     = "cp_ppid";

	private static final String VISIT_NAME_MSG               = "cp_visit_name";

	private static final String SPECIMEN_LABEL_MSG           = "cp_specimen_label";

	private static final String DERIVATIVE_LABEL_MSG         = "cp_derivative_label";

	private static final String ALIQUOT_LABEL_MSG            = "cp_aliquot_label";
	
	private static final String CP_OP_EMAIL_TMPL             = "cp_op";
	
	private static final String CP_DELETE_FAILED_EMAIL_TMPL  = "cp_delete_failed";

	private static final String CP_SITE_UPDATED_EMAIL_TMPL   = "cp_site_updated";

	private static final int OP_CP_SITE_ADDED = -1;

	private static final int OP_CP_SITE_REMOVED = 1;

	private static final int OP_CP_CREATED = 0;

	private static final int OP_CP_DELETED = 2;

	private static final String INSTITUTE_SITES_SQL =
			"select identifier from catissue_site where institute_id in (%s) and activity_status != 'Disabled'";
}
