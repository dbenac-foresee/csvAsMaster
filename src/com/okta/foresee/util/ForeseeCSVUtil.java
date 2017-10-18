/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.okta.foresee.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.okta.foresee.csv.ForeseeConstants;

/**
*
* @author Sivaji Sabbineni
*/
public class ForeseeCSVUtil {

    private static final char DEFAULT_SEPARATOR = ',';
    private static final char DEFAULT_QUOTE = '"';
    private static Object userFileLock=new Object();
    private static Object errorFileLock=new Object();
	private static Object logFileLock=new Object();
	private static Object foreseelogFileLock=new Object();

    public static List<String> parseLine(String cvsLine) {
    	 return parseLine(cvsLine, DEFAULT_SEPARATOR, DEFAULT_QUOTE);//,DEFAULT_TAB);
    }

    public static List<String> parseLine(String cvsLine, char separators) {
        return parseLine(cvsLine, separators, DEFAULT_QUOTE);
    }

    public static List<String> parseLine(String cvsLine, char separators, char customQuote) {
       List<String> result = new ArrayList<>();

        //if empty, return!
        if (cvsLine == null || cvsLine.isEmpty()) {
            return result;
        }

        if (customQuote == ' ') {
            customQuote = DEFAULT_QUOTE;
        }

        if (separators == ' ') {
            separators = DEFAULT_SEPARATOR;
        }

        StringBuffer curVal = new StringBuffer();
        boolean inQuotes = false;
        boolean startCollectChar = false;
        boolean doubleQuotesInColumn = false;
        char[] chars = cvsLine.toCharArray();

        for (char ch : chars) {
             if (inQuotes) {
                startCollectChar = true;
                if (ch == customQuote) {
                    inQuotes = false;
                    doubleQuotesInColumn = false;
                } else {
                    //Fixed : allow "" in custom quote enclosed
                    if (ch == '\"') {
                        if (!doubleQuotesInColumn) {
                            curVal.append(ch);
                            doubleQuotesInColumn = true;
                        }
                    } else {
                        curVal.append(ch);
                    }
                 }
            } else {
                if (ch == customQuote) {
                    inQuotes = true;
                    //double quotes in column will hit this!
                    if (startCollectChar) {
                        curVal.append('"');
                    }

                } else if (ch == separators || ch =='\t') {//adding support for tab-delim

                    result.add(curVal.toString());
                    curVal = new StringBuffer();
                    startCollectChar = false;

                } else if (ch == '\r') {
                    //ignore LF characters
                    continue;
                }
                else if (ch == '\n') {
                    //the end, break!
                    break;
                } else {
                    curVal.append(ch);
                }
            }

        }

        result.add(curVal.toString());

        return result;
    }
    
    public static String stripsquarebraces(String rstr) {
		if(rstr==null)
			return ForeseeConstants.BLANK_STRING;//throw an error message here
        rstr = rstr.substring(rstr.indexOf("{"), rstr.lastIndexOf("}") + 1);
		return rstr;
	}
	
	public static void recordSuccess(String string, String fileName) {
		try {
			synchronized (userFileLock) {
				PrintWriter pw = new PrintWriter(new FileOutputStream(new File(fileName), true));
				pw.println(string+", " + new Date());
				pw.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void log(String string) {
		try {
			synchronized (logFileLock) {
				PrintWriter pw = new PrintWriter(new FileOutputStream(new File("foresee.log"), true));
				pw.println( new Date()+": "+string);
				pw.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void logEmail(String string) {
		try {
			synchronized (logFileLock) {
				PrintWriter pw = new PrintWriter(new FileOutputStream(new File("foresee-email.csv"), true));
				pw.println( string);
				pw.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void recordFailure(String string, String fileName) {
		try {
			synchronized (foreseelogFileLock) {
				PrintWriter pw = new PrintWriter(new FileOutputStream(new File(fileName), true));
				pw.println(string+", " + new Date());
				pw.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void foreseeLog(String string, String fileName) {
		try {
			synchronized (errorFileLock) {
				PrintWriter pw = new PrintWriter(new FileOutputStream(new File(fileName), true));
				pw.println(string+", " + new Date());
				pw.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    
	public static boolean isValidUserNameFormat(String login) {
//        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
		String ePattern = "[\\p{L}\\p{Digit}\\-\\!\\#\\$\\%\\&\\*\\+\\-\\/\\=\\?\\^\\_\\{\\|\\}]+(?:\\.[\\p{L}\\p{Digit}\\-\\!\\#\\$\\%\\&\\*\\+\\-\\/\\=\\?\\^\\_\\{\\|\\}]+)*@(?:[\\p{L}\\p{Digit}](?:[\\p{L}\\p{Digit}-]*[\\p{L}\\p{Digit}])?\\.)+[\\p{L}]{2,20}";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(login);
        return m.matches();
	}
	
	  public static String generateLoginName(String loginName, int suffixNumber) {
		  
		  int index	=	(suffixNumber == 1) ? loginName.lastIndexOf('.') : loginName.lastIndexOf('.')-1;
		  
		  return loginName.substring(0, index) + suffixNumber + loginName.substring(loginName.lastIndexOf('.'));
	  }
	  public static org.json.simple.JSONObject searchForExistingUser(String urlPrefix, String token, Proxy proxy, String value) {

			String findpath = ForeseeConstants.USER_FIND_PATH + ForeseeConstants.QUOTES+value+ForeseeConstants.QUOTES;
			String path = ForeseeConstants.BLANK_STRING;
			
			org.json.simple.JSONObject userObject	=	null;	

			try {
				path = urlPrefix + ForeseeConstants.USER_SEARCH_PATH + java.net.URLEncoder.encode(findpath, "UTF-8");
			 }catch (UnsupportedEncodingException e) {
				  e.printStackTrace();
			    }
			
		
			Ret ret = OktaConnectionHealper.get(path, token, proxy);
			
			if (ret.responseCode == 200 && ret.emptyOutput.equals(Boolean.FALSE)) {
				JSONParser parser = new JSONParser();
				
				try {
					JSONArray jsonArray= (JSONArray) parser.parse(ret.output.toString());
					userObject		=	 (org.json.simple.JSONObject) jsonArray.get(0);
				} catch (ParseException e) {
					e.printStackTrace();
				}
				
			}
			
			return userObject;
		}
	  
	  public static String getCredential(org.json.simple.JSONObject userObject) {

			String credential	=	"";
			
			org.json.simple.JSONObject credentials	=	(org.json.simple.JSONObject) userObject.get("credentials");
			
			org.json.simple.JSONObject provider	=	(org.json.simple.JSONObject) credentials.get("provider");
			
			credential	=	(String) provider.get("name");
			
			ForeseeCSVUtil.log(" credential-------"+credential);
			
			return credential;
		}

}