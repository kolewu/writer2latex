/************************************************************************
 *
 *  Misc.java
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
 *  Version 1.2 (2012-02-26)
 *
 */

package writer2latex.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.Math;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.text.Collator;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
//import java.util.Hashtable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

// This class contains some usefull, but unrelated static methods 
public class Misc{

    private final static int BUFFERSIZE = 1024;

    public static final int[] doubleIntArray(int[] array) {
        int n = array.length;
        int[] newArray = new int[2*n];
        for (int i=0; i<n; i++) { newArray[i] = array[i]; }
        return newArray;
    }
    
    // Truncate a date+time to the date only
    public static final String dateOnly(String sDate) {
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
   		Date date = null;
    	try {
			date = sdf.parse(sDate);
		} catch (ParseException e) {
			// If the date cannot be parsed according to the given pattern, return the original string
			return sDate;
		}
		// Return using a default format for the given locale
		return sDate.substring(0,10);   		    	
    }
    
    public static final String formatDate(String sDate, String sLanguage, String sCountry) {
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
   		Date date = null;
    	try {
			date = sdf.parse(sDate);
		} catch (ParseException e) {
			// If the date cannot be parsed according to the given pattern, return the original string
			return sDate;
		}
		// Return using a default format for the given locale
		Locale locale = sCountry!=null ? new Locale(sLanguage,sCountry) : new Locale(sLanguage);
		return DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, locale).format(date);   		
    }
	
    public static final String int2roman(int number) {
    	assert number>0; // Only works for positive numbers!
        StringBuffer roman=new StringBuffer();
        while (number>=1000) { roman.append('m'); number-=1000; }
        if (number>=900) { roman.append("cm"); number-=900; }
        if (number>=500) { roman.append('d'); number-=500; }
        if (number>=400) { roman.append("cd"); number-=400; }
        while (number>=100) { roman.append('c'); number-=100; }
        if (number>=90) { roman.append("xc"); number-=90; }
        if (number>=50) { roman.append('l'); number-=50; }
        if (number>=40) { roman.append("xl"); number-=40; }
        while (number>=10) { roman.append('x'); number-=10; }
        if (number>=9) { roman.append("ix"); number-=9; }
        if (number>=5) { roman.append('v'); number-=5; }
        if (number>=4) { roman.append("iv"); number-=4; }
        while (number>=1) { roman.append('i'); number-=1; }
        return roman.toString();        
    }
	
    public static final String int2Roman(int number) {
        return int2roman(number).toUpperCase();
    }
	
    public static final String int2arabic(int number) {
        return new Integer(number).toString();
    }
	
    public static final String int2alph(int number, boolean bLetterSync) {
    	assert number>0; // Only works for positive numbers!
    	if (bLetterSync) {
    		char[] chars = new char[(number-1)/26+1]; // Repeat the character this number of times
    		Arrays.fill(chars, (char) ((number-1) % 26+97)); // Use this character
    		return String.valueOf(chars);
    	}
    	else {
    		int n=number-1;
    		// Least significant digit is special because a is treated as zero here!
    		int m = n % 26;
    		String sNumber = Character.toString((char) (m+97));
    		n = (n-m)/26;
    		// For the more significant digits, a is treated as one!
    		while (n>0) {
    			m = n % 26; // Calculate new least significant digit
   				sNumber = ((char) (m+96))+sNumber;
    			n = (n-m)/26;
    		}
            return sNumber;
    	}
    }
	
    public static final String int2Alph(int number, boolean bLetterSync) {
        return int2alph(number,bLetterSync).toUpperCase();
    }
    
    public static final int getPosInteger(String sInteger, int nDefault){
        int n;
        try {
            n=Integer.parseInt(sInteger);
        }
        catch (NumberFormatException e) {
            return nDefault;
        }
        return n>0 ? n : nDefault;
    }
    
    public static final float getFloat(String sFloat, float fDefault){
        float f;
        try {
            f=Float.parseFloat(sFloat);
        }
        catch (NumberFormatException e) {
            return fDefault;
        }
        return f;
    }
    
    public static final int getIntegerFromHex(String sHex, int nDefault){
        int n;
        try {
            n=Integer.parseInt(sHex,16);
        }
        catch (NumberFormatException e) {
            return nDefault;
        }
        return n;
    }
	
    public static String truncateLength(String sValue) {
        if (sValue.endsWith("inch")) {
            // Cut of inch to in
            return sValue.substring(0,sValue.length()-2);
        }
        else {
            return sValue;
        }
    }
	
    public static boolean isZero(String sValue) {
    	return Math.abs(getFloat(sValue.substring(0, sValue.length()-2),0))<0.001;
    }
    
    // Return units per inch for some unit
    private static final float getUpi(String sUnit) {
        if ("in".equals(sUnit)) { return 1.0F; }
        else if ("mm".equals(sUnit)) { return 25.4F; }
        else if ("cm".equals(sUnit)) { return 2.54F; }
        else if ("pc".equals(sUnit)) { return 6F; }
        else { return 72; } // pt or unknown
    }
	
    // Convert a length to px assuming 96ppi (cf. css spec)
    // Exception: Never return less than 1px
    public static final String length2px(String sLength) {
        if (sLength.equals("0")) { return "0"; }
        float fLength=getFloat(sLength.substring(0,sLength.length()-2),1);
        String sUnit=sLength.substring(sLength.length()-2);
        float fPixels = 96.0F/getUpi(sUnit)*fLength;
        if (Math.abs(fPixels)<0.01) {
            // Very small, treat as zero
            return "0";
        }
        else if (fPixels>0) {
            // Never return less that 1px
            return Float.toString(fPixels<1 ? 1 : fPixels)+"px";
        }
        else {
            // Or above -1px
            return Float.toString(fPixels>-1 ? -1 : fPixels)+"px";
        }
    }
    
    // Divide dividend by divisor and return the quotient as an integer percentage
    // (never below 1% except if the dividend is zero)
    public static final String divide(String sDividend, String sDivisor) {
    	return divide(sDividend,sDivisor,false);
    }

    // Divide dividend by divisor and return the quotient as an integer percentage
    // (never below 1% except if the dividend is zero, and never above 100% if last parameter is true)
    public static final String divide(String sDividend, String sDivisor, boolean bMax100) {
        if (sDividend.equals("0")) { return "0%"; }
        if (sDivisor.equals("0")) { return "100%"; }

        float fDividend=getFloat(sDividend.substring(0,sDividend.length()-2),1);
        String sDividendUnit=sDividend.substring(sDividend.length()-2);
        float fDivisor=getFloat(sDivisor.substring(0,sDivisor.length()-2),1);
        String sDivisorUnit=sDivisor.substring(sDivisor.length()-2);
        int nPercent = Math.round(100*fDividend*getUpi(sDivisorUnit)/fDivisor/getUpi(sDividendUnit));
        if (bMax100 && nPercent>100) {
        	return "100%";
        }
        else if (nPercent>0) {
        	return Integer.toString(nPercent)+"%";
        }
        else {
        	return "1%";
        }
    }
    
    public static final String multiply(String sPercent, String sLength){
        if (sLength.equals("0")) { return "0"; }
        float fPercent=getFloat(sPercent.substring(0,sPercent.length()-1),1);
        float fLength=getFloat(sLength.substring(0,sLength.length()-2),1);
        String sUnit=sLength.substring(sLength.length()-2);
        return Float.toString(fPercent*fLength/100)+sUnit;
    }

    public static final String add(String sLength1, String sLength2){
        if (sLength1.equals("0")) { return sLength2; }
        if (sLength2.equals("0")) { return sLength1; }
        float fLength1=getFloat(sLength1.substring(0,sLength1.length()-2),1);
        String sUnit1=sLength1.substring(sLength1.length()-2);
        float fLength2=getFloat(sLength2.substring(0,sLength2.length()-2),1);
        String sUnit2=sLength2.substring(sLength2.length()-2);
        // Use unit from sLength1:
        return Float.toString(fLength1+getUpi(sUnit1)/getUpi(sUnit2)*fLength2)+sUnit1;
    }

    public static final String sub(String sLength1, String sLength2){
        return add(sLength1,multiply("-100%",sLength2));
    }

    public static boolean isLessThan(String sThis, String sThat) {
        return sub(sThis,sThat).startsWith("-");
    }	
    
    public static String abs(String sLength) {
        return sLength.startsWith("-") ? sLength.substring(1) : sLength;
    }

    /*
     * Utility method to make sure the document name is stripped of any file
     * extensions before use.
     * (this is copied verbatim from PocketWord.java in xmerge)
     */
    public static final String trimDocumentName(String name,String extension) {
        String temp = name.toLowerCase();
        
        if (temp.endsWith(extension)) {
            // strip the extension
            int nlen = name.length();
            int endIndex = nlen - extension.length();
            name = name.substring(0,endIndex);
        }

        return name;
    }
	
    public static final String removeExtension(String sName) {
        int n = sName.lastIndexOf(".");
        if (n<0) { return sName; }
        return sName.substring(0,n);
    }
	
     /*
     * Utility method to retrieve a Node attribute or null
     */
    public static final String getAttribute (Node node, String attribute) {
        NamedNodeMap attrNodes = node.getAttributes();
        
        if (attrNodes != null) {
            Node attr = attrNodes.getNamedItem(attribute);
            if (attr != null) {
                return attr.getNodeValue();
            }
        }
        
        return null;
    }
	
    public static final boolean isElement(Node node) {
        return node.getNodeType()==Node.ELEMENT_NODE;
    }
	
    /* Utility method to determine if a Node is a specific Element
     */
    public static final boolean isElement(Node node, String sTagName) {
        return node.getNodeType()==Node.ELEMENT_NODE
            && node.getNodeName().equals(sTagName);
    }
	
    public static final boolean isText(Node node) {
        return node.getNodeType()==Node.TEXT_NODE;
    }
	
     /*
     * Utility method to retrieve an element attribute or null
     */
    public static final String getAttribute (Element node, String attribute) {
        if (node.hasAttribute(attribute)) { return node.getAttribute(attribute); }
        else { return null; }
    }

    /* utility method to get the first child with a given tagname */
    public static final Element getChildByTagName(Node node, String sTagName){
        if (node.hasChildNodes()){
            NodeList nl=node.getChildNodes();
            int nLen=nl.getLength();
            for (int i=0; i<nLen; i++){
                Node child = nl.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE &&
                    child.getNodeName().equals(sTagName)){
                    return (Element) child;
                }
            }
        }
        return null;
    }
	
    /* utility method to get the first <em>element</em> child of a node*/
    public static final Element getFirstChildElement(Node node) {
        Node child = node.getFirstChild();
        while (child!=null) {
            if (child.getNodeType()==Node.ELEMENT_NODE) {
                return (Element) child;
            }
            child = child.getNextSibling();
        }
        return null;
    }
	
    /* utility method that collects PCDATA content of an element */
    public static String getPCDATA(Node node) {
        StringBuffer buf = new StringBuffer();
        if (node.hasChildNodes()) {
            NodeList nl = node.getChildNodes();
            int nLen = nl.getLength();
            for (int i=0; i<nLen; i++) {
                if (nl.item(i).getNodeType()==Node.TEXT_NODE) {
                    buf.append(nl.item(i).getNodeValue());
                }
            }
        }
        return buf.toString();
    }
    
    // Utility method to return a sorted string array based on a set
    public static String[] sortStringSet(Set<String> theSet) {
    	String[] theArray = theSet.toArray(new String[theSet.size()]);
		Collator collator = Collator.getInstance();
		Arrays.sort(theArray, collator);
    	return theArray;
    }

    
    /* Utility method that url encodes a string */
    public static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s,"UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    /* Utility method that url decodes a string */
    public static String urlDecode(String s) {
        try {
            return URLDecoder.decode(s,"UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    /* utility method to make a file name valid for a href attribute
       (ie. replace spaces with %20 etc.)
     */
    public static String makeHref(String s) {
        try {
            java.net.URI uri = new java.net.URI(null, null, s, null);
            return uri.toString();	    
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return "error";
    }
    
    /* utility method to convert a *relative* URL to a file name
    (ie. replace %20 with spaces etc.)
     */
    public static String makeFileName(String sURL) {
    	try {
    		File file = new File(new java.net.URI("file:///"+sURL));
    		return file.getName();	    
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    	return "error";
    }
 
    
	
    /** <p>Read an <code>InputStream</code> into a <code>byte</code>array</p>
     *  @param is   the <code>InputStream</code> to read
     *  @return     a byte array with the contents read from the stream
     *  @throws     IOException  in case of any I/O errors.
     */
    public static byte[] inputStreamToByteArray(InputStream is) throws IOException {
        if (is==null) {
            throw new IOException ("No input stream to read");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int nLen = 0;
        byte buffer[] = new byte[BUFFERSIZE];
        while ((nLen = is.read(buffer)) > 0) {
            baos.write(buffer, 0, nLen);
        }
        return baos.toByteArray();
    }
	


}
