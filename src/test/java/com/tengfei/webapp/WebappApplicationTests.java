package com.tengfei.webapp;

import com.tengfei.webapp.controller.UserController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class WebappApplicationTests {

	@Test
	void testUsingSimpleRegex(){
		String emailAddress = "username@domain.com";
		String regexPattern = "^(.+)@(\\S+)$";
//		assertFalse(UserController.patternMatches(emailAddress, regexPattern));
		assertTrue(UserController.patternMatches(emailAddress, regexPattern));
	}
}
