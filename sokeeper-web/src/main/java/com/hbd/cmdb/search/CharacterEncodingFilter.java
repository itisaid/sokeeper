package com.hbd.cmdb.search;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class CharacterEncodingFilter implements Filter {

	private String encoding = "UTF-8" ;
	
	public void destroy() { 
		
	}

	public void doFilter(ServletRequest req, ServletResponse res,
			FilterChain chain) throws IOException, ServletException { 
		req.setCharacterEncoding(encoding);
		res.setCharacterEncoding(encoding);
		chain.doFilter(req, res);
		
	}

	public void init(FilterConfig cfg) throws ServletException { 
		encoding = cfg.getInitParameter("encoding");
	}

}
