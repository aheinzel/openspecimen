
package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.events.VisitSummary;
import com.krishagni.catissueplus.core.biospecimen.repository.VisitsDao;
import com.krishagni.catissueplus.core.biospecimen.repository.VisitsListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class VisitsDaoImpl extends AbstractDao<Visit> implements VisitsDao {
	
	@Override
	public Class<Visit> getType() {
		return Visit.class;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<VisitSummary> getVisits(VisitsListCriteria crit) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_VISITS_SUMMARY_BY_CPR_ID)
			.setLong("cprId", crit.cprId())
			.list();
		
		List<VisitSummary> visits = new ArrayList<>();
		Map<String, VisitSummary> visitsMap = new HashMap<>();

		Date regDate = null;
		int minEventPoint = 0;
		for (Object[] row : rows) {
			Long visitId = (Long)row[0];
			String eventStatus = (String)row[3];
			if (visitId == null && StringUtils.isNotBlank(eventStatus) && eventStatus.equals("Disabled")) {
				continue;
			}
			
			VisitSummary visit = new VisitSummary();
			visit.setId(visitId);
			visit.setEventId((Long)row[1]);
			visit.setName((String)row[2]);
			visit.setEventLabel((String)row[4]);
			visit.setEventPoint((Integer)row[5]);
			visit.setStatus((String)row[6]);
			visit.setVisitDate((Date)row[7]);
			regDate = (Date)row[8];
			visit.setMissedReason((String)row[9]);
			visit.setCpId((Long)row[10]);
			visits.add(visit);

			if (crit.includeStat()) {				
				visitsMap.put(getVisitKey(visit.getId(), visit.getEventId()), visit);
			}

			if (visit.getEventPoint() != null && visit.getEventPoint() < minEventPoint) {
				minEventPoint = visit.getEventPoint();
			}
		}

		Calendar cal = Calendar.getInstance();
		for (VisitSummary visit : visits) {
			if (visit.getEventPoint() == null) {
				continue;
			}

			cal.setTime(regDate);
			cal.add(Calendar.DAY_OF_YEAR, visit.getEventPoint() - minEventPoint);
			visit.setAnticipatedVisitDate(cal.getTime());
		}
						
		if (crit.includeStat() && !visitsMap.isEmpty()) {
			getVisitsCollectionStatus(crit.cprId(), visitsMap);
			getUtilizationStatus(visitsMap);
		}
	
		Collections.sort(visits);
		return visits;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Visit> getVisitsList(VisitsListCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(Visit.class, "visit");

		String startAlias = "cpr";
		if (crit.cpId() != null) {
			startAlias = "cpSite";
			query.createAlias("visit.registration", "cpr")
				.createAlias("cpr.collectionProtocol", "cp")
				.add(Restrictions.eq("cp.id", crit.cpId()));
		}

		boolean limitItems = true;
		if (CollectionUtils.isNotEmpty(crit.names())) {
			query.add(Restrictions.in("name", crit.names()));
			limitItems = false;
		}

		if (CollectionUtils.isNotEmpty(crit.siteCps())) {
			BiospecimenDaoHelper.getInstance().addSiteCpsCond(query, crit.siteCps(), crit.useMrnSites(), startAlias);
		}

		if (limitItems) {
			query.setFirstResult(crit.startAt()).setMaxResults(crit.maxResults());
		}

		return query.addOrder(Order.asc("id")).list();
	}

	@Override
	public Visit getByName(String name) {
		List<Visit> visits = getByName(Collections.singleton(name));
		return !visits.isEmpty() ? visits.iterator().next() : null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Visit> getByName(Collection<String> names) {
		return sessionFactory.getCurrentSession()
			.getNamedQuery(GET_VISIT_BY_NAME)
			.setParameterList("names", names)
			.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Visit> getByIds(Collection<Long> ids) {
		return sessionFactory.getCurrentSession()
			.getNamedQuery(GET_VISITS_BY_IDS)
			.setParameterList("ids", ids)
			.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Visit> getBySpr(String sprNumber) {
		return sessionFactory.getCurrentSession()
			.getNamedQuery(GET_VISIT_BY_SPR)
			.setString("sprNumber", sprNumber)
			.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> getCprVisitIds(String key, Object value) {
		List<Object[]> rows = getCurrentSession().createCriteria(Visit.class)
			.createAlias("registration", "cpr")
			.createAlias("cpr.collectionProtocol", "cp")
			.setProjection(
				Projections.projectionList()
					.add(Projections.property("id"))
					.add(Projections.property("cpr.id"))
					.add(Projections.property("cp.id")))
			.add(Restrictions.eq(key, value))
			.list();

		if (CollectionUtils.isEmpty(rows)) {
			return Collections.emptyMap();
		}

		Object[] row = rows.iterator().next();
		Map<String, Object> ids = new HashMap<>();
		ids.put("visitId", row[0]);
		ids.put("cprId", row[1]);
		ids.put("cpId", row[2]);
		return ids;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Visit getLatestVisit(Long cprId) {
		List<Visit> visits = sessionFactory.getCurrentSession()
			.getNamedQuery(GET_LATEST_VISIT_BY_CPR_ID)
			.setLong("cprId", cprId)
			.setMaxResults(1)
			.list();

		return visits.isEmpty() ? null :  visits.get(0);
	}


	private String getVisitKey(Long visitId, Long cpeId) {
		String key = "";
		if (visitId != null) {
			key += visitId;
		}
		
		key += "_" + cpeId;
		return key;
	}

	private void getVisitsCollectionStatus(Long cprId, Map<String, VisitSummary> visitsMap) {
		getPlannedCollectionStatus(cprId, visitsMap);
		getUnplannedCollectionStatus(cprId, visitsMap);
	}

	@SuppressWarnings("unchecked")
	private void getPlannedCollectionStatus(Long cprId, Map<String, VisitSummary> visitsMap) {
		Set<Long> eventIds = getNotNullIds(visitsMap, VisitSummary::getEventId);
		if (eventIds.isEmpty()) {
			return;
		}

		List<Object[]> rows = sessionFactory.getCurrentSession()
			.getNamedQuery(GET_VISITS_COLLECTION_STATUS)
			.setLong("cprId", cprId)
			.setParameterList("eventIds", eventIds)
			.list();
		
		for (Object[] row : rows) {
			Long scgId = (Long)row[0];
			Long eventId = (Long)row[1];
			
			VisitSummary visit = visitsMap.get(getVisitKey(scgId, eventId));
			visit.setAnticipatedSpecimens((Integer)row[2]);
			visit.setCollectedSpecimens((Integer)row[3]);
			visit.setUncollectedSpecimens((Integer)row[4]);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void getUnplannedCollectionStatus(Long cprId, Map<String, VisitSummary> visitsMap) {
		Set<Long> visitIds = getNotNullIds(visitsMap, VisitSummary::getId);
		if (visitIds.isEmpty()) {
			return;
		}

		List<Object[]> rows = sessionFactory.getCurrentSession()
			.getNamedQuery(GET_VISITS_UNPLANNED_SPECIMENS_STAT)
			.setLong("cprId", cprId)
			.setParameterList("visitIds", visitIds)
			.list();
		
		for (Object[] row : rows) {
			Long scgId = (Long)row[0];	
			Long eventId = (Long)row[1];
			
			VisitSummary visit = visitsMap.get(getVisitKey(scgId, eventId));
			visit.setUnplannedSpecimens((Integer)row[2]);
		}				
	}

	private void getUtilizationStatus(Map<String, VisitSummary> visitsMap) {
		Set<Long> visitIds = getNotNullIds(visitsMap, VisitSummary::getId);
		if (CollectionUtils.isEmpty(visitIds)) {
			return;
		}

		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_VISITS_SPMN_STORAGE_STAT)
			.setParameterList("visitIds", visitIds)
			.list();

		for (Object[] row : rows) {
			Long visitId = (Long) row[0];
			Long eventId = (Long) row[1];

			VisitSummary visit = visitsMap.get(getVisitKey(visitId, eventId));
			visit.setStoredSpecimens((Integer) row[2]);
			visit.setNotStoredSpecimens((Integer) row[3]);
			visit.setDistributedSpecimens((Integer) row[4]);
		}
	}

	private Set<Long> getNotNullIds(Map<String, VisitSummary> visitsMap, Function<VisitSummary, Long> idMapper) {
		return visitsMap.values().stream()
			.map(idMapper)
			.filter(id -> id != null)
			.collect(Collectors.toSet());
	}
		
	private static final String FQN = Visit.class.getName();
	
	private static final String GET_VISITS_SUMMARY_BY_CPR_ID = FQN + ".getVisitsSummaryByCprId";
	
	private static final String GET_VISITS_COLLECTION_STATUS = FQN + ".getVisitsCollectionStatus";
	
	private static final String GET_VISITS_UNPLANNED_SPECIMENS_STAT = FQN + ".getVisitsUnplannedSpecimenCount";

	private static final String GET_VISITS_SPMN_STORAGE_STAT = FQN + ".getVisitsSpmnStorageStat";

	private static final String GET_VISITS_BY_IDS = FQN + ".getVisitsByIds";

	private static final String GET_VISIT_BY_NAME = FQN + ".getVisitByName";

	private static final String GET_VISIT_BY_SPR = FQN + ".getVisitBySpr";

	private static final String GET_LATEST_VISIT_BY_CPR_ID = FQN + ".getLatestVisitByCprId";
}

