/**
 * Copyright (c) 2009 Andrew Rapp. All rights reserved.
 *  
 * This file is part of XBee-XMPP
 *  
 * XBee-XMPP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * XBee-XMPP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License
 * along with XBee-XMPP.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.rapplogic.xbee.xmpp.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

import com.rapplogic.xbee.api.PacketListener;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.XBeeFrameIdResponse;
import com.rapplogic.xbee.api.XBeeRequest;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.api.XBeeTimeoutException;
import com.rapplogic.xbee.xmpp.XBeeXmppPacket;
import com.rapplogic.xbee.xmpp.XBeeXmppUtil;

/**
 * Provides communication with an XBee radio over XMPP.
 * A instance of XBeeXmppGateway must be running for this class to function.
 * <p/>
 * Connects to gateway and subscribes to the gateway, if not already in roster
 * Gateway does not need to be online at startup but of course send/receive will not be possible until gateway is online.
 * <p/>
 * Important: You must make sure the client's JID is in the the gateway's client list or it will not be able to communicate
 * with the gateway.  XMPP does not allow communication with another user (such as the gateway) until you have subscribed to/invited them 
 * AND they have subscribed to/accepted you.  If only the client has subscribed, it will receive presence events and can send messages
 * but these messages will not be received and no error will be generated.
 * 
 * @author andrew
 *
 */
public abstract class XBeeXmppClient extends XBeeXmppPacket {

	private final static Logger log = Logger.getLogger(XBeeXmppClient.class);

	private String gateway;
	
	private final static int queueSize = 100;
	
	// we could set the capacity to queueSize but without synchronization on processMessage, the queue could exceed this size for brief moments
	private BlockingQueue<XBeeResponse> responseQueue = new LinkedBlockingQueue<XBeeResponse>();
	
	public XBeeXmppClient(String server, Integer port, String user, String password, String gateway) {
		super(server, port, user, password);
		this.setGateway(gateway);	
	}
	
	public Boolean isGatewayOnline() {
		return (Boolean) this.getPresenceMap().get(this.getGateway());
	}

	public String getGateway() {
		return gateway;
	}

	public void setGateway(String gateway) {
		this.gateway = gateway;
	}
	
	/**
	 * Establishes a connection to the XMPP Server
	 * 
	 * @throws XMPPException
	 */
	public void start() throws XMPPException {
		synchronized (this) {

			// default gateway to offline incase they are not in roster
			this.getPresenceMap().put(this.getGateway(), Boolean.FALSE);
			
			this.initXmpp();
		}
	}
	
    public void processMessage(Chat chat, Message message) {
		
    	try {
    		if (log.isDebugEnabled()) {
    			log.debug("Received message from gateway: " + message.toXML());	
    		}
    		
	    	int[] packet = this.decodeMessage(message);
	    	
	    	// works awesomely!
	    	XBeeResponse response = XBeeXmppUtil.decodeXBeeResponse(packet);
	    	
	    	if (log.isInfoEnabled()) {
	    		log.info("Hydrated response from gateway: " + response);	
	    	}
	    	
	    	// add to blocking queue
	    	while (responseQueue.size() >= queueSize) {
		    	if (log.isInfoEnabled()) {
		    		log.info("Queue size has reached or exceeded maximum of " + queueSize + ", trimming head by one");	
		    	}
		    	
	    		// discard one
	    		responseQueue.poll();	    		
	    	}
	    	
    		if (!responseQueue.offer(response)) {
    			log.error("Failed to offer response to queue.  Size is " + responseQueue.size());
    		} else {
        		if (log.isDebugEnabled()) {
        			log.debug("Added response to response queue");	
        		}
    		}
	    	
	    	// distribute the response to listeners
	    	for (PacketListener listener : this.getPacketListeners()) {
	    		listener.processResponse(response);
	    	}    		
    	} catch (Exception e) {
    		// TODO add to error listener
    		log.error("error processing message " + message.toXML(), e);
    	}	
    }
	
    /**
     * Retrieves a response from the response queue, waiting if necessary.
     * The response queue supports a maximum of 100 packets.  When full
     * it removes packets from the head of the queue to make space.
     *  
     * @return
     * @throws XBeeException
     */
	public XBeeResponse getResponse() throws XBeeException {
		try {
			return responseQueue.take();
		} catch (InterruptedException e) {
			throw new XBeeException(e);
		}
	}

