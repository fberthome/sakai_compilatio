package org.sakaiproject.contentreview.logic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.contentreview.impl.compilatio.CompilatioReviewServiceImpl;
import org.springframework.test.AbstractTransactionalSpringContextTests;

public class CompilatioTest extends AbstractTransactionalSpringContextTests {
	private static final Log log = LogFactory.getLog(CompilatioTest.class);
	
	protected String[] getConfigLocations() {
	      // point to the needed spring config files, must be on the classpath
	      // (add component/src/webapp/WEB-INF to the build path in Eclipse),
	      // they also need to be referenced in the maven file
	      return new String[] {"hibernate-test.xml", "spring-hibernate.xml"};
	   }

	
	public void testFileEscape() {
		CompilatioReviewServiceImpl compilatioService = new CompilatioReviewServiceImpl();
		String someEscaping = compilatioService.escapeFileName("Practical%203.docx", "contentId");
		assertEquals("Practical_3.docx", someEscaping);
		
		someEscaping = compilatioService.escapeFileName("Practical%203%.docx", "contentId");
		assertEquals("contentId", someEscaping);
		
		someEscaping = compilatioService.escapeFileName("Practical3.docx", "contentId");
		assertEquals("Practical3.docx", someEscaping);
		
		
	}
	
	
}
