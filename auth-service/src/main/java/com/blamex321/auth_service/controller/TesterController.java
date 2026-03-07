package com.blamex321.auth_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class TesterController {

	@GetMapping("/whoami")
	public String whoAmI(@RequestHeader("X-User-Email") String email) {
	    return "Authenticated as: " + email;
	}
}
