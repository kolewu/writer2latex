/************************************************************************
 *
 *  EpubOptionsDialog.java
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
 *  Copyright: 2002-2011 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.2 (2011-03-04)
 *
 */

package org.openoffice.da.comp.writer2xhtml;

import java.awt.GraphicsEnvironment;

import com.sun.star.awt.XDialog;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.XComponent;
import com.sun.star.ui.dialogs.XExecutableDialog;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import org.openoffice.da.comp.w2lcommon.helper.PropertyHelper;
import org.openoffice.da.comp.w2lcommon.filter.OptionsDialogBase;

/** This class provides a UNO component which implements a filter UI for the
 *  EPUB export
 */
public class EpubOptionsDialog extends OptionsDialogBase {
    
    /** The component will be registered under this name.
     */
    public static String __serviceName = "org.openoffice.da.writer2xhtml.EpubOptionsDialog";

    /** The component should also have an implementation name.
     */
    public static String __implementationName = "org.openoffice.da.comp.writer2xhtml.EpubOptionsDialog";
	
    @Override public String getDialogLibraryName() { return "W2XDialogs2"; }
	
    /** Return the name of the dialog within the library
     */
    @Override public String getDialogName() { return "EpubOptions"; }

    /** Return the name of the registry path
     */
    @Override public String getRegistryPath() {
        return "/org.openoffice.da.Writer2xhtml.Options/EpubOptions";
    }
	
    /** Create a new EpubOptionsDialog */
    public EpubOptionsDialog(XComponentContext xContext) {
        super(xContext);
        xMSF = W2XRegistration.xMultiServiceFactory;
    }

