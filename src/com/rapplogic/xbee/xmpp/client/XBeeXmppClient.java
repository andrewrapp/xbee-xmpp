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

import org.apache.log4j.Logger;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

import com.rapplogic.xbee.api.PacketListener;
import com.rapplogic.xbee.api.PacketStream;
import com.rapplogic.xbee.api.XBeeRequest;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.util.IntArrayInputStream;
import com.rapplogic.xbee.xmpp.XBeeXmppPacket;

/**
 * Provides communication with a XBee radio over XMPP
 * A instance of XBeeXmppGateway must be running for this class to function.
 * 
 * Connects to gateway and subscribes to the gateway, if not already in roster
 * Gateway does not need to be online at startup but of course send/receive will not be possible until gateway is online.
 * Once the gateway is online, client will resume sending/receiving data, if in forever loop
 * 
 * Important: You must make sure this client is in the the gateway's client list or it will not be able to communicate
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
	
	public void start() throws XMPPException {
		synchronized (this) {

			// default gateway to offline incase they are not in roster
			this.getPresenceMap().put(this.getGateway(), Boolean.FALSE);
			
			this.initXmpp();
		}
	}

    public void processMessage(Chat chat, Message message) {
		
    	try {
    		log.debug("Message is " + message.toXML());
    
	    	int[] packet = this.decodeMessage(message);
	    	
	    	// works awesomely!
	    	XBeeResponse response = hydrate(packet);
	    	
	    	log.debug("hydrated response: " + response);

	    	// distribute the response to listeners
	    	for (PacketListener listener : this.getPacketListeners()) {
	    		listener.processResponse(response);
	    	}    		
    	} catch (Exception e) {
    		// TODO add to error listener
    		log.error("error processing message " + message.toXML(), e);
    	}	
    }
	
    protected List<String> getRosterList() {
    	List<String> gateway = new ArrayList<String>();
    	gateway.add(this.getGateway());
    	return gateway;
    }
    
	public static XBeeResponse hydrate(int[] packet) {
    	IntArrayInputStream in = new IntArrayInputStream(packet);
    	
    	// reconstitute XBeeResponse from int array
    	PacketStream ps = new PacketStream(in);
    	// this method will not throw an exception
    	XBeeResponse response = ps.parsePacket();
    	
    	return response;
	}
	
	public void sendXBeeRequest(XBeeRequest request) throws XMPPException, GatewayOfflineException {
		Message message = this.encodeMessage(request);
		// TODO error handling -- for now we just assume it was received
		
		if (!this.isGatewayOnline()) {
			throw new GatewayOfflineException();
		} 
		
		this.getChat().sendMessage(message);	
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