	/**
	 * Retrieves a response from the response queue, waiting up to timeout milliseconds, if necessary.
	 * A XBeeTimeoutException exception is thrown if no response is received within timeout milliseconds.
	 * 
	 * @param timeout
	 * @return
	 * @throws XBeeException
	 * @throws XBeeTimeoutException
	 */
	public XBeeResponse getResponse(int timeout) throws XBeeException, XBeeTimeoutException {
		try {
			if (timeout < 0) {
				throw new IllegalArgumentException("timeout must be positive or zero");
			}
			
			if (timeout > 0) {
				XBeeResponse response = responseQueue.poll(timeout, TimeUnit.MILLISECONDS);
				
				if (response == null) {
					throw new XBeeTimeoutException();
				} else {
					return response;
				}
			} else {
				return responseQueue.take();	
			}
			
		} catch (InterruptedException e) {
			throw new XBeeException(e);
		}
	}
	
    protected List<String> getRosterList() {
    	List<String> gateway = new ArrayList<String>();
    	gateway.add(this.getGateway());
    	return gateway;
    }
	
    /**
     * Sends a request to the gateway and returns, not waiting for a response.
     * Throws GatewayOfflineException if gateway is offline.
     * 
     * @param request
     * @throws XMPPException
     * @throws XBeeException
     */
	public void sendAsynchronous(XBeeRequest request) throws XMPPException, XBeeException {
		Message message = this.encodeMessage(request);
		// TODO error handling -- for now we just assume it was received
		
		if (!this.isGatewayOnline() && !this.isOfflineMessages()) {
			throw new GatewayOfflineException();
		} 
		
		if (log.isInfoEnabled()) {
			log.info("Sending request to gateway: " + request);
		}
		
		this.getChat().sendMessage(message);			
	}
	
	/**
	 * Clears the response queue and sends the request to the radio.
	 * Waits up to timeout milliseconds for a response that matches the frame id of the request.
	 * If no matching response is found, a XBeeTimeoutException is thrown.
	 * The request must have a non-zero frame id to guarantee a response, or an exception will be thrown.
	 *  
	 * @param request
	 * @param timeout
	 * @return
	 * @throws XBeeTimeoutException
	 * @throws XBeeException
	 * @throws XMPPException
	 */
	public XBeeResponse sendSynchronous(final XBeeRequest request, int timeout) throws XBeeTimeoutException, XBeeException, XMPPException {

		final long start = System.currentTimeMillis();
		int varTimeout = timeout; 
			
		if (request.getFrameId() == XBeeRequest.NO_RESPONSE_FRAME_ID) {
			throw new XBeeException("Frame Id cannot be 0 for a synchronous call -- it will always timeout as there is no response!");
		}
		
		// clear queue so we don't waste time examining stale responses
		log.debug("synchronousSend(): clearing queue");
		this.responseQueue.clear();
		
		this.sendAsynchronous(request);
		
		XBeeResponse response = null;
		
		while (varTimeout > 0) {
			response = this.getResponse(varTimeout);

			if (response instanceof XBeeFrameIdResponse && ((XBeeFrameIdResponse)response).getFrameId() == request.getFrameId()) {
				// got it
				return response;
			} else {
				log.debug("synchronousSend(): Got response [" + response + "] but frame id does not match.  Adjusting timeout and trying again");
				// TODO we are trashing this response.  user's will need to use packetlistner to capture traffic that may be discarded in synchronousSend
				response = null;
				
				varTimeout = timeout - (int)(System.currentTimeMillis() - start);
			}		
		}
		
		// no matching responses
		throw new XBeeTimeoutException();
	}
	
	public BlockingQueue<XBeeResponse> getResponseQueue() {
		return responseQueue;
	}
	
	/**
	 * @deprecated use sendAsynchronous
	 * @param request
	 * @throws XMPPException
	 * @throws GatewayOfflineException
	 */
	public void sendXBeeRequest(XBeeRequest request) throws XMPPException, XBeeException {
		this.sendAsynchronous(request);
	}
	
	public void shutdown() {
		try {
			if (this.getConnection() != null) {
				this.getConnection().disconnect();			
			}
		} catch (Exception e) {
			log.error("failed to disconnect connection", e);
		}
	}
	
	/**
	 * Returns the gateway chat object -- the only chat object for a client!
	 * 
	 * @return
	 */
	public Chat getChat() {
		return this.getChatMap().get(this.getGateway());
	}
}
