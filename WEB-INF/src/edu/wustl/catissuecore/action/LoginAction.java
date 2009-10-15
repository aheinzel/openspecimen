package edu.wustl.catissuecore.action;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.xml.sax.SAXException;

import edu.wustl.catissuecore.actionForm.LoginForm;
import edu.wustl.catissuecore.bizlogic.UserBizLogic;
import edu.wustl.catissuecore.domain.User;
import edu.wustl.catissuecore.util.global.Constants;
import edu.wustl.common.beans.SessionDataBean;
import edu.wustl.common.bizlogic.DefaultBizLogic;
import edu.wustl.common.exception.ApplicationException;
import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.factory.AbstractFactoryConfig;
import edu.wustl.common.factory.IFactory;
import edu.wustl.common.util.XMLPropertyHandler;
import edu.wustl.common.util.global.Status;
import edu.wustl.common.util.logger.Logger;
import edu.wustl.domauthmgr.AuthenticationManagerFactory;
import edu.wustl.domauthmgr.authdomain.AuthenticationManager;
import edu.wustl.domauthmgr.authdomain.util.AuthenticationException;
import edu.wustl.domauthmgr.authdomain.util.InitializationException;
import edu.wustl.security.exception.SMException;
import edu.wustl.security.global.Roles;
import edu.wustl.security.manager.ISecurityManager;
import edu.wustl.security.manager.SecurityManagerFactory;
import edu.wustl.security.privilege.PrivilegeManager;
import edu.wustl.wustlkey.util.global.WUSTLKeyUtility;

/**
 * <p>
 * Title:
 * </p>
 *<p>
 * Description:
 * </p>
 *<p>
 * Copyright: (c) Washington University, School of Medicine 2005
 * </p>
 *<p>
 * Company: Washington University, School of Medicine, St. Louis.
 * </p>
 *
 * @author Aarti Sharma
 *@version 1.0
 */
public class LoginAction extends Action
{

	/**
	 * Common Logger for Login Action.
	 */
	private static final Logger logger = Logger.getCommonLogger(LoginAction.class);

	/**
	 * Overrides the execute method of Action class.
	 * @param mapping
	 *            object of ActionMapping
	 * @param form
	 *            object of ActionForm
	 * @param request
	 *            object of HttpServletRequest
	 * @param response
	 *            object of HttpServletResponse
	 * @return value for ActionForward object
	 */
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
	{
		if (form == null)
		{
			logger.debug("Form is Null");
			return mapping.findForward(Constants.FAILURE);
		}
		try
		{
			cleanSession(request);
			logger.info("Inside Login Action, Just before validation");
			final LoginForm loginForm = (LoginForm) form;
			String loginName = loginForm.getLoginName();
			final String userStatus = getUserStatus(loginName);
			if (!edu.wustl.wustlkey.util.global.Constants.CSM.equals(userStatus))
			{
				loginName = userStatus;
			}
			final String forwardTo = authenticateUser(request, loginForm, userStatus);
			if (Constants.SUCCESS.equals(forwardTo))
			{
				return validateUser(mapping, request, loginForm, loginName);
			}
			else
			{
				return mapping.findForward(forwardTo);
			}
		}
		catch (final Exception ex)
		{
			logger.info("Exception: " + ex.getMessage(), ex);
			cleanSession(request);
			handleError(request, "errors.incorrectLoginIDPassword");
			return mapping.findForward(Constants.FAILURE);
		}
	}

	/**
	 * @param loginName user login Name
	 * @return return UserStatus
	 * @throws ApplicationException object of ApplicationException
	 */
	private String getUserStatus(String loginName) throws ApplicationException
	{
		String userStatus = edu.wustl.wustlkey.util.global.Constants.CSM;
		final boolean isIDPEnabled = Boolean.parseBoolean(XMLPropertyHandler
				.getValue(Constants.IDP_ENABLED));
		if (isIDPEnabled)
		{
			userStatus = WUSTLKeyUtility.checkForMigratedUser(loginName,
					edu.wustl.wustlkey.util.global.Constants.APPLICATION_NAME);
		}
		return userStatus;
	}

