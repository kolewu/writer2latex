/************************************************************************
 *
 *  StyleMap.java
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
 *  Version 1.2 (2011-03-30) 
 * 
 */

package writer2latex.latex.util;

import java.util.Hashtable;
import java.util.Enumeration;

public class StyleMap {
    private Hashtable<String, StyleMapItem> items = new Hashtable<String, StyleMapItem>();
	
    public void put(String sName, String sBefore, String sAfter, String sNext, boolean bLineBreak, int nBreakAfter, boolean bVerbatim) {
        StyleMapItem item = new StyleMapItem();
        item.sBefore = sBefore;
        item.sAfter = sAfter;
        item.sNext = ";"+sNext+";";
        item.bLineBreak = bLineBreak;
        item.nBreakAfter = nBreakAfter;
        item.bVerbatim = bVerbatim;
        items.put(sName,item);
    }

    public void put(String sName, String sBefore, String sAfter, boolean bLineBreak, int nBreakAfter, boolean bVerbatim) {
        StyleMapItem item = new StyleMapItem();
        item.sBefore = sBefore;
        item.sAfter = sAfter;
        item.sNext = ";;";
        item.bLineBreak = bLineBreak;
        item.nBreakAfter = nBreakAfter;
        item.bVerbatim = bVerbatim;
        items.put(sName,item);
    }

    public void put(String sName, String sBefore, String sAfter, String sNext, boolean bVerbatim) {
        StyleMapItem item = new StyleMapItem();
        item.sBefore = sBefore;
        item.sAfter = sAfter;
        item.sNext = ";"+sNext+";";
        item.bLineBreak = true;
        item.nBreakAfter = StyleMapItem.PAR;
        item.bVerbatim = bVerbatim;
        items.put(sName,item);
    }
	
    public void put(String sName, String sBefore, String sAfter) {
        StyleMapItem item = new StyleMapItem();
        item.sBefore = sBefore;
        item.sAfter = sAfter;
        item.sNext = ";;";
        item.bLineBreak = true;
        item.nBreakAfter = StyleMapItem.PAR;
        item.bVerbatim = false;
        items.put(sName,item);
    }

    public boolean contains(String sName) {
        return sName!=null && items.containsKey(sName);
    }
	
    public String getBefore(String sName) {
        return items.get(sName).sBefore;
    }

    public String getAfter(String sName) {
        return items.get(sName).sAfter;
    }
	
    public String getNext(String sName) {
        String sNext = items.get(sName).sNext;
        return sNext.substring(1,sNext.length()-1);
    }

    public boolean isNext(String sName, String sNext) {
        String sNext1 = items.get(sName).sNext;
        return sNext1.indexOf(";"+sNext+";")>-1;
    }
	
    public boolean getLineBreak(String sName) {
        return contains(sName) && items.get(sName).bLineBreak;
    }
    
    public int getBreakAfter(String sName) {
    	return contains(sName) ? items.get(sName).nBreakAfter : StyleMapItem.PAR;
    }

    public boolean getVerbatim(String sName) {
        return contains(sName) && items.get(sName).bVerbatim;
    }

    public Enumeration<String> getNames() {
        return items.keys();
    }
	
}
