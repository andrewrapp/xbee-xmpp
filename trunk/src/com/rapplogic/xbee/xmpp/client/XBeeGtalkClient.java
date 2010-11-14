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

import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeConfiguration;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.xmpp.XBeeGtalkCommon;

/**
 * Client implementation for Google Talk
 * 
 * @author andrew
 *
 */
public class XBeeGtalkClient extends XBeeXmppClient {

	private final static Logger log = Logger.getLogger(XBeeGtalkClient.class);
		
	private XBeeXmppClient xmpp;
	
	public XBeeGtalkClient() {
		super();
	}
	
	public XBeeGtalkClient(XBeeConfiguration conf) {
		super(conf);
	}

	@Override
	protected XMPPConnection connect() throws XMPPException {
		return XBeeGtalkCommon.connect(this.getServer(), this.getPort(), this.getUser(), this.getPassword());
	}

	@Override
	protected boolean isAvailable(Presence presence) {
		return XBeeGtalkCommon.isAvailable(presence);
	}
}
