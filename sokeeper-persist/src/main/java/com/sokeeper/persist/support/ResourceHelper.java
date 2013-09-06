package com.sokeeper.persist.support;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import java.util.concurrent.atomic.AtomicInteger;
//import org.apache.http.HttpHost;
//import org.apache.http.conn.params.ConnRoutePNames;

public class ResourceHelper implements ResponseHandler<String>{
	final protected Logger logger = LoggerFactory.getLogger(getClass());
	
	private static ResourceHelper instance = new ResourceHelper();
	
	public static ResourceHelper getInstance() {
		return instance;
	}
	
	//private AtomicInteger balanced = new AtomicInteger(0); 

	private ResourceHelper() { 
	}
	
	public String getResPrefix() {
		String resPrefix = System.getProperty("RES_PREFIX",System.getenv("RES_PREFIX"));	
		if (resPrefix == null) {
			resPrefix = "" ;
		}
		return resPrefix;
	}
	
	public BufferedReader getReader(String theFile) throws FileNotFoundException { 
		BufferedReader reader = null;
		if (new File(theFile).exists()) {
			reader = new BufferedReader(new FileReader(theFile));
		} else if (new File(getResPrefix()+theFile).exists()){
			reader = new BufferedReader(new FileReader(theFile));
		} else {
			InputStream stream = ResourceHelper.class.getResourceAsStream("/" + theFile);
			if (stream == null) {
				stream = ResourceHelper.class.getResourceAsStream( getResPrefix() + theFile);
			}
			if (stream == null) {
				String strBody = request(getResPrefix() + theFile);
				reader = new BufferedReader(new StringReader(strBody));
			} else {
				reader = new BufferedReader(new InputStreamReader(stream));
			}
		}
		return reader;
	}
	
	public String request(String url) {
		HttpClient httpClient = getHttpClient();
		String responseBody = "";
		try {
			HttpGet httpget = new HttpGet(url); 
			responseBody = httpClient.execute(httpget,this); 
		} catch (Throwable t) {
			logger.error("",t);
		} finally {
			try {
				httpClient.getConnectionManager().shutdown();
			} catch (Throwable t) {
				logger.error("",t);
			}
		}
		return responseBody;
	}
	
	@Override
	public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
		StatusLine statusLine = response.getStatusLine();
        if (statusLine.getStatusCode() >= 300) {
            throw new HttpResponseException(statusLine.getStatusCode(),statusLine.getReasonPhrase());
        }
        HttpEntity entity = response.getEntity(); 
        return entity == null ? null : EntityUtils.toString(entity,"UTF-8");
	}
	
	public void request(String url, String file) {
		HttpClient httpClient = getHttpClient();
		InputStream responseBody = null;
		OutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(file));
			HttpGet httpget = new HttpGet(url);
			responseBody = httpClient.execute(httpget).getEntity().getContent();
			byte[] b = new byte[1024 * 10];
			int len;
			while ((len = responseBody.read(b)) != -1) {
				out.write(b, 0, len);
			}
		} catch (Throwable t) {
			logger.error("",t);
		} finally {
			try {
				httpClient.getConnectionManager().shutdown();
				out.close();
			} catch (Throwable t) {
				logger.error("",t);
			}
		}
	}
	
	private HttpClient getHttpClient() {  
		HttpClient httpClient = new DefaultHttpClient();
//		HttpHost[] proxys = new HttpHost[3];
//		proxys[0] = new HttpHost("child-prc.intel.com", 911, "http");
//		proxys[1] = new HttpHost("proxy-shz.intel.com", 911, "http");
//		proxys[2] = new HttpHost("proxy-mu.intel.com" , 911, "http");
//		int which = balanced.incrementAndGet() % proxys.length;
//		httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,proxys[which]); 
		return httpClient;
	}

}
