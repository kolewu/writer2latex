/************************************************************************
 *
 *  ExternalApps.java
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
 *  Version 1.2 (2011-01-25)
 *
 */ 
 
package org.openoffice.da.comp.writer4latex;

import java.io.File;
import java.io.IOException;
import java.lang.Process;
import java.lang.ProcessBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.openoffice.da.comp.w2lcommon.helper.RegistryHelper;
//import java.util.Map;

import com.sun.star.beans.XMultiHierarchicalPropertySet;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XChangesBatch;

       
/** This class manages and executes external applications used by Writer4LaTeX.
 *  These include TeX and friends as well as viewers for the various backend
 *  formats. The registry is used for persistent storage of the settings.
 */  
public class ExternalApps {

    public final static String LATEX = "LaTeX";
    public final static String PDFLATEX = "PdfLaTeX";
    public final static String XELATEX = "XeLaTeX";
    public final static String BIBTEX = "BibTeX";
    public final static String MAKEINDEX = "Makeindex";
    public final static String MK4HT = "Mk4ht";
    public final static String DVIPS = "Dvips";
    public final static String DVIVIEWER = "DVIViewer";
    public final static String POSTSCRIPTVIEWER = "PostscriptViewer";
    public final static String PDFVIEWER = "PdfViewer";
	
    private final static String[] sApps = { LATEX, PDFLATEX, XELATEX, BIBTEX, MAKEINDEX, MK4HT, DVIPS, DVIVIEWER, POSTSCRIPTVIEWER, PDFVIEWER };
	
    private XComponentContext xContext;

    private HashMap<String,String[]> apps;
	
    /** Construct a new ExternalApps object, with empty definitions */
    public ExternalApps(XComponentContext xContext) {
        this.xContext = xContext;
        apps = new HashMap<String,String[]>();
        for (int i=0; i<sApps.length; i++) {
            setApplication(sApps[i], "?", "?");
        }
    }
	
    /** Define an external application
     *  @param sAppName the name of the application to define
     *  @param sExecutable the system dependent path to the executable file
     *  @param sOptions the options to the external application; %s will be
     *  replaced by the filename on execution 
     */
    public void setApplication(String sAppName, String sExecutable, String sOptions) {
        String[] sValue = { sExecutable, sOptions };
        apps.put(sAppName, sValue);
    }
	
    /** Get the definition for an external application
     *  @param sAppName the name of the application to get
     *  @return a String array containg the system dependent path to the
     *  executable file as entry 0, and the parameters as entry 1
     *  returns null if the application is unknown
     */
    public String[] getApplication(String sAppName) {
        return apps.get(sAppName);
    } 
    
    /** Execute an external application
     *  @param sAppName the name of the application to execute
     *  @param sFileName the file name to use
     *  @param workDir the working directory to use
     *  @param env map of environment variables to set (or null if no variables needs to be set)
     *  @param bWaitFor true if the method should wait for the execution to finish
     *  @return error code 
     */
    public int execute(String sAppName, String sFileName, File workDir, Map<String,String> env, boolean bWaitFor) {
    	return execute(sAppName, "", sFileName, workDir, env, bWaitFor);
    }
	
    /** Execute an external application
     *  @param sAppName the name of the application to execute
     *  @param sCommand subcommand/option to pass to the command
     *  @param sFileName the file name to use
     *  @param workDir the working directory to use
     *  @param env map of environment variables to set (or null if no variables needs to be set)
     *  @param bWaitFor true if the method should wait for the execution to finish
     *  @return error code 
     */
    public int execute(String sAppName, String sCommand, String sFileName, File workDir, Map<String,String> env, boolean bWaitFor) {
        // Assemble the command
        String[] sApp = getApplication(sAppName);
        if (sApp==null) { return 1; }
 
        try {
			Vector<String> command = new Vector<String>();
			command.add(sApp[0]);
			String[] sArguments = sApp[1].split(" ");
			for (String s : sArguments) {
				command.add(s.replace("%c",sCommand).replace("%s",sFileName));
			}
			
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workDir);
            if (env!=null) {
            	pb.environment().putAll(env);
            	if (env.containsKey("BIBINPUTS")) {
            		System.out.println("Running "+sApp[0]+" with BIBINPUTS="+env.get("BIBINPUTS"));
            	}
            }
            Process proc = pb.start();        
        
            // Gobble the error stream of the application
            StreamGobbler errorGobbler = new 
                StreamGobbler(proc.getErrorStream(), "ERROR");            
            
            // Gooble the output stream of the application
            StreamGobbler outputGobbler = new 
                StreamGobbler(proc.getInputStream(), "OUTPUT");
                
            // Kick them off
            errorGobbler.start();
            outputGobbler.start();
                                    
            // Any error?
            return bWaitFor ? proc.waitFor() : 0;
        }
        catch (InterruptedException e) {
            return 1;
        }
        catch (IOException e) {
            return 1;
        }
    }
	
    /** Load the external applications from the registry
     */
    public void load() {
    	RegistryHelper registry = new RegistryHelper(xContext);
    	Object view;
    	try {
    		// Prepare registry view
    		view = registry.getRegistryView("/org.openoffice.da.Writer4LaTeX.Options/Applications",false);
    	}
        catch (com.sun.star.uno.Exception e) {
            // Give up...
            //setApplication(LATEX,"Error!",e.getMessage());
            return;
        }

        XMultiHierarchicalPropertySet xProps = (XMultiHierarchicalPropertySet)
            UnoRuntime.queryInterface(XMultiHierarchicalPropertySet.class, view);
        for (int i=0; i<sApps.length; i++) {
            String[] sNames = new String[2];
            sNames[0] = sApps[i]+"/Executable";
            sNames[1] = sApps[i]+"/Options";
            try {
                Object[] values = xProps.getHierarchicalPropertyValues(sNames);
                setApplication(sApps[i], (String) values[0], (String) values[1]);
            }
            catch (com.sun.star.uno.Exception e) {
                // Ignore...
            }
        }
		
        registry.disposeRegistryView(view);
    }
	
    /** Save the external applications to the registry
     */
    public void save() {
    	RegistryHelper registry = new RegistryHelper(xContext);
        Object view;
        try {
    		view = registry.getRegistryView("/org.openoffice.da.Writer4LaTeX.Options/Applications",true);
        }
        catch (com.sun.star.uno.Exception e) {
            // Give up...
            return;
        }

        XMultiHierarchicalPropertySet xProps = (XMultiHierarchicalPropertySet)
            UnoRuntime.queryInterface(XMultiHierarchicalPropertySet.class, view);
        for (int i=0; i<sApps.length; i++) {
            String[] sNames = new String[2];
            sNames[0] = sApps[i]+"/Executable";
            sNames[1] = sApps[i]+"/Options";
            String[] sValues = getApplication(sApps[i]);
            try {
                xProps.setHierarchicalPropertyValues(sNames, sValues);
            }
            catch (com.sun.star.uno.Exception e) {
                // Ignore...
            }
        }
		
        // Commit registry changes
        XChangesBatch  xUpdateContext = (XChangesBatch)
            UnoRuntime.queryInterface(XChangesBatch.class, view);
        try {
            xUpdateContext.commitChanges();
        }
        catch (Exception e) {
            // ignore
        }

        registry.disposeRegistryView(view);
    }
	
}