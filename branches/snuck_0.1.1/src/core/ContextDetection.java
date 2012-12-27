/*
   snuck - reflection context detector
   --------------------------------
   
   Author: Mauro Gentile <gentile.mauro.mg@gmail.com>

   Copyright 2012 Mauro Gentile

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */

package core;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

public class ContextDetection {
	// possibly useful in the case of injection in an HTML attribute
	private static String injected_attribute = null;

	@SuppressWarnings("unchecked")
	public static List<WebElement> getParents(String injection, boolean IE_enabled) {
		List<WebElement> parents = null;
		
		// Expected reflection context: <tag>UNTRUSTED DATA</tag>

		/*
		 * IE does not accept the XPath query we use in Firefox and Chrome, so we 
		 * need to employ a JS function. Nevertheless the following one is
		 * not completely correct as it returns the first occurrence of the injection instead of 
		 * every occurrence in a list. 
		 * - improvements are required here
		 */
		if (IE_enabled) {
			String js = "var res = []; var all = document.getElementsByTagName(\"*\");" +
					"for (var i=0, max=all.length; i < max; i++) {" +
					"	if  ((all[i].innerText.indexOf(\"" + injection + "\") != -1 || all[i].innerHTML.indexOf(\"" + injection + "\") != -1 ) && all[i].innerHTML.indexOf(\"<\") == -1)" +
					"   	res[0] = all[i];" +
					"}" +
					"return res;";
			
			parents = (List<WebElement>) (((JavascriptExecutor)Starter.getDriver()).executeScript(js));

		} else {
			parents = Starter.getDriver().findElements(By.xpath("//*[text()[contains(., '" + injection + "')]]"));   	        
		}
		
		return parents;
	}

	@SuppressWarnings("unchecked")
	public static List<WebElement> getParentsWithInjectedAttribute(String injection, boolean IE_enabled) {
		List<WebElement> elements = null;
		
		// Expected reflection context = <element attribute="UNTRUSTED_DATA">
        if (IE_enabled) {        
			String js = "var res = [];  var all = document.getElementsByTagName(\"*\"); var z = 0;"+
						"for (var i=0, max=all.length; i < max; i++)" +
						"	for (var j = 0, attrs = all[i].attributes, l = attrs.length; j < l; j++)" +
						"     		if  (attrs[j].value.indexOf(\"" + injection + "\") != -1){" +
						"      	 		res[z] = all[i];" +
						"				z++;" +
						"			}" +
						"" +
						"return res;";

			elements = (List<WebElement>) (((JavascriptExecutor)Starter.getDriver()).executeScript(js));

        } else {
			elements = Starter.getDriver().findElements(By.xpath("//attribute::*[contains(., '" + injection + "')]/.."));
		}
        
        return elements;
	}

	public static boolean isInHtmlComment(String injection) {
		// Expected reflection context = <!-- UNTRUSTED_DATA -->
        Pattern pattern_html_comment = Pattern.compile("<!--.*" + injection + ".*-->");
    	Matcher matcher = pattern_html_comment.matcher(Starter.getDriver().getPageSource());
    	return matcher.find();
	}

	@SuppressWarnings("unchecked")
	public static String getAllAttributes(WebElement parent, boolean IE_enabled) {
        List<String> ListAttributes = null;
        String attrs = null;
        
	    if (!IE_enabled) {
	    	ListAttributes = (List<String>) ((JavascriptExecutor)Starter.getDriver()).executeScript("" +
	    			"var el = arguments[0]; var arr = []; " +
	        		"for (var i=0, attrs=el.attributes, l=attrs.length; i<l; i++) {" +
	        			"arr.push(attrs[i].name);" +
	        		"} " +
	        		"return arr;"
	        		, parent);
        	        
	        for (String attribute : ListAttributes){
	        		try {
	        			attrs += attribute + "=\"" + parent.getAttribute(attribute) + "\" ";
	        		} catch (WebDriverException e) {
	        			break;
	        		}
	        }
        }
	    
		return (attrs == null) ? "" : attrs;
	}

	@SuppressWarnings("unchecked")
	public static String getAttributesFromInjected(WebElement element, String injection, boolean IE_enabled) {
		List<String> attributes = null;
		String attrs = "";
		
		// get all the attributes of the injected element
        if(IE_enabled) {
        	attributes = (List<String>) ((JavascriptExecutor)Starter.getDriver()).executeScript("" +
	        		"var el = arguments[0]; var arr = []; " +
	        		"for (var i=0, attrs=el.attributes, l=attrs.length; i<l; i++) {" +
	        		"	if (attrs[i].value.indexOf(\"null\") == -1)" +
	        		"   	if (attrs[i].value.length != 0)" +
	        		"			arr.push(attrs[i].name);" +
	        		"} " +
	        		"return arr;"
	        		, element);
        } else {
	        attributes = (List<String>) ((JavascriptExecutor)Starter.getDriver()).executeScript("" +
	        		"var el = arguments[0]; var arr = []; " +
	        		"for (var i=0, attrs=el.attributes, l=attrs.length; i<l; i++) {" +
	        			"arr.push(attrs[i].name);" +
	        		"} " +
	        		"return arr;"
	        		, element);
        }
	        
        for (String attribute : attributes){
        	if (element.getAttribute(attribute) != null && element.getAttribute(attribute).contains(injection))
        		injected_attribute = attribute;
        	else 
        		attrs += attribute + "=\"" + element.getAttribute(attribute) + "\" ";
        }
                
        return attrs;
	}
	
	public static String getInjectedAttribute(){
		return injected_attribute;
	}
}
