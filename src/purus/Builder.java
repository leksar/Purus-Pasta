package purus;

import java.awt.Color;

import haven.Button;
import haven.Coord;
import haven.FlowerMenu;
import haven.FlowerMenu.Petal;
import haven.Gob;
import haven.Inventory;
import haven.TextEntry;
import haven.UI;
import haven.Widget;
import haven.Window;

public class Builder {


	private final UI ui;
    private haven.Widget w;
    private haven.Inventory i;
    public Petal[] opts;
    private Widget window; 
    
    private String wndName;
    private boolean start = false;
    
	BotUtils BotUtils;

	public Builder (UI ui, Widget w, Inventory i) {
		this.ui = ui;
		this.w = w;
		this.i = i;
		BotUtils = new BotUtils(ui, w, i);
	}
	
	public void Run () {
	t.start();	
	}
	Thread t = new Thread(new Runnable() {
	public void run()  {
		window = BotUtils.gui().add(new StatusWindow(), 300, 200);
		BotUtils.sysMsg("Builder started, enter precise name of window to build.", Color.white);
		while(!start) {
			BotUtils.sleep(100);
		}
		while(BotUtils.gui().getwnd(wndName)!=null) {
			BotUtils.drink();
			Window wnd = BotUtils.gui().getwnd(wndName);
			BotUtils.pushButton("Build", wndName);
			sleep(1000);
            while(BotUtils.gui().prog >= 0) {
            	sleep(100);
            }
		}
		BotUtils.sysMsg("Builder finished.", Color.white);
        window.destroy();
	}
	});
	
	private void sleep(int t){
		try {
			Thread.sleep(t);
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
	}
	
	// This thingy makes that stupid window with cancel button, todo: make it better
			private class StatusWindow extends Window {
		        public StatusWindow() {
		            super(Coord.z, "Builder");
		            setLocal(true);
		            int y = 0;
		            add(new TextEntry(120, "Window Name") {
		                {dshow = true;}
		                public void activate(String text) {
		                    wndName = text;
		                }
		            }, new Coord(0, y));
		            y += 35;
		            add(new Button(120, "Start") {
		                public void click() {
		                	gameui().msg("Started builder", Color.WHITE);
		                	start = true;
		                }
		            }, new Coord(0, y));
		            y += 35;
		            add(new Button(120, "Cancel") {
		                public void click() {
		                    window.destroy();
		                    if(t != null) {
		                    	gameui().msg("Builder cancelled", Color.WHITE);
		                    	t.stop();
		                    }
		                }
		            }, new Coord(0, y));
		            pack();
		        }
		        public void wdgmsg(Widget sender, String msg, Object... args) {
		            if (sender == this && msg.equals("close")) {
		                t.stop();
		            }
		            super.wdgmsg(sender, msg, args);
		        }
		        
			}
			//
}