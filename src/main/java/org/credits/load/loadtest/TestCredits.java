package org.credits.load.loadtest;

import org.credits.load.loadtest.services.CallSmartContract;
import org.credits.load.loadtest.services.SendCreditsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@EnableScheduling
public class TestCredits implements CommandLineRunner {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestCredits.class);

	@Autowired
	SendCreditsService sendCreditsService;

	@Autowired
	CallSmartContract callSmartContract;

	public static void main(String[] args) {
		// launch app
		SpringApplication.run(TestCredits.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

		// TODO Auto-generated method stub
		LOGGER.info("Hey start APP");
		// doSend();
		sendCreditsService.executeAsynchronously();
		// callSmartContract.executeAsynchronously();

	}

}
