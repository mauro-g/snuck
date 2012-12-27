/*
   snuck - command line arguments parser
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

package commandline;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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

	// delay between any injection
	private int delay = 0;
	
	// remote repository for attack vectors
	private String remoteXSSVectorsRepositoryURL;
		
	private String usage = 	"snuck [-start xmlconfigfile] -config xmlconfigfile -report htmlreportfile [-d #ms_delay] [-proxy IP:port]  [-chrome chromedriver]  [-ie iedriver] [-remotevectors URL] [-stop-first] [-reflected targetURL -p parameter_toTest] [-no-multi]";
	
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
			HaltHandler.quit_nok();
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
		                		Debug.printError("ERROR: " + reportfile + " already exists. ");
		                		Debug.printError("Press Y and Enter to overwrite it or N and Enter to exit");
		                		
		                		String input = Debug.readLine();
		                		
		                		if (input != null && (input.equals("Y") || input.equals("y"))){
			                		Debug.print("\n");
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
		            } else if (arg.equals("-reflected")){
		            	if (i != args.length - 1 && !args[i+1].startsWith("-")) {
		            		if (!args[i+1].startsWith("http://") && !args[i+1].startsWith("https://")){
		            			Debug.printError("ERROR: -reflected requires a target URL that starts with http(s?)://");
			                    HaltHandler.quit_nok();
		            		}
		            			
		            		targetURL = args[i+1];
		                	i++;
		            	} else {
		            		Debug.printError("ERROR: -reflected requires a target URL");
		                    HaltHandler.quit_nok();
		                }
		            } else if (arg.equals("-p")){
		            	if (i != args.length - 1 && !args[i+1].startsWith("-")) {
		            		targetParam = args[i+1];
		                	i++;
		            	} 
		            } else if (arg.equals("-no-multi")){
		            	delay = 1;
		            } else {
		            	Debug.printError(usage);
		                HaltHandler.quit_nok();
		            }
				}
			}
			
			if (targetURL != null && targetParam == null){
				Debug.printError("INFO: -reflected should be associated to an HTTP GET parameter (-p argument).\n" +
								 "\tThe injection will be positioned at the end of the current path in the form of: http://target.foo/injection.\n" +
								 "\tNote that supplied HTTP GET parameters won't be considered.");
                
				writeXmlFile(targetURL, "");    
			} else if (targetURL == null && targetParam != null){
				Debug.printError("ERROR: -p must be associated to a target URL (-reflected argument)");
                HaltHandler.quit_nok();
			} else if (targetURL != null && targetParam != null){
				if (configfile != null){
					Debug.printError("ERROR: -config cannot be used with -reflected");
	                HaltHandler.quit_nok();
				}
				
				writeXmlFile(targetURL, targetParam);
			}
			
			if (configfile == null || reportfile == null) {
				Debug.printError(usage);
				HaltHandler.quit_ok();
			} else {
				Debug.print("INFO: If [!] is showed during a test, then a bypass has been detected");
				Debug.print("INFO: You can stop the test through CTRL+C - if this happens, then a list of successful attack vectors will be printed in stdout\n");
				Debug.print("INFO: Starting...\n");
			}
		}
    }

	private void writeXmlFile(String targetURL, String targetParam){
		String temp_conf_file = XmlConfigReader.generateReflectedConfigFile(targetURL, targetParam);
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
			out.write(temp_conf_file);
			out.close();
		} catch (IOException e) {
			Debug.printError("ERROR: unable to write the XML config file");
            HaltHandler.quit_nok();
		}
	}
	
	private void showHelpMessage() {
		Debug.print();
		Debug.print("Usage: ");
		Debug.print();
		Debug.print(usage);
		Debug.print();
		Debug.print("Options:");
		Debug.print();
		Debug.print(" -start\t\t path to the login use case (XML file)");
		Debug.print(" -config\t path to the injection use case (XML file)");
		Debug.print(" -report\t report file name (html extension is required)");
		Debug.print(" -d\t\t delay (ms) between each injection");
		Debug.print(" -proxy\t\t proxy server (IP:port)");
		Debug.print(" -chrome\t perform a test with Google Chrome, instead of Firefox.\n\t\t It needs the path to the chromedriver");
		Debug.print(" -ie\t\t perform a test with Internet Explorer, instead of Firefox.\n\t\t Disable the XSS filter in advance");
		Debug.print(" -remotevectors\t use an up-to-date online attack vectors' source instead of the local one");
		Debug.print(" -stop-first\t stop the test upon a successful vector is detected");
		Debug.print(" -no-multi\t deactivate multithreading for the reverse engineering process - a sequential approach will be adopted");
		Debug.print(" -reflected\t perform a reflected XSS test (without writing the XML config file)");
		Debug.print(" -p\t\t HTTP GET parameter to inject (useful if -reflected is setted)");
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
}
