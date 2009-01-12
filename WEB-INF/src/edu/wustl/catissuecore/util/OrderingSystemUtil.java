/**
 * <p>Title: OrderingSystemUtil Class>
 * <p>Description:	Utility class for Ordering System</p>
 * Copyright:    Copyright (c) year
 * Company: Washington University, School of Medicine, St. Louis.
 * @author Ashish Gupta
 * @version 1.00
 * Created on Oct 09,2006
 */
package edu.wustl.catissuecore.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import edu.wustl.catissuecore.bean.RequestViewBean;
import edu.wustl.catissuecore.domain.OrderDetails;
import edu.wustl.catissuecore.domain.PathologicalCaseOrderItem;
import edu.wustl.catissuecore.domain.Specimen;
import edu.wustl.catissuecore.domain.SpecimenCollectionGroup;
import edu.wustl.catissuecore.domain.SpecimenOrderItem;
import edu.wustl.catissuecore.util.global.Constants;
import edu.wustl.catissuecore.util.global.Utility;
import edu.wustl.common.beans.NameValueBean;
import edu.wustl.common.util.dbManager.HibernateMetaData;



public class OrderingSystemUtil
{
	
	public static void getPossibleStatusForDistribution(List possibleStatusList)
	{
		possibleStatusList.add(new NameValueBean(Constants.ORDER_REQUEST_STATUS_PENDING_FOR_DISTRIBUTION,Constants.ORDER_REQUEST_STATUS_PENDING_FOR_DISTRIBUTION));
		possibleStatusList.add(new NameValueBean(Constants.ORDER_REQUEST_STATUS_REJECTED_INAPPROPRIATE_REQUEST,Constants.ORDER_REQUEST_STATUS_REJECTED_INAPPROPRIATE_REQUEST));
		possibleStatusList.add(new NameValueBean(Constants.ORDER_REQUEST_STATUS_REJECTED_SPECIMEN_UNAVAILABLE,Constants.ORDER_REQUEST_STATUS_REJECTED_SPECIMEN_UNAVAILABLE));
		possibleStatusList.add(new NameValueBean(Constants.ORDER_REQUEST_STATUS_REJECTED_UNABLE_TO_CREATE,Constants.ORDER_REQUEST_STATUS_REJECTED_UNABLE_TO_CREATE));
		possibleStatusList.add(new NameValueBean(Constants.ORDER_REQUEST_STATUS_DISTRIBUTED,Constants.ORDER_REQUEST_STATUS_DISTRIBUTED));
		possibleStatusList.add(new NameValueBean(Constants.ORDER_REQUEST_STATUS_DISTRIBUTED_AND_CLOSE,Constants.ORDER_REQUEST_STATUS_DISTRIBUTED_AND_CLOSE));
	}

