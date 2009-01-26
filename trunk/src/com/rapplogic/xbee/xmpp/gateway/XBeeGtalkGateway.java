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

package com.rapplogic.xbee.xmpp.gateway;

import java.util.List;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.xmpp.XBeeGtalkCommon;

/**
 * XBee Gateway implementation for Google talk.
 * As long as you have access to the google gtalk server, your XMPP gateway and client can be
 * on different networks, even behind firewalls.
 * 
 * This solution allows you to share your xbee hardware with anyone on the internet to experiment with as well
 * as create xbee applications that reside on different physical machines and/or networks than the xbee.  
 * This is also a great way to distribute your xbee applications across the internet without requiring a server 
 * or static ip address -- google is the server.  Futhermore, your gateway can be brought down and upgraded without
 * affecting the clients.  When the gateway is brought up, clients will automatically get notified and resume communication
 * 
 * You will need a minimum of two gtalk accounts: one for the gateway and one for the client.
 * 
 * The client can go on and offline at will
 * 
 * Google talk accepts messages sent to offline user, delivering the messages will be delivery when the user signs on.
 * However the gateway will only send a message if the user's presence is available.
 * 
 * Caveat: I'm not sure how Google feels about using Gtalk for application communication.  It's possible they could rate limit
 * or suspend your account altogether; though this is more likely to occur if you send a lot of traffic.
 * 
 * @author andrew
 * 
 */
public class XBeeGtalkGateway extends XBeeXmppGateway {

	private final static Logger log = Logger.getLogger(XBeeGtalkGateway.class);
	
	/**
	 * Verify that all recipients are in your Gtalk address book and you can send to them.  You may need to invite them first
	 * 
	 * @param user
	 * @param password
	 * @param clientList
	 * @param comPort
	 * @param baudRate
	 * @throws XMPPException
	 * @throws XBeeException
	 */
	public XBeeGtalkGateway(String server, Integer port, String user, String password, List<String> clientList, String comPort, int baudRate) throws XMPPException, XBeeException {
		super(server, port, user, password, clientList, comPort, baudRate);
	}

	public XBeeGtalkGateway(String user, String password, List<String> clientList, String comPort, int baudRate) throws XMPPException, XBeeException {
		super(null, null, user, password, clientList, comPort, baudRate);
	}
	
	protected void connect() throws XMPPException {
		this.setConnection(XBeeGtalkCommon.connect(this.getServer(), this.getPort(), this.getUser(), this.getPassword()));
	}
	
	protected boolean isAvailable(Presence presence) {
		return XBeeGtalkCommon.isAvailable(presence);
	}
}
