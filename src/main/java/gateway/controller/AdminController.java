/**
 * Copyright 2016, RadiantBlue Technologies, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package gateway.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import gateway.controller.util.GatewayUtil;
import gateway.controller.util.PiazzaRestController;
import model.logger.Severity;
import model.response.ErrorResponse;
import model.response.PiazzaResponse;
import model.response.UUIDResponse;
import util.PiazzaLogger;

/**
 * REST Controller that defines administrative end points that reference logging, administartion, and debugging
 * information related to the Gateway component.
 * 
 * @author Patrick.Doody
 *
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
public class AdminController extends PiazzaRestController {
	@Autowired
	private PiazzaLogger logger;
	@Autowired
	private HttpServletRequest request;
	@Autowired
	private GatewayUtil gatewayUtil;
	@Value("${vcap.services.pz-kafka.credentials.host}")
	private String KAFKA_ADDRESS;
	@Value("${SPACE}")
	private String SPACE;
	@Value("${workflow.url}")
	private String WORKFLOW_URL;
	@Value("${search.url}")
	private String SEARCH_URL;
	@Value("${ingest.url}")
	private String INGEST_URL;
	@Value("${access.url}")
	private String ACCESS_URL;
	@Value("${jobmanager.url}")
	private String JOBMANAGER_URL;
	@Value("${servicecontroller.url}")
	private String SERVICECONTROLLER_URL;
	@Value("${uuid.url}")
	private String UUIDGEN_URL;
	@Value("${logger.url}")
	private String LOGGER_URL;
	@Value("${security.url}")
	private String SECURITY_URL;
	@Value("${release.url}")
	private String RELEASE_URL;

	@Autowired
	private RestTemplate restTemplate;

	private final static Logger LOGGER = LoggerFactory.getLogger(AdminController.class);

	/**
	 * Healthcheck required for all Piazza Core Services
	 * 
	 * @return String
	 */
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String getHealthCheck() {
		return "Hello, Health Check here for pz-gateway.";
	}

	@RequestMapping(value = "/version", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getVersion() {
		try {
			return new ResponseEntity<String>(restTemplate.getForObject(RELEASE_URL, String.class), HttpStatus.OK);
		} catch (Exception e) {
			String error = String.format("Error retrieving version for Piazza: %s", e.getMessage());
			LOGGER.error(error, e);
			return new ResponseEntity<PiazzaResponse>(new ErrorResponse(error, "Gateway"), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Returns administrative statistics for this Gateway component.
	 * 
	 * @return Component information
	 */
	@RequestMapping(value = "/admin/stats", method = RequestMethod.GET)
	public ResponseEntity<Map<String, Object>> getAdminStats() {
		Map<String, Object> stats = new HashMap<String, Object>();
		// Write the Kafka configs
		stats.put("Kafka Address", KAFKA_ADDRESS);
		// Write the URL configs
		stats.put("Space", SPACE);
		stats.put("Workflow", WORKFLOW_URL);
		stats.put("Search", SEARCH_URL);
		stats.put("Ingest", INGEST_URL);
		stats.put("Access", ACCESS_URL);
		stats.put("JobManager", JOBMANAGER_URL);
		stats.put("ServiceController", SERVICECONTROLLER_URL);
		stats.put("UUIDGen", UUIDGEN_URL);
		stats.put("Logger", LOGGER_URL);
		stats.put("Security", SECURITY_URL);
		stats.put("Release", RELEASE_URL);
		// Return
		return new ResponseEntity<Map<String, Object>>(stats, HttpStatus.OK);
	}

	@RequestMapping(value = "/bfkey", method = RequestMethod.GET)
	public ResponseEntity<?> getBFAPIKey(@RequestParam(value = "access_token", required = true) String accessToken) {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.set("Authorization", accessToken);
			try {
				GxResponse gxr = restTemplate.exchange("https://gxisaccess.gxaccess.com/ms_oauth/resources/userprofile/me", HttpMethod.GET,
					new HttpEntity<String>("parameters", headers), GxResponse.class).getBody();

				// Try to retrieve existing API Key based on uid

				// If APIKey exists, return
				gxr.setApiKey("alongtestapikeyforbeachfront");

				// If APIKey does not exist, create new user profile with new
				// APIKey, and return

				return new ResponseEntity<GxResponse>(gxr, HttpStatus.OK);

			} catch (HttpClientErrorException | HttpServerErrorException hee) {
				// if( hee.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
				// Authorization of AccessToken failed
				return new ResponseEntity<PiazzaResponse>(gatewayUtil.getErrorResponse(hee.getResponseBodyAsString()),
						hee.getStatusCode());
				// }
			}
		} catch (Exception exception) {
			exception.printStackTrace();
			String error = String.format("Error retrieving UUID: %s", exception.getMessage());
			LOGGER.error(error);
			return new ResponseEntity<PiazzaResponse>(new ErrorResponse(error, "Gateway"),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Generates a new API Key for a user based on their credentials. Accepts username/password or PKI Cert for GeoAxis.
	 * 
	 * @return API Key Response information
	 */
	@RequestMapping(value = "/key", method = RequestMethod.GET)
	public ResponseEntity<PiazzaResponse> getNewApiKeyV1() {
		return generateNewApiKey();
	}

	/**
	 * Generates a new API Key for a user based on their credentials. Accepts username/password or PKI Cert for GeoAxis.
	 * 
	 * @return API Key Response information
	 */
	@RequestMapping(value = "/v2/key", method = RequestMethod.POST)
	public ResponseEntity<PiazzaResponse> getNewApiKeyV2() {
		return generateNewApiKey();
	}

	/**
	 * Gets the existing API Key for the user.
	 * 
	 * @return API Key information
	 */
	@RequestMapping(value = "/v2/key", method = RequestMethod.GET)
	public ResponseEntity<PiazzaResponse> getExistingApiKeyV2() {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.set("Authorization", request.getHeader("Authorization"));
			try {
				return new ResponseEntity<PiazzaResponse>(new RestTemplate().exchange(SECURITY_URL + "/v2/key", HttpMethod.GET,
						new HttpEntity<String>("parameters", headers), UUIDResponse.class).getBody(), HttpStatus.CREATED);
			} catch (HttpClientErrorException | HttpServerErrorException hee) {
				LOGGER.error(hee.getResponseBodyAsString(), hee);
				return new ResponseEntity<PiazzaResponse>(gatewayUtil.getErrorResponse(hee.getResponseBodyAsString()), hee.getStatusCode());
			}
		} catch (Exception exception) {
			String error = String.format("Error retrieving API Key: %s", exception.getMessage());
			LOGGER.error(error, exception);
			logger.log(error, Severity.ERROR);
			return new ResponseEntity<PiazzaResponse>(new ErrorResponse(error, "Gateway"), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Generates the API Key from the Pz-Idam endpoint.
	 * 
	 * @return API Key response information.
	 */
	private ResponseEntity<PiazzaResponse> generateNewApiKey() {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.set("Authorization", request.getHeader("Authorization"));
			try {
				return new ResponseEntity<PiazzaResponse>(new RestTemplate().exchange(SECURITY_URL + "/v2/key", HttpMethod.POST,
						new HttpEntity<String>("parameters", headers), UUIDResponse.class).getBody(), HttpStatus.CREATED);
			} catch (HttpClientErrorException | HttpServerErrorException hee) {
				LOGGER.error(hee.getResponseBodyAsString(), hee);
				return new ResponseEntity<PiazzaResponse>(gatewayUtil.getErrorResponse(hee.getResponseBodyAsString()), hee.getStatusCode());
			}
		} catch (Exception exception) {
			String error = String.format("Error retrieving API Key: %s", exception.getMessage());
			LOGGER.error(error, exception);
			logger.log(error, Severity.ERROR);
			return new ResponseEntity<PiazzaResponse>(new ErrorResponse(error, "Gateway"), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
