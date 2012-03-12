/************************************************************************
 *
 *  ConfigurationDialog.java
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
 *  Version 1.2 (2012-03-12)
 *
 */ 
 
package org.openoffice.da.comp.writer2latex;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import writer2latex.api.ComplexOption;
import writer2latex.util.Misc;

import org.openoffice.da.comp.w2lcommon.filter.ConfigurationDialogBase;
import org.openoffice.da.comp.w2lcommon.helper.DialogAccess;
import org.openoffice.da.comp.w2lcommon.helper.FieldMasterNameProvider;
import org.openoffice.da.comp.w2lcommon.helper.StyleNameProvider;

import com.sun.star.lang.XServiceInfo;
import com.sun.star.uno.XComponentContext;

/** This class provides a UNO component which implements the configuration
 *  of Writer2LaTeX. The same component is used for all pages - using the
 *  dialog title to distinguish between the pages.
 */
public final class ConfigurationDialog extends ConfigurationDialogBase implements XServiceInfo {

	/** The component will be registered under this name.
     */
    public static String __serviceName = "org.openoffice.da.writer2latex.ConfigurationDialog";

    /** The component should also have an implementation name.
     */
    public static String __implementationName = "org.openoffice.da.comp.writer2latex.ConfigurationDialog";

    // Implement the interface XServiceInfo
    public boolean supportsService(String sServiceName) {
        return sServiceName.equals(__serviceName);
    }

    public String getImplementationName() {
        return __implementationName;
    }
    
    public String[] getSupportedServiceNames() {
        String[] sSupportedServiceNames = { __serviceName };
        return sSupportedServiceNames;
    }
	
    // Configure the base class
    @Override protected String getMIMEType() { return "application/x-latex"; }
    
    @Override protected String getDialogLibraryName() { return "W2LDialogs2"; }
    
    @Override protected String getConfigFileName() { return "writer2latex.xml"; }
    
    /** Construct a new <code>ConfigurationDialog</code> */
    public ConfigurationDialog(XComponentContext xContext) {
    	super(xContext);
    	
    	pageHandlers.put("Documentclass", new DocumentclassHandler());
    	pageHandlers.put("Headings", new HeadingsHandler());
    	pageHandlers.put("Styles", new StylesHandler());
    	pageHandlers.put("Characters", new CharactersHandler());
    	pageHandlers.put("Fonts", new FontsHandler());
    	pageHandlers.put("Pages", new PagesHandler());
    	pageHandlers.put("Tables", new TablesHandler());
    	pageHandlers.put("Figures", new FiguresHandler());
    	pageHandlers.put("TextAndMath", new TextAndMathHandler());
    }
    
    // Implement remaining method from XContainerWindowEventHandler
    public String[] getSupportedMethodNames() {
        String[] sNames = {
        		"NoPreambleChange", // Documentclass
        		"MaxLevelChange", "WriterLevelChange", "NoIndexChange", // Headings
        		"StyleFamilyChange", "StyleNameChange", "NewStyleClick", "DeleteStyleClick", "AddNextClick",
        			"RemoveNextClick", "LoadDefaultsClick", // Styles
        		"UseSoulChange", "FormattingAttributeChange", "CustomAttributeChange", // Characters
        		"ExportGeometryChange", "ExportHeaderAndFooterChange", // Pages
        		"NoTablesChange", "UseSupertabularChange", "UseLongtableChange", // Tables
        		"NoImagesChange", // Figures
        		"MathSymbolNameChange", "NewSymbolClick", "DeleteSymbolClick",
        		"TextInputChange", "NewTextClick", "DeleteTextClick" // Text and Math
        };
        return sNames;
    }

    // The page "Documentclass"
    // This page handles the options no_preamble, documentclass, global_options and the custom-preamble
    private class DocumentclassHandler extends PageHandler {
    	@Override protected void setControls(DialogAccess dlg) {
        	checkBoxFromConfig(dlg,"NoPreamble","no_preamble");
        	textFieldFromConfig(dlg,"Documentclass","documentclass");
        	textFieldFromConfig(dlg,"GlobalOptions","global_options");
        	textFieldFromConfig(dlg,"CustomPreamble","custom-preamble");
    		noPreambleChange(dlg);
    	}
    	
    	@Override protected void getControls(DialogAccess dlg) {
    		checkBoxToConfig(dlg,"NoPreamble", "no_preamble");
    		textFieldToConfig(dlg,"Documentclass","documentclass");
    		textFieldToConfig(dlg,"GlobalOptions","global_options");
    		textFieldToConfig(dlg,"CustomPreamble","custom-preamble");
    	}
    	
    	@Override protected boolean handleEvent(DialogAccess dlg, String sMethod) {
    		if (sMethod.equals("NoPreambleChange")) {
    			noPreambleChange(dlg);
    			return true;
    		}
    		return false;
    	}

    	private void noPreambleChange(DialogAccess dlg) {
        	boolean bPreamble = !dlg.getCheckBoxStateAsBoolean("NoPreamble");
        	dlg.setControlEnabled("DocumentclassLabel",bPreamble);
        	dlg.setControlEnabled("Documentclass",bPreamble);
        	dlg.setControlEnabled("GlobalOptionsLabel",bPreamble);
        	dlg.setControlEnabled("GlobalOptions",bPreamble);
        	dlg.setControlEnabled("CustomPreambleLabel",bPreamble);
        	dlg.setControlEnabled("CustomPreamble",bPreamble);
    	}    	
    }
    
    // The page "Headings"
    // This page handles the heading map as well as the options no_index, use_titlesec and use_titletoc
    private class HeadingsHandler extends PageHandler {
        ComplexOption headingMap = new ComplexOption(); // Cached heading map
        short nCurrentWriterLevel = -1; // Currently displayed level
        
