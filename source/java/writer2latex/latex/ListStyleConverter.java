/************************************************************************
 *
 *  ListStyleConverter.java
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
 *  Version 1.2 (2012-03-05)
 *
 */

package writer2latex.latex;

import java.util.Hashtable;

import writer2latex.util.*;
import writer2latex.office.*;
import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;

/* This class creates LaTeX code from OOo list styles
 */
public class ListStyleConverter extends StyleConverter {
	boolean bNeedSaveEnumCounter = false;
	private Hashtable<String, String[]> listStyleLevelNames = new Hashtable<String, String[]>();

	/** <p>Constructs a new <code>ListStyleConverter</code>.</p>
	 */
	public ListStyleConverter(OfficeReader ofr, LaTeXConfig config,
			ConverterPalette palette) {
		super(ofr,config,palette);
	}

	public void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl) {
		if (config.formatting()>=LaTeXConfig.CONVERT_MOST || !styleNames.isEmpty()) {
			decl.append("% List styles").nl();
			// May need an extra counter to handle continued numbering in lists
			if (bNeedSaveEnumCounter) {
				decl.append("\\newcounter{saveenum}").nl();
			}
			// If we export formatting, we need some hooks from lists to paragraphs:
			if (config.formatting()>=LaTeXConfig.CONVERT_MOST) {
				decl.append("\\newcommand\\writerlistleftskip{}").nl()
				.append("\\newcommand\\writerlistparindent{}").nl()
				.append("\\newcommand\\writerlistlabel{}").nl()
				.append("\\newcommand\\writerlistremovelabel{")
				.append("\\aftergroup\\let\\aftergroup\\writerlistparindent\\aftergroup\\relax")
				.append("\\aftergroup\\let\\aftergroup\\writerlistlabel\\aftergroup\\relax}").nl();
			}
			super.appendDeclarations(pack,decl);
		}
	}

	/** <p>Apply a list style to an ordered or unordered list.</p> */
	public void applyListStyle(boolean bOrdered, BeforeAfter ba, Context oc) {
		// Step 1. We may have a style map, this always takes precedence
		String sDisplayName = ofr.getListStyles().getDisplayName(oc.getListStyleName());
		if (config.getListStyleMap().contains(sDisplayName)) {
			ba.add(config.getListStyleMap().getBefore(sDisplayName),
					config.getListStyleMap().getAfter(sDisplayName)); 
			return;
		}
		// Step 2: The list style may not exist, or the user wants to ignore it.
		// In this case we create default lists
		ListStyle style = ofr.getListStyle(oc.getListStyleName());
		if (style==null || config.formatting()<=LaTeXConfig.IGNORE_MOST) {
			if (oc.getListLevel()<=4) {
				if (bOrdered) {
					ba.add("\\begin{enumerate}","\\end{enumerate}");
				}
				else {
					ba.add("\\begin{itemize}","\\end{itemize}");
				}
			}
			return;
		}
		// Step 3: Export as default lists, but redefine labels
		// (for list in tables this is the maximum formatting we export)
		if (config.formatting()==LaTeXConfig.CONVERT_BASIC ||
				(config.formatting()>=LaTeXConfig.CONVERT_MOST && oc.isInTable())) {
			if (oc.getListLevel()==1) {
				if (!styleNames.containsName(getDisplayName(oc.getListStyleName()))) {
					createListStyleLabels(oc.getListStyleName());
				}
				ba.add("\\liststyle"+styleNames.getExportName(getDisplayName(oc.getListStyleName()))+"\n","");
			}
			if (oc.getListLevel()<=4) {
				String sCounterName = listStyleLevelNames.get(oc.getListStyleName())[oc.getListLevel()]; 
				if (oc.isInContinuedList() && style.isNumber(oc.getListLevel())) {
					bNeedSaveEnumCounter = true;
					ba.add("\\setcounter{saveenum}{\\value{"+sCounterName+"}}\n","");
				}
				if (bOrdered) {
					ba.add("\\begin{enumerate}","\\end{enumerate}");
				}
				else {
					ba.add("\\begin{itemize}","\\end{itemize}");
				}
				if (oc.isInContinuedList() && style.isNumber(oc.getListLevel())) {
					ba.add("\n\\setcounter{"+sCounterName+"}{\\value{saveenum}}","");
				}
			}
			return;			
		}
		// Step 4: Export with formatting, as "Writer style" custom lists
		if (oc.getListLevel()<=4) { // TODO: Max level should not be fixed
			if (!styleNames.containsName(getDisplayName(oc.getListStyleName()))) {
				createListStyle(oc.getListStyleName());
			}
			String sTeXName="list"+styleNames.getExportName(getDisplayName(oc.getListStyleName()))
			+"level"+Misc.int2roman(oc.getListLevel());
			if (!oc.isInContinuedList() && style.isNumber(oc.getListLevel())) {
				int nStartValue = Misc.getPosInteger(style.getLevelProperty(oc.getListLevel(),XMLString.TEXT_START_VALUE),1)-1;
				// Note that we need a blank line after certain constructions to get proper indentation
				ba.add("\n\\setcounter{"+sTeXName+"}{"+Integer.toString(nStartValue)+"}\n","");
			}
			ba.add("\\begin{"+sTeXName+"}","\\end{"+sTeXName+"}");
		}
	}

	/** <p>Apply a list style to a list item.</p> */
	public void applyListItemStyle(String sStyleName, int nLevel, boolean bHeader,
			boolean bRestart, int nStartValue, BeforeAfter ba, Context oc) {
		// Step 1. We may have a style map, this always takes precedence
		String sDisplayName = ofr.getListStyles().getDisplayName(sStyleName);
		if (config.getListItemStyleMap().contains(sDisplayName)) {
			ba.add(config.getListItemStyleMap().getBefore(sDisplayName),
					config.getListItemStyleMap().getAfter(sDisplayName)); 
			return;
		}
		// Step 2: The list style may not exist, or the user wants to ignore it.
		// In this case we create default lists
		ListStyle style = ofr.getListStyle(sStyleName);
		if (style==null || config.formatting()<=LaTeXConfig.IGNORE_MOST) {
			if (nLevel<=4) {
				if (bHeader) { ba.add("\\item[] ",""); }
				else { ba.add("\\item ",""); }
			}
			return;
		}
		// Step 3: Export as default lists (with redefined labels)
		// (for list in tables this is the maximum formatting we export)
		if (config.formatting()==LaTeXConfig.CONVERT_BASIC ||
				(config.formatting()>=LaTeXConfig.CONVERT_MOST && oc.isInTable())) {
			if (nLevel<=4) {
				if (bHeader) { 
					ba.add("\\item[] ","");
				}
				else if (bRestart && style.isNumber(nLevel)) {
					ba.add("\n\\setcounter{enum"+Misc.int2roman(nLevel)
							+"}{"+(nStartValue-1)+"}\n\\item ","");
				}
				else {
					ba.add("\\item ","");
				}
			}
			return;			
		}
		// Step 4: Export with formatting, as "Writer style" custom lists
		if (nLevel<=4 && !bHeader) { // TODO: Max level should not be fixed
			String sTeXName="list"+styleNames.getExportName(getDisplayName(sStyleName))
			+"level"+Misc.int2roman(nLevel);
			if (bRestart && style.isNumber(nLevel)) {
				ba.add("\\setcounter{"+sTeXName+"}{"+(nStartValue-1)+"}\n","");
			}
			ba.add("\\item ","");
		}
	}


	/** <p>Create labels for default lists (enumerate/itemize) based on
	 *  a List Style
	 */
	private void createListStyleLabels(String sStyleName) {
		String sTeXName = styleNames.getExportName(getDisplayName(sStyleName));
		declarations.append("\\newcommand\\liststyle")
		.append(sTeXName).append("{%").nl();
		ListStyle style = ofr.getListStyle(sStyleName);
		int nEnum = 0;
		int nItem = 0;
		String sName[] = new String[5];
		for (int i=1; i<=4; i++) {
			if (style.isNumber(i)) { sName[i]="enum"+Misc.int2roman(++nEnum); }
			else { sName[i]="item"+Misc.int2roman(++nItem); }
		}
		listStyleLevelNames.put(sStyleName, sName);
		createLabels(style, sName, 4, false, true, false, declarations);
		declarations.append("}").nl();
	}

	/** <p>Create "Writer style" lists based on a List Style.
        <p>A list in writer is really a sequence of numbered paragraphs, so
           this is also how we implement it in LaTeX.
           The enivronment + redefined \item defines three hooks:
           \writerlistleftskip, \writerlistparindent, \writerlistlabel
           which are used by exported paragraph styles to apply numbering.
	 */
	private void createListStyle(String sStyleName) {
		ListStyle style = ofr.getListStyle(sStyleName);

		// Create labels
		String sTeXName = styleNames.getExportName(getDisplayName(sStyleName));
		String[] sLevelName = new String[5];
		for (int i=1; i<=4; i++) {
			sLevelName[i]="list"+sTeXName+"level"+Misc.int2roman(i);
		}
		createLabels(style,sLevelName,4,true,false,true,declarations);

		// Create environments
		for (int i=1; i<=4; i++) {
			// The alignment of the label works the same for old and new format
			String sTextAlign = style.getLevelStyleProperty(i,XMLString.FO_TEXT_ALIGN);
			String sAlignmentChar = "l"; // start (or left) is default
			if (sTextAlign!=null) {
				if ("end".equals(sTextAlign)) { sAlignmentChar="r"; }
				else if ("right".equals(sTextAlign)) { sAlignmentChar="r"; }
				else if ("center".equals(sTextAlign)) { sAlignmentChar="c"; }
			}

			if (style.isNewType(i)) {
				// The new type from ODT 1.2 is somewhat weird; we take it step by step
				
				// Fist the list style defines a left margin (leftskip) and a first line indent (parindent)
				// to *replace* the values from the paragraph style
				String sMarginLeft = style.getLevelStyleProperty(i, XMLString.FO_MARGIN_LEFT);
				if (sMarginLeft==null) { sMarginLeft = "0cm"; }
				String sTextIndent = style.getLevelStyleProperty(i, XMLString.FO_TEXT_INDENT);
				if (sTextIndent==null) { sTextIndent = "0cm"; }
				
				// Generate the LaTeX code to replace these values
				String sDefWriterlistleftskip = "\\def\\writerlistleftskip{\\setlength\\leftskip{"+sMarginLeft+"}}";
				String sDefWriterlistparindent = "\\def\\writerlistparindent{\\setlength\\parindent{"+sTextIndent+"}}";
				
				// Next we have three types of label format: listtab, space, nothing
				String sFormat = style.getLevelStyleProperty(i, XMLString.TEXT_LABEL_FOLLOWED_BY);
				
				// Generate LaTeX code to typeset the label, followed by a space character if required
				String sTheLabel = "\\label"+sLevelName[i]+("space".equals(sFormat) ? "\\ " : ""); 
				
				if ("listtab".equals(sFormat) || sAlignmentChar=="r") {
					// In these cases we typeset the label aligned at a zero width box (rather than as an integrated part of the text)
					sTheLabel = "\\makebox[0cm][" + sAlignmentChar + "]{"+sTheLabel+"}";
				
					if ("listtab".equals(sFormat)) {
						// In the tab case we must the calculate the hspace to put *after* the zero width box
						// This defines the position of an additional tab stop, which really means the start position of the text *after* the label
						String sTabPos = style.getLevelStyleProperty(i, XMLString.TEXT_LIST_TAB_STOP_POSITION);
						if (sTabPos==null) { sTabPos = "0cm"; }
						sTheLabel += "\\hspace{"+Misc.sub(sTabPos, Misc.add(sMarginLeft, sTextIndent))+"}";
					}
				}

				// We are now ready to declare the list style
				declarations.append("\\newenvironment{").append(sLevelName[i]).append("}{")
				// Initialize hooks
				.append(sDefWriterlistleftskip)
				.append("\\def\\writerlistparindent{}")
				.append("\\def\\writerlistlabel{}")
				// Redefine \item
				.append("\\def\\item{")
				// The new parindent is the position of the label
				.append(sDefWriterlistparindent)
				.append("\\def\\writerlistlabel{");
				if (style.isNumber(i)) {
					declarations.append("\\stepcounter{").append(sLevelName[i]).append("}");
				}
				declarations.append(sTheLabel).append("\\writerlistremovelabel}}}{}").nl();
			}
			else {
				String sSpaceBefore = getLength(style,i,XMLString.TEXT_SPACE_BEFORE);
				String sLabelWidth = getLength(style,i,XMLString.TEXT_MIN_LABEL_WIDTH);
				String sLabelDistance = getLength(style,i,XMLString.TEXT_MIN_LABEL_DISTANCE);
				declarations
				.append("\\newenvironment{")
				.append(sLevelName[i]).append("}{")
				.append("\\def\\writerlistleftskip{\\addtolength\\leftskip{")
				.append(Misc.add(sSpaceBefore,sLabelWidth)).append("}}")
				.append("\\def\\writerlistparindent{}")
				.append("\\def\\writerlistlabel{}");
				// Redefine \item
				declarations
				.append("\\def\\item{")
				.append("\\def\\writerlistparindent{\\setlength\\parindent{")
				.append("-").append(sLabelWidth).append("}}")
				.append("\\def\\writerlistlabel{");
				if (style.isNumber(i)) {
					declarations.append("\\stepcounter{")
					.append(sLevelName[i]).append("}");
				}
				declarations
				.append("\\makebox[").append(sLabelWidth).append("][")
				.append(sAlignmentChar).append("]{")
				.append("\\label").append(sLevelName[i]).append("}")
				.append("\\hspace{").append(sLabelDistance).append("}")
				.append("\\writerlistremovelabel}}}{}").nl();
			}
		}
	}

	/** <p>Create LaTeX list labels from an OOo list style. Examples:</p>
	 *  <p>Bullets:</p>
	 *  <pre>\newcommand\labelliststylei{\textbullet}
	 *  \newcommand\labelliststyleii{*}
	 *  \newcommand\labelliststyleiii{\textstylebullet{>}}</pre>
	 *  <p>Numbering:</p>
	 *  <pre>\newcounter{liststylei}
	 *  \newcounter{liststyleii}[liststylei]
	 *  \newcounter{liststyleiii}[liststyleii]
	 *  \renewcommand\theliststylei{\Roman{liststylei}}
	 *  \renewcommand\theliststyleii{\Roman{liststylei}.\arabic{liststyleii}}
	 *  \renewcommand\theliststyleiii{\alph{liststyleiii}}
	 *  \newcommand\labelliststylei{\textstylelabel{\theliststylei .}}
	 *  \newcommand\labelliststyleii{\textstylelabel{\theliststyleii .}}
	 *  \newcommand\labelliststyleiii{\textstylelabel{\theliststyleiii )}}</pre>
	 *
	 *  @param <code>style</code> the OOo list style to use
	 *  @param <code>sName</code> an array of label basenames to use
	 *  @param <code>nMaxLevel</code> the highest level in this numbering
	 *  @param <code>bDeclareCounters</code> true if counters should be declared (they may
	 *  exist already, eg. "section", "subsection"... or "enumi", "enumii"... 
	 *  @param <code>bRenewLabels</code> true if labels should be defined with \renewcommand
	 *  @param <code>bUseTextStyle</code> true if labels should be formatted with the associated text style
	 *  (rather than \newcommand).
	 *  @param <code>ldp</code> the <code>LaTeXDocumentPortion</code> to add LaTeX code to.
	 */
	private void createLabels(ListStyle style, String[] sName, int nMaxLevel,
			boolean bDeclareCounters, boolean bRenewLabels,
			boolean bUseTextStyle, LaTeXDocumentPortion ldp) {
		// Declare counters if required (eg. "\newcounter{countername1}[countername2]")
		if (bDeclareCounters) {
			int j = 0;
			for (int i=1; i<=nMaxLevel; i++) {
				if (style.isNumber(i)) {
					ldp.append("\\newcounter{").append(sName[i]).append("}");
					if (j>0) { ldp.append("[").append(sName[j]).append("]"); }
					ldp.nl();
					j = i;
				}
			}
		}
		// Create numbering for each level (eg. "\arabic{countername}")
		String[] sNumFormat = new String[nMaxLevel+1];
		for (int i=1; i<=nMaxLevel; i++) {
			String s = numFormat(style.getLevelProperty(i,XMLString.STYLE_NUM_FORMAT));
			if (s==null) { sNumFormat[i]=""; }
			else { sNumFormat[i] = s + "{" + sName[i] + "}"; }
		}
		// Create numberings (ie. define "\thecountername"):
		for (int i=1; i<=nMaxLevel; i++) {
			if (style.isNumber(i)) {
				ldp.append("\\renewcommand\\the").append(sName[i]).append("{");
				int nLevels = Misc.getPosInteger(style.getLevelProperty(i,XMLString.TEXT_DISPLAY_LEVELS),1);
				for (int j=i-nLevels+1; j<i; j++) {
					if (style.isNumber(j)) {
						ldp.append(sNumFormat[j]).append(".");
					}
				} 
				ldp.append(sNumFormat[i]);
				ldp.append("}").nl();
			}
		}
		// Create labels (ie. define "\labelcountername"):
		for (int i=1; i<=nMaxLevel; i++) {
			ldp.append(bRenewLabels ? "\\renewcommand" : "\\newcommand")
			.append("\\label").append(sName[i]).append("{");
			// Apply text style if required
			BeforeAfter baText = new BeforeAfter();
			if (bUseTextStyle) {
				String sStyleName = style.getLevelProperty(i,XMLString.TEXT_STYLE_NAME);
				palette.getCharSc().applyTextStyle(sStyleName,baText,new Context());
			}

			// Create label content
			if (style.isNumber(i)) {
				String sPrefix = style.getLevelProperty(i,XMLString.STYLE_NUM_PREFIX);
				String sSuffix = style.getLevelProperty(i,XMLString.STYLE_NUM_SUFFIX);
				// Apply style
				ldp.append(baText.getBefore());
				if (sPrefix!=null) { ldp.append(palette.getI18n().convert(sPrefix,false,"en")); }
				ldp.append("\\the").append(sName[i]);
				if (sSuffix!=null) { ldp.append(palette.getI18n().convert(sSuffix,false,"en")); }
				ldp.append(baText.getAfter());
			}
			else if (style.isBullet(i)) {
				String sBullet = style.getLevelProperty(i,XMLString.TEXT_BULLET_CHAR);
				// Apply style
				ldp.append(baText.getBefore());
				if (sBullet!=null) {
					String sFontName = palette.getCharSc().getFontName(style.getLevelProperty(i,XMLString.TEXT_STYLE_NAME));
					palette.getI18n().pushSpecialTable(sFontName);
					// Bullets are usually symbols, so this should be OK:
					ldp.append(palette.getI18n().convert(sBullet,false,"en"));
					palette.getI18n().popSpecialTable();
				}
				ldp.append(baText.getAfter());
			}
			else {
				// TODO: Support images!
				ldp.append("\\textbullet");
			}

			ldp.append("}").nl();
		}
	}

	/* Helper: Get a length property that defaults to 0cm. */
	private String getLength(ListStyle style,int nLevel,String sProperty) {
		String s = style.getLevelStyleProperty(nLevel,sProperty);
		if (s==null) { return "0cm"; }
		else { return s; }
	}	

	/* Helper: Get display name, or original name if it doesn't exist */
	private String getDisplayName(String sName) {
		String sDisplayName = ofr.getListStyles().getDisplayName(sName);
		return sDisplayName!=null ? sDisplayName : sName;
	}

	/* Helper: Convert OOo number format to LaTeX number format */
	public static final String numFormat(String sFormat){
		if ("1".equals(sFormat)) { return "\\arabic"; }
		else if ("i".equals(sFormat)) { return "\\roman"; }
		else if ("I".equals(sFormat)) { return "\\Roman"; }
		else if ("a".equals(sFormat)) { return "\\alph"; }
		else if ("A".equals(sFormat)) { return "\\Alph"; }
		else { return null; }
	}

}
