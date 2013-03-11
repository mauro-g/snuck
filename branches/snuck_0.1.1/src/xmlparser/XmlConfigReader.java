/*
   snuck - XML configuration files parser
   --------------------------------
   
   Author: Mauro Gentile <gentile.mauro.mg@gmail.com>

   Copyright 2012-2013 Mauro Gentile

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

package xmlparser;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.openqa.selenium.By;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import commandline.Debug;
import commandline.HaltHandler;
import commandline.HttpRequestParser;
import core.Inject;
import core.Server;
import core.Starter;

/**
 * Here follows the two possible configuration files
 * 
 * 1. XSS via GET parameter
<root>
	<get>
		<parameters>
			<targeturl>http://targeturl.ex/</targeturl>
			<reflectionurl>http://targeturl.ex/xssedpage.html</reflectionurl>
			<paramtoinject>x</paramtoinject>
			<parameter>id=1</parameter>
			<parameter>year=2012</parameter>
		</parameters>
	</get>
</root>
 * 
 * 2. XSS via form (to populate)
<root>
	<post>
		<parameters>
			<parameter>
				<name>username</name>
				<value>myname</value>
			</parameter>
			<commands>
				<command>
					<name>open</name>
					<target>http://target.foo</target>
					<value></value>
				</command>
				<command>
					<name>type</name>
					<target>name=author</target>
					<value>${username}</value>
				</command>
				<command>
					<name>click</name>
					<target>name=submit</target>
					<value></value>
				</command>
			</commands>
		</parameters>
	</post>
</root>
 *
 */

public class XmlConfigReader {	
		
	private DocumentBuilderFactory factory;
	private DocumentBuilder builder;
	private Document document;
	private Element root;
	private String xmlFilename;
	
	private Map<String, String> Parameters = new HashMap<String, String>();
	
	// method => 1 stands for reflected, 2 stands for stored, 0 stands for error
	private int method = 0;
	
	// useful in the case arguments such as -r or -data are supplied
	private static String target;
	
	public int getMethod() {
		return this.method;
	}
	
	public XmlConfigReader(String xmlConfigFile_Name) {
		factory = DocumentBuilderFactory.newInstance();
		xmlFilename = xmlConfigFile_Name;
		
		try {
			builder = factory.newDocumentBuilder();
			document = builder.parse(new File(xmlConfigFile_Name));
		} catch (ParserConfigurationException e) {
			Debug.printError("ERROR: unable to parse the XML configuration file.\n" + e.getMessage());
			HaltHandler.quit_nok();
		} catch (IOException e) {
			Debug.printError("ERROR: unable to open the XML configuration file.\n" + e.getMessage());
			HaltHandler.quit_nok();
		} catch (SAXException e) {
			Debug.printError("ERROR: unable to parse the XML configuration file.\n" + e.getMessage());
			HaltHandler.quit_nok();
		}
		
    	root = document.getDocumentElement();
    	
    	checkType();
	}

	private void checkType() {
    	NodeList list1 = root.getElementsByTagName("get");
    	NodeList list2 = root.getElementsByTagName("post");
    	
    	if (list1.getLength() == 0 && list2.getLength() == 0){
    		Debug.printError("ERROR: invalid use case ( " + xmlFilename + " )");
			HaltHandler.quit_nok();
    	}
    		
    	if (list1.getLength() != 0) this.method = 1;
    	else if (list2.getLength() != 0){
    		this.method = 2;
    		setParameters();
    	}
	}
	
	private String getCompleteTargetURL(){
		String url = GetTargetUrl() + "?";
		
    	NodeList list = root.getElementsByTagName("parameter");
    	
		for (int i = 0; i < list.getLength(); i++){
			url += "&" + ((Node)(list.item( i ))).getTextContent();
    	}
		
		return url;
	}
	