    	@Override protected void setControls(DialogAccess dlg) {
        	// Load heading map from config
    		headingMap.clear();
    		headingMap.copyAll(config.getComplexOption("heading-map"));
    		nCurrentWriterLevel = -1;
    		
        	// Determine and set the max level (from 0 to 10)
        	short nMaxLevel = 0;
        	while(nMaxLevel<10 && headingMap.containsKey(Integer.toString(nMaxLevel+1))) {
        		nMaxLevel++;
        	}
        	dlg.setListBoxSelectedItem("MaxLevel", nMaxLevel);
        	
        	maxLevelChange(dlg);
        	
        	// Get other controls from config
        	checkBoxFromConfig(dlg,"UseTitlesec","use_titlesec");

        	checkBoxFromConfig(dlg,"NoIndex","no_index");
        	checkBoxFromConfig(dlg,"UseTitletoc","use_titletoc");

    		noIndexChange(dlg);
    	}
    	
    	@Override protected void getControls(DialogAccess dlg) {
        	updateHeadingMap(dlg);
        	
        	// Save heading map to config
        	config.getComplexOption("heading-map").clear();
        	int nMaxLevel = dlg.getListBoxSelectedItem("MaxLevel");
    		for (int i=1; i<=nMaxLevel; i++) {
    			String sLevel = Integer.toString(i);
    			config.getComplexOption("heading-map").copy(sLevel,headingMap.get(sLevel));
    		}

        	// Save other controls to config
    		checkBoxToConfig(dlg,"UseTitlesec","use_titlesec");    	
    		checkBoxToConfig(dlg,"NoIndex","no_index");    	
    		checkBoxToConfig(dlg,"UseTitletoc","use_titletoc");    	
    	}
    	
    	@Override protected boolean handleEvent(DialogAccess dlg, String sMethod) { 
    		if (sMethod.equals("MaxLevelChange")) {
    			maxLevelChange(dlg);
    			return true;
    		}
    		else if (sMethod.equals("WriterLevelChange")) {
    			writerLevelChange(dlg);
    			return true;
    		}
    		else if (sMethod.equals("NoIndexChange")) {
    			noIndexChange(dlg);
    			return true;
    		}
    		return false;
    	}

    	private void maxLevelChange(DialogAccess dlg) {
    		// Remember current writer level and clear it
    		short nPreviousWriterLevel = nCurrentWriterLevel;
    		dlg.setListBoxSelectedItem("WriterLevel", (short) -1);
    		
        	// Adjust the presented writer levels to the max level
        	short nMaxLevel = dlg.getListBoxSelectedItem("MaxLevel");
        	String[] sWriterLevels = new String[nMaxLevel];
        	for (int i=0; i<nMaxLevel; i++) {
        		sWriterLevels[i]=Integer.toString(i+1);
        	}
        	dlg.setListBoxStringItemList("WriterLevel", sWriterLevels);
        	
        	if (nMaxLevel>0) {
        		short nNewWriterLevel;
        		if (nPreviousWriterLevel+1>nMaxLevel) {
                	// If we lower the max level, we may have to change the displayed Writer level
        			nNewWriterLevel = (short)(nMaxLevel-1);
        		}
        		else if (nPreviousWriterLevel>-1){
        			// Otherwise reselect the current level, if any
        			nNewWriterLevel = nPreviousWriterLevel;
        		}
        		else {
        			// Or select the top level
        			nNewWriterLevel = (short) 0;
        		}
        		dlg.setListBoxSelectedItem("WriterLevel", nNewWriterLevel);
        	}
        	
        	writerLevelChange(dlg);

        	// All controls should be disabled if the maximum level is zero
        	boolean bUpdate = dlg.getListBoxSelectedItem("MaxLevel")>0;
        	dlg.setControlEnabled("WriterLevelLabel", bUpdate);
        	dlg.setControlEnabled("WriterLevel", bUpdate);
        	dlg.setControlEnabled("LaTeXLevelLabel", bUpdate);
        	dlg.setControlEnabled("LaTeXLevel", bUpdate);
        	dlg.setControlEnabled("LaTeXNameLabel", bUpdate);
        	dlg.setControlEnabled("LaTeXName", bUpdate);
        	// Until implemented:
        	dlg.setControlEnabled("UseTitlesec", false);
        	//dlg.setControlEnabled("UseTitlesec", bUpdate);
    	}
    	
    	private void writerLevelChange(DialogAccess dlg) {
    		updateHeadingMap(dlg);
    		
        	// Load the values for the new level
    		nCurrentWriterLevel = dlg.getListBoxSelectedItem("WriterLevel");    		
        	if (nCurrentWriterLevel>-1) {
        		String sLevel = Integer.toString(nCurrentWriterLevel+1);
        		if (headingMap.containsKey(sLevel)) {
        			Map<String,String> attr = headingMap.get(sLevel);
        			dlg.setComboBoxText("LaTeXLevel", attr.containsKey("level") ? attr.get("level") : "");
        			dlg.setComboBoxText("LaTeXName", attr.containsKey("name") ? attr.get("name") : "");
        		}
        		else {
        			dlg.setListBoxSelectedItem("LaTeXLevel", (short)2);
        			dlg.setComboBoxText("LaTeXName", "");
        		}
        	}
        	else {
    			dlg.setComboBoxText("LaTeXLevel", "");
    			dlg.setComboBoxText("LaTeXName", "");
        	}
    	}

       	private void noIndexChange(DialogAccess dlg) {
        	// Until implemented:
        	dlg.setControlEnabled("UseTitletoc", false);
        	//boolean bNoIndex = dlg.getCheckBoxStateAsBoolean("NoIndex");
        	//dlg.setControlEnabled("UseTitletoc", !bNoIndex);    		
    	} 
    	
