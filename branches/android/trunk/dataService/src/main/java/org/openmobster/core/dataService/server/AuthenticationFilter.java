/**
 * Copyright (c) {2003,2009} {openmobster@gmail.com} {individual contributors as indicated by the @authors tag}.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.openmobster.core.dataService.server;

import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoSession;

import java.security.MessageDigest;

import org.openmobster.core.dataService.Constants;
import org.openmobster.core.dataService.model.AuthCredential;

import org.openmobster.core.security.device.Device;
import org.openmobster.core.security.device.DeviceController;

import org.openmobster.core.services.CometService;
import org.openmobster.core.services.subscription.SubscriptionManager;
import org.openmobster.core.services.subscription.Subscription;

/**
 * @author openmobster@gmail.com
 */
public class AuthenticationFilter extends IoFilterAdapter
{
	private DeviceController deviceController;
	
	public AuthenticationFilter()
	{
		
	}
		
	public DeviceController getDeviceController() 
	{
		return deviceController;
	}

	public void setDeviceController(DeviceController deviceController) 
	{
		this.deviceController = deviceController;
	}
	//------------------------------------------------------------------------------------------------------------------
	public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception
	{	
		if(!this.skipAuthentication(session))
		{
			if(session.getAttribute(Constants.subscription) == null &&
			   session.getAttribute(Constants.consoleSession) == null
			)
			{
				//This session is unauthenticated....the payload must have authentication info
				boolean auth = handleAuth(session);
				if(!auth)
				{
					session.write(Constants.status+"="+401+Constants.endOfStream);			
					return;
				}
			}
		}		
		
		//connecting device was successfully authenticated...proceed
		nextFilter.messageReceived(session, message);
	}	
	//-------------------------------------------------------------------------------------------------------------------
	private boolean handleAuth(IoSession session) throws Exception
	{
		String payload = (String)session.getAttribute(Constants.payload);
		AuthCredential authCredential = this.parseAuthCredential(payload);
		if(authCredential == null)
		{
			return false;
		}
		
		String deviceId = authCredential.getDeviceId();
		
		//check to make sure this isn't the console...console has its own authentication mechanism
		if(deviceId.startsWith("console:"))
		{
			session.setAttribute(Constants.consoleSession, Boolean.TRUE);
			return true;
		}
		
		
		String nonce = authCredential.getNonce();
		Device device = deviceController.read(deviceId);
		if(device != null && device.getIdentity().isActive())
		{
			String storedNonce = device.readAttribute("nonce").getValue();			
			if(nonce != null && MessageDigest.isEqual(nonce.getBytes(), storedNonce.getBytes()))
			{
				/*MessageDigest digest = MessageDigest.getInstance("SHA-512");
				String knownInput = device.getIdentifier() + device.getIdentity().getPrincipal() + 
				device.getIdentity().getCredential();
				String randomSalt = org.openmobster.core.common.Utilities.generateUID();
				byte[] newNonceBytes = digest.digest((knownInput+randomSalt).getBytes());
				String newNonce = org.openmobster.core.common.Utilities.encodeBinaryData(newNonceBytes);
				device.updateAttribute(new DeviceAttribute("nonce", newNonce));
				deviceController.update(device);
				session.setAttribute(Constants.nextNonce, newNonce);*/
				
				//Start Subscription
				Subscription subscription = new Subscription();
				subscription.setClientId(deviceId);
				SubscriptionManager manager = CometService.getInstance().activateSubscription(subscription);
				session.setAttribute(Constants.subscription, manager);
				return true;
			}
		}
		
		return false;
	}
	
	private AuthCredential parseAuthCredential(String payload) throws Exception
	{
		int startOfTokenIndex = payload.indexOf("<auth>");
		int endOfTokenIndex = payload.indexOf("</auth>", startOfTokenIndex);
		
		if(startOfTokenIndex != -1 && endOfTokenIndex > startOfTokenIndex)
		{			
			String authdata = payload.substring(startOfTokenIndex+"<auth>".length(), endOfTokenIndex);			
			if(authdata != null)
			{
				authdata = authdata.trim();
				int separatorIndex = authdata.indexOf('|');
				if(separatorIndex != -1)
				{
					String deviceId = authdata.substring(0, separatorIndex).trim();
					String nonce = authdata.substring(separatorIndex+1);
					AuthCredential authCredential = new AuthCredential();
					authCredential.setDeviceId(deviceId);
					authCredential.setNonce(nonce);
					return authCredential;
				}
			}
		}
		
		return null;
	}
	
	private boolean skipAuthentication(IoSession session)
	{		
		String payload = (String)session.getAttribute(Constants.payload);
		
		if(payload.startsWith("<auth>"))
		{
			return false;
		}
		
		//If just a processor is being picked for execution, go ahead
		if(payload.contains(Constants.processorId))
		{
			//Allows testsuite requests
			if(payload.contains("=testsuite") || payload.contains("=/testdrive/"))
			{
				session.setAttribute(Constants.anonymousMode, Boolean.TRUE);
			}
			
			return true;
		}
		if(session.getAttribute(Constants.anonymousMode) != null)
		{
			return true;
		}
		
		//Skip based on service in question
		//Provisioning requests should be skipped...Obviously otherwise
		//a device can never get activated (chicken/egg issue)
		//there is no security issue with making this service unprotected
		//In fact eventually there will be a list of protected and unprotected services
		//supported
		String authExceptionMatch1 = 
		"<entry><string>servicename</string><string>provisioning</string></entry>";	
		
		if(
		   payload.contains(authExceptionMatch1) 
		)
		{
			session.setAttribute(Constants.anonymousMode, Boolean.TRUE);
			return true;
		}
						
		return false;
	}
}