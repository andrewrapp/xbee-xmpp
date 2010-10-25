package com.rapplogic.xbee.xmpp.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.apache.log4j.Logger;

import com.rapplogic.xbee.XBeeConnection;
import com.rapplogic.xbee.util.ByteUtils;

public class XmppXBeeConnection implements XBeeConnection {

	private final static Logger log = Logger.getLogger(XBeeConnection.class);
	
	private int[] inputBuffer;
	private int[] outputBuffer = new int[200];
	private int readPosition;
	private int inputPosition;
	private int outPosition;
	private int inBytesAvailable;
	
	private XBeeSink sink;
	
	private PipedOutputStream pos = new PipedOutputStream();
	private PipedInputStream pis = new PipedInputStream();
	
//	private InputStream in = new InputStream() {
//
//		@Override
//		public int read() throws IOException {
//			
//			synchronized (inputBuffer) {
//				if (inBytesAvailable < 1) {
//					// block
//					try {
//						log.debug("Input buffer is empty.. waiting for new packets");
//						inputBuffer.wait();
//					} catch (InterruptedException e) {
//						log.warn("Input buffer wait interrupted");
//					}
//				}
//				
//				int ret = inputBuffer[readPosition];
//				
//				log.debug("read(): byte is " + ByteUtils.toBase16(ret) + ", pos is " + readPosition + ", bytes available is " + inBytesAvailable);
//						
//				// increment read position
//				if (++readPosition == inputBuffer.length) {
//					readPosition = 0;
//				}
//				
//				inBytesAvailable--;
//				
//				return ret;				
//			}
//		}
//	
//		@Override
//		public int available() {
//			synchronized (inputBuffer) {
//				return inBytesAvailable;	
//			}
//		}
//		
//	};
	
	private OutputStream out = new OutputStream() {

		@Override
		public void write(int arg0) throws IOException {
			
			if (outPosition >= outputBuffer.length) {
				throw new IOException("Problem: index at end of output buffer");
			}
			
			outputBuffer[outPosition] = arg0;
			outPosition++;
		}
		
		@Override
		public void flush() throws IOException {
			
			if (outPosition == 0) {
				throw new IOException("Nothing to write!");
			}
			
			// write all avail. bytes to sink
			int[] packet = new int[outPosition];
			System.arraycopy(outputBuffer, 0, packet, 0, outPosition);
			
			try {
				sink.send(packet);	
			} catch (Exception e) {
				throw new IOException("Failed to send packet to XBee", e);
			}
			
			//reset out pos
			outPosition = 0;
		}
	};
	
	public XmppXBeeConnection(XBeeSink sink, int inBufferSize) {
		this.sink = sink;
		inputBuffer = new int[inBufferSize];
		
		try {
			pis.connect(pos);	
		} catch (IOException e) {
			// cause it won't happen
			throw new RuntimeException(e);
		}
	}
	
	public void addPacket(int[] packet) throws IOException {
		
		log.debug("addPacket -- received " + ByteUtils.toBase16(packet));
		
		boolean wasEmpty = pis.available() == 0;
		
		for (int i = 0; i < packet.length; i++) {
			pos.write(packet[i]);	
		}
		
		if (wasEmpty) {
			// critical: notify XBee that data is available
			synchronized(this) {
				log.debug("Notifying any XBee input stream thread that new data is available");
				this.notify();
			}			
		}
		
		// always write to buffer starting at inputPosition
//		synchronized (inputBuffer) {
//			boolean empty = inBytesAvailable == 0;
//			
//			inBytesAvailable+= packet.length;
//			
//			if (inputPosition + packet.length < (inputBuffer.length)) {
//				System.arraycopy(packet, 0, inputBuffer, inputPosition, packet.length);
//				
//				if (inputPosition + packet.length == inputBuffer.length -1) {
//					inputPosition = 0;
//				} else {
//					inputPosition+= packet.length;
//				}
//			} else if (inputPosition == inputBuffer.length - 1) {
//				log.debug("at end of input buffer, flipping");
//				System.arraycopy(packet, 0, inputBuffer, 0, packet.length);
//				inputPosition = packet.length;
//			} else {
//				// split
//				log.debug("splittng packet across input buffer");
//				System.arraycopy(packet, 0, inputBuffer, inputPosition, inputBuffer.length - inputPosition - 1);
//				System.arraycopy(packet, 0, inputBuffer, 0, packet.length - (inputBuffer.length - inputPosition - 1));
//				inputPosition = packet.length - (inputBuffer.length - inputPosition - 1);
//			}
//			
//			// move inputPosition
//			
//			if (empty) {
//				log.debug("Notify threads blocking on read() that new data is available: " + inBytesAvailable + " bytes");
//				inputBuffer.notify();
//			}
//			
//			// critical: notify XBee that data is available
//			synchronized(this) {
//				this.notify();
//			}
//		}
	}
	
	public void close() {
		// nothing to do here
	}

	public InputStream getInputStream() {
		return this.pis;
	}

	public OutputStream getOutputStream() {
		return this.out;
	}
}