        private void updateHeadingMap(DialogAccess dlg) {
        	// Save the current writer level in our cache
        	if (nCurrentWriterLevel>-1) {
        		Map<String,String> attr = new HashMap<String,String>();
        		attr.put("name", dlg.getComboBoxText("LaTeXName"));
        		attr.put("level", dlg.getComboBoxText("LaTeXLevel"));
        		headingMap.put(Integer.toString(nCurrentWriterLevel+1), attr);
        	}
        }

    }
    
    // The page "Styles"
    // This page handles the various style maps as well as the options other_styles and formatting
	// Limitation: Cannot handle the values "error" and "warning" for other_styles
    private class StylesHandler extends StylesPageHandler {
    	private final String[] sLaTeXFamilyNames = { "text", "paragraph", "paragraph-block", "list", "listitem" };
    	private final String[] sLaTeXOOoFamilyNames = { "CharacterStyles", "ParagraphStyles", "ParagraphStyles", "NumberingStyles", "NumberingStyles" };
    	    	
    	protected StylesHandler() {
    		super(5);
    		sFamilyNames =sLaTeXFamilyNames;
    		sOOoFamilyNames = sLaTeXOOoFamilyNames;
     	}
    	
    	// Override standard PageHandler methods
    	@Override public void setControls(DialogAccess dlg) {
    		super.setControls(dlg);
    		
    		String sOtherStyles = config.getOption("other_styles");
    		if ("accept".equals(sOtherStyles)) {
    			dlg.setListBoxSelectedItem("OtherStyles", (short)1);
    		}
    		else {
    			dlg.setListBoxSelectedItem("OtherStyles", (short)0);
    		}

    		String sFormatting = config.getOption("formatting");
        	if ("ignore_all".equals(sFormatting)) {
        		dlg.setListBoxSelectedItem("Formatting", (short)0);
        	}
        	else if ("ignore_most".equals(sFormatting)) {
        		dlg.setListBoxSelectedItem("Formatting", (short)1);
        	}
        	else if ("convert_most".equals(sFormatting)) {
        		dlg.setListBoxSelectedItem("Formatting", (short)3);
        	}
        	else if ("convert_all".equals(sFormatting)) {
        		dlg.setListBoxSelectedItem("Formatting", (short)4);
        	}
        	else {
        		dlg.setListBoxSelectedItem("Formatting", (short)2);
        	}
    	}
    	
    	@Override public void getControls(DialogAccess dlg) {
    		super.getControls(dlg);
    		
    		switch (dlg.getListBoxSelectedItem("OtherStyles")) {
    		case 0: config.setOption("other_styles", "ignore"); break;
    		case 1: config.setOption("other_styles", "accept");
    		}
        	
        	switch (dlg.getListBoxSelectedItem("Formatting")) {
        	case 0: config.setOption("formatting", "ignore_all"); break;
        	case 1: config.setOption("formatting", "ignore_most"); break;
        	case 2: config.setOption("formatting", "convert_basic"); break;
        	case 3: config.setOption("formatting", "convert_most"); break;
        	case 4: config.setOption("formatting", "convert_all");
        	}
    	}
    	
    	@Override protected boolean handleEvent(DialogAccess dlg, String sMethod) {
    		if (sMethod.equals("AddNextClick")) {
    			addNextClick(dlg);
    			return true;
    		}
    		else if (sMethod.equals("RemoveNextClick")) {
    			removeNextClick(dlg);
    			return true;
    		}
    		return super.handleEvent(dlg, sMethod);
    	}
    	
    	// Define methods required by super
    	protected String getDefaultConfigName() {
    		return "clean.xml";
    	}
		
		protected void setControls(DialogAccess dlg, Map<String,String> attr) {
			// Always set before and after, and ensure they are defined
			if (!attr.containsKey("before")) { attr.put("before", ""); }
			if (!attr.containsKey("after")) { attr.put("after", ""); }
			dlg.setTextFieldText("Before", attr.get("before"));
	    	dlg.setTextFieldText("After", attr.get("after"));
	    	
	    	// Set next for paragraph block only
    		String[] sNextItems;
    		if (nCurrentFamily==2 && attr.containsKey("next") && attr.get("next").length()>0) {
    			sNextItems = attr.get("next").split(";");
    			// Localize known styles
				Map<String,String> displayNames = styleNameProvider.getDisplayNames(sOOoFamilyNames[nCurrentFamily]);
    			int nLen = sNextItems.length;
				for (int i=0; i<nLen; i++) {
					if (displayNames.containsKey(sNextItems[i])) {
						sNextItems[i]=displayNames.get(sNextItems[i]);
					}
				}
    		}
    		else {
    			sNextItems = new String[0];
    		}
    		dlg.setListBoxStringItemList("Next", sNextItems);
    		dlg.setListBoxSelectedItem("Next", (short)Math.min(sNextItems.length-1, 0));
	    	updateRemoveNextButton(dlg);
	    	
	        // Set verbatim for paragraph and character styles only
	    	if (nCurrentFamily<2) {
	    		dlg.setCheckBoxStateAsBoolean("Verbatim", 
	    				attr.containsKey("verbatim") ? "true".equals(attr.get("verbatim")) : false);
	    	}
	    	else {
	    		dlg.setCheckBoxStateAsBoolean("Verbatim", false);
	    	}
	    	
	    	// Set line break for paragraph style only
	    	if (nCurrentFamily==1) {
	    		dlg.setCheckBoxStateAsBoolean("LineBreak",
	    				attr.containsKey("line-break") ? "true".equals(attr.get("line-break")) : false);
	    	}
	    	else {
	    		dlg.setCheckBoxStateAsBoolean("LineBreak", false);
	    	}
		}
		
