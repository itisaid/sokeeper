package com.sokeeper.service.support;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

public class ResourceHelper { 
	final public static String IMAGE_HTTP_SERVER         = "/images/poster/";
	final public static String DATA_FILE_SUBJECT         = "subject.dat"    ;
	final public static String DATA_FILE_KEYWORD_SUBJECT = "keysubject.dat" ;
	final public static String DATA_FILE_BLACK_WORDS     = "blackwords.dat" ;
	private static ResourceHelper instance = new ResourceHelper();

	final private static Logger logger = LoggerFactory.getLogger(ResourceHelper.class);
		
	private Map<String,String> mapEnvName   = new HashMap<String,String>();
	
	public static ResourceHelper getInstance() {
		return instance;
	}
	
	private ResourceHelper() {
		mapEnvName.put(IMAGE_HTTP_SERVER        , "IMAGES_POSTER");
		mapEnvName.put(DATA_FILE_SUBJECT        , "SUBJECT_DAT");
		mapEnvName.put(DATA_FILE_KEYWORD_SUBJECT, "KEYWORD_DAT");
		mapEnvName.put(DATA_FILE_BLACK_WORDS    , "BLACKWORDS_DAT");
	}

	public String getEnv( String envName ) {
		String toEnvName = mapEnvName.get(envName);
		if (toEnvName == null) {
			toEnvName = envName ;
		}
		String env = System.getProperty(toEnvName,System.getenv(toEnvName));	
		if (env == null) {
			env = envName ;
		}
		return env;
	}
	
	public BufferedReader getReader(String resource) throws IOException { 
		Assert.hasText(resource,"resource can not be empty");
		
		BufferedReader reader = null;
		
		// STEP 1: it's normal file
		if (new File(resource).exists()) {
			FileReader fReader = new FileReader(resource);
			reader = new BufferedReader(fReader);
		} else {
			// STEP 2: it's normal resources inside jar file
			InputStream stream = ResourceHelper.class.getResourceAsStream("/" + resource);
			if (stream == null) {
				// STEP 3: configured through ENV
				String resourceCustomized = getEnv(resource); 
				
				logger.info( resource + " -> " + resourceCustomized);
				
				if (isWebResource(resourceCustomized)) { 
					stream = new URL(resourceCustomized).openStream();
				}
			}
			if (stream != null) {
				reader = new BufferedReader(new InputStreamReader(stream));
			}
		}
		// STEP 4: return the reader
		if (reader == null) {
			reader = new BufferedReader(new StringReader(""));
		}
		return reader ;
	}

	private boolean isWebResource(String resourceUrl) {
		String upUrl = resourceUrl.toUpperCase();
		return upUrl.toUpperCase().startsWith("HTTP:") || upUrl.toUpperCase().startsWith("HTTPS:") || upUrl.toUpperCase().startsWith("FTP:");
	}
}

//import java.net.MalformedURLException;
//import com.github.axet.wget.WGet;
//	private String workingDir = "." ;
//	workingDir = getEnv(ENV_WORKING_DIR);
//	if (ENV_WORKING_DIR.equals(workingDir)){
//		workingDir = makeWorkingDir();	
//	}
//	private String makeWorkingDir() {
//		String theDir = new File(".").getAbsolutePath();
//		for (int idx = theDir.length() - 1; idx >= 0 ; idx --) {
//			if (theDir.charAt(idx) == '.') {
//				continue;
//			}
//			if (theDir.charAt(idx) == File.separatorChar) {
//				continue;
//			}
//			theDir = theDir.substring(0,idx+1);
//			break;
//		}
//		return theDir;
//	}
//	
//	private String makeFileDir(String theFile){
//		for (int idx=0; idx<theFile.length(); idx ++) {
//			if (theFile.charAt(idx) == '/') {
//				continue;
//			}
//			if (theFile.charAt(idx) == '\\') {
//				continue;
//			}
//			if (idx == 0) {
//				theFile = workingDir + File.separator + theFile ;
//			}
//			break;
//		}
//		return theFile ;
//	}
//
//if (!resource.equals(resourceCustomized)){ 
//	File toFile = new File(makeFileDir(resource)) ;
//	// STEP 4: it's HTTP requested resources
//	if (isWebResource(resourceCustomized)) { 
//		try {
//            URL url = new URL(resourceCustomized);
//            
//            logger.info("GET:" + resourceCustomized + " -> " + toFile.getAbsolutePath() );
//            
//            WGet wGet = new WGet(url, toFile);
//            wGet.download();
//            if (toFile.exists()) {
//            	reader = new BufferedReader(new FileReader(toFile));
//            }
//            
//            logger.info("SUCCEED");
//        } catch (MalformedURLException e) { 
//        	logger.error(e.getMessage(),e);
//        } catch (RuntimeException e) { 
//        	logger.error(e.getMessage(),e);
//        }
//	}
//}	
