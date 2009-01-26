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

package com.rapplogic.xbee.xmpp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;

import com.rapplogic.xbee.api.PacketListener;


/**
 * Includes common functionality for gateways and clients.
 *
 * Automatically subscribes to all users in getRosterList on startup, if not already subscribed.
 * 
 * The goal is to eventually integrate this project with XBee-API so that you that you can open a connection
 * to a XBee via the serial port, or over the network via XMPP, both using the same API.
 * 
 * TODO add error listener
 * TODO receive connection failure events
 * TODO add Runtime shutdown hook
 * TODO need a mechanism to transmit errors that occur between xmpp client and server, for example gateway loses connection to xbee. as of now the client does not get an error and assumes it was sent
 * TODO determine max size of message/packet
 */
public abstract class XBeeXmpp implements MessageListener, RosterListener {
	
	private final static Logger log = Logger.getLogger(XBeeXmpp.class);
	
	private final List<PacketListener> listeners = new ArrayList<PacketListener>();
	
	//private final HashMap<String,Boolean> presenceMap = new HashMap<String,Boolean>();
	private final Hashtable<String,Boolean> presenceMap = new Hashtable<String,Boolean>();
	
	private final HashMap<String,Chat> chatMap = new HashMap<String,Chat>();
	
	private XMPPConnection connection;

	private String user;
	private String password;
	private String server;
	private Integer port;

	public XBeeXmpp(String server, Integer port, String user, String password) {
		if ((server != null && port == null) || (port != null && server == null)) {
			throw new IllegalArgumentException("either both server and port must be specified or neither");
		}
		
		this.setServer(server);
		this.setPort(port);
		this.setUser(user);
		this.setPassword(password);	
	}
	
	public void addPacketListener(PacketListener listener) {
		listeners.add(listener);
	}
	
	protected List<PacketListener> getPacketListeners() {
		return listeners;
	}

//	public HashMap<String, Boolean> getPresenceMap() {
//		return presenceMap;
//	}

	public Hashtable<String, Boolean> getPresenceMap() {
		return presenceMap;
	}
	
	public XMPPConnection getConnection() {
		return connection;
	}
	
	public void setConnection(XMPPConnection connection) {
		this.connection = connection;
	}
	
	public String getUser() {
		return user;
	}
	
