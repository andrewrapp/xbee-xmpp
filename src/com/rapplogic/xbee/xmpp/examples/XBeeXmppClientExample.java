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

package com.rapplogic.xbee.xmpp.examples;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jivesoftware.smack.XMPPException;

import com.rapplogic.xbee.api.AtCommand;
import com.rapplogic.xbee.api.AtCommandResponse;
import com.rapplogic.xbee.api.PacketListener;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.util.ByteUtils;
import com.rapplogic.xbee.xmpp.XBeeXmppUtil;
import com.rapplogic.xbee.xmpp.client.GatewayOfflineException;
import com.rapplogic.xbee.xmpp.client.XBeeOpenfireClient;
import com.rapplogic.xbee.xmpp.client.XBeeXmppClient;

public class XBeeXmppClientExample implements PacketListener {
	
	private final static Logger log = Logger.getLogger(XBeeXmppClientExample.class);
	
	private XBeeResponse response;
	private int frameId;
	
	public XBeeXmppClientExample() throws XMPPException, InterruptedException {
	
		XBeeXmppClient client = new XBeeOpenfireClient("localhost", 5222, "xbeeclient", "xbeeclient", "xbeegateway@sencha.local");
		//XBeeXmppClient client = new XBeeOpenfireClient("localhost", 5222, "xbeeclient2", "xbeeclient2", "xbeegateway@sencha.local");
		//XBeeXmppClient client = new XBeeOpenfireClient("localhost", 5222, "xbeeclient3", "xbeeclient3", "xbeegateway@sencha.local");
		
		// or use Gtalk (note: if you are using a google apps account for you domain, the username is username@yourdomain.com)
		//XBeeXmppClient client = new XBeeGtalkClient("xbeeclient@gmail.com", "password", "xbeegateway@gmail.com");
		
		client.addPacketListener(this);
		client.start();
		
		while (true) {
			synchronized (this) {
				// get association status
				AtCommand at = new AtCommand("AI");
				this.frameId = XBeeXmppUtil.getDifferentFrameId(frameId);
				at.setFrameId(frameId);
				
				try {
					client.sendXBeeRequest(at);
					log.debug("sent message " + ByteUtils.toBase16(at.getXBeePacket().getPacket()));
					
					log.debug("waiting");
					this.wait(5000);
					
					if (response == null) {
						log.warn("command timed out");
					}					
				} catch (GatewayOfflineException goe) {
					log.debug("gateway is not online.. waiting");
				}
			}
						
			//Thread.sleep(10*60000);
			Thread.sleep(10000);
		}
	}
	
	public void processResponse(XBeeResponse response) {
		synchronized(this) {
			if (response instanceof AtCommandResponse && ((AtCommandResponse)response).getFrameId() == this.frameId) {
				this.response = response;
				log.debug("received response " + response);
				this.notify();				
			} else {
				log.debug("received response but not the one we were expecting");
			}
		}
	}
	
	public static void main(String[] args) throws XMPPException, InterruptedException {
		PropertyConfigurator.configure("log4j.properties");
		new XBeeXmppClientExample();
	}
}
