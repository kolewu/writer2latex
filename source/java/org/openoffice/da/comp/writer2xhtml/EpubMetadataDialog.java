/************************************************************************
 *
 *  EpubMetadataDialog.java
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
 *  Version 1.2 (2011-02-23)
 *
 */

package org.openoffice.da.comp.writer2xhtml;

import java.util.HashSet;

import org.openoffice.da.comp.w2lcommon.helper.DialogBase;

import writer2latex.util.CSVList;

import com.sun.star.awt.XDialog;
import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.Property;
import com.sun.star.beans.PropertyExistException;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertyContainer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.document.XDocumentProperties;
import com.sun.star.document.XDocumentPropertiesSupplier;
import com.sun.star.frame.XDesktop;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XComponent;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

/** This class provides a UNO component which implements a custom metadata editor UI for the EPUB export
 */
public class EpubMetadataDialog extends DialogBase {
	// All the user defined properties we handle
	private static final String IDENTIFIER="Identifier";
	private static final String CREATOR="Creator";
	private static final String CONTRIBUTOR="Contributor";
	private static final String DATE="Date";
	private static final String PUBLISHER="Publisher";
	private static final String TYPE="Type";
	private static final String FORMAT="Format";
	private static final String SOURCE="Source";
	private static final String RELATION="Relation";
	private static final String COVERAGE="Coverage";
	private static final String RIGHTS="Rights";

	// Access to the document properties
	private XDocumentProperties xDocumentProperties=null;
	private XPropertyContainer xUserProperties=null;
	private XPropertySet xUserPropertySet=null;
    
    public EpubMetadataDialog(XComponentContext xContext) {
		super(xContext);
	}

	/** The component will be registered under this name.
     */
    public static String __serviceName = "org.openoffice.da.writer2xhtml.EpubMetadataDialog";

    /** The component should also have an implementation name.
     */
    public static String __implementationName = "org.openoffice.da.comp.writer2xhtml.EpubMetadataDialog";

    // --------------------------------------------------
    // Ensure that the super can find us :-)
    @Override public String getDialogLibraryName() {
		return "W2XDialogs2";
	}

	@Override public String getDialogName() {
		return "EpubMetadata";
	}
	
    // --------------------------------------------------
    // Implement the interface XDialogEventHandler
    @Override public boolean callHandlerMethod(XDialog xDialog, Object event, String sMethod) {
        if (sMethod.equals("UseCustomIdentifierChange")) {
        	return useCustomIdentifierChange();
        }
        else if (sMethod.equals("AuthorAddClick")) {
        	return authorAddclick();
        }
        else if (sMethod.equals("AuthorModifyClick")) {
        	return authorModifyclick();
        }
        else if (sMethod.equals("AuthorDeleteClick")) {
        	return authorDeleteclick();
        }
        else if (sMethod.equals("AuthorUpClick")) {
        	return authorUpclick();
        }
        else if (sMethod.equals("AuthorDownClick")) {
        	return authorDownclick();
        }
        else if (sMethod.equals("DateAddClick")) {
        	return dateAddClick();
        }
        else if (sMethod.equals("DateModifyClick")) {
        	return dateModifyClick();
        }
        else if (sMethod.equals("DateDeleteClick")) {
        	return dateDeleteClick();
        }
        return false;
    }
	
    @Override public String[] getSupportedMethodNames() {
        String[] sNames = { "UseCustomIdentifierChange",
        		"AuthorAddClick", "AuthorModifyClick", "AuthorDeleteClick", "AuthorUpClick", "AuthorDownClick",
        		"DataAddClick", "DateModifyClick", "DateDeleteClick"};
        return sNames;
    }
    
    private boolean useCustomIdentifierChange() {
    	boolean bEnabled = getCheckBoxStateAsBoolean("UseCustomIdentifier");
    	setControlEnabled("IdentifierLabel",bEnabled);
    	setControlEnabled("Identifier",bEnabled);
    	setControlEnabled("IdentifierTypeLabel",bEnabled);
    	setControlEnabled("IdentifierType",bEnabled);
    	return true;
    }
    
    private boolean authorAddclick() {
    	System.out.println("AuthorAddClick");
    	return true;
    }
    
    private boolean authorModifyclick() {
    	System.out.println("AuthorModifyClick");    	
    	return true;
    }
    
    private boolean authorDeleteclick() {
    	System.out.println("AuthorDeleteClick");	
    	return true;
    }
    
    private boolean authorUpclick() {
    	System.out.println("AuthorUpClick");	
    	return true;
    }
    
    private boolean authorDownclick() {
    	System.out.println("AuthorDownClick");	
    	return true;
    }
    
    private boolean dateAddClick() {
    	System.out.println("DateAddClick");	
    	return true;
    }
    
    private boolean dateModifyClick() {
    	System.out.println("DateModifyClick");
    	return true;
    }
    
    private boolean dateDeleteClick() {
    	System.out.println("DateDeleteClick");
    	return true;
    }
    
    // --------------------------------------------------
    // Get and set properties from and to current document 
    
	@Override protected void initialize() {
		// Get the document properties
    	XDesktop xDesktop;
    	Object desktop;
		try {
			desktop = xContext.getServiceManager().createInstanceWithContext("com.sun.star.frame.Desktop", xContext);
		} catch (Exception e) {
			// Failed to get desktop
			return;
		}
    	xDesktop = (XDesktop) UnoRuntime.queryInterface(com.sun.star.frame.XDesktop.class, desktop);
		XComponent xComponent = xDesktop.getCurrentComponent();
		XDocumentPropertiesSupplier xSupplier = (XDocumentPropertiesSupplier) UnoRuntime.queryInterface(XDocumentPropertiesSupplier.class, xComponent);
		
		// Get the document properties (we need several interfaces)
		xDocumentProperties = xSupplier.getDocumentProperties();
		xUserProperties= xDocumentProperties.getUserDefinedProperties();
		xUserPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xUserProperties);
		
