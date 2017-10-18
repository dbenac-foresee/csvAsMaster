package com.okta.foresee.csv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.okta.foresee.json.JSONObject;
import com.okta.foresee.util.ForeseeCSVUtil;
import com.okta.foresee.util.OktaApiHelper;
import com.okta.foresee.util.OktaConnectionHealper;

/**
 * @author Sivaji Sabbineni
 */
public class CSVNewFeedImporter implements CSVImporter {
	private static int rateLimit;
	private static List<String> attributes;
	private static OktaWorkerQueue oktaWorkerQueue;
	private static String urlPrefix;
	private static String token;
	private static Object userFileLock;
	private static Object errorFileLock;
	private static boolean doProxy;
	private static String port;
	private static String proxyhost;
	private static String username;
	private static String proxyPassword;
	private static String PF_LOCATION_MAP_FILE=null;
	private static	HashMap<String, String> groupsMap	 =	new HashMap<String, String>();

//	private static	HashMap<String, String> groupMap		=	new HashMap<String, String>();
	

	private static List<String> processedUserNames	=	new ArrayList<String>();
	
	public static class Item {
        List<String> items;
        Item(List<String> items) {
			this.items = items;
		}

		synchronized void writeOkta(Proxy proxy) {
			
			String foreseeID = ForeseeConstants.BLANK_STRING;
			String externalID = ForeseeConstants.BLANK_STRING;
			String clientID = ForeseeConstants.BLANK_STRING;
			String userName = ForeseeConstants.BLANK_STRING;
			String firstName = ForeseeConstants.BLANK_STRING;
			String lastName = ForeseeConstants.BLANK_STRING;
			String email = ForeseeConstants.BLANK_STRING;
			String phoneNumber = ForeseeConstants.BLANK_STRING;
			String accountEnabled = ForeseeConstants.BLANK_STRING;
			String userNameSuffix	=	ForeseeConstants.BLANK_STRING;
			String loginName	=	ForeseeConstants.BLANK_STRING;
			String authnProvider	=	ForeseeConstants.BLANK_STRING;
			
			String updateCreate	=	ForeseeConstants.BLANK_STRING;
			String credentials	=	ForeseeConstants.BLANK_STRING;
			
		    String data = ForeseeConstants.OPEN_BRACE;
		    
		    String url	=	urlPrefix;
		    
		    foreseeID = items.get( attributes.indexOf(ForeseeConstants.FORESEE_ID) ).trim();
		    externalID = items.get( attributes.indexOf(ForeseeConstants.EXTERNAL_ID) ).trim();
			clientID = items.get( attributes.indexOf(ForeseeConstants.CLIENT_ID) ).trim();
			userName = items.get( attributes.indexOf(ForeseeConstants.USERNAME) ).trim();
			userNameSuffix	=	items.get( attributes.indexOf(ForeseeConstants.USERNAME_SUFFIX) ).trim();
			firstName = items.get( attributes.indexOf(ForeseeConstants.FIRST_NAME) ).trim();
			lastName = items.get( attributes.indexOf(ForeseeConstants.LAST_NAME) ).trim();
			email = items.get( attributes.indexOf(ForeseeConstants.EMAIL) ).trim();
			String groupName	=	ForeseeConstants.GROUP_PREFIX+items.get(attributes.indexOf(ForeseeConstants.CLIENT_ID) );
			
			//Delete this
//			email	=	email.replaceAll("@", "@sivajitest").trim();
			
			phoneNumber = items.get( attributes.indexOf(ForeseeConstants.PHONE_NUMBER) ).trim();
			accountEnabled = items.get( attributes.indexOf(ForeseeConstants.ACCOUNT_ENABLED) ).trim();
			authnProvider = items.get( attributes.indexOf(ForeseeConstants.AUTHENTICATION_PROVIDER) ).trim();

			//check if all the required attributes are not null/blank
			if(ForeseeConstants.BLANK_STRING.equals(userName) || ForeseeConstants.BLANK_STRING.equals(firstName) || ForeseeConstants.BLANK_STRING.equals(lastName) || ! ForeseeCSVUtil.isValidUserNameFormat(email)){
				 ForeseeCSVUtil.log("recordFailure missing mandatory attributes------");
				 ForeseeCSVUtil.recordFailure("UserName: "+userName + ", " + "FN_LN_REQID_MISSING or EMAIL is not in format", ForeseeConstants.USER_FAILURE_FILE);
                 return;//exit if these fields are blank
			}
			
			//Check if the username is in email format, otherwise append suffix
			loginName	=	ForeseeCSVUtil.isValidUserNameFormat( userName) ? userName	 : userName + ForeseeConstants.AT_SYMBOL+userNameSuffix;
			
			/*//check if the user already processed
			if(processedUserNames.contains(loginName.toLowerCase())) {
				ForeseeCSVUtil.recordFailure("UserName: "+userName + ", " + "Already processed", ForeseeConstants.USER_FAILURE_FILE);
				return;
			}*/
			
			int suffixNumber		=	1;
			
			String userStatus	=	ForeseeConstants.USER_STATUS_ACTIVE;
			
			boolean	isCreate		=	false;
			org.json.simple.JSONObject userObject	=	null;
			
			//identify create or update scenario & generate unique user name for create
			while (true) {
				String userNameFromOkta = ForeseeConstants.BLANK_STRING;
				
				userObject	= ForeseeCSVUtil.searchForExistingUser(urlPrefix, token, proxy, loginName);
				org.json.simple.JSONObject profile	=	null;
				ForeseeCSVUtil.log(" userObject ----"+userObject);
				if(userObject != null ) {
					
					profile		=	 (org.json.simple.JSONObject) userObject.get(ForeseeConstants.PROFILE);
					
					if( profile.get(ForeseeConstants.LOGIN) != null   ) {
						
						
						userNameFromOkta		=	(String) profile.get(ForeseeConstants.LOGIN);
						
						userStatus	=	(String) userObject.get(ForeseeConstants.STATUS);
						
						ForeseeCSVUtil.log(" userStatus-------"+userStatus);
					}
						
				}
				
				
				if ("".equals(userNameFromOkta ) ) {//create user
					ForeseeCSVUtil.log(" user  doesn't exist=="+loginName+"-------Create");
					
					userStatus	=	ForeseeConstants.USER_STATUS_PROVISIONED;
					isCreate		=	true;
					break;
				} else if(userNameFromOkta.equalsIgnoreCase(loginName)  && ! ForeseeConstants.USER_STATUS_DEPROVISIONED.equals( userStatus ) ){ //update user
					
					String foreseeIDFromOkta		=	(String) profile.get(ForeseeConstants.FORESEEID);
					if(!foreseeID.equals(foreseeIDFromOkta)) {
						 ForeseeCSVUtil.log("Foresee ID of the user is not matching with the data recoring failure------");
						 ForeseeCSVUtil.recordFailure("UserName: "+userName + ", " + "Foresee ID of the user is not matching with the data", ForeseeConstants.USER_FAILURE_FILE);
						return; //don't process if the foresee id not matched - as per 10/9 discussion
					}
					
					updateCreate		=	loginName;
					ForeseeCSVUtil.log(" user exists ---------"+loginName+"-------Updating");
					
					break;
				} /*else { //////////////////////////////////////////////////
					//Generate userID
					ForeseeCSVUtil.log(" user exists with NOT matching case");
					
					loginName	=	ForeseeCSVUtil.generateLoginName (loginName,suffixNumber);
					ForeseeCSVUtil.log(" New user----"+loginName);
					
					suffixNumber++;
				}*/  // commented as per the discussion on 10/9
			}
			
			data += ForeseeConstants.QUOTES+ForeseeConstants.JSON_PROFILE_START;
			data += ForeseeConstants.QUOTES + ForeseeConstants.EXTERNALID + ForeseeConstants.QUOTES+ForeseeConstants.COLON+ForeseeConstants.QUOTES+ externalID + ForeseeConstants.QUOTES;
			data += ForeseeConstants.COMA+ForeseeConstants.QUOTES + ForeseeConstants.FORESEEID + ForeseeConstants.QUOTES+ForeseeConstants.COLON+ForeseeConstants.QUOTES + foreseeID+ ForeseeConstants.QUOTES;
			data += ForeseeConstants.COMA+ForeseeConstants.QUOTES + ForeseeConstants.CLIENTID + ForeseeConstants.QUOTES+ForeseeConstants.COLON+ForeseeConstants.QUOTES + clientID+ ForeseeConstants.QUOTES;
			data += ForeseeConstants.COMA+ForeseeConstants.QUOTES + ForeseeConstants.LOGIN + ForeseeConstants.QUOTES+ForeseeConstants.COLON+ForeseeConstants.QUOTES + loginName+ ForeseeConstants.QUOTES;
			data += ForeseeConstants.COMA+ForeseeConstants.QUOTES + ForeseeConstants.FIRSTNAME + ForeseeConstants.QUOTES+ForeseeConstants.COLON+ForeseeConstants.QUOTES + firstName+ ForeseeConstants.QUOTES;
			data += ForeseeConstants.COMA+ForeseeConstants.QUOTES + ForeseeConstants.LASTNAME + ForeseeConstants.QUOTES+ForeseeConstants.COLON+ForeseeConstants.QUOTES + lastName+ ForeseeConstants.QUOTES;
			data += ForeseeConstants.COMA+ForeseeConstants.QUOTES + ForeseeConstants.EMAIL.toLowerCase() + ForeseeConstants.QUOTES+ForeseeConstants.COLON+ForeseeConstants.QUOTES + email+ ForeseeConstants.QUOTES;
			data += ForeseeConstants.COMA+ForeseeConstants.QUOTES + ForeseeConstants.PRIMARY_PHONE + ForeseeConstants.QUOTES+ForeseeConstants.COLON+ForeseeConstants.QUOTES + phoneNumber+ ForeseeConstants.QUOTES+ForeseeConstants.CLOSE_BRACE;
			
			if(ForeseeConstants.FEDERATION.equals(authnProvider) ) {
				data		+=	ForeseeConstants.CREDENTIALS_FEDERATION;
			}
			
			if(isCreate) {
				ForeseeCSVUtil.log("------looking group ID for -----"+groupName);
				ForeseeCSVUtil.log("------lgroupsMap -----"+groupsMap);
				String groupID	=	groupsMap.get(groupName);
				ForeseeCSVUtil.log("------creating user with group-----"+groupID);
				
				/*if(groupID == null ) {
					groupID	=	OktaApiHelper.getGroupID(urlPrefix, token, groupName, proxy);
					ForeseeCSVUtil.log("------in create groupID from API-----"+groupID);
				}*/
				
				data		+=	ForeseeConstants.COMA+ForeseeConstants.QUOTES+ ForeseeConstants.GROUP_IDS+ForeseeConstants.QUOTES+ForeseeConstants.COLON+
						ForeseeConstants.SQ_OPEN_BRACE+ForeseeConstants.QUOTES + groupID+ForeseeConstants.QUOTES +ForeseeConstants.SQ_CLOSE_BRACE;
			}
			
			
			data += ForeseeConstants.CLOSE_BRACE;
			
			ForeseeCSVUtil.log("data----"+data);
			
			//Create or update call
            String	oktaUserID	=	OktaApiHelper.wrapperForUserPost(url, token, data, proxy, rateLimit, updateCreate);	  
            
            //Actvate or deactivate calls
            
            ForeseeCSVUtil.log("accountEnabled----"+accountEnabled);
            
            if(! ForeseeConstants.BLANK_STRING.equals(oktaUserID )  ) { //Post create/update user LCM operation - suspend, unsuspend, deactivate
            	
            		String userOperationLog	=	foreseeID+","+oktaUserID+","+data;
            		ForeseeCSVUtil.foreseeLog(userOperationLog, ForeseeConstants.FORESEE_LOG_FILE);
            	
//            		processedUserNames.add(loginName.toLowerCase());
            		
	            	if( ForeseeConstants.USER_STATUS_PROVISIONED.equals( userStatus ) && ForeseeConstants.STATUS_N.equalsIgnoreCase( accountEnabled )) {
	            		//Active to suspend
	            		ForeseeCSVUtil.log("user status PROVISIONED & Account disabled----"+loginName+"==Suspend");
	            		url	=	urlPrefix+ ForeseeConstants.URL_SUFFIX_USERS +loginName+ForeseeConstants.URL_SUFFIX_SUSPEND;
            			
            			ForeseeCSVUtil.log("Calling wrapperLCMPost=="+url);
            			
            			OktaApiHelper.wrapperLCMPost(url, token, proxy, loginName);
	            		
	            		
	            } else if( ForeseeConstants.USER_STATUS_SUSPENDED.equals( userStatus ) && ForeseeConstants.STATUS_Y.equalsIgnoreCase( accountEnabled )) {
	            		//suspend to unsuspend
            			ForeseeCSVUtil.log("user status SUSPENDED & Account enabled----"+loginName+"==unsuspend");
	            	
            			url	=	urlPrefix+ ForeseeConstants.URL_SUFFIX_USERS +loginName+ ForeseeConstants.URL_SUFFIX_UNSUSPEND;
            			
            			ForeseeCSVUtil.log("Calling wrapperLCMPost=="+url);
            			
            			OktaApiHelper.wrapperLCMPost(url, token, proxy, loginName);
	            	
	            } else if( ForeseeConstants.USER_STATUS_DEPROVISIONED.equals( userStatus ) && ForeseeConstants.STATUS_Y.equalsIgnoreCase( accountEnabled )) {
	            		//Deactive to Active
            			ForeseeCSVUtil.log("user status DEPROVISIONED & Account enabled----"+loginName+"==activating");
	            	
            			url	=	urlPrefix+ ForeseeConstants.URL_SUFFIX_USERS +loginName+ForeseeConstants.URL_SUFFIX_ACTIVATE;//"/lifecycle/activate?sendEmail=false";
            			
            			ForeseeCSVUtil.log("Calling wrapperLCMPost=="+url);
            			
            			OktaApiHelper.wrapperLCMPost(url, token, proxy, loginName);
	            }
            } 
            
            
            if(! isCreate) { //update scenario - Update group membership, Update Authentication provider
            
            		String userGroup		=	OktaApiHelper.getUserGroups(urlPrefix,  token,  oktaUserID, proxy);
            		
            		ForeseeCSVUtil.log("userGroup--------------"+userGroup+"-----------groupName----------"+groupName);
            		
            		if(! userGroup.equals(groupName) ) {
            			ForeseeCSVUtil.log("in side modify group--------------");
            			boolean modifyGroup	=	OktaApiHelper.wrapperRemoveGroup(urlPrefix, token, proxy, oktaUserID, groupsMap.get(userGroup));
            			
            			if(modifyGroup) {
            				ForeseeCSVUtil.log(userGroup+" Removed-------");
            				modifyGroup	=	OktaApiHelper.wrapperAddGroup(urlPrefix, token, proxy, oktaUserID, groupsMap.get(groupName));
            				ForeseeCSVUtil.log(groupName+" Added-------");
            			}
            		}
            		
            		
            		String credential	=	ForeseeCSVUtil.getCredential(userObject);
            		
            		String oktaAuthnProvider	=	!authnProvider.equals(ForeseeConstants.FEDERATION) ? "OKTA" : authnProvider;
            		
            		ForeseeCSVUtil.log("credential--------------"+credential+"-----------authnProvider----------"+oktaAuthnProvider);
            		
            		if(! ForeseeConstants.BLANK_STRING.equals(credential) && ! credential.equals(oktaAuthnProvider ) ) {
            			
            			boolean filipFederation	=	OktaApiHelper.flipFederation (urlPrefix,  token,  proxy,  oktaUserID, ForeseeConstants.FEDERATION.equals(oktaAuthnProvider));
            			ForeseeCSVUtil.log(" filipFederation status-------"+filipFederation);
            			
            		}
//            		if(! ForeseeConstants.FEDERATION.equals(authnProvider))
            	
            }
            
        }
		
