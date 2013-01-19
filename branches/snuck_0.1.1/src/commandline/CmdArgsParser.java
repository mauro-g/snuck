/*
   snuck - command line arguments parser
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

package commandline;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xmlparser.XmlConfigReader;

public class CmdArgsParser {
	// xml config file for login use case
	private String start_configfile;
	
	// xml config file for injection use case
	private String configfile;
	
	// report file
	private String reportfile;
	
	// IP:port
	private String proxyIP_port;
	
	// path to the Chromedriver file 
	private String chromedriver_file;
	
	// path to the IEdriver file 
	private String iedriver_file;
		
	private boolean IE_enabled = false;
	
	private boolean stop_first_positive = false;

	private boolean show_results = false;

	private String cookie;

	private HttpRequestParser http_request;

	private String post_parameters;

	// delay between any injection
	private int delay = 0;
	
	// remote repository for attack vectors
	private String remoteXSSVectorsRepositoryURL;
		
	private String usage = 	"java -jar snuck.jar [options]";
	
	public CmdArgsParser(String[] args){
		this.parseArguments(args);
	}
	
	/**
	 * Parses the arguments from the command line and populates the global parameters of the current CmdArgsParser object
	 */
	public void parseArguments(String[] args) {
		int i = 0;
		String arg;
        String targetURL = null;
        String targetParam = null;
        
		if (args.length == 0){
			showHelpMessage();
			HaltHandler.quit_ok();
		} else {
			for (String tmp : args){
				if (tmp.equals("-help") || tmp.equals("--help") || tmp.equals("-h") || tmp.equals("--h")) {
	            	showHelpMessage();
					HaltHandler.quit_ok();
				}
			}
			
			for (; i < args.length; i++) {
				arg = args[i];
					
				if (arg.startsWith("-")){
					if (arg.equals("-config")) {
		                if (i != args.length - 1 && !args[i+1].startsWith("-")){
		                	configfile = args[i+1];
		                	i++;
		                } else {
		                	Debug.printError("ERROR: -config requires a filename");
			                HaltHandler.quit_nok();
			            }
		            } else if (arg.equals("-report")) {
		                if (i != args.length - 1 && !args[i+1].startsWith("-")) {
		                	reportfile = args[i+1];
		                	i++;
		                	
		                	File f = new File(reportfile);
		                	if (f.exists()) {
		                		Debug.printError("\nERROR: " + reportfile + " already exists. Do you want to overwrite it? [Y/n]");
		                		
		                		String input = Debug.readLine();
		                		
		                		if (input != null && (input.equals("Y") || input.equals("y") || input.equals(""))){
		                			continue;
		                		} else 
		                			HaltHandler.quit_nok();
		                	} 
		                } else {
		                	Debug.printError("ERROR: -report requires a filename");
			                HaltHandler.quit_nok();
			            }
		            } else if (arg.equals("-d")) {
		            	if (i != args.length - 1 && !args[i+1].startsWith("-")) {
		            		
		            		try {
		                		delay = Integer.valueOf(args[i+1]);
		            		} catch (Exception e){
		            			 Debug.printError("ERROR: provide an integer value for the delay");
					             HaltHandler.quit_nok();
		            		}
		            		
		                	i++;
		            	} else {
		            		Debug.printError("ERROR: -d requires an integer (ms)");
		                    HaltHandler.quit_nok();
		                }
		            } else if (arg.equals("-start")) {
		            	if (i != args.length - 1 && !args[i+1].startsWith("-")) {
		                	start_configfile = args[i+1];
		                	i++;
		            	} else {
		            		Debug.printError("ERROR: -start requires a filename");
		                    HaltHandler.quit_nok();
		                }
		            }  else if (arg.equals("-proxy")) {
		            	if (i != args.length - 1 && !args[i+1].startsWith("-")) {
		                	proxyIP_port = args[i+1];
		                	i++;
		                	
		                	if (proxyIP_port.contains(":")){
			                	String ipAddress_Pattern = 
			                				"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
			                				"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
			                				"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
			                				"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
			                	Pattern pattern = Pattern.compile(ipAddress_Pattern);
			                	Matcher matcher = pattern.matcher(proxyIP_port.substring(0, proxyIP_port.indexOf(":")));
			                	
			                	if (!matcher.matches()){
			                		Debug.printError("ERROR: invalid IP address");
			                		HaltHandler.quit_nok();
			                	}
			                	
			                	String port = proxyIP_port.substring(proxyIP_port.indexOf(":") + 1);
			                	
			                	try {
			                		Integer.parseInt(port);
			                	} catch (Exception e){
			                		Debug.printError("ERROR: invalid port number");
			                		HaltHandler.quit_nok();
			                	}
		                	} else {
		                		Debug.printError("ERROR: invalid syntax for proxy address");
		                		HaltHandler.quit_nok();
		                	}
		                	 
		            	} else {
		            		Debug.printError("ERROR: -proxy requires a proxy server (IP:port)");
		                    HaltHandler.quit_nok();
		                }
		            	
		            } else if (arg.equals("-chrome")) {
		            	if (i != args.length - 1 && !args[i+1].startsWith("-")) {
		                	chromedriver_file = args[i+1];
		                	i++;
		            	} else {
		            		Debug.printError("ERROR: -chrome requires the path to chromedriver");
		                    HaltHandler.quit_nok();
		                }
		            } else if (arg.equals("-ie")) {
		            	if (i != args.length - 1 && !args[i+1].startsWith("-")) {
		                	iedriver_file = args[i+1];
		                	i++;
		            		IE_enabled = true;
		            	} else {
		            		Debug.printError("ERROR: -ie requires the path to IEdriver");
		                    HaltHandler.quit_nok();
		                }
		            } else if (arg.equals("-remotevectors")) {
		            	if (i != args.length - 1 && !args[i+1].startsWith("-")) {
		            		remoteXSSVectorsRepositoryURL = args[i+1];
		                	i++;
		            	} else {
		            		Debug.printError("ERROR: -remotevectors requires a repository URL");
		                    HaltHandler.quit_nok();
		                }
		            } else if (arg.equals("-stop-first")) {
		            	stop_first_positive = true;
		            } else if (arg.equals("-u")){
		            	if (i != args.length - 1 && !args[i+1].startsWith("-")) {
		            		if (!args[i+1].startsWith("http://") && !args[i+1].startsWith("https://")){
		            			Debug.printError("ERROR: -u requires a target URL that starts with http(s?)://");
			                    HaltHandler.quit_nok();
		            		}
		            			
		            		targetURL = args[i+1];
		                	i++;
		            	} else {
		            		Debug.printError("ERROR: -u requires a target URL");
		                    HaltHandler.quit_nok();
		                }
		            } else if (arg.equals("-p")){
		            	if (i != args.length - 1 && !args[i+1].startsWith("-")) {
		            		targetParam = args[i+1];
		                	i++;
		            	} 
		            } else if (arg.equals("-no-multi")){
		            	delay = 1;
		            } else if (arg.equals("-r")){
		            	if (i != args.length - 1 && !args[i+1].startsWith("-")) {      			
		            		http_request = new HttpRequestParser(args[i+1]);
		                	i++;
		            	} else {
		            		Debug.printError("ERROR: -r requires a file containing the HTTP request");
		                    HaltHandler.quit_nok();
		                }
		            } else if (arg.equals("-show-vectors")){
		            	show_results = true;
		            } else if (arg.equals("-data")){
		            	if (i != args.length - 1 && !args[i+1].startsWith("-")) {
		            		post_parameters = args[i+1];
		                	i++;
		            	} else {
		            		Debug.printError("ERROR: -data requires POST parameters");
		                    HaltHandler.quit_nok();
		                }
		            } else if (arg.equals("-cookie")){
		            	if (i != args.length - 1 && !args[i+1].startsWith("-")) {
		            		cookie = args[i+1];
		                	i++;
		            	} else {
		            		Debug.printError("ERROR: -cookie requires a valid cookie");
		                    HaltHandler.quit_nok();
		                }
		            }  else {
		            	Debug.printError(usage);
		                HaltHandler.quit_nok();
		            }
				}
			}
						
			if (http_request != null){
				if (configfile != null || start_configfile != null || proxyIP_port != null || cookie != null || targetURL != null || post_parameters != null){
					Debug.printError("ERROR: -r cannot be used with -config or -start or -proxy or -cookie or -u or -data");
	                HaltHandler.quit_nok();
				} else {
					if (http_request.getParameter(targetParam) != null)
						writeXmlFileFromHttpRequest(http_request, targetParam);
					else {
						Debug.printError("ERROR: the target parameter is not present in the HTTP request");
		                HaltHandler.quit_nok();
					}
				}
			}
			
			if (post_parameters != null){
				if (targetURL == null || targetParam == null){
					Debug.printError("ERROR: -data must be associated to a target url (-u) and a target parameter (-p)");
	                HaltHandler.quit_nok();
				} else {						
					writeXmlFileFromPostParameters(post_parameters, targetURL, targetParam);
				}
			}
			
			if (targetURL != null && targetParam == null){
				Debug.printError("INFO: -u should be associated to an HTTP GET parameter (-p argument).\n" +
								 "\tThe injection will be positioned at the end of the current path in the form of: http://target.foo/injection.\n" +
								 "\tNote that supplied HTTP GET parameters won't be considered.");
                
				writeXmlFile(targetURL, "");    
			} else if (targetURL == null && targetParam != null){
				if (http_request == null) {
					Debug.printError("ERROR: -p must be associated to a target URL (-u argument)");
	                HaltHandler.quit_nok();
				}
			} else if (targetURL != null && targetParam != null && post_parameters == null){
				if (configfile != null){
					Debug.printError("ERROR: -config cannot be used with -u");
	                HaltHandler.quit_nok();
				}
				
				URL target = null;
				LinkedList<String> getParams = null;
				String[] split;
				boolean isPresent = false;
				
				try {
					target = new URL(targetURL);
				} catch (MalformedURLException e) {
					Debug.printError("ERROR: unable to parse the supplied URL (" + targetURL + ")");
		            HaltHandler.quit_nok();
				}
				
				getParams = XmlConfigReader.getQueryMap(target.getQuery());
				
				for (String tmp : getParams){
					split = tmp.split("=");
					if (split[0].equals(targetParam)){
						isPresent = true;
						int pos = targetURL.indexOf(split[0]+"="+split[1]);
						int length = (split[0]+"="+split[1]).length();
						
						targetURL = targetURL.substring(0, pos) + 
									targetURL.substring(pos+length);
						break;
					}
				}
				
				if (!isPresent){
					Debug.printError("ERROR: the target parameter is not present in the target URL");
		            HaltHandler.quit_nok();
				}
				
				writeXmlFile(targetURL, targetParam);
			}
			
			if (configfile == null || reportfile == null) {
				Debug.printError(usage);
				HaltHandler.quit_ok();
			} else {
				Debug.print("INFO: If [!] is shown during a test, then a bypass has been detected");
				Debug.print("INFO: You can stop the test through CTRL+C - if this happens, then a list of successful attack vectors will be printed in stdout\n");
				Debug.print("INFO: Starting...\n");
			}
		}
    }

	private void writeXmlFileFromPostParameters(String post_parameters, String targetURL, String targetParam) {
		String config = XmlConfigReader.generateConfigFileFromPostParameter(post_parameters, targetURL, targetParam);
		writeFile(config);
	}

	private void writeXmlFileFromHttpRequest(HttpRequestParser request, String targetParam) {
		String config = XmlConfigReader.generateConfigFileFromHTTPRequest(request, targetParam);
		writeFile(config);
	}

	private void writeXmlFile(String targetURL, String targetParam){
		String config = XmlConfigReader.generateReflectedConfigFile(targetURL, targetParam);
		writeFile(config);
	}
	
	private void writeFile(String config){
		DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy-HH-mm-ss");
		Date date = new Date();
		FileWriter fstream = null;
		BufferedWriter out =  null;
		configfile = "config_" + dateFormat.format(date).toString() + ".xml";
		
		try {
			fstream = new FileWriter(configfile);
		} catch (IOException e) {
			Debug.printError("ERROR: unable to create the XML config file");
            HaltHandler.quit_nok();
		}
		
		out = new BufferedWriter(fstream);
		
		try {
			out.write(config);
			out.close();
		} catch (IOException e) {
			Debug.printError("ERROR: unable to write the XML config file");
            HaltHandler.quit_nok();
		}
	}
	
	private void showHelpMessage() {
		Debug.print();
		Debug.print(" snuck - automatic XSS filter bypass tool");
		Debug.print(" https://code.google.com/p/snuck/");
		Debug.print();
		Debug.print("Usage: " + usage);
		Debug.print();
		Debug.print("Options:");
		Debug.print();
		Debug.print(" -start\t\t path to the login use case (XML file)");
		Debug.print(" -config\t path to the injection use case (XML file)");
		Debug.print(" -report\t report file name (html extension required)");
		Debug.print(" -u\t\t target URL");
		Debug.print(" -p\t\t target parameter");
		Debug.print(" -cookie\t authentication cookie");
		Debug.print(" -r\t\t load HTTP request from a file");
		Debug.print(" -data\t\t data string to be sent through POST");
		Debug.print(" -proxy\t\t proxy server (IP:port)");
		Debug.print(" -d\t\t delay (ms) between each injection");
		Debug.print(" -chrome\t use Google Chrome as testing browser.\n\t\t It needs the path to the chromedriver");
		Debug.print(" -ie\t\t use Internet Explorer as testing browser.\n\t\t It needs the path to the IEDriverServer");
		Debug.print(" -remotevectors\t use your online attack vectors' repository instead of the local one");
		Debug.print(" -stop-first\t stop the test upon a successful vector is detected");
		Debug.print(" -show-vectors\t attack vectors will be printed in stdout before quitting");
		Debug.print(" -no-multi\t deactivate multithreading for the reverse engineering process");
		Debug.print(" -help\t\t show this help menu");
	}
		
	public String getProxyInfo() {
		return proxyIP_port;
	}
	
	public String getStartConfigfileName() {
		return start_configfile;
	}
	
	public String getConfigfileName() {
		return configfile;
	}
	
	public String getReportfileName() {
		return reportfile;
	}
	
	public int getDelayInterval() {
		return delay;
	}

	public String getChromeDriverPath() {
		return chromedriver_file;
	}
	
	public boolean getEnabledIE() {
		return IE_enabled;
	}
	
	public String getAttackVectorsRepositoryURL() {
		return remoteXSSVectorsRepositoryURL;
	}

	public String getIEDriverPath() {
		return iedriver_file;
	}
	
	public boolean getStopFirstPositive() {
		return stop_first_positive;
	}
	
	public String getCookie() {
		return cookie;
	}
	
	public boolean getShowVectors() {
		return show_results;
	}
	
	public HttpRequestParser getHttpRequest(){
		return http_request;
	}
	
	public String getPostParameters(){
		return post_parameters;
	}
}