	public String getTargetDomain(){
		if (method == 1)
			return GetTargetUrl();
		else {
			if (target != null) {
				try {
					URL target_domain = new URL(target);
					
					return target_domain.getProtocol() + "://" + target_domain.getHost() + (target_domain.getPort() != -1 ? ":" + target_domain.getPort() : "");
				} catch (MalformedURLException e) {
					Debug.printError("\nERROR: unable to identify the target domain");
					HaltHandler.quit_nok();
				}
			}
			
			NodeList commands = root.getElementsByTagName("name");
			
			for (int i = 0; i < commands.getLength(); i++){
				if (commands.item(i).getTextContent().equals("open") && 
					commands.item(i).getParentNode() != null && 
					commands.item(i).getParentNode().getNodeName().equals("command")){

					String url = root.getElementsByTagName("target").item(i - Parameters.size()).getTextContent();
					try {
						URL target_domain = new URL(url);
						
						return target_domain.getProtocol() + "://" + target_domain.getHost() + (target_domain.getPort() != -1 ? ":" + target_domain.getPort() : "");
					} catch (MalformedURLException e) {
						Debug.printError("\nERROR: unable to identify the target domain");
						HaltHandler.quit_nok();
					}
				}
			}
		}
		
		return null;
	}
	
	
	private String GetTargetUrl(){	
		String targetURL = getTextContentByName("targeturl");
		
		if (targetURL == null){
			Debug.printError("ERROR: missing <targeturl> ( " + xmlFilename + " )");
			HaltHandler.quit_nok();
		}
		
		return targetURL;
	}
	
	private String GetReflectionUrl(){	
		return getTextContentByName("reflectionurl");
	}
	
	boolean empty_paramtoinject_flag = false;
	private String GetParamToInject(){
		String param = getTextContentByName("paramtoinject");
		
		if (param == null){
			Debug.printError("ERROR: missing <paramtoinject> ( " + xmlFilename + " ).");
			HaltHandler.quit_nok();
		}
		
		if (param.equals("") && !empty_paramtoinject_flag){
			String t_url = GetTargetUrl();
			Debug.print("INFO: <paramtoinject> is empty ( " + xmlFilename + " ). " +
						"Injecting in the form of: " + ( t_url.endsWith("/") ? t_url : t_url + "/" ) + "injection");
			empty_paramtoinject_flag = true;
		}
		
		return param;
	}
	
	private String getTextContentByName(String tagName){
		if (root.getElementsByTagName(tagName).getLength() != 0)
    		return root.getElementsByTagName(tagName).item(0).getTextContent();
		else return null;
	}
		
	private void setParameters(){
		NodeList list = root.getElementsByTagName("parameter");

		for (int i = 0; i < list.getLength(); i++){
			NodeList subChild = ((Node)list.item(i)).getChildNodes();
			
			String name = subChild.item(1).getTextContent();
			String value = subChild.item(3).getTextContent();

			Parameters.put(name, value);
    	}
	}
	
	private void NavigateTo(WebDriver driver, String url) {
		NavigateTo(driver, url, 0);
	}
	
	private void NavigateTo(WebDriver driver, String url, int recursion_count) {
		if (recursion_count >= 2) {
			Debug.printError("\nERROR: unable to continue the injection process");
			HaltHandler.quit_nok();
		}
		
		try {		
			driver.get(url);
		} catch (UnhandledAlertException e){

			Inject.getAlertsWithTimeout(null, false);
			// the driver has probably changed!
			NavigateTo(Starter.getDriver(), url, recursion_count++);
	    // IE can trigger a WebDriverException instead of the UnhandledAlertException upon blocked by alert dialog windows
	    } catch (WebDriverException e){
			Inject.getAlertsWithTimeout(null, false);
			// the driver has probably changed!
			NavigateTo(Starter.getDriver(), url, recursion_count++);
	    }
    }
	
	public void commonInject(final WebDriver driver, String injection, int delay){
		if (delay != 0){
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				Debug.printError("Error: " + e.getMessage());
				HaltHandler.quit_nok();
			}
		}
		