		synchronized void deleteFromOkta(Proxy proxy) {
			
			String loginName	=	ForeseeConstants.BLANK_STRING;
			String userName	=	ForeseeConstants.BLANK_STRING;
		    String url	=	urlPrefix;
			String userNameSuffix	=	ForeseeConstants.BLANK_STRING;
		    
			userName = items.get( attributes.indexOf(ForeseeConstants.USERNAME) ).trim();
			userNameSuffix	=	items.get( attributes.indexOf(ForeseeConstants.USERNAME_SUFFIX) ).trim();

			loginName	=	ForeseeCSVUtil.isValidUserNameFormat( userName) ? userName	 : userName + ForeseeConstants.AT_SYMBOL+userNameSuffix;
			
	        url	=	urlPrefix+ForeseeConstants.URL_SUFFIX_USERS+loginName+ForeseeConstants.URL_SUFFIX_DEACTIVATE;
	        
	        
	        ForeseeCSVUtil.log("Calling wrapperLCMPost---"+url);
	      
	      //Create or update call
	        boolean	lcmStatus	=	OktaApiHelper.wrapperLCMPost(url, token, proxy, loginName); 
	        
//	        if(lcmStatus) {
        		ForeseeCSVUtil.log("Calling wrapperLCMDelete-----"+url);
        		lcmStatus	=	OktaApiHelper.wrapperLCMDelete(urlPrefix+ForeseeConstants.URL_SUFFIX_USERS+loginName, token, proxy, loginName); 
//	        }
	        
	    }
	}
	

