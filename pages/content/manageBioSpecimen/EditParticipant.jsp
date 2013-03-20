<link rel="stylesheet" type="text/css" href="css/catissue_suite.css" />
<script language="JavaScript" type="text/javascript" src="jss/javaScript.js"></script>

<%
	String[] activityStatusList = (String[]) request.getAttribute(Constants.ACTIVITYSTATUSLIST);
    ParticipantForm form = (ParticipantForm) request.getAttribute("participantForm");
%>

<%
		if (pageOf.equals(Constants.PAGE_OF_PARTICIPANT_CP_QUERY)) {
		strCheckStatus = "checkActivityStatus(this,'"
		+ Constants.CP_QUERY_BIO_SPECIMEN + "')";
	}
%>
<script language="JavaScript">

		function setSubmittedForParticipanteMPIGenerate(submittedFor,forwardTo){
			var noOfreg = '<%=noOrRowsCollectionProtocolRegistration%>';
            var vbirthDate = document.getElementById('birthDate');
			var vdeathDate = document.getElementById('deathDate');
            validateDate(vbirthDate);
			validateDate(vdeathDate);
			validateRegDate(noOfreg);
			document.forms[0].submittedFor.value = submittedFor;
			document.forms[0].forwardTo.value    = forwardTo;
			document.forms[0].action="<%=edu.wustl.common.participant.utility.Constants.PARTICIPANT_EMPI_GENERATION_ACTION%>";
			<%if (pageOf.equals(Constants.PAGE_OF_PARTICIPANT_CP_QUERY))
					{%>
				document.forms[0].action="<%=edu.wustl.common.participant.utility.Constants.CP_QUERY_PARTICIPANT_EMPI_GENERATION_ACTION%>";
			<%}%>
			document.forms[0].isGenerateEMPIID.value="yes";
			document.forms[0].submit();
		}

		
    	function setSubmittedForParticipant(submittedFor,forwardTo)
		{
			document.forms[0].submittedFor.value = submittedFor;
			document.forms[0].forwardTo.value    = forwardTo;
			<%if(request.getAttribute(Constants.SUBMITTED_FOR)!=null && request.getAttribute(Constants.SUBMITTED_FOR).equals("AddNew")){%>
				document.forms[0].submittedFor.value = "AddNew";
			<%}%>
			<%if(request.getAttribute(edu.wustl.simplequery.global.Constants.SPREADSHEET_DATA_LIST)!=null && dataList.size()>0){%>
				if(document.forms[0].radioValue.value=="Add")
				{
					document.forms[0].action="<%=Constants.PARTICIPANT_ADD_ACTION%>";
					<%if(pageOf.equals(Constants.PAGE_OF_PARTICIPANT_CP_QUERY))
					{
							if(operation.equals(Constants.ADD))
							{%>
							document.forms[0].action="<%=Constants.CP_QUERY_PARTICIPANT_ADD_ACTION%>";
						<%}
						else
							{%>
							document.forms[0].action="<%=Constants.CP_QUERY_PARTICIPANT_EDIT_ACTION%>";
						<%}
					}%>
				}
				else
				{
					if(document.forms[0].radioValue.value=="Lookup")
					{
						document.forms[0].action="<%=Constants.PARTICIPANT_LOOKUP_ACTION%>";
						<%if(pageOf.equals(Constants.PAGE_OF_PARTICIPANT_CP_QUERY))
						{%>
							document.forms[0].action="<%=Constants.CP_QUERY_PARTICIPANT_LOOKUP_ACTION%>";
						<%}%>
						document.forms[0].submit();
					}
				}
			<%}%>
	//setCollectionProtocolTitle();
	if((document.forms[0].activityStatus != undefined) && (document.forms[0].activityStatus.value == "Disabled"))
   	{
	    var go = confirm("Disabling any data will disable ALL its associated data also. Once disabled you will not be able to recover any of the data back from the system. Please refer to the user manual for more details. \n Do you really want to disable?");
		if (go==true)
		{
			var actionForward = document.forms[0].action;
			actionForward = actionForward + "?disableParticipant=true";
			document.forms[0].action = actionForward;
			document.forms[0].submit();
		}
	}
	else
	{
			checkActivityStatusForCPR();
	}
}
	

