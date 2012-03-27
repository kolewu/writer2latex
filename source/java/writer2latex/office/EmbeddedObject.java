/************************************************************************
 *
 *  EmbeddedObject.java
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
 *  Version 1.4 (2012-03-27)
 *
 */

package writer2latex.office;

/** This class represents and embedded object within an ODF package document
 */
public abstract class EmbeddedObject {
    private String sName;
    private String sType;
    
    /** Construct a new embedded object
     *
     * @param   sName    The name of the object.
     * @param   sType    The MIME-type of the object.
     */
    protected EmbeddedObject(String name, String type) {
        sName = name;
        sType = type;
    }
    
    /** Get the name of the embedded object represented by this instance.
     *  The name refers to the manifest.xml file
     *
     * @return  The name of the object.
     */
    public final String getName() {
        return sName;
    }
    
    /** Get the MIME type of the embedded object represented by this instance.
     *  The MIME type refers to the manifest.xml file
     */
    public final String getType() {
        return sType;
    }
    
}