package com.okta.foresee.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.okta.foresee.csv.ForeseeConstants;
import com.okta.foresee.json.JSONObject;

/**
*
* @author Sivaji Sabbineni
*/
public class OktaApiHelper {
	
	
	private static Map<String, List<String>> locMap;
	
	public static HashMap<String, String> getAllGroups(String resource, String token) {
		Ret ret = new Ret();
		HttpURLConnection conn;
		HashMap<String, String> groupsMap=null;
		
		resource		=	resource+"/api/v1/groups";
		
		try {
			URL url = new URL(resource);
			
			conn = (HttpURLConnection) url.openConnection();
			
			conn.setConnectTimeout(1000000);
			conn.setReadTimeout(1000000);
			conn.setRequestProperty(ForeseeConstants.AUTHZ_PROPERTY, ForeseeConstants.SSWS + token);
			conn.setRequestProperty(ForeseeConstants.CONTENT_TYPE, ForeseeConstants.APP_JSON);
			conn.setRequestProperty(ForeseeConstants.ACCEPT, ForeseeConstants.APP_JSON);
			conn.setRequestMethod(ForeseeConstants.METHOD_GET);
			String line;
			
			ret.responseCode = conn.getResponseCode();
			if (ret.responseCode == 200) {
				InputStream s = conn.getInputStream();
				if (s != null) {
					try (BufferedReader rd = new BufferedReader(new InputStreamReader(s))) {
						ret.output = ForeseeConstants.BLANK_STRING;
						while ((line = rd.readLine()) != null) {
							ret.output += line;
						}
						if(! ret.output.equals("[]")){
							groupsMap=new HashMap<String, String>();
							JSONParser parser = new JSONParser();
							try {
								org.json.simple.JSONArray jsonArray= (org.json.simple.JSONArray) parser.parse((ret.output).toString());
								for (int i = 0; i < jsonArray.size();  i++)
							    {
//									ForeseeCSVUtil.log("First Element----------"+jsonArray.get(i));
									
									org.json.simple.JSONObject jsonObject		=	 (org.json.simple.JSONObject) jsonArray.get(i);
									
									String groupID	=	(String) jsonObject.get(ForeseeConstants.PROFILE_ID);
									
									org.json.simple.JSONObject profile		=	 (org.json.simple.JSONObject) jsonObject.get(ForeseeConstants.PROFILE);
									
									groupsMap.put( (String) profile.get("name"), groupID);
									
									ForeseeCSVUtil.log("profile.get(\"name\")----------"+ profile.get("name"));
							    }
								
							} catch (ParseException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
						}
						
							
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
			String rll = conn.getHeaderField("X-Rate-Limit-Remaining");
			String rlr = conn.getHeaderField("X-Rate-Limit-Reset");
			if (rll != null && rll.length() > 0) {
				try {
				} catch (Exception e) {
					ret.rateLimitLimit = 1200;
				}
			}
			conn.disconnect();
		} catch (IOException e) {
			ret.exception = e.getMessage();
		} 
		return groupsMap;
	}

	public static String wrapperGroupPost(String urlPrefix, String token,  String groupName, String groupDescription){
		
		String path	=	ForeseeConstants.BLANK_STRING;
		String data	=	"{ \"profile\": {\"name\": \""+     groupName    +"\",\"description\": \""+   groupDescription   +"\"} }";
		
		String groupID	=	ForeseeConstants.BLANK_STRING;
		
		 path = urlPrefix+"/api/v1/groups";//urlPrefix.replace("/users", "/groups");
		 ForeseeCSVUtil.log("------------wrapperGroupPost-----"+groupName);
		
		 Ret ret = OktaConnectionHealper.post(path, token, data, null);
		 ForeseeCSVUtil.log("------------after post-------");
			/*String outstr = null;
			if(ret.output!=null){
	            outstr = ForeseeCSVUtil.stripsquarebraces(ret.output);
			    JSONObject obj = new JSONObject(outstr);
			}*/
		
		if (ret.exception != null) {
			ForeseeCSVUtil.log(groupName + "," + ret.exception + ", TRY AGAIN");
		   } 
		else if (ret.responseCode == 201 || ret.responseCode == 200) {
			ForeseeCSVUtil.recordSuccess(groupName + "," + ret.output, ForeseeConstants.OTHER_SUCCESS_FILE);
			
			try {
				
				org.json.simple.parser.JSONParser parser = new JSONParser();
				
				org.json.simple.JSONObject userObject		=	 (org.json.simple.JSONObject)  parser.parse(ret.output.toString());
					
				groupID	=	(String) userObject.get("id");
					
					ForeseeCSVUtil.log("groupID----------"+ groupID);
				
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
	       } 
		else if (ret.responseCode == 429) {
			ForeseeCSVUtil.recordFailure(groupName + ", Create, " + ret.responseCode + ", " + ret.errorOutput+","+path, ForeseeConstants.OTHER_FAILURE_FILE);
			ForeseeCSVUtil.log(groupName + ", Rate Limit Hit, TRY AGAIN");
		  } 
		else {
			ForeseeCSVUtil.recordFailure(groupName + ", Create, " + ret.responseCode + ", " + ret.errorOutput+","+path, ForeseeConstants.OTHER_FAILURE_FILE);
		}
		return groupID;
	}
	
	public static String getGroupID(String urlPrefix, String token,  String groupName, Proxy proxy){
		
		String path=ForeseeConstants.BLANK_STRING;
		
		String groupID	=	ForeseeConstants.BLANK_STRING;
		
		 try {
			path = urlPrefix+ForeseeConstants.GROUP_SEARCH_PATH+java.net.URLEncoder.encode(groupName, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		 ForeseeCSVUtil.log("------------getGrouID-----"+path);
		
		Ret ret = OktaConnectionHealper.get(path, token, proxy);
		 ForeseeCSVUtil.log("------------after post-------");
			/*String outstr = null;
			if(ret.output!=null){
	            outstr = ForeseeCSVUtil.stripsquarebraces(ret.output);
//			    JSONObject obj = new JSONObject(outstr);
			}*/
		
		if (ret.exception != null) {
			ForeseeCSVUtil.log(groupName + "," + ret.exception + ", TRY AGAIN");
		   } 
		else if (ret.responseCode == 201 || ret.responseCode == 200) {
			ForeseeCSVUtil.recordSuccess(groupName + "," + ret.output, ForeseeConstants.OTHER_SUCCESS_FILE);
			ForeseeCSVUtil.log("------------ret.output-------"+ret.output);
			try {
				JSONParser parser = new JSONParser();
				
				JSONArray jsonArray= (JSONArray) parser.parse(ret.output.toString());
				org.json.simple.JSONObject userObject		=	 (org.json.simple.JSONObject) jsonArray.get(0);
					
				groupID	=	(String) userObject.get(ForeseeConstants.PROFILE_ID);
					
					ForeseeCSVUtil.log("groupID----------"+ groupID);
				
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
	       } 
		else if (ret.responseCode == 429) {
			ForeseeCSVUtil.recordFailure(groupName + ", Create, " + ret.responseCode + ", " + ret.errorOutput+","+path, ForeseeConstants.OTHER_FAILURE_FILE);
			ForeseeCSVUtil.log(groupName + ", Rate Limit Hit, TRY AGAIN");
		  } 
		else {
			ForeseeCSVUtil.recordFailure(groupName + ", Create, " + ret.responseCode + ", " + ret.errorOutput+","+path, ForeseeConstants.OTHER_FAILURE_FILE);
		}
		return groupID;
	}
	
	/*
	 * Retruns group other than Everyone group
	 */
	public static String getUserGroups(String urlPrefix, String token,  String userName, Proxy proxy){
		
		String path=ForeseeConstants.BLANK_STRING;
		
		String userGroup	=	ForeseeConstants.BLANK_STRING;
		
//		List<String> groupIDList		=	new ArrayList<String>();
		
		 try {
			path = urlPrefix+ForeseeConstants.URL_SUFFIX_USERS+java.net.URLEncoder.encode(userName, "UTF-8")+ForeseeConstants.USER_SUFFIX_GROUP;///api/v1/users/{{userid}}/groups
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		 ForeseeCSVUtil.log("------------getGrouID-----"+path);
		
		Ret ret = OktaConnectionHealper.get(path, token, proxy);
		 ForeseeCSVUtil.log("------------after post-------");
			/*String outstr = null;
			if(ret.output!=null){
	            //outstr = stripsquarebraces(ret.output);
			}*/
		
		if (ret.exception != null) {
			ForeseeCSVUtil.log(userName + "," + ret.exception + ", TRY AGAIN");
		   } 
		else if (ret.responseCode == 201 || ret.responseCode == 200) {
			ForeseeCSVUtil.recordSuccess(userName + "," + ret.output, ForeseeConstants.OTHER_SUCCESS_FILE);
			ForeseeCSVUtil.log("------------ret.output-------"+ret.output);
			
			try {
				JSONParser parser = new JSONParser();
				org.json.simple.JSONArray jsonArray = (org.json.simple.JSONArray) parser.parse(ret.output);
				for (int i = 0; i < jsonArray.size();  i++)
			    {
					org.json.simple.JSONObject groupObject		=	 (org.json.simple.JSONObject) jsonArray.get(i);
					
//					groupIDList.add(	(String) groupObject.get(ForeseeConstants.PROFILE_ID) );
					
					org.json.simple.JSONObject profile		=	 (org.json.simple.JSONObject) groupObject.get(ForeseeConstants.PROFILE);
					
					if(!  "Everyone".equals((String) profile.get("name") ) ) {
						ForeseeCSVUtil.log( "------------Group Name-------"+(String) profile.get("name") );
						return	(String) profile.get("name");
					}
//					groupsMap.put( (String) profile.get("name"), groupID);
			    }
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			
	       } 
		else if (ret.responseCode == 429) {
			ForeseeCSVUtil.recordFailure(userName + ", GetGroups, " + ret.responseCode + ", " + ret.errorOutput, ForeseeConstants.OTHER_FAILURE_FILE);
			ForeseeCSVUtil.log(userName + ", Rate Limit Hit, TRY AGAIN");
		  } 
		else {
			ForeseeCSVUtil.recordFailure(userName + ", GetGroups, " + ret.responseCode + ", " + ret.errorOutput, ForeseeConstants.OTHER_FAILURE_FILE);
		}
		return userGroup;
	}
	
	public static boolean wrapperLCMPost(String url, String token, Proxy proxy, String uid){
		
		boolean lcmStatus	=	false;
		
		Ret ret = OktaConnectionHealper.post(url, token, ForeseeConstants.BLANK_STRING, proxy);
		 ForeseeCSVUtil.log("------------wrapperLCMPost-------"+url);
		 
			String outstr = null;
			if(ret.output!=null){
	            outstr = ForeseeCSVUtil.stripsquarebraces(ret.output);
			    JSONObject obj = new JSONObject(outstr);
				 ForeseeCSVUtil.log("------------wrapperLCMPost   obj-------"+obj);
			}
		
		if (ret.exception != null) {
			ForeseeCSVUtil.log(  "LCM operation exception for "+uid+"   " + ret.exception + ", TRY AGAIN");
		   } 
		else if (ret.responseCode == 201 || ret.responseCode == 200) {
			ForeseeCSVUtil.recordSuccess(uid + "," + ret.output, ForeseeConstants.OTHER_SUCCESS_FILE);
			lcmStatus	=	true;
	       } 
		else if (ret.responseCode == 429) {
			ForeseeCSVUtil.recordFailure(uid + ", LCM, " + ret.responseCode + ", " + ret.errorOutput+","+url, ForeseeConstants.OTHER_FAILURE_FILE);
			ForeseeCSVUtil.log(uid + ", Rate Limit Hit, TRY AGAIN");
		  } 
		else {
			ForeseeCSVUtil.recordFailure(uid + ", LCM, " + ret.responseCode + ", " + ret.errorOutput+","+url, ForeseeConstants.OTHER_FAILURE_FILE);
		}
		return lcmStatus;
	}
	
	public static boolean wrapperLCMDelete(String url, String token, Proxy proxy, String uid){
		
		boolean lcmStatus	=	false;
		
		Ret ret = OktaConnectionHealper.delete(url, token, ForeseeConstants.BLANK_STRING, proxy);
		 ForeseeCSVUtil.log("------------wrapperLCMPost-------"+url);
		 
			String outstr = null;
			if(ret.output!=null){
	            outstr = ForeseeCSVUtil.stripsquarebraces(ret.output);
			    JSONObject obj = new JSONObject(outstr);
				 ForeseeCSVUtil.log("------------wrapperLCMPost   obj-------"+obj);
			}
		
		if (ret.exception != null) {
			ForeseeCSVUtil.log(  "LCM operation exception for "+uid+"   " + ret.exception + ", TRY AGAIN");
		   } 
		else if (ret.responseCode == 201 || ret.responseCode == 200) {
			ForeseeCSVUtil.recordSuccess(uid + "," + ret.output, ForeseeConstants.OTHER_SUCCESS_FILE);
			lcmStatus	=	true;
	       } 
		else if (ret.responseCode == 429) {
			ForeseeCSVUtil.recordFailure(uid + ", LCM, " + ret.responseCode + ", " + ret.errorOutput+","+url, ForeseeConstants.OTHER_FAILURE_FILE);
			ForeseeCSVUtil.log(uid + ", Rate Limit Hit, TRY AGAIN");
		  } 
		else {
			ForeseeCSVUtil.recordFailure(uid + ", LCM, " + ret.responseCode + ", " + ret.errorOutput+","+url, ForeseeConstants.OTHER_FAILURE_FILE);
		}
		return lcmStatus;
	}
	
	public static String wrapperForUserPost(String urlPrefix, String token, String data, Proxy proxy, int rateLimit, String uid){
		String path=ForeseeConstants.BLANK_STRING;
		String createUpdate	=	"Create";
		
		String oktaUserID	=	ForeseeConstants.BLANK_STRING;
		
		path		=	urlPrefix+"/api/v1/users";
		
		boolean status	=	false;
		
		if(ForeseeConstants.BLANK_STRING.equalsIgnoreCase(uid)){//create scenario since okta record is not created yet
			 path = path + "?activate=true&sendEmail=false&&provider=true";

			 ForeseeCSVUtil.log("-----------Create operation------");
		}
		else {//update scenario
			
            path=path+"/"+uid;
//			success_file=UPDATE_SUCCESS_FILE;
//			failure_file=UPDATE_FAILURE_FILE;
			createUpdate	=	"Update";
			 ForeseeCSVUtil.log("-----------Update operation------");
		}
		ForeseeCSVUtil.log("-----------User creation URL------"+path);

		ForeseeCSVUtil.log("-----------User creation data------"+data);
		Ret ret = OktaConnectionHealper.post(path, token, data, proxy);
        
//		if(ForeseeConstants.BLANK_STRING.equalsIgnoreCase(uid)){//create scenario
			String outstr = null;
			if(ret.output!=null){
	            outstr = ForeseeCSVUtil.stripsquarebraces(ret.output);
			    JSONObject obj = new JSONObject(outstr);
			    oktaUserID = obj.getString("id");
			    ForeseeCSVUtil.log("-----------Update operation  ret.output------"+ret.output);
			}
//		}
		
		if (ret.exception != null) {
			ForeseeCSVUtil.log(uid + "," + ret.exception + ", TRY AGAIN");
		   } 
		else if (ret.responseCode == 201 || ret.responseCode == 200) {
			rateLimit = ret.rateLimitLimit;
			ForeseeCSVUtil.recordSuccess(uid + "," + ret.output, ForeseeConstants.USER_SUCCESS_FILE);
			status	=	 true;
	       } 
		else if (ret.responseCode == 429) {
			ForeseeCSVUtil.recordFailure(uid + ", "+createUpdate+", " + ret.responseCode + ", " + ret.errorOutput+","+data, ForeseeConstants.USER_FAILURE_FILE);
			ForeseeCSVUtil.log(uid + ", Rate Limit Hit, TRY AGAIN");
		  } 
		else {
			ForeseeCSVUtil.log("Failed UID----------------------"+uid+"------"+data);
			ForeseeCSVUtil.log(uid + ", "+createUpdate+", "  + ret.responseCode + ", " + ret.errorOutput);
			ForeseeCSVUtil.recordFailure(uid + ", "+createUpdate+", "  + ret.responseCode + ", " + ret.errorOutput+","+data, ForeseeConstants.USER_FAILURE_FILE);
		}
		return	oktaUserID;
	}
	
	/*
	 * Add user to group
	 */
	public static boolean wrapperAddGroup(String urlPrefix, String token, Proxy proxy, String uid, String groupID){
		
		boolean addGroup	=	false;
		
		 String url	=	urlPrefix+ForeseeConstants.GROUP_URL_PREFIX+groupID+"/users/"+uid;
		 
		Ret ret = OktaConnectionHealper.put(url, token, ForeseeConstants.BLANK_STRING, proxy);
		 ForeseeCSVUtil.log("------------wrapperAddGroup-------"+url);
		 
			String outstr = null;
			if(ret.output!=null){
	            outstr = ForeseeCSVUtil.stripsquarebraces(ret.output);
			    JSONObject obj = new JSONObject(outstr);
				 ForeseeCSVUtil.log("------------wrapperAddGroup   obj-------"+obj);
			}
		
		if (ret.exception != null) {
			ForeseeCSVUtil.log(  "wrapperAddGroup operation exception for "+uid+"   " + ret.exception + ", TRY AGAIN");
		   } 
		else if (ret.responseCode == 201 || ret.responseCode == 200 || ret.responseCode == 204) {
			ForeseeCSVUtil.recordSuccess(uid + "," + ret.output, ForeseeConstants.OTHER_SUCCESS_FILE);
			addGroup	=	true;
	       } 
		else if (ret.responseCode == 429) {
			ForeseeCSVUtil.recordFailure(uid + ", ADD Group, " + ret.responseCode + ", " + ret.errorOutput+","+url, ForeseeConstants.OTHER_FAILURE_FILE);
			ForeseeCSVUtil.log(uid + ", Rate Limit Hit, TRY AGAIN");
		  } 
		else {
			ForeseeCSVUtil.recordFailure(uid + ", ADD Group, " + ret.responseCode + ", " + ret.errorOutput+","+url, ForeseeConstants.OTHER_FAILURE_FILE);
		}
		return addGroup;
	}
	
	public static boolean wrapperRemoveGroup(String urlPrefix, String token, Proxy proxy, String uid, String groupID){
		
		boolean removeGroup	=	false;
		
		 String url	=	urlPrefix+ForeseeConstants.GROUP_URL_PREFIX+groupID+"/users/"+uid;
		 
		Ret ret = OktaConnectionHealper.delete(url, token, "", proxy);
		 ForeseeCSVUtil.log("------------wrapperRemoveGroup-------"+url);
		 
			String outstr = null;
			if(ret.output!=null){
	            outstr = ForeseeCSVUtil.stripsquarebraces(ret.output);
			    JSONObject obj = new JSONObject(outstr);
				 ForeseeCSVUtil.log("------------wrapperRemoveGroup   obj-------"+obj);
			}
		
		if (ret.exception != null) {
			ForeseeCSVUtil.log(  "wrapperRemoveGroup operation exception for "+uid+"   " + ret.exception + ", TRY AGAIN");
		   } 
		else if (ret.responseCode == 201 || ret.responseCode == 200 || ret.responseCode == 204) {
			ForeseeCSVUtil.recordSuccess(uid + "," + ret.output, ForeseeConstants.OTHER_SUCCESS_FILE);
			removeGroup	=	true;
	       } 
		else if (ret.responseCode == 429) {
			ForeseeCSVUtil.recordFailure(uid + ", REMOVE Group, " + ret.responseCode + ", " + ret.errorOutput+","+url, ForeseeConstants.OTHER_FAILURE_FILE);
			ForeseeCSVUtil.log(uid + ", Rate Limit Hit, TRY AGAIN");
		  } 
		else {
			ForeseeCSVUtil.recordFailure(uid + ", REMOVE Group, " + ret.responseCode + ", " + ret.errorOutput+","+url, ForeseeConstants.OTHER_FAILURE_FILE);
		}
		return removeGroup;
	}
	
	public static boolean flipFederation(String urlPrefix, String token, Proxy proxy, String uid, boolean toFederation){
		
		boolean flipFederation	=	false;
		
		 String url	=	urlPrefix+"/api/v1/users/"+uid+"/lifecycle/reset_password?sendEmail=false";
		 
		 if(toFederation) {
			 url		+=	"&provider=FEDERATION";	
		 }
		
		Ret ret = OktaConnectionHealper.post(url, token, "", proxy);
		 ForeseeCSVUtil.log("------------flipFederation-------"+url);
		 
			String outstr = null;
			if(ret.output!=null){
	            outstr = ForeseeCSVUtil.stripsquarebraces(ret.output);
			    JSONObject obj = new JSONObject(outstr);
				 ForeseeCSVUtil.log("------------flipFederation   obj-------"+obj);
			}
		
		if (ret.exception != null) {
			ForeseeCSVUtil.log(  "flipFederation operation exception for "+uid+"   " + ret.exception + ", TRY AGAIN");
		   } 
		else if (ret.responseCode == 201 || ret.responseCode == 200 || ret.responseCode == 204) {
			ForeseeCSVUtil.recordSuccess(uid + "," + ret.output, ForeseeConstants.OTHER_SUCCESS_FILE);
			flipFederation	=	true;
	       } 
		else if (ret.responseCode == 429) {
			ForeseeCSVUtil.recordFailure(uid + ", flipFederation, " + ret.responseCode + ", " + ret.errorOutput+","+url, ForeseeConstants.OTHER_FAILURE_FILE);
			ForeseeCSVUtil.log(uid + ", Rate Limit Hit, TRY AGAIN");
		  } 
		else {
			ForeseeCSVUtil.recordFailure(uid + ", flipFederation, " + ret.responseCode + ", " + ret.errorOutput+","+url, ForeseeConstants.OTHER_FAILURE_FILE);
		}
		return flipFederation;
	}
	
	
}
