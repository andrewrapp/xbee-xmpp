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

import org.apache.log4j.Logger;

/**
 * Implementation of XBeeGateway for Openfire
 * 
 * @author andrew
 *
 */
public class XBeeOpenfireGateway extends XBeeXmppGateway {

	private final static Logger log = Logger.getLogger(XBeeOpenfireGateway.class);
	
	// syntax of chat user is username@[openfire servername] (e.g. username@foo.local).  
	// The servername is generally not necessarily the same
	// as the server host name.  You can find servername in the server properties, in the openfire admin console.

	public XBeeOpenfireGateway() {
		
	}
}