	/**

	 * @param request
	 * 		  object of HttpServletRequest
	 * @param loginForm
	 * 		  object of LoginForm
	 * @param userStatus
	 * 		  Status of logged in user
	 * @return String forwardTo
	 * @throws IOException IO Exception
	 * @throws SAXException SAXException
	 * @throws ParserConfigurationException ParserConfigurationException
	 * @throws InitializationException InitializationException
	 * @throws NamingException  NamingException
	 * @throws AuthenticationException  AuthenticationException
	 * @throws SMException SMException
	 */
	private String authenticateUser(HttpServletRequest request, LoginForm loginForm,
			String userStatus) throws IOException, InitializationException,
			ParserConfigurationException, SAXException, SMException, AuthenticationException,
			NamingException
			{
		String forwardTo = Constants.SUCCESS;
		AuthenticationManager authManager = null;
		AuthenticationManagerFactory factory = null;
		final String absolutePath = System.getProperty("app.domainAuthFilePath");
		final String authType = authenticationType(userStatus);
		boolean loginOK = false;
		if (edu.wustl.wustlkey.util.global.Constants.MIGRATED_TO_WUSTLKEY.equals(userStatus))
		{
			handleError(request, "app.migrateduser");
			forwardTo = Constants.FAILURE;
		}
		else
		{

			factory = AuthenticationManagerFactory.getAuthenticationManagerFactory();
			authManager = factory.getAuthenticationManager(authType, absolutePath);
			loginOK = authManager.authenticate(loginForm.getLoginName(), loginForm.getPassword());
			if (loginOK)
			{
				if (edu.wustl.wustlkey.util.global.Constants.NEW_WASHU_USER.equals(userStatus))
				{
					final Map<String, String> userAttrs =
						authManager.getUserAttributes(loginForm.getLoginName());
					final String firstName = userAttrs.get(Constants.FIRSTNAME);
					final String lastName = userAttrs.get(Constants.LASTNAME);
					request.setAttribute(edu.wustl.wustlkey.util.global.Constants.WUSTLKEY,
							loginForm.getLoginName());
					request.setAttribute(edu.wustl.wustlkey.util.global.Constants.FIRST_NAME,
							firstName);
					request.setAttribute(edu.wustl.wustlkey.util.global.Constants.LAST_NAME,
							lastName);
					forwardTo = edu.wustl.wustlkey.util.global.Constants.WASHU;
				}
			}
			else
			{
				logger.info("User " + loginForm.getLoginName()
						+ " Invalid user. Sending back to the login Page");
				handleError(request, "errors.incorrectLoginIDPassword");
				forwardTo = Constants.FAILURE;
			}
		}
		return forwardTo;
			}

	/**
	 * This method will determine the type of AuthenticationManager i.e. WASHU_LDAP or CSM
	 * @param userStatus
	 * 			Status of logged in user
	 * @return authenticationType
	 */
	private String authenticationType(String userStatus)
	{
		String authenticationType = edu.wustl.wustlkey.util.global.Constants.CSM;
		if (!edu.wustl.wustlkey.util.global.Constants.CSM.equals(userStatus))
		{
			authenticationType = edu.wustl.wustlkey.util.global.Constants.WASHU_LDAP;
		}
		return authenticationType;
	}

	/**
	 * This method will clean session.
	 * @param request
	 * 		  object of HttpServletRequest
	 */
	private void cleanSession(HttpServletRequest request)
	{
		final HttpSession prevSession = request.getSession();
		if (prevSession != null)
		{
			prevSession.invalidate();
		}
	}

	/**
	 * This method checks the validity of logged in user and perform necessary action after validating.
	 * @param mapping
	 * 				object of ActionMapping
	 * @param request
	 * 				object of  HttpServletRequest
	 * @param loginName
	 * 				login Name
	 * @param loginForm
	 * 				Login Form
	 * @return value for ActionForward object
	 * @throws ApplicationException object of ApplicationException
	 */
	private ActionForward validateUser(ActionMapping mapping, HttpServletRequest request,
			LoginForm loginForm, String loginName) throws ApplicationException
			{

		final User validUser = getUser(loginName);
		if (validUser != null)
		{
			return performAdminChecks(mapping, request, loginForm, validUser);
		}
		else
		{
			logger.debug("User " + loginName + " Invalid user. Sending back to the login Page");
			handleError(request, "errors.incorrectLoginIDPassword");
			return mapping.findForward(Constants.FAILURE);
		}
			}

