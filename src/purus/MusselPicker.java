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
    
    private boolean stop;
    
	BotUtils BotUtils;

	public MusselPicker (UI ui, Widget w, Inventory i) {
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
		retry:
		while(BotUtils.findObjectByNames(1000, "gfx/terobjs/herbs/mussels") != null) {
			Gob gob = BotUtils.findObjectByNames(1000, "gfx/terobjs/herbs/mussels");
			BotUtils.doClick(gob, 3, 0);
			while(ui.root.findchild(FlowerMenu.class)==null)
					BotUtils.sleep(10);
			if(stop)
				return;
			@SuppressWarnings("deprecation")
			FlowerMenu menu = ui.root.findchild(FlowerMenu.class);
	            if (menu != null) {
	                for (FlowerMenu.Petal opt : menu.opts) {
	                    if (opt.name.equals("Pick")) {
	                        menu.choose(opt);
	                        menu.destroy();
	            			while(BotUtils.findObjectById(gob.id) != null) {
	            				sleep(10);
	            			}
	                    }
	                }
	            } else
	            	continue retry;
		}
		BotUtils.sysMsg("No mussels found, mussel picker finished.", Color.WHITE);
        window.destroy();
				return;
		}
	});
	
	private void sleep(int t){
		try {
			Thread.sleep(t);
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
	}
	
			private class StatusWindow extends Window {
		        public StatusWindow() {
		            super(Coord.z, "Mussel Picker");
		            setLocal(true);
		            add(new Button(120, "Cancel") {
		                public void click() {
		                    window.destroy();
		                    if(t != null) {
		                    	gameui().msg("Mussel Picker Cancelled", Color.WHITE);
		                    	stop = true;
		                    }
		                }
		            });
		            pack();
		        }
		        @Override
		        public void wdgmsg(Widget sender, String msg, Object... args) {
		            if (sender == cbtn) {
		                stop = true;
		                reqdestroy();
		            }
		            else
		                super.wdgmsg(sender, msg, args);
		        }
		        
			}
}