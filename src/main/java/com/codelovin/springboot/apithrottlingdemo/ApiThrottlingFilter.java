package com.codelovin.springboot.apithrottlingdemo;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Component
public class ApiThrottlingFilter implements Filter {

	final Logger logger = LoggerFactory.getLogger(ApiThrottlingFilter.class);
	
	@Autowired
	private Environment env;

	private LoadingCache<String, Integer> requestsPerIpAddress;

	public ApiThrottlingFilter() {
		super();
		requestsPerIpAddress = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.SECONDS)
				.build(new CacheLoader<String, Integer>() {
					public Integer load(String key) {
						return 0;
					}
				});
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
		String clientIpAddress = getClientIpAddress((HttpServletRequest) servletRequest);
		if (isLimitCrossed(clientIpAddress)) {
			httpServletResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			int maxRequests = Integer.parseInt(env.getProperty("api.throttle.requests.per.second"));
			logger.info("Too many requests: Only " + maxRequests + " requests per second allowed.");
			httpServletResponse.getWriter().write("Too many requests: Only " + maxRequests + " requests per second allowed.");
			return;
		}

		filterChain.doFilter(servletRequest, servletResponse);
	}

	private boolean isLimitCrossed(String clientIpAddress) {
		int requests = 0;
		try {
			int maxRequests = Integer.parseInt(env.getProperty("api.throttle.requests.per.second"));
			requests = requestsPerIpAddress.get(clientIpAddress);
			if (requests > maxRequests) {
				requestsPerIpAddress.put(clientIpAddress, requests);
				return true;
			}
		} catch (ExecutionException e) {
			requests = 0;
		}
		requests++;
		requestsPerIpAddress.put(clientIpAddress, requests);
		return false;
	}

	public String getClientIpAddress(HttpServletRequest request) {
		String xfHeader = request.getHeader(env.getProperty("api.throttle.x.forwardedfor.header"));
		if (xfHeader == null) {
			return request.getRemoteAddr();
		}
		return xfHeader.split(",")[0];
	}
}