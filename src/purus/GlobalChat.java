package purus;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.client.ClientConnectedEvent;
import org.kitteh.irc.client.library.event.client.ClientConnectionClosedEvent;
import org.kitteh.irc.client.library.util.AcceptingTrustManagerFactory;
import org.kitteh.irc.lib.net.engio.mbassy.listener.Handler;

import haven.Coord;
import haven.TextEntry;
import haven.Textlog;
import haven.Widget;
import haven.Window;

public class GlobalChat extends Window {
    private TextEntry in;
    private Textlog out;
    private Client client;
    private String charName;
    
    private String channel = "#haven";
    private String server = "naamio.fi.eu.synirc.net";

	public GlobalChat(Coord sz, String chrName) {
        super(sz, "Global Chat");
        in = add(new TextEntry(sz.x, ""), 0, sz.y - 20);
        in.canactivate = true;
        out = add(new Textlog(new Coord(sz.x, sz.y - 20)), Coord.z);
        this.charName = chrName.replaceAll(" ", "_");
        join();
	}
	
    public class Listener {
    	@Handler
        public void onChannelMessageEvent(ChannelMessageEvent event) {
        	out.append(new SimpleDateFormat("[HH:mm] ").format(new Date())+
        			event.getActor().getNick()+": "+event.getMessage());
        }
        
        @Handler
        public void onClientConnectedEvent(ClientConnectedEvent event) {
        	out.append("Succesfully connected as " + client.getNick() + "!");
        }
        
        @Handler
        public void onClientConnectionClosedEvent(ClientConnectionClosedEvent event) {
        	out.append("Lost connection to the server" + event.isReconnecting() != null ? " attempting to reconnect..." : "");
        }
    }
	
	public void join() {
    	out.append("Connecting as "+charName+"...");
        client = Client.builder().secureTrustManagerFactory(new AcceptingTrustManagerFactory()).
        		nick(charName).serverHost(server).build();
        client.getEventManager().registerEventListener(new Listener());
        client.addChannel(channel);
	}
	
	public void disconnect() {
    	client.shutdown("");
	}
	
	@Override
    public void uimsg(String msg, Object... args) {
        if (msg == "log") {
            out.append((String) args[0]);
        } else {
            super.uimsg(msg, args);
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == in) {
            if (msg == "activate") {
        		client.sendMessage(channel, (String) args[0]);
            	out.append(new SimpleDateFormat("[HH:mm] ").format(new Date())+" "+args[0]);
                in.settext("");
                return;
            }
        } else if (sender == this && msg.equals("close")) {
        	disconnect();
            reqdestroy();
        } else
        super.wdgmsg(sender, msg, args);
    }
	
}