		protected void getControls(DialogAccess dlg, Map<String,String> attr) {
			// Always get before and after
    		attr.put("before", dlg.getTextFieldText("Before"));
    		attr.put("after", dlg.getTextFieldText("After"));
	    	
	    	// Get next for paragraph block only
    		if (nCurrentFamily==2) {
    			String[] sNextItems = dlg.getListBoxStringItemList("Next");
    			// Internalize known styles
				Map<String,String> internalNames = styleNameProvider.getInternalNames(sOOoFamilyNames[nCurrentFamily]);
    			int nLen = sNextItems.length;
				for (int i=0; i<nLen; i++) {
					if (internalNames.containsKey(sNextItems[i])) {
						sNextItems[i]=internalNames.get(sNextItems[i]);
					}
				}
    			StringBuffer list = new StringBuffer();
    			for (int i=0; i<nLen; i++) {
    				if (i>0) list.append(';');
    				list.append(sNextItems[i]);
    			}
    			attr.put("next", list.toString());
    		}
	    	
	        // Get verbatim for paragraph and character styles only
    		if (nCurrentFamily<2) {
    			attr.put("verbatim", Boolean.toString(dlg.getCheckBoxStateAsBoolean("Verbatim")));
    		}
	    	
	    	// Get line break for paragraph style only
			if (nCurrentFamily==1) {
				attr.put("line-break", Boolean.toString(dlg.getCheckBoxStateAsBoolean("LineBreak")));
	    	}
		}
		
		protected void clearControls(DialogAccess dlg) {
			dlg.setTextFieldText("Before", "");
			dlg.setTextFieldText("After", "");
			dlg.setListBoxStringItemList("Next", new String[0]);
			dlg.setCheckBoxStateAsBoolean("Verbatim", false);
			dlg.setCheckBoxStateAsBoolean("LineBreak", false);
		}
		
		protected void prepareControls(DialogAccess dlg, boolean bHasMappings) {
			dlg.setControlEnabled("BeforeLabel", bHasMappings);
			dlg.setControlEnabled("Before", bHasMappings);
			dlg.setControlEnabled("AfterLabel", bHasMappings);
			dlg.setControlEnabled("After", bHasMappings);
        	dlg.setControlEnabled("NextLabel", bHasMappings && nCurrentFamily==2);
        	dlg.setControlEnabled("Next", bHasMappings && nCurrentFamily==2);
        	dlg.setControlEnabled("AddNextButton", bHasMappings && nCurrentFamily==2);
        	//dlg.setControlEnabled("RemoveNextButton", bHasMappings && nCurrentFamily==2);
        	dlg.setControlEnabled("Verbatim", bHasMappings && nCurrentFamily<2);
        	dlg.setControlEnabled("LineBreak", bHasMappings && nCurrentFamily==1);
        	updateRemoveNextButton(dlg);
		}
		
		// Define own event handlers
		private void addNextClick(DialogAccess dlg) {
			appendItem(dlg, "Next",styleNameProvider.getInternalNames(sOOoFamilyNames[nCurrentFamily]).keySet());
			updateRemoveNextButton(dlg);
		}

		private void removeNextClick(DialogAccess dlg) {
			deleteCurrentItem(dlg, "Next");
			updateRemoveNextButton(dlg);
		}
		
		private void updateRemoveNextButton(DialogAccess dlg) {
			dlg.setControlEnabled("RemoveNextButton", dlg.getListBoxStringItemList("Next").length>0);
		}

    }
    
    // The page "Characters"
    // This page handles the options use_color, use_soul, use_ulem and use_hyperref
    // In addition it handles style maps for formatting attributes
    private class CharactersHandler extends AttributePageHandler {
    	private final String[] sLaTeXAttributeNames = { "bold", "italic", "small-caps", "superscript", "subscript" };
        
        protected CharactersHandler() {
        	super();
        	sAttributeNames = sLaTeXAttributeNames;
        }
        
    	@Override protected void setControls(DialogAccess dlg) {
    		super.setControls(dlg);
    		checkBoxFromConfig(dlg,"UseHyperref","use_hyperref");
    		checkBoxFromConfig(dlg,"UseColor","use_color");
    		checkBoxFromConfig(dlg,"UseSoul","use_soul");
    		checkBoxFromConfig(dlg,"UseUlem","use_ulem");
    	}
    	
    	@Override protected void getControls(DialogAccess dlg) {
    		super.getControls(dlg);
    		checkBoxToConfig(dlg,"UseHyperref","use_hyperref");
    		checkBoxToConfig(dlg,"UseColor","use_color");
    		checkBoxToConfig(dlg,"UseSoul","use_soul");
    		checkBoxToConfig(dlg,"UseUlem","use_ulem");
    	}
    	
		@Override protected void setControls(DialogAccess dlg, Map<String, String> attr) {
    		if (!attr.containsKey("before")) { attr.put("before", ""); }
    		if (!attr.containsKey("after")) { attr.put("after", ""); }
    		dlg.setTextFieldText("Before", attr.get("before"));
    		dlg.setTextFieldText("After", attr.get("after"));			
		}

		@Override protected void getControls(DialogAccess dlg, Map<String, String> attr) {
    		attr.put("before", dlg.getComboBoxText("Before"));
    		attr.put("after", dlg.getComboBoxText("After"));
		}
		
		@Override protected void prepareControls(DialogAccess dlg, boolean bEnable) {
    		dlg.setControlEnabled("BeforeLabel", bEnable);
    		dlg.setControlEnabled("Before", bEnable);
    		dlg.setControlEnabled("AfterLabel", bEnable);
    		dlg.setControlEnabled("After", bEnable);
		}

		@Override protected boolean handleEvent(DialogAccess dlg, String sMethod) {
    		if (sMethod.equals("UseSoulChange")) {
    			useSoulChange(dlg);
    			return true;
    		}
    		else {
    			return super.handleEvent(dlg, sMethod);
    		}
    	}
    	
