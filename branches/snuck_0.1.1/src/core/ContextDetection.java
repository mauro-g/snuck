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
import org.openqa.selenium.WebElement;

public class ContextDetection {
	// possibly useful in the case of injection in an HTML attribute
	private static String injected_attribute = null;

	public static List<WebElement> getParents(String injection) {   
		// This XPath queries returns successfully in Firefox, Chrome, and IE 10 (IE <= 9 does not correctly support it) 
		return Starter.getDriver().findElements(By.xpath("//*[text()[contains(., '" + injection + "')]]"));
	}

	public static List<WebElement> getParentsWithInjectedAttribute(String injection) {       
		// This XPath queries returns successfully in Firefox, Chrome, and IE 10 (IE <= 9 does not correctly support it) 
        return Starter.getDriver().findElements(By.xpath("//attribute::*[contains(., '" + injection + "')]/.."));
	}

	public static boolean isInHtmlComment(String injection) {
		// Expected reflection context = <!-- UNTRUSTED_DATA -->
        Pattern pattern_html_comment = Pattern.compile("<!--.*" + injection + ".*-->");
    	Matcher matcher = pattern_html_comment.matcher(Starter.getDriver().getPageSource());
    	return matcher.find();
	}

	private static String getAttributesJSFunction = "" +
			"var el = arguments[0]; var arr = []; " +
    		"for (var i=0, attrs=el.attributes, l=attrs.length; i<l; i++) {" +
    			"arr.push(attrs[i].name);" +
    		"} " +
    		"return arr;";
    		
	@SuppressWarnings("unchecked")
	public static String getAllAttributes(WebElement parent) {
        List<String> ListAttributes = (List<String>) ((JavascriptExecutor)Starter.getDriver()).executeScript(getAttributesJSFunction, parent);
        String attrs = "";
                
	    for (String attribute : ListAttributes)
	    	attrs += attribute + "=\"" + parent.getAttribute(attribute) + "\" ";

	    return attrs;
	}

	@SuppressWarnings("unchecked")
	public static String getAttributesFromInjected(WebElement element, String injection) {
		// get all the attributes of the injected element
		List<String> attributes = (List<String>) ((JavascriptExecutor)Starter.getDriver()).executeScript(getAttributesJSFunction, element);
		String attrs = "";    
	        
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
