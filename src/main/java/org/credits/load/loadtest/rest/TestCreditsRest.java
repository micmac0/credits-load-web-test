package org.credits.load.loadtest.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestCreditsRest {
	public static final Logger LOGGER = LoggerFactory.getLogger(TestCreditsRest.class);

	@RequestMapping(value = "/balance", method = RequestMethod.GET)
	public ResponseEntity<String> getBalance() {

		return null;

	}

}
