/*
   snuck - intercepting proxy
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

import java.io.IOException;
import java.net.UnknownHostException;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.browsermob.proxy.ProxyServer;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import commandline.Debug;
import commandline.HttpRequestParser;

public class Proxy {
	/*
	 *  @browser
	 *  	0 -> htmlunit
	 *  	1 -> firefox
	 *  	2 -> chrome (not done yet)
	 *  	3 -> IE  (not done yet)
	 */
	@SuppressWarnings("deprecation")
	public static WebDriver createProxedDriver(final HttpRequestParser http_request, int browser){
		// start the proxy
		int port = 0;
		ProxyServer server = new ProxyServer(port);
		try {
			server.start();
		} catch (Exception e) {
			Debug.printError("\nERROR: unable to start browsermob proxy server");
			Starter.detectedError();
		}

		org.openqa.selenium.Proxy proxy = null;
		try {
			proxy = server.seleniumProxy();
		} catch (UnknownHostException e) {
			Debug.printError("\nERROR: unable to get the selenium proxy object");
			Starter.detectedError();
		}

		// configure it as a desired capability
		DesiredCapabilities capabilities = new DesiredCapabilities();
		FirefoxProfile profile = new FirefoxProfile();
        profile.setAcceptUntrustedCertificates(true);
        profile.setAssumeUntrustedCertificateIssuer(true);
        profile.setPreference("network.proxy.http", "localhost");
        profile.setPreference("network.proxy.http_port", port);
        profile.setPreference("network.proxy.ssl", "localhost");
        profile.setPreference("network.proxy.ssl_port", port);
        profile.setPreference("network.proxy.type", 1);
        profile.setPreference("network.proxy.no_proxies_on", "");
        profile.setProxyPreferences(proxy);
        capabilities.setCapability(FirefoxDriver.PROFILE,profile);
        capabilities.setCapability(CapabilityType.PROXY, proxy);

        server.addRequestInterceptor(new HttpRequestInterceptor() {
 			@Override
            public void process(HttpRequest request, org.apache.http.protocol.HttpContext context) throws HttpException, IOException {
                 if (request instanceof org.apache.http.client.methods.HttpUriRequest) {
                     org.apache.http.client.methods.HttpUriRequest r = (org.apache.http.client.methods.HttpUriRequest) request;
                     // org.apache.http.HttpHost host = (org.apache.http.HttpHost) context.getAttribute("http.target_host");
                 
                     for (String h : http_request.getHeaders().keySet()){
                    	 if (h.equals("Content-Length")){
                    		 continue;
                    	 }
                    	 if (r.getFirstHeader(h) != null){
	                    	 r.removeHeaders(h);
	                    	 r.addHeader(h, http_request.getHeader(h));
                    	 } else {
	                    	 r.addHeader(h, http_request.getHeader(h));
                    	 }
                     }
                  
                     if (!http_request.getMethod().equals("POST") || http_request.getParameters() == null)
	                     for (Header j : r.getAllHeaders()){
	                    	 if (http_request.getHeader(j.getName()) == null){
	                    		 r.removeHeaders(j.getName());
	                    	 }
	                     }

                     /*
                     System.out.println("Host = " + host.getHostName());
                     System.out.println("Path = " + r.getURI().getPath());
                     */
                 }
             }
         });	
        
        WebDriver driver = null;
        
        if (browser == 0) {
        	driver = new HtmlUnitDriver(capabilities);
            driver = Starter.setThrowExceptionOnScriptError(driver);
        } else
        	// Chrome and IE are not yet supported
        	driver = new FirefoxDriver(capabilities);

		server.newHar("injection");
		return driver;
	}
}