	public void setUser(String user) {
		this.user = user;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public HashMap<String, Chat> getChatMap() {
		return chatMap;
	}
	
	protected abstract void connect() throws XMPPException;

    protected abstract List<String> getRosterList();
    
    protected abstract boolean isAvailable(Presence presence);
	
	/**
	 * Called when a roster subscription is sent/received
	 * This fires on the instance where roster.addEntry is called
	 * Default Smack policy is to accept all roster subscriptions.
	 */
	public void entriesAdded(Collection<String> addresses) {
		for (String address : addresses) {
			log.info("entry added: " + address);
		}
	}
	
	public void entriesDeleted(Collection<String> addresses) {
		for (String address : addresses) {
			log.info("entriesDeleted: [" + address + "]");
		}
	}
	
	/**
	 * Called when subscription is approved? and addEntry request
	 */
    public void entriesUpdated(Collection<String> addresses) {
    	for (String address : addresses) {
			log.info("entriesUpdated: [" + address + "]");
		}
    }
        
	public final void presenceChanged(Presence presence) {
    	
		log.debug("Presence changed: from: " + presence.getFrom() + ", mode is " + presence.getMode() + ", tostring is [" + presence.toString() + "]");
    	
    	// not equals because that would be something like username@gmail.com/Talk.v10482E0B62B for gtalk
		// and username@host/Smack for openfire
    	
		// TODO smack questions: how do we get presence mode?  how do we getFrom without additional garbage?
		
    	for (String user: this.getRosterList()) {
        	if (presence.getFrom().startsWith(user)) {
			
        		// unfortunately smack presence.getMode returns null, and toString is inconsistent between openfire/gtalk so we defer to subclass
        		// to determine if user is online
        		
				if (this.isAvailable(presence)) {
					log.debug(user + " is online");
					this.getPresenceMap().put(user, Boolean.TRUE);
				} else {
					// could also be busy
					this.getPresenceMap().put(user, Boolean.FALSE);
					log.debug(user + " is offline");
				}
        	}    		
    	}
    }
	
	/**
	 * This appears to be a bug in Openfire and/or Smack where presence "available" is not sent during sign-on
	 * for senders/recipients that are in fact "available".  This is a patch/workaround to the issue.
	 * 
	 * @param sender
	 * Jan 24, 2009
	 */
	public void verifyPresence(String sender) {
       
    	if (this.getRosterList().contains(sender)) {
    		// BUG at times the gateway does not receive a presence event during sign-on for online users.
        	if (this.getPresenceMap().get(sender) == null) {
        		// user is obviously online and sending us messages
        		log.warn("Did not receive available presence event for " + sender + ", but they are online!");
        		this.getPresenceMap().put(sender, Boolean.TRUE);
        	}    		
    	}
	}

	/**
	 * Determines if the sender is valid.  
	 * In general only XMPP users in your roster can send you messages, so this may be redundant.
	 * This checks to make sure the user is in your roster list.
	 * 
	 * @param sender
	 * @param body
	 * @return
	 * Jan 24, 2009
	 */
	public boolean isValidSender(String sender, String body) {
	       
    	if (!this.getRosterList().contains(sender)) {
    		log.warn("ignoring message from [" + sender + "], who is not an approved sender.  message is " + body);
    		return false;
    	}
    	
    	return true;
	}
	
	/**
	 * Connects to XMPP server and subscribes to XMPP users, if necessary
	 * 
	 * @throws XMPPException
	 * Jan 24, 2009
	 */
    protected void initXmpp() throws XMPPException {
    	synchronized (this) {
    		this.connect();

    		Roster roster = this.getConnection().getRoster();
    		// this is necessary to know who is online/offline
    		roster.addRosterListener(this);	
    		
    		for (RosterEntry entry : roster.getEntries()) {
    			log.info("Roster user is " + entry.getUser() + ", display name is " + entry.getName() + ", toString is " + entry + ", status is " + entry.getStatus() + ", item type is " + entry.getType());
    		}
    		
    		// if user is offline, the server will automatically forward the subscription request once the user is online
    		for (String user : this.getRosterList()) {
    			if (!roster.contains(user)) {
    				log.info("Recipient [" + user + "] not in roster.  subscribing now!");
    				roster.createEntry(user, user, null);		
    			} else {
    				// "none" seems to be a pending subscription, where user is offline and hasn't accepted								
    				if (roster.getEntry(user).getType() == RosterPacket.ItemType.none) {
    					log.info("[" + user + "] is in roster but has a pending subscription.  the user may be offline, but will accept once they come back online");
    				} else if (roster.getEntry(user).getType() == RosterPacket.ItemType.from) {
    					log.info("[" + user + "] has subscribed to us, but we have not subscribed to the user.  this may be the first time we are connecting to the user.  subscribing now!");
    					roster.createEntry(user, user, null);									
    				} else if (roster.getEntry(user).getType() == RosterPacket.ItemType.to) {
    					log.warn("We have subscribed to [" + user + "], but they have not subscribed to us yet.  Make sure this user is in the gateway's client list.  Messages from this user will be discarded until they subscribe.");
    				} else if (roster.getEntry(user).getType() == RosterPacket.ItemType.both) {
    					log.info("[" + user + "] is fully subscribed");
    				} else {
    					log.warn("Unknown roster subscription type: " + roster.getEntry(user).getType());
    				}
    			}
    		}
    		
//    		for (RosterEntry entry : roster.getEntries()) {
//    			log.info("[after] Roster user is " + entry.getUser() + ", display name is " + entry.getName() + ", toString is " + entry + ", status is " + entry.getStatus() + ", item type is " + entry.getType());
//    		}
    		
			// create chat object for each recipient
			for (String user: this.getRosterList()) {
				log.info("Creating Chat object for " + user);
				Chat chat = this.getConnection().getChatManager().createChat(user, this);
				chatMap.put(user, chat);
			}
    	}    	
    }
    
	public void shutdown() {
		
		if (this.getConnection() != null) {
			try {
				this.getConnection().disconnect();
			} catch (Exception e) {
				log.error("failed to disconnect xmpp connection", e);
			}
		} 
	}
	
}
