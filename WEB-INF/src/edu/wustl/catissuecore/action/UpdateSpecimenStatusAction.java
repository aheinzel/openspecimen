package edu.wustl.catissuecore.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

import edu.wustl.catissuecore.actionForm.ViewSpecimenSummaryForm;
import edu.wustl.catissuecore.bean.CollectionProtocolEventBean;
import edu.wustl.catissuecore.bean.GenericSpecimen;
import edu.wustl.catissuecore.bizlogic.NewSpecimenBizLogic;
import edu.wustl.catissuecore.bizlogic.SpecimenCollectionGroupBizLogic;
import edu.wustl.catissuecore.domain.MolecularSpecimen;
import edu.wustl.catissuecore.domain.Specimen;
import edu.wustl.catissuecore.domain.SpecimenCollectionGroup;
import edu.wustl.catissuecore.domain.SpecimenObjectFactory;
import edu.wustl.catissuecore.domain.SpecimenPosition;
import edu.wustl.catissuecore.domain.StorageContainer;
import edu.wustl.catissuecore.util.CollectionProtocolUtil;
import edu.wustl.catissuecore.util.SpecimenDetailsTagUtil;
import edu.wustl.catissuecore.util.StorageContainerUtil;
import edu.wustl.catissuecore.util.global.Constants;
import edu.wustl.common.action.BaseAction;
import edu.wustl.common.beans.SessionDataBean;
import edu.wustl.common.exception.AssignDataException;
import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.util.logger.Logger;

public class UpdateSpecimenStatusAction extends BaseAction
{

