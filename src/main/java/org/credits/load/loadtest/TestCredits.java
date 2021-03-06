package org.credits.load.loadtest;

import org.credits.load.loadtest.services.CallSmartContract;
import org.credits.load.loadtest.services.DeploySmartContract;
import org.credits.load.loadtest.services.SendCreditsService;
import org.credits.load.loadtest.util.GeneralProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.RestController;

import com.credits.general.util.GeneralConverter;

@SpringBootApplication
@RestController
@EnableScheduling
public class TestCredits implements CommandLineRunner {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestCredits.class);

	@Autowired
	SendCreditsService sendCreditsService;

	@Autowired
	CallSmartContract callSmartContract;
	
	@Autowired
	DeploySmartContract deploySmartContract;
	
	@Autowired
	GeneralProperties generalProperties;
	
	

	public static void main(String[] args) {
		// launch app
		SpringApplication.run(TestCredits.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

		// TODO Auto-generated method stub
		LOGGER.info("Hey start APP");
		
		String hexa = "46A6F86B8D7CDB9ECF02D50DE41931CEF73076EA4D58AE901A5D62A8FA079AED";
		byte[] bytes = hexStringToByteArray(hexa);

		

		

		if(generalProperties.getMode() == 1) {
			sendCreditsService.executeAsynchronously();
		} else if(generalProperties.getMode() == 2) {
			callSmartContract.executeAsynchronously();
		} else if(generalProperties.getMode() == 3) {
			deploySmartContract.executeAsynchronously();
		}

		

	}
	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
}
