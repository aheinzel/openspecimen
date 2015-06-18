
package com.krishagni.catissueplus.core.init;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.repository.UserDao;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.QueryFolder;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;
import com.krishagni.catissueplus.core.de.repository.DaoFactory;

public class ImportDefaultQueries implements InitializingBean {

	private PlatformTransactionManager txnMgr;

	private UserDao userDao;

	private DaoFactory daoFactory;

	private User sysUser;

	public void setTxnMgr(PlatformTransactionManager txnMgr) {
		this.txnMgr = txnMgr;
	}

	public void setUserDao(UserDao userDao) {
		this.userDao = userDao;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		TransactionTemplate txnTmpl = new TransactionTemplate(txnMgr);
		txnTmpl.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRED);
		txnTmpl.execute(new TransactionCallback<Void>() {

			@Override
			public Void doInTransaction(TransactionStatus status) {
				try {
					importQueries();
					return null;
				} catch (Exception e) {
					status.setRollbackOnly();
					throw new RuntimeException(e);
				}
			}
		});
	}

	private void importQueries() throws Exception {
		sysUser = userDao.getSystemUser();
		Resource[] resources = new PathMatchingResourcePatternResolver().getResources(QUERIES_DIRECTORY + "/**");

		Set<SavedQuery> queries = new HashSet<SavedQuery>();
		for (Resource resource : resources) {
			String filename = QUERIES_DIRECTORY + File.separator + resource.getFilename();
			System.out.println("Importing query from file: " + filename);
			String newDigest = Utility.getResourceDigest(filename);
			byte[] content = IOUtils.toByteArray(resource.getURI());
			Map<String, Object> result = daoFactory.getSavedQueryDao().getChangelogDetails(filename);
			
			if (result == null) {
				SavedQuery query = insertQuery(filename, content, newDigest);
				if(query != null){
					queries.add(query);	
				}
			} else {
				Long queryId = ((Number)result.get("queryId")).longValue();
				String existingDigest = (String)result.get("md5Digest");

				if (existingDigest != null && existingDigest.equals(newDigest)) {
					System.out.println("No change found in file " + filename + " since last import");
					continue;
				}
				updateQuery(queryId, filename, content, newDigest);
			}
		}
		shareDefaultQueries(queries);
	}
	
	private SavedQuery insertQuery(String filename, byte[] queryContent, String md5) {
		SavedQuery savedQuery = new SavedQuery();
		try {
			savedQuery.setQueryDefJson(new String(queryContent));
			savedQuery.setCreatedBy(sysUser);
			savedQuery.setLastUpdated(new Date());
			savedQuery.setLastUpdatedBy(sysUser);
			daoFactory.getSavedQueryDao().saveOrUpdate(savedQuery);
			if (savedQuery.getId() == null) {
				System.out.println("Error saving query definition from file: " + filename);
				return null;
			}
			insertChangeLog(filename, md5, "INSERTED", savedQuery.getId());
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error saving query definition from file: " + filename);
		}
		return savedQuery;
	}

	private void updateQuery(Long queryId, String filename, byte[] queryContent, String md5) {
		try {
			SavedQuery savedQuery = new SavedQuery();
			savedQuery.setQueryDefJson(new String(queryContent));
			savedQuery.setId(queryId);
			daoFactory.getSavedQueryDao().saveOrUpdate(savedQuery);
			insertChangeLog(filename, md5, "UPDATED", queryId);
		} catch (Exception e) {
			System.out.println("Error updating query " + queryId + " using definition from file: " + filename);
		}
	}

	private void insertChangeLog(String filename, String md5Digest, String status, Long id) {
		daoFactory.getSavedQueryDao().insertFormChangeLog(filename, md5Digest, status, id);
	}

	private void shareDefaultQueries(Set<SavedQuery> queries) {
		QueryFolder folder = daoFactory.getQueryFolderDao().getByName(DEFAULT_QUERIES);
		if (folder == null) {
			folder = new QueryFolder();
		}
		folder.getSavedQueries().addAll(queries);
		folder.setSharedWithAll(true);
		folder.setName(DEFAULT_QUERIES);
		daoFactory.getQueryFolderDao().saveOrUpdate(folder);
	}
	
	private static final String DEFAULT_QUERIES = "Default Queries";
	
	private static final String QUERIES_DIRECTORY = File.separator + "aq-queries";

}