    	private void useSoulChange(DialogAccess dlg) {
        	// Until implemented...
        	dlg.setControlEnabled("UseSoul", false);
        	// After which it should be...
        	//boolean bUseSoul = dlg.getCheckBoxStateAsBoolean("UseSoul");   	    	
        	//dlg.setControlEnabled("UseUlem", !bUseSoul);
    	}

    }

    // The page "Fonts"
    // This page handles the options use_fontspec, use_pifont, use_tipa, use_eurosym, use_wasysym,
    // use_ifsym, use_bbding
    private class FontsHandler extends PageHandler {
    	@Override protected void setControls(DialogAccess dlg) {
        	checkBoxFromConfig(dlg,"UsePifont","use_pifont");
        	checkBoxFromConfig(dlg,"UseTipa","use_tipa");
        	checkBoxFromConfig(dlg,"UseEurosym","use_eurosym");
        	checkBoxFromConfig(dlg,"UseWasysym","use_wasysym");
        	checkBoxFromConfig(dlg,"UseIfsym","use_ifsym");
        	checkBoxFromConfig(dlg,"UseBbding","use_bbding");
        	checkBoxFromConfig(dlg,"UseFontspec","use_fontspec");
        	// Until implemented:
        	dlg.setControlEnabled("UseFontspec", false);
    	}
    	
    	@Override protected void getControls(DialogAccess dlg) {
        	checkBoxToConfig(dlg,"UsePifont","use_pifont");
        	checkBoxToConfig(dlg,"UseTipa","use_tipa");
        	checkBoxToConfig(dlg,"UseEurosym","use_eurosym");
        	checkBoxToConfig(dlg,"UseWasysym","use_wasysym");
        	checkBoxToConfig(dlg,"UseIfsym","use_ifsym");
        	checkBoxToConfig(dlg,"UseBbding","use_bbding");
        	checkBoxToConfig(dlg,"UseFontspec","use_fontspec");
    	}
    	
    	@Override protected boolean handleEvent(DialogAccess dlg, String sMethod) {
    		// Currently no events
    		return false;
    	}
    }
    
    // The page "Pages"
    // This page handles the options page_formatting, use_geometry, use_fancyhdr, use_lastpage and use_endnotes
    private class PagesHandler extends PageHandler {
    	@Override protected void setControls(DialogAccess dlg) {
    		// The option page_formatting is presented as two options in the user interface
        	String sPageFormatting = config.getOption("page_formatting");
        	if ("ignore_all".equals(sPageFormatting)) {
        		dlg.setCheckBoxStateAsBoolean("ExportGeometry", false);
        		dlg.setCheckBoxStateAsBoolean("ExportHeaderFooter", false);
        	}
        	else if ("convert_geometry".equals(sPageFormatting)) {
        		dlg.setCheckBoxStateAsBoolean("ExportGeometry", true);
        		dlg.setCheckBoxStateAsBoolean("ExportHeaderFooter", false);
        	}
        	else if ("convert_header_footer".equals(sPageFormatting)) {
        		dlg.setCheckBoxStateAsBoolean("ExportGeometry", false);
        		dlg.setCheckBoxStateAsBoolean("ExportHeaderFooter", true);
        	}
        	else if ("convert_all".equals(sPageFormatting)) {
        		dlg.setCheckBoxStateAsBoolean("ExportGeometry", true);
        		dlg.setCheckBoxStateAsBoolean("ExportHeaderFooter", true);
        	}
        	
        	checkBoxFromConfig(dlg,"UseGeometry", "use_geometry");
        	checkBoxFromConfig(dlg,"UseFancyhdr", "use_fancyhdr");
        	checkBoxFromConfig(dlg,"UseLastpage", "use_lastpage");
        	checkBoxFromConfig(dlg,"UseEndnotes", "use_endnotes");

        	// Trigger change events
			exportGeometryChange(dlg);
			exportHeaderAndFooterChange(dlg);
    	}
    	
    	@Override protected void getControls(DialogAccess dlg) {
        	boolean bGeometry = dlg.getCheckBoxStateAsBoolean("ExportGeometry");
        	boolean bHeaderFooter = dlg.getCheckBoxStateAsBoolean("ExportHeaderFooter");
        	if (bGeometry && bHeaderFooter) {
        		config.setOption("page_formatting", "convert_all");
        	}
        	else if (bGeometry && !bHeaderFooter) {
        		config.setOption("page_formatting", "convert_geometry");
        	}
        	else if (!bGeometry && bHeaderFooter) {
        		config.setOption("page_formatting", "convert_header_footer");
        	}
        	else {
        		config.setOption("page_formatting", "ignore_all");
        	}
        	
        	checkBoxToConfig(dlg,"UseGeometry", "use_geometry");
        	checkBoxToConfig(dlg,"UseFancyhdr", "use_fancyhdr");
        	checkBoxToConfig(dlg,"UseLastpage", "use_lastpage");
        	checkBoxToConfig(dlg,"UseEndnotes", "use_endnotes");
    	}
    	
    	@Override protected boolean handleEvent(DialogAccess dlg, String sMethod) {
    		if (sMethod.equals("ExportGeometryChange")) {
    			exportGeometryChange(dlg);
    			return true;
    		}
    		else if (sMethod.equals("ExportHeaderAndFooterChange")) {
    			exportHeaderAndFooterChange(dlg);
    			return true;
    		}
    		return false;
    	}
    	
    	private void exportGeometryChange(DialogAccess dlg) {
        	dlg.setControlEnabled("UseGeometry", dlg.getCheckBoxStateAsBoolean("ExportGeometry"));    		
    	}
    	
    	private void exportHeaderAndFooterChange(DialogAccess dlg) {
        	dlg.setControlEnabled("UseFancyhdr", dlg.getCheckBoxStateAsBoolean("ExportHeaderFooter"));
    	}
    }
    	
