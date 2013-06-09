/*
   snuck - HTTP request parser
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

import java.io.*;
import java.net.URLDecoder;
import java.util.*;

public class HttpRequestParser {

	private BufferedReader reader;
	private String method, url;
	private HashMap<String, String> headers;
	// useful for POST requests
	private HashMap<String, String> data;
	private String file_name;
	
	private String cannot_read_error = "ERROR: unable to read the HTTP request file";
	private String no_host_error = "ERROR: there is no Host header in the HTTP request";

	public HttpRequestParser(String http_request_filename) {
		InputStream is = null;
		
		try {
			is = new FileInputStream(http_request_filename);
		} catch (FileNotFoundException e) {
			Debug.printError("ERROR: " + http_request_filename + " not found");
    		HaltHandler.quit_nok();
		}
		
		file_name = http_request_filename;
		reader = new BufferedReader(new InputStreamReader(is));
		headers = new HashMap<String, String>();
		data = new HashMap<String, String>();
		parse();
	}
	
	public int parse() {
		String firstline = null;
		String basic[];
	    
		try {
			firstline = reader.readLine();
		} catch (IOException e) {
			Debug.printError(cannot_read_error);
    		HaltHandler.quit_nok();
		}
	    if (firstline == null || firstline.length() == 0 || firstline.split("\\s").length != 3) return -1;
	  
	    basic = firstline.split("\\s");
	    method = basic[0];
    	url = basic[1];
    	parseHeaders();

    	try {
			reader.close();
		} catch (IOException e) {
			Debug.printError("ERROR: unable to close the HTTP request file");
    		HaltHandler.quit_nok();
		}
	    return 1;
	}
	
	private void parseHeaders() {
		String line = null;
		
		try {
			line = reader.readLine();
		} catch (IOException e) {
			Debug.printError(cannot_read_error);
    		HaltHandler.quit_nok();
		}
		
	    while (line != null && !line.equals("")) {
	    	if (line.split(":").length == 2){
	    		String [] tmp = line.split(":");
	    		String name = tmp[0];
	    		String value = tmp[1];
		        headers.put(name, value.trim());
	    	}
	    	
	    	try {
				line = reader.readLine();
			} catch (IOException e) {
				Debug.printError(cannot_read_error);
	    		HaltHandler.quit_nok();
			}
	    }
	    
	    if (!headers.containsKey("host") && !headers.containsKey("Host")){
	    	Debug.printError(no_host_error);
    		HaltHandler.quit_nok();
	    } else {
	    	String host = headers.get("Host") != null ? headers.get("Host") : headers.get("host");
	    	url = "http://" + host + url;
	    }
	    
	    if (this.getMethod().equals("POST"))
	    	parsePostParameters();
	    else
	    	parseGetParameters();
	}
	
	private void parseGetParameters() {
		if (url.split("\\?").length > 1){
			String[] params = url.split("\\?")[1].split("&");  
			
			for (String param : params) {  
				String name = param.split("=")[0];  
			    String value = null;
			    if (param.split("=").length == 2)
			      	value = param.split("=")[1];  
			        
			    if (!name.equals(""))
			        	data.put(name, ( (value == null) ? "" : value ) );  
			    }  
		}
	}

	private void parsePostParameters() {
		String line = null;
		
		try {
			line = reader.readLine();
		} catch (IOException e) {
			Debug.printError(cannot_read_error);
    		HaltHandler.quit_nok();
		}
		
		String[] tmp = line.split("&");
		String[] param;
		
		for (int i = 0; i < tmp.length; i++){
			param = tmp[i].split("=");
			
			if (param.length == 2){
				try {
					data.put(param[0], URLDecoder.decode(param[1], "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					Debug.printError("ERROR: unable to decode the POST parameters' values supplied in the HTTP request file");
		    		HaltHandler.quit_nok();
				}
			} else if (param.length == 1){
				data.put(param[0], "");
			}
		}
	}

	public String getMethod() {
		return this.method;
	}
	
	public String getHeader(String key) {
		if (headers != null)
			return headers.get(key);
		else return null;
	}
	
	public String getParameter(String key) {
		if (data != null)
			return data.get(key);
		else return null;
	}
	
	public String getTargetURL() {
		if (url != null)
			return url;
		else return null;
	}
	
	public HashMap<String, String> getParameters() {
		return data;
	}
	
	public HashMap<String, String> getHeaders() {
		return headers;
	}	
	
	public String getFileName(){
		return file_name;
	}
}