		if (method == 1){
			String target_url = "";
			
			try {
				if (GetParamToInject().equals("")){
					target_url = ( GetTargetUrl().endsWith("/") ? GetTargetUrl() : GetTargetUrl() + "/" ) + URLEncoder.encode(injection, "UTF-8");
				} else 
					target_url = getCompleteTargetURL() + "&" + GetParamToInject() + "=" + URLEncoder.encode(injection, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				Debug.printError("\nERROR: unable to encode the target URL");
				HaltHandler.quit_nok();
			}

			// handle DoS vectors
			try {				
				/*
				 * Here follows a simple workaround to make it work in Google Chrome > 21.
				 * It seems that starting from Chrome 22, the following code will block the
				 * webdriver navigation:
				 * 
				 * driver.get(targetURL);
				 * driver.switchTo().alert();
				 * => an exception is launched!  
				 *
				 * targetURL is a web page containing an alert dialog window, such as
				 * <script>alert(1)</script>
				 * 
				 * Since it works in the case the targeURL is reached from a link, then
				 * we can force the browser to produce an injection starting from a link in a data URI web page.
				 * 
				 * Note that the injection is obviously url-encoded two times! 
				 */
				if (Starter.getCurrentBrowser() != null && Starter.getCurrentBrowser().contains("Chrome") && ( GetReflectionUrl() == null || GetReflectionUrl().equals("") ) ){
					try {
						NavigateTo(driver, "data:text/html,<a href='" + URLEncoder.encode(target_url, "UTF-8") + "'>go</click>");
					} catch (UnsupportedEncodingException e) {
						Debug.printError("\nERROR: unable to encode the target URL");
						HaltHandler.quit_nok();
					}
			   		driver.findElements(By.xpath("//a")).get(0).click();
				} else 		
					NavigateTo(driver, target_url);
				
			} catch (UnhandledAlertException e) {
				Starter.refreshDriver();
				Debug.printError("\nERROR: the last injection resulted in a denial of service! Starting a new driver to continue the injection process");
			} catch (WebDriverException e) {
				Starter.refreshDriver();
				Debug.printError("\nINFO: an alert dialog window was blocking the injection process, starting a new driver...");
			}
						
			// <reflectionurl> is set
			if (GetReflectionUrl() != null && !GetReflectionUrl().equals("")){
				if (Starter.getCurrentBrowser() != null && Starter.getCurrentBrowser().contains("Chrome")){
					try {
						NavigateTo(driver, "data:text/html,<a href='" + URLEncoder.encode(target_url, "UTF-8") + "'>go</click>");
					} catch (UnsupportedEncodingException e) {
						Debug.printError("\nERROR: unable to encode the target URL");
						HaltHandler.quit_nok();
					}
					driver.findElements(By.xpath("//a")).get(0).click();
				} else 
					NavigateTo(driver, GetReflectionUrl());
			}
			
		} else if (method == 2){
			NodeList commands = root.getElementsByTagName("name");
			NodeList targets = root.getElementsByTagName("target");
			NodeList values = root.getElementsByTagName("value");

			if (commands.getLength() == 0 || targets.getLength() == 0 || values.getLength() == 0){
				Debug.printError("ERROR: malformed <command> tags ( " + xmlFilename + " )");
				HaltHandler.quit_nok();
			} else if (commands.getLength() - Parameters.size() != targets.getLength() || commands.getLength() != values.getLength()){
				Debug.printError("ERROR: malformed <command> tags ( " + xmlFilename + " )");
				HaltHandler.quit_nok();
			}
			
			for (int i = Parameters.size(); i < commands.getLength(); i++){

				String name = commands.item(i).getTextContent();
				String target = root.getElementsByTagName("target").item(i - Parameters.size()).getTextContent();				
				String value = root.getElementsByTagName("value").item(i).getTextContent();
				
				WebElement ele = null;

				try {
					if (target.startsWith("id="))
						ele = driver.findElement(By.id(target.replace("id=", "")));
					else if (target.startsWith("xpath=")) 
						ele = driver.findElement(By.xpath(target.replace("xpath=", "")));
					else if (target.startsWith("name="))
						ele = driver.findElement(By.name(target.replace("name=", "")));
				} catch (Exception e){
					Debug.printError("\nERROR: unable select the element [" + target + "]");
					Starter.brokenPage();
				}
				
					if (name.equals("open")){		
						
						if  (value.equals("noforce")){
							if (!stripFragmentFromCurrentURL(driver).equals(target))
								NavigateTo(driver, target);
						} else {
								NavigateTo(driver, target);
						}						
						
					} else if (name.equals("type")){
						try {
							if (value.startsWith("${") && value.endsWith("}")){
								
								if (value.substring(2, value.length()-1).equals("RANDOM"))
									ele.sendKeys("" + (new Random()).nextInt(100000));
								else if (value.substring(2, value.length()-1).equals("RANDOM_EMAIL"))
									ele.sendKeys((new Random()).nextInt(100000) + "@" + (new Random()).nextInt(100000) + ".org");
								else if (value.substring(2, value.length()-1).equals("INJECTION"))
									ele.sendKeys((injection != null) ? injection : "");
								else 
									ele.sendKeys(Parameters.get(value.toLowerCase().replace("$", "").replace("{", "").replace("}", "")));
								
							} else if (value.equals("")){
								ele.clear();
							} else 
								ele.sendKeys(value);
						} catch (NullPointerException e){
							Debug.printError("\nERROR: cannot find the element defined through [" + target + "]");
							Starter.brokenPage();
						}
					
					} else if (name.equals("submit")){		
						try {
							ele.submit();
						} catch (NullPointerException e){
							Debug.printError("\nERROR: cannot find the element defined through [" + target + "]");
							Starter.brokenPage();
						}
					} else if (name.equals("deleteCookies")) {
						driver.manage().deleteAllCookies();
					} else if (name.equals("click")) {
						try {
							ele.click();
						} catch (NullPointerException e){
							Debug.printError("\nERROR: cannot find the element defined through [" + target + "]");
							Starter.brokenPage();
						}
					}			
				}
		}
	}
	