	/**
	 * @param initialStatus The initial status from Db.
	 * @return possibleStatusList
	 */
	public static List getPossibleStatusList(String initialStatus)
	{
		List possibleStatusList = new ArrayList();
		if(initialStatus.trim().equalsIgnoreCase(Constants.ORDER_REQUEST_STATUS_NEW))
		{
			possibleStatusList.add(new NameValueBean(Constants.ORDER_REQUEST_STATUS_NEW,Constants.ORDER_REQUEST_STATUS_NEW));
			possibleStatusList.add(new NameValueBean(Constants.ORDER_REQUEST_STATUS_PENDING_PROTOCOL_REVIEW,Constants.ORDER_REQUEST_STATUS_PENDING_PROTOCOL_REVIEW));
			possibleStatusList.add(new NameValueBean(Constants.ORDER_REQUEST_STATUS_PENDING_SPECIMEN_PREPARATION,Constants.ORDER_REQUEST_STATUS_PENDING_SPECIMEN_PREPARATION));
			getPossibleStatusForDistribution(possibleStatusList);
		}
		if(initialStatus.trim().equalsIgnoreCase(Constants.ORDER_REQUEST_STATUS_PENDING_PROTOCOL_REVIEW))
		{
			possibleStatusList.add(new NameValueBean(Constants.ORDER_REQUEST_STATUS_PENDING_PROTOCOL_REVIEW,Constants.ORDER_REQUEST_STATUS_PENDING_PROTOCOL_REVIEW));
			possibleStatusList.add(new NameValueBean(Constants.ORDER_REQUEST_STATUS_PENDING_SPECIMEN_PREPARATION,Constants.ORDER_REQUEST_STATUS_PENDING_SPECIMEN_PREPARATION));
			getPossibleStatusForDistribution(possibleStatusList);
		}
		else if(initialStatus.trim().equalsIgnoreCase(Constants.ORDER_REQUEST_STATUS_PENDING_SPECIMEN_PREPARATION))
		{
			possibleStatusList.add(new NameValueBean(Constants.ORDER_REQUEST_STATUS_PENDING_SPECIMEN_PREPARATION,Constants.ORDER_REQUEST_STATUS_PENDING_SPECIMEN_PREPARATION));
			getPossibleStatusForDistribution(possibleStatusList);
			
		}
		else if(initialStatus.trim().equalsIgnoreCase(Constants.ORDER_REQUEST_STATUS_PENDING_FOR_DISTRIBUTION))
		{
			getPossibleStatusForDistribution(possibleStatusList);
		}
		else if(initialStatus.trim().equalsIgnoreCase(Constants.ORDER_REQUEST_STATUS_DISTRIBUTED))
		{
			possibleStatusList.add(new NameValueBean(Constants.ORDER_REQUEST_STATUS_DISTRIBUTED,Constants.ORDER_REQUEST_STATUS_DISTRIBUTED));
		}
		else if(initialStatus.trim().equalsIgnoreCase(Constants.ORDER_REQUEST_STATUS_REJECTED_INAPPROPRIATE_REQUEST))
		{
			possibleStatusList.add(new NameValueBean(Constants.ORDER_REQUEST_STATUS_REJECTED_INAPPROPRIATE_REQUEST,Constants.ORDER_REQUEST_STATUS_REJECTED_INAPPROPRIATE_REQUEST));
		}
		else if(initialStatus.trim().equalsIgnoreCase(Constants.ORDER_REQUEST_STATUS_REJECTED_SPECIMEN_UNAVAILABLE))
		{
			possibleStatusList.add(new NameValueBean(Constants.ORDER_REQUEST_STATUS_REJECTED_SPECIMEN_UNAVAILABLE,Constants.ORDER_REQUEST_STATUS_REJECTED_SPECIMEN_UNAVAILABLE));
		}
		else if(initialStatus.trim().equalsIgnoreCase(Constants.ORDER_REQUEST_STATUS_REJECTED_UNABLE_TO_CREATE))
		{
			possibleStatusList.add(new NameValueBean(Constants.ORDER_REQUEST_STATUS_REJECTED_UNABLE_TO_CREATE,Constants.ORDER_REQUEST_STATUS_REJECTED_UNABLE_TO_CREATE));
		}
		else if(initialStatus.trim().equalsIgnoreCase(Constants.ORDER_REQUEST_STATUS_DISTRIBUTED_AND_CLOSE))
		{
			possibleStatusList.add(new NameValueBean(Constants.ORDER_REQUEST_STATUS_DISTRIBUTED_AND_CLOSE,Constants.ORDER_REQUEST_STATUS_DISTRIBUTED_AND_CLOSE));
		}
		return possibleStatusList;
	}
	/**
	 * @param order OrderDetails
	 * @return RequestViewBean object.
	 */
	public static RequestViewBean getRequestViewBeanToDisplay(OrderDetails order)
	{
		RequestViewBean requestViewBean = new RequestViewBean();
		requestViewBean.setDistributionProtocol(order.getDistributionProtocol().getTitle());
		requestViewBean.setDistributionProtocolId(order.getDistributionProtocol().getId().toString());
		requestViewBean.setOrderName(order.getName());
		requestViewBean.setRequestedBy(order.getDistributionProtocol().getPrincipalInvestigator().getLastName()+", "+order.getDistributionProtocol().getPrincipalInvestigator().getFirstName());
		requestViewBean.setRequestedDate(Utility.parseDateToString(order.getRequestedDate(), edu.wustl.catissuecore.util.global.Variables.dateFormat));
		requestViewBean.setEmail(order.getDistributionProtocol().getPrincipalInvestigator().getEmailAddress());
		return requestViewBean;
	}
	/**
	 * @param rootNode Specimen
	 * @param childNodes Collection
	 * @return List. ArrayList of children Specimen objects.
	 */
	public static List getAllChildrenSpecimen(Collection childNodes)
	{
		ArrayList childrenSpecimenList = new ArrayList();	
		
		//If no childNodes present then tree will contain only the root node.
		if(childNodes == null)
		{
			return null;
		}
		
		//Otherwise
		Iterator specimenItr = childNodes.iterator();
		while(specimenItr.hasNext())
		{
			Specimen specimen  = (Specimen)specimenItr.next();
			List subChildNodesList = getAllChildrenSpecimen(specimen.getChildSpecimenCollection());
			childrenSpecimenList.add(specimen);
		}
		
		return childrenSpecimenList;
	}
	
