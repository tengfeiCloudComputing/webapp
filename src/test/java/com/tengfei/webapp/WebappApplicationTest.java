package com.tengfei.webapp;

import com.tengfei.webapp.controller.UserController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class WebappApplicationTest {

	@Test
	void testUsingSimpleRegex(){
		String emailAddress = "username@domain";
		String regexPattern = "^(.+)@(\\S+)$";
//		assertFalse(UserController.patternMatches(emailAddress, regexPattern));
		assertTrue(000UserController.patternMatches(emailAddress, regexPattern));
	}
}
