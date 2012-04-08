/************************************************************************
 *
 *  ConverterBase.java
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
 *  Copyright: 2002-2012 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.4 (2012-04-07)
 *
 */

package writer2latex.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2latex.api.GraphicConverter;
import writer2latex.api.Converter;
import writer2latex.api.ConverterResult;
import writer2latex.api.OutputFile;
import writer2latex.office.EmbeddedObject;
import writer2latex.office.EmbeddedXMLObject;
import writer2latex.office.ImageLoader;
import writer2latex.office.MIMETypes;
import writer2latex.office.MetaData;
import writer2latex.office.OfficeDocument;
import writer2latex.office.OfficeReader;
import writer2latex.office.XMLString;
import writer2latex.util.Misc;

/**<p>Abstract base implementation of <code>writer2latex.api.Converter</code></p>
 */
public abstract class ConverterBase implements Converter {

    // Helper	
    protected GraphicConverter graphicConverter;

    // The source document
    protected OfficeDocument odDoc;
    protected OfficeReader ofr;
    protected MetaData metaData;
    protected ImageLoader imageLoader;

    // The output file(s)
    protected String sTargetFileName;
    protected ConverterResultImpl converterResult;
    
    // Result of latest parsing of a display equation
    private Element theEquation = null;
	private Element theSequence = null;
		
    // Constructor
    public ConverterBase() {
        graphicConverter = null;
        converterResult = new ConverterResultImpl();
    }
	
    // Implement the interface
    public void setGraphicConverter(GraphicConverter graphicConverter) {
        this.graphicConverter = graphicConverter;
    }
	
    // Provide a do noting fallback method
    public void readTemplate(InputStream is) throws IOException { }
	
    // Provide a do noting fallback method
    public void readTemplate(File file) throws IOException { }

    // Provide a do noting fallback method
    public void readStyleSheet(InputStream is) throws IOException { }
	
    // Provide a do noting fallback method
    public void readStyleSheet(File file) throws IOException { }

    // Provide a do noting fallback method
    public void readResource(InputStream is, String sFileName, String sMediaType) throws IOException { }
    
    // Provide a do noting fallback method
    public void readResource(File file, String sFileName, String sMediaType) throws IOException { }

    public ConverterResult convert(File source, String sTargetFileName) throws FileNotFoundException,IOException {
        return convert(new FileInputStream(source), sTargetFileName);
    }

    public ConverterResult convert(InputStream is, String sTargetFileName) throws IOException {
        // Read document
        odDoc = new OfficeDocument();
        odDoc.read(is);
        return convert(sTargetFileName);
    }
    
    public ConverterResult convert(org.w3c.dom.Document dom, String sTargetFileName) throws IOException {
    	// Read document
    	odDoc = new OfficeDocument();
    	odDoc.read(dom);
    	return convert(sTargetFileName);
    }
    
    private ConverterResult convert(String sTargetFileName) throws IOException {
        ofr = new OfficeReader(odDoc,false);
        metaData = new MetaData(odDoc);
        imageLoader = new ImageLoader(odDoc,true);
        imageLoader.setGraphicConverter(graphicConverter);

        // Prepare output
        this.sTargetFileName = sTargetFileName;
        converterResult.reset();
        
        converterResult.setMetaData(metaData);
        if (metaData.getLanguage()==null || metaData.getLanguage().length()==0) {
        	metaData.setLanguage(ofr.getMajorityLanguage());
        }
		
        convertInner();
        
        return converterResult;
    }
	
    // The subclass must provide the implementation
    public abstract void convertInner() throws IOException;

    public MetaData getMetaData() { return metaData; }
    
    public ImageLoader getImageLoader() { return imageLoader; }
	
    public void addDocument(OutputFile doc) { converterResult.addDocument(doc); }
	
    public EmbeddedObject getEmbeddedObject(String sHref) {
        return odDoc.getEmbeddedObject(sHref);
    }
    
	/** Get the equation found by the last invocation of <code>parseDisplayEquation</code>
	 * 
	 * @return the equation or null if no equation was found
	 */
	public Element getEquation() {
		return theEquation;
	}
	