		// Get the custom identifier and set the text fields
		String[] sIdentifiers = getProperties(IDENTIFIER,false);
		setCheckBoxStateAsBoolean("UseCustomIdentifier",sIdentifiers.length>0);
		useCustomIdentifierChange();
		if (sIdentifiers.length>0) { // Use the first if we have several...
			setTextFieldText("Identifier",getValue(sIdentifiers[0]));
			int nDot = sIdentifiers[0].indexOf(".");
			setTextFieldText("IdentifierType",nDot>-1 ? sIdentifiers[0].substring(nDot+1) : "");
		}
		
		// Get the standard properties and set the text fields
		setTextFieldText("Title",xDocumentProperties.getTitle());
		setTextFieldText("Subject",xDocumentProperties.getSubject());
		String[] sKeywords = xDocumentProperties.getKeywords();
		CSVList keywords = new CSVList(", ");
		for (String sKeyword : sKeywords) {
			keywords.addValue(sKeyword);
		}
		setTextFieldText("Keywords",keywords.toString());
		setTextFieldText("Description",xDocumentProperties.getDescription());
		
		// Get the simple user properties and set the text fields
		readSimpleProperty(PUBLISHER);
		readSimpleProperty(TYPE);
		readSimpleProperty(FORMAT);
		readSimpleProperty(SOURCE);
		readSimpleProperty(RELATION);
		readSimpleProperty(COVERAGE);
		readSimpleProperty(RIGHTS);

	}
		
	@Override protected void endDialog() {
		// Set the custom identifier from the text fields
		String[] sIdentifiers = getProperties(IDENTIFIER,false);
		for (String sIdentifier : sIdentifiers) { // Remove old identifier(s)
			removeProperty(sIdentifier);
		}
		if (getCheckBoxStateAsBoolean("UseCustomIdentifier")) {
			String sName = IDENTIFIER;
			if (getTextFieldText("IdentifierType").trim().length()>0) {
				sName+="."+getTextFieldText("IdentifierType").trim();
			}
			addProperty(sName);
			setValue(sName,getTextFieldText("Identifier"));
		}
		
		// Set the standard properties from the text fields
		xDocumentProperties.setTitle(getTextFieldText("Title"));
		xDocumentProperties.setSubject(getTextFieldText("Subject"));
		String[] sKeywords = getTextFieldText("Keywords").split(",");
		for (int i=0; i<sKeywords.length; i++) {
			sKeywords[i] = sKeywords[i].trim();
		}
		xDocumentProperties.setKeywords(sKeywords);
		xDocumentProperties.setDescription(getTextFieldText("Description"));
		
		// Set the simple user properties from the text fields
		writeSimpleProperty(PUBLISHER);
		writeSimpleProperty(TYPE);
		writeSimpleProperty(FORMAT);
		writeSimpleProperty(SOURCE);
		writeSimpleProperty(RELATION);
		writeSimpleProperty(COVERAGE);
		writeSimpleProperty(RIGHTS);
	}

	// Get all currently defined user properties with a specific name or prefix
	private String[] getProperties(String sPrefix, boolean bComplete) {
		HashSet<String> names = new HashSet<String>();
		Property[] xProps = xUserPropertySet.getPropertySetInfo().getProperties();
		for (Property prop : xProps) {
			String sName = prop.Name;
			String sLCName = sName.toLowerCase();
			String sLCPrefix = sPrefix.toLowerCase();
			if ((bComplete && sLCName.equals(sLCPrefix)) || (!bComplete && sLCName.startsWith(sLCPrefix))) {
				names.add(sName);
			}
		}
		return names.toArray(new String[names.size()]);
	}
	
	// Add a user property
	private void addProperty(String sName) {
		try {
			xUserProperties.addProperty(sName, (short) 128, ""); // 128 means removeable, last parameter is default value
		} catch (PropertyExistException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalTypeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// Delete a user property
	private void removeProperty(String sName) {
		try {
			xUserProperties.removeProperty(sName);
		} catch (UnknownPropertyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotRemoveableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// Set the value of a user property (failing silently if the property does not exist)
	private void setValue(String sName, String sValue) {
		try {
			xUserPropertySet.setPropertyValue(sName, sValue);
		} catch (UnknownPropertyException e) {
		} catch (PropertyVetoException e) {
		} catch (IllegalArgumentException e) {
		} catch (WrappedTargetException e) {
		}
	}
	
	// Get the value of a user property (returning null if the property does not exist)
	private String getValue(String sName) {
		Object value;
		try {
			value = xUserPropertySet.getPropertyValue(sName);
		} catch (UnknownPropertyException e) {
			return null;
		} catch (WrappedTargetException e) {
			return null;
		}

		if (AnyConverter.isString(value)) {
			try {
				return AnyConverter.toString(value);
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
		return null;
	}
	
	private void readSimpleProperty(String sName) {
		String[] sNames = getProperties(sName,true);
		if (sNames.length>0) {
			String sValue = getValue(sNames[0]);
			if (sValue!=null) {
				setTextFieldText(sName, sValue);
			}
		}
	}
	
	private void writeSimpleProperty(String sName) {
		String[] sOldNames = getProperties(sName,true);
		for (String sOldName : sOldNames) {
			removeProperty(sOldName);
		}
		String sValue = getTextFieldText(sName);
		if (sValue.length()>0) {
			addProperty(sName);
			setValue(sName,sValue);
		}
	}
	
    
}
