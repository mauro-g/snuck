/*
   snuck - simple web server
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
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import commandline.Debug;
import commandline.HaltHandler;


public class Server extends Thread {
	private static String response;
	private static HttpServer server;
	
	public static void configureServer(String response) {
		int portNumber = 9000;
		InetSocketAddress port = new InetSocketAddress(portNumber);
		try {
			server = HttpServer.create(port, 0);
		} catch (IOException e) {
			Debug.printError("ERROR: unable to start a local server at port " + portNumber);
            HaltHandler.quit_nok();
		}
	    server.createContext("/", new MyHandler());
	    server.setExecutor(null); 
	    Server.response = response;
	    server.start();
	}
		
	static class MyHandler implements HttpHandler {
		public void handle(HttpExchange t) throws IOException {
			Headers h = t.getResponseHeaders();
		    h.add("Content-Type", "text/html");
			t.sendResponseHeaders(200, response.length());
	        OutputStream os = t.getResponseBody();
	        os.write(response.getBytes());
	        os.close();
	    }
	}
	
	public static void stopServer(){
		if (server != null)
			server.stop(MAX_PRIORITY);
	}
}