	/** Get the sequence number found by the last invocation of <code>parseDisplayEquation</code>
	 * 
	 * @return the sequence number or null if no sequence number was found
	 */
	public Element getSequence() {
		return theSequence;
	}
	
	/** Determine whether or not a paragraph contains a display equation.
	 *  A paragraph is a display equation if it contains a single formula and no text content except whitespace
	 *  and an optional sequence number which may be in brackets.
	 *  As a side effect, this method keeps a reference to the equation and the sequence number
	 * 
	 * @param node the paragraph
	 * @return true if this is a display equation
	 */
	public boolean parseDisplayEquation(Node node) {
		theEquation = null;
		theSequence = null;
		return doParseDisplayEquation(node);
	}
	
    private boolean doParseDisplayEquation(Node node) {
        Node child = node.getFirstChild();
        while (child!=null) {
            Node equation = getFormula(child);
            if (equation!=null) {
                if (theEquation==null) {
                    theEquation = (Element) equation;
                }
                else { // two or more equations -> not a display
                    return false;
                }
            }
            else if (Misc.isElement(child)) {
                String sName = child.getNodeName();
                if (XMLString.TEXT_SEQUENCE.equals(sName)) {
                    if (theSequence==null) {
                        theSequence = (Element) child;
                    }
                    else { // two sequence numbers -> not a display
                        return false;
                    }
                }
                else if (XMLString.TEXT_SPAN.equals(sName)) {
                    if (!doParseDisplayEquation(child)) {
                        return false;
                    }
                }
                else if (XMLString.TEXT_S.equals(sName)) {
                    // Spaces are allowed
                }
                else if (XMLString.TEXT_TAB.equals(sName)) {
                    // Tab stops are allowed
                }
                else if (XMLString.TEXT_TAB_STOP.equals(sName)) { // old
                    // Tab stops are allowed
                }
                else if (XMLString.TEXT_SOFT_PAGE_BREAK.equals(sName)) { // since ODF 1.1
                	// Soft page breaks are allowed
                }
                else {
                    // Other elements -> not a display
                    return false;
                }
            }
            else if (Misc.isText(child)) {
                String s = child.getNodeValue();
                int nLen = s.length();
                for (int i=0; i<nLen; i++) {
                    char c = s.charAt(i);
                    if (c!='(' && c!=')' && c!='[' && c!=']' && c!='{' && c!='}' && c!=' ' && c!='\u00A0') {
                        // Characters except brackets and whitespace -> not a display
                        return false;
                    }
                }
            }
            child = child.getNextSibling();
        }
        return true;
    }
    
    // TODO: Extend OfficeReader to handle frames
    private Node getFormula(Node node) {
        if (Misc.isElement(node,XMLString.DRAW_FRAME)) {
            node=Misc.getFirstChildElement(node);
        }
        
        String sHref = Misc.getAttribute(node,XMLString.XLINK_HREF);
		
        if (sHref!=null) { // Embedded object in package or linked object
            if (ofr.isInPackage(sHref)) { // Embedded object in package
                if (sHref.startsWith("#")) { sHref=sHref.substring(1); }
                if (sHref.startsWith("./")) { sHref=sHref.substring(2); }
                EmbeddedObject object = getEmbeddedObject(sHref); 
                if (object!=null) {
                    if (MIMETypes.MATH.equals(object.getType()) || MIMETypes.ODF.equals(object.getType())) { // Formula!
                        try {
                            Document formuladoc = ((EmbeddedXMLObject) object).getContentDOM();
                            Element formula = Misc.getChildByTagName(formuladoc,XMLString.MATH); // Since OOo 3.2
                            if (formula==null) {
                            	formula = Misc.getChildByTagName(formuladoc,XMLString.MATH_MATH);
                            }
                            return formula;
                        }
                        catch (org.xml.sax.SAXException e) {
                            e.printStackTrace();
                        }
                        catch (java.io.IOException e) {
                            e.printStackTrace();
                        }
	                }
                }
            }
        }
        else { // flat XML, object is contained in node
            Element formula = Misc.getChildByTagName(node,XMLString.MATH); // Since OOo 3.2
            if (formula==null) {
            	formula = Misc.getChildByTagName(node,XMLString.MATH_MATH);
            }
            return formula;
        }
        return null;
    }



}