	/**
	 * This method will create object of privilege cache for logged in user and create SessionDataBean object.
	 * @param mapping
	 * 				object of ActionMapping
	 * @param request
	 * 				object of  HttpServletRequest
	 * @param validUser
	 * 			User object
	 * @param loginForm
	 * 			Login Form
	 * @return
	 * 			value for ActionForward object
	 * @throws ApplicationException Object of ApplicationException
	 */
	private ActionForward performAdminChecks(ActionMapping mapping, HttpServletRequest request,
			LoginForm loginForm, final User validUser) throws ApplicationException
			{
		PrivilegeManager.getInstance().getPrivilegeCache(validUser.getLoginName());
		logger.info(">>>>>>>>>>>>> SUCESSFUL LOGIN A <<<<<<<<< ");
		final HttpSession session = request.getSession(true);
		final String ipAddress = request.getRemoteAddr();
		boolean adminUser = false;
		if (validUser.getRoleId().equalsIgnoreCase(Constants.ADMIN_USER))
		{
			adminUser = true;
		}
		final SessionDataBean sessionData = setSessionDataBean(validUser, ipAddress, adminUser);
		logger.debug("CSM USer ID ....................... : " + validUser.getCsmUserId());
		session.setAttribute(Constants.SESSION_DATA, sessionData);
		session.setAttribute(Constants.USER_ROLE, validUser.getRoleId());
		String result = passExpCheck(loginForm, validUser);
		setSecurityParamsInSessionData(validUser, sessionData);
		final String validRole = getForwardToPageOnLogin(validUser.getCsmUserId().longValue());
		if (validRole != null && validRole.contains(Constants.PAGE_OF_SCIENTIST))
		{
			handleError(request, "errors.noRole");
			session.setAttribute(Constants.SESSION_DATA, null);
			return mapping.findForward(Constants.FAILURE);
		}
		if (!result.equals(Constants.SUCCESS))
		{
			handleCustomMessage(request, result);
			session.setAttribute(Constants.SESSION_DATA, null);
			session.setAttribute(Constants.TEMP_SESSION_DATA, sessionData);
			request.setAttribute(Constants.PAGE_OF, Constants.PAGE_OF_CHANGE_PASSWORD);
			return mapping.findForward(Constants.ACCESS_DENIED);
		}
		String forwardToPage = edu.wustl.wustlkey.util.global.Constants.PAGE_NON_WASHU;
		if(Boolean.parseBoolean(XMLPropertyHandler
				.getValue(Constants.IDP_ENABLED)))
		{
			forwardToPage = WUSTLKeyUtility.migrate(request, loginForm.getLoginName(),
					edu.wustl.wustlkey.util.global.Constants.APPLICATION_NAME);
		}
		return mapping.findForward(forwardToPage);
	}
	/**
	 * This method will check the expiry of the password.
	 * @param loginForm LoginForm
	 * @param validUser Object of valid user
	 * @return result
	 * @throws BizLogicException BizLogic Exception
	 */
	private String passExpCheck(LoginForm loginForm, final User validUser) throws BizLogicException
	{
		String result = Constants.SUCCESS;
		final IFactory factory = AbstractFactoryConfig.getInstance().getBizLogicFactory();
		final UserBizLogic userBizLogic = (UserBizLogic) factory
		.getBizLogic(Constants.USER_FORM_ID);
		if(Boolean.parseBoolean(XMLPropertyHandler
				.getValue(Constants.IDP_ENABLED)))
		{
			result = wustlLogicForPass(loginForm, validUser, result, userBizLogic);
		}
		else
		{
			result = userBizLogic.checkFirstLoginAndExpiry(validUser);
		}
		return result;
	}
	/**
	 * WustlKey logic for password checking.
	 * @param loginForm LoginForm
	 * @param validUser user object
	 * @param result result String
	 * @param userBizLogic user Bizlogic object
	 * @return result
	 */
	private String wustlLogicForPass(LoginForm loginForm, final User validUser, String result,
			final UserBizLogic userBizLogic)
	{
		if (edu.wustl.wustlkey.util.global.Constants.NON_WASHU.equals(WUSTLKeyUtility
				.getUserFrom(loginForm.getLoginName())))
		{
			result = userBizLogic.checkFirstLoginAndExpiry(validUser);
		}
		else if (edu.wustl.wustlkey.util.global.Constants.WASHU.equals(WUSTLKeyUtility
				.getUserFrom(loginForm.getLoginName())))
		{
			result = Constants.SUCCESS;
			boolean firstTimeLogin = userBizLogic.getFirstLogin(validUser);
			if (firstTimeLogin)
			{
				result="errors.changePassword.changeFirstLogin";
			}
		}
		return result;
	}

