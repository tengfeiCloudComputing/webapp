package com.tengfei.webapp;

import com.tengfei.webapp.controller.UserController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class WebappApplicationTests {

	@Test
	void contextLoads() {

	}

	@Test
	void testUsingSimpleRegex(){
		String emailAddress = "username@domain.com";
		String regexPattern = "^(.+)@(\\S+)$";
		assertTrue(UserController.patternMatches(emailAddress, regexPattern));
	}

	
}
