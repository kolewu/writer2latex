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
 *  Version 1.2 (2011-02-22)
 *
 */

package org.openoffice.da.comp.writer2xhtml;

import java.util.HashSet;

import org.openoffice.da.comp.w2lcommon.helper.DialogBase;

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

	// Access to the user defined properties
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

	@Override
	public String getDialogLibraryName() {
		return "W2XDialogs2";
	}

	@Override
	public String getDialogName() {
		return "EpubMetadata";
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
	
	@Override
	protected void initialize() {
		// Get the document properties
    	XDesktop xDesktop;
    	Object desktop;
		try {
			desktop = xContext.getServiceManager().createInstanceWithContext("com.sun.star.frame.Desktop", xContext);
		} catch (Exception e) {
			// Failed to get desktop
			System.out.println("Failed to get desktop");
			return;
		}
    	xDesktop = (XDesktop) UnoRuntime.queryInterface(com.sun.star.frame.XDesktop.class, desktop);
		XComponent xComponent = xDesktop.getCurrentComponent();
		XDocumentPropertiesSupplier xSupplier = (XDocumentPropertiesSupplier) UnoRuntime.queryInterface(XDocumentPropertiesSupplier.class, xComponent);
		XDocumentProperties xProperties = xSupplier.getDocumentProperties();
		
		// Get the user defined properties from the properties (we need several interfaces)
		xUserProperties= xProperties.getUserDefinedProperties();
		xUserPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xUserProperties);
		
		// Get and fill the simple values
		readSimpleProperty(PUBLISHER);
		readSimpleProperty(TYPE);
		readSimpleProperty(FORMAT);
		readSimpleProperty(SOURCE);
		readSimpleProperty(RELATION);
		readSimpleProperty(COVERAGE);
		readSimpleProperty(RIGHTS);

	}
		
	@Override
	protected void finalize() {
		// Set the simple values
		writeSimpleProperty(PUBLISHER);
		writeSimpleProperty(TYPE);
		writeSimpleProperty(FORMAT);
		writeSimpleProperty(SOURCE);
		writeSimpleProperty(RELATION);
		writeSimpleProperty(COVERAGE);
		writeSimpleProperty(RIGHTS);
		
		xUserProperties = null;
		xUserPropertySet = null;
	}

    
}
