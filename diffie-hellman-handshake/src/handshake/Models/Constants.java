package handshake.Models;

public class Constants {
	 //----------------------
     // MTE Client ID header
     //----------------------
     public static String ClientIdHeader = "x-client-id";
    
     // ------------------
     // Set Rest API URL
     // ------------------
     // Use this URL when running API locally
     // public static String RestAPIName = "http://localhost:52603";
     // Use this URL to use public Eclypses API
     public static String RestAPIName = "https://dev-echo.eclypses.com";
     public static String JsonContentType = "application/json";
     public static String TextContentType = "text/plain";
     
     //---------------
     // RestApi Routes
     //---------------
     public static String HandshakeRoute = "/api/handshake";
     
     //-------------------
     // result codes
     //-------------------
     public static String STR_SUCCESS = "SUCCESS";
     public static String RC_SUCCESS = "000";
     public static String RC_VALIDATION_ERROR = "100";
     public static String RC_HTTP_ERROR = "200";
     public static String RC_HTTP_EXCEPTION = "201";
     public static String RC_HANDSHAKE_EXCEPTION = "302";
}
