package com.okta.foresee.csv;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Logger;

import com.okta.foresee.util.ForeseeCSVUtil;
import com.okta.foresee.util.OktaApiHelper;

/**
 *
 * @author Sivaji Sabbineni
 */
public class CSVImportsMain {
	 private static final Logger LOGGER = Logger.getLogger(CSVImportsMain.class.getName());
	
    public static void main(String[] args) throws ParseException {

    		ForeseeCSVUtil.log("CSVImportsMain Start------");

		Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        ForeseeCSVUtil.log( "Load start time-----------"+sdf.format(cal.getTime()) );
        System.out.println( "Load start time----------"+sdf.format(cal.getTime())  );
       CSVImporter ci = new  CSVNewFeedImporter();
        ci.executeImport(args);
       
  }
    
}
