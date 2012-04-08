Writer2LaTeX source version 1.2
===============================

Writer2LaTeX is (c) 2002-2012 by Henrik Just.
The source is available under the terms and conditions of the
GNU LESSER GENERAL PUBLIC LICENSE, version 2.1.
Please see the file COPYING.TXT for details.


Overview
--------

The source of Writer2LaTeX consists of three major parts:

* A general purpose java library for converting OpenDocument files into LaTeX,
  BibTeX, xhtml, xhtml+MathML and EPUB
  This is to be found in the packages writer2latex.* and should only be used
  through the provided api writer2latex.api.*
* A command line utility writer2latex.Application
* A collection of components for OpenOffice.org
  These are to be found in the packages org.openoffice.da.comp.*
  
Currently parts of the source for Writer2LaTeX are somewhat messy and
undocumented. This situation is improving from time to time :-)


Third-party software
--------------------

From OpenOffice.org:

Writer2LaTeX includes some classes from the OpenOffice.org project:
writer2latex.xmerge.* contains some classes which are part of the xmerge
project within OOo (some of the classes are slightly modified)
See copyright notices within the source files


From JSON.org:

The classes org.json.* are copyright (c) 2002 JSON.org and is used subject to the following notice

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
(the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

The Software shall be used for Good, not Evil.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. 

From iharder.sourceforge.net:

The class writer2latex.util.Base64 is Robert Harders public domain Base64 class


Building Writer2LaTeX
---------------------

Writer2LaTeX uses Ant version 1.5 or later (http://ant.apache.org) to build.


Some java libraries from OOo are needed to build the filter part of Writer2LaTeX,
these are jurt.jar, unoil.jar, ridl.jar and juh.jar.

To make these files available for the compiler, edit the file build.xml
as follows:

The lines
	<property name="OFFICE_CLASSES" location="/usr/share/java/openoffice" />
	<property name="URE_CLASSES" location="/usr/share/java/openoffice" />
should be modified to the directories where your OOo installation keeps these files

To build, open a command shell, navigate to the source directory and type

ant oxt

(this assumes, that ant is in your path; otherwise specifify the full path.)

In addition to oxt, the build file supports the following targets:
    all
        Build nearly everything
    compile
        Compile all file except the tests.        
    jar
        Create the standalone jar file.
    javadoc
        Create the javadoc documentation in target/javadoc.
    distro
	    Create distribution files 
    clean


Henrik Just, March 2012


Thanks to Michael Niedermair for writing the original ant build file