<logic:equal name="participantForm" property="pHIView" value="false">
      <%  
	   isSubmitDisabled = true;
      %>
</logic:equal>
</script>
<script language="JavaScript" type="text/javascript"
	src="jss/javaScript.js"></script>
<link href="css/catissue_suite.css" rel="stylesheet" type="text/css" />
<table width="100%" border="0" cellpadding="0" cellspacing="0"
	class="maintable" height="100%"><!-- Mandar 6Nov08 -->
	<tr>
		<td><input type="hidden" name="participantId" value="<%=participantId%>" /> 
			<input type="hidden" name="cpId" id="cpId" />
			<input type="hidden" name="radioValue" />
			
			<!--  Added by amol for eMPI integration  -->
          	<input type="hidden" name="clickedRowSelected" value=""/>
			<html:hidden property="empiId"/>
  			<input type="hidden" name="generateeMPIIdforPartiId" value=""/>
			<input type="hidden" name="clinPortalPartiId" value=""/>
			<html:hidden property="empiIdStatus"/>
  			<input type="hidden" name="isGenerateEMPIID" value=""/>
			<input type="hidden" name="isGenerateHL7" value=""/>
			<input type="hidden" name="isCaTissueLookUpNeeded" value="false"/>
			
            <html:hidden property="<%=Constants.OPERATION%>" value="<%=operation%>" /> 
			<html:hidden property="submittedFor" value="<%=submittedFor%>" /> 
			<html:hidden property="forwardTo" value="<%=forwardTo%>" />
			<html:hidden property="cprId"/>
		</td>
		<td><html:hidden property="valueCounter" /></td>
		<td><html:hidden
			property="collectionProtocolRegistrationValueCounter" /></td>
		<td><html:hidden property="onSubmit" /></td>
		<td><html:hidden property="id" /> <html:hidden
			property="redirectTo" /></td>
		<td><html:hidden property="pageOf" value="<%=pageOf%>" /></td>
	</tr>
		<logic:notEqual name="<%=Constants.PAGE_OF%>"
							value="<%=Constants.PAGE_OF_PARTICIPANT_CP_QUERY%>">
		<tr>
			<td class="td_color_bfdcf3">
			<table border="0" cellpadding="0" cellspacing="0">
				<tr>
					<td class="td_table_head"><span class="wh_ar_b"><bean:message
						key="app.participant" /></span></td>
					<td><img
						src="images/uIEnhancementImages/table_title_corner2.gif"
						alt="Page Title - Participant" width="31" height="24" /></td>
				</tr>
			</table>
			</td>
		</tr>
		</logic:notEqual>

	<tr height="98%">
		<td class="tablepadding" valign="top">
		<logic:equal name="operation" value="add">
			<logic:notEqual name="<%=Constants.PAGE_OF%>"
							value="<%=Constants.PAGE_OF_PARTICIPANT_CP_QUERY%>">
			<table width="100%" border="0" cellpadding="0" cellspacing="0">
				<tr>
					<td class="td_tab_bg"><img
						src="images/uIEnhancementImages/spacer.gif" alt="spacer"
						width="50" height="1"></td>
					<td valign="bottom"><img
						src="images/uIEnhancementImages/tab_add_selected.jpg" alt="Add"
						width="57" height="22" /></td>
					<td valign="bottom"><html:link
						page="/SimpleQueryInterface.do?pageOf=pageOfParticipant&aliasName=Participant&menuSelected=5">
						<img src="images/uIEnhancementImages/tab_edit_notSelected.jpg"
							alt="Edit" width="59" height="22" border="0" />
					</html:link></td>
					<td width="90%" valign="bottom" class="td_tab_bg">&nbsp;</td>
				</tr>
			</table>
				</logic:notEqual>
		</logic:equal>
		<!--for Edit-->
			<logic:equal name="operation" value="edit">
			<table width="100%" border="0" cellpadding="0" cellspacing="0">
				<tr>
					<td class="td_tab_bg"><img src="images/uIEnhancementImages/spacer.gif" alt="spacer" width="50" height="1"></td>
					<td valign="bottom"><img src="images/uIEnhancementImages/tab_edit_participant2.gif" alt="Edit Participant" width="116" height="22" border="0"></td>
					<td valign="bottom"><a href="#" onClick="viewSPR('<%=reportId%>','<%=pageOf%>')" id="viewSPR"><img src="images/uIEnhancementImages/tab_view_surgical2.gif" alt="View Surgical Pathology Report" width="216" height="22" border="0"></a></td>
					<td valign="bottom" ><html:link href="#" onclick="showAnnotations()" styleId="showAnnotation"><img src="images/uIEnhancementImages/tab_view_annotation2.gif" alt="View Annotation" width="116" height="22"  border="0"></html:link></td><td width="90%" valign="bottom" class="td_tab_bg">&nbsp;</td></tr></table>
			</logic:equal>
		<table width="100%" border="0" cellpadding="3" cellspacing="0"
			class="whitetable_bg" >
			<tr>
				<td colspan="2" align="left" class="bottomtd">

			<%if (request.getAttribute(Constants.SPREADSHEET_DATA_LIST) != null
									&& dataList.size() > 0){%>
				<tr>
					<td colspan="2" align="left" class="bottomtd"><%@ include
						file="/pages/content/common/caTissueParticipantRegActionErrors.jsp"%></td>
				</tr>
			<%}else{%>
			<tr>
				<td colspan="2" align="left" class="bottomtd"><%@ include
					file="/pages/content/common/ActionErrors.jsp"%></td>
			</tr>
           <%}%>
				</td>
			</tr>
	    	<tr>
				<td colspan="2" align="left" class="tr_bg_blue1"><span
					class="blue_ar_b">&nbsp;<bean:message
					key="participant.details" /></span></td>
			</tr>
			 <!--This included jsp file is used to include Participant matching grid. -->
			 <jsp:include page="/pages/content/manageBioSpecimen/ParticipantLookup.jsp"/>

			<tr>
				<td colspan="2" align="left" class="showhide" height="100%">
				<table width="100%" border="0" cellspacing="0" cellpadding="3" height="100%">
					<!-- Added by Amol -->
					<%if(pageView.equals("edit") && csEMPIStatus.equals("true") && generateeMPIButtonName!=null && !generateeMPIButtonName.equals("")){%>
						<tr>
							<td width="1%" align="center" class="black_ar">&nbsp;</td>
							<td width="17%"><label for="eMPIId"
									class="black_ar"> <bean:message
									key="participant.eMPIId" /> </label>
							</td>
							<td width="82%">
								<html:text styleClass="black_new" styleId="eMPIId" readonly="true"
								property="empiId"/>
							<%
									if( request.getAttribute(Constants.SPREADSHEET_DATA_LIST) == null ){
							%>
									<html:button styleClass="blue_ar_b"
											property="registratioPage"
											title="Submit Only"
											value="<%=generateeMPIButtonName%>"
											onclick="<%=normalSubmitForEMPIGenerate%>"
											disabled="<%=isGenerateEMPIDisabled%>"
											styleId="btnParticipantSubmit">
									</html:button>
							<%
									}

							%>
							</td>
						</tr>
						<%}%>
					
					
					<%
						if(!Variables.isSSNRemove) {
					%>
						<c:forEach var="attributeName" items="${participantAttributeDisplaySetInfo}">    	  
						 <c:if test="${attributeName == 'Social Security Number'}">
						
						<tr>
							<td width="1%" align="center" class="black_ar">&nbsp;</td>
							<td width="17%"><label for="socialSecurityNumber"
								class="black_ar"> <bean:message
								key="participant.socialSecurityNumber" /> <br />
							</label></td>
							<td width="82%"><html:text styleClass="black_ar" size="3"
								maxlength="3" styleId="socialSecurityNumberPartA"
								property="socialSecurityNumberPartA"
								readonly="<%=readOnlyForAll%>" onkeypress="intOnly(this);"
								onchange="intOnly(this);"
								onkeyup="intOnly(this);moveToNext(this,this.value,'socialSecurityNumberPartB');"
								style="text-align:right" /> - <html:text styleClass="black_ar"
								size="2" maxlength="2" styleId="socialSecurityNumberPartB"
								property="socialSecurityNumberPartB"
								readonly="<%=readOnlyForAll%>" onkeypress="intOnly(this);"
								onchange="intOnly(this);"
								onkeyup="intOnly(this);moveToNext(this,this.value,'socialSecurityNumberPartC');"
								style="text-align:right" /> - <html:text styleClass="black_ar"
								size="4" maxlength="4" styleId="socialSecurityNumberPartC"
								property="socialSecurityNumberPartC"
								readonly="<%=readOnlyForAll%>" onkeypress="intOnly(this);"
								onchange="intOnly(this);" onkeyup="intOnly(this);"
								style="text-align:right" /></td>
						</tr>
						</c:if>
						</c:forEach>   		
					<%
						}
					%>
					<tr>
						<td width="1%" align="center" class="black_ar">&nbsp;</td>
						<td class="black_ar"><bean:message
							key="participant.Name" /> </td>
						<td>
						<table width="35%" border="0" cellpadding="0" cellspacing="0">
							<tr>
								<td class="black_ar"><bean:message
											key="participant.lastName" /></br>

								<html:text styleClass="black_ar" maxlength="255"
											size="15" styleId="lastName" name="participantForm"
											property="lastName" readonly="<%=readOnlyForAll%>"
											onkeyup="moveToNext(this,this.value,'firstName')" /></td>
								<td width="2">&nbsp;</td>
								<td  class="black_ar"><bean:message
											key="participant.firstName" /></br><html:text styleClass="black_ar" maxlength="255"
											size="15" styleId="firstName" property="firstName"
											readonly="<%=readOnlyForAll%>"
											onkeyup="moveToNext(this,this.value,'middleName')" /></td>
								<td width="2">&nbsp;</td>
								<td  class="black_ar"><bean:message
											key="participant.middleName" /></br><html:text styleClass="black_ar" maxlength="255"
											size="15" styleId="middleName" property="middleName"
											readonly="<%=readOnlyForAll%>" /></td>
							</tr>
						</table>
						</td>
					</tr>
					<c:forEach var="attributeName" items="${participantAttributeDisplaySetInfo}">    						 <c:if test="${attributeName == 'Birth Date'}">
					<tr>
						<td width="1%" align="center" class="black_ar">&nbsp;</td>
						<td><label for="birthDate" class="black_ar"><bean:message
							key="participant.birthDate" /></label></td>
						<td>
						<html:text property="birthDate" styleClass="black_ar"
							styleId="birthDate" size="10"  
							value="<%=currentBirthDate%>" onclick="doInitCalendar('birthDate',false);" />
			
			           <span class="grey_ar_s capitalized"> [<bean:message key="date.pattern" />]</span>&nbsp;</td>
					</tr>
					</c:if>
					</c:forEach>
					<c:forEach var="attributeName" items="${participantAttributeDisplaySetInfo}">    						 <c:if test="${attributeName == 'Vital Status'}">	
					<tr>
						<td width="1%" align="center" class="black_ar">&nbsp;</td>
						<td><label for="vitalStatus" class="black_ar"><bean:message
							key="participant.vitalStatus" /></label></td>

						<td class="black_ar"><logic:iterate id="nvb"
							name="<%=Constants.VITAL_STATUS_LIST%>">
							<%
								NameValueBean nameValueBean = (NameValueBean) nvb;
							%>
							<html:radio property="vitalStatus"
								onclick="onVitalStatusRadioButtonClick(this)"
								value="<%=nameValueBean.getValue()%>">
								<%=nameValueBean.getName()%>
							</html:radio>&nbsp;&nbsp;&nbsp;
								</logic:iterate></td>
					</tr>
					<tr>
						<td width="1%" align="center" class="black_ar">&nbsp;</td>
						<td class="black_ar"><bean:message key="participant.deathDate" /></td>
						<td>
						<%
							
													Boolean deathDisable = new Boolean("false");
													if(form.getVitalStatus()!=null && !form.getVitalStatus().trim().equals("Dead")) {
														deathDisable = new Boolean("true");
													}
						%> 
													
					    <html:text property="deathDate" styleClass="black_ar"
							   styleId="deathDate" size="10" value="<%=currentDeathDate%>" 
                               disabled="<%=deathDisable%>" onclick="doInitCalendar('deathDate',false);" />								
					    <span class="grey_ar_s capitalized"> [<bean:message key="date.pattern" />]</span>&nbsp;</td>
					</tr>
					</c:if>
					</c:forEach>
					<c:forEach var="attributeName" items="${participantAttributeDisplaySetInfo}"> 
					 <c:if test="${attributeName == 'Gender'}">
					<tr>
						<td width="1%" align="center" class="black_ar">&nbsp;</td>
						<td class="black_ar"><bean:message
							key="participant.gender" /></td>
						<td class="black_ar"><logic:iterate id="nvb"
							name="<%=Constants.GENDER_LIST%>">
							<%
								NameValueBean nameValueBean = (NameValueBean) nvb;
							%>
							<html:radio property="gender"
								value="<%=nameValueBean.getValue()%>">
								<%=nameValueBean.getName()%>
							</html:radio>&nbsp; &nbsp;
								</logic:iterate></td>
					</tr>
					</c:if>
					</c:forEach>	
				<%
					if(!Variables.isSexGenoTypeRemove) {
				%>
				    <c:forEach var="attributeName" items="${participantAttributeDisplaySetInfo}">    						 <c:if test="${attributeName == 'Sex Genotype'}">
					<tr>
						<td width="1%" align="center" class="black_ar">&nbsp;</td>
						<td class="black_ar"><bean:message
							key="participant.genotype" /> </td>


						<td class="black_ar"><label><autocomplete:AutoCompleteTag
							property="genotype"
							optionsList="<%=request.getAttribute(Constants.GENOTYPE_LIST)%>"
							initialValue="<%=form.getGenotype()%>"
							styleClass="black_ar" size="27"/></label></td>

					</tr>
					</c:if>
					</c:forEach>
				<%
					}
				%>
				<%
					if(!Variables.isRaceRemove) {
				%>
					<c:forEach var="attributeName" items="${participantAttributeDisplaySetInfo}">    						 <c:if test="${attributeName == 'Race'}">
					<tr>
						<td width="1%" align="center" class="black_ar">&nbsp;</td>
						<td class="black_ar_t"><bean:message
							key="participant.race" /></td>

						<td class="black_ar"><html:select property="raceTypes"
							styleClass="formFieldSizedNew" styleId="race" size="4"
							multiple="true" disabled="<%=readOnlyForAll%>"
							onmouseover="showTip(this.id)" onmouseout="hideTip(this.id)">
							<html:options collection="<%=Constants.RACELIST%>"
								labelProperty="name" property="value" />
						</html:select></td>
					</tr>
					</c:if>
					</c:forEach>
				<%
					}
				%>
				<%
					if(!Variables.isEthnicityRemove){
				%>
					<c:forEach var="attributeName" items="${participantAttributeDisplaySetInfo}"> 
					 <c:if test="${attributeName == 'Ethnicity'}">
					<tr>
						<td width="1%" align="center" class="black_ar">&nbsp;</td>
						<td><span class="black_ar"><bean:message
							key="participant.ethnicity" /></span></td>
						<td class="black_ar"><label><autocomplete:AutoCompleteTag
							property="ethnicity"
							optionsList="<%=request.getAttribute(Constants.ETHNICITY_LIST)%>"
							initialValue="<%=form.getEthnicity()%>"
							styleClass="black_ar" size="27" /></label></td>
					</tr>
					</c:if>
					</c:forEach>
			   <%
			   	}
			   %>
			  
					<!-- activitystatus -->
					<logic:equal name="<%=Constants.OPERATION%>"
						value="<%=Constants.EDIT%>">
						<tr>
							<td width="1%" align="center" class="black_ar"><span class="blue_ar_b"><img src="images/uIEnhancementImages/star.gif" alt="Mandatory" width="6" height="6" hspace="0" vspace="0" /></span></td>

							<td valign="middle"><label for="activityStatus"
								class="black_ar"><bean:message
								key="participant.activityStatus" /></label></td>
							<td class="black_ar_s"><html:select
								property="activityStatus" styleClass="formFieldSizedNew"
								styleId="activityStatus" size="1" onchange="<%=strCheckStatus%>"
								onmouseover="showTip(this.id)" onmouseout="hideTip(this.id)">
								<html:options name="<%=Constants.ACTIVITYSTATUSLIST%>"
									labelName="<%=Constants.ACTIVITYSTATUSLIST%>" />
							</html:select></td>
						</tr>
					</logic:equal>

					<tr>
						<td width="1%" align="center" class="black_ar">&nbsp;</td>
						<td class="black_ar_t"><bean:message
							key="participant.collectionProtocolReg.participantProtocolID" /></td>

						<td class="black_ar"> <html:text property="ppId" styleClass="black_ar"
							   styleId="ppId" size="27" />			
							   
						</td>
					</tr>
					
					<tr>
						<td width="1%" align="center" class="black_ar">&nbsp;</td>
						<td class="black_ar_t"><bean:message
							key="participant.collectionProtocolReg.barcode" /></td>

						<td class="black_ar"> <html:text property="barcode" styleClass="black_ar"
							   styleId="barcode" size="27" />			
							   
						</td>
					</tr>
					
					<tr>
						<td width="1%" align="center" class="black_ar">&nbsp;</td>
						<td class="black_ar_t"><bean:message
							key="participant.collectionProtocolReg.participantRegistrationDate" /></td>

						<td class="black_ar"> 
							<html:text property="registrationDate" styleClass="black_ar"
							   styleId="registrationDate" size="10" onclick="doInitCalendar('birthDate',false);"/>	
							   <span class="grey_ar_s capitalized"> [<bean:message key="date.pattern" />]</span>&nbsp;</td>
						</td>
					</tr>
					
					
				</table>
				</td>
			</tr>

			<!-- Medical Identifiers Begin here -->
			<tr onclick="javascript:showHide('add_medical_identifier')">
				<td width="90%" class="tr_bg_blue1"><span class="blue_ar_b">&nbsp;<bean:message key="participant.medicalIdentifier" /> </span></td>
				<td width="10%" align="right" class="tr_bg_blue1"><a href="#"
					id="imgArrow_add_medical_identifier">
					<img src="images/uIEnhancementImages/dwn_arrow1.gif" alt="Show Details"
					width="80" height="9" hspace="10" border="0" />
				</a></td>
			</tr>
			<tr>
				<td colspan="2" class="showhide1">
				<div id="add_medical_identifier" style="display:none">
				<table width="100%" border="0" cellspacing="0" cellpadding="3">

					<tr class="tableheading">
						<td width="8%" align="left" class="black_ar_b"><bean:message
							key="app.select" /></td>
						<td width="24%" align="left" class="black_ar_b"><bean:message
							key="medicalrecord.source" /></td>
						<td width="68%" align="left" class="black_ar_b"><bean:message
							key="medicalrecord.number" /></td>
					</tr>
					<script> document.forms[0].valueCounter.value = <%=noOfRows%> </script>
					<tbody id="addMore">
						<%
							for (int i = 1; i <= noOfRows; i++) {
														String siteName = "value(ParticipantMedicalIdentifier:" + i
														+ "_Site_id)";
														String medicalRecordNumber = "value(ParticipantMedicalIdentifier:"
														+ i + "_medicalRecordNumber)";
														String identifier = "value(ParticipantMedicalIdentifier:" + i
														+ "_id)";
														String check = "chk_" + i;
						%>
						<tr>
							<%
								String key = "ParticipantMedicalIdentifier:" + i + "_id";
																boolean bool = edu.wustl.common.util.Utility.isPersistedValue(map, key);
																String condition = "";
																if (bool)
																	condition = "disabled='disabled'";
							%>
							<td align="left" class="black_ar"><html:hidden
								property="<%=identifier%>" /> <input type="checkbox"
								name="<%=check%>" id="<%=check%>" <%=condition%>
								onClick="enableButton(document.forms[0].deleteMedicalIdentifierValue,document.forms[0].valueCounter,'chk_')">
							</td>
							<td nowrap class="black_ar"><html:select
								property="<%=siteName%>" styleClass="formFieldSized12"
								styleId="<%=siteName%>" size="1" disabled="<%=readOnlyForAll%>"
								onmouseover="showTip(this.id)" onmouseout="hideTip(this.id)">
								<html:options collection="<%=Constants.SITELIST%>"
									labelProperty="name" property="value" />
							</html:select></td>
							<td class="black_ar"><html:text styleClass="black_ar"
								size="15" styleId="<%=medicalRecordNumber%>"
								property="<%=medicalRecordNumber%>"
								readonly="<%=readOnlyForAll%>"  /></td>
						</tr>
						<%
							}
						%>
					</tbody>
					<!-- Medical Identifiers End here -->
					<tr>
						<td align="left" class="black_ar"><html:button
							property="addKeyValue" styleClass="black_ar"
							onclick="insRow('addMore')" disabled="<%=isSubmitDisabled%>">
							<bean:message key="buttons.addMore" />
						</html:button></td>
						<td class="black_ar"><html:button
							property="deleteMedicalIdentifierValue" styleClass="black_ar"
							onclick="deleteChecked('addMore','Participant.do?operation=<%=operation%>&pageOf=<%=pageOf%>&status=true',document.forms[0].valueCounter,'chk_',false)"
							disabled="true">
							<bean:message key="buttons.delete" />
						</html:button></td>
						<td class="black_ar">&nbsp;</td>
					</tr>

				</table>
				</div>
				</td>
			</tr>
			<tr>
				<td colspan="2" class="bottomtd"></td>
			</tr>
			<tr>
				<td colspan="2" class="buttonbg">
				<% String changeAction = "setFormAction('" + formName + "')";
						%><!-- action buttons begins -->
				<table cellpadding="0" cellspacing="0" border="0">
					<logic:equal name="<%=Constants.SUBMITTED_FOR%>" value="AddNew">
						<%
								isAddNew = true;
								%>
					</logic:equal>
					<tr>
						<%--
									String normalSubmitFunctionName = "setSubmittedForParticipant('" + submittedFor+ "','" + Constants.PARTICIPANT_FORWARD_TO_LIST[0][1]+"')";
									String forwardToSubmitFunctionName = "setSubmittedForParticipant('ForwardTo','" + Constants.PARTICIPANT_FORWARD_TO_LIST[1][1]+"')";
									String confirmDisableFuncName = "confirmDisable('" + formName +"',document.forms[0].activityStatus)";
									String normalSubmit = normalSubmitFunctionName + ","+confirmDisableFuncName;
									String forwardToSubmit = forwardToSubmitFunctionName + ","+confirmDisableFuncName;
								--%>
						<%
											String normalSubmitFunctionName = "setSubmittedForParticipant('"
											+ submittedFor + "','"
											+ Constants.PARTICIPANT_FORWARD_TO_LIST[0][1] + "')";
									String forwardToSubmitFunctionName = "setSubmittedForParticipant('ForwardTo','"
											+ Constants.PARTICIPANT_FORWARD_TO_LIST[3][1] + "')";
									String forwardToSCGFunctionName = "setSubmittedForParticipant('ForwardTo','"
											+ Constants.PARTICIPANT_FORWARD_TO_LIST[2][1] + "')";
									String normalSubmit = normalSubmitFunctionName;
									String forwardToSubmit = forwardToSubmitFunctionName;
									String forwardToSCG = forwardToSCGFunctionName;
									String normalSubmitForcaTissueParticipantMatch = "setSubmittedForCaTissueParticipantMatch('" + submittedFor + "','"+ Constants.PARTICIPANT_FORWARD_TO_LIST[0][1] + "')";
									
								%>
						<!-- PUT YOUR COMMENT HERE -->
						<logic:equal name="<%=Constants.PAGE_OF%>"
							value="<%=Constants.PAGE_OF_PARTICIPANT_CP_QUERY%>">
                         <td nowrap >
							<html:button
								styleClass="blue_ar_b" property="registratioPage"
								title="Register Participant"
								value="<%=Constants.PARTICIPANT_FORWARD_TO_LIST[0][0]%>"
								onclick="<%=forwardToSubmit%>"
								disabled="<%=isSubmitDisabled%>"
								>
							</html:button>
						</td>
						</logic:equal>
						<logic:notEqual name="<%=Constants.PAGE_OF%>"
							value="<%=Constants.PAGE_OF_PARTICIPANT_CP_QUERY%>">
							<%