	private static String stripFragmentFromCurrentURL(WebDriver current_driver){
		String current_url = current_driver.getCurrentUrl();
		int t = current_url.indexOf("#");
		
		if (t != -1)
			return  current_url.substring(0, t);
		else 
			return current_url;
	}
	
	public static String generateReflectedConfigFile(String targetURL, String targetParam) {
		URL tmp = null;
		LinkedList<String> parameters = null;
		String params = "";
		
		try {
			tmp = new URL(targetURL);
		} catch (MalformedURLException e) {
			Debug.printError("ERROR: unable to parse the supplied URL (" + targetURL + ")");
            HaltHandler.quit_nok();
		}
		
		if (tmp.getQuery() != null){
			parameters = getQueryMap(tmp.getQuery());
				
			for (String p : parameters) {  
				params += "			<parameter>" +
							p.split("=")[0] + ( p.split("=")[1].equals("") ? "" : "=" + p.split("=")[1] );
				params += "</parameter>\n";
			}  
		}
		
		return "<root>\n" +
				"	<get>\n" +
				"		<parameters>\n" +
				"			<targeturl>" + (targetURL.split("\\?") != null ? targetURL.split("\\?")[0] : targetURL) + "</targeturl>\n" +
				"			<paramtoinject>" + targetParam + "</paramtoinject>\n" +
							params + 
				"		</parameters>\n" +
				"	</get>\n" +
				"</root>\n";
	}
	
	public static LinkedList<String> getQueryMap(String query) {  
		if (query == null)
			return new LinkedList<String>();
					
	    String[] params = query.split("&");  
	    LinkedList<String> parameters = new LinkedList<String>();

	    for (String param : params) {  
	        String name = param.split("=")[0];  
	        String value = null;
	        if (param.split("=").length == 2)
	        	value = param.split("=")[1];  
	        
	        if (!name.equals(""))
	        	parameters.add(name + "=" + ( (value == null) ? "" : value ) );  
	    }  
	    return parameters;  
	}

