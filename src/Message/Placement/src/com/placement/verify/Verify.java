package com.placement.verify;
/* STILL TO BE DONE
 * 
 * The appURL to be placed
 * Check for hashcode length
 * checkSubcribed 
 * addmobileHash
 *
 */
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLEncoder;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


@SuppressWarnings("serial")
public class Verify extends HttpServlet 
{
	final String START_RESPONSE = "<html>"
	            +"<head>"
	            +"<meta name='txtweb-appkey' content='64f42155-855a-4bef-9549-883c7da61a06'>"
	            +"</head>"
	            +"<body>";
	
    final String END_RESPONSE = "</body></html>";
	final String VERIFYSERVICE_APIURL = "http://api.txtweb.com/v3/verify";
	final String SUCCESS_CODE = "0";
	final String APP_URL = "place-your-web-application's-url-here";
	private void sendResponse(HttpServletResponse httpResponse, String response) throws IOException
	{
		httpResponse.setContentType("text/html");
        try(PrintWriter out = httpResponse.getWriter())
        {   
            out.println(this.START_RESPONSE+response+this.END_RESPONSE);
        }
    }
		
	private static String getTagValue(String tagName, Element element) 
	{
	     NodeList nodeList = element.getElementsByTagName(tagName).item(0).getChildNodes();
	     Node node = nodeList.item(0);
	        return node.getNodeValue();
	}
		
	private boolean verifyRequestFromTXTWEB(HttpServletResponse resp,String message, String mobileHash, String verifyId,String protocol) throws IOException
	{
        boolean textWebRequest = false;
		String urlParams =     "txtweb-message="+URLEncoder.encode(message,"UTF-8")
                +"&txtweb-mobile="+URLEncoder.encode(mobileHash,"UTF-8")
                +"&txtweb-verifyid="+URLEncoder.encode(verifyId,"UTF-8")
                +"&txtweb-protocol="+URLEncoder.encode(protocol,"UTF-8");

		//Using DOM parser to parse the XML response from the API
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document doc =null;
		try{
		DocumentBuilder db = dbf.newDocumentBuilder();
		doc = db.parse(new URL(this.VERIFYSERVICE_APIURL+"?"+urlParams).openStream());
		}
		catch(ParserConfigurationException p)
		{
		p.printStackTrace();
		System.out.println("Could Not get DocumentBuilder: Some weird error");
		}
		catch(SAXException s)
		{
		s.printStackTrace();
		System.out.println("No internet or txtweb offline or no response from the api");
		}
		if(doc != null)
		{
		    NodeList childNodes = doc.getChildNodes();
		    String code = "-1";
		    String appUrl = "";
		    for(int index = 0; index < childNodes.getLength(); index++)
		    {
		        Node childNode = childNodes.item(index);
		        if( childNode.getNodeType() == Node.ELEMENT_NODE )
		        {
		            Element element = (Element) childNode;
		            code = getTagValue("code", element);
		            appUrl = getTagValue("url", element);
		            //if statusCode is 0 and url matches with the web application URL, 
		            //then the request has been successfully verified by txtWeb
		            if (this.SUCCESS_CODE.equals(code) && this.APP_URL.equals(appUrl))
		            {
		                //request successfully verified by txtWeb using the verify service API
		                textWebRequest = true;
		            }
		            else
		            {
		                //request not verified by txtWeb using the verify service API
		                textWebRequest = false;
		                sendResponse(resp, "Sorry, Invalid Request/ Use of API");
		            }
		        }
		    }
		}
		return textWebRequest;
	}
	
	private boolean verifyRequest(HttpServletResponse resp, String message,String mobileHash,String verifyId,String protocol) throws IOException
	{
		if(message == null || mobileHash == null || protocol == null || verifyId == null){
            //these parameters are mandatory to call the verify service API
            sendResponse(resp,"Sorry We couldnot verify. Please try again.");
            return false;
        }
		// we have to verify whether the servlet is executed by txtweb or someone else
		boolean textWebRequest = verifyRequestFromTXTWEB(resp,message,mobileHash,verifyId,protocol); 
	    // Now the request has been verified that it is from textweb
		//Check whether the request is in the subscribe data-store
		boolean validated = false;
		if(textWebRequest)
		{
			validated = verifyRequestFromSubscribe(resp,message,mobileHash,verifyId,protocol);
		}
		return validated;
	}
	
	private boolean verifyRequestFromSubscribe(HttpServletResponse resp, String message,String mobileHash,String verifyId,String protocol) throws IOException
	{
		String mess[] = null;
        boolean validated = false;
		mess = message.split(" ");
		if(mess.length != 2 || mess[0].length() != 10 )
		{
			sendResponse(resp, "Incorrect format of text message/ USN/ HASH, please try again");
		}
		else if(checkSubcribed(mess[0],mess[1]))
		{
			validated = true;
		}
		else
		{
			// If not present then return error indicating the error
			sendResponse(resp, "We couldnot authenticate Your request, try again");
		}
		return validated;
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)	throws IOException 
	{
		//get request parameters : especially the encrypted text
		String message = req.getParameter("txtweb-message");
        //extract their hashed mobile number
        String mobileHash = req.getParameter("txtweb-mobile");
        //extract the txtWeb verify ID
        String verifyId = req.getParameter("txtweb-verifyid");
        //extract the protocol of the origin of user request
        String protocol = req.getParameter("txtweb-protocol");
        //perform the verify service call here and parse the XML response returned by it
        boolean validated = verifyRequest(resp, message, mobileHash, verifyId, protocol);
		// Validated request now must be added to mobile hash data store
		if(validated)
		{
			String []mess = message.split(" ");
			addMobileHash(mess[0],mess[1]);
		}
        
	}
	
	private void addMobileHash(String string, String string2) 
	{
		
	}

	private boolean checkSubcribed(String usn, String hash) {

		return false;
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException 
	{
		doGet(req,resp);
	}
	
}





