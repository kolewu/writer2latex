/************************************************************************
 *
 *  XeTeXI18n.java
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
 *  Version 1.2 (2010-12-15) 
 * 
 */

package writer2latex.latex.i18n;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Polyglossia {
	private static Map<String,String> languageMap;
	private static Map<String,String> variantMap;
	
	static {
		languageMap = new HashMap<String,String>();
		languageMap.put("am", "amharic");
		languageMap.put("ar", "arabic");
		languageMap.put("ast", "asturian");
		languageMap.put("bg", "bulgarian"); 
		languageMap.put("bn", "bengali"); 
		languageMap.put("br", "breton");
		languageMap.put("ca", "catalan"); 
		languageMap.put("cop", "coptic"); 
		languageMap.put("cs", "czech");
		languageMap.put("cy", "welsh");
		languageMap.put("da", "danish");
		languageMap.put("de", "german"); 
		languageMap.put("dsb", "lsorbian");
		languageMap.put("dv", "divehi");
		languageMap.put("el", "greek"); 
		languageMap.put("en", "english");
		languageMap.put("eo", "esperanto"); 
		languageMap.put("es", "spanish");
		languageMap.put("et", "estonian"); 
		languageMap.put("eu", "basque"); 
		languageMap.put("fa", "farsi"); 
		languageMap.put("fi", "finnish"); 
		languageMap.put("fr", "french"); 
		languageMap.put("ga", "irish");
		languageMap.put("gd", "scottish"); 
		languageMap.put("gl", "galician"); 
		languageMap.put("grc", "greek");
		languageMap.put("he", "hebrew");
		languageMap.put("hi", "hindi");
		languageMap.put("hr", "croatian");
		languageMap.put("hsb", "usorbian");
		languageMap.put("hu", "magyar"); 
		languageMap.put("hy", "armenian");
		languageMap.put("id", "bahasai"); // Bahasa Indonesia
		languageMap.put("ie", "interlingua");
		languageMap.put("is", "icelandic");
		languageMap.put("it", "italian");
		languageMap.put("la", "latin"); 
		languageMap.put("lo", "lao"); 
		languageMap.put("lt", "lithuanian"); 
		languageMap.put("lv", "latvian");
		languageMap.put("ml", "malayalam"); 
		languageMap.put("mr", "marathi"); 
		languageMap.put("ms", "bahasam"); // Bahasa Melayu
		languageMap.put("nb", "norsk");
		languageMap.put("nl", "dutch"); 
		languageMap.put("nn", "nynorsk");
		languageMap.put("oc", "occitan");
		languageMap.put("pl", "polish");
		languageMap.put("pt", "portuges"); 
		languageMap.put("pt-BR", "brazilian"); 
		languageMap.put("ro", "romanian"); 
		languageMap.put("ru", "russian"); 
		languageMap.put("sa", "sanskrit"); 
		languageMap.put("sk", "slovak"); 
		languageMap.put("sl", "slovenian");
		languageMap.put("sq", "albanian");
		languageMap.put("sr", "serbian"); 
		languageMap.put("sv", "swedish");
		languageMap.put("syr", "syriac");
		languageMap.put("ta", "tamil");
		languageMap.put("te", "telugu");
		languageMap.put("th", "thai");
		languageMap.put("tk", "turkmen");
		languageMap.put("tr", "turkish");
		languageMap.put("uk", "ukrainian");
		languageMap.put("ur", "urdu");
		languageMap.put("vi", "vietnamese");
		// TODO: Which language is samin?? One guess could be sami with the n for north?
		//languageMap.put("??", "samin");
		
		variantMap = new HashMap<String,String>();
		// English variants
		variantMap.put("en-US", "american");
		variantMap.put("en-GB", "british");
		variantMap.put("en-AU", "australian");
		variantMap.put("en-NZ", "newzealand");
		// Greek variants
		variantMap.put("el", "monotonic");
		variantMap.put("grc", "ancient"); // Supported in OOo since 3.2
	}
	
	private static String getEntry(Map<String,String> map, String sLocale, String sLang) {
		if (map.containsKey(sLocale)) {
			return map.get(sLocale);
		}
		else if (map.containsKey(sLang)) {
			return map.get(sLang);
		}
		return null;
	}
	
	// This ended the static part of Polyglossia
	
	private Set<String> languages = new HashSet<String>();
	private List<String> declarations = new ArrayList<String>();
	private Map<String,String[]> commands = new HashMap<String,String[]>();
	
	/** <p>Get the declarations for the applied languages, in the form</p>
	 *  <p><code>\\usepackage{polyglossia}</code></p>
	 *  <p><code>\\setdefaultlanguage{language1}</code></p>
	 *  <p><code>\\setotherlanguage{language2}</code></p>
	 *  <p><code>\\setotherlanguage{language3}</code></p>
	 *  <p><code>...</code></p>
	 * 
	 * @return the declarations as a string array
	 */
	public String[] getDeclarations() {
		return declarations.toArray(new String[declarations.size()]);
	}
	
	/** <p>Add the given locale to the list of applied locales and return definitions for applying the
	 * language to a text portion:</p>
	 * <ul>
	 * <li>A command of the forn <code>\textlanguage[variant=languagevariant]</code></li>
	 * <li>An environment in the form
	 * <code>\begin{language}[variant=languagevariant]</code>...<code>\end{language}</code></li>
	 * </ul>
	 * <p>The first applied language is the default language</p>
	 * 
	 * @param sLang The language
	 * @param sCountry The country (may be null)
	 * @return a string array containing definitions to apply the language: Entry 0 contains a command
	 * and Entry 1 and 2 contains an environment
	 */
	public String[] applyLanguage(String sLang, String sCountry) {
		String sLocale = sCountry!=null ? sLang+"-"+sCountry : sLang;
		if (commands.containsKey(sLocale)) {
			return commands.get(sLocale);
		}
		else {
			// Get the Polyglossia language and variant
			String sPolyLang = getEntry(languageMap,sLocale,sLang);
			if (sPolyLang!=null) {
				String sVariant = getEntry(variantMap,sLocale,sLang);
				if (sVariant!=null) {
					sVariant = "[variant="+sVariant+"]";
				}
				else {
					sVariant = "";
				}
				
				if (languages.size()==0) {
					// First language, load Polyglossia and make the language default
					declarations.add("\\usepackage{polyglossia}");
					declarations.add("\\setdefaultlanguage"+sVariant+"{"+sPolyLang+"}");
					languages.add(sPolyLang);
					sVariant = ""; // Do not apply variant directly
				}
				else if (!languages.contains(sPolyLang)) {
					// New language, add to declarations
					declarations.add("\\setotherlanguage"+sVariant+"{"+sPolyLang+"}");
					languages.add(sPolyLang);
					sVariant = ""; // Do not apply variant directly
				}
				
				String[] sCommand = new String[3];
				sCommand[0] = "\\text"+sPolyLang+sVariant;
				if ("arabic".equals(sPolyLang)) { sPolyLang="Arabic"; }
				sCommand[1] = "\\begin{"+sPolyLang+"}"+sVariant;
				sCommand[2] = "\\end{"+sPolyLang+"}";
				commands.put(sLocale, sCommand);
				return sCommand;
			}
			else {
				// Unknown language
				String[] sCommand = new String[3];
				sCommand[0] = "";
				sCommand[1] = "";
				sCommand[2] = "";
				commands.put(sLocale, sCommand);
				return sCommand;
			}
		}
	}
}