    // The page "Tables"
    // This page handles the options table_content, use_tabulary, use_colortbl, use_multirow, use_supertabular, use_longtable,
    // table_first_head_style, table_head_style, table_foot_style, table_last_foot_style
	// Limitation: Cannot handle the values "error" and "warning" for table_content
    private class TablesHandler extends PageHandler {
    	
    	protected TablesHandler() {
    	}
    	
    	@Override protected void setControls(DialogAccess dlg) {
    		// Fill the table style combo boxes with style names
    		StyleNameProvider styleNameProvider = new StyleNameProvider(xContext);
    		Map<String,String> internalNames = styleNameProvider.getInternalNames("ParagraphStyles");
    		if (internalNames!=null) {
    			String[] styleNames = Misc.sortStringSet(internalNames.keySet());
    			dlg.setListBoxStringItemList("TableFirstHeadStyle",styleNames);
    			dlg.setListBoxStringItemList("TableHeadStyle",styleNames);
    			dlg.setListBoxStringItemList("TableFootStyle",styleNames);
    			dlg.setListBoxStringItemList("TableLastFootStyle",styleNames);
    		}
    		
    		// Fill the table sequence combo box with sequence names
    		FieldMasterNameProvider fieldMasterNameProvider = new FieldMasterNameProvider(xContext);
    		dlg.setListBoxStringItemList("TableSequenceName",
    				Misc.sortStringSet(fieldMasterNameProvider.getFieldMasterNames("com.sun.star.text.fieldmaster.SetExpression.")));

    		dlg.setCheckBoxStateAsBoolean("NoTables", !"accept".equals(config.getOption("table_content")));
        	checkBoxFromConfig(dlg,"UseColortbl","use_colortbl");
        	checkBoxFromConfig(dlg,"UseTabulary","use_tabulary");
        	//checkBoxFromConfig(dlg,"UseMultirow","use_multirow");
        	checkBoxFromConfig(dlg,"UseSupertabular","use_supertabular");
        	checkBoxFromConfig(dlg,"UseLongtable","use_longtable");
        	textFieldFromConfig(dlg,"TableFirstHeadStyle","table_first_head_style");
        	textFieldFromConfig(dlg,"TableHeadStyle","table_head_style");
        	textFieldFromConfig(dlg,"TableFootStyle","table_foot_style");
        	textFieldFromConfig(dlg,"TableLastFootStyle","table_last_foot_style");
        	textFieldFromConfig(dlg,"TableSequenceName","table_sequence_name");
        	
        	checkBoxChange(dlg);
    	}
    	
    	@Override protected void getControls(DialogAccess dlg) {
        	config.setOption("table_content", dlg.getCheckBoxStateAsBoolean("NoTables") ? "ignore" : "accept");
        	checkBoxToConfig(dlg,"UseColortbl","use_colortbl");
        	checkBoxToConfig(dlg,"UseTabulary","use_tabulary");
        	//checkBoxToConfig(dlg,"UseMultirow","use_multirow");
        	checkBoxToConfig(dlg,"UseSupertabular","use_supertabular");
        	checkBoxToConfig(dlg,"UseLongtable","use_longtable");
        	textFieldToConfig(dlg,"TableFirstHeadStyle","table_first_head_style");
        	textFieldToConfig(dlg,"TableHeadStyle","table_head_style");
        	textFieldToConfig(dlg,"TableFootStyle","table_foot_style");
        	textFieldToConfig(dlg,"TableLastFootStyle","table_last_foot_style");
        	textFieldToConfig(dlg,"TableSequenceName","table_sequence_name");
    	}
    	
    	@Override protected boolean handleEvent(DialogAccess dlg, String sMethod) {
    		if (sMethod.equals("NoTablesChange")) {
    			checkBoxChange(dlg);
    			return true;
    		}
    		else if (sMethod.equals("UseSupertabularChange")) {
    			checkBoxChange(dlg);
    			return true;
    		}
    		else if (sMethod.equals("UseLongtableChange")) {
    			checkBoxChange(dlg);
    			return true;
    		}
    		return false;
    	}
    	
    	private void checkBoxChange(DialogAccess dlg) {
    		boolean bNoTables = dlg.getCheckBoxStateAsBoolean("NoTables");
    		boolean bSupertabular = dlg.getCheckBoxStateAsBoolean("UseSupertabular");
    		boolean bLongtable = dlg.getCheckBoxStateAsBoolean("UseLongtable");
    		dlg.setControlEnabled("UseColortbl", !bNoTables);
    		dlg.setControlEnabled("UseTabulary", !bNoTables);
    		dlg.setControlEnabled("UseMultirow", false);
    		dlg.setControlEnabled("UseSupertabular", !bNoTables);
    		dlg.setControlEnabled("UseLongtable", !bNoTables && !bSupertabular);
    		dlg.setControlEnabled("TableFirstHeadLabel", !bNoTables && (bSupertabular || bLongtable));
    		dlg.setControlEnabled("TableFirstHeadStyle", !bNoTables && (bSupertabular || bLongtable));
    		dlg.setControlEnabled("TableHeadLabel", !bNoTables && (bSupertabular || bLongtable));
    		dlg.setControlEnabled("TableHeadStyle", !bNoTables && (bSupertabular || bLongtable));
    		dlg.setControlEnabled("TableFootLabel", !bNoTables && (bSupertabular || bLongtable));
    		dlg.setControlEnabled("TableFootStyle", !bNoTables && (bSupertabular || bLongtable));
    		dlg.setControlEnabled("TableLastFootLabel", !bNoTables && (bSupertabular || bLongtable));
    		dlg.setControlEnabled("TableLastFootStyle", !bNoTables && (bSupertabular || bLongtable));
    		dlg.setControlEnabled("TableSequenceLabel", !bNoTables);
    		dlg.setControlEnabled("TableSequenceName", !bNoTables);
    	}    
    	
    }