	private static class OktaWorkerQueue {
        private final BlockingQueue<Item> workQueue;
		private final ExecutorService service;

		private int workLeft() {
			return workQueue.size();
		}

		private OktaWorkerQueue(int numWorkers, int workQueueSize) {
			workQueue = new LinkedBlockingQueue<Item>(workQueueSize);
			service = Executors.newFixedThreadPool(numWorkers);
			for (int i = 0; i < numWorkers; i++) {
				service.submit(new Worker(workQueue));
			}
			
			service.shutdown();
			
		}

		private void produce(Item item) {
			try {
				workQueue.put(item);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}

		private class Worker implements Runnable {

			private final BlockingQueue<Item> workQueue;
			private final Proxy proxy;

			public Worker(BlockingQueue<Item> workQueue) {
				this.workQueue = workQueue;
				if ( doProxy) {
					int portInt = Integer.parseInt(port);
					this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyhost, portInt));
					Authenticator authenticator = new Authenticator() {
						public PasswordAuthentication getPasswordAuthentication() {
							return (new PasswordAuthentication(username, proxyPassword.toCharArray()));
						}
					};
					Authenticator.setDefault(authenticator);
				} else {
					this.proxy = null;
				}
			}

			@Override
			public void run() {
				try {
					while (!Thread.currentThread().isInterrupted()) {
						try {
							Item item = workQueue.take();
//							ForeseeCSVUtil.log("item in run----"+item+"----==proxy=="+proxy);
							item.writeOkta(proxy);
//							item.deleteFromOkta(proxy);
						} catch (InterruptedException ex) {
							ex.printStackTrace();
							Thread.currentThread().interrupt();
							break;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}


	@Override
	public void executeImport(String[] args) {
//		rateLimit = 600;
		int count = 0;
		userFileLock = new Object();
		errorFileLock = new Object();
		urlPrefix = args[0];
		token = args[1];
		if (args.length > 4) {
			PF_LOCATION_MAP_FILE = args[4];
		} 
		BufferedReader br=null;
		String available;
		ArrayList<Item> csvLines	=	new ArrayList<Item>();
		
		
		try{
			//REad the CSV
			br = new BufferedReader(new FileReader(new File(args[2])));  
			attributes = ForeseeCSVUtil.parseLine(br.readLine());
			
			if(attributes != null ) {
				//Get all Okta Groups
				groupsMap	=	OktaApiHelper.getAllGroups(urlPrefix, token);
				
				ForeseeCSVUtil.log("All the existing Groups ---- "+groupsMap);
			}
			HashSet<String> groupNameSet	 =	new HashSet<String>();
			
			
			while((available = br.readLine()) != null) {
	      		List<String> line = ForeseeCSVUtil.parseLine(available);
	      		
	      		String foreseeID		=	(String) line.get(attributes.indexOf(ForeseeConstants.FORESEE_ID));
	      		
	      		if(! processedUserNames.contains(foreseeID))  {
	      			csvLines.add(new Item(line));
	      			processedUserNames.add(foreseeID);
	      		}

	      		String groupName	=	ForeseeConstants.GROUP_PREFIX+line.get(attributes.indexOf(ForeseeConstants.CLIENT_ID) );
				String groupDescription	=	"Group for "+ line.get( attributes.indexOf(ForeseeConstants.NAME) ).trim();
				
				if(!groupNameSet.contains( groupName )    &&  ! groupsMap.containsKey( groupName)) {
					
					String groupID	=	OktaApiHelper.wrapperGroupPost(urlPrefix,token, groupName, groupDescription);
					
					if(! ForeseeConstants.BLANK_STRING.equals(groupID)) {
						groupNameSet.add(groupName);
						groupsMap.put( groupID, groupName);
					}
				}
			}
			ForeseeCSVUtil.log("All Groups in Okta "+groupsMap);
		
			oktaWorkerQueue = new OktaWorkerQueue(10, 20);
			
			ForeseeCSVUtil.log("csvLines.size-------"+csvLines.size());
			
			for (Item itm : csvLines) {
				
				count++;
				ForeseeCSVUtil.log("Processing record: "+count+
						"----RATE_LIMIT_USER_UPDATE------"+OktaConnectionHealper.RATE_LIMIT_USER_UPDATE+
						"----RATE_LIMIT_USER_UPDATE------"+OktaConnectionHealper.RATE_LIMIT_USER_UPDATE+
						"----RATE_LIMIT_GROUP_CREATE"+OktaConnectionHealper.RATE_LIMIT_GROUP_CREATE+
						"----RATE_LIMIT_GROUP_UPDATE"+OktaConnectionHealper.RATE_LIMIT_GROUP_UPDATE);
				
//				if (RATE_LIMIT_USER_UPDATE-80 < 0 || OktaApiHelper.RATE_LIMIT_UPDATE-80 < 0) {
				if( OktaConnectionHealper.RATE_LIMIT_USER_UPDATE-80 <0 || OktaConnectionHealper.RATE_LIMIT_USER_CREATE-80 < 0 || OktaConnectionHealper.RATE_LIMIT_GROUP_CREATE-80 <0 || OktaConnectionHealper.RATE_LIMIT_GROUP_UPDATE-80< 0 ) {
					try {

//						ForeseeCSVUtil.log("RATE_LIMIT_RESET------"+OktaApiHelper.RATE_LIMIT_RESET);
//						ForeseeCSVUtil.log("Current time----------"+System.currentTimeMillis() / 1000L);
						
//						long timeOut	=	(OktaApiHelper.RATE_LIMIT_RESET - System.currentTimeMillis() / 1000L) > 61 ? (OktaApiHelper.RATE_LIMIT_RESET - System.currentTimeMillis() / 1000L): 61;
						long timeOut	=	61;
						ForeseeCSVUtil.log("waiting for "+timeOut+" secs for rate limit");
						Thread.sleep(timeOut *1000); 
//						Thread.sleep(61*1000 );
						
						//Reset the rate limits after timeout
						OktaConnectionHealper.RATE_LIMIT_USER_UPDATE	=	600;
						OktaConnectionHealper.RATE_LIMIT_USER_UPDATE	=	600;
						OktaConnectionHealper.RATE_LIMIT_GROUP_CREATE	=	600;
						OktaConnectionHealper.RATE_LIMIT_GROUP_UPDATE	=	600;

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
				oktaWorkerQueue.produce(itm);
				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
	         if (br != null) {
	            try {
	               br.close();
	            } catch (IOException e) {
	               e.printStackTrace();
	            }
	         }
		}
	         
		while (true) {
			int owl = oktaWorkerQueue.workLeft();
			if (owl == 0) {
				try {
					Thread.sleep(10000);//how much 
				} catch (Exception e) {
					e.printStackTrace();
				}
				ForeseeCSVUtil.log("Load Completed!");
				
					Calendar cal = Calendar.getInstance();
			        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
					sdf = new SimpleDateFormat("HH:mm:ss");
			        ForeseeCSVUtil.log( "Load End time----------"+sdf.format(cal.getTime()) );
			        System.out.println( "Load end time----------"+sdf.format(cal.getTime())  );
			        ForeseeCSVUtil.log("CSVImportsMain End==");
			        
				System.exit(0);
			}
			try {
				Thread.sleep(1 * 60 * 1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public CSVNewFeedImporter() {

	}
}