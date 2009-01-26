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

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.XBeePacket;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.util.ByteUtils;
import com.rapplogic.xbee.xmpp.XBeeXmppPacket;
import com.rapplogic.xbee.xmpp.XBeeXmppUtil;

/**
 * Interfaces a serial connected XBee network to XMPP.
 * All XBee response objects received from the serial line are forwarded to XMPP for awaiting clients  
 * Receives XBee request objects via XMPP and forwards to the XBee network
 * 
 * TODO add JMX
 * TODO errorhandling
 * TODO request listener in case you want to log incoming requests
 * TODO allow option for offline messages.  default for gtalk when user is offline.  openfire requires configuration
 * 
 * @author andrew
 *
 */
public abstract class XBeeXmppGateway extends XBeeXmppPacket {
	private final static Logger log = Logger.getLogger(XBeeXmppGateway.class);
	
	private XBee xbee;
	private List<String> clientList;
	private String comPort;
	private int baudRate;
	
	public String getComPort() {
		return comPort;
	}

	public void setComPort(String comPort) {
		this.comPort = comPort;
	}

	public int getBaudRate() {
		return baudRate;
	}

	public void setBaudRate(int baudRate) {
		this.baudRate = baudRate;
	}

	/**
	 * 
	 * @param server
	 * @param port
	 * @param user
	 * @param password
	 * @param clientList Only xmpp users in this list will receive xbee responses.  Recipients do not need to be online at gateway startup
	 * @param comPort
	 * @param baudRate
	 * @throws XMPPException
	 * @throws XBeeException
	 */
	public XBeeXmppGateway(String server, Integer port, String user, String password, List<String> clientList, String comPort, int baudRate) throws XMPPException, XBeeException {
		super(server, port, user, password);
		this.setClientList(clientList);
		this.setComPort(comPort);
		this.setBaudRate(baudRate);
	}
	
	public void start() {
		try {			
			xbee = new XBee();
			log.info("opening xbee serial port connection to " + comPort);
			xbee.open(this.getComPort(), this.getBaudRate());
	
			this.initXmpp();
			
			while (true) {
				XBeeResponse response = xbee.getResponse();
				log.debug("received response from xbee " + response);
				
				// TODO check for error.  if error occurred in PacketParser class, this will not be preserved in re-hydration unless exact byte array is preserved
				
				if (response.isError()) {
					log.error("response is error: " + response.toString());
				}
				
				Message msg = this.encodeMessage(response);
				
				log.debug("forwarding response to xmpp clients");
				
				// send to all online clients
				for (String client :this.getChatMap().keySet()) {
					Boolean presence = this.getPresenceMap().get(client);
					
					if (presence != null && presence == Boolean.TRUE) {
						log.debug("sending packet to " + client + ", message: " + msg.getBody());
						this.getChatMap().get(client).sendMessage(msg.getBody());
					} else {
						// such and such is offline
						log.debug(client + " is offline and will not receive the response");
					}
				}
			}
		} catch (Exception e) {
			log.error("error: ", e);
		} finally {
			log.info("closing XBee and Smack");
			this.shutdown();
		}
	}
	
	public void shutdown() {
		super.shutdown(); 
		
		if (xbee != null) {
			try {
				xbee.close();
			} catch (Exception e) {
				log.error("failed to shutdown xbee", e);
			}
		}
	}
	
    public final void processMessage(Chat chat, Message message) {
    	
    	log.debug("received message from client [" + message.getFrom() + "] message: " + message.toXML());
    	   	
    	// TODO if request has frameid, override with sequential frame id and only delivery response to the sender of the packet.  
    	// if i/o sample then no frameid
    	
    	int[] packet = null;
    	
    	String sender = XBeeXmppUtil.stripProviderFromXmppUser(message.getFrom());
    	
	    try {

	    	// security.. make sure the sender is one of our approved clients
	    	if (!this.isValidSender(sender, message.getBody())) {
	    		return;
	    	}
	    	
	    	this.verifyPresence(sender);
	 
	    	packet = this.decodeMessage(message);
	    	
	    	if (packet == null) {
	    		log.warn("could not parse packet from xmpp message");
	    	} else {
		    	log.debug("received packet from " + message.getFrom() + ", message: " + ByteUtils.toBase16(packet));
		    	
	    		synchronized(XBee.class) {
	    			// send xbee packet to device
	    			
	    			// TODO currently there is no mechanism for obtaining the XBeeRequest object from the packet bytes, 
	    			// so we verify the checksum and call sendPacket
	    			if (XBeePacket.verify(packet)) {
	    				log.debug("forwarding packet to xbee");
	    				try {
							xbee.sendPacket(packet);
						} catch (IOException e) {
							// TODO communicate error back to client
							log.error("error occurred sending packet to XBee: ", e);
						}	
	    			} else {
	    				log.warn("packet failed checksum verification.  discarding " + ByteUtils.toBase16(packet));
	    			}
	    		}	
    		}
	    } catch (Exception e) {
	    	// TODO communicate error back to client
	    	log.error("failed to send packet " + (packet != null ? ByteUtils.toBase16(packet) : "null"), e);
	    }
    }
    
    protected List<String> getRosterList() {
    	return clientList;
    }
	
	public List<String> getClientList() {
		return clientList;
	}

	public void setClientList(List<String> clientList) {
		this.clientList = clientList;
	}
}