	public static List getAllSpecimen(Specimen specimen)
	{
		ArrayList allSpecimenList = new ArrayList();	
		allSpecimenList.add(specimen);
		
		Iterator childSpec = specimen.getChildSpecimenCollection().iterator();
		while(childSpec.hasNext())
		{
			List subChildNodesList = getAllSpecimen((Specimen)childSpec.next());
			for(int i=0;i<subChildNodesList.size();i++)
			allSpecimenList.add(subChildNodesList.get(i));
		}
		
		return allSpecimenList;
		
	}
	
	/**
	 * @param childrenSpecimenList Collection
	 * @param className String
	 * @param type String
	 * @return List. The namevaluebean list of children specimen of particular 'class' and 'type' to display.
	 */
	public static List getChildrenSpecimenForClassAndType(Collection childrenSpecimenList,String className,String type)
	{
		List finalChildrenSpecimenList = new ArrayList();		
		Iterator childrenSpecimenListIterator = childrenSpecimenList.iterator();
		while(childrenSpecimenListIterator.hasNext())
		{
			Specimen childrenSpecimen = (Specimen)childrenSpecimenListIterator.next();
			childrenSpecimen= (Specimen)HibernateMetaData.getProxyObjectImpl(childrenSpecimen);
			if(childrenSpecimen.getSpecimenClass().trim().equalsIgnoreCase(className) && childrenSpecimen.getSpecimenType().trim().equalsIgnoreCase(type) && childrenSpecimen.getAvailableQuantity() > 0)
			{
				finalChildrenSpecimenList.add(childrenSpecimen);
			}
		}
		return finalChildrenSpecimenList;
	}
	/**
	 * @param listToConvert Collection
	 * @return List. The namevaluebean list of children specimen to display.
	 */
	public static List getNameValueBeanList(Collection listToConvert,Specimen requestFor)
	{
		Vector nameValueBeanList = new Vector();
		
		
		if(requestFor != null)
			nameValueBeanList.add(0,new NameValueBean(requestFor.getLabel(),requestFor.getId().toString()));
		else 
			nameValueBeanList.add(0,new NameValueBean(" ","#"));
		
		Iterator iter = listToConvert.iterator();		
		while(iter.hasNext())
		{
			Specimen specimen = (Specimen)iter.next();
			NameValueBean nameValueBean = new NameValueBean(specimen.getLabel(),specimen.getId().toString());
			if(!nameValueBeanList.contains(nameValueBean))
				nameValueBeanList.add(nameValueBean);
		}
		
		return nameValueBeanList;
	}
	
	
	/**
	 * @param listToConvert Collection
	 * @return List. The namevaluebean list of children specimen to display.
	 */
	public static List getNameValueBean(Specimen specimen)
	{
		List nameValueBeanList = new ArrayList();
		nameValueBeanList.add(new NameValueBean(specimen.getLabel(),specimen.getId().toString()));		
		Iterator iter = specimen.getChildSpecimenCollection().iterator();		
		while(iter.hasNext())
		{
			Specimen childSpecimen = (Specimen)iter.next();
			nameValueBeanList.add(new NameValueBean(childSpecimen.getLabel(),childSpecimen.getId().toString()));
		}
		
		return nameValueBeanList;
	}
	
	
	/**
	 * @param specimenCollectionGroup
	 * @param pathologicalCaseOrderItem
	 * @return
	 */
	/*public static List getRequestForListForPathologicalCases(SpecimenCollectionGroup specimenCollectionGroup,PathologicalCaseOrderItem pathologicalCaseOrderItem)
	{
		Collection childrenSpecimenList = specimenCollectionGroup.getSpecimenCollection();
		List totalChildrenSpecimenColl = new ArrayList();
    	//List childrenSpecimenListToDisplay = new ArrayList();
    	
    	Iterator childrenSpecimenListIterator = childrenSpecimenList.iterator();
    	while (childrenSpecimenListIterator.hasNext())
    	{
    		Specimen specimen = (Specimen)childrenSpecimenListIterator.next();
    		List childSpecimenCollection = OrderingSystemUtil.getAllChildrenSpecimen(specimen.getChildSpecimenCollection());
    		List finalChildrenSpecimenCollection = null;
    		if(pathologicalCaseOrderItem.getSpecimenClass() != null && pathologicalCaseOrderItem.getSpecimenType() != null && !pathologicalCaseOrderItem.getSpecimenClass().trim().equalsIgnoreCase("") && !pathologicalCaseOrderItem.getSpecimenType().trim().equalsIgnoreCase(""))
    	    {	//"Derived"	
    			finalChildrenSpecimenCollection = OrderingSystemUtil.getChildrenSpecimenForClassAndType(childSpecimenCollection,pathologicalCaseOrderItem.getSpecimenClass(),pathologicalCaseOrderItem.getSpecimenType());
    		}	    	
		    else
		    {  	//"Block" . Specimen class = "Tissue" , Specimen Type = "Block".
		    	finalChildrenSpecimenCollection = OrderingSystemUtil.getChildrenSpecimenForClassAndType(childSpecimenCollection,"Tissue","Block");
		    }
    		//adding specimen to the collection
    		if(specimen.getClassName().equalsIgnoreCase(pathologicalCaseOrderItem.getSpecimenClass()) && specimen.getSpecimenType().equalsIgnoreCase(pathologicalCaseOrderItem.getSpecimenType()))
			{
    			finalChildrenSpecimenCollection.add(specimen);
			}
    		if(finalChildrenSpecimenCollection!=null)
    		{
    			Iterator finalChildrenSpecimenCollectionIterator = finalChildrenSpecimenCollection.iterator();
    			while(finalChildrenSpecimenCollectionIterator.hasNext())
    			{	    		
    				totalChildrenSpecimenColl.add((Specimen)(finalChildrenSpecimenCollectionIterator.next()));
    			}	    			
    		}
    	} 	
    	
    	return totalChildrenSpecimenColl;
	}*/
	
	
	/**
	 * @param specimenCollectionGroup
	 * @param pathologicalCaseOrderItem
	 * @return
	 */
	public static List getAllSpecimensForPathologicalCases(SpecimenCollectionGroup specimenCollectionGroup,PathologicalCaseOrderItem pathologicalCaseOrderItem)
	{
		Collection specimenList = specimenCollectionGroup.getSpecimenCollection();
		List totalSpecimenColl = new ArrayList();
    	    	
    	Iterator specimenListIterator = specimenList.iterator();
    	while (specimenListIterator.hasNext())
    	{
    		Specimen specimen = (Specimen)specimenListIterator.next();
    		if(specimen.getClassName().equalsIgnoreCase(pathologicalCaseOrderItem.getSpecimenClass()) && specimen.getSpecimenType().equalsIgnoreCase(pathologicalCaseOrderItem.getSpecimenType()))
    		{	
	    		totalSpecimenColl.add(specimen);
	    		List childSpecimenCollection = OrderingSystemUtil.getAllSpecimen(specimen);
	    		if(childSpecimenCollection!=null)
	    		{
	    			Iterator childSpecimenCollectionIterator = childSpecimenCollection.iterator();
	    			while(childSpecimenCollectionIterator.hasNext())
	    			{	    		
	    				totalSpecimenColl.add((Specimen)(childSpecimenCollectionIterator.next()));
	    			}	    			
	    		}
    		}	
    	} 	
    	
    	return totalSpecimenColl;
	}
	
