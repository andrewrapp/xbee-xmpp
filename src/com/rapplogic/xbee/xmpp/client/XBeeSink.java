package com.rapplogic.xbee.xmpp.client;

import org.jivesoftware.smack.XMPPException;

import com.rapplogic.xbee.api.XBeeException;

public interface XBeeSink {
	void send(int[] packet) throws XBeeException, XMPPException;
}
