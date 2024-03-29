/************************************************************************
 *
 *  MathmlConverter.java
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
 *  Version 1.2 (2012-02-23)
 *
 */

package writer2latex.latex;

// TODO: Use parseDisplayEquation of ConverterBase

//import java.util.Hashtable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

//import writer2latex.latex.i18n.I18n;
import writer2latex.office.EmbeddedObject;
import writer2latex.office.EmbeddedXMLObject;
import writer2latex.office.MIMETypes;
import writer2latex.office.OfficeReader;
import writer2latex.office.TableReader;
import writer2latex.office.XMLString;
import writer2latex.util.Misc;

/**
 *  This class converts mathml nodes to LaTeX.
 *  (Actually it only converts the starmath annotation currently, if available).
 */
public final class MathmlConverter extends ConverterHelper {
    
    private StarMathConverter smc;
	
    private boolean bContainsFormulas = false;
    private boolean bAddParAfterDisplay = false;
	
    public MathmlConverter(OfficeReader ofr,LaTeXConfig config, ConverterPalette palette) {
        super(ofr,config,palette);
        smc = new StarMathConverter(palette.getI18n(),config);
        bAddParAfterDisplay = config.formatting()>=LaTeXConfig.CONVERT_MOST;
    }

    public void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl) {
        if (bContainsFormulas) {
            if (config.useOoomath()) {
                pack.append("\\usepackage{ooomath}").nl();
            }
            else {
                smc.appendDeclarations(pack,decl);
            }
        }
    }
	
    public String convert(Node settings, Node formula) {
        // TODO: Use settings to determine display mode/text mode
        // formula must be a math:math node
        // First try to find a StarMath annotation
    	Node semantics = Misc.getChildByTagName(formula,XMLString.SEMANTICS); // Since OOo 3.2
    	if (semantics==null) {
    		semantics = Misc.getChildByTagName(formula,XMLString.MATH_SEMANTICS);
    	}
		if (semantics!=null) {
			Node annotation = Misc.getChildByTagName(semantics,XMLString.ANNOTATION); // Since OOo 3.2
			if (annotation==null) {
				annotation = Misc.getChildByTagName(semantics,XMLString.MATH_ANNOTATION);
			}
            if (annotation!=null) {
                String sStarMath = "";
                if (annotation.hasChildNodes()) {
                    NodeList anl = annotation.getChildNodes();
                    int nLen = anl.getLength();
                    for (int i=0; i<nLen; i++) {
                        if (anl.item(i).getNodeType() == Node.TEXT_NODE) {
                            sStarMath+=anl.item(i).getNodeValue();
                        }
                    }
                    bContainsFormulas = true;      
                    return smc.convert(sStarMath);
                }
            }
        }
        // No annotation was found. In this case we should convert the mathml,
        // but currently we ignore the problem.
        // TODO: Investigate if Vasil I. Yaroshevich's MathML->LaTeX
        // XSL transformation could be used here. (Potential problem:
        // OOo uses MathML 1.01, not MathML 2)
		if (formula.hasChildNodes()) {
			return "\\text{Warning: No StarMath annotation}";
		}
		else { // empty formula
			return " ";
		}
			
    }
	
    // Data for display equations
    private Element theEquation = null;
    private Element theSequence = null;
    
    /** Try to convert a table as a display equation:
     *  A 1 row by 2 columns table in which each cell contains exactly one paragraph,
     *  the left cell contains exactly one formula and the right cell contains exactly
     *  one sequence number is treated as a (numbered) display equation.
     *  This happens to coincide with the AutoText provided with OOo Writer :-)
     *  @param table the table reader
     *  @param ldp the LaTeXDocumentPortion to contain the converted equation
     *  @return true if the conversion was successful, false if the table
     * did not represent a display equation
     */
    public boolean handleDisplayEquation(TableReader table, LaTeXDocumentPortion ldp) {
    	if (table.getRowCount()==1 && table.getColCount()==2 &&
    		OfficeReader.isSingleParagraph(table.getCell(0, 0)) && OfficeReader.isSingleParagraph(table.getCell(0, 1)) ) {
    		// Table of the desired form
    		theEquation = null;
    		theSequence = null;
    		if (parseDisplayEquation(Misc.getFirstChildElement(table.getCell(0, 0))) && theEquation!=null && theSequence==null) {
    			// Found equation in first cell
    			Element myEquation = theEquation;
        		theEquation = null;
        		theSequence = null;
    			if (parseDisplayEquation(Misc.getFirstChildElement(table.getCell(0, 1))) && theEquation==null && theSequence!=null) {
    				// Found sequence in second cell
    				handleDisplayEquation(myEquation, theSequence, ldp);
    				return true;
    			}
    		}
    	}
    	return false;
    }

    /**Try to convert a paragraph as a display equation:
     * A paragraph which contains exactly one formula + at most one sequence
     * number is treated as a display equation. Other content must be brackets
     * or whitespace (possibly with formatting).
     * @param node the paragraph
     * @param ldp the LaTeXDocumentPortion to contain the converted equation
     * @return true if the conversion was successful, false if the paragraph
     * did not contain a display equation
     */
    public boolean handleDisplayEquation(Element node, LaTeXDocumentPortion ldp) {
        theEquation = null;
        theSequence = null;
        if (parseDisplayEquation(node) && theEquation!=null) {
        	handleDisplayEquation(theEquation, theSequence, ldp);
        	return true;
        }
        else {
            return false;
        }
    }
    
    private void handleDisplayEquation(Element equation, Element sequence, LaTeXDocumentPortion ldp) {
    	String sLaTeX = convert(null,equation);
    	if (!" ".equals(sLaTeX)) { // ignore empty formulas
    		if (sequence!=null) {
    			// Numbered equation
    			ldp.append("\\begin{equation}");
    			palette.getFieldCv().handleSequenceLabel(sequence,ldp);
    			ldp.nl()
    			.append(sLaTeX).nl()
    			.append("\\end{equation}").nl();
    			if (bAddParAfterDisplay) { ldp.nl(); }
    		}
    		else {
    			// Unnumbered equation
    			ldp.append("\\begin{equation*}").nl()
    			.append(sLaTeX).nl()
    			.append("\\end{equation*}").nl();
    			if (bAddParAfterDisplay) { ldp.nl(); }
    		}    	
    	}
    }
	
    private boolean parseDisplayEquation(Node node) {
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
                    if (!parseDisplayEquation(child)) {
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
                EmbeddedObject object = palette.getEmbeddedObject(sHref); 
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
        else { // flat xml, object is contained in node
            Element formula = Misc.getChildByTagName(node,XMLString.MATH); // Since OOo 3.2
            if (formula==null) {
            	formula = Misc.getChildByTagName(node,XMLString.MATH_MATH);
            }
            return formula;
        }
        return null;
    }
	


}