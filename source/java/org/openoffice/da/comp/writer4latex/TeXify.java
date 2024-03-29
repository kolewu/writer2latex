/************************************************************************
 *
 *  TeXify.java
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
 *  Copyright: 2002-2010 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.2 (2011-01-25)
 *
 */ 
 
package org.openoffice.da.comp.writer4latex;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.sun.star.uno.XComponentContext;
       
/** This class builds LaTeX documents into DVI, Postscript or PDF and displays
 *  the result.
 */
public final class TeXify {
	
    /** Backend format generic (dvi) */
    public static final short GENERIC = 1;

    /** Backend format dvips (postscript) */
    public static final short DVIPS = 2;

    /** Backend format pdfTeX (pdf) */
    public static final short PDFTEX = 3;
    
    /** Backend format XeTeX (also pdf, usually) */
    public static final short XETEX = 4;

    // Define the applications to run for each backend
    private static final String[] genericTexify = {
        ExternalApps.LATEX, ExternalApps.BIBTEX, ExternalApps.MAKEINDEX,
        ExternalApps.LATEX, ExternalApps.MAKEINDEX, ExternalApps.LATEX };
    private static final String[] pdfTexify = {
        ExternalApps.PDFLATEX, ExternalApps.BIBTEX, ExternalApps.MAKEINDEX,
        ExternalApps.PDFLATEX, ExternalApps.MAKEINDEX, ExternalApps.PDFLATEX };
    private static final String[] dvipsTexify = {
        ExternalApps.LATEX, ExternalApps.BIBTEX, ExternalApps.MAKEINDEX,
        ExternalApps.LATEX, ExternalApps.MAKEINDEX, ExternalApps.LATEX,
        ExternalApps.DVIPS };
    private static final String[] xeTexify = {
        ExternalApps.XELATEX, ExternalApps.BIBTEX, ExternalApps.MAKEINDEX,
        ExternalApps.XELATEX, ExternalApps.MAKEINDEX, ExternalApps.XELATEX };

    // Global objects
    //private XComponentContext xContext;
    private ExternalApps externalApps; 
	
    public TeXify(XComponentContext xContext) {
        //this.xContext = xContext;
        externalApps = new ExternalApps(xContext);
    }
	
    /** Process a document
     *  @param file the LaTeX file to process
     *  @param sBibinputs value for the BIBINPUTS environment variable (or null if it should not be extended)
     *  @param nBackend the desired backend format (generic, dvips, pdftex)
     *  @param bView set the true if the result should be displayed in the viewer
     *  @throws IOException if the document cannot be read
     *  @return true if the first LaTeX run was successful
     */
    public boolean process(File file, String sBibinputs, short nBackend, boolean bView) throws IOException {
        // Remove extension from file
        if (file.getName().endsWith(".tex")) {
            file = new File(file.getParentFile(),
                   file.getName().substring(0,file.getName().length()-4));
        }
        
        // Update external apps from registry
        externalApps.load();

        // Process LaTeX document
        boolean bResult = false;
        if (nBackend==GENERIC) {
            bResult = doTeXify(genericTexify, file, sBibinputs);
            if (!bResult) return false;
            if (externalApps.execute(ExternalApps.DVIVIEWER,
                new File(file.getParentFile(),file.getName()+".dvi").getPath(),
                file.getParentFile(), null, false)>0) {
                throw new IOException("Error executing dvi viewer");
            }
        }
        else if (nBackend==PDFTEX) {
        	bResult = doTeXify(pdfTexify, file, sBibinputs);
            if (!bResult) return false;
            if (externalApps.execute(ExternalApps.PDFVIEWER,
                new File(file.getParentFile(),file.getName()+".pdf").getPath(),
                file.getParentFile(), null, false)>0) {
                throw new IOException("Error executing pdf viewer");
            }
        }
        else if (nBackend==DVIPS) {
        	bResult = doTeXify(dvipsTexify, file, sBibinputs);
            if (!bResult) return false;
            if (externalApps.execute(ExternalApps.POSTSCRIPTVIEWER,
                new File(file.getParentFile(),file.getName()+".ps").getPath(),
                file.getParentFile(), null, false)>0) {
                throw new IOException("Error executing postscript viewer");
            }
        }
        else if (nBackend==XETEX) {
        	bResult = doTeXify(xeTexify, file, sBibinputs);
            if (!bResult) return false;
            if (externalApps.execute(ExternalApps.PDFVIEWER,
                    new File(file.getParentFile(),file.getName()+".pdf").getPath(),
                    file.getParentFile(), null, false)>0) {
                    throw new IOException("Error executing pdf viewer");
                }
        }
        return bResult;

    }
	
    private boolean doTeXify(String[] sAppList, File file, String sBibinputs) throws IOException {
    	// Remove the .aux file first (to avoid potential error messages)
        File aux = new File(file.getParentFile(), file.getName()+".aux");
        aux.delete();
        for (int i=0; i<sAppList.length; i++) {
            // Execute external application
        	Map<String,String> env =null;
        	if (ExternalApps.BIBTEX.equals(sAppList[i]) && sBibinputs!=null) {
        		env = new HashMap<String,String>();
        		env.put("BIBINPUTS", sBibinputs);
        	}
            int nReturnCode = externalApps.execute(
                sAppList[i], file.getName(), file.getParentFile(), env, true);
            if (i==0 && nReturnCode>0) {
            	return false;
                //throw new IOException("Error executing "+sAppList[i]);
            }
        }
                
        return true;
    }

}