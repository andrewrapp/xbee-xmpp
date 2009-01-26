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

import java.util.Random;

public class XBeeXmppUtil {
	/**
	 * Formats a byte array in hex, without the "0x" notation.
	 * 
	 * @param arr
	 * @return
	 */
    public static String formatByteArrayAsHexString(int[] arr) {
        
//    	log.debug("packet is " + ByteUtils.toBase16(arr));
    	
    	StringBuffer strbuf = new StringBuffer();
    	
    	for (int i = 0; i < arr.length; i++) {
    		if (arr[i] > 255) {
    			throw new IllegalArgumentException("value in array exceeds one byte: " + arr[i]);
    		}
    		
    		strbuf.append(padHex(arr[i]));
    	}
    
    	return strbuf.toString();
    }
    
    /**
     * The hex string must 2 chars per byte for it to be parsed correctly, 
     * so 0x1 is represented as 01
     * 
     * @param b
     * @return
     */
    public static String padHex(int b) {
		if (b < 0x10) {
			return "0" + Integer.toHexString(b);
		} else {
			return Integer.toHexString(b);
		}
    } 
    
    private static Random random = new Random();
    
    /**
     * Returns a random frame id between 1 and 255
     * 
     * @return
     * Jan 24, 2009
     */
    public static int getRandomFrameId() {
    	return random.nextInt(0xff) + 1;
    }
    
    /**
     * Returns a random frame id that is not equal to one provided
     * 
     * @param last
     * @return
     * Jan 24, 2009
     */
    public static int getDifferentFrameId(int last) {
    	int frameId = 0;
    	
    	do {
    		frameId = getRandomFrameId();
    	} while (last == frameId);
    	
    	return frameId; 
    }
    
    /**
     * Removes the provider from a xmmp address.  For example, removes "/Smack" from xbeegateway@sencha.local/Smack
     * 
     * @param user
     * @return
     * Jan 24, 2009
     */
    public static String stripProviderFromXmppUser(String user) {
    	if (user.indexOf("/") > 0) {
    		return user.substring(0, user.lastIndexOf("/"));
    	}
    	
    	return user;
    }
}
