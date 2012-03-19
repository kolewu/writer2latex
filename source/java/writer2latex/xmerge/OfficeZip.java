/************************************************************************
 *
 *  The Contents of this file are made available subject to the terms of
 *
 *         - GNU Lesser General Public License Version 2.1
 *
 *  Sun Microsystems Inc., October, 2000
 *
 *  GNU Lesser General Public License Version 2.1
 *  =============================================
 *  Copyright 2000 by Sun Microsystems, Inc.
 *  901 San Antonio Road, Palo Alto, CA 94303, USA
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
 *  The Initial Developer of the Original Code is: Sun Microsystems, Inc.
 *
 *  Copyright: 2000 by Sun Microsystems, Inc.
 *
 *  All Rights Reserved.
 *
 *  Contributor(s): _______________________________________
 *
 *
 ************************************************************************/

// This version is adapted for Writer2LaTeX
// Version 1.4 (2012-03-19)
 
package writer2latex.xmerge;

import java.util.List;
import java.util.LinkedList;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

/**
 *  Class used by {@link
 *  org.openoffice.xmerge.converter.OfficeDocument
 *  OfficeDocument} to handle reading
 *  from a ZIP file, as well as storing ZIP entries.
 *
 *  @author   Herbie Ong
 */
class OfficeZip {

    /** File name of the XML file in a zipped document. */
    private final static String CONTENTXML = "content.xml";

    private final static String STYLEXML = "styles.xml";
    private final static String METAXML = "meta.xml";
    private final static String SETTINGSXML = "settings.xml";
    private final static String MANIFESTXML = "META-INF/manifest.xml";

    private final static int BUFFERSIZE = 1024;

    private List<Entry> entryList = null;

    private int contentIndex = -1;
    private int styleIndex = -1;
    private int metaIndex = -1;
    private int settingsIndex = -1;
    private int manifestIndex = -1;

    /** Default constructor. */
    OfficeZip() {

        entryList = new LinkedList<Entry>();
    }


    /**
     *  <p>Read each zip entry in the <code>InputStream</code> object
     *  and store in entryList both the <code>ZipEntry</code> object
     *  as well as the bits of each entry.  Call this method before
     *  calling the <code>getContentXMLBytes</code> method or the
     *  <code>getStyleXMLBytes</code> method.</p>
     *
     *  <p>Keep track of the CONTENTXML and STYLEXML using
     *  contentIndex and styleIndex, respectively.</p>
     *
     *  @param  is  <code>InputStream</code> object to read.
     *
     *  @throws  IOException  If any I/O error occurs.
     */
    void read(InputStream is) throws IOException {

        ZipInputStream zis = new ZipInputStream(is);
        ZipEntry ze = null;
        int i = -1;

        while ((ze = zis.getNextEntry()) != null) {

            String name = ze.getName();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            int len = 0;
            byte buffer[] = new byte[BUFFERSIZE];

            while ((len = zis.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }

            byte bytes[] = baos.toByteArray();
            Entry entry = new Entry(ze,bytes);

            entryList.add(entry);

            i++;

            if (name.equalsIgnoreCase(CONTENTXML)) {
                contentIndex = i;
            }
            else if (name.equalsIgnoreCase(STYLEXML)) {
                styleIndex = i;
            }
            else if (name.equalsIgnoreCase(METAXML)) {
                metaIndex = i;
            }
            else if (name.equalsIgnoreCase(SETTINGSXML)) {
                settingsIndex = i;
            }
            else if (name.equalsIgnoreCase(MANIFESTXML)) {
                manifestIndex = i;
            }
	    
        }

        zis.close();
    }


    /**
     *  This method returns the CONTENTXML file in a
     *  <code>byte</code> array.  It returns null if there is no
     *  CONTENTXML in this zip file.
     *
     *  @return  CONTENTXML in a <code>byte</code> array.
     */
    byte[] getContentXMLBytes() {

        return getEntryBytes(contentIndex);
    }


    /**
     *  This method returns the STYLEXML file in a
     *  <code>byte</code> array.  It returns null if there is
     *  no STYLEXML in this zip file.
     *
     *  @return  STYLEXML in a <code>byte</code> array.
     */
    byte[] getStyleXMLBytes() {

        return getEntryBytes(styleIndex);
    }

     /**
     *  This method returns the METAXML file in a
     *  <code>byte</code> array.  It returns null if there is
     *  no METAXML in this zip file.
     *
     *  @return  METAXML in a <code>byte</code> array.
     */
    byte[] getMetaXMLBytes() {
        return getEntryBytes(metaIndex);
    }

      /**
     *  This method returns the SETTINGSXML file in a
     *  <code>byte</code> array.  It returns null if there is
     *  no SETTINGSXML in this zip file.
     *
     *  @return  SETTINGSXML in a <code>byte</code> array.
     */
    byte[] getSettingsXMLBytes() {
        return getEntryBytes(settingsIndex);
    }
    
    /**
     * This method returns the MANIFESTXML file in a <code>byte</code> array.
     * It returns null if there is no MANIFESTXML in this zip file.
     *
     * @return  MANIFESTXML in a <code>byte</code> array.
     */
    byte[] getManifestXMLBytes() {
        return getEntryBytes(manifestIndex);
    }

    /**
     * This method returns the bytes corresponding to the entry named in the
     * parameter.  
     *
     * @param   name    The name of the entry in the Zip file to retrieve.
     *
     * @return  The data for the named entry in a <code>byte</code> array or
     *          <code>null</code> if no entry is found.
     */
    byte[] getNamedBytes(String name) {
        
        // The list is not sorted, and sorting it for a binary search would
        // invalidate the indices stored for the main files.
        
        // Could improve performance by caching the name and index when 
        // iterating through the ZipFile in read().
        for (int i = 0; i < entryList.size(); i++) {
            Entry e = entryList.get(i);
            
            if (e.zipEntry.getName().equals(name)) {
                return getEntryBytes(i);
            }
        }
        
        return null;
    }
    
    
    /**
     *  Used by the <code>getContentXMLBytes</code> method and the
     *  <code>getStyleXMLBytes</code> method to return the
     *  <code>byte</code> array from the corresponding
     *  <code>entry</code> in the <code>entryList</code>.
     *
     *  @param  index  Index of <code>Entry</code> object in
     *                 <code>entryList</code>.
     *
     *  @return  <code>byte</code> array associated in that 
     *           <code>Entry</code> object or null, if there is
     *           not such <code>Entry</code>.
     */
    private byte[] getEntryBytes(int index) {
	
        byte[] bytes = null;

        if (index > -1) {
            Entry entry = entryList.get(index);
            bytes = entry.bytes;
        }
        return bytes;
    }

    /**
     *  This inner class is used as a data structure for holding
     *  a <code>ZipEntry</code> info and its corresponding bytes.
     *  These are stored in entryList.
     */
    private class Entry {

        ZipEntry zipEntry = null;
        byte bytes[] = null;

        Entry(ZipEntry zipEntry, byte bytes[]) {
            this.zipEntry = zipEntry;
            this.bytes = bytes;
        }
    }
}

