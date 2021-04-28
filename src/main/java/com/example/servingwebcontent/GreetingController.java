package com.example.servingwebcontent;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Controller
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GreetingController {
	private final ZeroMqService service;

	@GetMapping("/greeting")
	public String greeting(@RequestParam(name="name", required=false, defaultValue="World") String name, Model model) throws InterruptedException, ExecutionException, TimeoutException {
		model.addAttribute("name", service.getText(name).get(10, TimeUnit.SECONDS));
		return "greeting";
	}

}
