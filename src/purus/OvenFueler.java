package purus;

import java.awt.Color;
import java.util.List;

import haven.Button;
import haven.Coord;
import haven.GItem;
import haven.Gob;
import haven.Inventory;
import haven.UI;
import haven.WItem;
import haven.Widget;
import haven.Window;

public class OvenFueler {
	/* This script collects dragonfly with crawlin speed around swamp
	 * How it works plan:
	 * 1. Right click nearest dragonfly
	 * 2. Check if player is not moving or dragonfly is out of camera/collected to inventory
	 * 3. Check if inv is on hand (something in hand) if not then repeat
	 */

	private final UI ui;
    private haven.Widget w;
    private Inventory i;
    private Widget window;  
    
	BotUtils BotUtils;

	public OvenFueler (UI ui, Widget w, Inventory i) {
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
			List<Gob> ovens = BotUtils.findObjectsByNames(2000, "gfx/terobjs/oven");
			Coord startCoord = new Coord((int)BotUtils.player().rc.x, (int)BotUtils.player().rc.y);
			for(Gob gob : ovens) {
				BotUtils.pfRightClick(gob, 1);
				BotUtils.waitForWindow("Oven");
				Boolean keepfueling = true;
				while(keepfueling) {
			        BotUtils.takeItem(BotUtils.gui().maininv.getItemPartial("Branch").item);
			        int timeout = 0;
			        while (BotUtils.gui().hand.isEmpty()) {
			        	timeout++;
			        	if(timeout==50) {
			        		BotUtils.sysMsg("No branches found in the inventory!", Color.white);
			        		window.destroy();
			        		t.stop();
			        	}
			        	BotUtils.sleep(100);
			        }
		            BotUtils.gui().map.wdgmsg("itemact", Coord.z, gob.rc, 0, 0, (int) gob.id, gob.rc, 0, -1);
		            while(BotUtils.getItemAtHand()!=null) {
		            	BotUtils.sleep(100);
		            }
		            BotUtils.pfRightClick(gob);
					BotUtils.waitForWindow("Oven"); 
		            if(BotUtils.getFuelMeter(BotUtils.gui().getwnd("Oven"))>=13)
		            	keepfueling = false;
				}
				BotUtils.pfLeftClick(startCoord, null);
				int retry = 0;
                while(BotUtils.player().rc.x!=startCoord.x||BotUtils.player().rc.y!=startCoord.y) {
                	// Horrible workaround, probably
                	if(!BotUtils.isMoving()) {
                		retry++;
                		// If player is not moving for 10 seconds retry pf click
                		if(retry>50) {
                			retry = 0;
                			System.out.println("Player seems not to be moving, retrying click to: " + startCoord.x + ", " + startCoord.y);
            				BotUtils.pfLeftClick(startCoord, null);
                		}
                	}
    				BotUtils.sleep(100);
			}
			}
			
			
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

		// This thingy makes that stupid window with cancel button, TODO: make it better
		private class StatusWindow extends Window {
	        public StatusWindow() {
	            super(Coord.z, "Oven Fueler");
	            setLocal(true);
	            add(new Button(120, "Cancel") {
	                public void click() {
	                    window.destroy();
	                    if(t != null) {
	                    	gameui().msg("Oven Fueler Cancelled", Color.WHITE);
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
	}