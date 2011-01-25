/************************************************************************
 *
 *  BibliographyDialog.java
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
 *  Version 1.2 (2011-01-24)
 *
 */ 
 
package org.openoffice.da.comp.writer4latex;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.sun.star.awt.XContainerWindowEventHandler;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XWindow;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XChangesBatch;

import com.sun.star.lib.uno.helper.WeakBase;

import org.openoffice.da.comp.w2lcommon.helper.DialogAccess;
import org.openoffice.da.comp.w2lcommon.helper.FolderPicker;
import org.openoffice.da.comp.w2lcommon.helper.RegistryHelper;
import org.openoffice.da.comp.w2lcommon.helper.XPropertySetHelper;

/** This class provides a uno component which implements the configuration
 *  of the bibliography in Writer4LaTeX.
 */
public final class BibliographyDialog
    extends WeakBase
    implements XServiceInfo, XContainerWindowEventHandler {
	
	public static final String REGISTRY_PATH = "/org.openoffice.da.Writer4LaTeX.Options/BibliographyOptions";

    private XComponentContext xContext;
    private FolderPicker folderPicker;
    
    /** The component will be registered under this name.
     */
    public static String __serviceName = "org.openoffice.da.writer4latex.BibliographyDialog";

    /** The component should also have an implementation name.
     */
    public static String __implementationName = "org.openoffice.da.comp.writer4latex.BibliographyDialog";

    /** Create a new ConfigurationDialog */
    public BibliographyDialog(XComponentContext xContext) {
        this.xContext = xContext;
        folderPicker = new FolderPicker(xContext);
    }

    
    // Implement XContainerWindowEventHandler
    public boolean callHandlerMethod(XWindow xWindow, Object event, String sMethod)
        throws com.sun.star.lang.WrappedTargetException {
		XDialog xDialog = (XDialog)UnoRuntime.queryInterface(XDialog.class, xWindow);
		DialogAccess dlg = new DialogAccess(xDialog);

        try {
            if (sMethod.equals("external_event") ){
                return handleExternalEvent(dlg, event);
            }
            else if (sMethod.equals("BibTeXDirClick")) {
                return bibTeXDirClick(dlg);
            }
            else if (sMethod.equals("ConvertZoteroCitationsChange")) {
                return convertZoteroCitationsChange(dlg);
            }
            else if (sMethod.equals("ConvertJabRefCitationsChange")) {
                return convertJabRefCitationsChange(dlg);
            }
            else if (sMethod.equals("UseExternalBibTeXFilesChange")) {
                return useExternalBibTeXFilesChange(dlg);
            }
        }
        catch (com.sun.star.uno.RuntimeException e) {
            throw e;
        }
        catch (com.sun.star.uno.Exception e) {
            throw new com.sun.star.lang.WrappedTargetException(sMethod, this, e);
        }
        return false;
    }
	
	public String[] getSupportedMethodNames() {
        String[] sNames = { "external_event", "UseExternalBibTeXFilesChange", "ConvertZoteroCitationsChange",
        		"ConvertJabRefCitationsChange", "ExternalBibTeXDirClick" };
        return sNames;
    }
    
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
	
    // Private stuff
    
    private boolean handleExternalEvent(DialogAccess dlg, Object aEventObject)
        throws com.sun.star.uno.Exception {
        try {
            String sMethod = AnyConverter.toString(aEventObject);
            if (sMethod.equals("ok")) {
                saveConfiguration(dlg);
                return true;
            } else if (sMethod.equals("back") || sMethod.equals("initialize")) {
                loadConfiguration(dlg);
                return true;
            }
        }
        catch (com.sun.star.lang.IllegalArgumentException e) {
            throw new com.sun.star.lang.IllegalArgumentException(
            "Method external_event requires a string in the event object argument.", this,(short) -1);
        }
        return false;
    }
    
    // Load settings from the registry into the dialog
    private void loadConfiguration(DialogAccess dlg) {
    	RegistryHelper registry = new RegistryHelper(xContext);
    	try {
    		Object view = registry.getRegistryView(REGISTRY_PATH, false);
    		XPropertySet xProps = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class,view);
    		dlg.setCheckBoxStateAsBoolean("UseExternalBibTeXFiles",
    				XPropertySetHelper.getPropertyValueAsBoolean(xProps, "UseExternalBibTeXFiles"));
    		dlg.setCheckBoxStateAsBoolean("ConvertZoteroCitations",
    				XPropertySetHelper.getPropertyValueAsBoolean(xProps, "ConvertZoteroCitations"));
    		dlg.setCheckBoxStateAsBoolean("ConvertJabRefCitations",
    				XPropertySetHelper.getPropertyValueAsBoolean(xProps, "ConvertJabRefCitations"));
        	dlg.setTextFieldText("NatbibOptions",
        			XPropertySetHelper.getPropertyValueAsString(xProps, "NatbibOptions"));
        	dlg.setTextFieldText("BibTeXDir",
        			XPropertySetHelper.getPropertyValueAsString(xProps, "BibTeXDir"));
        	registry.disposeRegistryView(view);
    	}
    	catch (Exception e) {
    		// Failed to get registry view
    	}
    	
    	// Update dialog according to the settings
    	convertZoteroCitationsChange(dlg);
    	useExternalBibTeXFilesChange(dlg);
	}

    // Save settings from the dialog to the registry
	private void saveConfiguration(DialogAccess dlg) {
		RegistryHelper registry = new RegistryHelper(xContext);
    	try {
    		Object view = registry.getRegistryView(REGISTRY_PATH, true);
    		XPropertySet xProps = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class,view);
			XPropertySetHelper.setPropertyValue(xProps, "UseExternalBibTeXFiles", dlg.getCheckBoxStateAsBoolean("UseExternalBibTeXFiles"));
    		XPropertySetHelper.setPropertyValue(xProps, "ConvertZoteroCitations", dlg.getCheckBoxStateAsBoolean("ConvertZoteroCitations"));
    		XPropertySetHelper.setPropertyValue(xProps, "ConvertJabRefCitations", dlg.getCheckBoxStateAsBoolean("ConvertJabRefCitations"));
   			XPropertySetHelper.setPropertyValue(xProps, "NatbibOptions", dlg.getTextFieldText("NatbibOptions"));
   			XPropertySetHelper.setPropertyValue(xProps, "BibTeXDir", dlg.getTextFieldText("BibTeXDir"));
   			
            // Commit registry changes
            XChangesBatch  xUpdateContext = (XChangesBatch)
                UnoRuntime.queryInterface(XChangesBatch.class,view);
            try {
                xUpdateContext.commitChanges();
            }
            catch (Exception e) {
                // ignore
            }
                        
        	registry.disposeRegistryView(view);
    	}
    	catch (Exception e) {
    		// Failed to get registry view
    	}		
	}

	private boolean useExternalBibTeXFilesChange(DialogAccess dlg) {
		enableBibTeXDir(dlg);
		return true;
	}

	private boolean convertZoteroCitationsChange(DialogAccess dlg) {
		enableNatbibOptions(dlg);
		enableBibTeXDir(dlg);
		return true;
	}

	private boolean convertJabRefCitationsChange(DialogAccess dlg) {
		enableNatbibOptions(dlg);
		enableBibTeXDir(dlg);
		return true;
	}
	
	private void enableNatbibOptions(DialogAccess dlg) {
		boolean bConvertZotero = dlg.getCheckBoxStateAsBoolean("ConvertZoteroCitations");
		boolean bConvertJabRef = dlg.getCheckBoxStateAsBoolean("ConvertJabRefCitations");
		dlg.setControlEnabled("NatbibOptionsLabel", bConvertZotero || bConvertJabRef);
		dlg.setControlEnabled("NatbibOptions", bConvertZotero || bConvertJabRef);
	}
	
	private void enableBibTeXDir(DialogAccess dlg) {
		boolean bExternal = dlg.getCheckBoxStateAsBoolean("UseExternalBibTeXFiles");
		boolean bConvertZotero = dlg.getCheckBoxStateAsBoolean("ConvertZoteroCitations");
		boolean bConvertJabRef = dlg.getCheckBoxStateAsBoolean("ConvertJabRefCitations");
		dlg.setControlEnabled("BibTeXDirLabel", bExternal || bConvertZotero || bConvertJabRef);
		dlg.setControlEnabled("BibTeXDir", bExternal || bConvertZotero || bConvertJabRef);
		dlg.setControlEnabled("BibTeXDirButton", bExternal|| bConvertZotero || bConvertJabRef);
	}

	private boolean bibTeXDirClick(DialogAccess dlg) {
		String sPath = folderPicker.getPath();
    	if (sPath!=null) {
    		try {
    			dlg.setTextFieldText("BibTeXDir", new File(new URI(sPath)).getCanonicalPath());
			}
    		catch (IOException e) {
			}
    		catch (URISyntaxException e) {
			}
    	}     
		return true;
	}
	
}



