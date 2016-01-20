package purus;

import java.awt.Color;

import haven.Button;
import haven.Coord;
import haven.FlowerMenu;
import haven.FlowerMenu.Petal;
import haven.Gob;
import haven.Inventory;
import haven.UI;
import haven.Widget;
import haven.Window;

public class MusselPicker {

public static boolean MusselsNearby;

	private final UI ui;
    private haven.Widget w;
    private haven.Inventory i;
    public Petal[] opts;
    private Widget window; 
    
	BotUtils BotUtils;

	public MusselPicker (UI ui, Widget w, Inventory i) {
		this.ui = ui;
		this.w = w;
		this.i = i;
		BotUtils = new BotUtils(ui, w, i);
	}
	
	// Whole script  is example of bad coding, you cant stop it and it will probably leave thread just running in background
	
	public void Run () {
	t.start();	
	}
	Thread t = new Thread(new Runnable() {
	public void run()  {
		Gob gob = BotUtils.findObjectByNames(500, "gfx/terobjs/herbs/mussels");
		if(gob != null)
			MusselsNearby = true;
		else
			MusselsNearby = false;
		while(MusselsNearby = true) {
			BotUtils.doClick(gob, 3, 0);
			//
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			@SuppressWarnings("deprecation")
			FlowerMenu menu = ui.root.findchild(FlowerMenu.class);
	            if (menu != null) {
	                for (FlowerMenu.Petal opt : menu.opts) {
	                    if (opt.name.equals("Pick")) {
	                        menu.choose(opt);
	                        menu.destroy();
	            			boolean onolemassa = true;
	            			while(onolemassa) {
		            			Gob onko = BotUtils.findObjectById(gob.id);
		            			if (onko != null) 
		            			onolemassa = true;
		            			else
		            			onolemassa = false;
	            			}
	                    }
	                }
	            }
         //   BotUtils.Choose(opts[1])
			gob = BotUtils.findObjectByNames(500, "gfx/terobjs/herbs/mussels");
			if(gob != null) 
			MusselsNearby = true;
			else
				break;
		}
		BotUtils.sysMsg("Mussel Picker Finished", Color.WHITE);
        window.destroy();
				return;
	}
	});
	
	// This thingy makes that stupid window with cancel button, todo: make it better
			private class StatusWindow extends Window {
		        public StatusWindow() {
		            super(Coord.z, "Mussel Picker");
		            setLocal(true);
		            add(new Button(120, "Cancel") {
		                public void click() {
		                    window.destroy();
		                    if(t != null) {
		                    	gameui().info("Mussel Picker Cancelled", Color.WHITE);
		                    	t.stop();
		                    }
		                }
		            });
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