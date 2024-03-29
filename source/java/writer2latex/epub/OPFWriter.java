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
 *  Copyright: 2001-2012 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  version 1.4 (2012-03-19)
 *
 */

package writer2latex.epub;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

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
import writer2latex.xmerge.DOMDocument;

/** This class writes an OPF-file for an EPUB document (see http://www.idpf.org/2007/opf/OPF_2.0_final_spec.html).
 */
public class OPFWriter extends DOMDocument {
	private String sUID=null;

	public OPFWriter(ConverterResult cr) {
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
        
        // Title and language (required)
        appendElement(contentDOM, metadata, "dc:title", cr.getMetaData().getTitle());
        appendElement(contentDOM, metadata, "dc:language", cr.getMetaData().getLanguage());
        
        // Subject and keywords in ODF both map to Dublin core subjects
        if (cr.getMetaData().getSubject().length()>0) {
        	appendElement(contentDOM, metadata, "dc:subject", cr.getMetaData().getSubject());
        }
        if (cr.getMetaData().getKeywords().length()>0) {
        	String[] sKeywords = cr.getMetaData().getKeywords().split(",");
        	for (String sKeyword : sKeywords) {
        		appendElement(contentDOM, metadata, "dc:subject", sKeyword.trim());
        	}
        }
        if (cr.getMetaData().getDescription().length()>0) {
        	appendElement(contentDOM, metadata, "dc:description", cr.getMetaData().getDescription());
        }
        
        // User defined meta data
        // The identifier, creator, contributor and date has an optional attribute and there may be multiple instances of
        // the first three. The key must be in the form name[id][.attribute]
        // where the id is some unique id amongst the instances with the same name
        // Furthermore the instances will be sorted on the id
        // Thus you can have e.g. creator1.aut="John Doe" and creator2.aut="Jane Doe", and "John Doe" will be the first author
        boolean bHasIdentifier = false;
        boolean bHasCreator = false;
        boolean bHasDate = false;
        // First rearrange the user-defined meta data
        Map<String,String> userDefinedMetaData = cr.getMetaData().getUserDefinedMetaData();
        Map<String,String[]> dc = new HashMap<String,String[]>();
        for (String sKey : userDefinedMetaData.keySet()) {
        	if (sKey.length()>0) {
        		String[] sValue = new String[2];
        		sValue[0] = userDefinedMetaData.get(sKey);
        		String sNewKey;
        		int nDot = sKey.indexOf(".");
        		if (nDot>0) {
        			sNewKey = sKey.substring(0, nDot).toLowerCase();
        			sValue[1] = sKey.substring(nDot+1);
        		}
        		else {
        			sNewKey = sKey.toLowerCase();
        			sValue[1] = null;
        		}
        		dc.put(sNewKey, sValue);
        	}
        }
        // Then export it
        String[] sKeys = Misc.sortStringSet(dc.keySet());
        for (String sKey : sKeys) {
        	String sValue = dc.get(sKey)[0];
        	String sAttributeValue = dc.get(sKey)[1];
        	if (sKey.startsWith("identifier")) {
        		Element identifier = appendElement(contentDOM, metadata, "dc:identifier", sValue);
        		if (!bHasIdentifier) { // The first identifier is the unique ID
        			identifier.setAttribute("id", "BookId");
        			sUID = sValue;
        		}
        		if (sAttributeValue!=null) {
        			identifier.setAttribute("opf:scheme", sAttributeValue);
        		}
        		bHasIdentifier = true;
        	}
        	else if (sKey.startsWith("creator")) {
        		Element creator = appendElement(contentDOM, metadata, "dc:creator", sValue);
        		creator.setAttribute("opf:file-as", fileAs(sValue));
        		if (sAttributeValue!=null) {
        			creator.setAttribute("opf:role", sAttributeValue);
        		}
        		bHasCreator = true;
        	}
        	else if (sKey.startsWith("contributor")) {
        		Element contributor = appendElement(contentDOM, metadata, "dc:contributor", sValue);
        		contributor.setAttribute("opf:file-as", fileAs(sValue));
        		if (sAttributeValue!=null) {
        			contributor.setAttribute("opf:role", sAttributeValue);
        		}
        	}
        	else if (sKey.startsWith("date")) {
        		Element date = appendElement(contentDOM, metadata, "dc:date", sValue);
        		if (sAttributeValue!=null) {
        			date.setAttribute("opf:event", sAttributeValue);
        		}
        		bHasDate = true;
        	}
        	// Remaining properties must be unique and has not attributes, hence
        	else if (sAttributeValue==null) {
        		if ("publisher".equals(sKey)) {
        			appendElement(contentDOM, metadata, "dc:publisher", sValue);
        		}
        		else if ("type".equals(sKey)) {
        			appendElement(contentDOM, metadata, "dc:type", sValue);
        		}
        		else if ("format".equals(sKey)) {
        			appendElement(contentDOM, metadata, "dc:format", sValue);
        		}
        		else if ("source".equals(sKey)) {
        			appendElement(contentDOM, metadata, "dc:source", sValue);
        		}
        		else if ("relation".equals(sKey)) {
        			appendElement(contentDOM, metadata, "dc:relation", sValue);
        		}
        		else if ("coverage".equals(sKey)) {
        			appendElement(contentDOM, metadata, "dc:coverage", sValue);
        		}
        		else if ("rights".equals(sKey)) {
        			appendElement(contentDOM, metadata, "dc:rights", sValue);
        		}
        	}
        }
        
        // Fall back values for identifier, creator and date
    	if (!bHasIdentifier) {
    		// Create a universal unique ID
    		sUID = UUID.randomUUID().toString(); 
    		Element identifier = appendElement(contentDOM, metadata, "dc:identifier", sUID);
    		identifier.setAttribute("id", "BookId");
    		identifier.setAttribute("opf:scheme", "UUID");
    	}
    	if (!bHasCreator && cr.getMetaData().getCreator().length()>0) {
    		appendElement(contentDOM, metadata, "dc:creator", cr.getMetaData().getCreator())
    			.setAttribute("opf:file-as", fileAs(cr.getMetaData().getCreator()));
    	}
    	if (!bHasDate && cr.getMetaData().getDate().length()>0) {
    		// TODO: Support meta:creation-date?
    		appendElement(contentDOM, metadata, "dc:date", Misc.dateOnly(cr.getMetaData().getDate()));
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
        	// Treat cover as recommended by Threepress consulting (http://blog.threepress.org/2009/11/20/best-practices-in-epub-cover-images/)
        	if (cr.getCoverFile()!=null && cr.getCoverFile().getFile()==file) {
        		item.setAttribute("id", "cover");
        		
        		Element itemref = contentDOM.createElement("itemref");
        		itemref.setAttribute("idref", "cover");
        		itemref.setAttribute("linear", "no"); // maybe problematic
        		spine.appendChild(itemref);
        	}
        	else if (cr.getCoverImageFile()!=null && cr.getCoverImageFile().getFile()==file) {
        		item.setAttribute("id", "cover-image");
        		
        		Element meta = contentDOM.createElement("meta");
        		meta.setAttribute("name", "cover");
        		meta.setAttribute("content", "cover-image");
        		metadata.appendChild(meta);
        	}
        	else if (file.isMasterDocument()) {
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
        addGuideReference(contentDOM,guide,"cover",cr.getCoverFile());
       	addGuideReference(contentDOM,guide,"title-page",cr.getTitlePageFile());
       	addGuideReference(contentDOM,guide,"text",cr.getTextFile());
       	addGuideReference(contentDOM,guide,"toc",cr.getTocFile());
       	addGuideReference(contentDOM,guide,"index",cr.getIndexFile());
       	addGuideReference(contentDOM,guide,"loi",cr.getLofFile());
       	addGuideReference(contentDOM,guide,"lot",cr.getLotFile());
       	addGuideReference(contentDOM,guide,"bibliography",cr.getBibliographyFile());
        
        setContentDOM(contentDOM);
	}
	
	/** Get the unique ID associated with this EPUB document (either collected from the user-defined
	 *  meta data or a generated UUID)
	 * 
	 * @return the ID
	 */
	public String getUid() {
		return sUID;
	}
	
	private String fileAs(String sName) {
		int nSpace = sName.lastIndexOf(' ');
		if (nSpace>-1) {
			return sName.substring(nSpace+1).trim()+", "+sName.substring(0, nSpace).trim();
		}
		else {
			return sName.trim();
		}
	}
	
	private Element appendElement(Document contentDOM, Element node, String sTagName, String sContent) {
		Element child = contentDOM.createElement(sTagName);
		node.appendChild(child);
		child.appendChild(contentDOM.createTextNode(sContent));
		return child;
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