    /** Load settings from the registry to the dialog */
    @Override protected void loadSettings(XPropertySet xProps) {
        // Style
        loadConfig(xProps);
        loadNumericOption(xProps, "Scaling");
        loadNumericOption(xProps, "ColumnScaling");
        loadCheckBoxOption(xProps, "RelativeFontSize");
        loadNumericOption(xProps, "FontScaling");
        loadCheckBoxOption(xProps, "RelativeFontSize");
        loadCheckBoxOption(xProps, "UseDefaultFont");
        loadComboBoxOption(xProps, "DefaultFontName");
        loadCheckBoxOption(xProps, "ConvertToPx");
        loadCheckBoxOption(xProps, "OriginalImageSize");

        // Fill the font name list with all installed fonts
        setListBoxStringItemList("DefaultFontName", 
        		GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        
        // AutoCorrect
        loadCheckBoxOption(xProps, "IgnoreHardLineBreaks");
        loadCheckBoxOption(xProps, "IgnoreEmptyParagraphs");
        loadCheckBoxOption(xProps, "IgnoreDoubleSpaces");

        // Special content
        loadCheckBoxOption(xProps, "DisplayHiddenText");
        loadCheckBoxOption(xProps, "Notes");
			
        // Document division
        loadCheckBoxOption(xProps, "Split");
        loadListBoxOption(xProps, "SplitLevel");
        loadCheckBoxOption(xProps, "UsePageBreakSplit");
        loadListBoxOption(xProps, "PageBreakSplit");
        loadCheckBoxOption(xProps, "UseSplitAfter");
        loadNumericOption(xProps, "SplitAfter");
        
        // Navigation table
        loadListBoxOption(xProps, "ExternalTocDepth");
        loadCheckBoxOption(xProps, "IncludeToc");

        updateLockedOptions();
        enableControls();
    }
	
    /** Save settings from the dialog to the registry and create FilterData */
    @Override protected void saveSettings(XPropertySet xProps, PropertyHelper helper) {
        // Style
        short nConfig = saveConfig(xProps, helper);
        switch (nConfig) {
            case 0: helper.put("ConfigURL","*default.xml"); break;
            case 1: helper.put("ConfigURL","$(user)/writer2xhtml.xml");
            		helper.put("AutoCreate","true");
            		helper.put("TemplateURL", "$(user)/writer2xhtml-template.xhtml");
            		helper.put("StyleSheetURL", "$(user)/writer2xhtml-styles.css");
        }
		
        saveNumericOptionAsPercentage(xProps, helper, "Scaling", "scaling");
        saveNumericOptionAsPercentage(xProps, helper, "ColumnScaling", "column_scaling");
        saveCheckBoxOption(xProps, helper, "RelativeFontSize", "relative_font_size");
        saveNumericOptionAsPercentage(xProps, helper, "FontScaling", "font_scaling");
        saveCheckBoxOption(xProps, helper, "UseDefaultFont", "use_default_font");
        saveTextFieldOption(xProps, helper, "DefaultFontName", "default_font_name");
        saveCheckBoxOption(xProps, helper, "ConvertToPx", "convert_to_px");
        saveCheckBoxOption(xProps, helper, "OriginalImageSize", "original_image_size");

        // AutoCorrect
        saveCheckBoxOption(xProps, helper, "IgnoreHardLineBreaks", "ignore_hard_line_breaks");
        saveCheckBoxOption(xProps, helper, "IgnoreEmptyParagraphs", "ignore_empty_paragraphs");
        saveCheckBoxOption(xProps, helper, "IgnoreDoubleSpaces", "ignore_double_spaces");

        // Special content
        saveCheckBoxOption(xProps, helper, "DisplayHiddenText", "display_hidden_text");
        saveCheckBoxOption(xProps, helper, "Notes", "notes");
  		
        // Document division
        boolean bSplit = saveCheckBoxOption(xProps, "Split");
        short nSplitLevel = saveListBoxOption(xProps, "SplitLevel");
        if (!isLocked("split_level")) {
            if (bSplit) {
               helper.put("split_level",Integer.toString(nSplitLevel+1));
            }
            else {
                helper.put("split_level","0");
            }
        }
        
        boolean bUsePageBreakSplit = saveCheckBoxOption(xProps, "UsePageBreakSplit");
        short nPageBreakSplit = saveListBoxOption(xProps, "PageBreakSplit");
        if (!isLocked("page_break_split")) {
            if (bUsePageBreakSplit) {
            	switch (nPageBreakSplit) {
            	case 0: helper.put("page_break_split", "styles"); break;
            	case 1: helper.put("page_break_split", "explicit"); break;
            	case 2: helper.put("page_break_split", "all");
            	}
            }
            else {
                helper.put("page_break_split","none");
            }
        }

        boolean bUseSplitAfter = saveCheckBoxOption(xProps, "UseSplitAfter");
        int nSplitAfter = saveNumericOption(xProps, "SplitAfter");
        if (!isLocked("split_after")) {
        	if (bUseSplitAfter) {
        		helper.put("split_after", Integer.toString(nSplitAfter));
        	}
        	else {
        		helper.put("split_after", "0");
        	}
        }
        
        // Navigation table
        short nExternalTocDepth = saveListBoxOption(xProps, "ExternalTocDepth");
        helper.put("external_toc_depth", Integer.toString(nExternalTocDepth+1));
        saveCheckBoxOption(xProps, helper, "IncludeToc", "include_toc");
    }
	
	
    // Implement XDialogEventHandler
    @Override public boolean callHandlerMethod(XDialog xDialog, Object event, String sMethod) {
        if (sMethod.equals("ConfigChange")) {
            updateLockedOptions();
            enableControls();
        }
        else if (sMethod.equals("RelativeFontSizeChange")) {
        	relativeFontSizeChange();
        }
        else if (sMethod.equals("UseDefaultFontChange")) {
        	useDefaultFontChange();
        }
        else if (sMethod.equals("EditMetadataClick")) {
            editMetadataClick();
        }
        else if (sMethod.equals("SplitChange")) {
            splitChange();
        }
        else if (sMethod.equals("UsePageBreakSplitChange")) {
        	usePageBreakSplitChange();
        }
        else if (sMethod.equals("UseSplitAfterChange")) {
        	useSplitAfterChange();
        }
        return true;
    }

    @Override public String[] getSupportedMethodNames() {
        String[] sNames = { "ConfigChange", "RelativeFontSizeChange", "UseDefaultFontChange", "EditMetadataClick",
        		"SplitChange", "UsePageBreakSplitChange", "UseSplitAfterChange" };
        return sNames;
    }
	
    private void enableControls() {
        // Style
        setControlEnabled("ScalingLabel",!isLocked("scaling"));
        setControlEnabled("Scaling",!isLocked("scaling"));
        setControlEnabled("ColumnScalingLabel",!isLocked("column_scaling"));
        setControlEnabled("ColumnScaling",!isLocked("column_scaling"));
        
        boolean bRelativeFontSize = getCheckBoxStateAsBoolean("RelativeFontSize");
        setControlEnabled("RelativeFontSize",!isLocked("relative_font_size"));
		setControlEnabled("FontScalingLabel", !isLocked("font_scaling") && bRelativeFontSize);    		
        setControlEnabled("FontScaling",!isLocked("font_scaling") && bRelativeFontSize);
		setControlEnabled("FontScalingPercentLabel", !isLocked("font_scaling") && bRelativeFontSize);
		
		boolean bUseDefaultFont = getCheckBoxStateAsBoolean("UseDefaultFont");
		setControlEnabled("UseDefaultFont",!isLocked("use_default_font"));
		setControlEnabled("DefaultFontNameLabel",!isLocked("default_font_name") && bUseDefaultFont);
		setControlEnabled("DefaultFontName",!isLocked("default_font_name") && bUseDefaultFont);
        
		setControlEnabled("ConvertToPx",!isLocked("convert_to_px"));
        setControlEnabled("OriginalImageSize",!isLocked("original_image_size"));

        // AutoCorrect
        setControlEnabled("IgnoreHardLineBreaks",!isLocked("ignore_hard_line_breaks"));
        setControlEnabled("IgnoreEmptyParagraphs",!isLocked("ignore_empty_paragraphs"));
        setControlEnabled("IgnoreDoubleSpaces",!isLocked("ignore_double_spaces"));

        // Special content
        setControlEnabled("DisplayHiddenText",!isLocked("display_hidden_text"));
        setControlEnabled("Notes",!isLocked("notes"));
			
        // Document division
        boolean bSplit = getCheckBoxStateAsBoolean("Split");
        setControlEnabled("Split",!isLocked("split_level"));
        setControlEnabled("SplitLevelLabel",!isLocked("split_level") && bSplit);
        setControlEnabled("SplitLevel",!isLocked("split_level") && bSplit);
        
        boolean bUsePageBreakSplit = getCheckBoxStateAsBoolean("UsePageBreakSplit");
        setControlEnabled("UsePageBreakSplit",!isLocked("page_break_split"));
        setControlEnabled("PageBreakSplitLabel",!isLocked("page_break_split") && bUsePageBreakSplit);
        setControlEnabled("PageBreakSplit",!isLocked("page_break_split") && bUsePageBreakSplit);

        boolean bUseSplitAfter = getCheckBoxStateAsBoolean("UseSplitAfter");
        setControlEnabled("UseSplitAfter",!isLocked("split_after"));
        setControlEnabled("SplitAfterLabel",!isLocked("split_after") && bUseSplitAfter);
        setControlEnabled("SplitAfter",!isLocked("split_after") && bUseSplitAfter);
        
        // Navigation table
        setControlEnabled("ExternalTocDepthLabel", !isLocked("external_toc_depth"));
        setControlEnabled("ExternalTocDepth", !isLocked("external_toc_depth"));
        setControlEnabled("IncludeToc", !isLocked("include_toc"));
    }
	
    private void relativeFontSizeChange() {
    	if (!isLocked("font_scaling")) {
    		boolean bState = getCheckBoxStateAsBoolean("RelativeFontSize");
    		setControlEnabled("FontScalingLabel", bState);
    		setControlEnabled("FontScaling", bState);
    		setControlEnabled("FontScalingPercentLabel", bState);    		
    	}
    }
    
    private void useDefaultFontChange() {
    	if (!isLocked("default_font_name")) {
    		boolean bState = getCheckBoxStateAsBoolean("UseDefaultFont");
    		setControlEnabled("DefaultFontNameLabel", bState);
    		setControlEnabled("DefaultFontName", bState);
    	}    	
    }
    
    private void editMetadataClick() {
        Object dialog;
		try {
			dialog = xContext.getServiceManager().createInstanceWithContext("org.openoffice.da.writer2xhtml.EpubMetadataDialog", xContext);
	        XExecutableDialog xDialog = (XExecutableDialog) UnoRuntime.queryInterface(XExecutableDialog.class, dialog);
	        xDialog.execute();
	        // Dispose the dialog after execution (to free up the memory)
	        XComponent xComponent = (XComponent) UnoRuntime.queryInterface(XComponent.class, dialog);
	        if (xComponent!=null) {
	        	System.out.println("Disposing the dialog!");
	        	xComponent.dispose();
	        }
		} catch (Exception e) {
			// Failed to get dialog
		}
    }
    
    private void splitChange() {
        if (!isLocked("split_level")) {
            boolean bState = getCheckBoxStateAsBoolean("Split");
            setControlEnabled("SplitLevelLabel",bState);
            setControlEnabled("SplitLevel",bState);
        }
    }
    
    private void usePageBreakSplitChange() {
        if (!isLocked("page_break_split")) {
            boolean bState = getCheckBoxStateAsBoolean("UsePageBreakSplit");
            setControlEnabled("PageBreakSplitLabel",bState);
            setControlEnabled("PageBreakSplit",bState);
        }    	
    }
    
    private void useSplitAfterChange() {
        if (!isLocked("split_after")) {
            boolean bState = getCheckBoxStateAsBoolean("UseSplitAfter");
            setControlEnabled("SplitAfterLabel",bState);
            setControlEnabled("SplitAfter",bState);
        }
    }
    
    
    
}
