/*
   snuck - html report generator
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

package report;

import java.io.*;
import java.util.List;
import java.util.Map;

import commandline.Debug;
import commandline.HaltHandler;

public class ReportGenerator {
	 public static void generateReport(String reportFileName, String XMLconfigFile, String InUseBrowser, 
			 String reflectionContext, int operation, Map<String, List<String>> allowedElements, 
			 List<String> allowedProtocols, List<String> detectedXSSVectors, 
			 boolean weakFilter, boolean brokenPage, int method) {
		 
		 try{
			 FileWriter fstream = new FileWriter(reportFileName);
			 BufferedWriter out = new BufferedWriter(fstream);
			 out.write("<!DOCTYPE html>");
			 out.write("<html>");
			 out.write("<head>" +
			 "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />" +
			 "<title>Test results</title>");
			 
			 if (method == 1){
				 out.write("<script>" +
				 "var escape_flag = 0;" +
				 "function flipUrlEncoding(){" +
				 "	var z = document.getElementsByName('vector');" +
				 "	var i;" +
				 "	if (escape_flag == 0){" +
				 "		for (i = 0; i < z.length; i++){" +
			     "			z[i].innerHTML = encodeURIComponent(z[i].innerHTML.replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&amp;/g,'&')).replace(/%5BCRLF%5D/g,'%0D%0A');" +
				 "		}" +
				 "		escape_flag = 1;" +
				 "	}else {" +
				 "	   for (i = 0; i < z.length; i++){" +
				 "			z[i].innerHTML = decodeURIComponent(z[i].innerHTML.replace(/%0D%0A/g,'%5BCRLF%5D')).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');" +
				 "		}" +
				 "		escape_flag = 0;" +
				 "	}" +
				 "}" +
				 "</script>");
			 }
			 
			 out.write("<style type=\"text/css\">" +
			 			"body" +
				 		"{ 	line-height: 1.4em; }" +
						"" +
						"#hor-minimalist-b" +
						"{" +
						"font-family: \"Lucida Sans Unicode\", \"Lucida Grande\", Sans-Serif;" +
						"font-size: 12px;" +
						"background: #fff;" +
						"width: 750px;" +
						"border-collapse: collapse;" +
						"text-align: left;" +
						"}" +
						"" +
						"#hor-minimalist-b th" +
						"{" +
						"font-size: 14px;" +
						"font-weight: normal;" +
						"color: #039;" +
						"padding: 10px 8px;" +
						"border-bottom: 2px solid #6678b1;" +
						"}" +
						"" +
						"#hor-minimalist-b td" +
						"{" +
						"border-bottom: 1px solid #ccc;" +
						"color: #669;" +
						"padding: 6px 8px;" +
						"}" +
						"" +
						"#hor-minimalist-b tbody tr:hover td" +
						"{" +
						"color: #009;" +
						"}" +
						"#left {" +
						"float:left;" +
						"width:800px;" +
						"}" +
						"" +
						"#rigth {" +
						"float:right;" +
						"}" +
						"" +
						"#wrap {" +
						"width:100%;" +
						"margin:0 auto;" +
						"}" +
						"" +
						"div.redsquare {" +
						"align: right;" +
						"height: 17px;" +
						"width: 17px;" +
						"background-color: #FF0000;" +
						"}" +
						"" +
						"div.redsquare * {" +
						"display: none;" +
						"}" +
						"" +
						"div.orangesquare {" +
						"align: right;" +
						"height: 17px;" +
						"width: 17px;" +
						"background-color: #FFA500;" +
						"}" +
						"" +
						"div.orangesquare * {" +
						"display: none;" +
						"}" +
						"</style>" +
			 "</head>" +
			 "<body>");
			 			 
			 out.write("" +
			 		"<div id=\"wrap\">" +
			 		"<div id=\"left\">" +
			 		"<table id=\"hor-minimalist-b\">" +
					 "  <thead>" +
					 "   	<tr>" +
					 "       	<th scope=\"col\">Reflection context</th>" +
					 "      </tr>" +
					 " </thead>" +
					 " <tbody>" +
					 "<tr>" +
					 "<td>");
					 if (reflectionContext != null)
						 out.write(reflectionContext.
								 replace("&", "&amp").
								 replace("<", "&lt;").
								 replace(">", "&gt;").
								 replace("UNTRUSTED DATA", "<font color=\"red\">UNTRUSTED_DATA</font>"));
			  out.write("</td>" +
					 "</tr>" +
					 "<tbody>" +
					 "</table>" +
					 "<br />");
			 
			 out.write("" +
				 		"<table id=\"hor-minimalist-b\">" +
						 "  <thead>" +
						 "   	<tr>" +
						 "       	<th scope=\"col\">Detected operation</th>" +
						 "      </tr>" +
						 " </thead>" +
						 " <tbody>" +
						 "<tr>" +
						 "<td>" +
						 ((operation == 1) ? "INSERT" : "UPDATE") +
						 "</td>" +
						 "</tr>" +
						 "<tbody>" +
						 "</table>" +
						 "<br />");
			 
			 if (allowedElements.size() != 0) {
				 out.write("<table id=\"hor-minimalist-b\">" +
						 "  <thead>" +
						 "   	<tr>" +
						 "       	<th scope=\"col\">Allowed Tags</th>" +
						 "          <th scope=\"col\">Allowed Attributes</th>" +
						 "      </tr>" +
						 " </thead>" +
						 " <tbody>");
				 
				 for (String key : allowedElements.keySet()){
					 
					 out.write("<tr>");
					 out.write("<td>" + key + "</td>");
					 
					 out.write("<td>");
		            	if (allowedElements.get(key) != null) {
			            	for (String t : allowedElements.get(key)){
			            		 out.write(t+ " ");
			            	}
		            	} else 
		            		out.write("-");
		            out.write("</td>");
		            out.write("</tr>");
		         }
				 	
				out.write("</tbody>" +
				"</table>");
			} else {
					            
	            if (allowedProtocols.size() == 0){
		            out.write("<table id=\"hor-minimalist-b\">" +
							"   <thead>" +
							"	  	<tr>" +
							"       	<th scope=\"col\">Use Case</th>" +
							"        </tr>" +
							"   </thead>" +
							"   <tbody>" +
							"<tr>" +
							"<td>" +
							"<textarea style=\"width: 100%; height: 500px;\">");
									
							readConfigFile(XMLconfigFile, out);
							    	
							out.write("</textarea>" +
									"</td>" +
									"</tr>" +
									"</tbody>" +
							"</table>");
		        }
			}
			 		 
			if (allowedProtocols.size() != 0) {
				 out.write("<table id=\"hor-minimalist-b\">" +
						 "  <thead>" +
						 "   	<tr>" +
						 "       	<th scope=\"col\">Allowed Protocols (schemes)</th>" +
						 "      </tr>" +
						 " </thead>" +
						 " <tbody>");
				 
				 for (String prot : allowedProtocols) {
					out.write("<tr>");
					out.write("<td>" + prot + "</td>");
		            out.write("</tr>");
		         }
				 	
				out.write("</tbody>" +
				"</table>");
			}	
			
			out.write("</div>");
			out.write("<div id=\"right\">");

			out.write("<table id=\"hor-minimalist-b\">" +
					"   <thead>" +
					"	  	<tr>" +
					"       	<th scope=\"col\">Web Browser</th>" +
					"        </tr>" +
					"   </thead>" +
					"   <tbody>" +
					"<tr>" +
					"<td>" +
					InUseBrowser + 
					"</td>" +
					"</tr>" +
					"</tbody>" +
					"</table>" +
					"<br />");
			
			out.write("<table id=\"hor-minimalist-b\">" +
			"   <thead>" +
			"	  	<tr>" +
			"       	<th scope=\"col\">Detected XSS " + (method == 1 ? "[<a href=\"#\" onclick=\"flipUrlEncoding();\">URL (e|de)ncode vectors</a>]" : "") + "</th>" +
			"        </tr>" +
			"   </thead>" +
			"   <tbody>");
					
			if (detectedXSSVectors.size() != 0){
				for (String t : detectedXSSVectors){
					out.write("<tr>");
					out.write("<td name=\"vector\">" + t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\r\n", "[CRLF]") + "</td>");
					out.write("</tr>");
				}
				if (weakFilter) {
					out.write("<tr>");
					out.write("<td style=\"text-align: right;\"><div class=\"redsquare\"><p>.</p></div>" +
							"Note: the tested XSS filter is too weak. Many other attack vectors would also work!" +
							"</td>");
					out.write("</tr>");
				}		
				if (brokenPage){
					out.write("<tr>");
					out.write("<td style=\"text-align: right;\"><div class=\"orangesquare\"><p>.</p></div>" +
							"Note: the last injection broke the reflection page, therefore the test had to be stopped." +
							"</td>");
					out.write("</tr>");
				}
			} else {
				out.write("<tr>");
				out.write("<td>No XSS detected!</td>");
				out.write("</tr>");
				
				if (brokenPage){
					out.write("<tr>");
					out.write("<td style=\"text-align: right;\"><div class=\"orangesquare\"><p>.</p></div>" +
							"Note: the last injection broke the reflection page, therefore the test had to be stopped." +
							"</td>");
					out.write("</tr>");
				}
			}
			    	
			out.write("</tbody>" +
			"</table>" +
			"<br />");
					
			if (allowedElements.size() != 0 || allowedProtocols.size() != 0) {
				out.write("<table id=\"hor-minimalist-b\">" +
						"   <thead>" +
						"	  	<tr>" +
						"       	<th scope=\"col\">Config file</th>" +
						"        </tr>" +
						"   </thead>" +
						"   <tbody>" +
						"<tr>" +
						"<td>" +
						"<textarea style=\"width: 100%; height: 400px;\">");
								
						readConfigFile(XMLconfigFile, out);
						    	
						out.write("</textarea>" +
								"</td>" +
								"</tr>" +
								"</tbody>" +
						"</table>");
			}

			out.write("</div>" +
					"</div>" +
					"</body>" +
					"</html>");
			
			out.close();

		 }catch (Exception e){
			 Debug.printError("ERROR: report generation failure");
			 HaltHandler.quit_nok();
		 }
	 }

	private static void readConfigFile(String xMLconfigFile, BufferedWriter out){
		 try{
			 FileInputStream fstream = new FileInputStream(xMLconfigFile);
			 DataInputStream in = new DataInputStream(fstream);
			 BufferedReader br = new BufferedReader(new InputStreamReader(in));
			 String strLine;

			 while ((strLine = br.readLine()) != null) {
				out.write(strLine + "\n");
			 }

			  in.close();
		}catch (Exception e){
			Debug.printError("ERROR: unable to read the xml config file");
			HaltHandler.quit_nok();
		}
	}
}