if(request.getAttribute("ZERO_MATCHES") != null)
{
	isSubmitDisabled = true;
}
%>
							<td>
                                <html:button styleClass="blue_ar_b"
								property="registratioPage" title="Submit Only"
								onclick="<%=normalSubmit%>"
								disabled="<%=isSubmitDisabled%>"
								>
								
								<bean:message key="buttons.submit" />
								</html:button>
								<!-- delete button added for deleting the objects -->
								<logic:equal name="operation" value="edit">
									<%
										String deleteAction="deleteObject('" + formName +"','" + Constants.ADMINISTRATIVE + "')";
									%>
									|&nbsp;<html:button styleClass="blue_ar_c" property="disableRecord"
										title="Delete" value="Delete" onclick="<%=deleteAction%>"
										disabled="<%=isSubmitDisabled%>"
										>
									</html:button>
								</logic:equal>
							</td>
						</logic:notEqual>
						<logic:equal name="<%=Constants.PAGE_OF%>"
							value="<%=Constants.PAGE_OF_PARTICIPANT_CP_QUERY%>">
							<td nowrap>&nbsp;|&nbsp;<html:button styleClass="blue_ar_c"
								property="registratioPage"
								value="<%=Constants.PARTICIPANT_FORWARD_TO_LIST[2][0]%>"
								onclick="<%=forwardToSCG%>"
								disabled="<%=isSubmitDisabled%>"
								onmouseover="showMessage('Create additional Specimen Collection Group to collect specimens which were  not anticipated as per protocol')">
							</html:button>
							<logic:equal name="operation" value="edit">
										|<%
										String deleteAction="deleteObject('" + formName +"','" + Constants.CP_QUERY_BIO_SPECIMEN + "')";
									%>
								<html:button styleClass="blue_ar_c" property="disableRecord"
									title="Disable Participant" value="Delete" onclick="<%=deleteAction%>" disabled="<%=isSubmitDisabled%>">
								</html:button>&nbsp;
							 </logic:equal>
							 </td>
						</logic:equal>
						<logic:equal name="<%=edu.wustl.common.participant.utility.Constants.IS_GENERATE_EMPI_PAGE%>"
							value="true">
							<td nowrap>&nbsp;|
								<input type="button" style="width:70"
									class="blue_ar_c"
									name="ProNextParticipant"
									title="Proceed to the next participant"
									value="Skip"
									onclick="processNextPartForEMPI();"
									id="ProNextParticipant">
								</input>
								</td>
						</logic:equal>
						
					</tr>
				</table>
				<!-- action buttons end --></td>
			</tr>
		</table>
		</td>
	</tr>
</table>
