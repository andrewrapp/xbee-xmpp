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

// TODO rename package to xbeexmpp
package com.rapplogic.xbee.xmpp;

import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPException;

import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeConfiguration;
import com.rapplogic.xmppthing.DefaultConnector;
import com.rapplogic.xmppthing.XmppConnector;
import com.rapplogic.xmppthing.XmppThing;

/**
 * Includes common functionality for gateways and clients.
 * <p/>
 * Automatically subscribes to all users in getRosterList on startup, if not already subscribed.
 * <p/>
 * Note: if you send a message to a recipient, whom is not subscribed to you, the message will be echoed back
 * in processMessage(..).  The recipient will get a "so and so wants to add you as a friend yes/no" (if using Google Talk)
 * <p/>
 * Google talk will allow multiple connections with the same login, but messages will not be broadcast to all connections;
 * instead messages will echo back to processMessage
 * 
 */
public abstract class XBeeXmpp extends XBee implements MessageListener {
	
	 //TODO add error listener
	 //TODO receive connection failure events
	 //TODO add Runtime shutdown hook
	 //TODO need a mechanism to transmit errors that occur between xmpp client and server, for example gateway loses connection to xbee. as of now the client does not get an error and assumes it was sent
	 //TODO promiscuous mode where gateway will accept messages from any address -- client list ignored
	 
//	private final static Logger log = Logger.getLogger(XBeeXmpp.class);
	
	private final XmppThing xmppThing = new XmppThing();
	private boolean offlineMessages = false;

	public XBeeXmpp() {
	
	}
	
	public XBeeXmpp(XBeeConfiguration conf) {
		super(conf);
	}
	
	protected XmppConnector getXmppConnector(String server, Integer port, String user, String password) {
		// return default connector
		return new DefaultConnector(server, port, user, password);
	}
	
	/**
	 * Connects to XMPP server and subscribes to XMPP users, if necessary
	 * 
	 * @throws XMPPException
	 */
    public void connectXmpp(String server, Integer port, String user, String password) throws XMPPException {   
		if ((server != null && port == null) || (port != null && server == null)) {
			throw new IllegalArgumentException("either both server and port must be specified or neither");
		}

    	xmppThing.connect(this.getXmppConnector(server, port, user, password), this);
    }
    
	public void shutdown() {
		this.getXmppThing().disconnect(); 
	}

	/**
	 * Returns true if offline messages are enabled
	 * 
	 * @return
	 * Feb 25, 2009
	 */
	public boolean isOfflineMessages() {
		return offlineMessages;
	}

	/**
	 * Set to true to enable offline messages.  The default setting is false
	 * <p/>
	 * If you are using Openfire, you must enable offline messages via the admin console.
	 * Google talk supports offline message by default.
	 * <p/>
	 * If you are enabling this setting from a gateway, it means that packets to clients will be queued
	 * until the client is back online.
	 * <p/>
	 * If you are enabling this setting from a client, it means that packets will be delivered when
	 * the gateway comes back online.
	 * 
	 * @param offlineMessages
	 * Feb 25, 2009
	 */
	public void setOfflineMessages(boolean offlineMessages) {
		this.offlineMessages = offlineMessages;
	}

	public XmppThing getXmppThing() {
		return xmppThing;
	}
}
