package edu.wustl.catissuecore.action;

import javax.servlet.http.HttpServletRequest;

import edu.wustl.catissuecore.actionForm.EventParametersForm;
import edu.wustl.catissuecore.actionForm.ProcedureEventParametersForm;
import edu.wustl.catissuecore.util.global.Constants;

public class ProcedureEventParametersAction extends SpecimenEventParametersAction
{
 @Override
protected void setRequestParameters(HttpServletRequest request, EventParametersForm eventParametersForm) throws Exception
{
	// TODO Auto-generated method stub
	 String operation = (String) request.getAttribute(Constants.OPERATION);
     String formName, specimenId=null;

     boolean readOnlyValue;
     ProcedureEventParametersForm procedureEventParametersForm=(ProcedureEventParametersForm)eventParametersForm;
     if (procedureEventParametersForm.getOperation().equals(Constants.EDIT))
     {
         formName = Constants.PROCEDURE_EVENT_PARAMETERS_EDIT_ACTION;
         readOnlyValue = true;
     }
     else
     {
         formName = Constants.PROCEDURE_EVENT_PARAMETERS_ADD_ACTION;
			specimenId = (String) request.getAttribute(Constants.SPECIMEN_ID);
         readOnlyValue = false;
     }
     String changeAction = "setFormAction('" + formName + "');";
	    request.setAttribute("formName", formName);
		request.setAttribute("readOnlyValue", readOnlyValue);
		request.setAttribute("changeAction", changeAction);
		
	
}
}