    // The page "Figures"
    // This page handles the options use_caption, align_frames, figure_sequence_name, image_content,
    // remove_graphics_extension and image_options
	// Limitation: Cannot handle the values "error" and "warning" for image_content
    private class FiguresHandler extends PageHandler {
    	@Override protected void setControls(DialogAccess dlg) {
    		// Fill the figure sequence combo box with sequence names
    		FieldMasterNameProvider fieldMasterNameProvider = new FieldMasterNameProvider(xContext);
    		dlg.setListBoxStringItemList("FigureSequenceName",
    				Misc.sortStringSet(fieldMasterNameProvider.getFieldMasterNames("com.sun.star.text.fieldmaster.SetExpression.")));
    		
        	checkBoxFromConfig(dlg,"UseCaption","use_caption");
        	checkBoxFromConfig(dlg,"AlignFrames","align_frames");
        	textFieldFromConfig(dlg,"FigureSequenceName","figure_sequence_name");
        	dlg.setCheckBoxStateAsBoolean("NoImages", !"accept".equals(config.getOption("image_content")));
        	checkBoxFromConfig(dlg,"RemoveGraphicsExtension","remove_graphics_extension");
        	textFieldFromConfig(dlg,"ImageOptions","image_options");
        	
        	noImagesChange(dlg);
    	}
    	
    	@Override protected void getControls(DialogAccess dlg) {
        	checkBoxToConfig(dlg,"UseCaption","use_caption");
        	checkBoxToConfig(dlg,"AlignFrames","align_frames");
        	textFieldToConfig(dlg,"FigureSequenceName","figure_sequence_name");
        	config.setOption("image_content", dlg.getCheckBoxStateAsBoolean("NoImages") ? "ignore" : "accept");
        	checkBoxToConfig(dlg,"RemoveGraphicsExtension","remove_graphics_extension");
        	textFieldToConfig(dlg,"ImageOptions","image_options");
    	}
    	
    	@Override protected boolean handleEvent(DialogAccess dlg, String sMethod) {
    		if (sMethod.equals("NoImagesChange")) {
    			noImagesChange(dlg);
    			return true;
    		}
    		return false;
    	}
    	
    	private void noImagesChange(DialogAccess dlg) {
        	boolean bNoImages = dlg.getCheckBoxStateAsBoolean("NoImages");
        	dlg.setControlEnabled("RemoveGraphicsExtension", !bNoImages);
        	dlg.setControlEnabled("ImageOptionsLabel", !bNoImages);
        	dlg.setControlEnabled("ImageOptions", !bNoImages);
    	}    
    	
    }
        
    // The page "TextAndMath"
    // This page handles the options use_ooomath and tabstop as well as the 
    // text replacements and math symbol definitions
    private class TextAndMathHandler extends UserListPageHandler {
        private CustomSymbolNameProvider customSymbolNameProvider = null;
        private ComplexOption mathSymbols;
        private ComplexOption stringReplace;
        private String sCurrentMathSymbol = null;
        private String sCurrentText = null;
        
        protected TextAndMathHandler() {
        	super();
        	customSymbolNameProvider = new CustomSymbolNameProvider(xContext);
        }

    	@Override protected void setControls(DialogAccess dlg) {
    		// Get math symbols from config
    		if (mathSymbols!=null) { mathSymbols.clear(); }
    		else { mathSymbols = new ComplexOption(); }
    		mathSymbols.copyAll(config.getComplexOption("math-symbol-map"));
    		sCurrentMathSymbol = null;
        	dlg.setListBoxStringItemList("MathSymbolName", Misc.sortStringSet(mathSymbols.keySet()));
        	dlg.setListBoxSelectedItem("MathSymbolName", (short)Math.min(0,mathSymbols.keySet().size()-1));
        	// Trigger change event (on some versions of OOo this is automatic due to a bug)
        	mathSymbolNameChange(dlg);

        	// Get string replace from config
        	if (stringReplace!=null) { stringReplace.clear(); }
    		else { stringReplace = new ComplexOption(); }
    		stringReplace.copyAll(config.getComplexOption("string-replace"));
    		sCurrentText = null;
        	dlg.setListBoxStringItemList("TextInput", Misc.sortStringSet(stringReplace.keySet()));
        	dlg.setListBoxSelectedItem("TextInput", (short)Math.min(0,stringReplace.keySet().size()-1));
        	// Trigger change event (on some versions of OOo this is automatic due to a bug)
        	textInputChange(dlg);
        	    	
        	// Get other options from config
        	checkBoxFromConfig(dlg,"UseOoomath","use_ooomath");
        	textFieldFromConfig(dlg,"TabStopLaTeX", "tabstop");
    	}
    	
    	@Override protected void getControls(DialogAccess dlg) {        	
        	// Save math symbols to config
        	updateSymbol(dlg);
    		config.getComplexOption("math-symbol-map").clear();
    		config.getComplexOption("math-symbol-map").copyAll(mathSymbols);

    		// Save string replace to config
        	updateText(dlg);
    		config.getComplexOption("string-replace").clear();
    		config.getComplexOption("string-replace").copyAll(stringReplace);
        	
    		// Save other options to config
        	checkBoxToConfig(dlg,"UseOoomath","use_ooomath");
        	textFieldToConfig(dlg,"TabStopLaTeX", "tabstop");
    	}
    	
    	@Override protected boolean handleEvent(DialogAccess dlg, String sMethod) {
    		if (sMethod.equals("MathSymbolNameChange")) {
    			mathSymbolNameChange(dlg);
    			return true;
    		}
    		else if (sMethod.equals("NewSymbolClick")) {
    			newSymbolClick(dlg);
    			return true;
    		}
    		else if (sMethod.equals("DeleteSymbolClick")) {
    			deleteSymbolClick(dlg);
    			return true;
    		}
    		else if (sMethod.equals("TextInputChange")) {
    			textInputChange(dlg);
    			return true;
    		}
    		else if (sMethod.equals("NewTextClick")) {
    			newTextClick(dlg);
    			return true;
    		}
    		else if (sMethod.equals("DeleteTextClick")) {
    			deleteTextClick(dlg);
    			return true;
    		}
    		return false;
    	}
    	
