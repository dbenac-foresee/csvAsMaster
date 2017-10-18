

import java.io.FileInputStream;
import java.io.FileReader;
import java.util.HashSet;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class JsonParserTest {

private static final String filePath = "/Users/sivaji/Desktop/sampleuser.txt";
	
	public static void main(String[] args) {

		try {
			// read the json file
			FileReader reader = new FileReader(filePath);
			
			
			
			FileInputStream fileInputStream = null;
			String data="";
			StringBuffer stringBuffer = new StringBuffer("");
			
			
			try{
			    fileInputStream=new FileInputStream(filePath);
			    
			    
			    int i;
			    while((i=fileInputStream.read())!=-1)
			    {
			        stringBuffer.append((char)i);
			    }
			    data = stringBuffer.toString();
			    //System.out.println("Data ===="+data);
			}
			catch(Exception e){
			        System.out.println(e);
			}
			finally{
			    if(fileInputStream!=null){  
			        fileInputStream.close();
			    }
			}
			
			
			
			JSONParser parser = new JSONParser();
			
			org.json.simple.JSONArray jsonArray= (org.json.simple.JSONArray) parser.parse(data);
			
			
			
//			System.out.println("Total users======"+jsonArray.size());
			
			/*org.json.simple.JSONObject jsonObject		=	 (JSONObject) jsonArray.get(1);
			
			org.json.simple.JSONObject profile		=	 (JSONObject) jsonObject.get("profile");
			
			//if(profile.get("secondEmail") != )
			
			System.out.println("secondEmail======"+ (profile.get("secondEmail")).toString().length() );
			*/
			
			HashSet<String> set=new HashSet<String>();  
			
			for (int i = 0; i < jsonArray.size();  i++)
		    {
//				System.out.println("First Element======"+jsonArray.get(i));
				
				
				org.json.simple.JSONObject userObject		=	 (org.json.simple.JSONObject) jsonArray.get(i);
					
				String groupID	=	(String) userObject.get("id");
				org.json.simple.JSONObject profile		=	 (JSONObject) userObject.get("profile");
				
				
				System.out.println("groupID==="+groupID + "Group Name-----"+profile.get("name"));
				
				
				
				/*if( profile.get("name") != null   ) {
					int secondEmailSize	=	(profile.get("name")).toString().length();
					
					if (secondEmailSize > 0 ) {
						System.out.println(profile.get("login"));
					}
				}
				*/
				
				
				
//				System.out.println("secondEmail======"+ profile.get("secondEmail") );
				
		    }
				
				
//			System.out.println("set======"+ set.contains("APIGroup"));
			
			
			
			
			
//			for(Iterator iterator = jsonObject.keySet().iterator(); iterator.hasNext();) {
//			    String key = (String) iterator.next();
//			    System.out.println("Key====="+key+"======Value === "+jsonObject.get(key));
//			}
			
			
			

		}  catch (Exception ex) {
			System.out.println("====Exception: "+ex.getMessage());
		} 



	}

}
