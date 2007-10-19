					   /**
 * 
 */
package edu.wustl.catissuecore.annotations.xmi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.jmi.model.ModelPackage;
import javax.jmi.model.MofPackage;
import javax.jmi.xmi.XmiReader;

import org.netbeans.api.mdr.MDRManager;
import org.netbeans.api.mdr.MDRepository;
import org.omg.uml.UmlPackage;
import org.openide.util.Lookup;

import edu.common.dynamicextensions.bizlogic.BizLogicFactory;
import edu.common.dynamicextensions.domain.AbstractMetadata;
import edu.common.dynamicextensions.domain.integration.EntityMap;
import edu.common.dynamicextensions.domain.integration.EntityMapCondition;
import edu.common.dynamicextensions.domain.integration.FormContext;
import edu.common.dynamicextensions.domaininterface.EntityInterface;
import edu.common.dynamicextensions.domaininterface.userinterface.ContainerInterface;
import edu.common.dynamicextensions.exception.DynamicExtensionsApplicationException;
import edu.common.dynamicextensions.exception.DynamicExtensionsSystemException;
import edu.common.dynamicextensions.xmi.XMIUtilities;
import edu.common.dynamicextensions.xmi.importer.XMIImportProcessor;
import edu.wustl.catissuecore.action.annotations.AnnotationConstants;
import edu.wustl.catissuecore.bizlogic.AnnotationBizLogic;
import edu.wustl.catissuecore.bizlogic.AnnotationUtil;
import edu.wustl.catissuecore.domain.CollectionProtocol;
import edu.wustl.catissuecore.util.global.Constants;
import edu.wustl.common.bizlogic.DefaultBizLogic;
import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.security.exceptions.UserNotAuthorizedException;
import edu.wustl.common.util.dbManager.DAOException;

/**
 * @author ashish_gupta
 *
 */
public class ImportXmi
{
//	 name of a UML extent (instance of UML metamodel) that the UML models will be loaded into
	private static final String UML_INSTANCE = "UMLInstance";
	// name of a MOF extent that will contain definition of UML metamodel
	private static final String UML_MM = "UML";

	// repository
	private static MDRepository rep;
	// UML extent
	private static UmlPackage uml;

