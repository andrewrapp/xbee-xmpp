## Overview ##

This project uses [XMPP](http://en.wikipedia.org/wiki/Extensible_Messaging_and_Presence_Protocol) (AKA Jabber) to provide IP connectivity to an XBee network, allowing it to be shared by multiple applications that may reside on different machines and even different networks!  As long as you can connect to an XMPP server, your XBee network and clients can reside behind firewalls and/or [NAT](http://en.wikipedia.org/wiki/Network_address_translation) devices and still talk to each other.  This project is built on [XBee-API](http://code.google.com/p/xbee-api/) and [Smack API](http://www.igniterealtime.org/projects/smack/index.jsp) and is supported on Windows, Mac, Linux and any other OS that supports Java 5+ and RXTX (RXTX for Gateway only).

![http://xbee-xmpp.googlecode.com/svn/trunk/docs/diagrams/architecture.png](http://xbee-xmpp.googlecode.com/svn/trunk/docs/diagrams/architecture.png)

## News ##

  * 11/7/10 The 0.2 release is now available!  In this release the [XBeeGtalkClient](http://code.google.com/p/xbee-xmpp/source/browse/trunk/src/com/rapplogic/xbee/xmpp/client/XBeeGtalkClient.java) and [XBeeOpenfireClient](http://code.google.com/p/xbee-xmpp/source/browse/trunk/src/com/rapplogic/xbee/xmpp/client/XBeeOpenfireClient.java) classes now extend XBee, so that after the initial XMPP connection, you can use the same code from an XBee-API project!

## How it Works ##

The system consists of one Gateway and n-Clients.  The Gateway is a daemon process that connects to an XBee radio, via a serial connection, and relays packets between internet and XBee network.  The Gateway receives packets from the internet, encoded in XMPP messages, and converts them to Object representations (via xbee-api) and forwards to the XBee network.  Similarly, the Gateway receives packets from the XBee network, encodes them as XMPP messages, and forwards them to Clients.  A Client may exist anywhere on the internet, as long as it can access the XMPP server.  Clients forward/receive packets to/from the XBee network via the Gateway (as depicted in the diagram above).

## XMPP Providers ##

Currently there are two choices for XMPP servers: Google Talk and [Openfire](http://www.igniterealtime.org/projects/openfire/index.jsp), although it should be possible to use any compliant XMPP server. The Google Talk option is recommended only if you want to talk to your XBee network from anywhere on the internet and you don't have access to a server with a public IP address.  If your Client and Gateway are both on the same private network, or you have a server with a public IP address, you should use Openfire.  Openfire is easy to install and has a very nice web console.

<a href='Hidden comment: 
This is accomplished with a Gateway instance that forwards XBee packets to XMPP Clients and similarly receives XBee packets from XMPP Clients and forwards them to the XBee network.

*** Google [http://code.google.com/apis/talk/open_communications.html encourages] open communication and even provides a Google Talk [http://code.google.com/apis/talk/libjingle/index.html C library] for writing applications, but keep in mind that they apparently limit traffic.  Here"s an excerpt from the [http://googletalk.blogspot.com/2007_12_01_googletalk_archive.html Google Talk Blog]:

_Please note that the Google Talk service enforces traffic limitations on user accounts, so if you want to support more than a few thousand Google Talk users on your bot, connect using the server-to-server protocol (either by making your bot act as an XMPP server or by hosting the bot on your own XMPP server)_

I"m not sure what the traffic limits are (amount of data or just # of users) but if you are concerned, consider using Openfire instead.
'></a>

## Problems? ##

This project is very beta and while there are no known issues at this time, you can expect bugs to crop up.  If you come across an issue please report it on the [issue tracker](http://code.google.com/p/xbee-xmpp/issues/list), so that I can improve the project.

<a href='Hidden comment: 
==Reliability==
if an exception occurs in gateway, during send packet, or received packet, it may not be communicated back to the client.

network issues
'></a>

<a href='Hidden comment: 
== API ==

The API is very basic at this time, providing only the core features for sending and receiving packets.  Request packets are sent with the sendXBeeRequest method.  This method is asynchronous, and similar to the sendAsynchronous method in the XBee-API:

client.sendXBeeRequest(new AtCommand("AI"));

Response packets are received through the XBee-API PacketListener interface.  Just as with XBee-API, you add a listener and implement this interface:

```
client.addPacketListener(this);
...
public void processResponse(XBeeResponse response) {
    System.out.println("I got a response " + response);
}
```

'></a>

## Questions/Feedback ##

Questions about this project should be posted to http://groups.google.com/group/xbee-api?pli=1

## Getting Started ##

Before working the XBee-XMPP you should first be familiar with [XBee-API](http://code.google.com/p/xbee-api/)

I'm going to use Google Talk in this example so that we get can up and running quickly without needing to install a server.  You must have at least two Google Talk accounts to run this example: one account will be used for the Gateway and the other for the Client.

Download the [code](http://xbee-xmpp.googlecode.com/files/xbee-xmpp-0.1.zip), unzip and create an Eclipse project for it (on the machine that your XBee is plugged into).  The download includes everything thing you need to run the examples, except Eclipse and Java.

Open the XBeeXmppGatewayExample.java and replace yourclient@gmail.com with your Google Talk Client user account:

`clientList.add("yourclient@gmail.com");`

Now create a XBeeGtalkGateway object, with the Google Talk account for your Gateway user, the password, and the COM port/baud rate of the attached XBee:

`gateway = new XBeeGtalkGateway("yourgateway@gmail.com", "yourgatewaypassword", clientList, "/dev/tty.usbserial-A6005v5M", 9600);`

Save it. (Eclipse automatically compiles it)

Now create a command prompt and startup the Gateway (sorry, currently only mac/linux):

`./startXmppClass.sh com.rapplogic.xbee.xmpp.examples.XBeeXmppGatewayExample`

You should see some console output.

Now we are ready to start the Client.  The wonderful thing about this solution is that the Client doesn't need to be on the same machine or even the same network.  Since XMPP is a network protocol, we can start the client anywhere, as long as it can connect to the Google Talk server (e.g. coffee shop, hotel etc.)

If you are on a different machine, again download the code, unzip and create an Eclipse project for it.

Open XBeeXmppClientExample.java in Eclipse and create a XBeeGtalkClient object with the Google Talk Client user and password, and the Gateway user

`XBeeXmppClient client = new XBeeGtalkClient("yourclient@gmail.com", "yourclientpassword", "yourgateway@gmail.com");`

Save

Now we will run the Client in Eclipse.  Select Run As->Java Application from the Run menu

This example sends an AI (Association Status) to the XBee that is attached to the Gateway every 10 seconds.  You should be seeing output in the Eclipse console.

To demonstrate how well XMPP tolerates disconnects, kill the the Gateway and you should see a "gateway offline" message in the Client console.  When the Gateway is offline the Client just waits for it to come back online.  Now startup the Gateway and the Client will resume sending requests.  You can do a similar test with the Gateway.

Using Openfire is identical except that you will be instead creating XBeeOpenfireClient and XBeeOpenfireGateway objects.  Of course, you'll also need to setup your own Openfire server.  The good news is that it's not hard and I'll describe how to do that at a later time.

Now go off and write your remote XBee XMPP application and send/receive any XBeeRequest/XBeeResponse to your XBee nework from anywhere on the internet!

[Here's](http://code.google.com/p/xbee-xmpp/source/browse/trunk/src/com/rapplogic/xbee/xmpp/examples/XBeeXmppClientRemoteAtExample.java) a more involved example that uses Remote AT to request samples from a remote XBee