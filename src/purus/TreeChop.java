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

public class TreeChop {


	private final UI ui;
    private haven.Widget w;
    private haven.Inventory i;
    public Petal[] opts;
    private Widget window; 
    private boolean stop;
    
	BotUtils BotUtils;

	public TreeChop (UI ui, Widget w, Inventory i) {
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
		BotUtils.sysMsg("Attempting to chop nearest tree", Color.WHITE);
		Gob tree = BotUtils.findNearestTree(1000);
		if(tree==null) {
			BotUtils.sysMsg("No trees found nearby", Color.WHITE);
			window.destroy();
			t.stop();
		}
		while(BotUtils.findObjectById(tree.id) != null) {
			BotUtils.drink();
			if(stop)
				return;
			BotUtils.doClick(tree, 3, 0);
			while(ui.root.findchild(FlowerMenu.class)==null)
				BotUtils.sleep(10);
			if(stop)
				return;
			FlowerMenu menu = ui.root.findchild(FlowerMenu.class);
	        if (menu != null) {
	            for (FlowerMenu.Petal opt : menu.opts) {
	                if (opt.name.equals("Chop")) {
	                    menu.choose(opt);
	                    menu.destroy();
	                }
	            }
	        }
	        sleep(2000);
            while(BotUtils.gui().prog >= 0) {
            	sleep(10);
            }
			if(stop)
				return;
		}
		
		BotUtils.sysMsg("Tree succesfully chopped! Tree chopper finished.", Color.white);
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
		            super(Coord.z, "Tree Chopper");
		            setLocal(true);
		            add(new Button(120, "Cancel") {
		                public void click() {
		                    window.destroy();
		                    if(t != null) {
		                    	gameui().msg("Tree Chopper Cancelled", Color.WHITE);
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