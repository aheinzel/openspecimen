package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.Shipment;
import com.krishagni.catissueplus.core.administrative.domain.Shipment.Status;
import com.krishagni.catissueplus.core.administrative.domain.factory.ShipmentErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.ShipmentFactory;
import com.krishagni.catissueplus.core.administrative.events.ShipmentDetail;
import com.krishagni.catissueplus.core.administrative.events.ShipmentListCriteria;
import com.krishagni.catissueplus.core.administrative.repository.ShipmentDao;
import com.krishagni.catissueplus.core.administrative.services.ShipmentService;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.EmailService;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.rbac.common.errors.RbacErrorCode;

public class ShipmentServiceImpl implements ShipmentService {
	private static final String SHIPMENT_SHIPPED_EMAIL_TMPL = "shipment_shipped";
	
	private static final String SHIPMENT_RECEIVED_EMAIL_TMPL = "shipment_received";
	
	private DaoFactory daoFactory;
	
	private ShipmentFactory shipmentFactory;
	
	private EmailService emailService;
	
	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}
	
	public void setShipmentFactory(ShipmentFactory shipmentFactory) {
		this.shipmentFactory = shipmentFactory;
	}

	public void setEmailService(EmailService emailService) {
		this.emailService = emailService;
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<ShipmentDetail>> getShipments(RequestEvent<ShipmentListCriteria> req) {
		try {
			Set<Long> siteIds = AccessCtrlMgr.getInstance().getReadAccessShipmentSites();
			if (siteIds != null && siteIds.isEmpty()) {
				return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
			}
			
			ShipmentListCriteria listCrit = req.getPayload();
			if (siteIds != null) {
				listCrit.siteIds(siteIds);
			}
			
			return ResponseEvent.response(ShipmentDetail.from(getShipmentDao().getShipments(listCrit)));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<ShipmentDetail> getShipment(RequestEvent<Long> req) {
		try {
			Long shipmentId = req.getPayload();
			Shipment shipment = getShipmentDao().getById(shipmentId);
			if (shipment == null) {
				return ResponseEvent.userError(ShipmentErrorCode.NOT_FOUND);
			}
			
			AccessCtrlMgr.getInstance().ensureReadShipmentRights(shipment);
			return ResponseEvent.response(ShipmentDetail.from(shipment));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<ShipmentDetail> createShipment(RequestEvent<ShipmentDetail> req) {
		try {
			AccessCtrlMgr.getInstance().ensureCreateShipmentRights();
			ShipmentDetail detail = req.getPayload();
			Shipment shipment = shipmentFactory.createShipment(detail, detail.getStatus() == null ? Status.PENDING : null);
			
			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			ensureUniqueConstraint(null, shipment, ose);
			ensureValidSpecimens(shipment, ose);
			ose.checkAndThrow();
			
			if (shipment.isShipped()) {
				shipment.ship();
			}
			
			getShipmentDao().saveOrUpdate(shipment);
			sendEmailNotifications(shipment, null);
			return ResponseEvent.response(ShipmentDetail.from(shipment));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<ShipmentDetail> updateShipment(RequestEvent<ShipmentDetail> req) {
		try {
			ShipmentDetail detail = req.getPayload();
			Shipment existing = getShipmentDao().getById(detail.getId());
			if (existing == null) {
				return ResponseEvent.userError(ShipmentErrorCode.NOT_FOUND);
			}
			
			AccessCtrlMgr.getInstance().ensureUpdateShipmentRights(existing);
			Shipment newShipment = shipmentFactory.createShipment(detail, null);
			if (newShipment.isReceived()) {
				AccessCtrlMgr.getInstance().ensureReceiveShipmentRights(newShipment);
			} else {
				AccessCtrlMgr.getInstance().ensureUpdateShipmentRights(newShipment);
			}
			
			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			ensureUniqueConstraint(existing, newShipment, ose);
			ensureValidSpecimens(newShipment, ose);
			ose.checkAndThrow();
			
			Status oldStatus = existing.getStatus();
			existing.update(newShipment);
			getShipmentDao().saveOrUpdate(existing, true);
			sendEmailNotifications(existing, oldStatus);
			return ResponseEvent.response(ShipmentDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private void ensureUniqueConstraint(Shipment existing, Shipment newShipment, OpenSpecimenException ose) {
		if (existing == null || !newShipment.getName().equals(existing.getName())) {
			Shipment shipment = getShipmentDao().getShipmentByName(newShipment.getName());
			if (shipment != null) {
				ose.addError(ShipmentErrorCode.DUP_NAME, newShipment.getName());
			}
		}
	}
	
	private void ensureValidSpecimens(Shipment shipment, OpenSpecimenException ose) {
		List<String> specimenLabels = Utility.<List<String>>collect(shipment.getShipmentItems(), "specimen.label");
		
		List<Pair<Long, Long>> siteCpPairs = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
		if (siteCpPairs != null && siteCpPairs.isEmpty()) {
			ose.addError(ShipmentErrorCode.INVALID_SPECIMENS);
			return;
		}
		
		SpecimenListCriteria crit = new SpecimenListCriteria()
			.labels(specimenLabels)
			.siteCps(siteCpPairs);
		
		List<Specimen> specimens = daoFactory.getSpecimenDao().getSpecimens(crit);
		if (specimens.size() != specimenLabels.size()) {
			ose.addError(ShipmentErrorCode.INVALID_SPECIMENS);
			return;
		}
		
		List<Specimen> closedSpecimens = new ArrayList<Specimen>();
		List<Specimen> unavailableSpecimens = new ArrayList<Specimen>();
		for (Specimen specimen : specimens) {
			if (specimen.isClosed()) {
				closedSpecimens.add(specimen);
			} else if (!specimen.getIsAvailable()) {
				unavailableSpecimens.add(specimen);
			}
		}
		
		if (CollectionUtils.isNotEmpty(closedSpecimens)) {
			List<String> labels = Utility.<List<String>>collect(closedSpecimens, "label");
			ose.addError(ShipmentErrorCode.CLOSED_SPECIMENS, StringUtils.join(labels, ','));
		}
		
		if (CollectionUtils.isNotEmpty(unavailableSpecimens)) {
			List<String> labels = Utility.<List<String>>collect(unavailableSpecimens, "label");
			ose.addError(ShipmentErrorCode.UNAVAILABLE_SPECIMENS, StringUtils.join(labels, ','));
		}
		
		Set<Long> specimenIds = Utility.<Set<Long>>collect(specimens, "id", true);
		Map<String, Set<Long>> specimenSiteIdsMap = daoFactory.getSpecimenDao().getSpecimenSites(specimenIds);
		Set<Long> shipmentAllowedIds = AccessCtrlMgr.getInstance().getCreateUpdateAccessShipmentSites();
		Long shipmentSiteId = shipment.getSite().getId();
		for (Map.Entry<String, Set<Long>> specimenSiteId : specimenSiteIdsMap.entrySet()) {
			if (!specimenSiteId.getValue().contains(shipmentSiteId)) {
				ose.addError(ShipmentErrorCode.SPECIMEN_NOT_BELONG_SITE);
			}
			
			if (shipmentAllowedIds != null &&
					CollectionUtils.intersection(specimenSiteId.getValue(), shipmentAllowedIds).isEmpty()) {
				ose.addError(ShipmentErrorCode.INVALID_SPECIMENS);
			}
		}
		
		if (shipment.isReceived()) {
			return;
		}
		
		List<Specimen> shippedSpecimens = getShipmentDao().getShippedSpecimensByLabels(specimenLabels);
		if (CollectionUtils.isNotEmpty(shippedSpecimens)) {
			List<String> labels = Utility.<List<String>>collect(shippedSpecimens, "label");
			ose.addError(ShipmentErrorCode.SPECIMEN_ALREADY_SHIPPED, StringUtils.join(labels, ','));
		}
	}
	
	private void sendEmailNotifications(Shipment shipment, Status oldStatus) {
		if ((oldStatus == null || oldStatus == Status.PENDING) && shipment.isShipped()) {
			sendShipmentShippedEmail(shipment);
		} else if (oldStatus == Status.SHIPPED && shipment.isReceived()) {
			sendShipmentReceivedEmail(shipment);
		}
	}
	
	private void sendShipmentShippedEmail(Shipment shipment) {
		Set<String> emailIds = Utility.<Set<String>>collect(shipment.getSite().getCoordinators(), "emailAddress", true);
		emailIds.add(shipment.getSender().getEmailAddress());
		String[] subjectParams = {shipment.getName()};
		
		Map<String, Object> props = new HashMap<String, Object>();
		props.put("$subject", subjectParams);
		props.put("shipment", shipment);
		emailService.sendEmail(SHIPMENT_SHIPPED_EMAIL_TMPL, emailIds.toArray(new String[0]), props);
	}
	
	private void sendShipmentReceivedEmail(Shipment shipment) {
		String[] emailIds = new String[] {shipment.getSender().getEmailAddress()};
 		String[] subjectParams = {shipment.getName()};
		
		Map<String, Object> props = new HashMap<String, Object>();
		props.put("$subject", subjectParams);
		props.put("shipment", shipment);
		emailService.sendEmail(SHIPMENT_RECEIVED_EMAIL_TMPL, emailIds, props);
	}
	
	private ShipmentDao getShipmentDao() {
		return daoFactory.getShipmentDao();
	}
}
