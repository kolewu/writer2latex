/************************************************************************
 *
 *  XeTeXI18n.java
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
 *  Version 1.2 (2012-02-27)
 * 
 */

package writer2latex.latex.i18n;

import java.text.Bidi;

import writer2latex.office.*;
import writer2latex.latex.LaTeXConfig;
import writer2latex.latex.LaTeXDocumentPortion;
import writer2latex.latex.ConverterPalette;
import writer2latex.latex.util.BeforeAfter;

/** This class takes care of i18n in XeLaTeX
 */
public class XeTeXI18n extends I18n {

    private Polyglossia polyglossia;
    private boolean bUsePolyglossia;
    private boolean bUseXepersian;

    /** Construct a new XeTeXI18n as ConverterHelper
     *  @param ofr the OfficeReader to get language information from
     *  @param config the configuration which determines the symbols to use
     *  @param palette the ConverterPalette (unused)
     */
    public XeTeXI18n(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
    	super(ofr,config,palette);
    	polyglossia = new Polyglossia();
    	polyglossia.applyLanguage(sDefaultLanguage, sDefaultCountry);
    	
    	// Currently all languages except farsi (fa_IR) are handled with polyglossia
    	// Actually only LTR languages are supported as yet
    	// TODO: Support CTL languages using polyglossia
    	bUsePolyglossia = !"fa".equals(sDefaultCTLLanguage);
    	// For farsi, we load xepersian.sty
    	// TODO: Add a use_xepersian option, using polyglossia if false
    	bUseXepersian = !bUsePolyglossia;
    }
	
    /** Add declarations to the preamble to load the required packages
     *  @param pack usepackage declarations
     *  @param decl other declarations
     */
    public void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl) {
    	pack.append("\\usepackage{amsmath,amssymb,amsfonts}").nl()
    		.append("\\usepackage{fontspec}").nl()
    		.append("\\usepackage{xunicode}").nl()
    		.append("\\usepackage{xltxtra}").nl();
    	if (bUsePolyglossia) {
    		String[] polyglossiaDeclarations = polyglossia.getDeclarations();
    		for (String s: polyglossiaDeclarations) {
    			pack.append(s).nl();
    		}
    	}
    	else if (bUseXepersian) {
        	// xepersian.sty must be loaded as the last package
    		// We put it in the declarations part to achieve this
    		decl.append("\\usepackage{xepersian}").nl();
    		// Set the default font to the default CTL font defined in the document
    		StyleWithProperties defaultStyle = ofr.getDefaultParStyle();
    		if (defaultStyle!=null) {
    			String sDefaultCTLFont = defaultStyle.getProperty(XMLString.STYLE_FONT_NAME_COMPLEX);
    			if (sDefaultCTLFont!=null) {
    	    		decl.append("\\settextfont{").append(sDefaultCTLFont).append("}").nl();
    			}
    		}
    	}
    }
    
    /** Apply a language
     *  @param style the OOo style to read attributes from
     *  @param bDecl true if declaration form is required
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    public void applyLanguage(StyleWithProperties style, boolean bDecl, boolean bInherit, BeforeAfter ba) {
        if (bUsePolyglossia && !bAlwaysUseDefaultLang && style!=null) {
        	// TODO: Support CTL and CJK
            String sISOLang = style.getProperty(XMLString.FO_LANGUAGE,bInherit);
            String sISOCountry = style.getProperty(XMLString.FO_COUNTRY, bInherit);
            if (sISOLang!=null) {
            	String[] sCommand = polyglossia.applyLanguage(sISOLang, sISOCountry);
            	if (bDecl) {
            		ba.add(sCommand[1],sCommand[2]);
            	} 
            	else {
            		ba.add(sCommand[0]+"{","}");
            	}
            }
        }
    }

    /** Push a font to the font stack
     *  @param sName the name of the font
     */
    public void pushSpecialTable(String sName) {
    	// TODO
    }
	
    /** Pop a font from the font stack
     */
    public void popSpecialTable() {
    	// TODO
    }
	
    /** Convert a string of characters into LaTeX
     *  @param s the source string
     *  @param bMathMode true if the string should be rendered in math mode
     *  @param sLang the ISO language of the string
     *  @return the LaTeX string
     */
    public String convert(String s, boolean bMathMode, String sLang){
    	StringBuffer buf = new StringBuffer();
    	int nLen = s.length();
        char c;
        if (bMathMode) {
        	// No string replace or writing direction in math mode
        	for (int i=0; i<nLen; i++) {
        		convert(s.charAt(i),buf);
        	}        	
        }
        else if (bUsePolyglossia) {
        	int i = 0;
        	while (i<nLen) {
        		ReplacementTrieNode node = stringReplace.get(s,i,nLen);
        		if (node!=null) {
        			buf.append(node.getLaTeXCode());
        			i += node.getInputLength();
        		}
        		else {
        			c = s.charAt(i++);
        			convert (c,buf);
        		}
        	}
        }
        else if (bUseXepersian) {
        	// TODO: Add support for string replace
			Bidi bidi = new Bidi(s,Bidi.DIRECTION_RIGHT_TO_LEFT);
			int nCurrentLevel = bidi.getBaseLevel();
			int nNestingLevel = 0;
			for (int i=0; i<nLen; i++) {
				int nLevel = bidi.getLevelAt(i);
				if (nLevel>nCurrentLevel) {
					if (nLevel%2==0) { // even is LTR
						buf.append("\\lr{");
					}
					else { // odd is RTL
						buf.append("\\rl{");						
					}
					nCurrentLevel=nLevel;
					nNestingLevel++;
				}
				else if (nLevel<nCurrentLevel) {
					buf.append("}");
					nCurrentLevel=nLevel;
					nNestingLevel--;
				}
				convert(s.charAt(i),buf);
			}
			while (nNestingLevel>0) {
				buf.append("}");
				nNestingLevel--;
			}
		}
        
        return buf.toString();
    }
    
    private void convert(char c, StringBuffer buf) {
		switch (c) {
		case '#' : buf.append("\\#"); break; // Parameter
		case '$' : buf.append("\\$"); break; // Math shift
		case '%' : buf.append("\\%"); break; // Comment
		case '&' : buf.append("\\&"); break; // Alignment tab
		case '\\' : buf.append("\\textbackslash{}"); break; // Escape
		case '^' : buf.append("\\^{}"); break; // Superscript
		case '_' : buf.append("\\_"); break; // Subscript
		case '{' : buf.append("\\{"); break; // Begin group
		case '}' : buf.append("\\}"); break; // End group
		case '~' : buf.append("\\textasciitilde{}"); break; // Active (non-breaking space)
		case '\u00A0' : buf.append('~'); break; // Make non-breaking spaces visible
		default: buf.append(c);
	}
    	
    }
    

	

}