	public ActionForward executeAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)
			throws Exception
	{
		ViewSpecimenSummaryForm specimenSummaryForm = (ViewSpecimenSummaryForm) form;
		try
		{
			String eventId = specimenSummaryForm.getEventId();

			HttpSession session = request.getSession();
			NewSpecimenBizLogic bizLogic = new NewSpecimenBizLogic();

			LinkedHashSet specimenDomainCollection = getSpecimensToSave(eventId, session);

			SessionDataBean sessionDataBean = (SessionDataBean) session.getAttribute(Constants.SESSION_DATA);

			//bizLogic.updaupdateAnticipatorySpecimens(specimenDomainCollection, sessionDataBean);
			if (specimenDomainCollection != null && specimenDomainCollection.size() > 0)
				bizLogic.update(specimenDomainCollection, specimenDomainCollection, Constants.HIBERNATE_DAO, sessionDataBean);
			Object obj = session.getAttribute("SCGFORM");

			//11July08 : Mandar : For GenericSpecimen
			SpecimenDetailsTagUtil.setAnticipatorySpecimenDetails(request, specimenSummaryForm, false);
			if (specimenSummaryForm.getPrintCheckbox() != null && specimenSummaryForm.getPrintCheckbox().equals("true"))
			{
				//By Falguni Sachde
				//Code Reviewer:Abhijit Naik
				//Bug :6569 : In case of collected SCG ,the specimenDomainCollection not contains all specimen.
				//To get all specimen related with give SCG ,query with SCG id and get SpecimenCollection 
				if (obj == null)
				{
					Logger.out.fatal("SCG id is null failed to execute print of scg -UpdateSpecimenStatusAction");
				}
				else
				{
					HashSet specimenprintCollection = getSpecimensToPrint((Long) obj, sessionDataBean);
					HashMap forwardToPrintMap = new HashMap();
					forwardToPrintMap.put("printAntiSpecimen", specimenprintCollection);
					request.setAttribute("forwardToPrintMap", forwardToPrintMap);
					request.setAttribute("AntiSpecimen", "1");
					return mapping.findForward("printAnticipatorySpecimens");
				}
			}

			if (specimenSummaryForm.getForwardTo() != null && specimenSummaryForm.getForwardTo().equals(Constants.ADD_MULTIPLE_SPECIMEN_TO_CART))
			{
				HashSet specimenprintCollection = getSpecimensToPrint((Long) obj, sessionDataBean);

				Iterator iter = specimenprintCollection.iterator();
				List specimenIdList = new ArrayList();
				while (iter.hasNext())
				{
					specimenIdList.add(((Specimen) iter.next()).getId());
				}
				request.setAttribute("specimenIdList", specimenIdList);
				saveToken(request);

				return mapping.findForward(Constants.ADD_MULTIPLE_SPECIMEN_TO_CART);
			}
			if (request.getParameter("target") != null && request.getParameter("target").equals("viewSummary"))
			{
				ActionMessages actionMessages = new ActionMessages();
				actionMessages.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("object.add.successOnly", "Specimens"));
				specimenSummaryForm.setShowbarCode(true);
				specimenSummaryForm.setShowLabel(true);
				saveMessages(request, actionMessages);
				specimenSummaryForm.setReadOnly(true);

			}
			return mapping.findForward(Constants.SUCCESS);
		}
		catch (Exception exception)
		{
			//11July08 : Mandar : For GenericSpecimen
			SpecimenDetailsTagUtil.setAnticipatorySpecimenDetails(request, specimenSummaryForm, false);
			// Suman-For bug #8228
			String s = "";
			if (exception.getMessage().equals("Failed to update multiple specimen Stroage location already in use")
					|| exception.getMessage().equals("Failed to update multiple specimen Either Storagecontainer is full! or it cannot accomodate all the specimens.")
					|| exception.getMessage().equals("Failed to update multiple specimen Storagecontainer information not found!"))
			{
				clearSCLocation(specimenSummaryForm);
				s = "Please allocate a different container to the specimens shown below with empty container names as the container you specified has insufficient space";
			} else {
				s = exception.getMessage();
			}
			ActionErrors actionErrors = new ActionErrors();
			actionErrors.add(actionErrors.GLOBAL_MESSAGE, new ActionError("errors.item", s));
			saveErrors(request, actionErrors);
			saveToken(request);
			String pageOf = request.getParameter(Constants.PAGEOF);
			if(pageOf != null)
				request.setAttribute(Constants.PAGEOF,pageOf);
			return mapping.findForward(Constants.FAILURE);
		}

	}
	// bug 8228 -suman
	// this method checks for free locations of a container and clears the ones
	// which are not available
	private void clearSCLocation(ViewSpecimenSummaryForm specimenSummaryForm)
			throws Exception {
		List<GenericSpecimen> specimenList = specimenSummaryForm.getSpecimenList();
		List<GenericSpecimen> aliquotList = specimenSummaryForm.getAliquotList();
		List<GenericSpecimen> derivedList = specimenSummaryForm.getDerivedList();
		List<String> allocatedPositions = new ArrayList<String>();
		int freeSizeofContainer = 0;
		for (GenericSpecimen spec : specimenList) 
		{
			String conName = spec.getSelectedContainerName();
			String conId = spec.getContainerId();
			try {
				freeSizeofContainer = StorageContainerUtil.getCountofFreeLocationOfContainer(conId, conName);
			}
			catch (Exception e) 
			{
				e.printStackTrace();
		    }
		}
		int tempContainerSize = checkList(specimenList, allocatedPositions,freeSizeofContainer);
		tempContainerSize = checkList(aliquotList, allocatedPositions,tempContainerSize);
		checkList(derivedList, allocatedPositions, tempContainerSize);
	}

	// this method check if the x and y positions are given and clears them if
	// not allocated.
	public int checkList(List<GenericSpecimen> gs,
			List<String> allocatedPositions, int containerSize) {
		for (GenericSpecimen spec : gs) {
			String positionOne = spec.getPositionDimensionOne();
			String positionTwo = spec.getPositionDimensionTwo();
			String containerName = spec.getStorageContainerForSpecimen();
			String key = containerName + ":" + positionOne + "," + positionTwo;
			if (positionOne != "" && positionTwo != "") 
			{
				if (!(StorageContainerUtil.isPostionAvaialble(spec.getContainerId(), spec.getSelectedContainerName(),positionOne, positionTwo))
						|| allocatedPositions.contains(key)) {
					spec.setPositionDimensionOne("");
					spec.setPositionDimensionTwo("");
					spec.setStorageContainerForSpecimen("");
					spec.setSelectedContainerName("");
				} else {
					allocatedPositions.add(key);
				}
			} else {
				if (containerSize >= 1) {
					allocatedPositions.add(key);
				} else {
					spec.setPositionDimensionOne("");
					spec.setPositionDimensionTwo("");
					spec.setStorageContainerForSpecimen("");
					spec.setSelectedContainerName("");
				}
			}
			containerSize = containerSize - 1;
		}
		return containerSize;
	}

	// end bug 8228 - Suman
	

	/**
	 * @param eventId
	 * @param session
	 * @return
	 * @throws BizLogicException
	 */
	protected LinkedHashSet getSpecimensToSave(String eventId, HttpSession session) throws BizLogicException
	{
		Map collectionProtocolEventMap = (Map) session.getAttribute(Constants.COLLECTION_PROTOCOL_EVENT_SESSION_MAP);

		CollectionProtocolEventBean eventBean = (CollectionProtocolEventBean) collectionProtocolEventMap.get(eventId);

		LinkedHashMap specimenMap = (LinkedHashMap) eventBean.getSpecimenRequirementbeanMap();

		Collection specimenCollection = specimenMap.values();
		Iterator iterator = specimenCollection.iterator();

		LinkedHashSet specimenDomainCollection = new LinkedHashSet();
		while (iterator.hasNext())
		{
			GenericSpecimen specimenVO = (GenericSpecimen) iterator.next();
			Specimen specimen = null;
			if (!specimenVO.getReadOnly())
			{
				specimen = createSpecimenDomainObject(specimenVO);
				specimen.setChildSpecimenCollection(getChildrenSpecimens(specimenVO, specimen));
				specimenDomainCollection.add(specimen);
			}
			else
			{
				specimenDomainCollection.addAll(getChildrenSpecimens(specimenVO, specimen));
			}
		}
		return specimenDomainCollection;
	}

	//Abhishek Mehta : Performance related Changes
	private Collection getChildrenSpecimens(GenericSpecimen specimenVO, Specimen parentSpecimen) throws BizLogicException
	{
		HashSet childrenSpecimens = new HashSet();
		LinkedHashMap aliquotMap = specimenVO.getAliquotSpecimenCollection();

		if (aliquotMap != null && !aliquotMap.isEmpty())
		{
			Collection aliquotCollection = aliquotMap.values();
			Iterator iterator = aliquotCollection.iterator();
			while (iterator.hasNext())
			{
				GenericSpecimen aliquotSpecimen = (GenericSpecimen) iterator.next();
				Specimen specimen = null;
				if (!aliquotSpecimen.getReadOnly())
				{
					specimen = createSpecimenDomainObject(aliquotSpecimen);
					specimen.setParentSpecimen(parentSpecimen);
					specimen.setChildSpecimenCollection(getChildrenSpecimens(aliquotSpecimen, specimen));
					childrenSpecimens.add(specimen);
				}
				else
				{
					childrenSpecimens.addAll(getChildrenSpecimens(aliquotSpecimen, specimen));
				}
			}
		}

		LinkedHashMap derivedMap = specimenVO.getDeriveSpecimenCollection();

		if (derivedMap != null && !derivedMap.isEmpty())
		{
			Collection aliquotCollection = derivedMap.values();
			Iterator iterator = aliquotCollection.iterator();
			while (iterator.hasNext())
			{
				GenericSpecimen derivedSpecimen = (GenericSpecimen) iterator.next();
				Specimen specimen = null;
				if (!derivedSpecimen.getReadOnly())
				{
					specimen = createSpecimenDomainObject(derivedSpecimen);
					specimen.setParentSpecimen(parentSpecimen);
					specimen.setChildSpecimenCollection(getChildrenSpecimens(derivedSpecimen, specimen));
					childrenSpecimens.add(specimen);
				}
				else
				{
					childrenSpecimens.addAll(getChildrenSpecimens(derivedSpecimen, specimen));
				}
			}
		}
		return childrenSpecimens;
	}

	/**
	 * @param specimenVO
	 * @return
	 */
	protected Specimen createSpecimenDomainObject(GenericSpecimen specimenVO) throws BizLogicException
	{

		Specimen specimen;
		try
		{
			specimen = (Specimen) new SpecimenObjectFactory().getDomainObject(specimenVO.getClassName());
		}
		catch (AssignDataException e1)
		{
			e1.printStackTrace();
			return null;
		}

		if (Constants.MOLECULAR.equals(specimenVO.getClassName()))
		{
			Double concentration = null;
			try
			{
				concentration = new Double(specimenVO.getConcentration());
			}
			catch (Exception exception)
			{
				concentration = new Double(0);
			}
			((MolecularSpecimen) specimen).setConcentrationInMicrogramPerMicroliter(concentration);
		}
		Long id = getSpecimenId(specimenVO);
		specimen.setId(id);
		specimen.setSpecimenClass(specimenVO.getClassName());
		specimen.setSpecimenType(specimenVO.getType());
		if ("".equals(specimenVO.getDisplayName()))
			specimen.setLabel(null);
		else
			specimen.setLabel(specimenVO.getDisplayName());

		specimen.setBarcode(specimenVO.getBarCode());

		/* bug 6015  vaishali khandelwal*/

		/* end bug 6015 */

		String initialQuantity = specimenVO.getQuantity();
		if (initialQuantity != null)
		{
			if (!initialQuantity.equals(""))
			{
				specimen.setInitialQuantity(new Double(initialQuantity));
			}
		}
		if (specimenVO.getCheckedSpecimen())
		{
			specimen.setCollectionStatus(Constants.SPECIMEN_COLLECTED);
			specimen.setAvailableQuantity(new Double(initialQuantity));
			if ((specimen.getAvailableQuantity() != null && specimen.getAvailableQuantity().doubleValue() > 0))
			{
				specimen.setIsAvailable(Boolean.TRUE);
			}
		}
		else
		{
			specimen.setCollectionStatus("Pending");
			//Mandar : 25July08: ------- start ------------
			specimen.setAvailableQuantity(new Double(0));
			//Mandar : 25July08: ------- end ------------
		}

		if ("Virtual".equals(specimenVO.getStorageContainerForSpecimen()))
		{
			//	specimen.setStorageContainer(null);
			specimen.setSpecimenPosition(null);
		}
		else
		{
			setStorageContainer(specimenVO, specimen);
		}

		return specimen;
	}

	/**
	 * @param specimenVO
	 * @param specimen
	 */
	private void setStorageContainer(GenericSpecimen specimenVO, Specimen specimen) throws BizLogicException
	{

		String pos1 = specimenVO.getPositionDimensionOne();
		String pos2 = specimenVO.getPositionDimensionTwo();

		if (!specimenVO.getCheckedSpecimen())
		{
			specimenVO.setPositionDimensionOne(String
					.valueOf(CollectionProtocolUtil.getStorageTypeValue(specimenVO.getStorageContainerForSpecimen())));

			return;
		}
		SpecimenPosition specPos = specimen.getSpecimenPosition();

		if (specPos == null)
		{
			specPos = new SpecimenPosition();
		}
		if (pos1 != null)
		{
			try
			{
				specPos.setPositionDimensionOne(Integer.parseInt(pos1));
			}
			catch (NumberFormatException exception)
			{
				specPos.setPositionDimensionOne(null);
			}
		}
		if (pos2 != null)
		{
			try
			{
				specPos.setPositionDimensionTwo(Integer.parseInt(pos2));
			}
			catch (NumberFormatException exception)
			{
				specPos.setPositionDimensionTwo(null);
			}
		}
		StorageContainer storageContainer = new StorageContainer();
		specPos.setSpecimen(specimen);
		specPos.setStorageContainer(storageContainer);
		specimen.setSpecimenPosition(specPos);

		String containerId = specimenVO.getContainerId();

		if (containerId != null && containerId.trim().length() > 0)
		{
			storageContainer.setId(new Long(containerId));
		}
		if (specimenVO.getSelectedContainerName() == null || specimenVO.getSelectedContainerName().trim().length() == 0)
		{
			throw new BizLogicException("Container name is missing for specimen :" + specimenVO.getDisplayName());
		}
		storageContainer.setName(specimenVO.getSelectedContainerName());
		//	specimen.setStorageContainer(storageContainer);
	}

	/**
	 * @param specimenVO
	 * @return
	 */
	private Long getSpecimenId(GenericSpecimen specimenVO)
	{
		long uniqueId = specimenVO.getId();
		if (uniqueId <= 0)
		{
			return null;
		}
		Long id = new Long(uniqueId);
		return id;
	}

	/**
	 * @param scgId
	 * @param sessionDataBean
	 * @return
	 * @throws BizLogicException
	 */
	protected HashSet getSpecimensToPrint(Long scgId, SessionDataBean sessionDataBean) throws BizLogicException
	{

		SpecimenCollectionGroupBizLogic bizLogic = new SpecimenCollectionGroupBizLogic();
		SpecimenCollectionGroup objSCG = bizLogic.getSCGFromId(scgId, sessionDataBean, true);
		HashSet specimenCollection = new HashSet(objSCG.getSpecimenCollection());

		return specimenCollection;

	}

}