	/**
	 * This method will createSessionDataBeanObject.
	 * @param validUser existing user
	 * @param ipAddress IP address of the system
	 * @param adminUser true/false
	 * @return sessionData
	 */
	private SessionDataBean setSessionDataBean(final User validUser, final String ipAddress,
			boolean adminUser)
	{
		final SessionDataBean sessionData = new SessionDataBean();
		sessionData.setAdmin(adminUser);
		sessionData.setUserName(validUser.getLoginName());
		sessionData.setIpAddress(ipAddress);
		sessionData.setUserId(validUser.getId());
		sessionData.setFirstName(validUser.getFirstName());
		sessionData.setLastName(validUser.getLastName());
		sessionData.setCsmUserId(validUser.getCsmUserId().toString());
		return sessionData;
	}

	/**
	 * To set the Security Parameters in the given SessionDataBean object
	 * depending upon the role of the user.
	 *
	 * @param validUser
	 *            reference to the User.
	 * @param sessionData
	 *            The reference to the SessionDataBean object.
	 * @throws SMException : SMException
	 */
	private void setSecurityParamsInSessionData(User validUser, SessionDataBean sessionData)
	throws SMException
	{
		final String userRole = SecurityManagerFactory.getSecurityManager().getRoleName(
				validUser.getCsmUserId());
		if (userRole != null
				&& (userRole.equalsIgnoreCase(Roles.ADMINISTRATOR) || userRole
						.equals(Roles.SUPERVISOR)))
		{
			sessionData.setSecurityRequired(false);
		}
		else
		{
			sessionData.setSecurityRequired(true);
		}
	}

	/**
	 * Patch ID: 3842_2 This function will take LoginID for user and return the
	 * appropriate default page. Get role from securitymanager and modify the
	 * role name where first character is in upper case and rest all are in
	 * lower case add prefix "pageOf" to modified role name and forward to that
	 * page.
	 *
	 * @param loginId : loginId
	 * @return String : String
	 * @throws SMException : SMException
	 */

	private String getForwardToPageOnLogin(Long loginId) throws SMException
	{
		final ISecurityManager securityManager = SecurityManagerFactory.getSecurityManager();
		final String roleName = securityManager.getRoleName(loginId);
		String modifiedRolename = "";
		if (roleName == null || roleName.equals(""))
		{
			modifiedRolename = "pageOfScientist";
		}
		else
		{
			modifiedRolename = "pageOfAdministrator";
		}
		return modifiedRolename;
	}

	/**
	 *
	 * @param request : request
	 * @param errorKey : errorKey
	 */
	private void handleError(HttpServletRequest request, String errorKey)
	{
		final ActionErrors errors = new ActionErrors();
		errors.add(ActionErrors.GLOBAL_ERROR, new ActionError(errorKey));
		// Report any errors we have discovered
		if (!errors.isEmpty())
		{
			saveErrors(request, errors);
		}
	}

	/**
	 * This method is for showing Custom message.
	 * @param request HttpServletRequest
	 * @param errorMsg Error message
	 */
	private void handleCustomMessage(HttpServletRequest request, String errorMsg)
	{
		final ActionMessages msg = new ActionMessages();
		final ActionMessage msgs = new ActionMessage(errorMsg);
		msg.add(ActionErrors.GLOBAL_ERROR, msgs);
		if (!msg.isEmpty())
		{
			saveMessages(request, msg);
		}
	}

	/**
	 *
	 * @param loginName : loginName
	 * @return User : User
	 * @throws BizLogicException : BizLogicException
	 */
	private User getUser(String loginName) throws BizLogicException
	{
		User validUser = null;
		final String getActiveUser = "from edu.wustl.catissuecore.domain.User user where "
			+ "user.activityStatus= " + "'" + Status.ACTIVITY_STATUS_ACTIVE.toString()
			+ "' and user.loginName =" + "'" + loginName + "'";
		final DefaultBizLogic bizlogic = new DefaultBizLogic();
		final List<User> users = bizlogic.executeQuery(getActiveUser);
		if (users != null && !users.isEmpty())
		{
			validUser = users.get(0);
		}
		return validUser;
	}
}