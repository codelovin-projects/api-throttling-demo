package com.codelovin.springboot.apithrottlingdemo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ThrottledRestController {

	@GetMapping("/api/hello")
	public String sayHello() {
		return "Hello World";
	}
}