    	private void mathSymbolNameChange(DialogAccess dlg) {
    		updateSymbol(dlg);
    		updateSymbolControls(dlg);
    	}
    	
    	private void newSymbolClick(DialogAccess dlg) {
        	appendItem(dlg,"MathSymbolName",customSymbolNameProvider.getNames());
        	// Trigger change event (on some versions of OOo this is automatic due to a bug)
        	mathSymbolNameChange(dlg);
    	}
    	
    	private void deleteSymbolClick(DialogAccess dlg) {
    		String sMathSymbol = sCurrentMathSymbol; 
        	if (deleteCurrentItem(dlg,"MathSymbolName")) {
        		mathSymbols.remove(sMathSymbol);
        		sCurrentMathSymbol=null; // invalidate current symbol
        	}    		
        	// Trigger change event (on some versions of OOo this is automatic due to a bug)
        	mathSymbolNameChange(dlg);
    	}
    	
    	private void updateSymbol(DialogAccess dlg) {
        	// Save the current math symbol in our cache
        	if (sCurrentMathSymbol!=null) {
        		Map<String,String> attr = new HashMap<String,String>();
        		attr.put("latex", dlg.getTextFieldText("MathLaTeX"));
        		mathSymbols.put(sCurrentMathSymbol, attr);
        	}
    	}
    	
    	// Update symbol controls based on currently selected list item
    	private void updateSymbolControls(DialogAccess dlg) {
        	short nSymbolItem = dlg.getListBoxSelectedItem("MathSymbolName");
        	if (nSymbolItem>=0) {
        		sCurrentMathSymbol = dlg.getListBoxStringItemList("MathSymbolName")[nSymbolItem];
        		Map<String,String> attributes;
        		if (mathSymbols.containsKey(sCurrentMathSymbol)) {
        			attributes = mathSymbols.get(sCurrentMathSymbol);
        		}
        		else { // New symbol, add empty definition to cache
        			attributes = new HashMap<String,String>();
        			attributes.put("latex", "");
        			mathSymbols.put(sCurrentMathSymbol, attributes);
        		}
        		dlg.setTextFieldText("MathLaTeX", attributes.get("latex"));
        		dlg.setControlEnabled("MathLaTeX", true);
        		dlg.setControlEnabled("DeleteSymbolButton", true);
        	}
        	else { // The list is empty, or nothing is selected
        		sCurrentMathSymbol = null;
        		dlg.setTextFieldText("MathLaTeX", "");
        		dlg.setControlEnabled("MathLaTeX", false);
        		dlg.setControlEnabled("DeleteSymbolButton", false);
        	}
    	}
    	
    	private void textInputChange(DialogAccess dlg) {
    		updateText(dlg);
    		updateTextControls(dlg);	
    	}
    	
    	private void newTextClick(DialogAccess dlg) {
        	appendItem(dlg, "TextInput", new HashSet<String>());
        	// Trigger change event (on some versions of OOo this is automatic due to a bug)
        	textInputChange(dlg);
    	}
    	
    	private void deleteTextClick(DialogAccess dlg) {
    		String sText = sCurrentText;
    		if (deleteCurrentItem(dlg, "TextInput")) {
        		stringReplace.remove(sText);
        		sCurrentText = null; // Invalidate current string replace
        	}
        	// Trigger change event (on some versions of OOo this is automatic due to a bug)
    		textInputChange(dlg);
    	}
    	
    	private void updateText(DialogAccess dlg) {
        	// Save the current string replace in our cache
        	if (sCurrentText!=null) {
        		Map<String,String> attr = new HashMap<String,String>();
        		attr.put("latex-code", dlg.getTextFieldText("LaTeX"));
        		attr.put("fontenc", "any");
        		stringReplace.put(sCurrentText, attr);
        	} 		
    	}

    	// Update text controls based on currently selected list item
    	private void updateTextControls(DialogAccess dlg) {
        	// Get the current input string, if any
        	short nItem = dlg.getListBoxSelectedItem("TextInput");
        	if (nItem>=0) {
        		sCurrentText = dlg.getListBoxStringItemList("TextInput")[nItem];
        		
        		Map<String,String> attributes;
        		if (stringReplace.containsKey(sCurrentText)) {
        			attributes = stringReplace.get(sCurrentText);
        		}
        		else { // New string replace, add empty definition to cache
        			attributes = new HashMap<String,String>();
        			attributes.put("latex-code", "");
        			attributes.put("fontenc", "any");
        			stringReplace.put(sCurrentText, attributes);
        		}

        		dlg.setTextFieldText("LaTeX", attributes.get("latex-code"));
        		//dlg.setTextFieldText("Fontenc", attributes.get("fontenc"));
        		dlg.setControlEnabled("LaTeX", true);
        		dlg.setControlEnabled("DeleteTextButton",
        				!"\u00A0!".equals(sCurrentText) && !"\u00A0?".equals(sCurrentText) && 
        				!"\u00A0:".equals(sCurrentText) && !"\u00A0;".equals(sCurrentText) &&
        				!"\u00A0\u2014".equals(sCurrentText));
        	}
        	else { // The list is empty, or nothing is selected
        		sCurrentText = null;
        		dlg.setTextFieldText("LaTeX", "");
        		//dlg.setTextFieldText("Fontenc", "any");
        		dlg.setControlEnabled("DeleteTextButton", false);
        	}
    	}
    }
    
}
