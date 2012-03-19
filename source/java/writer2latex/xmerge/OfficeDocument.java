/************************************************************************
 *
 *  The Contents of this file are made available subject to the terms of
 *
 *         - GNU Lesser General Public License Version 2.1
 *
 *  Sun Microsystems Inc., October, 2000
 *
 *  GNU Lesser General Public License Version 2.1
 *  =============================================
 *  Copyright 2000 by Sun Microsystems, Inc.
 *  901 San Antonio Road, Palo Alto, CA 94303, USA
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
 *  The Initial Developer of the Original Code is: Sun Microsystems, Inc.
 *
 *  Copyright: 2000 by Sun Microsystems, Inc.
 *
 *  All Rights Reserved.
 *
 *  Contributor(s): _______________________________________
 *
 *
 ************************************************************************/
 
// This version is adapted for Writer2LaTeX
// Version 1.4 (2012-03-19)

package writer2latex.xmerge;

import java.io.InputStream;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;

import writer2latex.office.MIMETypes;
import writer2latex.util.Misc;

/**
 *  This class implements reading of ODF files
 */
public class OfficeDocument
    implements OfficeConstants {

    /** Factory for <code>DocumentBuilder</code> objects. */
    private static DocumentBuilderFactory factory =
       DocumentBuilderFactory.newInstance();

    /** DOM <code>Document</code> of content.xml. */
    private Document contentDoc = null;

   /** DOM <code>Document</code> of meta.xml. */
    private Document metaDoc = null;

   /** DOM <code>Document</code> of settings.xml. */
    private Document settingsDoc = null;

    /** DOM <code>Document</code> of content.xml. */
    private Document styleDoc = null;
    
    /** DOM <code>Document</code> of META-INF/manifest.xml. */
    private Document manifestDoc = null;

    private String documentName = null;
    private String fileName = null;

    /**
     *  <code>OfficeZip</code> object to store zip contents from
     *  read <code>InputStream</code>.  Note that this member
     *  will still be null if it was initialized using a template
     *  file instead of reading from a StarOffice zipped
     *  XML file.
     */
    private OfficeZip zip = null;
       
    /** Collection to keep track of the embedded objects in the document. */
    private Map<String, EmbeddedObject> embeddedObjects = null;

    /**
     *  Default constructor.
     *
     *  @param  name  <code>Document</code> name.
     */
    public OfficeDocument(String name)
    {
        this(name, true, false);
    }


    /**
     *  Constructor with arguments to set <code>namespaceAware</code>
     *  and <code>validating</code> flags.
     *
     *  @param  name            <code>Document</code> name (may or may not
     *                          contain extension).
     *  @param  namespaceAware  Value for <code>namespaceAware</code> flag.
     *  @param  validating      Value for <code>validating</code> flag.
     */
    public OfficeDocument(String name, boolean namespaceAware, boolean validating) {

        //res = Resources.getInstance();
        factory.setValidating(validating);
        factory.setNamespaceAware(namespaceAware);
        this.documentName = trimDocumentName(name);
        this.fileName = documentName + getFileExtension();
    }


    /**
     *  Removes the file extension from the <code>Document</code>
     *  name.
     *
     *  @param  name  Full <code>Document</code> name with extension.
     *
     *  @return  Name of <code>Document</code> without the extension.
     */
    private String trimDocumentName(String name) {
        String temp = name.toLowerCase();
        String ext = getFileExtension();

        if (temp.endsWith(ext)) {
            // strip the extension
            int nlen = name.length();
            int endIndex = nlen - ext.length();
            name = name.substring(0,endIndex);
        }

        return name;
    }

    // FIX2 (HJ): Determine wether this is package or flat format
    /** Package or flat format? 
     *  @return true if the document is in package format, false if it's flat xml
     */
    public boolean isPackageFormat() { return zip!=null; }

    /**
     *  Return a DOM <code>Document</code> object of the content.xml
     *  file.  Note that a content DOM is not created when the constructor
     *  is called.  So, either the <code>read</code> method or the
     *  <code>initContentDOM</code> method will need to be called ahead
     *  on this object before calling this method.
     *
     *  @return  DOM <code>Document</code> object.
     */
    public Document getContentDOM() {

        return contentDoc;
    }

 /**
     *  Return a DOM <code>Document</code> object of the meta.xml
     *  file.  Note that a content DOM is not created when the constructor
     *  is called.  So, either the <code>read</code> method or the
     *  <code>initContentDOM</code> method will need to be called ahead
     *  on this object before calling this method.
     *
     *  @return  DOM <code>Document</code> object.
     */
    public Document getMetaDOM() {

        return metaDoc;
    }


 /**
     *  Return a DOM <code>Document</code> object of the settings.xml
     *  file.  Note that a content DOM is not created when the constructor
     *  is called.  So, either the <code>read</code> method or the
     *  <code>initContentDOM</code> method will need to be called ahead
     *  on this object before calling this method.
     *
     *  @return  DOM <code>Document</code> object.
     */
    public Document getSettingsDOM() {

        return settingsDoc;
    }


    /**
     *  Return a DOM <code>Document</code> object of the style.xml file.
     *  Note that this may return null if there is no style DOM.
     *  Note that a style DOM is not created when the constructor
     *  is called.  Depending on the <code>InputStream</code>, a
     *  <code>read</code> method may or may not build a style DOM.  When
     *  creating a new style DOM, call the <code>initStyleDOM</code> method
     *  first.
     *
     *  @return  DOM <code>Document</code> object.
     */
    public Document getStyleDOM() {

        return styleDoc;
    }


    /**
     *  Return the name of the <code>Document</code>.
     *
     *  @return  The name of <code>Document</code>.
     */
    public String getName() {

        return documentName;
    }


    /**
     *  Return the file name of the <code>Document</code>, possibly
     *  with the standard extension.
     *
     *  @return  The file name of <code>Document</code>.
     */
    public String getFileName() {

        return fileName;
    }


    /**
     *  Returns the file extension for this type of
     *  <code>Document</code>.
     *
     *  @return  The file extension of <code>Document</code>.
     */
	// TODO: is this used?
    protected String getFileExtension() { return ""; }


    /**
     * Returns all the embedded objects (graphics, formulae, etc.) present in
     * this document.
     *
     * @return An <code>Iterator</code> of <code>EmbeddedObject</code> objects.
     */
    public Iterator<EmbeddedObject> getEmbeddedObjects() {
        
        if (embeddedObjects == null && manifestDoc != null) {            
            embeddedObjects = new HashMap<String, EmbeddedObject>();           
            
            // Need to read the manifest file and construct a list of objects                       
            NodeList nl = manifestDoc.getElementsByTagName(TAG_MANIFEST_FILE);
                      
            // Dont create the HashMap if there are no embedded objects
            int len = nl.getLength();
            for (int i = 0; i < len; i++) {
                Node n = nl.item(i);
                
                NamedNodeMap attrs = n.getAttributes();
                
                String type = attrs.getNamedItem(ATTRIBUTE_MANIFEST_FILE_TYPE).getNodeValue();
                String path = attrs.getNamedItem(ATTRIBUTE_MANIFEST_FILE_PATH).getNodeValue();
                
                
                /*
                 * According to OpenOffice.org XML File Format document (ver. 1)
                 * there are only two types of embedded object:
                 *
                 *      Objects with an XML representation.
                 *      Objects without an XML representation.
                 *
                 * The former are represented by one or more XML files.
                 * The latter are in binary form.
                 */
                // FIX2 (HJ): Allow either OOo 1.x or OpenDocument embedded objects
                if (type.startsWith("application/vnd.sun.xml") || type.startsWith("application/vnd.oasis.opendocument"))       
                {
                    if (path.equals("/")) {
                        // Exclude the main document entries
                        continue;
                    }
                    // Take off the trailing '/'
                    String name = path.substring(0, path.length() - 1);
                    embeddedObjects.put(name, new EmbeddedXMLObject(name, type, zip));
                }
                else if (type.equals("text/xml")) {
                    // XML entries are either embedded StarOffice doc entries or main
                    // document entries
                    continue;
                }
                else { // FIX (HJ): allows empty MIME type      
                    embeddedObjects.put(path, new EmbeddedBinaryObject(path, type, zip));
                }
            }
        }
        
        return embeddedObjects.values().iterator();
    }
    
    /**
     * Returns the embedded object corresponding to the name provided.
     * The name should be stripped of any preceding path characters, such as 
     * '/', '.' or '#'.
     *
     * @param   name    The name of the embedded object to retrieve.
     *
     * @return  An <code>EmbeddedObject</code> instance representing the named 
     *          object.
     */
    public EmbeddedObject getEmbeddedObject(String name) {
        if (name == null) {
            return null;
        }
        
        if (embeddedObjects == null) {
            // FIX2 (HJ): Return null if there's no manifest
            if (manifestDoc != null) {            
                getEmbeddedObjects();
            }
            else {
                return null;
            }
        }
        
        if (embeddedObjects.containsKey(name)) {
            return embeddedObjects.get(name);
        }
        else {
            return null;
        }
    }
    
    
    /**
     * Adds a new embedded object to the document.
     *
     * @param   embObj  An instance of <code>EmbeddedObject</code>.
     */
    /*public void addEmbeddedObject(EmbeddedObject embObj) {
        if (embObj == null) {
            return;
        }
        
        if (embeddedObjects == null) {
            embeddedObjects = new HashMap<String, EmbeddedObject>();
        }
        
        embeddedObjects.put(embObj.getName(), embObj);
    }*/
    
    
    /**
     *  Read the Office <code>Document</code> from the given
     *  <code>InputStream</code>.
     *  FIX3 (HJ): Perform simple type detection to determine package or flat format
     *
     *  @param  is  Office document <code>InputStream</code>.
     *
     *  @throws  IOException  If any I/O error occurs.
     */
    public void read(InputStream is) throws IOException {
        byte[] doc = Misc.inputStreamToByteArray(is);
        boolean bZip = MIMETypes.ZIP.equals(MIMETypes.getMagicMIMEType(doc));
        // if it's zip, assume package - otherwise assume flat
        read(new ByteArrayInputStream(doc),bZip);
    }

    private void readZip(InputStream is) throws IOException {

        // Debug.log(Debug.INFO, "reading Office file");

        DocumentBuilder builder = null;

        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            throw new OfficeDocumentException(ex);
        }

        // read in Office zip file format

        zip = new OfficeZip();
        zip.read(is);

        // grab the content.xml and
        // parse it into contentDoc.

        byte contentBytes[] = zip.getContentXMLBytes();

        if (contentBytes == null) {

            throw new OfficeDocumentException("Entry content.xml not found in file");
        }

        try {

            contentDoc = parse(builder, contentBytes);

        } catch (SAXException ex) {

            throw new OfficeDocumentException(ex);
        }

        // if style.xml exists, grab the style.xml
        // parse it into styleDoc.

        byte styleBytes[] = zip.getStyleXMLBytes();

        if (styleBytes != null) {

            try {

                styleDoc = parse(builder, styleBytes);

            } catch (SAXException ex) {

                throw new OfficeDocumentException(ex);
            }
        }

	byte metaBytes[] = zip.getMetaXMLBytes();

        if (metaBytes != null) {

            try {

                metaDoc = parse(builder, metaBytes);

            } catch (SAXException ex) {

                throw new OfficeDocumentException(ex);
            }
        }

	byte settingsBytes[] = zip.getSettingsXMLBytes();

        if (settingsBytes != null) {

            try {

                settingsDoc = parse(builder, settingsBytes);

            } catch (SAXException ex) {

                throw new OfficeDocumentException(ex);
            }
        }

        
        // Read in the META-INF/manifest.xml file
        byte manifestBytes[] = zip.getManifestXMLBytes();
        
        if (manifestBytes != null) {
            
            try {
                manifestDoc = parse(builder, manifestBytes);
            } catch (SAXException ex) {
                throw new OfficeDocumentException(ex);
            }
        }

    }


    /**
     *  Read the Office <code>Document</code> from the given
     *  <code>InputStream</code>.
     *
     *  @param  is  Office document <code>InputStream</code>.
     *  @param  isZip <code>boolean</code> Identifies whether 
     *                 a file is zipped or not
     *
     *  @throws  IOException  If any I/O error occurs.
     */
    public void read(InputStream is, boolean isZip) throws IOException {

        // Debug.log(Debug.INFO, "reading Office file");

        DocumentBuilder builder = null;

        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            throw new OfficeDocumentException(ex);
        }
	
	if (isZip)
	{
            readZip(is);
	}
	else{
	    try{
		//contentDoc=  builder.parse((InputStream)is);
               Reader r = secondHack(is);
               InputSource ins = new InputSource(r);
	        org.w3c.dom.Document newDoc = builder.parse(ins);
	        //org.w3c.dom.Document newDoc = builder.parse((InputStream)is);
	        Element rootElement=newDoc.getDocumentElement();
                
	        NodeList nodeList;
	        Node tmpNode;
	        Node rootNode = (Node)rootElement;
                if (newDoc !=null){
		    /*content*/
                   contentDoc = createDOM(TAG_OFFICE_DOCUMENT_CONTENT);
                   rootElement=contentDoc.getDocumentElement();
                   rootNode = (Node)rootElement;
                   
                   // FIX (HJ): Include office:font-decls in content DOM
                   nodeList= newDoc.getElementsByTagName(TAG_OFFICE_FONT_DECLS);
                   if (nodeList.getLength()>0){
                       tmpNode = contentDoc.importNode(nodeList.item(0),true);
                       rootNode.appendChild(tmpNode);
                   }
                   
                   // FIX2 (HJ): Include office:font-face-decls (OpenDocument) in content DOM
                   nodeList= newDoc.getElementsByTagName(TAG_OFFICE_FONT_FACE_DECLS);
                   if (nodeList.getLength()>0){
                       tmpNode = contentDoc.importNode(nodeList.item(0),true);
                       rootNode.appendChild(tmpNode);
                   }
                   
                   nodeList= newDoc.getElementsByTagName(TAG_OFFICE_AUTOMATIC_STYLES);
                   if (nodeList.getLength()>0){
	              tmpNode = contentDoc.importNode(nodeList.item(0),true);
	              rootNode.appendChild(tmpNode);
                   }
                   
                    nodeList= newDoc.getElementsByTagName(TAG_OFFICE_BODY);
                   if (nodeList.getLength()>0){
	              tmpNode = contentDoc.importNode(nodeList.item(0),true);
	              rootNode.appendChild(tmpNode);
                   }
                  
		   /*Styles*/
                   styleDoc = createDOM(TAG_OFFICE_DOCUMENT_STYLES);
                   rootElement=styleDoc.getDocumentElement();
                   rootNode = (Node)rootElement;
				   
                   // FIX (HJ): Include office:font-decls in styles DOM
                   nodeList= newDoc.getElementsByTagName(TAG_OFFICE_FONT_DECLS);
                   if (nodeList.getLength()>0){
    	              tmpNode = styleDoc.importNode(nodeList.item(0),true);
                      rootNode.appendChild(tmpNode);
                   }
				   
                   // FIX2 (HJ): Include office:font-face-decls in styles DOM
                   nodeList= newDoc.getElementsByTagName(TAG_OFFICE_FONT_FACE_DECLS);
                   if (nodeList.getLength()>0){
    	              tmpNode = styleDoc.importNode(nodeList.item(0),true);
                      rootNode.appendChild(tmpNode);
                   }
				   
                   nodeList= newDoc.getElementsByTagName(TAG_OFFICE_STYLES);
                   if (nodeList.getLength()>0){
	              tmpNode = styleDoc.importNode(nodeList.item(0),true);
	              rootNode.appendChild(tmpNode);
                   }

                   // FIX (HJ): Include office:automatic-styles in styles DOM
                   nodeList= newDoc.getElementsByTagName(TAG_OFFICE_AUTOMATIC_STYLES);
                   if (nodeList.getLength()>0){
                      tmpNode = styleDoc.importNode(nodeList.item(0),true);
                      rootNode.appendChild(tmpNode);
                   }
				   
                   // FIX (HJ): Include office:master-styles in styles DOM
                   nodeList= newDoc.getElementsByTagName(TAG_OFFICE_MASTER_STYLES);
                   if (nodeList.getLength()>0){
                       tmpNode = styleDoc.importNode(nodeList.item(0),true);
                       rootNode.appendChild(tmpNode);
                   }

		   /*Settings*/
                   settingsDoc = createDOM(TAG_OFFICE_DOCUMENT_SETTINGS);
                   rootElement=settingsDoc.getDocumentElement();
                   rootNode = (Node)rootElement;
                   nodeList= newDoc.getElementsByTagName(TAG_OFFICE_SETTINGS);
                   if (nodeList.getLength()>0){
	              tmpNode = settingsDoc.importNode(nodeList.item(0),true);
	              rootNode.appendChild(tmpNode);
                   } 
		   /*Meta*/
                   metaDoc = createDOM(TAG_OFFICE_DOCUMENT_META);
                   rootElement=metaDoc.getDocumentElement();
                   rootNode = (Node)rootElement;
                   nodeList= newDoc.getElementsByTagName(TAG_OFFICE_META);
                   if (nodeList.getLength()>0){
	              tmpNode = metaDoc.importNode(nodeList.item(0),true);
	              rootNode.appendChild(tmpNode);
                   } 
                }
	    }
	    catch (SAXException ex) {
	    	throw new OfficeDocumentException(ex);
	    }
	}
      
    }



    /**
     *  Parse given <code>byte</code> array into a DOM
     *  <code>Document</code> object using the
     *  <code>DocumentBuilder</code> object.
     *
     *  @param  builder  <code>DocumentBuilder</code> object for parsing.
     *  @param  bytes    <code>byte</code> array for parsing.
     *
     *  @return  Resulting DOM <code>Document</code> object.
     *
     *  @throws  SAXException  If any parsing error occurs.
     */
    static Document parse(DocumentBuilder builder, byte bytes[])
        throws SAXException, IOException {

        Document doc = null;

        ByteArrayInputStream is = new ByteArrayInputStream(bytes);

        // TODO:  replace hack with a more appropriate fix.

        Reader r = hack(is);
        InputSource ins = new InputSource(r);
        doc = builder.parse(ins);

        return doc;
    }
    
    
    /**
     *  <p>Creates a new DOM <code>Document</code> containing minimum
     *  OpenOffice XML tags.</p>
     *
     *  <p>This method uses the subclass
     *  <code>getOfficeClassAttribute</code> method to get the
     *  attribute for <i>office:class</i>.</p>
     *
     *  @param  rootName  root name of <code>Document</code>.
     *
     *  @throws  IOException  If any I/O error occurs.
     */
    private final Document createDOM(String rootName) throws IOException {

        Document doc = null;

        try {

            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.newDocument();

        } catch (ParserConfigurationException ex) {

            throw new OfficeDocumentException(ex);

        }

        Element root = (Element) doc.createElement(rootName);
        doc.appendChild(root);

        root.setAttribute("xmlns:office", "http://openoffice.org/2000/office");
        root.setAttribute("xmlns:style", "http://openoffice.org/2000/style");
        root.setAttribute("xmlns:text", "http://openoffice.org/2000/text");
        root.setAttribute("xmlns:table", "http://openoffice.org/2000/table");
        root.setAttribute("xmlns:draw", "http://openoffice.org/2000/drawing");
        root.setAttribute("xmlns:fo", "http://www.w3.org/1999/XSL/Format");
        root.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
        root.setAttribute("xmlns:number", "http://openoffice.org/2000/datastyle");
        root.setAttribute("xmlns:svg", "http://www.w3.org/2000/svg");
        root.setAttribute("xmlns:chart", "http://openoffice.org/2000/chart");
        root.setAttribute("xmlns:dr3d", "http://openoffice.org/2000/dr3d");
        root.setAttribute("xmlns:math", "http://www.w3.org/1998/Math/MathML");
        root.setAttribute("xmlns:form", "http://openoffice.org/2000/form");
        root.setAttribute("xmlns:script", "http://openoffice.org/2000/script");
        root.setAttribute("office:class", getOfficeClassAttribute());
        root.setAttribute("office:version", "1.0");

        return doc;
    }


    /**
     *  Return the <i>office:class</i> attribute value.
     *
     *  @return  The attribute value.
     */
	// not really used...
    protected String getOfficeClassAttribute() { return ""; }


    /**
     *  <p>Hacked code to filter <!DOCTYPE> tag before
     *  sending stream to parser.</p>
     *
     *  <p>This hacked code needs to be changed later on.</p>
     *
     *  <p>Issue: using current jaxp1.0 parser, there is no way
     *  to turn off processing of dtds.  Current set of dtds
     *  have bugs, processing them will throw exceptions.</p>
     *
     *  <p>This is a simple hack that assumes the whole <!DOCTYPE>
     *  tag are all in the same line.  This is sufficient for
     *  current StarOffice 6.0 generated XML files.  Since this
     *  hack really needs to go away, I don't want to spend
     *  too much time in making it a perfect hack.</p>
     *  FIX (HJ): Removed requirement for DOCTYPE to be in one line
     *  FIX (HJ): No longer removes newlines
     *
     *  @param  is  <code>InputStream</code> to be filtered.
     *
     *  @return  Reader value without the <!DOCTYPE> tag.
     *
     *  @throws  IOException  If any I/O error occurs.
     */
    private static Reader hack(InputStream is) throws IOException {

        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuffer buffer = new StringBuffer();

        String str = null;

        while ((str = br.readLine()) != null) {

            int sIndex = str.indexOf("<!DOCTYPE");

            if (sIndex > -1) {

                buffer.append(str.substring(0, sIndex));

                int eIndex = str.indexOf('>', sIndex + 8 );

                if (eIndex > -1) {

                    buffer.append(str.substring(eIndex + 1, str.length()));
                    // FIX (HJ): Preserve the newline
                    buffer.append("\n");

                } else {

                    // FIX (HJ): More than one line. Search for '>' in following lines
                    boolean bOK = false;
                    while ((str = br.readLine())!=null) {
                        eIndex = str.indexOf('>');
                        if (eIndex>-1) {
                            buffer.append(str.substring(eIndex+1));
                            // FIX (HJ): Preserve the newline
                            buffer.append("\n");
                            bOK = true;
                            break;
                        }
                    }

                    if (!bOK) { throw new IOException("Invalid XML"); }
                }

            } else {

                buffer.append(str);
                // FIX (HJ): Preserve the newline
                buffer.append("\n");
            }
        }

        StringReader r = new StringReader(buffer.toString());
        return r;
    }

    /**
     *  <p>Transform the InputStream to a Reader Stream.</p>
     *
     *  <p>This hacked code needs to be changed later on.</p>
     *
     *  <p>Issue: the new oasis input file stream means 
     *  that the old input stream fails. see #i33702# </p>
     *
     *  @param  is  <code>InputStream</code> to be filtered.
     *
     *  @return  Reader value of the InputStream().
     *
     *  @throws  IOException  If any I/O error occurs.
     */
    private static Reader secondHack(InputStream is) throws IOException {

        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        char[] charArray = new char[4096];
        StringBuffer sBuf = new StringBuffer();
        int n = 0;
        while ((n=br.read(charArray, 0, charArray.length)) > 0)
            sBuf.append(charArray, 0, n);
        
        // ensure there is no trailing garbage after the end of the stream.
        int sIndex = sBuf.lastIndexOf("</office:document>");
        sBuf.delete(sIndex, sBuf.length());
        sBuf.append("</office:document>");
        StringReader r = new StringReader(sBuf.toString());
        return r;
    }
    
    
}

