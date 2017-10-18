package com.okta.foresee.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

import com.okta.foresee.csv.ForeseeConstants;

public class OktaConnectionHealper {
	public static int RATE_LIMIT_USER_CREATE	=	600;
	public static int RATE_LIMIT_USER_UPDATE	=	600;
	public static int RATE_LIMIT_GROUP_CREATE	=	600;
	public static int RATE_LIMIT_GROUP_UPDATE	=	600;
	public static long RATE_LIMIT_RESET	=	System.currentTimeMillis() / 1000L; //current time
	
	public static Ret get(String resource, String token, Proxy proxy) {
		Ret ret = new Ret();
		HttpURLConnection conn;
		
		try {
			URL url = new URL(resource);
			
			if (proxy != null) {
				conn = (HttpURLConnection) url.openConnection(proxy);
			} else {
				conn = (HttpURLConnection) url.openConnection();
			}
			
			conn.setConnectTimeout(1000000);
			conn.setReadTimeout(1000000);
			conn.setRequestProperty(ForeseeConstants.AUTHZ_PROPERTY, ForeseeConstants.SSWS + token);
			conn.setRequestProperty(ForeseeConstants.CONTENT_TYPE, ForeseeConstants.APP_JSON);
			conn.setRequestProperty(ForeseeConstants.ACCEPT, ForeseeConstants.APP_JSON);
			conn.setRequestMethod(ForeseeConstants.METHOD_GET);
			String line;
			
			ret.responseCode = conn.getResponseCode();
			
			ForeseeCSVUtil.log("ret.responseCode------------"+ret.responseCode);
			

			ForeseeCSVUtil.log("ret.output------------"+ret.output);
			
			if (ret.responseCode == 200) {
				InputStream s = conn.getInputStream();
				if (s != null) {
					try (BufferedReader rd = new BufferedReader(new InputStreamReader(s))) {
						ret.output = ForeseeConstants.BLANK_STRING;
						while ((line = rd.readLine()) != null) {
							ret.output += line;
						}
						if(ret.output.equals("[]")){
							ret.emptyOutput=Boolean.TRUE;
						}
						else
							ret.emptyOutput=Boolean.FALSE;
				     }
				}
			} else {
				InputStream s = conn.getErrorStream();
				if (s != null) {
					try (BufferedReader rd = new BufferedReader(new InputStreamReader(s))) {
						ret.errorOutput = ForeseeConstants.BLANK_STRING;
						while ((line = rd.readLine()) != null) {
							ret.errorOutput += line;
						}
					}
				}
			}
			String rll = conn.getHeaderField(ForeseeConstants.RATE_LIMIT_REMAINING);
//			String rlr = conn.getHeaderField(ForeseeConstants.RATE_LIMIT_RESET);
			if (rll != null && rll.length() > 0) {
				try {
					if(resource.contains("/v1/users") ) {
						RATE_LIMIT_USER_UPDATE 	=	Integer.parseInt(rll);
					} else {
						RATE_LIMIT_GROUP_UPDATE  	=	Integer.parseInt(rll);
					}
					
				} catch (Exception e) {
					ret.rateLimitLimit = 1200;
				}
			}
			
			conn.disconnect();
		} catch (IOException e) {
			ret.exception = e.getMessage();
		} 
		return ret;
	}
	
	
	public static Ret put(String resource, String token, String data, Proxy proxy) {
		Ret ret = new Ret();
		try {
			URL url = new URL(resource);
			HttpURLConnection conn;
			if (proxy != null) {
				conn = (HttpURLConnection) url.openConnection(proxy);
			} else {
				conn = (HttpURLConnection) url.openConnection();
			}
			conn.setConnectTimeout(1000000);
			conn.setReadTimeout(1000000);
			if (data != null) {
				conn.setDoOutput(true);
			}
			conn.setRequestProperty(ForeseeConstants.AUTHZ_PROPERTY, ForeseeConstants.SSWS + token);
			conn.setRequestProperty(ForeseeConstants.CONTENT_TYPE, ForeseeConstants.APP_JSON);
			conn.setRequestMethod("PUT");
			/*if (data != null) {
				DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
				wr.write(data.getBytes("UTF-8"));
				wr.flush();
				wr.close();
			}*/
			String line;
			ret.responseCode = conn.getResponseCode();
			if (ret.responseCode == 201 || ret.responseCode == 200) {
				InputStream s = conn.getInputStream();
				if (s != null) {
					try (BufferedReader rd = new BufferedReader(new InputStreamReader(s))) {
						ret.output = ForeseeConstants.BLANK_STRING;
						while ((line = rd.readLine()) != null) {
							ret.output += line;
						}
					}
				}
			} else if (ret.responseCode == 204) {
					
			} else {
				InputStream s = conn.getErrorStream();
				if (s != null) {
					try (BufferedReader rd = new BufferedReader(new InputStreamReader(s))) {
						ret.errorOutput = ForeseeConstants.BLANK_STRING;
						while ((line = rd.readLine()) != null) {
							ret.errorOutput += line;
						}
					}
				}
			}
			String rll = conn.getHeaderField(ForeseeConstants.RATE_LIMIT_REMAINING);
//			String rlr = conn.getHeaderField(ForeseeConstants.RATE_LIMIT_RESET);
			if (rll != null && rll.length() > 0) {
				try {
//					ret.rateLimitLimit = Integer.parseInt(rll);
//					RATE_LIMIT	=	Integer.parseInt(rll);
					if(resource.contains("/v1/users") ) {
						RATE_LIMIT_USER_UPDATE 	=	Integer.parseInt(rll);
					} else {
						RATE_LIMIT_GROUP_UPDATE  	=	Integer.parseInt(rll);
					}
//					OktaApiHelper.RATE_LIMIT_CREATE	=	Integer.parseInt(rll);
//					OktaApiHelper.RATE_LIMIT_RESET	=	Integer.parseInt(rlr);
				} catch (Exception e) {
					ret.rateLimitLimit = 1200;
				}
			}
			conn.disconnect();
		} catch (IOException e) {
			ret.exception = e.getMessage();
		}
		return ret;
	}
	
