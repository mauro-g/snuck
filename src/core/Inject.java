/*
   snuck - injector
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

package core;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


import org.apache.commons.lang.StringEscapeUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import commandline.Debug;
import commandline.HaltHandler;


public class Inject {
	private final String pathTo_common_payloads = "payloads/html_payloads";
	private final String pathTo_uri_alert_payloads = "payloads/uri_payloads";
	private final String pathTo_js_alert_payloads = "payloads/js_alert_payloads";
	private final String pathTo_expression_alert_payloads = "payloads/expression_alert_payloads";
	
	private final String _dummyNodeAttributeKey = fuzzCases("dummy_attr");
	private final String _dummyNodeAttributeValue = fuzzCases("dummy_attr_value");
		 
	// " dummy_attr=dummy_attr_value "
	private final String _dummy_attribute = "'\" " + _dummyNodeAttributeKey
											+ "=" + _dummyNodeAttributeValue + " \"";
	
	private String _dummy_link_text = "_dummy_";
	
	private String[] _script_payloads;
	
	private String[] _vectors;
	
	private String[] _malicious_uris;
	
	private String[] _expression_payloads;
	
	private boolean areAttributesBreakable = false;
	
	private static String[] htmlAttributes = {
		"onblur", "onchange", "onclick", "ondblclick", "onerror", "onfocus", "onkeydown",  "onkeypress",
		"onkeyup", "onload", "onmousedown", "onmousemove", "onmouseout", "onmouseover", "onmouseup",
		"onresize", "onselect", "onunload",
		"accept", "accept-charset", "accesskey", "action", "align", "alt", "async", "autocomplete",
		"autofocus", "autoplay", "bgcolor", "border", "buffered", "challenge", "charset", "checked",
		"cite", "class", "code", "codebase", "color", "cols", "colspan", "content", "contenteditable",
		"contextmenu", "controls", "coords", "data", "datetime", "default", "defer", "dir", "dirname",
		"disabled", "draggable", "dropzone", "enctype", "for", "form", "headers", "height", "hidden",
		"high", "href", "hreflang", "http-equiv", "icon", "id", "ismap", "itemprop", "keytype",
		"kind", "label", "lang", "language", "list", "loop", "low", "max", "maxlenght", "media",
		"method", "name", "pattern", "preload", "radiogroup", "readonly", "rel", "required", "reversed",
		"rows", "rowspan", "sandbox", "selected", "shape", "size", "span", "src", "style", "target",
		"title", "type", "usemap", "value", "width", "DUMMYattribute"
	};
	
	private static String[] htmlElements = { 
		"a", "abbr", "acronym", "address", "applet", "area", "article", "aside", "audio",
		"b", "base", "bdi", "bdo", "bgsound", "big", "blink", "blockquote",
		"body", "br", "button", "canvas", "caption", "center", "cite", "code", "col", "colgroup",
		"command", "datalist", "dd", "del", "details", "dfn", "dir", "div", "dl", "dt",
		"em", "embed", "fieldset", "figure", "font", "footer", "form",
		"frame", "frameset", "h1", "head", "header", "hgroup", "hr", "i", "iframe",
		"img", "input", "ins", "kbd", "keygen", "label", "legend", "li", "link", "listing",
		"map", "mark", "marquee", "math", "menu", "meta", "meter", "nav", "nobr", "object", "ol",
		"option", "output", "p", "param", "pre", "progress", "q", "script",
		"select", "small", "source", "span", "strike", "strong", "style", "sub", "summary", "sup",
		"table", "tbody", "td", "textarea", "tfoot", "th", "thead", "time", "title", "tr",
		"track", "tt", "u", "ul", "video", "wbr", "xmp"
	};
	
	private static String[] protocols = {
		"http", "https", "ftp", "mailto", "news", "irc", "news", "irc", "gopher",
		"nntp", "feed", "telnet", "mms", "rtsp", "svn", "javascript", "data", "vbscript"
	}; 
	
	private static int numberOfXSS = 0;
	
	private static int limitNumberOfXSS = 3;
	
	public static commandline.ProgressBar bar = null;
	
	public Inject(){
		_vectors = loadPayloadsFromfile(pathTo_common_payloads);
		_malicious_uris = loadPayloadsFromfile(pathTo_uri_alert_payloads);
		_script_payloads = loadPayloadsFromfile(pathTo_js_alert_payloads);
		_expression_payloads = loadPayloadsFromfile(pathTo_expression_alert_payloads);	
	}
	
	public Inject(String attackVectorsRepositoryURL) {
		Debug.print("Loading attack vectors from the remote repository ( " + attackVectorsRepositoryURL + " )");
		commandline.ProgressBar bar = new commandline.ProgressBar();
		
		int size = 4, i = 0;
		bar.update(i++, size);
		_vectors = loadPayloadsFromURL(attackVectorsRepositoryURL + pathTo_common_payloads);
		bar.update(i++, size);
		_malicious_uris = loadPayloadsFromURL(attackVectorsRepositoryURL + pathTo_uri_alert_payloads);
		bar.update(i++, size);
		_script_payloads = loadPayloadsFromURL(attackVectorsRepositoryURL + pathTo_js_alert_payloads);
		bar.update(i++, size);
		_expression_payloads = loadPayloadsFromURL(attackVectorsRepositoryURL + pathTo_expression_alert_payloads);
	}

	public int getNumberXSS(){
		return numberOfXSS;
	}
	
	public int getLimitNumberXSS(){
		return limitNumberOfXSS;
	}
		
	private String[] loadPayloadsFromfile(String pathTo_payloads) {
		String[] _payloads = null;
		
		try{
			FileInputStream fstream = new FileInputStream(pathTo_payloads);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			int i = 0;
			
			LineNumberReader  lnr = new LineNumberReader(new FileReader(pathTo_payloads));
			lnr.skip(Long.MAX_VALUE);

			_payloads = new String[lnr.getLineNumber()];
			
			while ((strLine = br.readLine()) != null) {
				_payloads[i] = strLine;
				i++;
			}

			in.close();
		}catch (Exception e){
				Debug.printError("ERROR: unable to load the attack payloads");
				HaltHandler.quit_nok();
		}
		
		return _payloads;
	}
	
	private String[] loadPayloadsFromURL(String attackVectorsRepositoryURL) {
		String[] _payloads = null;
		
		try{
			URL repo = new URL(attackVectorsRepositoryURL);
			BufferedReader in = new BufferedReader(new InputStreamReader(repo.openStream()));
			
		    String inputLine;
			int i = 0;

		    while ((inputLine = in.readLine()) != null)
		    	i++;
		    			
			_payloads = new String[i];
			i = 0;
			in = new BufferedReader(new InputStreamReader(repo.openStream()));
			
			while ((inputLine = in.readLine()) != null) {
				_payloads[i] = inputLine;
				i++;
			}

			in.close();
		}catch (Exception e){
				Debug.printError("\nERROR: unable to load the attack payloads from the remote repository");
				HaltHandler.quit_nok();
		}
		
		return _payloads;
	}

	public boolean areAttributesBreakable(){
		return areAttributesBreakable;
	}
	
	public static void log(String attackVector){
		if (!Starter.getDetectedXSSVector().contains(attackVector)){
			Starter.addDetectedXSSVector(attackVector);
			
			if (Starter.getOperation() == 1)
				numberOfXSS++;
		}
	}
	
	/*
	private void waitForAlert(String current_injection){
		final WebDriver driver = Starter.getDriver();

		Alert alert = (new WebDriverWait(driver, 0))
				.until(new ExpectedCondition<Alert>(){	
					public Alert apply(WebDriver d) {
						return driver.switchTo().alert();
					}
			    });

		alert.accept();
		
		if (current_injection != null){
			log(current_injection);

			if (bar != null)
				bar.setBroken();
			
			if (Starter.getParsedArgs().getStopFirstPositive()) {
				Starter.forceQuit();
			}
		}
	}
	*/
	
	public static void getAlertsWithTimeout(String attack_vector, boolean user_interaction) {
		ExecutorService executor = Executors.newSingleThreadExecutor();
	    Future<String> future = executor.submit(new AlertDetection(numberOfXSS, attack_vector, user_interaction));
	    
	    try {
	    	future.get(10, TimeUnit.SECONDS);
	    } catch (TimeoutException e) {
 			Starter.refreshDriver();
	    } catch (InterruptedException e) { } 
	      catch (ExecutionException e) { }
	    
 	    executor.shutdownNow();
	}
	
	private String modifyVector(String vector){
		
		vector = vector.replace("%alert%", selectRandom(_script_payloads));
		vector = vector.replace("%uri%", selectRandom(_malicious_uris));
		
		return vector;
	}
	
	private String selectRandom(String[] x){
		return x[new Random().nextInt(x.length)];
	}
	
	public String fuzzCases(String original_string){
		String fuzzed_string = "";
		Random randomGenerator = new Random();
		int randomInt = 0;
		String tmp = "";
		
		for (int i = 0; i < original_string.length(); i++){
			randomInt = randomGenerator.nextInt(10);
			tmp = original_string.substring(i, i+1);
			
			if (tmp.matches("[a-zA-Z]"))
				fuzzed_string += (randomInt > 5) ? tmp.toLowerCase() : tmp.toUpperCase() ;
			else
				fuzzed_string += tmp;
		}
		
		return fuzzed_string;
	}
	
	private void injector(String[] vectors, String closing_elements) {
		bar = new commandline.ProgressBar();
		int i = 0, size = vectors.length;
		
		for (String current : vectors){
			bar.update(i, size);
			i++;
			
			// INSERT OPERATION -> stop the test in the case the filter is too weak (do not flood the browser with an infinite # of alert dialogs)
			if ( (numberOfXSS < limitNumberOfXSS && Starter.getOperation() == 1 ) ||
						Starter.getOperation() == 2) {
				
				// put "something" before the payload
				current = (closing_elements == null) ? current : closing_elements + current;
				
				String processedVector = modifyVector(current);
				
				// inject!!
				Starter.inject(processedVector);
				
				getAlertsWithTimeout(processedVector, false);
			} 
		}
		
	}
	
	public void injectWithinScriptTag(String closing_string_character, String tail) {	
		int i = 0;
		String[] _Inscript_vectors = new String[_script_payloads.length];
		
		for (String current : _script_payloads){
			_Inscript_vectors[i] = ( (closing_string_character == null) ? "" : closing_string_character + ";" ) +
					current +
					((tail == null) ? "" : tail);
			i++;
		}
		
		injector(_Inscript_vectors, null);
	}

	public void injectTag(String closing_string_character) {
		// try with no user interaction vectors
		injector(_vectors, ((closing_string_character == null) ? null : fuzzCases(closing_string_character)) );
		
		bar = new commandline.ProgressBar();
		int i = 0, size = _malicious_uris.length;
		
		Debug.print("Injecting user-interaction vectors...");
		// user interaction vectors
		for (String current : _malicious_uris){
			bar.update(i, size);
			i++;
			
			if ( (numberOfXSS <= limitNumberOfXSS - 1 && Starter.getOperation() == 1)
					|| Starter.getOperation() == 2 ) {

				int random_part = (new Random()).nextInt(100000);
				String linkText = fuzzCases(_dummy_link_text) + random_part;
				
				String anchor_vector = ((closing_string_character == null) ? "" : fuzzCases(closing_string_character)) +
									"<" + fuzzCases("a") +
									" " + fuzzCases("href") + "=" +
									current +
									">" +
									linkText;								
				
				Starter.inject(anchor_vector);
				
				List<WebElement> link =  Starter.getDriver().findElements(By.linkText(linkText));  
				List<WebElement> link2 =  Starter.getDriver().findElements(By.xpath("//a[@href='" + StringEscapeUtils.unescapeHtml(current) + "']"));  
				
				if (Starter.getOperation() == 1)
					getAlertsWithTimeout(null, false);
				
				if (link.size() != 0 && !link.get(0).getAttribute("href").startsWith("denied")){
					link.get(0).click();		
					
					getAlertsWithTimeout(anchor_vector, true);
				} else if (link2.size() != 0 && !link2.get(0).getAttribute("href").startsWith("denied")){
					link2.get(0).click();
					
					getAlertsWithTimeout(anchor_vector, true);
				}
			    
			}
		}
	}
	
	public void injectScriptTag(String closing_string_character) {
		int i = 0;
		String[] _script_vector = new String[_script_payloads.length];
		
		for (String current : _script_payloads){
			_script_vector[i] = ( (closing_string_character == null) ? "" : fuzzCases(closing_string_character) ) +
					"<" + 
					fuzzCases("script") +
					">" +
					current +
					"</" +
					fuzzCases("script") +
					">";
			i++;
		}
		
		injector(_script_vector, null);
	}
	
	public void injectDummyAttribute() {
		Starter.inject(_dummy_attribute);
		
		// check whether the attribute is breakable => does //*[@dummy_attr='dummy_attr_value'] exist?!
		this.areAttributesBreakable = Starter.getDriver().findElements(By.xpath("//*[@" +_dummyNodeAttributeKey.toLowerCase() + "='" + _dummyNodeAttributeValue + "']")).size() != 0;		
	}

	public void injectAttribute(String preamble) {
		bar = new commandline.ProgressBar();
		int i = 0, size = _script_payloads.length;
		
		for (String current : _script_payloads){
			bar.update(i, size);
			i++;
			
			if ( (numberOfXSS <= limitNumberOfXSS - 1 && Starter.getOperation() == 1)
					|| Starter.getOperation() == 2 ) {

				// '" onclick=payload//
				String breaking_vector = ((preamble != null)? preamble : "") + "?'\" " + fuzzCases("onclick") + "=" + current + "// ";
				
				Starter.inject(breaking_vector);
			
				if (Starter.getOperation() == 1)
					getAlertsWithTimeout(null, false);

				List<WebElement> injected_nodes = Starter.getDriver().findElements(By.xpath("//*[contains(@onclick,\"" + current + "\")]"));
				
				if (injected_nodes.size() != 0){
					for (WebElement e : injected_nodes)
						if (e.isDisplayed()){
							e.click();
							break;
						}
					
					getAlertsWithTimeout(breaking_vector, true);
				}
			}
		}
		
	}

	public void injectUntrustedURL_src() {
		injector(_malicious_uris, null);
	}

	public void injectUntrustedURL_href(String linkText) {
		bar = new commandline.ProgressBar();
		int i = 0, size = _malicious_uris.length;
		
		for (String current : _malicious_uris){
			bar.update(i, size);
			i++;
			
			if ( (numberOfXSS <= limitNumberOfXSS - 1 && Starter.getOperation() == 1)
					|| Starter.getOperation() == 2 ) {
					
				Starter.inject(current);
							
				List<WebElement> link =  Starter.getDriver().findElements(By.xpath("//a[@href='" + current + "']"));  
				List<WebElement> link2 = Starter.getDriver().findElements(By.linkText(linkText));
				
				if (Starter.getOperation() == 1)
					getAlertsWithTimeout(null, false);
				
				if (link.size() != 0 && !link.get(0).getAttribute("href").startsWith("denied")){
					link.get(0).click();
								
					getAlertsWithTimeout(current, true);
				} else if (link2.size() != 0 
							  && ( 
									StringEscapeUtils.unescapeHtml(link2.get(0).getAttribute("href")).startsWith("javascript") || 
									StringEscapeUtils.unescapeHtml(link2.get(0).getAttribute("href")).startsWith("data") || 
									StringEscapeUtils.unescapeHtml(link2.get(0).getAttribute("href")).startsWith("feed") || 
									StringEscapeUtils.unescapeHtml(link2.get(0).getAttribute("href")).startsWith("vbscript") )
								){
					link2.get(0).click();
						
					getAlertsWithTimeout(current, true);
				}	
			}
		}
	}

	public void breakElement(String post) {
		injector(_vectors, "'\">" + ( (post == null) ? "" : post ) );
	}

	public void checkReflectionContext(String injection) {
		
		// <tag>INJECTION</tag>
        if (Starter.getDriverFast().findElements(By.xpath("//*[text()[contains(., '" + injection + "')]]")).size() != 0){
        	
        	Debug.print("Reversing the XSS filter...");

        	checkElements(injection);
            
        // <tag attribute=INJECTION>
        } else if (Starter.getDriverFast().findElements(By.xpath("//attribute::*[contains(., '" + injection + "')]/..")).size() != 0){
        	List<WebElement> tmp = Starter.getDriverFast().findElements(By.xpath("//attribute::*[contains(., '" + injection + "')]/.."));
        	        	
        	if ( ( tmp.get(0).getAttribute("src") != null && 
        			tmp.get(0).getAttribute("src").contains(injection) ) ||
        		 ( tmp.get(0).getAttribute("href") != null &&
        			tmp.get(0).getAttribute("href").contains(injection) ) ){
        		
        		Debug.print("Reversing the XSS filter...");
        		
        		checkProtocols();
        	
        	}       	
        }
	}
	
	private void checkProtocols() {
		bar = new commandline.ProgressBar();
		LinkedList<String> allowedProtocols = new LinkedList<String>();
		int size = protocols.length, i = 0;
		String injection = "";
		
		for (String scheme : protocols){
			bar.update(i, size);
			i++;
			
			injection = scheme + ":" + ((scheme.equals("http") || scheme.equals("https") || scheme.equals("ftp")) ? "//" : "" ) + (new Random()).nextInt(100000);

			Starter.injectFast(injection);
				    			
			if (Starter.getDriverFast().findElements(By.xpath("//attribute::*[starts-with(., '" + injection + "')]/..")).size() != 0) {
				for (WebElement e : Starter.getDriverFast().findElements(By.xpath("//attribute::*[starts-with(., '" + injection + "')]/..")))
					if (e.getAttribute("href") != null && e.getAttribute("href").contains(injection) || 
							e.getAttribute("src") != null && e.getAttribute("src").contains(injection))
						allowedProtocols.add(scheme);
			}
		}
		
		Starter.setAllowedProtocols(allowedProtocols);
	}

	public void checkElements(String injection) {

		if (Starter.getParsedArgs().getDelayInterval() == 0){
			// 5 threads are started in order to address the reversion of the filter
			// if a delay is setted, then a sequential approach will be adopted
			int partitions_size = 25;
			int i = 0, j = 0;
			String[] partition_a = new String[partitions_size];
			String[] partition_b = new String[partitions_size];
			String[] partition_c = new String[partitions_size];
			String[] partition_d = new String[partitions_size];
			String[] partition_e = new String[htmlElements.length - (partitions_size * 4)];
			String attributes = "";
	
			for (String attribute : htmlAttributes){
				attributes += attribute + "=\"X\" ";
			}
			
			for (String element : htmlElements){
				if (j >= partitions_size && i <= 4*partitions_size)
					j = 0;
				
				if (i / partitions_size == 0) 
					partition_a[j] = element;
				if (i / partitions_size == 1) 
					partition_b[j] = element;
				if (i / partitions_size == 2) 
					partition_c[j] = element;
				if (i / partitions_size == 3) 
					partition_d[j] = element;
				if (i / partitions_size >= 4)
					partition_e[j] = element;
				
				i++;
				j++;
			}
	
			Thread reverser_a = new Reverser(partition_a, attributes, htmlAttributes, htmlElements.length);
			Thread reverser_b = new Reverser(partition_b, attributes, htmlAttributes, htmlElements.length);
			Thread reverser_c = new Reverser(partition_c, attributes, htmlAttributes, htmlElements.length);
			Thread reverser_d = new Reverser(partition_d, attributes, htmlAttributes, htmlElements.length);
			Thread reverser_e = new Reverser(partition_e, attributes, htmlAttributes, htmlElements.length);
			
			reverser_a.start();
			reverser_b.start();
			reverser_c.start();
			reverser_d.start();
			reverser_e.start();
	
			try {
				reverser_a.join();
				reverser_b.join();
				reverser_c.join();
				reverser_d.join();
				reverser_e.join();
			} catch (InterruptedException e) {
				Debug.printError("\nERROR: unable to terminate a thread");
			}
			
			Starter.setAllowedElements(Reverser.getAllowedElements());
		} else {
			reverse(injection);
		}
	}

	public void reverse(String injection){
		bar = new commandline.ProgressBar();
		String current;
		String attributes = null;
		String delimiter;
		HashMap<String, List<String>> allowedElements = new HashMap<String, List<String>>();
		int i = 0;
		
		for (String attribute : htmlAttributes){
			attributes += attribute + "=\"X\" ";
		}
		
		int size = htmlElements.length;
		
		for (String element : htmlElements){
			bar.update(i, size);
			i++;
			
			delimiter = injection + i;
			current = "<" + element + " " +
					attributes + ">" + "X" + "</" + element + ">";
			
			Starter.injectFast(delimiter + current + delimiter);
				    	
			Reverser.checkReversion(delimiter, element, htmlAttributes, allowedElements, Starter.getDriverFast());
		}
		
		Starter.setAllowedElements(allowedElements);
	}
	
	public void injectExpression() {
		bar = new commandline.ProgressBar();
		int i = 0;
		String[] _InStyle_vectors = new String[_expression_payloads.length * 2];
		int size = _InStyle_vectors.length;
		String[] head = { "color", "width", "-", "x", "xss" };
				
		Debug.print("INFO: Reflection context: style attribute - you should switch IE in compatibility mode in order to proceed...");
		Debug.print("Press Y and Enter once switched or N and Enter to quit.");
		
		String input = Debug.readLine();
		
		if (input != null && (input.equals("N") || input.equals("n"))){
			Starter.getDriver().quit();
			HaltHandler.quit_nok();
		}
		
		// <div style="color: UNTRUSTED_DATA;">
		for (String current : _expression_payloads){
			_InStyle_vectors[i] = Starter.randomColor() + ";" + head[(int) (Math.random() * head.length)] + ":" + current + ";";
			i++;
		}
		
		// <div style="background:url(UNTRUSTED_DATA);">
		for (String current : _expression_payloads){
			_InStyle_vectors[i] = "http://foo.com/boh);" + head[(int) (Math.random() * head.length)] + ":" + current + "/*";
			i++;
		}
		
		i = 0;
		for (String current : _InStyle_vectors){
			bar.update(i, size);
			i++;
			
			String current_url = Starter.getDriver().getCurrentUrl();
			
			Starter.inject(current);
			
			URL tmp = null;
			
			try {
				tmp = new URL(Starter.getDriver().getCurrentUrl());
			} catch (MalformedURLException e) {
				Debug.printError("ERROR: unable to select the current URL");
				HaltHandler.quit_nok();
			}
			
			if (tmp.getPath().endsWith("/0") && !tmp.equals(current_url)){
				log(current);
			}
		}
	}

	public void injectWithinEventHandler(String closing_string_character) {
		bar = new commandline.ProgressBar();
		int i = 0;
		int size =  _script_payloads.length;
		
		for (String current : _script_payloads){
			bar.update(i, size);
			i++;
			
			if ( (numberOfXSS <= limitNumberOfXSS - 1 && Starter.getOperation() == 1)
					|| Starter.getOperation() == 2 ) {
				
				String breaking_vector = ( (closing_string_character == null) ? "" : closing_string_character + ";" ) +
										current +
										"//";

				Starter.inject(breaking_vector);
			
				if (Starter.getOperation() == 1)
					getAlertsWithTimeout(null, false);
				
				List<WebElement> injected_nodes = Starter.getDriver().findElements(By.xpath("//*[contains(@onclick,\"" + current + "\")]"));
				
				if (injected_nodes.size() != 0){
					for (WebElement e : injected_nodes)
						if (e.isDisplayed()){
							e.click();
							break;
						}
					
					getAlertsWithTimeout(breaking_vector, true);
				}
			}
		}
	}
}
