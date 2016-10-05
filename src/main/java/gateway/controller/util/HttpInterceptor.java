package gateway.controller.util;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Component
public class HttpInterceptor extends HandlerInterceptorAdapter {

	private final static Logger LOGGER = LoggerFactory.getLogger(HttpInterceptor.class);
	

	// Pre-handler
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

		LOGGER.info("pre-handler triggered, oh yeahhhh...");
		long startTime = System.currentTimeMillis();
		request.setAttribute("startTime", startTime);

		LOGGER.info("\n\n----------------------------request.toString(): " + request.getProtocol() + " -- " + request.getMethod());
		if (request.getProtocol().contains("HTTP/")) {
			response.sendRedirect("www.google.com");
			return false;
		}
		
		return true;
	}

	// Post handler
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {

		long startTime = (Long) request.getAttribute("startTime");
		long endTime = System.currentTimeMillis();
		long executeTime = endTime - startTime;
		LOGGER.info("[" + handler + "] executeTime : " + executeTime + "ms");
	}
}
