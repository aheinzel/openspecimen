package edu.wustl.common.util.dbManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import net.sf.hibernate.cfg.Configuration;
import net.sf.hibernate.metadata.ClassMetadata;
import edu.wustl.catissuecore.domain.Department;
import edu.wustl.catissuecore.util.global.Variables;
import edu.wustl.common.util.logger.Logger;

/**
 * <p>Title: DBUtil Class>
 * <p>Description:  Utility class provides database specific utilities methods </p>
 * Copyright:    Copyright (c) year
 * Company: Washington University, School of Medicine, St. Louis.
 * @author Kapil Kaveeshwar
 * @version 1.00
 */
public class DBUtil
{
	//A factory for DB Session which provides the Connection for client. 
	private static  SessionFactory m_sessionFactory;

	//ThreadLocal to hold the Session for the current executing thread. 
	private static final ThreadLocal session = new ThreadLocal();
	
	//Initialize the session Factory in the Static block.
	static 
	{
		try
		{
			File file = new File(Variables.catissueHome+System.getProperty("file.separator")+"hibernate.properties");
			Logger.out.info("File "+file);
			BufferedInputStream stram = new BufferedInputStream(new FileInputStream(file));
			Properties p = new Properties();
			p.load(stram);
			
			stram.close();
			Configuration cfg = new Configuration();
			cfg.setProperties(p);
			m_sessionFactory = cfg.configure().buildSessionFactory();
		}
		catch(Exception ex)
		{
		    Logger.out.debug("Exception: "+ex.getMessage(),ex);
			throw new RuntimeException(ex.getMessage());
		}
	}

	/**
	 * Follows the singleton pattern and returns only current opened session.
	 * @return Returns the current db session.  
	 * */
	public static Session currentSession() throws HibernateException
	{
		Session s = (Session) session.get();
		
		//Open a new Session, if this Thread has none yet
		if (s == null)
		{
			s = m_sessionFactory.openSession();
			session.set(s);
		}
		return s;
	}

	/**
	 * Close the currently opened session.
	 * */
	public static void closeSession() throws HibernateException
	{
		Session s = (Session) session.get(); 
		session.set(null);
		if (s != null)
			s.close();
	}
}