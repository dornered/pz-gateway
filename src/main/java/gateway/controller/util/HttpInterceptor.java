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
package gateway.controller.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * Interceptor used to catch http requests and redirect to https.
 * 
 * @author Sonny.Saniev
 * 
 */
@Component
public class HttpInterceptor extends HandlerInterceptorAdapter {
	private final static Logger LOGGER = LoggerFactory.getLogger(HttpInterceptor.class);

	/**
	 * Catches HTTP requests and redirects to HTTPS.
	 */
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if (request.getScheme().equals("http")) {
			String redirectUrl = String.format("%s://%s%s", "https", request.getServerName(), request.getRequestURI());
			response.sendRedirect(redirectUrl);
			LOGGER.info(String.format("Redirecting from %s to %s.", request.getRequestURI(), redirectUrl));
			return false;
		}
		return true;
	}
}
