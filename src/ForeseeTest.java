import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.okta.foresee.util.ForeseeCSVUtil;
import com.okta.foresee.util.OktaApiHelper;

public class ForeseeTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String urlprefix	=	"https://shivaji.oktapreview.com";
		String token		=	"001f5GpDXB4HaNVHNx9n3pkEn4YLexAHXTCpfKxh6e";
//		System.out.println( OktaApiHelper.wrapperRemoveGroup(urlprefix, token, null, "00uccn6ejl1Nnvd8S0h7", "00gceoe03bmWfVlzD0h7") );
//		System.out.println( OktaApiHelper.flipFederation (urlprefix,  token,  null,  "00uccn3zakT0f0kzf0h7", false));
		
//		System.out.println( OktaApiHelper.getUserGroups(urlprefix,  token,  "00uccn3zakT0f0kzf0h7", null));
		
//		System.out.println( OktaApiHelper.getAllGroups(urlprefix,  token));
		
		System.out.println( ForeseeCSVUtil.isValidUserNameFormat("ddfsv#af@dasd.com"));
	}

}
