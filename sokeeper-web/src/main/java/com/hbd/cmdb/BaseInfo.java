package com.hbd.cmdb;

public class BaseInfo {
	private static String getBaseDir() {
		return System.getProperty("BASE_DIR", System.getProperty("user.home"));
	}
	
	public static String path = getBaseDir() + "/research/";
	public static String indexPath = getBaseDir() + "/research/";
	public static String imgPath = path + "img/";
	public static String output = getBaseDir() + "/";
	public static String subjectPath = BaseInfo.path + "subject.txt";
	public static String reviewFile = "/review.dat";
	public static String subjectFile = "/subject.txt";
	public static String totalSubjectFile = indexPath + "subject.dat";
}
