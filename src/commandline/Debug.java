/*
   snuck - IO operations
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Debug {
	public static void print(){
		System.out.println();
	}
	
	public static void print(String message){
		System.out.println(message);
	}
	
	public static void printError(String message){
		System.err.println(message);
	}
	
	public static String readLine(){
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	    String input = null;
	   
	    try {
	    	input = br.readLine();
	    } catch (IOException e) {
	        Debug.printError("ERROR: " + e.getMessage());
	        HaltHandler.quit_nok();
	    }
	    
	    return input;
	}

	public static boolean askForContext(String reflContext) {
		Debug.print("\nINFO: detected reflection context: " + reflContext);
		Debug.print("Do you want to consider this one for specializing the injection? [Y/n]");
		String input = Debug.readLine();
		
		if (input != null && !input.equals("N") && !input.equals("n"))
    		return true;
		
		return false;
	}
}