	// XMI reader
	private static XmiReader reader;
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		FileInputStream in  = null;
		try
		{
			if(args.length == 0)
			{
				throw new Exception("Please Specify the file name to be imported");
			}
			if(args.length < 2)
			{
				throw new Exception("Please Specify the hook entity name");
			}
			if(args.length < 3)
			{
				throw new Exception("Please Specify the main container csv file name");
			}
			
			//Ist parameter is fileName
	//		Fully qualified Name of the xmi file to be imported
			String fileName = args[0]; 
			String hookEntity = args[1];

			
				//"C://Documents and Settings//ashish_gupta//Desktop//XMLs//caTissueCore_1.4_Edited.xmi";	
			
			fileName = fileName.replaceAll("\\\\", "//");		
			System.out.println("Filename = " +fileName);
			System.out.println("Hook Entity = " +hookEntity);
			String packageName = "";			
			String conditionRecordObjectCsvFileName = "";
			String pathCsvFileName = "";
							
			int beginIndex = fileName.lastIndexOf("//");
			int endIndex = fileName.lastIndexOf(".");
			String domainModelName = fileName.substring(beginIndex+2, endIndex);
			
			System.out.println("Name of the file = " +domainModelName);
			
			// get the default repository
			rep = MDRManager.getDefault().getDefaultRepository();
			// create an XMIReader
			reader = (XmiReader) Lookup.getDefault().lookup(XmiReader.class);
			
			init();
	
			in = new FileInputStream(fileName);
	
			// start a read-only transaction
			rep.beginTrans(true);
			
			// read the document
			reader.read(in, null, uml);
			
			if(args.length > 2)
			{
				pathCsvFileName = args[2];
			}
			if(args.length > 3)
			{
				conditionRecordObjectCsvFileName = args[3];
			}
			if(args.length > 4)
			{
				packageName = args[4];
			}
			List<Long> conditionObjectIds = new ArrayList<Long>();
			if(!conditionRecordObjectCsvFileName.equals(""))
			{
				List<String> conditionObjectNames = readFile(conditionRecordObjectCsvFileName);				
				for(String conditionObjName :conditionObjectNames)
				{
					System.out.println("conditionObjName = " + conditionObjName);
					Long cpId =(Long) getObjectIdentifier(conditionObjName,CollectionProtocol.class.getName(),Constants.TITLE);
					if(cpId == null)
					{
						throw new DynamicExtensionsSystemException("Specified Collection Protocol does not exist.");
					}
					conditionObjectIds.add(cpId);
				}
			}
			
			DefaultBizLogic defaultBizLogic = BizLogicFactory.getDefaultBizLogic();
			List staticEntityList = defaultBizLogic.retrieve(AbstractMetadata.class.getName(), Constants.NAME, hookEntity);			
			if(staticEntityList == null || staticEntityList.size() == 0)
			{
				throw new DynamicExtensionsSystemException("Please enter correct Hook Entity name.");
			}
			EntityInterface staticEntity = (EntityInterface) staticEntityList.get(0);
			
			
			XMIImportProcessor xmiImportProcessor = new XMIImportProcessor();
			Map<String, List<ContainerInterface>> entityNameVsContainers = xmiImportProcessor.processXmi(uml, domainModelName,packageName);
			
			boolean isEditedXmi = xmiImportProcessor.isEditedXmi;
			System.out.println("Package name = " +packageName);
			System.out.println("isEditedXmi = "+isEditedXmi);
			System.out.println("Forms have been created !!!!");
			System.out.println("Associating with hook entity.");
			
			List<ContainerInterface> mainContainerList = getMainContainerList(pathCsvFileName,entityNameVsContainers);
			//Integrating with hook entity
			associateHookEntity(mainContainerList,conditionObjectIds,staticEntity,isEditedXmi);
			System.out.println("--------------- Done ------------");
		
		}
		catch (Exception e)
		{
			System.out.println("Fatal error reading XMI.");
			e.printStackTrace();
		}
		finally
		{
			// release the transaction
			rep.endTrans();
			MDRManager.getDefault().shutdownAll();
			try
			{
				in.close();
			}
			catch(IOException io)
			{
				System.out.println("Error. Specified file does not exist.");
			}
			XMIUtilities.cleanUpRepository();
		
		}
	}
	/**
	 * @param path
	 * @param entityNameVsContainers
	 * @return
	 * @throws IOException
	 */
	private static List<ContainerInterface> getMainContainerList(String path,Map<String, List<ContainerInterface>> entityNameVsContainers) throws IOException
	{
		 List<String> containerNames = readFile(path);
		List<ContainerInterface> mainContainerList = getContainerObjectList(containerNames,entityNameVsContainers);
		return mainContainerList;
	}
	/**
	 * @param path
	 * @return
	 * @throws IOException
	 */
	private static  List<String> readFile(String path) throws IOException
	{
		 List<String> containerNames = new ArrayList<String>();
		File file = new File(path);
	 
		BufferedReader bufRdr  = new BufferedReader(new FileReader(file));
		String line = null;
		 
		//read each line of text file
		while((line = bufRdr.readLine()) != null)
		{	
			StringTokenizer st = new StringTokenizer(line,",");
			while (st.hasMoreTokens())
			{
				//get next token and store it in the array
				containerNames.add(st.nextToken());
			}		
		}
		return containerNames;
	}
	/**
	 * @param containerNames
	 * @param entityNameVsContainers
	 * @return
	 */
	private static List<ContainerInterface> getContainerObjectList( List<String> containerNames,Map<String, List<ContainerInterface>> entityNameVsContainers)
	{
		List<ContainerInterface> mainContainerList = new ArrayList<ContainerInterface>();
			
		for(String name : containerNames)
		{
			Object containerList = entityNameVsContainers.get(name);
			if(containerList != null)
			{
				mainContainerList.add(((ContainerInterface)((List)containerList).get(0)));				
			}						
		}
		return mainContainerList;
	}

	private static void init() throws Exception
	{
		uml = (UmlPackage) rep.getExtent(UML_INSTANCE);

		if (uml == null)
		{
			// UML extent does not exist -> create it (note that in case one want's to instantiate
			// a metamodel other than MOF, they need to provide the second parameter of the createExtent
			// method which indicates the metamodel package that should be instantiated)
			uml = (UmlPackage) rep.createExtent(UML_INSTANCE, getUmlPackage());			
		}
	}

	/** Finds "UML" package -> this is the topmost package of UML metamodel - that's the
	 * package that needs to be instantiated in order to create a UML extent
	 */
	private static MofPackage getUmlPackage() throws Exception
	{
		// get the MOF extent containing definition of UML metamodel
		ModelPackage umlMM = (ModelPackage) rep.getExtent(UML_MM);
		if (umlMM == null)
		{
			// it is not present -> create it
			umlMM = (ModelPackage) rep.createExtent(UML_MM);
		}
		// find package named "UML" in this extent
		MofPackage result = getUmlPackage(umlMM);		
		if (result == null)
		{
			// it cannot be found -> UML metamodel is not loaded -> load it from XMI
			reader.read(UmlPackage.class.getResource("resources/01-02-15_Diff.xml").toString(),
					umlMM);
			// try to find the "UML" package again
			result = getUmlPackage(umlMM);			
		}
		return result;
	}

	/** Finds "UML" package in a given extent
	 * @param umlMM MOF extent that should be searched for "UML" package.
	 */
	private static MofPackage getUmlPackage(ModelPackage umlMM)
	{
		// iterate through all instances of package
		System.out.println("Here");
		for (Iterator it = umlMM.getMofPackage().refAllOfClass().iterator(); it.hasNext();)
		{
			MofPackage pkg = (MofPackage) it.next();
			System.out.println("\n\nName = " + pkg.getName());			
			// is the package topmost and is it named "UML"?
			if (pkg.getContainer() == null && "UML".equals(pkg.getName()))
			{				
				// yes -> return it
				return pkg;
			}
		}
		// a topmost package named "UML" could not be found
		return null;
	}
	
	/**
	 * @throws DAOException 
	 * @throws DynamicExtensionsSystemException 
	 * @throws UserNotAuthorizedException 
	 * @throws BizLogicException 
	 * @throws DynamicExtensionsApplicationException 
	 * 
	 */
	private static void associateHookEntity(List<ContainerInterface> mainContainerList,List<Long> conditionObjectIds,EntityInterface staticEntity,boolean isEditedXmi) throws DAOException, DynamicExtensionsSystemException, BizLogicException, UserNotAuthorizedException, DynamicExtensionsApplicationException
	{		
		Object typeId = getObjectIdentifier("edu.wustl.catissuecore.domain.CollectionProtocol",AbstractMetadata.class.getName(),Constants.NAME);
		DefaultBizLogic defaultBizLogic = BizLogicFactory.getDefaultBizLogic();		
		//Set<String> keySet = entityNameVsContainers.keySet();
//		for(String key : keySet)
		for(ContainerInterface container: mainContainerList)
		{
//			List<ContainerInterface> containerList = entityNameVsContainers.get(key);
//			ContainerInterface container = containerList.get(0);
						
			if(isEditedXmi)
			{//Retrieve entity map
				List<EntityMap> entityMapList = defaultBizLogic.retrieve(EntityMap.class.getName(), "containerId", container.getId());
				if(entityMapList != null && entityMapList.size() > 0)
				{
					EntityMap entityMap = entityMapList.get(0);
					if(conditionObjectIds != null)
					{
						editConditions(entityMap,conditionObjectIds,typeId);
					}
					AnnotationBizLogic annotation = new AnnotationBizLogic();
					annotation.updateEntityMap(entityMap);
				}
				else
				{//Create new Entity Map
					createIntegrationObjects(container,staticEntity,conditionObjectIds,typeId);
				}
			}
			else
			{//Create new Entity Map
				createIntegrationObjects(container,staticEntity,conditionObjectIds,typeId);
			}				
		}
	}
			/**
	 * @param entityMap
	 * @param clinicalStudyId
	 * @param typeId
	 * @throws DynamicExtensionsSystemException
	 * @throws DAOException
	 */
	private static void editConditions(EntityMap entityMap,List<Long> conditionObjectIds,Object typeId) throws DynamicExtensionsSystemException, DAOException
	{
		Collection<FormContext> formContextColl = entityMap.getFormContextCollection();
		for(FormContext formContext : formContextColl)
		{
//			Collection<EntityMapCondition> entityMapCondColl = formContext.getEntityMapConditionCollection();
//			int temp = 0;
//			for(EntityMapCondition condition : entityMapCondColl)
//			{
//				if(condition.getStaticRecordId().compareTo(collectionProtocolId) == 0)
//				{
//					temp++;
//					break;								
//				}
//			}
//			if(temp == 0)
//			{
//				EntityMapCondition entityMapCondition = getEntityMapCondition(formContext,collectionProtocolId,typeId);
//				entityMapCondColl.add(entityMapCondition);
//			}
			formContext.setEntityMapConditionCollection(getEntityMapCondition(formContext,conditionObjectIds,typeId));
		}
	}
	/**
	 * @param container
	 * @param staticEntity
	 * @param clinicalStudyId
	 * @param typeId
	 * @throws DynamicExtensionsSystemException
	 * @throws DAOException
	 * @throws BizLogicException
	 * @throws UserNotAuthorizedException
	 * @throws DynamicExtensionsApplicationException
	 */
	private static void createIntegrationObjects(ContainerInterface container,EntityInterface staticEntity,List<Long> conditionObjectIds,Object typeId) throws DynamicExtensionsSystemException, DAOException, BizLogicException, UserNotAuthorizedException, DynamicExtensionsApplicationException
	{
		EntityMap entityMap = getEntityMap(container,staticEntity.getId());
					
		Collection<FormContext> formContextColl = getFormContext(entityMap,conditionObjectIds,typeId);
					
		entityMap.setFormContextCollection(formContextColl);
		AnnotationBizLogic annotation = new AnnotationBizLogic();
		annotation.insert(entityMap, Constants.HIBERNATE_DAO);
		AnnotationUtil.addAssociation(staticEntity.getId(), container.getId(), true);
		
	}
	/**
	 * @param entityMap
	 * @param collectionProtocolName
	 * @param typeId
	 * @return
	 * @throws DynamicExtensionsSystemException
	 * @throws DAOException
	 */
	private static Collection<FormContext> getFormContext(EntityMap entityMap,List<Long> conditionObjectIds,Object typeId) throws DynamicExtensionsSystemException, DAOException
	{
		Collection<FormContext> formContextColl = new HashSet<FormContext>();
		FormContext formContext = new FormContext();
		formContext.setEntityMap(entityMap);
		
		Collection<EntityMapCondition> entityMapConditionColl = new HashSet<EntityMapCondition>();
		 if(conditionObjectIds != null)
		{
			entityMapConditionColl = getEntityMapCondition(formContext,conditionObjectIds,typeId);
		}
		
		formContext.setEntityMapConditionCollection(entityMapConditionColl);
					
		formContextColl.add(formContext);
		
		return formContextColl;
	}
	/**
	 * @param formContext
	 * @param collectionProtocolName
	 * @param typeId
	 * @return
	 * @throws DynamicExtensionsSystemException
	 * @throws DAOException
	 */
	private static Collection<EntityMapCondition> getEntityMapCondition(FormContext formContext,List<Long> conditionObjectIds,Object typeId) throws DynamicExtensionsSystemException, DAOException
	{	
		Collection<EntityMapCondition> entityMapCondColl = new HashSet<EntityMapCondition>();
		for(Long cpId : conditionObjectIds)
		{
			EntityMapCondition entityMapCondition = new EntityMapCondition();		
			entityMapCondition.setStaticRecordId((cpId));
						
			entityMapCondition.setTypeId(((Long)typeId));
			entityMapCondition.setFormContext(formContext);	
			entityMapCondColl.add(entityMapCondition);
		}
		return entityMapCondColl;
	}
	/**
	 * @param container
	 * @param hookEntityName
	 * @return
	 * @throws DAOException
	 * @throws DynamicExtensionsSystemException
	 */
	private static EntityMap getEntityMap(ContainerInterface container,Object staticEntityId) throws DAOException, DynamicExtensionsSystemException
	{
		EntityMap entityMap = new EntityMap();
		entityMap.setContainerId(container.getId());
		entityMap.setCreatedBy("");
		entityMap.setCreatedDate(new Date());
		entityMap.setLinkStatus(AnnotationConstants.STATUS_ATTACHED);
				
		entityMap.setStaticEntityId(((Long)staticEntityId));
		
		return entityMap;
	}
	/**
	 * @param hookEntityName
	 * @return
	 * @throws DAOException
	 */
	private static Object getObjectIdentifier(String whereColumnValue,String selectObjName,String whereColumnName) throws DAOException
	{
		DefaultBizLogic defaultBizLogic = BizLogicFactory.getDefaultBizLogic();
		String[] selectColName = {Constants.SYSTEM_IDENTIFIER};
		String[] whereColName = {whereColumnName};
		Object[] whereColValue = {whereColumnValue};
		String[] whereColCondition = {Constants.EQUALS};
		String joinCondition = Constants.AND_JOIN_CONDITION;
		List id = defaultBizLogic.retrieve(selectObjName, selectColName, whereColName, whereColCondition, whereColValue, joinCondition);
		if(id != null && id.size() > 0)
		{
			return id.get(0);
		}
		return null;
	}

}
