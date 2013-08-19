package com.hbd.cmdb.spider;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

public class SpiderUtil {
	public static HttpClient httpClient;
	public static int lb = 0;

	public static void init() throws Exception {
		httpClient = new DefaultHttpClient();
		HttpHost[] proxys = new HttpHost[3];
		proxys[0] = new HttpHost("child-prc.intel.com", 911, "http");
		proxys[1] = new HttpHost("proxy-shz.intel.com", 911, "http");
		proxys[2] = new HttpHost("proxy-mu.intel.com", 911, "http");
		int i = lb++ % 3;
		httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,
				proxys[i]);
		Thread.sleep(200);
	}

	public static String request(String url) throws Exception {
		String responseBody = null;
		try {
			init();
			HttpGet httpget = new HttpGet(url);

			System.out.println("executing request " + httpget.getURI());

			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			responseBody = SpiderUtil.httpClient.execute(httpget,
					responseHandler);
		} catch (HttpResponseException e) {
			// if (e.getStatusCode() != 404) {
			// throw e;
			// }
		} catch (Throwable t) {

		} finally {
			try {
				httpClient.getConnectionManager().shutdown();
			} catch (Throwable t) {
			}
		}
		return responseBody;
	}

	/**
	 * write response to file.
	 * 
	 * @param url
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public static void request(String url, String file) throws Exception {
		InputStream responseBody = null;
		OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		try {
			init();
			HttpGet httpget = new HttpGet(url);

			System.out.println("executing request " + httpget.getURI());

			responseBody = SpiderUtil.httpClient.execute(httpget).getEntity()
					.getContent();

			byte[] b = new byte[1024 * 10];
			int len;
			while ((len = responseBody.read(b)) != -1) {
				out.write(b, 0, len);
			}

		} catch (HttpResponseException e) {
			// if (e.getStatusCode() != 404) {
			// throw e;
			// }
		} catch (Throwable t) {

		} finally {
			try {
				httpClient.getConnectionManager().shutdown();
				out.close();
			} catch (Throwable t) {
			}
		}

	}

	public static List<String> findLink(String page) {
		List<String> linkList = new ArrayList<String>();
		int length = page.length();
		for (int i = 0; i < length; i++) {
			if (page.charAt(i) == 'h' && page.charAt(i + 1) == 'r'
					&& page.charAt(i + 2) == 'e' && page.charAt(i + 3) == 'f'
					&& page.charAt(i + 4) == '=' && page.charAt(i + 5) == '"') {
				int j = i + 6;
				StringBuffer sb = new StringBuffer();
				char ca = page.charAt(j);
				while (ca != '"') {
					sb.append(ca);
					ca = page.charAt(++j);
				}
				linkList.add(sb.toString());
				i = j;
			}
		}
		return linkList;
	}
}
