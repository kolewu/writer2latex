/************************************************************************
 *
 *  EmbeddedBinaryObject.java
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
 *  Version 1.4 (2012-03-26)
 *
 */

package writer2latex.office;

import writer2latex.util.SimpleZipReader;

/**
 * This class represents an embedded object with a binary representation in an ODF package document
 */
public class EmbeddedBinaryObject extends EmbeddedObject {
    
    /** The object's binary representation. */
    private byte[] objData = null;
        
    /**
     * Package private constructor for use when reading an object from a 
     * package ODF file
     *
     * @param   name    The name of the object.
     * @param   type    The MIME-type of the object.
     * @param   source  A <code>SimpleZipReader</code> containing the object
     */    
    protected EmbeddedBinaryObject(String sName, String sType, SimpleZipReader source) {
    	super(sName,sType);
    	objData = source.getEntry(sName);
    }
    
    /** Get the binary data for this object
     *
     * @return  A <code>byte</code> array containing the object's data.
     */
    public byte[] getBinaryData() {
        return objData;
    }    
    
}