	public static Ret delete(String resource, String token, String data, Proxy proxy) {
		Ret ret = new Ret();
		try {
			URL url = new URL(resource);
			
			HttpURLConnection conn;
			if (proxy != null) {
				conn = (HttpURLConnection) url.openConnection(proxy);
			} else {
				conn = (HttpURLConnection) url.openConnection();
			}
			conn.setConnectTimeout(1000000);
			conn.setReadTimeout(1000000);
			conn.setDoOutput(true);
			conn.setRequestProperty(ForeseeConstants.AUTHZ_PROPERTY, ForeseeConstants.SSWS + token);
			conn.setRequestProperty(ForeseeConstants.CONTENT_TYPE, ForeseeConstants.APP_JSON);
			conn.setRequestMethod("DELETE");
			
			DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
			wr.write(data.getBytes("UTF-8"));
			wr.flush();
			wr.close();
			String line;
			ret.responseCode = conn.getResponseCode();

//			ForeseeCSVUtil.log("------------ret.responseCode -------"+ret.responseCode );
			if (ret.responseCode == 201 || ret.responseCode == 200) {
				InputStream s = conn.getInputStream();
				if (s != null) {
					try (BufferedReader rd = new BufferedReader(new InputStreamReader(s))) {
						ret.output = ForeseeConstants.BLANK_STRING;
						while ((line = rd.readLine()) != null) {
							ret.output += line;
						}
					}
				}
				 ForeseeCSVUtil.log("------------ret.output-------"+ret.output);
			} else {
				ForeseeCSVUtil.log("------------error--");
				InputStream s = conn.getErrorStream();
				if (s != null) {
					try (BufferedReader rd = new BufferedReader(new InputStreamReader(s))) {
						ret.errorOutput = ForeseeConstants.BLANK_STRING;
						while ((line = rd.readLine()) != null) {
							ret.errorOutput += line;
						}
					}
				}

				ForeseeCSVUtil.log("------------error=="+ret.errorOutput);
			}
			String rll = conn.getHeaderField(ForeseeConstants.RATE_LIMIT_REMAINING);
//			String rlr = conn.getHeaderField(ForeseeConstants.RATE_LIMIT_RESET);
			if (rll != null && rll.length() > 0) {
				try {
					if(resource.contains("/v1/users") ) {
						RATE_LIMIT_USER_UPDATE 	=	Integer.parseInt(rll);
					} else {
						RATE_LIMIT_GROUP_UPDATE  	=	Integer.parseInt(rll);
					}
				} catch (Exception e) {
					ret.rateLimitLimit = 1200;
				}
			}
			conn.disconnect();
			
		} catch (IOException e) {
			ret.exception = e.getMessage();
		}
		return ret;
	}

