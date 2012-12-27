/*
   snuck - shutdown operations
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

import java.util.List;

import org.openqa.selenium.remote.UnreachableBrowserException;

import core.Starter;

public class HaltHandler extends Thread {
	public void run() { 
		
		Debug.print("\n\nShutting down..."); 
		
		List<String> toPrint = Starter.getDetectedXSSVector();
		
		if (toPrint != null && toPrint.size() != 0){
			Debug.print("Successful attack vectors:"); 

			for(String tmp : toPrint){
				Debug.print(tmp); 
			}
		}
	}
	
	public static void quit_ok(){
    	Runtime.getRuntime().removeShutdownHook(Starter.getHaltHandler());
    	
    	if (Starter.getDriverFast() != null)
    		Starter.getDriverFast().quit();
    	
    	if (Starter.getDriver() != null)
    		try {
    			Starter.getDriver().quit();
    		} catch (UnreachableBrowserException e){ 
    			// nothing - already died browser
    		} 
		
		if (Starter.getChromeService() != null)
			Starter.getChromeService().stop();
		
		System.exit(0);
	}
	
	public static void quit_nok(){
		Runtime.getRuntime().removeShutdownHook(Starter.getHaltHandler());	
		
		if (Starter.getDriverFast() != null)
    		Starter.getDriverFast().quit();
    	
    	if (Starter.getDriver() != null)
    		try {
    			Starter.getDriver().quit();
    		} catch (UnreachableBrowserException e){ 
    			// nothing - already died browser
    		}
    	
		if (Starter.getChromeService() != null)
			Starter.getChromeService().stop();
		
		System.exit(1);
	}
}
