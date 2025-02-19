package example.controller;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;

import example.dto.StringResponse;
import example.model.HostData;
import example.service.ExampleService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/basic")
public class Controller {

	@Autowired
	private ExampleService service;
	private static Map<String, HostData> inventory = new HashMap<>();

	public Controller(ExampleService data) {
		service = data;
	}

	@GetMapping(produces = { MediaType.TEXT_PLAIN_VALUE })
	public String hello() {
		return service.hello();
	}

	@GetMapping(value = "/json", produces = { MediaType.APPLICATION_JSON_VALUE })
	public Data json() {
		return new Data(service.hello());
	}

	// not needed
	@GetMapping(value = "/array_json", produces = {
			MediaType.APPLICATION_JSON_VALUE })
	public List<Data> arrayJson() {
		Data[] datas = { new Data(service.hello()) };
		return Arrays.asList(datas);
	}

	@GetMapping(value = "/hostdata", produces = {
			MediaType.APPLICATION_JSON_VALUE })
	public HostData hostdata() {

		String hostname = "localhost";
		HostData hostdata = new HostData(hostname, System.getProperty("user.dir"),
				"dummy1.txt");
		hostdata.addFilePath(System.getProperty("user.dir"), "dummy2.txt");
		return hostdata;
	}

	@GetMapping(value = "/listdata/{hostname}", produces = {
			MediaType.APPLICATION_JSON_VALUE })
	public List<String> arrayFile(@PathVariable String hostname) {

		HostData hostdata = new HostData(hostname, System.getProperty("user.dir"),
				"dummy1.txt");
		hostdata.addFilePath(System.getProperty("user.dir"), "dummy2.txt");

		inventory.put(hostname, hostdata);
		final List<String> results = (inventory.containsKey(hostname))
				? inventory.get(hostname).getFilePaths() : new ArrayList<>();

		return results;
	}


	@PostMapping(value = "/post", consumes = {
			MediaType.APPLICATION_JSON_VALUE }, produces = {
					MediaType.APPLICATION_JSON_VALUE })
	public Data post(@RequestBody Data data) {
		return data;
	}

	// example execution:
	// GET http://192.168.0.64:8085/basic/params?values=a&values=b&values=c
	// 200 [ "a" ]
	// http://192.168.0.64:8085/basic
	// 405 []
	@GetMapping(value = "/params", produces = {
			MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<List<String>> paramArrayEcho(
			@RequestParam Optional<List<String>> values) {
		return (values.isPresent() && values.get().size() > 0)
				? ResponseEntity.status(HttpStatus.OK).body(values.get())
				: ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
						.body(new ArrayList<String>());
	}

	private static final RestTemplate restTemplate = new RestTemplate();

	// @Value("${server.port:8085}")
	// private int port;
	// the @Value annotation is not working
	private int port = 8085;
	// when run test, seeing:
	// org.springframework.web.client.ResourceAccessException: I/O error on POST
	// request
	// for "http://localhost:0/basic/post": connect:
	// Address is invalid on local machine, or port is not valid on remote
	// machine

	@PostMapping(value = "/customHello", produces = {
			MediaType.APPLICATION_JSON_VALUE })

	public ResponseEntity<StringResponse> stringResponsePage(
			@RequestParam Optional<String> message) {
		if (message.isPresent()) {
			StringResponse respose = new StringResponse(
					(message.get().length() == 0 ? "Custom " : message.get()));
			return ResponseEntity.status(HttpStatus.OK).body(respose);
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
		}
	}

	@GetMapping(value = "/defaultHello", produces = {
			MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<StringResponse> objectResponse(
			@RequestParam Optional<String> message) {
		String finalMessage = String.format("Hello %s",
				(message.isPresent() ? message.get() : "World!"));
		StringResponse respose = new StringResponse(finalMessage);
		return ResponseEntity.status(HttpStatus.OK).body(respose);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/pathHello/{message}", produces = {
			MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<StringResponse> objectResponse(
			@PathVariable String message) {
		String finalMessage = String.format("Hello %s",
				(message == null ? "world" : message));
		StringResponse respose = new StringResponse(finalMessage);
		return ResponseEntity.status(HttpStatus.OK).body(respose);
	}

	@ResponseBody
	// 406 Not Acceptable client error response
	// 415 Unsupported Media Type
	@PostMapping(value = "/page" /*
																* , consumes = { MediaType.TEXT_PLAIN_VALUE }
																*/, produces = { MediaType.TEXT_PLAIN_VALUE })
	public ResponseEntity<String> page(@RequestParam String name) {
		final String url = String.format("http://localhost:%d/basic/post", port);
		// perform API call to localhost

		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		final Data input = new Data();
		input.setName(name);
		final Gson gson = new Gson();

		final String payload = gson.toJson(input);
		System.err
				.println(String.format("POSTING %s to %s", payload.toString(), url));
		final HttpEntity<String> request = new HttpEntity<String>(payload, headers);
		final ResponseEntity<Data> responseEntity = restTemplate.postForEntity(url,
				request, Data.class, headers);
		final String result = responseEntity.getBody().getName();
		return ResponseEntity.status(HttpStatus.OK).body(result);
	}

	public static class Data {

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String data) {
			name = data;
		}

		public Data(String name) {
			this.name = name;
		}

		public Data() {
		}
	}
}
