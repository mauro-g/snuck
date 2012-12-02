/*
   snuck - starter
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

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import commandline.CmdArgsParser;
import commandline.Debug;
import commandline.HaltHandler;

import xmlparser.XmlConfigReader;

public class Starter  {	
	private static Map<String, String[]> notRenderedContent_tags = null;
	
	private static WebDriver driver = null;
	
	private static HtmlUnitDriver driverFast = null;
		
	private static List<String> detectedXSSVectors = new LinkedList<String>();
	
	private static Map<String, List<String>> allowedElements = new HashMap<String, List<String>>();
	
	private static List<String> allowedProtocols =  new LinkedList<String>();
	
	// XmlConfigReader object whose purpose is to parse the XML config file (use case)
	private static XmlConfigReader xmlConfig;
	
	// useful for an optional login use case
	private static XmlConfigReader xmlConfigStart;
	
	private static String reflectionContext;
	
	// CmdArgsParser object, it keeps track of the passed arguments
	private static CmdArgsParser parseArgs;
		
	// it refers to the detected operation for the tested XSS filter
	// 1 stands for INSERT, 2 stands for UPDATE
	private static int operation = 0;
	
	// useful for driving Google Chrome
	private static ChromeDriverService service;
	
	// am I working with IE? If so, I have to modify some structures in order to be "IE-compliant"
	private static boolean IE_enabled;
		
	// it tracks the web browser I am running the test with
	private static String usedBrowser;
	
	// generate a random string to make the first injection
	private static String injection = new BigInteger(130, new SecureRandom()).toString(32);
	
	// monitoring the shutdown signal is mandatory for giving the attack vectors back in the case of forced quit
	private static HaltHandler shutdown_hook = new HaltHandler();
	
    public static void main(String[] args) {
    	Runtime.getRuntime().addShutdownHook(shutdown_hook);
    	 
    	// parse command line arguments 
    	parseArgs = new CmdArgsParser(args);    	
    	
    	// parse the xml config file
    	xmlConfig = new XmlConfigReader(parseArgs.getConfigfileName());

    	// create and Inject object - it will be used later to make injections
    	Inject inject;
    	
    	// check whether the tester wants to use a remote repository for the attack vectors
    	if (parseArgs.getAttackVectorsRepositoryURL() != null)
    		inject = new Inject(parseArgs.getAttackVectorsRepositoryURL());
    	else
    		inject = new Inject();
    		
		// avoid useless and annoying logs
		Logger logger = Logger.getLogger ("");
		logger.setLevel (Level.OFF);
				
		/**
		 * Here starts the reverse engineering process
		 */	
   	
		// HtmlUnitDriver is started
   		setupDriverFast();
		
		if (parseArgs.getStartConfigfileName() != null){
			// parse the start config file - i.e. login use case
	    	xmlConfigStart = new XmlConfigReader(parseArgs.getStartConfigfileName());
	    	xmlConfigStart.commonInject(driverFast, null, 0);
		}
		    
		if (!isReflected()) {
			Debug.printError("ERROR: the parameter is not reflected within the page => no XSS possible!");
    		driverFast.quit();
    		HaltHandler.quit_nok();
		}

        // check whether the XSS filter can be reversed on the basis of the reflection context
	    inject.checkReflectionContext(injection);   
	    	
	    // check whether the filter makes an UPDATE operation or
	    // an INSERT operation
	    checkOperation(injection, (new Random()).nextInt(10000) + ""); 	
        	
    	driverFast.quit();

    	/**
		 * Here starts the (really) malicious test
		 */ 
    	
   		setupDriver();
    	         
    	if (parseArgs.getStartConfigfileName() != null){
	    	xmlConfigStart.commonInject(driver, null, 0);
		}
	    	
    	String init_injection = injection + (new Random()).nextInt(100000);
       	inject(init_injection);
   		startMaliciousInjection(inject, init_injection);		
    	
    	boolean weakFilter = (inject.getNumberXSS() - inject.getLimitNumberXSS() == 0) && 
    							operation == 1;
    	
    	report.ReportGenerator.generateReport(parseArgs.getReportfileName(), parseArgs.getConfigfileName(), 
    											usedBrowser, reflectionContext, operation, allowedElements, 
    											allowedProtocols, detectedXSSVectors, weakFilter, false, xmlConfig.getMethod());
    	
    	Debug.print("\nThe malicious test has finished. A report has been generated: " + parseArgs.getReportfileName());

    	driver.quit();
    	
    	// Running on Chrome? Ok let's stop the service
    	if (service != null)
    		service.stop();
    	
    	HaltHandler.quit_ok();
    }
    
    /**
   	 * Start the HtmlUnit web driver
   	 */
    private static void setupDriverFast(){
    	if (parseArgs.getProxyInfo() != null){
			String proxy_conf = parseArgs.getProxyInfo();

	    	org.openqa.selenium.Proxy proxy = new org.openqa.selenium.Proxy();
	    	proxy.setHttpProxy(proxy_conf).setFtpProxy(proxy_conf).setSslProxy(proxy_conf);
	    	DesiredCapabilities cap = new DesiredCapabilities();
	    	cap.setCapability(CapabilityType.PROXY, proxy);
	    	driverFast = new HtmlUnitDriver(cap);
		} else 
			driverFast = new HtmlUnitDriver();
    }
    
    /**
	 * Start the requested web driver
	 */
	private static void setupDriver(){
    	
    	if (parseArgs.getChromeDriverPath() != null){
    		
    		String path_to_chromedriver = parseArgs.getChromeDriverPath();
    		
    		service = new ChromeDriverService.Builder()
    						.usingDriverExecutable(new File(path_to_chromedriver))
							.usingAnyFreePort()
							.build();
    		
    		try {
				service.start();
			} catch (IOException e) {
				Debug.printError("ERROR: unable to load Chrome server. \n" + e.getMessage());
				HaltHandler.quit_nok();
			}

    		// disable the built-in Chrome XSS filter
    		DesiredCapabilities capabilities = DesiredCapabilities.chrome();
    		capabilities.setCapability("chrome.switches", Arrays.asList("--disable-xss-auditor", "--disable-extensions"));

        	if (parseArgs.getProxyInfo() != null)
        		capabilities.setCapability("chrome.switches", Arrays.asList("--proxy-server=" + parseArgs.getProxyInfo()));

    		driver = new RemoteWebDriver(service.getUrl(), capabilities);
    		
    		usedBrowser = "Google Chrome";
    	} else if (parseArgs.getEnabledIE()) {
    		IE_enabled = true;
    		
    		if (parseArgs.getProxyInfo() != null){
    			String proxy_conf = parseArgs.getProxyInfo();

    	    	org.openqa.selenium.Proxy proxy = new org.openqa.selenium.Proxy();
    	    	proxy.setHttpProxy(proxy_conf).setFtpProxy(proxy_conf).setSslProxy(proxy_conf);
    	    	DesiredCapabilities cap = DesiredCapabilities.internetExplorer();
    	    	cap.setCapability(CapabilityType.PROXY, proxy);
    	    	driver = new InternetExplorerDriver(cap);
    		} else {
        		String path_to_IEdriver = parseArgs.getIEDriverPath();

    			File file = new File(path_to_IEdriver);
    			System.setProperty("webdriver.ie.driver", file.getAbsolutePath());
    			
		        driver = new InternetExplorerDriver();
    		}
    		
    		usedBrowser = "Internet Explorer";
    	} else {
    		
    		if (parseArgs.getProxyInfo() != null){
    			String proxy_conf = parseArgs.getProxyInfo();

    	    	org.openqa.selenium.Proxy proxy = new org.openqa.selenium.Proxy();
    	    	proxy.setHttpProxy(proxy_conf).setFtpProxy(proxy_conf).setSslProxy(proxy_conf);
    	    	DesiredCapabilities cap = new DesiredCapabilities();
    	    	cap.setCapability(CapabilityType.PROXY, proxy);
    	    	driver = new FirefoxDriver(cap);
    		} else {
    			driver = new FirefoxDriver();
    		}
    		
    		usedBrowser = "Mozilla Firefox";
    	}
    }
	
	private static int attempt_num = 0;
	// proposals is used for keeping track of the requests for injection to the user
    private static Set<String> proposals = new HashSet<String>(); 
    private static boolean flag = false;
	// does the injection fall in multiple contexts?
    private static boolean multi_contexts = false;
	private static void startMaliciousInjection(Inject inject, String injection) {        
		List<WebElement> parents = null;
        List<WebElement> elements = null;
    	boolean inHtmlComment = false;
    	
		// Expected reflection context: <tag>UNTRUSTED DATA</tag>
        parents = ContextDetection.getParents(injection, IE_enabled);
		// Expected reflection context = <element attribute="UNTRUSTED_DATA">
        elements = ContextDetection.getParentsWithInjectedAttribute(injection, IE_enabled);
        // Expected reflection context = <!-- UNTRUSTED_DATA -->
    	inHtmlComment = ContextDetection.isInHtmlComment(injection);
    	    	   	
    	if (attempt_num == 0)
    		if (!multi_contexts)
    			multi_contexts = ( parents.size() + elements.size() + (inHtmlComment ? 1 : 0) ) > 1;
    			
		for (int i = 0; i < parents.size() && attempt_num == 0; i++){
			String tagName = parents.get(i).getTagName();
	        String attrs = ContextDetection.getAllAttributes(parents.get(i), IE_enabled);
	        
	        // Reflection context: <script>UNTRUSTED DATA</script>
	        if (tagName.toLowerCase().equals("script") && (parents.get(i).getAttribute("src").equals("")) ){
	        	
	        	boolean test = false;
	        	String scriptContent = "";
	        	
	        	// textContent does not work in IE! Let's use innerHTML
	        	if (IE_enabled){
	        		scriptContent = (String) ((JavascriptExecutor)driver).executeScript("" +
	        				"return arguments[0].innerHTML;"
	        				, parents.get(i));
	        	} else {
	        		scriptContent = (String) ((JavascriptExecutor)driver).executeScript("" +
	    	        		"return arguments[0].textContent;"
	    	     	       	, parents.get(i));
	        	}
	        	
	        	if (!attrs.equals(""))
	        		reflectionContext = "<script " + attrs + ">" + scriptContent.replace(injection, "UNTRUSTED DATA")  + "</script>";
	        	else 
	        		reflectionContext = "<script>" + scriptContent.replace(injection, "UNTRUSTED DATA") + "</script>";

	        	// <script>someFunction('blablaUNTRUSTED_DATAbla');</script>
	        	Pattern pattern = Pattern.compile("\\('[^'\\)]*" + injection + ".*");
	        	Matcher match = pattern.matcher(scriptContent);		
	        	
	        	if (match.find()){
	        		if (!multi_contexts || (proposals.add("1") 
	        				&& Debug.askForContext("<script>someFunction('UNTRUSTED_DATA');</script>"))){
		        		
	        			test = true;
			        	Debug.print("Injecting malicious vectors... (1)");
		        		inject.injectWithinScriptTag("')", "//");
		        		// ' => \'
		        		Debug.print("Injecting malicious vectors... (2)");
		        		inject.injectWithinScriptTag("\\')", "//");
		        		Debug.print("Injecting malicious vectors... (3)");
		        		inject.injectTag("</script>");
		        		return;
	        		}
	        	} 
	        	
	        	// <script>someFunction("blablaUNTRUSTED_DATAbla");</script>
	        	pattern = Pattern.compile("\\(\"[^\"\\)]*" + injection + ".*");
	        	match = pattern.matcher(scriptContent);		

	        	if (match.find()){
	        		if (!multi_contexts || (proposals.add("2") 
	        				&& Debug.askForContext("<script>someFunction(\"UNTRUSTED_DATA\");</script>"))){
		        		
		        		test = true;
			        	Debug.print("Injecting malicious vectors... (1)");
		        		inject.injectWithinScriptTag("\")", "//");
		        		// " => \"
		        		Debug.print("Injecting malicious vectors... (2)");
		        		inject.injectWithinScriptTag("\\\")", "//");
		        		Debug.print("Injecting malicious vectors... (3)");
		        		inject.injectTag("</script>");
		        		return;
	        		}
	        	}
	        		
	        	// <script>someFunction(UNTRUSTED_DATA);</script>
	        	pattern = Pattern.compile("\\([^('|\"|\\))]*" + injection + ".*");
	        	match = pattern.matcher(scriptContent);
	        	
	        	if (match.find()){
	        		if (!multi_contexts || (proposals.add("3") 
	        				&& Debug.askForContext("<script>someFunction(UNTRUSTED_DATA);</script>"))){
		        		
		        		test = true;
			        	Debug.print("Injecting malicious vectors... (1)");
		        		inject.injectWithinScriptTag(")", "//");
		        		Debug.print("Injecting malicious vectors... (2)");
		        		inject.injectTag("</script>");
		        		return;
	        		}
	        	}
	        		
	        	// <script>var variable = 'blablaUNTRUSTED_DATAbla';</script>
	        	pattern = Pattern.compile("'[^']*" + injection + ".*");
	        	match = pattern.matcher(scriptContent);	
	        	boolean f = match.find();
	        	
	        	pattern = Pattern.compile("\\('[^'\\)]*" + injection + ".*");
	        	match = pattern.matcher(scriptContent);	
	        	
	        	// really dirty here...
	        	if (!match.find() && f){
	        		if (!multi_contexts || (proposals.add("4") 
	        				&& Debug.askForContext("<script>var variable = 'UNTRUSTED_DATA';</script>"))){
		        		
		        		test = true;
			        	Debug.print("Injecting malicious vectors... (1)");
		        		inject.injectWithinScriptTag("'", "//");
		        		Debug.print("Injecting malicious vectors... (2)");
		        		inject.injectWithinScriptTag("\\')", "//");
		        		Debug.print("Injecting malicious vectors... (3)");
		        		inject.injectTag("</script>");
		        		return;
	        		}
	        	}
	        		
	        	// <script>var variable = "blablaUNTRUSTED_DATAbla";</script>
	        	pattern = Pattern.compile("[^\\(]\"[^\"]*" + injection + ".*");
	        	match = pattern.matcher(scriptContent);		
	        	
	        	if (match.find()){
	        		if (!multi_contexts || (proposals.add("5") 
	        				&& Debug.askForContext("<script>var variable = \"UNTRUSTED_DATA\";</script>"))){
		        		
		        		test = true;
			        	Debug.print("Injecting malicious vectors... (1)");
		        		inject.injectWithinScriptTag("\"", "//");
		        		Debug.print("Injecting malicious vectors... (2)");
		        		inject.injectWithinScriptTag("\\\"", "//");
		        		Debug.print("Injecting malicious vectors... (3)");
		        		inject.injectTag("</script>");
		        		return;
	        		}
	        	}
	        	
	        	// <script> // blabla UNTRUSTED_DATA</script>
	        	pattern = Pattern.compile("//[^\\r?\\n]*" + injection + ".*");
	        	match = pattern.matcher(scriptContent);	

	        	if (match.find()){
	        		if (!multi_contexts || (proposals.add("6") 
	        				&& Debug.askForContext("<script> // UNTRUSTED_DATA</script>"))){
		        		
		        		test = true;
			        	Debug.print("Injecting malicious vectors... (1)");
		        		inject.injectWithinScriptTag("\r\n0", null);
		        		Debug.print("Injecting malicious vectors... (2)");
		        		inject.injectTag("</script>");
		        		return;
	        		}
	        	}
	        	
	        	// <script> /* blabla UNTRUSTED_DATA blabla */ </script>
	        	pattern = Pattern.compile("/\\*[^\\*/]*" + injection + ".*");
	        	match = pattern.matcher(scriptContent);	

	        	if (match.find()){
	        		if (!multi_contexts || (proposals.add("7") 
	        				&& Debug.askForContext("<script> /* UNTRUSTED_DATA */</script>"))){
		        	
		        		test = true;
			        	Debug.print("Injecting malicious vectors... (1)");
		        		inject.injectWithinScriptTag("*/\r\n", "/*");
		        		Debug.print("Injecting malicious vectors... (2)");
		        		inject.injectTag("</script>");
		        		return;
	        		}
	        	}
	        	
	        	if (!test && proposals.size() == 0) {
	        		if (!multi_contexts || (proposals.add("8") 
	        				&& Debug.askForContext("<script> UNTRUSTED_DATA </script>"))){
	        			
	        			Debug.print("Injecting malicious vectors...");
			        	inject.injectWithinScriptTag(null, "//");
			        	return;
	        		}
	        	}
	        	
	        } else { 
	        	if (!attrs.equals(""))
	        		reflectionContext = "<" + tagName + " " + attrs + ">" + "UNTRUSTED DATA" + "</" + tagName + ">";
	        	else 
	        		reflectionContext = "<" + tagName + ">" + "UNTRUSTED DATA" + "</" + tagName + ">";

	        	if (notRenderedContent_tags == null)
	        		populateNotRendered();
	        		
	        	if (notRenderedContent_tags.containsKey(tagName.toLowerCase())){
	        		for (String k : notRenderedContent_tags.keySet()) {
	        			if (tagName.toLowerCase().equals(k)){
	        				String[] attributes = notRenderedContent_tags.get(k);
	        				
	        				if (attributes == null){	
	        					if (!multi_contexts || (proposals.add(k) 
	        	        				&& Debug.askForContext("<" + k + "> UNTRUSTED_DATA </" + k + ">"))){
	        		        		
	        						Debug.print("Injecting malicious vectors... (1)");
					        		inject.injectTag("</" + k + ">");
					        		Debug.print("Injecting malicious vectors... (2)");
									inject.injectScriptTag("</" + k + ">");
									return;
	        		        	}
							}
	        				else {
		       					for (String attr : attributes){
		       						if (parents.get(i).getAttribute(attr) != null){
		       							if (!multi_contexts  || (proposals.add(k) 
			        	        				&& Debug.askForContext("<" + k + "> UNTRUSTED_DATA </" + k + ">"))){
		       								
			       							Debug.print("Injecting malicious vectors... (1)");
			       							inject.injectTag("</" + k + ">");
			       							Debug.print("Injecting malicious vectors... (2)");
			       							inject.injectScriptTag("</" + k + ">");
			       							return;
		       							}
		       						}
		       					}
	        				}
	        			}
	        		}
	        	} else {
	        		
	        		if (!multi_contexts || (proposals.add("9") 
	        				&& Debug.askForContext("<HTMLtag> UNTRUSTED_DATA </HTMLtag>"))){
	        			
		        		Debug.print("Injecting malicious vectors...");
			       		inject.injectTag(null);
			        		
		        		if (allowedElements.keySet().contains("script")){
		        			Debug.print("Injecting malicious vectors through <script>...");
			       			inject.injectScriptTag(null);
		        		}
		        		return;
	        		}
	        	}
	        }
	        
        }
                
		if (!flag){
			inject.injectDummyAttribute();
			flag = true;
		}
		
		if (elements.size() != 0 && attempt_num == 0) {
    		// reload the page
        	inject(injection);
        }
		
        elements = ContextDetection.getParentsWithInjectedAttribute(injection, IE_enabled);

		for (int i = 0; i < elements.size(); i++){
        	String tagName = elements.get(i).getTagName();
        	String attrs = ContextDetection.getAttributesFromInjected(elements.get(i), injection, IE_enabled);
        	String injected_attribute = ContextDetection.getInjectedAttribute();
        	        	
        	if (injected_attribute == null || elements.get(i).getAttribute(injected_attribute) == null) break; 
	                	
	        reflectionContext = "<" + tagName + " " + attrs + " " + injected_attribute + "=\"" + 
	        						elements.get(i).getAttribute(injected_attribute).replace(injection, "UNTRUSTED DATA") + "\">[...]</" + tagName + ">";
        	
        	boolean test = false;
        	String linkText = elements.get(i).getText();
        	String injected_attribute_content = elements.get(i).getAttribute(injected_attribute);

        	// href? src? 
	        if (injected_attribute.equals("src") && !tagName.equals("img")){
	        	if (!multi_contexts || (proposals.add("10") 
        				&& Debug.askForContext("<HTMLtag src=\"UNTRUSTED_DATA\">"))){
		        	test = true;
		        	
		        	if (inject.areAttributesBreakable())
		        		Debug.print("Injecting malicious vectors... (1)");
		        	else 
		        		Debug.print("Injecting malicious vectors... ");
		        	inject.injectUntrustedURL_src();
	        	}
	        } else if (injected_attribute.equals("href") && !test){		
	        	if (!multi_contexts || (proposals.add("11") 
        				&& Debug.askForContext("<a href=\"UNTRUSTED_DATA\">"))){
	        		
		        	test = true;
		        	if (inject.areAttributesBreakable())
		        		Debug.print("Injecting malicious vectors... (1)");
		        	else 
		        		Debug.print("Injecting malicious vectors... ");
		        	inject.injectUntrustedURL_href(linkText);
	        	}
	        // IE => width: expression(alert(1));  
	        } else if (injected_attribute.equals("style") && IE_enabled && !test){	
	        	if (!multi_contexts || (proposals.add("12") 
        				&& Debug.askForContext("<HTMLtag style=\"UNTRUSTED_DATA\">"))){
	        		
		        	test = true;
		        	if (inject.areAttributesBreakable())
		        		Debug.print("Injecting malicious vectors... (1)");
		        	else 
		        		Debug.print("Injecting malicious vectors... ");
		        	inject.injectExpression();
	        	}
	        } else if (injected_attribute.startsWith("on") && !test){
	        			        	
		        	boolean z = false;
		        	
		        	Pattern pattern = Pattern.compile("'[^']*" + injection + ".*");
		        	Matcher match = pattern.matcher(injected_attribute_content);		
		        	
		        	if (match.find()){
		        		z = true;
		        		if (injected_attribute.equals("onclick"))
		        			if (!multi_contexts || (proposals.add("13") 
		            				&& Debug.askForContext("<HTMLtag onclick=\"var z = 'UNTRUSTED_DATA';\">"))){
		        				
		        				if (inject.areAttributesBreakable())
		    		        		Debug.print("Injecting malicious vectors... (1)");
		    		        	else 
		    		        		Debug.print("Injecting malicious vectors... ");
		        				
		        				inject.injectWithinEventHandler("'");
		        				test = true;
		        			}
		        		else
		        			if (!multi_contexts || (proposals.add("14") 
		            				&& Debug.askForContext("<HTMLtag eventHandler=\"var z = 'UNTRUSTED_DATA';\">"))){
		        				
		        				if (inject.areAttributesBreakable())
		    		        		Debug.print("Injecting malicious vectors... (1)");
		    		        	else 
		    		        		Debug.print("Injecting malicious vectors... ");
		        				
		        				inject.injectWithinScriptTag("'", "//");
		        				test = true;
		        			}
		        	} 
		        	
		        	pattern = Pattern.compile("\"[^\"]*" + injection + ".*");
		        	match = pattern.matcher(injected_attribute_content);		
		        	
		        	if (match.find()){
		        		z = true;
		        		if (injected_attribute.equals("onclick"))
		        			if (!multi_contexts || (proposals.add("15") 
		            				&& Debug.askForContext("<HTMLtag onclick='var z = \"UNTRUSTED_DATA\";'>"))){
		        				
		        				if (inject.areAttributesBreakable())
		    		        		Debug.print("Injecting malicious vectors... (1)");
		    		        	else 
		    		        		Debug.print("Injecting malicious vectors... ");
		        				
		        				inject.injectWithinEventHandler("\"");
		        				test = true;
		        			}
		        		else
		        			if (!multi_contexts || (proposals.add("16") 
		            				&& Debug.askForContext("<HTMLtag eventHandler='var z = \"UNTRUSTED_DATA\";'>"))){
		        				
		        				if (inject.areAttributesBreakable())
		    		        		Debug.print("Injecting malicious vectors... (1)");
		    		        	else 
		    		        		Debug.print("Injecting malicious vectors... ");
		        				
		        				inject.injectWithinScriptTag("\"", "//");
		        				test = true;
		        			}
		        	} 
		        	
		        	if (!z)
		        		if (injected_attribute.equals("onclick")) {
		        			if (!multi_contexts || (proposals.add("17") 
		            				&& Debug.askForContext("<HTMLtag onclick=\"UNTRUSTED_DATA\">"))){
		        				
		        				if (inject.areAttributesBreakable())
		    		        		Debug.print("Injecting malicious vectors... (1)");
		    		        	else 
		    		        		Debug.print("Injecting malicious vectors... ");
		        				
		        				inject.injectWithinEventHandler("0");
		        				test = true;
		        			}
		        		} else {
		        			if (!multi_contexts || (proposals.add("18") 
		            				&& Debug.askForContext("<HTMLtag eventHandler=\"UNTRUSTED_DATA\">"))){
		        				
		        				if (inject.areAttributesBreakable())
		    		        		Debug.print("Injecting malicious vectors... (1)");
		    		        	else 
		    		        		Debug.print("Injecting malicious vectors... ");
		        				
		        				inject.injectWithinScriptTag("0", "//");
		        				test = true;
		        			}
		        		}
	        }
	        
	        if (inject.areAttributesBreakable()){
	        	boolean tmp = false;
	        	
	        	// inject an on* attribute
	        	if (tagName.equals("img")) {
	        		// Change the image as you prefer! It is just useful for injections like
	        		// <img src="UNTRUSTED_DATA" />
	        		String path_to_real_image = "http://www.sneaked.net/Images/12300.png";
	        		if (test){
	        			Debug.print("Injecting malicious vectors... (2)");
	        			inject.injectAttribute(path_to_real_image);
	        		} else {
	        			if (!multi_contexts || (proposals.add("19") 
	            				&& Debug.askForContext("<img src=\"UNTRUSTED_DATA\" />"))) {
	        				
	        				Debug.print("Injecting malicious vectors... (1)");
		        			inject.injectAttribute(path_to_real_image);
	        				tmp = true;
	        			}
	        		}
	        	} else
	        		if (test) {
	        			Debug.print("Injecting malicious vectors... (2)");
	        			inject.injectAttribute(null);
	        		} else if (!multi_contexts || (proposals.add("20") 
	        					&& Debug.askForContext("<HTMLtag attribute=\"UNTRUSTED_DATA\">"))) {
	        			
        				Debug.print("Injecting malicious vectors... (1)");
		        		inject.injectAttribute(null);
		        		tmp = true;
	        		}
				
				// break the element and inject a new tag
				if (test || tmp) {
					if (test) Debug.print("Injecting malicious vectors... (3)");
		        	else Debug.print("Injecting malicious vectors... (2)");
					
					if (tagName.equals("iframe"))
						inject.breakElement("</iframe>");
					else 
						inject.breakElement(null);
					
					return;
				}
	        } else if (test)
	        	return;	        
	    }
        
        
    	if (inHtmlComment){
        	reflectionContext = "<!-- UNTRUSTED DATA -->";

        	if (!multi_contexts || (proposals.add("21") 
    				&& Debug.askForContext("<!-- UNTRUSTED_DATA -->"))) {
        		
	        		Debug.print("Injecting malicious vectors... (1)");
	        		inject.injectTag("-->");
	        		Debug.print("Injecting malicious vectors... (2)");
	    			inject.injectScriptTag("-->");
	    			return;
        	}
    	} 
	   			
		// last attempt 
		// we need to understand whether the injection falls in CSS context
		if ( ( multi_contexts || ( elements != null && elements.size() == 0) || ( parents != null && parents.size() == 0 ) ) && attempt_num == 0 ){
			attempt_num++;		    
			String css_injection = randomColor();
	       	inject(css_injection);
	   		startMaliciousInjection(inject, css_injection);	
	   		return;
		}
		
		// I'm here?! Ok, nothing to do...
		if (multi_contexts){
    		Debug.printError("\nINFO: no more reflection contexts available...");
    		HaltHandler.quit_ok();
    	}
    }

	/**
	 * Inject with the chosen web driver 
	 */
	public static void inject(String injection){
		xmlConfig.commonInject(driver, injection, parseArgs.getDelayInterval());
	}

	/**
	 * Inject with the HtmlUnitDriver
	 */
	public static void injectFast(String injection){	
		xmlConfig.commonInject(driverFast, injection, parseArgs.getDelayInterval());
	}
	
	/**
	 * Check whether the injection is reflected in the reflection page
	 */
	private static boolean isReflected() {
		// alphanumeric string
		injectFast(injection);
				
		if (driverFast.getPageSource().contains(injection) || driverFast.getPageSource().contains(injection.toLowerCase()))
			return true;
		
		// numeric string
		injection =  "" + (new Random()).nextInt(100000);
		injectFast(injection);

		if (driverFast.getPageSource().contains(injection) || driverFast.getPageSource().contains(injection.toLowerCase()))
			return true;
		
		// URL
		injection = "http://www.test.com";
		injectFast(injection);

		if (driverFast.getPageSource().contains(injection) || driverFast.getPageSource().contains(injection.toLowerCase()))
			return true;
		
		// email
		injection = "test@gmail.com";
		injectFast(injection);

		if (driverFast.getPageSource().contains(injection) || driverFast.getPageSource().contains(injection.toLowerCase()))
			return true;
		
		return false;
	}

	/**
	 * Detect which kind of operation is performed by the XSS filter among INSERT (1) or UPDATE (2)
	 */
	private static void checkOperation(String injection1, String injection2) {
		injectFast(injection2);
		
		operation = ( driverFast.getPageSource().contains(injection1) && driverFast.getPageSource().contains(injection2) ) ? 1 : 2;
	}
	
	private static void populateNotRendered() {
		notRenderedContent_tags = new HashMap<String, String[]>();

		notRenderedContent_tags.put("marquee", null);
    	notRenderedContent_tags.put("style", null);
    	notRenderedContent_tags.put("xmp", null);
    	notRenderedContent_tags.put("iframe", null);
    	notRenderedContent_tags.put("noscript", null);
    	notRenderedContent_tags.put("textarea", null);
    	notRenderedContent_tags.put("title", null);
    	notRenderedContent_tags.put("script", new String[] { "src" });
	}
	
	public static String randomColor() {
    	String[] colors = { "aqua", "black", "blue", "fuchsia", "gray", "green", "lime", "maroon", "navy", "olive", "purple", "red",
    						"silver", "teal", "white", "yellow" };
    	
        return colors[(int) (Math.random() * colors.length)];
	}
	
	public static String getCurrentBrowser(){
		return usedBrowser;
	}
	
	public static WebDriver getDriver() {
		return (driver == null) ? null : driver;
	}
	
	public static HtmlUnitDriver getDriverFast() {
		return (driverFast == null) ? null : driverFast;
	}

	public static List<String> getDetectedXSSVector() {
		return detectedXSSVectors;
	}
	
	public static void addDetectedXSSVector(String xss) {
		detectedXSSVectors.add(xss);
	}
	
	public static void setAllowedProtocols(LinkedList<String> allowedSchemes) {
		allowedProtocols = allowedSchemes;		
	}

	public static void setAllowedElements(HashMap<String, List<String>> allowedHTMLElements) {
		allowedElements = allowedHTMLElements;		
	}

	public static int getOperation() {
		return operation;
	}
	
	public static ChromeDriverService getChromeService() {
		return service;
	}
	
	public static void refreshDriver(){		
		if (driver != null)
    		driver.quit();	
		
		if (service != null)
    		service.stop();
		
		setupDriver();
	}
	
	public static CmdArgsParser getParsedArgs() {
		return parseArgs;
	}

	public static Thread getHaltHandler() {
		return shutdown_hook;
	}
	
	public static void brokenPage() {
		if (reflectionContext != null) {
			report.ReportGenerator.generateReport(parseArgs.getReportfileName(), parseArgs.getConfigfileName(), usedBrowser, 
													reflectionContext, operation, allowedElements, allowedProtocols, detectedXSSVectors, 
													false, true, xmlConfig.getMethod());
	    	Debug.print("\nA report has been generated: " + parseArgs.getReportfileName());
		}
		
		if (driverFast != null)
			driverFast.quit();
		
    	if (driver != null)
    		driver.quit();	
    	
    	if (service != null)
    		service.stop();
    	
    	HaltHandler.quit_nok();
	}
	
	public static void forceQuit() {
		if (reflectionContext != null) {
			report.ReportGenerator.generateReport(parseArgs.getReportfileName(), parseArgs.getConfigfileName(), usedBrowser, 
													reflectionContext, operation, allowedElements, allowedProtocols, detectedXSSVectors, 
													false, false, xmlConfig.getMethod());
	    	Debug.print("\n\nJust broken the tested XSS filter! \nA report has been generated: " + parseArgs.getReportfileName());
		}
		
		if (driverFast != null)
			driverFast.quit();
		
    	if (driver != null)
    		driver.quit();	
    	
    	if (service != null)
    		service.stop();
    	
    	HaltHandler.quit_ok();
	}
}