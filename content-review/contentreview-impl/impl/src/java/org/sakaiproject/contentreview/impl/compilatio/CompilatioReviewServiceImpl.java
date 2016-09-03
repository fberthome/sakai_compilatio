/**********************************************************************************
 *
 * Copyright (c) 2006 Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/
package org.sakaiproject.contentreview.impl.compilatio;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.validator.EmailValidator;
import org.sakaiproject.api.common.edu.person.SakaiPerson;
import org.sakaiproject.api.common.edu.person.SakaiPersonManager;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.assignment.api.AssignmentContent;
import org.sakaiproject.assignment.api.AssignmentService;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.contentreview.dao.impl.ContentReviewDao;
import org.sakaiproject.contentreview.exception.QueueException;
import org.sakaiproject.contentreview.exception.ReportException;
import org.sakaiproject.contentreview.exception.SubmissionException;
import org.sakaiproject.contentreview.exception.TransientSubmissionException;
import org.sakaiproject.contentreview.impl.hbm.BaseReviewServiceImpl;
import org.sakaiproject.contentreview.model.ContentReviewItem;
import org.sakaiproject.contentreview.model.ContentReviewItem.Error;
import org.sakaiproject.db.api.SqlReader;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.genericdao.api.search.Restriction;
import org.sakaiproject.genericdao.api.search.Search;
import org.sakaiproject.service.gradebook.shared.GradebookExternalAssessmentService;
import org.sakaiproject.service.gradebook.shared.GradebookNotFoundException;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.cover.TimeService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.compilatio.util.CompilatioAPIUtil;
import org.sakaiproject.user.api.PreferencesService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.ResourceLoader;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class CompilatioReviewServiceImpl extends BaseReviewServiceImpl {

	private static final Log log = LogFactory.getLog(CompilatioReviewServiceImpl.class);

	public static final String COMPILATIO_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

	private static final String SERVICE_NAME = "Compilatio";

	// Site property to enable or disable use of Compilatio for the site
	private static final String COMPILATIO_SITE_PROPERTY = "compilatio";

	final static long LOCK_PERIOD = 12000000;

	private Long maxRetry = 20L;

	private CompilatioAccountConnection compilatioConn;

	public void setCompilatioConn(CompilatioAccountConnection compilatioConn) {
		this.compilatioConn = compilatioConn;
	}

	/**
	 * Setters
	 */

	private ServerConfigurationService serverConfigurationService;

	public void setServerConfigurationService(ServerConfigurationService serverConfigurationService) {
		this.serverConfigurationService = serverConfigurationService;
	}

	private EntityManager entityManager;

	public void setEntityManager(EntityManager en) {
		this.entityManager = en;
	}

	private ContentHostingService contentHostingService;

	private AssignmentService assignmentService;

	public void setContentHostingService(ContentHostingService contentHostingService) {
		this.contentHostingService = contentHostingService;
	}

	public void setAssignmentService(AssignmentService assignmentService) {
		this.assignmentService = assignmentService;
	}

	private SakaiPersonManager sakaiPersonManager;

	public void setSakaiPersonManager(SakaiPersonManager s) {
		this.sakaiPersonManager = s;
	}

	private ContentReviewDao dao;

	public void setDao(ContentReviewDao dao) {
		super.setDao(dao);
		this.dao = dao;
	}

	private UserDirectoryService userDirectoryService;

	public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
		super.setUserDirectoryService(userDirectoryService);
		this.userDirectoryService = userDirectoryService;
	}

	private SiteService siteService;

	public void setSiteService(SiteService siteService) {
		this.siteService = siteService;
	}

	private SqlService sqlService;

	public void setSqlService(SqlService sql) {
		sqlService = sql;
	}

	private PreferencesService preferencesService;

	public void setPreferencesService(PreferencesService preferencesService) {
		this.preferencesService = preferencesService;
	}

	private CompilatioContentValidator compilatioContentValidator;

	public void setCompilatioContentValidator(CompilatioContentValidator compilatioContentValidator) {
		this.compilatioContentValidator = compilatioContentValidator;
	}

	/**
	 * If set to true in properties, will result in 3 random digits being
	 * appended to the email name. In other words, adrian.r.fish@gmail.com will
	 * become something like adrian.r.fish593@gmail.com
	 */
	private boolean spoilEmailAddresses = false;

	private SecurityService securityService = (SecurityService) ComponentManager.get(SecurityService.class.getName());
	private SessionManager sessionManager = (SessionManager) ComponentManager.get(SessionManager.class.getName());

	/**
	 * Place any code that should run when this class is initialized by spring
	 * here
	 */

	public void init() {

		log.info("init()");

		if (siteAdvisor != null) {
			log.info("Using siteAdvisor: " + siteAdvisor.getClass().getName());
		}

	}

	public String getServiceName() {
		return SERVICE_NAME;
	}

	/**
	 * Allow Compilatio for this site?
	 */
	public boolean isSiteAcceptable(Site s) {

		if (s == null) {
			return false;
		}

		log.debug("isSiteAcceptable: " + s.getId() + " / " + s.getTitle());

		// Delegated to another bean
		if (siteAdvisor != null) {
			return siteAdvisor.siteCanUseReviewService(s);
		}

		// Check site property
		ResourceProperties properties = s.getProperties();

		String prop = (String) properties.get(COMPILATIO_SITE_PROPERTY);
		if (prop != null) {
			log.debug("Using site property: " + prop);
			return Boolean.parseBoolean(prop);
		}

		// No property set, no restriction on site types, so allow
		return true;
	}

	public String getIconUrlforScore(Long score) {

		String urlBase = "/sakai-contentreview-tool-compilatio/images/compilatio_";
		String suffix = ".png";

		if (score.compareTo(Long.valueOf(5)) <= 0) {
			return urlBase + "green" + suffix;
		} else if (score.compareTo(Long.valueOf(20)) <= 0) {
			return urlBase + "orange" + suffix;
		} else {
			return urlBase + "red" + suffix;
		}

	}

	@Override
	public String getReviewReportInstructor(String contentId) throws QueueException, ReportException {
		return getReviewReport(contentId);
	}
	
	@Override	
	public String getReviewReportStudent(String contentId) throws QueueException, ReportException {
		return getReviewReport(contentId);
	}
	
	public String getReviewReport(String contentId) throws QueueException, ReportException {

		Search search = new Search();
		search.addRestriction(new Restriction("contentId", contentId));
		List<ContentReviewItem> matchingItems = dao.findBySearch(ContentReviewItem.class, search);
		if (matchingItems.size() == 0) {
			log.debug("Content " + contentId + " has not been queued previously");
			throw new QueueException("Content " + contentId + " has not been queued previously");
		}

		if (matchingItems.size() > 1)
			log.debug("More than one matching item found - using first item found");

		// check that the report is available
		// TODO if the database record does not show report available check with
		// compilatio (maybe)

		ContentReviewItem item = (ContentReviewItem) matchingItems.iterator().next();
		if (item.getStatus().compareTo(ContentReviewItem.SUBMITTED_REPORT_AVAILABLE_CODE) != 0) {
			log.debug("Report not available: " + item.getStatus());
			throw new ReportException("Report not available: " + item.getStatus());
		}

		// report is available - generate the URL to display
		Map params = CompilatioAPIUtil.packMap("action", "getDocumentReportURL", "idDocument", item.getExternalId());

		String reportURL = null;
		try {
			Document reportURLDoc = compilatioConn.callCompilatioReturnDocument(params);
			boolean successQuery = reportURLDoc.getElementsByTagName("success") != null;
			if (successQuery) {
				reportURL = ((CharacterData) (reportURLDoc.getElementsByTagName("success").item(0).getFirstChild()))
						.getData();
			}

		} catch (TransientSubmissionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SubmissionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return reportURL;
	}

	private List<ContentReviewItem> getItemsByContentId(String contentId) {
		Search search = new Search();
		search.addRestriction(new Restriction("contentId", contentId));
		List<ContentReviewItem> existingItems = dao.findBySearch(ContentReviewItem.class, search);
		return existingItems;
	}

	private List<ContentReviewItem> getItemsByExternalId(String externalId) {
		Search search = new Search();
		search.addRestriction(new Restriction("externalId", externalId));
		List<ContentReviewItem> existingItems = dao.findBySearch(ContentReviewItem.class, search);
		return existingItems;
	}

	/**
	 * Get additional data from String if available
	 * 
	 * @return array containing site ID, Task ID, Task Title
	 */
	private String[] getAssignData(String data) {
		String[] assignData = null;
		try {
			if (data.contains("#")) {
				assignData = data.split("#");
			}
		} catch (Exception e) {
		}
		return assignData;
	}

	public String getInlineTextId(String assignmentReference, String userId, long submissionTime) {
		return "";
	}

	public boolean acceptInlineAndMultipleAttachments() {
		return false;
	}

	public int getReviewScore(String contentId, String assignmentRef, String userId)
			throws QueueException, ReportException, Exception {
		ContentReviewItem item = null;
		try {
			List<ContentReviewItem> matchingItems = getItemsByContentId(contentId);
			if (matchingItems.size() == 0) {
				log.debug("Content " + contentId + " has not been queued previously");
			}
			if (matchingItems.size() > 1)
				log.debug("More than one matching item - using first item found");

			item = (ContentReviewItem) matchingItems.iterator().next();
			if (item.getStatus().compareTo(ContentReviewItem.SUBMITTED_REPORT_AVAILABLE_CODE) != 0) {
				log.debug("Report not available: " + item.getStatus());
			}
		} catch (Exception e) {
			log.error("(getReviewScore)" + e);
		}

		String[] assignData = null;
		try {
			assignData = getAssignData(contentId);
		} catch (Exception e) {
			log.error("(assignData)" + e);
		}

		String siteId = "", taskId = "", taskTitle = "";
		Map<String, Object> data = new HashMap<String, Object>();
		if (assignData != null) {
			siteId = assignData[0];
			taskId = assignData[1];
			taskTitle = assignData[2];
		} else {
			siteId = item.getSiteId();
			taskId = item.getTaskId();
			taskTitle = getAssignmentTitle(taskId);
			data.put("assignment1", "assignment1");
		}

		return item.getReviewScore().intValue();
	}

	/**
	 * Check if grade sync has been run already for the specified site
	 * 
	 * @param sess
	 *            Current Session
	 * @param taskId
	 * @return
	 */
	public boolean gradesChecked(Session sess, String taskId) {
		String sessSync = "";
		try {
			sessSync = sess.getAttribute("sync").toString();
			if (sessSync.equals(taskId)) {
				return true;
			}
		} catch (Exception e) {
			// log.error("(gradesChecked)"+e);
		}
		return false;
	}

	/**
	 * Check if the specified user has the student role on the specified site.
	 * 
	 * @param siteId
	 *            Site ID
	 * @param userId
	 *            User ID
	 * @return true if user has student role on the site.
	 */
	public boolean isUserStudent(String siteId, String userId) {
		boolean isStudent = false;
		try {
			Set<String> studentIds = siteService.getSite(siteId).getUsersIsAllowed("section.role.student");
			List<User> activeUsers = userDirectoryService.getUsers(studentIds);
			for (int i = 0; i < activeUsers.size(); i++) {
				User user = activeUsers.get(i);
				if (userId.equals(user.getId())) {
					return true;
				}
			}
		} catch (Exception e) {
			log.info("(isStudentUser)" + e);
		}
		return isStudent;
	}

	/**
	 * private methods
	 */
	private String encodeParam(String name, String value, String boundary) {
		return "--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n";
	}

	/**
	 * This returns the String that will be used as the Assignment Title in Turn
	 * It In.
	 *
	 * The current implementation here has a few interesting caveats so that it
	 * will work with both, the existing Assignments 1 integration, and the new
	 * Assignments 2 integration under development.
	 *
	 * We will check and see if the taskId starts with /assignment/. If it does
	 * we will look up the Assignment Entity on the legacy Entity bus. (not the
	 * entitybroker). This needs some general work to be made generally modular
	 * ( and useful for more than just Assignments 1 and 2 ). We will need to
	 * look at some more concrete use cases and then factor it accordingly in
	 * the future when the next scenerio is required.
	 *
	 * Another oddity is that to get rid of our hard dependency on Assignments 1
	 * we are invoking the getTitle method by hand. We probably need a mechanism
	 * to register a title handler or something as part of the setup process for
	 * new services that want to be reviewable.
	 *
	 * @param taskId
	 * @return
	 */
	private String getAssignmentTitle(String taskId) {
		String togo = taskId;
		if (taskId.startsWith("/assignment/")) {
			try {
				Reference ref = entityManager.newReference(taskId);
				log.debug("got ref " + ref + " of type: " + ref.getType());
				EntityProducer ep = ref.getEntityProducer();

				Entity ent = ep.getEntity(ref);
				log.debug("got entity " + ent);
				String title = scrubSpecialCharacters(ent.getClass().getMethod("getTitle").invoke(ent).toString());
				log.debug("Got reflected assignemment title from entity " + title);
				togo = URLDecoder.decode(title, "UTF-8");

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// compilatio requires Assignment titles to be at least two characters
		// long
		if (togo.length() == 1) {
			togo = togo + "_";
		}

		return togo;

	}

	private String getAssignmentGenerateOriginalityReport(String taskId) {
		String togo = null;
		if (taskId.startsWith("/assignment/")) {
			try {
				Reference ref = entityManager.newReference(taskId);
				log.debug("got ref " + ref + " of type: " + ref.getType());
				EntityProducer ep = ref.getEntityProducer();

				Entity ent = ep.getEntity(ref);
				log.debug("got entity " + ent);
				if (ent != null && ent.getClass() != null && ent.getClass().getMethod("getContent") != null) {
					AssignmentContent ac = (AssignmentContent) ent.getClass().getMethod("getContent").invoke(ent);
					if (ac != null) {
						togo = ac.getGenerateOriginalityReport();
					}

				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return togo;

	}

	private Time getAssignmentCloseTime(String taskId) {
		Time dueTime = null;
		if (taskId.startsWith("/assignment/")) {
			try {
				Reference ref = entityManager.newReference(taskId);
				log.debug("got ref " + ref + " of type: " + ref.getType());
				EntityProducer ep = ref.getEntityProducer();

				Entity ent = ep.getEntity(ref);
				log.debug("got entity " + ent);
				dueTime = (Time) ent.getClass().getMethod("getCloseTime").invoke(ent);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return dueTime;

	}

	private String scrubSpecialCharacters(String title) {

		try {
			if (title.contains("&")) {
				title = title.replace('&', 'n');
			}
			if (title.contains("%")) {
				title = title.replace("%", "percent");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return title;
	}

	/**
	 * @param siteId
	 * @param taskId
	 * @throws SubmissionException
	 * @throws TransientSubmissionException
	 */
	public void createAssignment(String siteId, String taskId)
			throws SubmissionException, TransientSubmissionException {
		createAssignment(siteId, taskId, null);
	}

	/*
	 * Obtain a lock on the item
	 */
	private boolean obtainLock(String itemId) {
		Boolean lock = dao.obtainLock(itemId, serverConfigurationService.getServerId(), LOCK_PERIOD);
		return (lock != null) ? lock : false;
	}

	/*
	 * Get the next item that needs to be submitted
	 *
	 */
	private ContentReviewItem getNextItemToSendQueue() {

		Search search = new Search();
		search.addRestriction(new Restriction("status", ContentReviewItem.NOT_SENDED_CODE));

		List<ContentReviewItem> notSubmittedItems = dao.findBySearch(ContentReviewItem.class, search);
		for (int i = 0; i < notSubmittedItems.size(); i++) {
			ContentReviewItem item = (ContentReviewItem) notSubmittedItems.get(i);

			// can we get a lock?
			if (obtainLock("item." + Long.valueOf(item.getId()).toString()))
				return item;
		}

		search = new Search();
		search.addRestriction(new Restriction("status", ContentReviewItem.SENDING_ERROR_RETRY_CODE));
		notSubmittedItems = dao.findBySearch(ContentReviewItem.class, search);

		// we need the next one whose retry time has not been reached
		for (int i = 0; i < notSubmittedItems.size(); i++) {
			ContentReviewItem item = (ContentReviewItem) notSubmittedItems.get(i);
			if (hasReachedRetryTime(item) && obtainLock("item." + Long.valueOf(item.getId()).toString()))
				return item;

		}

		return null;
	}

	private boolean acceptableForSubmissionContentReview(ContentReviewItem currentItem) {
		// Si l analyse doit etre lancée manuellement
		boolean acceptable = false;
		String assignmentGenerateOriginalityReport = getAssignmentGenerateOriginalityReport(currentItem.getTaskId());
		if (assignmentGenerateOriginalityReport == null) {
			acceptable = false;
		}

		if ("0".equals(assignmentGenerateOriginalityReport)) {
			acceptable = true;
		} else if ("1".equals(assignmentGenerateOriginalityReport)) {
			// remise manuel
			acceptable = false;
		} else if ("2".equals(assignmentGenerateOriginalityReport)
				&& getAssignmentCloseTime(currentItem.getTaskId()).before(TimeService.newTime())) {
			acceptable = true;
		}
		return acceptable;
	}

	/*
	 * Get the next item that needs to be submitted
	 *
	 */
	private ContentReviewItem getNextItemInSubmissionQueue() {

		Search search = new Search();
		search.addRestriction(new Restriction("status", ContentReviewItem.NOT_SUBMITTED_CODE));

		List<ContentReviewItem> notSubmittedItems = dao.findBySearch(ContentReviewItem.class, search);
		for (int i = 0; i < notSubmittedItems.size(); i++) {
			ContentReviewItem item = (ContentReviewItem) notSubmittedItems.get(i);
			if (item.getExternalId() != null && acceptableForSubmissionContentReview(item)) {

				// can we get a lock?
				if (obtainLock("item." + Long.valueOf(item.getId()).toString()))
					return item;
			}
		}

		search = new Search();
		search.addRestriction(new Restriction("status", ContentReviewItem.SUBMISSION_ERROR_RETRY_CODE));
		notSubmittedItems = dao.findBySearch(ContentReviewItem.class, search);

		// we need the next one whose retry time has not been reached
		for (int i = 0; i < notSubmittedItems.size(); i++) {
			ContentReviewItem item = (ContentReviewItem) notSubmittedItems.get(i);
			if (item.getExternalId() != null && acceptableForSubmissionContentReview(item)) {
				if (hasReachedRetryTime(item) && obtainLock("item." + Long.valueOf(item.getId()).toString()))
					return item;
			}

		}

		return null;
	}

	private boolean hasReachedRetryTime(ContentReviewItem item) {

		// has the item reached its next retry time?
		if (item.getNextRetryTime() == null)
			item.setNextRetryTime(new Date());

		if (item.getNextRetryTime().after(new Date())) {
			// we haven't reached the next retry time
			log.info("next retry time not yet reached for item: " + item.getId());
			dao.update(item);
			return false;
		}

		return true;

	}

	private void releaseLock(ContentReviewItem currentItem) {
		dao.releaseLock("item." + currentItem.getId().toString(), serverConfigurationService.getServerId());
	}

	public void processQueue() {

		log.info("Processing submission queue");
		int errors = 0;
		int success = 0;

		for (ContentReviewItem currentItem = getNextItemInSubmissionQueue(); currentItem != null; currentItem = getNextItemInSubmissionQueue()) {

			log.debug("Attempting to submit content: " + currentItem.getContentId() + " for user: "
					+ currentItem.getUserId() + " and site: " + currentItem.getSiteId());

			if (currentItem.getRetryCount() == null) {
				currentItem.setRetryCount(Long.valueOf(0));
				currentItem.setNextRetryTime(this.getNextRetryTime(0));
				dao.update(currentItem);
			} else if (currentItem.getRetryCount().intValue() > maxRetry) {
				currentItem.setStatus(ContentReviewItem.SUBMISSION_ERROR_RETRY_EXCEEDED);
				dao.update(currentItem);
				errors++;
				continue;
			} else {
				long l = currentItem.getRetryCount().longValue();
				l++;
				currentItem.setRetryCount(Long.valueOf(l));
				currentItem.setNextRetryTime(this.getNextRetryTime(Long.valueOf(l)));
				dao.update(currentItem);
			}
			Document document = null;
			// Start Compilation Analyse
			try {
				Map params = CompilatioAPIUtil.packMap("action", "startDocumentAnalyse", "idDocument",
						currentItem.getExternalId());

				document = compilatioConn.callCompilatioReturnDocument(params);

			} catch (TransientSubmissionException e) {
				currentItem.setStatus(ContentReviewItem.SUBMISSION_ERROR_RETRY_CODE);
				currentItem.setLastError(
						"Error Submitting Assignment for Submission: " + e.getMessage() + ". Assume unsuccessful");
				dao.update(currentItem);
				releaseLock(currentItem);
				errors++;
				continue;
			} catch (SubmissionException e) {
				currentItem.setStatus(ContentReviewItem.SUBMISSION_ERROR_RETRY_CODE);
				currentItem.setLastError(
						"Error Submitting Assignment for Submission: " + e.getMessage() + ". Assume unsuccessful");
				dao.update(currentItem);
				releaseLock(currentItem);
				errors++;
				continue;
			}

			Element root = document.getDocumentElement();

			boolean successQuery = root.getElementsByTagName("sucess") != null;
			if (successQuery) {
				log.debug("Submission successful");
				currentItem.setStatus(ContentReviewItem.SUBMITTED_AWAITING_REPORT_CODE);
				currentItem.setRetryCount(Long.valueOf(0));
				currentItem.setLastError(null);
				currentItem.setErrorCode(null);
				currentItem.setDateSubmitted(new Date());
				success++;
				dao.update(currentItem);
			} else {
				String rMessage = null, rCode = null;

				if (root.getElementsByTagName("faultstring").item(0) != null
						&& root.getElementsByTagName("faultcode").item(0) != null) {
					rMessage = ((CharacterData) (root.getElementsByTagName("faultstring").item(0).getFirstChild()))
							.getData();
					rCode = ((CharacterData) (root.getElementsByTagName("faultcode").item(0).getFirstChild()))
							.getData();
				}

				log.debug("Submission not successful: " + rMessage + "(" + rCode + ")");
				if (Error.ANALYSE_ALREADY_STARTED.equals(Error.valueOf(rCode))) {
					log.debug("ContentReview id " + currentItem.getId() + "externalId" + currentItem.getExternalId()
							+ " has nwe status : " + ContentReviewItem.SUBMITTED_AWAITING_REPORT);
					currentItem.setStatus(ContentReviewItem.SUBMITTED_AWAITING_REPORT_CODE);
					currentItem.setRetryCount(Long.valueOf(0));
					currentItem.setLastError(null);
					currentItem.setErrorCode(null);
					currentItem.setDateSubmitted(new Date());
				} else {
					log.warn("Submission not successful. It will be retried.");
					currentItem.setStatus(ContentReviewItem.SUBMISSION_ERROR_RETRY_CODE);
					currentItem.setLastError("Submission Error: " + rMessage + "(" + rCode + ")");
					int errorCodeInt = -1;
					if (ContentReviewItem.Error.valueOf(rCode) != null) {
						errorCodeInt = ContentReviewItem.Error.valueOf(rCode).getErrorCode();
					}
					currentItem.setErrorCode(errorCodeInt);
					errors++;
				}

				/*
				 * if (rMessage.equals("User password does not match user email"
				 * ) || "1001".equals(rCode) || "".equals(rMessage) ||
				 * "413".equals(rCode) || "1025".equals(rCode) ||
				 * "250".equals(rCode)) {
				 * currentItem.setStatus(ContentReviewItem.
				 * SUBMISSION_ERROR_RETRY_CODE); log.warn(
				 * "Submission not successful. It will be retried."); errors++;
				 * } else if (rCode.equals("423")) {
				 * currentItem.setStatus(ContentReviewItem.
				 * SUBMISSION_ERROR_USER_DETAILS_CODE); errors++;
				 * 
				 * } else if (rCode.equals("301")) { // this took a long time
				 * log.warn(
				 * "Submission not successful due to timeout. It will be retried."
				 * ); currentItem.setStatus(ContentReviewItem.
				 * SUBMISSION_ERROR_RETRY_CODE); Calendar cal =
				 * Calendar.getInstance(); cal.set(Calendar.HOUR_OF_DAY, 22);
				 * currentItem.setNextRetryTime(cal.getTime()); errors++;
				 * 
				 * } else { log.error(
				 * "Submission not successful. It will NOT be retried.");
				 * currentItem.setStatus(ContentReviewItem.
				 * SUBMISSION_ERROR_NO_RETRY_CODE); errors++; }
				 */

				dao.update(currentItem);

			}
			// release the lock so the reports job can handle it
			releaseLock(currentItem);
			getNextItemInSubmissionQueue();
		}

		log.info("Submission queue run completed: " + success + " items submitted, " + errors + " errors.");
	}

	@Override
	public String startAnalyse(String contentId) throws QueueException {

		log.debug("Returning review status for content: " + contentId);

		List<ContentReviewItem> matchingItems = getItemsByExternalId(contentId);

		if (matchingItems.size() == 0) {
			log.debug("Content " + contentId + " has not been queued previously");
			throw new QueueException("Content " + contentId + " has not been queued previously");
		}

		if (matchingItems.size() > 1)
			log.debug("more than one matching item found - using first item found");

		ContentReviewItem currentItem = (ContentReviewItem) matchingItems.iterator().next();
		if (!"1".equals(getAssignmentGenerateOriginalityReport(currentItem.getTaskId()))) {
			log.debug("the manual Review status of the content review is not set to true");
			return null;
		}
		long l = currentItem.getRetryCount().longValue();
		l++;
		currentItem.setRetryCount(Long.valueOf(l));
		currentItem.setNextRetryTime(this.getNextRetryTime(Long.valueOf(l)));
		dao.update(currentItem);

		Document document = null;
		// Start Compilation Analyse
		try {
			Map params = CompilatioAPIUtil.packMap("action", "startDocumentAnalyse", "idDocument",
					currentItem.getExternalId());

			document = compilatioConn.callCompilatioReturnDocument(params);

		} catch (TransientSubmissionException e) {
			currentItem.setStatus(ContentReviewItem.SUBMISSION_ERROR_RETRY_CODE);
			currentItem.setLastError(
					"Error Submitting Assignment for Submission: " + e.getMessage() + ". Assume unsuccessful");
			dao.update(currentItem);
			releaseLock(currentItem);
		} catch (SubmissionException e) {
			currentItem.setStatus(ContentReviewItem.SUBMISSION_ERROR_RETRY_CODE);
			currentItem.setLastError(
					"Error Submitting Assignment for Submission: " + e.getMessage() + ". Assume unsuccessful");
			dao.update(currentItem);
			releaseLock(currentItem);
		}

		Element root = document.getDocumentElement();

		boolean successQuery = root.getElementsByTagName("sucess") != null;
		if (successQuery) {
			log.debug("Submission successful");
			currentItem.setStatus(ContentReviewItem.SUBMITTED_AWAITING_REPORT_CODE);
			currentItem.setRetryCount(Long.valueOf(0));
			currentItem.setLastError(null);
			currentItem.setErrorCode(null);
			currentItem.setDateSubmitted(new Date());
			dao.update(currentItem);
		} else {
			String rMessage = null, rCode = null;

			if (root.getElementsByTagName("faultstring").item(0) != null
					&& root.getElementsByTagName("faultcode").item(0) != null) {
				rMessage = ((CharacterData) (root.getElementsByTagName("faultstring").item(0).getFirstChild()))
						.getData();
				rCode = ((CharacterData) (root.getElementsByTagName("faultcode").item(0).getFirstChild())).getData();
			}

			log.debug("Submission not successful: " + rMessage + "(" + rCode + ")");
			if (Error.ANALYSE_ALREADY_STARTED.equals(Error.valueOf(rCode))) {
				log.debug("ContentReview id " + currentItem.getId() + "externalId" + currentItem.getExternalId()
						+ " has nwe status : " + ContentReviewItem.SUBMITTED_AWAITING_REPORT);
				currentItem.setStatus(ContentReviewItem.SUBMITTED_AWAITING_REPORT_CODE);
				currentItem.setRetryCount(Long.valueOf(0));
				currentItem.setLastError(null);
				currentItem.setErrorCode(null);
				currentItem.setDateSubmitted(new Date());
			} else {
				log.warn("Submission not successful. It will be retried.");
				currentItem.setStatus(ContentReviewItem.SUBMISSION_ERROR_RETRY_CODE);
				currentItem.setLastError("Submission Error: " + rMessage + "(" + rCode + ")");
				int errorCodeInt = -1;
				if (ContentReviewItem.Error.valueOf(rCode) != null) {
					errorCodeInt = ContentReviewItem.Error.valueOf(rCode).getErrorCode();
				}
				currentItem.setErrorCode(errorCodeInt);
			}

			dao.update(currentItem);

		}
		// release the lock so the reports job can handle it
		releaseLock(currentItem);

		return null;
	}

	public String escapeFileName(String fileName, String contentId) {
		log.debug("origional filename is: " + fileName);
		if (fileName == null) {
			// use the id
			fileName = contentId;
		} else if (fileName.length() > 199) {
			fileName = fileName.substring(0, 199);
		}
		log.debug("fileName is :" + fileName);
		try {
			fileName = URLDecoder.decode(fileName, "UTF-8");
			// in rare cases it seems filenames can be double encoded
			while (fileName.indexOf("%20") > 0 || fileName.contains("%2520")) {
				try {
					fileName = URLDecoder.decode(fileName, "UTF-8");
				} catch (IllegalArgumentException eae) {
					log.warn("Unable to decode fileName: " + fileName);
					eae.printStackTrace();
					// as the result is likely to cause a MD5 exception use the
					// ID
					return contentId;
					/*
					 * currentItem.setStatus(ContentReviewItem.
					 * SUBMISSION_ERROR_NO_RETRY_CODE);
					 * currentItem.setLastError("FileName decode exception: " +
					 * fileName); dao.update(currentItem);
					 * releaseLock(currentItem); errors++; throw new
					 * SubmissionException("Can't decode fileName!");
					 */
				}

			}
		} catch (IllegalArgumentException eae) {
			log.warn("Unable to decode fileName: " + fileName);
			eae.printStackTrace();
			return contentId;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		fileName = fileName.replace(' ', '_');
		// its possible we have double _ as a result of this lets do some
		// cleanup
		fileName = StringUtils.replace(fileName, "__", "_");

		log.debug("fileName is :" + fileName);
		return fileName;
	}

	private String truncateFileName(String fileName, int i) {
		// get the extension for later re-use
		String extension = "";
		if (fileName.contains(".")) {
			extension = fileName.substring(fileName.lastIndexOf("."));
		}

		fileName = fileName.substring(0, i - extension.length());
		fileName = fileName + extension;

		return fileName;
	}

	public void checkForReports() {
		checkForReportsBulk();
	}

	/*
	 * Fetch reports on a class by class basis
	 */
	@SuppressWarnings({ "deprecation", "unchecked" })
	public void checkForReportsBulk() {

		SimpleDateFormat dform = ((SimpleDateFormat) DateFormat.getDateInstance());
		dform.applyPattern(COMPILATIO_DATETIME_FORMAT);

		log.info("Fetching reports from Compilatio");

		// get the list of all items that are waiting for reports
		List<ContentReviewItem> awaitingReport = dao.findByProperties(ContentReviewItem.class,
				new String[] { "status" }, new Object[] { ContentReviewItem.SUBMITTED_AWAITING_REPORT_CODE });

		awaitingReport.addAll(dao.findByProperties(ContentReviewItem.class, new String[] { "status" },
				new Object[] { ContentReviewItem.REPORT_ERROR_RETRY_CODE }));

		Iterator<ContentReviewItem> listIterator = awaitingReport.iterator();
		HashMap<String, Integer> reportTable = new HashMap<String, Integer>();

		log.debug("There are " + awaitingReport.size() + " submissions awaiting reports");

		ContentReviewItem currentItem;
		while (listIterator.hasNext()) {
			currentItem = (ContentReviewItem) listIterator.next();

			// has the item reached its next retry time?
			if (currentItem.getNextRetryTime() == null)
				currentItem.setNextRetryTime(new Date());

			if (currentItem.getNextRetryTime().after(new Date())) {
				// we haven't reached the next retry time
				log.info("next retry time not yet reached for item: " + currentItem.getId());
				dao.update(currentItem);
				continue;
			}

			if (currentItem.getRetryCount() == null) {
				currentItem.setRetryCount(Long.valueOf(0));
				currentItem.setNextRetryTime(this.getNextRetryTime(0));
			} else if (currentItem.getRetryCount().intValue() > maxRetry) {
				currentItem.setStatus(ContentReviewItem.SUBMISSION_ERROR_RETRY_EXCEEDED);
				dao.update(currentItem);
				continue;
			} else {
				log.debug("Still have retries left, continuing. ItemID: " + currentItem.getId());
				// Moving down to check for report generate speed.
				// long l = currentItem.getRetryCount().longValue();
				// l++;
				// currentItem.setRetryCount(Long.valueOf(l));
				// currentItem.setNextRetryTime(this.getNextRetryTime(Long.valueOf(l)));
				// dao.update(currentItem);
			}

			if (currentItem.getExternalId() == null || currentItem.getExternalId().equals("")) {
				currentItem.setStatus(Long.valueOf(ContentReviewItem.SUBMISSION_ERROR_RETRY_CODE));
				dao.update(currentItem);
				continue;
			}

			if (!reportTable.containsKey(currentItem.getExternalId())) {
				// get the list from compilatio and see if the review is
				// available

				log.debug("Attempting to update hashtable with reports for site " + currentItem.getSiteId());

				Map params = CompilatioAPIUtil.packMap("action", "getDocument", "idDocument",
						currentItem.getExternalId());

				Document document = null;

				try {
					document = compilatioConn.callCompilatioReturnDocument(params);
				} catch (TransientSubmissionException e) {
					log.warn("Update failed due to TransientSubmissionException error: " + e.toString(), e);
					currentItem.setStatus(ContentReviewItem.REPORT_ERROR_RETRY_CODE);
					currentItem.setLastError(e.getMessage());
					dao.update(currentItem);
					break;
				} catch (SubmissionException e) {
					log.warn("Update failed due to SubmissionException error: " + e.toString(), e);
					currentItem.setStatus(ContentReviewItem.REPORT_ERROR_RETRY_CODE);
					currentItem.setLastError(e.getMessage());
					dao.update(currentItem);
					break;
				}

				Element root = document.getDocumentElement();
				if (root.getElementsByTagName("documentStatus").item(0) != null) {
					// ((CharacterData)
					// (root.getElementsByTagName("rcode").item(0).getFirstChild())).getData().trim()
					// .compareTo("72") == 0) {
					log.debug("Report list returned successfully");

					NodeList objects = root.getElementsByTagName("documentStatus");
					log.debug(objects.getLength() + " objects in the returned list");
					String reportVal = ((CharacterData) (root.getElementsByTagName("indice").item(0).getFirstChild()))
							.getData().trim();
					String status = ((CharacterData) (root.getElementsByTagName("status").item(0).getFirstChild()))
							.getData().trim();

					if ("ANALYSE_COMPLETE".equals(status)) {
						currentItem.setReviewScore((int) Math.round(Double.parseDouble(reportVal)));
						currentItem.setStatus(ContentReviewItem.SUBMITTED_REPORT_AVAILABLE_CODE);
					} else {
						if (root.getElementsByTagName("progression") != null) {
							String progression = ((CharacterData) (root.getElementsByTagName("progression").item(0)
									.getFirstChild())).getData().trim();
							currentItem.setReviewScore((int) Double.parseDouble(progression));
						}
					}
					currentItem.setDateReportReceived(new Date());
					dao.update(currentItem);
					log.debug("new report received: " + currentItem.getExternalId() + " -> "
							+ currentItem.getReviewScore());

				} else {
					log.debug("Report list request not successful");
					log.debug(document.getTextContent());

				}
			}
		}

		log.info("Finished fetching reports from Compilatio");
	}

	/**
	 * Is this a valid email the service will recognize
	 * 
	 * @param email
	 * @return
	 */
	private boolean isValidEmail(String email) {

		// TODO: Use a generic Sakai utility class (when a suitable one exists)

		if (email == null || email.equals(""))
			return false;

		email = email.trim();
		// must contain @
		if (email.indexOf("@") == -1)
			return false;

		// an email can't contain spaces
		if (email.indexOf(" ") > 0)
			return false;

		// use commons-validator
		EmailValidator validator = EmailValidator.getInstance();
		if (validator.isValid(email))
			return true;

		return false;
	}

	// Methods for updating all assignments that exist
	public void doAssignments() {
	}

	/**
	 * Update Assignment. This method is not currently called by Assignments 1.
	 */
	public void updateAssignment(String siteId, String taskId) throws SubmissionException {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sakaiproject.contentreview.service.ContentReviewService#
	 * isAcceptableContent(org.sakaiproject.content.api.ContentResource)
	 */
	public boolean isAcceptableContent(ContentResource resource) {
		return compilatioContentValidator.isAcceptableContent(resource);
	}

	/**
	 * find the next time this item should be tried
	 * 
	 * @param retryCount
	 * @return
	 */
	private Date getNextRetryTime(long retryCount) {
		int offset = 5;

		if (retryCount > 9 && retryCount < 20) {

			offset = 10;

		} else if (retryCount > 19 && retryCount < 30) {
			offset = 20;
		} else if (retryCount > 29 && retryCount < 40) {
			offset = 40;
		} else if (retryCount > 39 && retryCount < 50) {
			offset = 80;
		} else if (retryCount > 49 && retryCount < 60) {
			offset = 160;
		} else if (retryCount > 59) {
			offset = 220;
		}

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, offset);
		return cal.getTime();
	}

	public String getLocalizedStatusMessage(String messageCode, String userRef) {

		String userId = EntityReference.getIdFromRef(userRef);
		ResourceLoader resourceLoader = new ResourceLoader(userId, "compilatio");
		return resourceLoader.getString(messageCode);
	}

	public String getReviewError(String contentId) {
		return getLocalizedReviewErrorMessage(contentId);
	}

	public String getLocalizedStatusMessage(String messageCode) {
		return getLocalizedStatusMessage(messageCode, userDirectoryService.getCurrentUser().getReference());
	}

	public String getLocalizedStatusMessage(String messageCode, Locale locale) {
		// TODO not sure how to do this with the sakai resource loader
		return null;
	}

	public String getLocalizedReviewErrorMessage(String contentId) {
		log.debug("Returning review error for content: " + contentId);

		List<ContentReviewItem> matchingItems = dao.findByExample(new ContentReviewItem(contentId));

		if (matchingItems.size() == 0) {
			log.debug("Content " + contentId + " has not been queued previously");
			return null;
		}

		if (matchingItems.size() > 1) {
			log.debug("more than one matching item found - using first item found");
		}

		// its possible the error code column is not populated
		Integer errorCode = ((ContentReviewItem) matchingItems.iterator().next()).getErrorCode();
		if (errorCode == null) {
			return ((ContentReviewItem) matchingItems.iterator().next()).getLastError();
		}
		return getLocalizedStatusMessage(errorCode.toString());
	}

	@Override
	public String addDocumentToCompilatio(ContentReviewItem currentItem) {
		// to get the name of the initial submited file we need the title
		ContentResource resource = null;
		ResourceProperties resourceProperties = null;
		String fileName = null;
		int errors = 0;
		boolean success = false;
		try {
			try {
				resource = contentHostingService.getResource(currentItem.getContentId());

			} catch (TypeException e4) {
				// ToDo we should probably remove these from the Queue
				log.warn("IdUnusedException: no resource with id " + currentItem.getContentId());
				errors++;
			} catch (IdUnusedException e) {
				// ToDo we should probably remove these from the Queue
				log.warn("IdUnusedException: no resource with id " + currentItem.getContentId());
				errors++;
			}
			resourceProperties = resource.getProperties();
			fileName = resourceProperties.getProperty(resourceProperties.getNamePropDisplayName());
			fileName = escapeFileName(fileName, resource.getId());
		} catch (PermissionException e2) {
			log.error("Submission failed due to permission error.", e2);
			currentItem.setStatus(ContentReviewItem.SUBMISSION_ERROR_NO_RETRY_CODE);
			currentItem.setLastError("Permission exception: " + e2.getMessage());
			releaseLock(currentItem);
			errors++;
		}

		// TODO Auto-generated method stub
		Document document = null;
		String idDocument = null;
		try {
			Map params = CompilatioAPIUtil.packMap("action", "addDocumentBase64", "filename",
					URLEncoder.encode(fileName, "UTF-8"), "mimetype", resource.getContentType(), "content",
					Base64.encodeBase64String(resource.getContent()));

			document = compilatioConn.callCompilatioReturnDocument(params);

			if (document == null) {
				return ContentReviewItem.Error.TEMPORARY_UNAVAILABLE.name();
			}
			Element root = document.getDocumentElement();
			if (root.getElementsByTagName("idDocument").item(0) != null) {
				idDocument = ((CharacterData) (root.getElementsByTagName("idDocument").item(0).getFirstChild()))
						.getData();
				System.out.println("rMessage" + idDocument);
				log.info("Add document run completed: " + success + " items submitted, " + errors + " errors.");
				if (idDocument != null) {
					currentItem.setExternalId(idDocument);
					currentItem.setStatus(ContentReviewItem.NOT_SUBMITTED_CODE);
					currentItem.setRetryCount(Long.valueOf(0));
					currentItem.setLastError(null);
					currentItem.setErrorCode(null);
					currentItem.setDateSubmitted(new Date());
				}
			} else {
				currentItem.setStatus(ContentReviewItem.NOT_SENDED_CODE);
				String faultCode = ((CharacterData) (root.getElementsByTagName("faultcode").item(0).getFirstChild()))
						.getData();

				return faultCode;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error(e);
		}

		return null;
	}

	@Override
	public void addDocumentContentReview() {
		log.info("Processing submission queue");
		int errors = 0;
		int success = 0;

		for (ContentReviewItem currentItem = getNextItemToSendQueue(); currentItem != null; currentItem = getNextItemToSendQueue()) {

			log.debug("Attempting to sending content: " + currentItem.getContentId() + " for user: "
					+ currentItem.getUserId() + " and site: " + currentItem.getSiteId());

			if (currentItem.getRetryCount() == null) {
				currentItem.setRetryCount(Long.valueOf(0));
				currentItem.setNextRetryTime(this.getNextRetryTime(0));
				dao.update(currentItem);
			} else if (currentItem.getRetryCount().intValue() > maxRetry) {
				currentItem.setStatus(ContentReviewItem.SUBMISSION_ERROR_RETRY_EXCEEDED);
				dao.update(currentItem);
				errors++;
				continue;
			} else {
				long l = currentItem.getRetryCount().longValue();
				l++;
				currentItem.setRetryCount(Long.valueOf(l));
				currentItem.setNextRetryTime(this.getNextRetryTime(Long.valueOf(l)));
				dao.update(currentItem);
			}

			// to get the name of the initial submited file we need the title
			ContentResource resource = null;
			ResourceProperties resourceProperties = null;
			String fileName = null;
			try {
				try {
					resource = contentHostingService.getResource(currentItem.getContentId());

				} catch (IdUnusedException e4) {
					// ToDo we should probably remove these from the Queue
					log.warn("IdUnusedException: no resource with id " + currentItem.getContentId());
					dao.delete(currentItem);
					errors++;
					continue;
				}
				resourceProperties = resource.getProperties();
				fileName = resourceProperties.getProperty(resourceProperties.getNamePropDisplayName());
				fileName = escapeFileName(fileName, resource.getId());
			} catch (PermissionException e2) {
				log.error("Submission failed due to permission error.", e2);
				currentItem.setStatus(ContentReviewItem.SUBMISSION_ERROR_NO_RETRY_CODE);
				currentItem.setLastError("Permission exception: " + e2.getMessage());
				dao.update(currentItem);
				releaseLock(currentItem);
				errors++;
				continue;
			} catch (TypeException e) {
				log.error("Submission failed due to content Type error.", e);
				currentItem.setStatus(ContentReviewItem.SUBMISSION_ERROR_NO_RETRY_CODE);
				currentItem.setLastError("Type Exception: " + e.getMessage());
				dao.update(currentItem);
				releaseLock(currentItem);
				errors++;
				continue;
			}

			// TII-97 filenames can't be longer than 200 chars
			if (fileName != null && fileName.length() >= 200) {
				fileName = truncateFileName(fileName, 198);
			}

			Document document = null;
			try {
				Map params = CompilatioAPIUtil.packMap("action", "addDocumentBase64", "filename",
						URLEncoder.encode(fileName, "UTF-8"), "mimetype", resource.getContentType(), "content",
						Base64.encodeBase64String(resource.getContent()));

				document = compilatioConn.callCompilatioReturnDocument(params);

			} catch (TransientSubmissionException e) {
				currentItem.setStatus(ContentReviewItem.SUBMISSION_ERROR_RETRY_CODE);
				currentItem.setLastError(
						"Error Submitting Assignment for Submission: " + e.getMessage() + ". Assume unsuccessful");
				dao.update(currentItem);
				releaseLock(currentItem);
				errors++;
				continue;
			} catch (SubmissionException e) {
				currentItem.setStatus(ContentReviewItem.SUBMISSION_ERROR_RETRY_CODE);
				currentItem.setLastError(
						"Error Submitting Assignment for Submission: " + e.getMessage() + ". Assume unsuccessful");
				dao.update(currentItem);
				releaseLock(currentItem);
				errors++;
				continue;
			} catch (UnsupportedEncodingException e) {
				currentItem.setStatus(ContentReviewItem.SUBMISSION_ERROR_RETRY_CODE);
				currentItem.setLastError(
						"Error Submitting Assignment for Submission: " + e.getMessage() + ". Assume unsuccessful");
				dao.update(currentItem);
				releaseLock(currentItem);
				errors++;
				continue;
			} catch (ServerOverloadException e) {
				currentItem.setStatus(ContentReviewItem.SUBMISSION_ERROR_RETRY_CODE);
				currentItem.setLastError(
						"Error Submitting Assignment for Submission: " + e.getMessage() + ". Assume unsuccessful");
				dao.update(currentItem);
				releaseLock(currentItem);
				errors++;
				continue;
			}

			if (document != null) {
				Element root = document.getDocumentElement();

				String externalId = null;
				if (root.getElementsByTagName("idDocument").item(0) != null) {
					externalId = ((CharacterData) (root.getElementsByTagName("idDocument").item(0).getFirstChild()))
							.getData().trim();
				}

				if (externalId != null) {
					if (externalId != null && externalId.length() > 0) {
						log.debug("Submission successful");
						currentItem.setExternalId(externalId);
						currentItem.setStatus(ContentReviewItem.NOT_SUBMITTED_CODE);
						currentItem.setRetryCount(Long.valueOf(0));
						currentItem.setLastError(null);
						currentItem.setErrorCode(null);
						currentItem.setDateSubmitted(new Date());
						success++;
						dao.update(currentItem);
					} else {
						log.warn("invalid external id");
						currentItem.setLastError("Submission error: no external id received");
						currentItem.setStatus(ContentReviewItem.SUBMISSION_ERROR_RETRY_CODE);
						errors++;
						dao.update(currentItem);
					}
				} else {
					String rMessage = null, rCode = null;

					if (root.getElementsByTagName("faultstring").item(0) != null
							&& root.getElementsByTagName("faultcode").item(0) != null) {
						rMessage = ((CharacterData) (root.getElementsByTagName("faultstring").item(0).getFirstChild()))
								.getData();
						rCode = ((CharacterData) (root.getElementsByTagName("faultcode").item(0).getFirstChild()))
								.getData();
					}

					log.debug("Add Document To compilatio not successful: " + rMessage + "(" + rCode + ")");

					currentItem.setStatus(ContentReviewItem.SUBMISSION_ERROR_RETRY_CODE);
					log.warn("Add Document To compilatio not successful. It will be retried.");
					errors++;

					/*
					 * if ("User password does not match user email"
					 * .equals(rMessage) || "1001".equals(rCode) ||
					 * "".equals(rMessage) || "413".equals(rCode) ||
					 * "1025".equals(rCode) || "250".equals(rCode)) {
					 * currentItem.setStatus(ContentReviewItem.
					 * SUBMISSION_ERROR_RETRY_CODE); log.warn(
					 * "Submission not successful. It will be retried.");
					 * errors++; } else if (rCode.equals("423")) {
					 * currentItem.setStatus(ContentReviewItem.
					 * SUBMISSION_ERROR_USER_DETAILS_CODE); errors++;
					 * 
					 * } else if (rCode.equals("301")) { // this took a long
					 * time log.warn(
					 * "Submission not successful due to timeout. It will be retried."
					 * ); currentItem.setStatus(ContentReviewItem.
					 * SUBMISSION_ERROR_RETRY_CODE); Calendar cal =
					 * Calendar.getInstance(); cal.set(Calendar.HOUR_OF_DAY,
					 * 22); currentItem.setNextRetryTime(cal.getTime());
					 * errors++;
					 * 
					 * } else { log.error(
					 * "Submission not successful. It will NOT be retried.");
					 * currentItem.setStatus(ContentReviewItem.
					 * SUBMISSION_ERROR_NO_RETRY_CODE); errors++; }
					 */
					currentItem.setLastError("Add Document To compilatio Error: " + rMessage + "(" + rCode + ")");
					int errorCodeInt = -1;
					ContentReviewItem.Error errorCode = ContentReviewItem.Error.valueOf(rCode);
					if (errorCode != null) {
						errorCodeInt = errorCode.getErrorCode();
					}
					currentItem.setErrorCode(errorCodeInt);
					dao.update(currentItem);

				}
			}
			// release the lock so the reports job can handle it
			releaseLock(currentItem);
			getNextItemInSubmissionQueue();
		}
		log.info("Add Document To compilatio run completed: " + success + " items submitted, " + errors + " errors.");
	}

	@Override
	public Map getAssignment(String siteId, String taskId) throws SubmissionException, TransientSubmissionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void createAssignment(String siteId, String taskId, Map extraAsnnOpts)
			throws SubmissionException, TransientSubmissionException {
		// TODO Auto-generated method stub

	}

	@Override
	public void refreshItemScore(ContentReviewItem contentReviewItem) {
		// TODO Auto-generated method stub
		Map params = CompilatioAPIUtil.packMap("action", "getDocument", "idDocument",
				contentReviewItem.getExternalId());

		Document document = null;

		try {
			document = compilatioConn.callCompilatioReturnDocument(params);
			if (document != null) {
				Element root = document.getDocumentElement();
				if (root != null && root.getElementsByTagName("documentStatus").item(0) != null) {
					// ((CharacterData)
					// (root.getElementsByTagName("rcode").item(0).getFirstChild())).getData().trim()
					// .compareTo("72") == 0) {
					log.debug("Report list returned successfully");

					NodeList objects = root.getElementsByTagName("documentStatus");
					log.debug(objects.getLength() + " objects in the returned list");
					String reportVal = ((CharacterData) (root.getElementsByTagName("indice").item(0).getFirstChild()))
							.getData().trim();
					String status = ((CharacterData) (root.getElementsByTagName("status").item(0).getFirstChild()))
							.getData().trim();

					if ("ANALYSE_COMPLETE".equals(status)) {
						contentReviewItem.setReviewScore((int) Math.round(Double.parseDouble(reportVal)));
						dao.update(contentReviewItem);
					}

				} else {
					log.debug("Report list request not successful");
					log.debug(document.getTextContent());

				}
			}
		} catch (Exception e) {
			log.warn("Get score failed due to error: " + e.toString(), e);
		}

	}

}