	/**
	 * @param specimenOrderItemCollection collection
	 * @param arrayRequestBean objetc
	 * @return DefinedArrayRequestBean object
	 */
	public static String determineCreateArrayCondition(Collection specimenOrderItemCollection)
	{
		Iterator iter = specimenOrderItemCollection.iterator();
		int readyCounter = 0;
		int rejectedCounter = 0;
		int pendingCounter = 0;
		int newCounter = 0;
		while(iter.hasNext())
		{
			SpecimenOrderItem specimenOrderItem = (SpecimenOrderItem)iter.next();
			if(specimenOrderItem.getStatus().trim().equalsIgnoreCase(Constants.ORDER_REQUEST_STATUS_NEW))
			{
				newCounter++;
			}
			else if(specimenOrderItem.getStatus().trim().equalsIgnoreCase(Constants.ORDER_REQUEST_STATUS_PENDING_FOR_DISTRIBUTION)
					|| specimenOrderItem.getStatus().trim().equalsIgnoreCase(Constants.ORDER_REQUEST_STATUS_PENDING_PROTOCOL_REVIEW)
					|| specimenOrderItem.getStatus().trim().equalsIgnoreCase(Constants.ORDER_REQUEST_STATUS_PENDING_SPECIMEN_PREPARATION))
			{
				pendingCounter++;
			}
			else if(specimenOrderItem.getStatus().trim().equalsIgnoreCase(Constants.ORDER_REQUEST_STATUS_REJECTED_INAPPROPRIATE_REQUEST)
					|| specimenOrderItem.getStatus().trim().equalsIgnoreCase(Constants.ORDER_REQUEST_STATUS_REJECTED_SPECIMEN_UNAVAILABLE)
					|| specimenOrderItem.getStatus().trim().equalsIgnoreCase(Constants.ORDER_REQUEST_STATUS_REJECTED_UNABLE_TO_CREATE))
			{
				rejectedCounter++;
			}
			else if(specimenOrderItem.getStatus().trim().equalsIgnoreCase(Constants.ORDER_REQUEST_STATUS_READY_FOR_ARRAY_PREPARATION))
			{
				readyCounter++;
			}
		}
		if((newCounter > 0) || (pendingCounter > 0)	|| (specimenOrderItemCollection.size() == rejectedCounter) || (specimenOrderItemCollection.size() != rejectedCounter+readyCounter))
		{
			return "true"; 
		}
		else
		{
			return "false"; 
		}		
	}
	
	
	public static String getUnit(Specimen specimen)
	{
		String specimenQuantityUnit="";
		if(specimen.getSpecimenClass().equals("Tissue"))
		{
			if(specimen.getSpecimenType().equals(Constants.FROZEN_TISSUE_SLIDE) || specimen.getSpecimenType().equals(Constants.FIXED_TISSUE_BLOCK)
					||specimen.getSpecimenType().equals(Constants.FROZEN_TISSUE_BLOCK) || specimen.getSpecimenType().equals(Constants.NOT_SPECIFIED)
					|| specimen.getSpecimenType().equals(Constants.FIXED_TISSUE_SLIDE))
			{
				specimenQuantityUnit = Constants.UNIT_CN;
				
			}	
			else 
			{
					if(specimen.getSpecimenType().equals(Constants.MICRODISSECTED))
					{
						specimenQuantityUnit = Constants.UNIT_CL;
					}
					else
					{
						specimenQuantityUnit = Constants.UNIT_GM;
					}
			}	
		}
		else if(specimen.getSpecimenClass().equals("Fluid"))
		{
			specimenQuantityUnit = Constants.UNIT_ML;
		}
		else if(specimen.getSpecimenClass().equals("Cell"))
		{
			specimenQuantityUnit = Constants.UNIT_CC;
		}
		else if(specimen.getSpecimenClass().equals("Molecular"))
		{
			specimenQuantityUnit = Constants.UNIT_MG;
		}
		return specimenQuantityUnit;
	}
}
