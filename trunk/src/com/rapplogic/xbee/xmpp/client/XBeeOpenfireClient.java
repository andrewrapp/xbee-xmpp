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

import org.apache.log4j.Logger;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

import com.rapplogic.xbee.xmpp.XBeeOpenfireCommon;

/**
 * Client implementation for Openfire
 * <p/>
 * Only one client instance per XMPP user account is supported.  If you connect using
 * a user account that is already in use, the other instance will get a unavailable gateway presence.
 * 
 * @author andrew
 *
 */
public class XBeeOpenfireClient extends XBeeXmppClient {

	private final static Logger log = Logger.getLogger(XBeeOpenfireClient.class);

	public XBeeOpenfireClient(String server, int port, String user, String password, String xbeeUser) throws XMPPException {
		super(server, port, user, password, xbeeUser);
	}
	
	protected XMPPConnection connect() throws XMPPException {
		return XBeeOpenfireCommon.connect(this.getServer(), this.getPort(), this.getUser(), this.getPassword());
	}
	
	protected boolean isAvailable(Presence presence) {
		return XBeeOpenfireCommon.isAvailable(presence);
	}
}