	public static String generateConfigFileFromHTTPRequest(HttpRequestParser http_request, String parameter_to_inject) {	
		if (http_request.getMethod().equals("POST")){
			target = http_request.getTargetURL();
			String form = "Injecting... <form action=\"" + http_request.getTargetURL() + "\" method=\"POST\">";
			String element_to_click = "submit";
			
			for (String h : http_request.getParameters().keySet()){
				if (h.equals(parameter_to_inject))
					form += "<input id=\"inject\" name=\"" + h + "\" value=\"\" type=\"text\" />";
				else 
					form += "<input name=\"" + h + "\" value=\"" + http_request.getParameter(h) + "\" type=\"hidden\" />";
			}
			
			
			if (http_request.getParameters().containsKey("submit")) {
				element_to_click = "submit_snuck";
				form += "<input name=\"" + element_to_click + "\" type=\"submit\" /></form>";
			} else
				form += "<input name=\"" + element_to_click + "\" type=\"submit\" /></form>";

			Server.configureServer(form);
			
			String xmlConfig = "<root>\n" +
								"	<post>\n" +
								"		<parameters>\n" +
								"			<commands>\n" +
								"				<command>\n" +
								"					<name>open</name>\n" +
								"					<target>http://localhost:9000</target>\n" +
								"					<value></value>\n" +
								"				</command>\n" +
								"				<command>\n" +
								"					<name>type</name>\n" +
								"					<target>id=inject</target>\n" +
								"					<value>${INJECTION}</value>\n" +
								"				</command>\n" +
								"				<command>\n" +
								"					<name>click</name>\n" +
								"					<target>name=" + element_to_click + "</target>\n" +
								"					<value></value>\n" +
								"				</command>\n" +
								"			</commands>\n" +
								"		</parameters>\n" +
								"	</post>\n" +
								"</root>\n";
			
			return xmlConfig;
		} else {
			URL target = null;
			String targetURL = http_request.getTargetURL();
			LinkedList<String> parameters = null;
			String params = "";
			
			try {
				target = new URL(targetURL);
			} catch (MalformedURLException e) {
				Debug.printError("ERROR: unable to parse the supplied URL (" + targetURL + ")");
	            HaltHandler.quit_nok();
			}
			
			if (target.getQuery() != null){
				parameters = getQueryMap(target.getQuery());
					
				for (String p : parameters) {  
					if (p.split("=").length == 2) {
						if (!p.split("=")[0].equals(parameter_to_inject)){
							params += "			<parameter>" +
										p.split("=")[0] + ( p.split("=")[1].equals("") ? "" : "=" + p.split("=")[1] );
							params += "</parameter>\n";
						}
					}
				}  
			}
						
			return "<root>\n" +
					"	<get>\n" +
					"		<parameters>\n" +
					"			<targeturl>" + targetURL.split("\\?")[0] + "</targeturl>\n" +
					"			<paramtoinject>" + parameter_to_inject + "</paramtoinject>\n" +
								params + 
					"		</parameters>\n" +
					"	</get>\n" +
					"</root>\n";
		}
	}

	public static String generateConfigFileFromPostParameter(String post_parameters, String targetURL, String targetParam) {
		target = targetURL;
		String form = "Injecting... <form action=\"" + targetURL + "\" method=\"POST\">";
		String element_to_click = "submit";
		LinkedList<String> params = getQueryMap(post_parameters);
		boolean flag = false;
		
		for (String h : params){
			String[] tmp = h.split("=");
			if (tmp[0].equals(targetParam)) {
				form += "<input id=\"inject\" name=\"" + tmp[0] + "\" value=\"\" type=\"text\" />";
				flag = true;
			} else 
				form += "<input name=\"" + tmp[0] + "\" value=\"" + tmp[1] + "\" type=\"hidden\" />";
		}
		
		if (!flag){
			Debug.printError("ERROR: the target parameter is not present in the POST parameters");
            HaltHandler.quit_nok();
		}
		
		for (String h : params){
			String[] tmp = h.split("=");
			
			if (tmp[0].contains("submit")) {
				element_to_click = "submit_snuck";
				form += "<input name=\"" + element_to_click + "\" type=\"submit\" /></form>";
			} 
		}
		
		if (!element_to_click.equals("submit_snuck"))
			form += "<input name=\"" + element_to_click + "\" type=\"submit\" /></form>";

		Server.configureServer(form);		
		
		String xmlConfig = "<root>\n" +
							"	<post>\n" +
							"		<parameters>\n" +
							"			<commands>\n" +
							"				<command>\n" +
							"					<name>open</name>\n" +
							"					<target>http://localhost:9000</target>\n" +
							"					<value></value>\n" +
							"				</command>\n" +
							"				<command>\n" +
							"					<name>type</name>\n" +
							"					<target>id=inject</target>\n" +
							"					<value>${INJECTION}</value>\n" +
							"				</command>\n" +
							"				<command>\n" +
							"					<name>click</name>\n" +
							"					<target>name=" + element_to_click + "</target>\n" +
							"					<value></value>\n" +
							"				</command>\n" +
							"			</commands>\n" +
							"		</parameters>\n" +
							"	</post>\n" +
							"</root>\n";
		
		return xmlConfig;
	}
}