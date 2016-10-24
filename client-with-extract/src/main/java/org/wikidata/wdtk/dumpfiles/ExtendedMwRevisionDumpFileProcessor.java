package org.wikidata.wdtk.dumpfiles;

import java.io.InputStream;
import java.lang.reflect.Field;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.dumpfiles.MwRevisionDumpFileProcessor;
import org.wikidata.wdtk.dumpfiles.MwRevisionProcessor;

import de.upb.wdqa.wdvd.Revision;


public class ExtendedMwRevisionDumpFileProcessor extends MwRevisionDumpFileProcessor {

	static final Logger logger = LoggerFactory.getLogger(ExtendedMwRevisionDumpFileProcessor.class);
	
	static final String A_DELETED = "deleted";

	public ExtendedMwRevisionDumpFileProcessor(
			MwRevisionProcessor mwRevisionProcessor) {
		super(mwRevisionProcessor);
		
		// A little hack to change the final field mwRevision
		try {
			setFinalField(this, this.getClass().getSuperclass().getDeclaredField("mwRevision"),
					new ExtendedMwRevisionImpl());
		} catch ( Exception e) {
			logger.error("", e);
		}
	}
	
	void setFinalField(Object owner, Field field, Object newValue) throws Exception {
		field.setAccessible(true);
	    field.set(owner, newValue);
	 }
	
	
	
	/**
	 * Modified version of the method
	 * {@link MwDumpFileProcessor#processDumpFileContents()} that does not
	 * require a MwDumpFile.
	 * 
	 * @param inputStream
	 *            to access the contents of the dump
	 */
	public void processDumpFileContents(InputStream inputStream) {
		this.namespaces.clear();
		this.sitename = "";
		this.baseUrl = "";

		this.xmlReader = null;

		try {
			this.xmlReader = this.xmlFactory.createXMLStreamReader(inputStream);
			processXmlMediawiki();
		} catch (XMLStreamException | MwDumpFormatException e) {
			MwRevisionDumpFileProcessor.logger.error(e.toString());
		} finally { // unfortunately, xmlReader does not implement AutoClosable
			if (this.xmlReader != null) {
				try {
					this.xmlReader.close();
				} catch (XMLStreamException e) {
					throw new RuntimeException(
							"Problem closing XML Reader. This hides an earlier exception.",
							e);
				}
			}
		}

		this.mwRevisionProcessor.finishRevisionProcessing();
	}
	
	/**
	 * Modified version of the method
	 * {@link MwDumpFileProcessor#processDumpFileContents()} that does not
	 * require a MwDumpFile.
	 * 
	 * @param inputStream
	 *            to access the contents of the dump
	 */
	public void processDumpFileContents(MwRevision revision) {
		this.namespaces.clear();
		this.sitename = "";
		this.baseUrl = "";

		//this.xmlReader = null;

		try {
			//this.xmlReader = this.xmlFactory.createXMLStreamReader(inputStream);
			processXmlMediawiki(revision);
		} catch (XMLStreamException | MwDumpFormatException e) {
			MwRevisionDumpFileProcessor.logger.error(e.toString());
		} finally { // unfortunately, xmlReader does not implement AutoClosable
//			if (this.xmlReader != null) {
//				try {
//					this.xmlReader.close();
//				} catch (XMLStreamException e) {
//					throw new RuntimeException(
//							"Problem closing XML Reader. This hides an earlier exception.",
//							e);
//				}
//			}
		}

		this.mwRevisionProcessor.finishRevisionProcessing();
	}
	
	
	// The following method was overwritten, because WDTK cannot read all elements in the Wikidata XML dump.
	// For example, the dump contains elements of the form  
	//   <comment deleted="deleted" />
    //   <text deleted="deleted" />	
	@Override
	void processXmlRevision() throws XMLStreamException, MwDumpFormatException {
		this.mwRevision.resetCurrentRevisionData();

		this.xmlReader.next(); // skip current start tag
		while (this.xmlReader.hasNext()) {
			switch (this.xmlReader.getEventType()) {

			case XMLStreamConstants.START_ELEMENT:
				switch (this.xmlReader.getLocalName()) {
				case MwRevisionDumpFileProcessor.E_REV_COMMENT:
					((ExtendedMwRevisionImpl) this.mwRevision).isCommentDeleted = this.xmlReader.getAttributeValue(null, ExtendedMwRevisionDumpFileProcessor.A_DELETED) != null;
					this.mwRevision.comment = this.xmlReader.getElementText();					
					break;
				case MwRevisionDumpFileProcessor.E_REV_TEXT:
					((ExtendedMwRevisionImpl) this.mwRevision).isTextDeleted = this.xmlReader.getAttributeValue(null, ExtendedMwRevisionDumpFileProcessor.A_DELETED) != null;
					this.mwRevision.text = this.xmlReader.getElementText();					
					break;
				case MwRevisionDumpFileProcessor.E_REV_TIMESTAMP:
					this.mwRevision.timeStamp = this.xmlReader.getElementText();
					break;
				case MwRevisionDumpFileProcessor.E_REV_FORMAT:
					this.mwRevision.format = this.xmlReader.getElementText();
					break;
				case MwRevisionDumpFileProcessor.E_REV_MODEL:
					this.mwRevision.model = this.xmlReader.getElementText();
					break;
				case MwRevisionDumpFileProcessor.E_REV_CONTRIBUTOR:
					processXmlContributor();
					break;
				case MwRevisionDumpFileProcessor.E_REV_ID:
					this.mwRevision.revisionId = Long.valueOf(
							this.xmlReader.getElementText());
					break;
				case MwRevisionDumpFileProcessor.E_REV_PARENT_ID:
					((ExtendedMwRevisionImpl) this.mwRevision).parentId = this.xmlReader.getElementText();
					break;
				case MwRevisionDumpFileProcessor.E_REV_SHA1:
					((ExtendedMwRevisionImpl) this.mwRevision).sha1 = this.xmlReader.getElementText();
					break;
				case MwRevisionDumpFileProcessor.E_REV_MINOR:
					((ExtendedMwRevisionImpl) this.mwRevision).isMinor = true;
					break;
				default:
					throw new MwDumpFormatException("Unexpected element \""
							+ this.xmlReader.getLocalName() + "\" in revision.");
				}

				break;

			case XMLStreamConstants.END_ELEMENT:
				if (MwRevisionDumpFileProcessor.E_PAGE_REVISION
						.equals(this.xmlReader.getLocalName())) {
					this.mwRevisionProcessor.processRevision(this.mwRevision);
					return;
				}
				break;
			}

			this.xmlReader.next();
		}
	}
	
