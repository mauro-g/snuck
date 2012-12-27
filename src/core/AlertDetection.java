package core;

import java.util.concurrent.Callable;

import org.openqa.selenium.Alert;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

public class AlertDetection implements Callable<String> {
	private int alerts_number;
	private String vector;
	boolean user_interaction = false;
	
	public AlertDetection(int alerts_number, String vector, boolean user_interaction){
		this.alerts_number = alerts_number;
		this.vector = vector;
		this.user_interaction = user_interaction;
		
		if (user_interaction)
			alerts_number = 0;
	}
	
	@Override
	public String call() {
		int local_count = 0;
		
		for (int i = 0; i <= alerts_number; i++){
			try {
				Alert alert = (new WebDriverWait(Starter.getDriver(), 2))
						.until(new ExpectedCondition<Alert>(){	
							public Alert apply(WebDriver d) {
								return Starter.getDriver().switchTo().alert();
							}
					    });
	
				alert.accept();
				
				if (Starter.getOperation() == 2)
					if (vector != null)
						Inject.log(vector);

				if (Inject.bar != null)
					Inject.bar.setBroken();
					
				if (Starter.getParsedArgs().getStopFirstPositive())
					Starter.forceQuit();
				
				local_count++;
			} catch (Exception e) { } 
			
			if (Starter.getOperation() == 1 && local_count > alerts_number)
				if (vector != null)
					Inject.log(vector);
		}
		
		return "";
    }
}
