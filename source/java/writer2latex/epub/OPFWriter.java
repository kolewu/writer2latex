/************************************************************************
 *
 *  OPFWriter.java
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 *  MA  02111-1307  USA
 *
 *  Copyright: 2001-2010 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  version 1.2 (2010-12-16)
 *
 */

package writer2latex.epub;

import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;

import writer2latex.api.ContentEntry;
import writer2latex.api.ConverterResult;
import writer2latex.api.OutputFile;
import writer2latex.util.Misc;
import writer2latex.xmerge.NewDOMDocument;

/** This class writes an OPF-file for an EPUB document (see http://www.idpf.org/2007/opf/OPF_2.0_final_spec.html).
 */
public class OPFWriter extends NewDOMDocument {

	public OPFWriter(ConverterResult cr, String sUUID, boolean bUseDublinCore) {
		super("book", "opf");
		
        // create DOM
        Document contentDOM = null;
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            DOMImplementation domImpl = builder.getDOMImplementation();
            DocumentType doctype = domImpl.createDocumentType("package","",""); 
            contentDOM = domImpl.createDocument("http://www.idpf.org/2007/opf","package",doctype);
        }
        catch (ParserConfigurationException t) { // this should never happen
            throw new RuntimeException(t);
        }
        
        // Populate the DOM tree
        Element pack = contentDOM.getDocumentElement();
        pack.setAttribute("version", "2.0");
        pack.setAttribute("xmlns","http://www.idpf.org/2007/opf");
        pack.setAttribute("unique-identifier", "BookId");
        
        // Meta data, at least dc:title, dc:language and dc:identifier are required by the specification
        Element metadata = contentDOM.createElement("metadata");
        metadata.setAttribute("xmlns:dc", "http://purl.org/dc/elements/1.1/");
        metadata.setAttribute("xmlns:opf", "http://www.idpf.org/2007/opf");
        pack.appendChild(metadata);
        
        Element title = contentDOM.createElement("dc:title");
        metadata.appendChild(title);
        title.appendChild(contentDOM.createTextNode(cr.getMetaData().getTitle()));
        
        Element language = contentDOM.createElement("dc:language");
        metadata.appendChild(language);
        language.appendChild(contentDOM.createTextNode(cr.getMetaData().getLanguage()));
        
        // Additional meta data
        if (bUseDublinCore) {
        	// Subject and keywords in ODF both map to Dublin core subjects
        	if (cr.getMetaData().getSubject().length()>0) {
        		Element subject = contentDOM.createElement("dc:subject");
        		metadata.appendChild(subject);
        		subject.appendChild(contentDOM.createTextNode(cr.getMetaData().getSubject()));
        	}
        	if (cr.getMetaData().getKeywords().length()>0) {
        		String[] sKeywords = cr.getMetaData().getKeywords().split(",");
        		for (String sKeyword : sKeywords) {
        			Element subject = contentDOM.createElement("dc:subject");
        			metadata.appendChild(subject);
        			subject.appendChild(contentDOM.createTextNode(sKeyword.trim()));
        		}
        	}
        	if (cr.getMetaData().getDescription().length()>0) {
        		Element description = contentDOM.createElement("dc:description");
        		metadata.appendChild(description);
        		description.appendChild(contentDOM.createTextNode(cr.getMetaData().getDescription()));
        	}
        }
        
        // User defined meta data
        // The identifier, creator, contributor and date has an optional attribute and there may be multiple instances of
        // the first three. The key can be in any of the forms name, name.attribute, name.attribute.id, name..id
        // where the id is some unique id amongst the instances with the same name
        // Thus you can have e.g. creator.aut.1="John Doe" and creator.aut.2="Jane Doe"
        boolean bHasIdentifier = false;
        boolean bHasCreator = false;
        boolean bHasDate = false;
        Map<String,String> userDefined = cr.getMetaData().getUserDefinedMetaData();
        for (String sKey : userDefined.keySet()) {
        	if (sKey.length()>0) {
        		String[] sKeyElements = sKey.toLowerCase().split("\\.");
        		String sValue = userDefined.get(sKey);
        		if ("identifier".equals(sKeyElements[0])) {
        			Element identifier = contentDOM.createElement("dc:identifier");
            		identifier.setAttribute("id", "BookId");
        			if (sKeyElements.length>1 && sKeyElements[1].length()>0) {
        				identifier.setAttribute("opf:scheme", sKeyElements[1]);
        			}
        			metadata.appendChild(identifier);
        			identifier.appendChild(contentDOM.createTextNode(sValue));
        			bHasIdentifier = true;
        		}
        		else if ("creator".equals(sKeyElements[0])) {
        			Element creator = contentDOM.createElement("dc:creator");
        			if (sKeyElements.length>1 && sKeyElements[1].length()>0) {
        				creator.setAttribute("opf:role", sKeyElements[1]);
        			}
        			metadata.appendChild(creator);
        			creator.appendChild(contentDOM.createTextNode(sValue));
        			bHasCreator = true;
        		}
        		else if ("contributor".equals(sKeyElements[0])) {
        			Element contributor = contentDOM.createElement("dc:contributor");
        			if (sKeyElements.length>1 && sKeyElements[1].length()>0) {
        				contributor.setAttribute("opf:role", sKeyElements[1]);
        			}
        			metadata.appendChild(contributor);
        			contributor.appendChild(contentDOM.createTextNode(sValue));
        		}
        		else if ("date".equals(sKeyElements[0])) {
        			Element date = contentDOM.createElement("dc:date");
        			if (sKeyElements.length>1 && sKeyElements[1].length()>0) {
        				date.setAttribute("opf:event", sKeyElements[1]);
        			}
        			metadata.appendChild(date);
        			date.appendChild(contentDOM.createTextNode(sValue));
        			bHasDate = true;
        		}
        		else if (sKeyElements.length==1) {
        			// Remaining meta data elements must be unique
        			if ("publisher".equals(sKeyElements[0])) {
        				Element publisher = contentDOM.createElement("dc:publisher");
        				metadata.appendChild(publisher);
        				publisher.appendChild(contentDOM.createTextNode(sValue));
        			}
        			else if ("type".equals(sKeyElements[0])) {
        				Element type = contentDOM.createElement("dc:type");
        				metadata.appendChild(type);
        				type.appendChild(contentDOM.createTextNode(sValue));
        			}
        			else if ("format".equals(sKeyElements[0])) {
        				Element format = contentDOM.createElement("dc:format");
        				metadata.appendChild(format);
        				format.appendChild(contentDOM.createTextNode(sValue));        			
        			}
        			else if ("source".equals(sKeyElements[0])) {
        				Element source = contentDOM.createElement("dc:source");
        				metadata.appendChild(source);
        				source.appendChild(contentDOM.createTextNode(sValue));        			        			
        			}
        			else if ("relation".equals(sKeyElements[0])) {
        				Element relation = contentDOM.createElement("dc:relation");
        				metadata.appendChild(relation);
        				relation.appendChild(contentDOM.createTextNode(sValue));        			        			
        			}
        			else if ("coverage".equals(sKeyElements[0])) {
        				Element coverage = contentDOM.createElement("dc:coverage");
        				metadata.appendChild(coverage);
        				coverage.appendChild(contentDOM.createTextNode(sValue));        				
        			}
        			else if ("rights".equals(sKeyElements[0])) {
        				Element rights = contentDOM.createElement("dc:rights");
        				metadata.appendChild(rights);
        				rights.appendChild(contentDOM.createTextNode(sValue));        					
        			}
        		}
        	}
        }
        
