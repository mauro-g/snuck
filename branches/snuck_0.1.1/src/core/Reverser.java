/*
   snuck - reverse engineering processor
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

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import xmlparser.XmlConfigReader;

import commandline.CmdArgsParser;

public class Reverser extends Thread {
	// HTML attributes this thread is going to inject
	private String htmlAttributes = null;
	
	// all the possible HTML attributes
	private String[] htmlAttributes_arr = null;
	
	// HTML elements this thread is going to inject
	private String[] htmlElements = null;
	
	// HTMLUnit web driver - each thread has its own driver
	private HtmlUnitDriver driver = null;
	
	// XML configuration file parser
	private XmlConfigReader xmlConfig = null;
	
	// total # of injection for all the started threads
	private int num_injections;
	
	// "shared" bar
	private static commandline.ProgressBar bar = new commandline.ProgressBar();
	
	// "shared" map
	private static HashMap<String, List<String>> allowedElements = new HashMap<String, List<String>>();
	
	private static int j = 0;
	
	public Reverser(String[] elements, String attributes, String[] htmlAttributes_arr, int length) {
		this.htmlElements = elements;
		this.htmlAttributes = attributes;
		this.htmlAttributes_arr = htmlAttributes_arr;
		this.num_injections = length;
		CmdArgsParser args = Starter.getParsedArgs();
		
		xmlConfig = new XmlConfigReader(args.getConfigfileName());
				
		if (args.getProxyInfo() != null){
			String proxy_conf = args.getProxyInfo();

	    	org.openqa.selenium.Proxy proxy = new org.openqa.selenium.Proxy();
	    	proxy.setHttpProxy(proxy_conf).setFtpProxy(proxy_conf).setSslProxy(proxy_conf);
	    	DesiredCapabilities cap = new DesiredCapabilities();
	    	cap.setCapability(CapabilityType.PROXY, proxy);
	    	driver = new HtmlUnitDriver(cap);
		} else 
			driver = new HtmlUnitDriver();
		
		if (args.getStartConfigfileName() != null){
			XmlConfigReader xmlConfigStart = new XmlConfigReader(args.getStartConfigfileName());
	    	xmlConfigStart.commonInject(driver, null, 0);
		}
	}
		 
	public void run() {
		String first_delimiter = new BigInteger(130, new SecureRandom()).toString(16);
		String current;
		String delimiter;
		int i = 0;
						
		for (String element : htmlElements){
			i++;
			bar.update(j, num_injections);
			j++;

			delimiter = first_delimiter + i;
			current = "<" + element + " " +
					htmlAttributes + ">" + "X" + "</" + element + ">";
			
			inject(delimiter + current + delimiter);

			checkReversion(delimiter, element, htmlAttributes_arr, allowedElements, driver);
		}
		
		driver.quit();
	}
	
	public void inject(String injection){	
		xmlConfig.commonInject(driver, injection, 0);
	}
	
	public static void checkReversion(String delimiter, String element, 
										String[] htmlAttributes, 
										HashMap<String, List<String>> allowedElements,
										HtmlUnitDriver driver){		
		String reflection = null;
		Pattern pattern = Pattern.compile(delimiter + "(.*)" + delimiter, Pattern.DOTALL);
    	Matcher match = pattern.matcher(driver.getPageSource().replace("\n", ""));	
    		
    	if (match.find()){
    		reflection = match.group().replace(delimiter, "").trim();

    		if(reflection != null && reflection.trim().contains("<" + element)){
    			LinkedList<String> currentAttributes = new LinkedList<String>();
    			   				    				
    			for (String attribute : htmlAttributes){    					
    				if (reflection.contains(attribute + "=\"X\"") || (reflection.contains(attribute.toLowerCase() + "=\"X\"")))
    					currentAttributes.add(attribute);
    			}
    			
    			allowedElements.put(element, (currentAttributes.size() == 0) ? null : currentAttributes);
       		}
    	}		
	}
	
	public static HashMap<String, List<String>> getAllowedElements(){
		return allowedElements;
	}
}
