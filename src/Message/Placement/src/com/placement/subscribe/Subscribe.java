package com.placement.subscribe;

import java.util.*;
import java.io.IOException;
import java.io.PrintWriter; 
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;


@SuppressWarnings("serial") 
public class Subscribe extends HttpServlet{
	private String subscribeRequestDataStore ="SubscribeRequestDataStore";
	private String subscribeEntity ="SubscribeRequest";
	private String verificationProp = "verificationhash";
	final String START_RESPONSE = "<html>"
            +"<head>"
            +"</head>"
            +"<body>";

	final String END_RESPONSE = "</body></html>";
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)	throws IOException 
	{
		String type = req.getParameter("type");
		String usn = req.getParameter("usn");
		String hash = req.getParameter("hash");
		String usnList[] = usn.split(";");
		String hashList[] = hash.split(";");
		
		if(usnList.length != hashList.length)
		{
			sendResponse(resp, "Sorry. The Length of the USN List and the length of the hashkeys are not the same");
			return;
		}
		else if(type.equals("0"))
		{
			int i =0;
			for(String USN: usnList)
			{
				if(addToSubscribeRequest(USN, hashList[i]))
				{
					sendResponse(resp, "added"+usn+'\n');
				}else
				{
					sendResponse(resp, "not added"+usn);
				}
				++i;
			}
			
		}else if(type.equals("1"))
		{
			int i =0;
			for(String USN: usnList)
			{
				removeSubscribeRequest(USN, hashList[i]);
				++i;
			}
		}
	}
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException 
	{
		doGet(req,resp);
	}
	private boolean addToSubscribeRequest(String usn,String verificationHash)
	{
		 boolean successful = false;
		 DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		 Key usnKey = KeyFactory.createKey("SubscribeRequestDataStore", usn);
		 // Run an ancestor query to ensure we see the most up-to-date
		 // view of the SubscribeRequests belonging to the selected USN.
		 Query query = new Query("SubscribeRequest", usnKey);
		 List<Entity> mobileList = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(2));
		 if(mobileList.isEmpty())
		 {
			// The user is subscribing for the first time
		    	Entity usnEntity = new Entity("SubscribeRequest",usnKey);
		    	usnEntity.setProperty("verificationhash", verificationHash);
		    	datastore.put(usnEntity);
		    	successful = true;
		 }
		 else if(mobileList.size() == 1)
		 {
			Iterator<Entity> usnEntity = mobileList.iterator();
			Entity verificationHashPresentForTheUSN = usnEntity.next();
			verificationHashPresentForTheUSN.setProperty("verificationhash", verificationHash);
			datastore.put(verificationHashPresentForTheUSN);
			successful = true;
		 }else
		 {
			 //Something wrong, we do not allow one person to register 2 verification keys
			 successful = false;
		 }
		return successful;
	}
	private boolean removeSubscribeRequest(String usn,String verificationHash)
	{ 
		 boolean successful = false;
		 DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		 Key usnKey = KeyFactory.createKey(this.subscribeRequestDataStore, usn);
		 // Run an ancestor query to ensure we see the most up-to-date
		 // view of the SubscribeRequests belonging to the selected USN.
		 Query query = new Query(this.subscribeEntity, usnKey);
		 List<Entity> mobileList = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(2));
		 if(mobileList.isEmpty())
		 {
			// Nothing to remove
		    	
		 }
		 else if(mobileList.size() == 1)
		 {
			
			Iterator<Entity> usnEntity = mobileList.iterator();
			Entity verificationHashPresentForTheUSN = usnEntity.next();
			if(verificationHash.equals(verificationHashPresentForTheUSN.getProperty(this.verificationProp)))
			{
				verificationHashPresentForTheUSN.removeProperty(this.verificationProp);
				datastore.put(verificationHashPresentForTheUSN);
				successful = true;
			}
			else{
				successful = false;
			}
		 }else
		 {
			 //Something wrong, we do not allow one person to register 2 verification keys
			 // Should we remove those inconsistencies
			 // Probably yes if something catastrophic is wrong then allow the poor user to
			 // start fresh
			 // But what if its due to the time needed from consensus
			 // But, from '3db' Its no longer an issue and my app will be communicating
			 // to a single replica. What if the master/coordinator changes??. The previous coordiator
			 // will return network address of new coordinator internally.
			 
			 for(Entity usnEntity : mobileList)
			 {
				 if(successful && verificationHash.equals(usnEntity.getProperty(this.verificationProp)))
					{
						usnEntity.removeProperty(this.verificationProp);
						datastore.put(usnEntity);
						successful = true;
					}
					else{
						successful = false;
					} 
			 }
			 successful = false;
		 }
		return successful;
		}
	private void sendResponse(HttpServletResponse httpResponse, String response) throws IOException
	{
		httpResponse.setContentType("text/html");
        try(PrintWriter out = httpResponse.getWriter())
        {   
            out.println(this.START_RESPONSE+response+this.END_RESPONSE);
        }
    }
} 
 