	// The following method was overwritten, because WDTK cannot read all elements in the Wikidata XML dump.
	// For example, the dump contains elements of the form  
	//   <comment deleted="deleted" />
    //   <text deleted="deleted" />	
	void processXmlRevision(MwRevision revision) throws XMLStreamException, MwDumpFormatException {		
		this.mwRevisionProcessor.processRevision(revision);
	}
	
	/**
	 * Processes current XML starting from a &lt;page&gt; start tag up to the
	 * corresponding end tag. This method uses the current state of
	 * {@link #xmlReader} and stores its results in according member fields.
	 * When the method has finished, {@link #xmlReader} will be at the next
	 * element after the closing tag of this block.
	 * 
	 * @throws XMLStreamException
	 *             if there was a problem reading the XML or if the XML is
	 *             malformed
	 * @throws MwDumpFormatException
	 *             if the contents of the XML file did not match our
	 *             expectations of a MediaWiki XML dump
	 */
	void processXmlPage(MwRevision revision) throws XMLStreamException, MwDumpFormatException {
		processXmlRevision(revision);
	}
	
	/**
	 * Tries to processes current XML starting from a &lt;page&gt; start tag up
	 * to the corresponding end tag using {@link #processXmlPage()}. If this
	 * fails for some reason, it tries to recover to read all remaining page
	 * blocks nonetheless.
	 * 
	 * @throws XMLStreamException
	 *             if there was a problem reading the XML
	 */
	void tryProcessXmlPage(MwRevision revision) throws XMLStreamException {
		try {
			processXmlPage(revision);
		} catch (MwDumpFormatException e) {
//			MwRevisionDumpFileProcessor.logger
//					.error("Error when trying to process revision block for page \""
//							+ this.mwRevision.getPrefixedTitle()
//							+ "\" (namespace "
//							+ this.mwRevision.getNamespace()
//							+ ", id "
//							+ this.mwRevision.getPageId()
//							+ "): "
//							+ e.toString());
//
//			MwRevisionDumpFileProcessor.logger.info("Trying to recover ...");
//			while (this.xmlReader.hasNext()) {
//				this.xmlReader.next();
//				if (this.xmlReader.getEventType() == XMLStreamConstants.END_ELEMENT
//						&& this.xmlReader.getLocalName() == MwRevisionDumpFileProcessor.E_PAGE) {
//					MwRevisionDumpFileProcessor.logger
//							.info("... recovery successful. Continuing processing.");
//					return;
//				}
//			}
//			MwRevisionDumpFileProcessor.logger
//					.error("Recovery failed. Could not process remaining XML.");
		}
	}
	
	/**
	 * Processes current XML starting from a &lt;mediawiki&gt; start tag up to
	 * the corresponding end tag. This method uses the current state of
	 * {@link #xmlReader} and stores its results in according member fields.
	 * When the method has finished, {@link #xmlReader} will be at the next
	 * element after the closing tag of this block.
	 * 
	 * @throws XMLStreamException
	 *             if there was a problem reading the XML or if the XML is
	 *             malformed
	 * @throws MwDumpFormatException
	 *             if the contents of the XML file did not match our
	 *             expectations of a MediaWiki XML dump
	 */
	void processXmlMediawiki(MwRevision revision) throws XMLStreamException, MwDumpFormatException {
		this.mwRevisionProcessor.startRevisionProcessing(this.sitename, this.baseUrl, this.namespaces);
		tryProcessXmlPage(revision);
		
	}

}
