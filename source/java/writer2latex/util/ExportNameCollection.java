/************************************************************************
 *
 *  ExportNameCollection.java
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
 *  Copyright: 2002-2009 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.2 (2009-06-05)
 *
 */

package writer2latex.util;

import java.util.Enumeration;
import java.util.Hashtable;

/** Maintain a collection of export names. 
 *  This is used to map named collections to simpler names (only A-Z, a-z and 0-9, and possibly additional characters)
 */
public class ExportNameCollection{
    private Hashtable<String, String> exportNames = new Hashtable<String, String>();
    private String sPrefix;
    private String sAdditionalChars;
    private boolean bAcceptNumbers;
    
    public ExportNameCollection(String sPrefix, boolean bAcceptNumbers, String sAdditionalChars) {
        this.sPrefix=sPrefix;
        this.bAcceptNumbers = bAcceptNumbers;
        this.sAdditionalChars = sAdditionalChars;
    }
    
    public ExportNameCollection(String sPrefix, boolean bAcceptNumbers) {
    	this(sPrefix,bAcceptNumbers,"");
    }
	
    public ExportNameCollection(boolean bAcceptNumbers) {
        this("",bAcceptNumbers,"");
    }
	
    public Enumeration<String> keys() {
        return exportNames.keys();
    }
    
    public void addName(String sName){
        if (containsName(sName)) { return; }
        StringBuffer outbuf=new StringBuffer();
        SimpleInputBuffer inbuf=new SimpleInputBuffer(sName);
		
        // Don't start with a digit
        if (bAcceptNumbers && inbuf.peekChar()>='0' && inbuf.peekChar()<='9') {
            outbuf.append('a');
        }

        char c;
        // convert numbers to roman numbers and discard unwanted characters
        while ((c=inbuf.peekChar())!='\0'){
            if ((c>='a' && c<='z') || (c>='A' && c<='Z')) {
                outbuf.append(inbuf.getChar());
            }
            else if (c>='0' && c<='9'){
                if (bAcceptNumbers) {
                    outbuf.append(inbuf.getInteger());
                }
                else {
                    outbuf.append(Misc.int2roman(
                                  Integer.parseInt(inbuf.getInteger())));
                }
            }
            else if (sAdditionalChars.indexOf(c)>-1) {
            	outbuf.append(inbuf.getChar());
            }
            else {
                inbuf.getChar(); // ignore this character
            }
        }
        String sExportName=outbuf.toString();
        // the result may exist in the collecion; add a's at the end
        while (exportNames.containsValue(sExportName)){
            sExportName+="a";
        }
        exportNames.put(sName,sExportName);
    }
    
    public String getExportName(String sName) {
        // add the name, if it does not exist
        if (!containsName(sName)) { addName(sName); }
        return sPrefix + exportNames.get(sName);
    }

    public boolean containsName(String sName) {
        return exportNames.containsKey(sName);
    }
    
    public boolean isEmpty() {
        return exportNames.size()==0;
    }
}