        // Fall back values for creator and date
        if (bUseDublinCore) {
        	if (!bHasIdentifier) {
        		Element identifier = contentDOM.createElement("dc:identifier");
        		identifier.setAttribute("id", "BookId");
        		identifier.setAttribute("opf:scheme", "UUID");
        		metadata.appendChild(identifier);
        		identifier.appendChild(contentDOM.createTextNode(sUUID));
        	}
        	if (!bHasCreator && cr.getMetaData().getCreator().length()>0) {
        		Element creator = contentDOM.createElement("dc:creator");
        		metadata.appendChild(creator);
        		creator.appendChild(contentDOM.createTextNode(cr.getMetaData().getCreator()));
        	}
        	if (!bHasDate && cr.getMetaData().getDate().length()>0) {
        		Element date = contentDOM.createElement("dc:date");
        		metadata.appendChild(date);
        		date.appendChild(contentDOM.createTextNode(cr.getMetaData().getDate()));
        	}
        }
        
        // Manifest must contain references to all the files in the XHTML converter result
        // Spine should contain references to all the master documents within the converter result
        Element manifest = contentDOM.createElement("manifest");
        pack.appendChild(manifest);
        
        Element spine = contentDOM.createElement("spine");
        spine.setAttribute("toc", "ncx");
        pack.appendChild(spine);
        
        int nMasterCount = 0;
        int nResourceCount = 0;
        Iterator<OutputFile> iterator = cr.iterator();
        while (iterator.hasNext()) {
        	OutputFile file = iterator.next();
        	Element item = contentDOM.createElement("item");
        	manifest.appendChild(item);
        	item.setAttribute("href",Misc.makeHref(file.getFileName()));
        	item.setAttribute("media-type", file.getMIMEType());
        	if (file.isMasterDocument()) {
        		String sId = "text"+(++nMasterCount);
        		item.setAttribute("id", sId);
        		
        		Element itemref = contentDOM.createElement("itemref");
        		itemref.setAttribute("idref", sId);
        		spine.appendChild(itemref);
        	}
        	else {
        		item.setAttribute("id", "resource"+(++nResourceCount));
        	}
        }
        
        Element item = contentDOM.createElement("item");
        item.setAttribute("href", "book.ncx");
        item.setAttribute("media-type", "application/x-dtbncx+xml");
        item.setAttribute("id", "ncx");
        manifest.appendChild(item);
        
        // The guide may contain references to some fundamental structural components
        Element guide = contentDOM.createElement("guide");
        pack.appendChild(guide);        
       	addGuideReference(contentDOM,guide,"title-page",cr.getTitlePageFile());
       	addGuideReference(contentDOM,guide,"text",cr.getTextFile());
       	addGuideReference(contentDOM,guide,"toc",cr.getTocFile());
       	addGuideReference(contentDOM,guide,"index",cr.getIndexFile());
       	addGuideReference(contentDOM,guide,"loi",cr.getLofFile());
       	addGuideReference(contentDOM,guide,"lot",cr.getLotFile());
       	addGuideReference(contentDOM,guide,"bibliography",cr.getBibliographyFile());
        
        setContentDOM(contentDOM);
	}
	
	private void addGuideReference(Document contentDOM, Element guide, String sType, ContentEntry entry) {
		if (entry!=null) {
			Element reference = contentDOM.createElement("reference");
			reference.setAttribute("type", sType);
			reference.setAttribute("title", entry.getTitle());
			String sHref = Misc.makeHref(entry.getFile().getFileName());
			if (entry.getTarget()!=null) { sHref+="#"+entry.getTarget(); }
			reference.setAttribute("href", sHref);
			guide.appendChild(reference);
		}
	}

}