	public static Ret post(String resource, String token, String data, Proxy proxy) {
		Ret ret = new Ret();
		try {
			URL url = new URL(resource);
			
			HttpURLConnection conn;
			if (proxy != null) {
				conn = (HttpURLConnection) url.openConnection(proxy);
			} else {
				conn = (HttpURLConnection) url.openConnection();
			}
			conn.setConnectTimeout(1000000);
			conn.setReadTimeout(1000000);
			conn.setDoOutput(true);
			conn.setRequestProperty(ForeseeConstants.AUTHZ_PROPERTY, ForeseeConstants.SSWS + token);
			conn.setRequestProperty(ForeseeConstants.CONTENT_TYPE, ForeseeConstants.APP_JSON);
			conn.setRequestMethod(ForeseeConstants.METHOD_POST);
			
			DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
			wr.write(data.getBytes("UTF-8"));
			wr.flush();
			wr.close();
			String line;
			ret.responseCode = conn.getResponseCode();

//			ForeseeCSVUtil.log("------------ret.responseCode -------"+ret.responseCode );
			if (ret.responseCode == 201 || ret.responseCode == 200) {
				InputStream s = conn.getInputStream();
				if (s != null) {
					try (BufferedReader rd = new BufferedReader(new InputStreamReader(s))) {
						ret.output = ForeseeConstants.BLANK_STRING;
						while ((line = rd.readLine()) != null) {
							ret.output += line;
						}
					}
				}
				 ForeseeCSVUtil.log("------------ret.output-------"+ret.output);
			} else {
				ForeseeCSVUtil.log("------------error--");
				InputStream s = conn.getErrorStream();
				if (s != null) {
					try (BufferedReader rd = new BufferedReader(new InputStreamReader(s))) {
						ret.errorOutput = ForeseeConstants.BLANK_STRING;
						while ((line = rd.readLine()) != null) {
							ret.errorOutput += line;
						}
					}
				}

				ForeseeCSVUtil.log("------------error=="+ret.errorOutput);
			}
			String rll = conn.getHeaderField(ForeseeConstants.RATE_LIMIT_REMAINING);
			if (rll != null && rll.length() > 0) {
				try {
					
					if(resource.contains("/v1/users") ) {
						if(resource.contains("activate=true") ) {
							RATE_LIMIT_USER_UPDATE	=	Integer.parseInt(rll);
						} else {
							RATE_LIMIT_USER_CREATE	=	Integer.parseInt(rll);
						}
					} else if( resource.contains("users")){
						RATE_LIMIT_GROUP_UPDATE  	=	Integer.parseInt(rll);
					} else {
						RATE_LIMIT_GROUP_CREATE  	=	Integer.parseInt(rll);
					}
					
				} catch (Exception e) {
					ret.rateLimitLimit = 1200;
				}
			}
			conn.disconnect();
			
		} catch (IOException e) {
			ret.exception = e.getMessage();
		}
		return ret;
	}
	
}
