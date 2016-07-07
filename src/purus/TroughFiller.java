package purus;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import haven.Button;
import haven.CharWnd;
import haven.Coord;
import haven.Frame;
import haven.GItem;
import haven.GOut;
import haven.Gob;
import haven.Inventory;
import haven.Label;
import haven.Loading;
import haven.OCache;
import haven.Resource;
import haven.RichTextBox;
import haven.Text;
import haven.UI;
import haven.WItem;
import haven.Widget;
import haven.Window;
import haven.automation.GobSelectCallback;

public class TroughFiller extends Window implements GobSelectCallback {
	
	// Basically picks up carrots from ground and puts them in troughs
	// 1. Choose troughs by alt clicking them
	// 2. Then it collects carrots until inventory is full or no carrots found
	// 3. Then right click trough with them until trough is full or no carrots in inventory
	// 4. Repeat from 2.
	
	// HUOM Menu grid pitää käynnistää ilman run(); 
	private static final Text.Foundry infof = new Text.Foundry(Text.sans, 10).aa(true);
    private static final Text.Foundry countf = new Text.Foundry(Text.sans.deriveFont(Font.BOLD), 12).aa(true);
    private List<Gob> troughs = new ArrayList<>();
    private final Label lbls;
    public boolean terminate = false;
    private boolean running = false;
    private Button clearbtn, runbtn, stopbtn;
	
	private BotUtils BotUtils;
	
	private Window window;

	public TroughFiller(UI ui, Widget w, Inventory i) {
        super(new Coord(270, 180), "Trough Filler");
        
		BotUtils = new BotUtils(ui, w, i);

	        Widget inf = add(new Widget(new Coord(245, 55)) {
	            public void draw(GOut g) {
	                g.chcolor(0, 0, 0, 128);
	                g.frect(Coord.z, sz);
	                g.chcolor();
	                super.draw(g);
	            }

	        }, new Coord(10, 10).add(wbox.btloff()));
	        Frame.around(this, Collections.singletonList(inf));
	        inf.add(new RichTextBox(new Coord(245, 55),
	        ("Alt + Click to select troughs to fill or leave empty to use all troughs in the area.\n\n" +
	                        "Currently only works for carrots.\n\n"), CharWnd.ifnd));

	        Label lblstxt = new Label("Troughs Selected:", infof);
	        add(lblstxt, new Coord(15, 90));
	        lbls = new Label("0", countf, true);
	        add(lbls, new Coord(120, 88));

	        clearbtn = new Button(140, "Clear Selection") {
	            @Override
	            public void click() {
	                troughs.clear();
	                lbls.settext(troughs.size() + "");
	            }
	        };
	        add(clearbtn, new Coord(65, 115));

	        runbtn = new Button(140, "Run") {
	            @Override
	            public void click() {
	                if (troughs.size() == 0) {

	                    OCache oc = ui.sess.glob.oc;
	                    synchronized (oc) {
	                        for (Gob gob : oc) {
	                            try {
	                                Resource res = gob.getres();
	                                if (res != null && res.name.contains("gfx/terobjs/trough")) {
	                                	troughs.add(gob);
	                                }
	                            } catch (Loading l) {
	                            }
	                        }
	                    }

	                    if (troughs.size() == 0) {
	                        gameui().error("No troughs selected or found.");
	                        return;
	                    } else {
	                        lbls.settext(troughs.size() + "");
	                    }
	                }

	                this.hide();
	                cbtn.hide();
	                clearbtn.hide();
	                stopbtn.show();
	                terminate = false;

	                t.start();
	            }
	        };
	        add(runbtn, new Coord(65, 150));

	        stopbtn = new Button(140, "Stop") {
	            @Override
	            public void click() {
	                running = false;
	                terminate = true;
	                if (gameui().map.pfthread != null)
	                    gameui().map.pfthread.interrupt();
	                if (t != null)
	                    t.interrupt();
	                try {
	                    if (running)
	                        gameui().map.wdgmsg("click", Coord.z, gameui().map.player().rc, 1, 0);
	                } catch (Exception e) { // ignored
	                }
	                this.hide();
	                runbtn.show();
	                clearbtn.show();
	                cbtn.show();
	                troughs.clear();
	                lbls.settext(troughs.size() + "");
	            }
	        };
	        stopbtn.hide();
	        add(stopbtn, new Coord(65, 150));
	    }
	
	Thread t = new Thread(new Runnable() {
		public void run()  {
			running = true;
			while(!terminate) {
				if(BotUtils.findObjectByNames(1000, "gfx/terobjs/items/carrot")==null)
					terminate();
				// Pick up carrots until carrots not found or no space left
				while(BotUtils.invFreeSlots()>0 && BotUtils.findObjectByNames(1000, "gfx/terobjs/items/carrot")!=null) {
                    if (terminate)
                        return;
					Gob gob = BotUtils.findObjectByNames(1000, "gfx/terobjs/items/carrot");
					BotUtils.pfRightClick(gob, 3);
					while(BotUtils.findObjectById(gob.id)!=null) {
						BotUtils.sleep(10);
					}
				}
				// And now we put carrots in trough
				boolean wait = false;
				while(true) {
					
					if(troughs.get(0)==null) {
						BotUtils.sysMsg("No troughs found or all selected troughs are full", Color.white);
						terminate();
					}
                    if (terminate)
                        return;
					Gob trough = BotUtils.findObjectById(troughs.get(0).id);
					BotUtils.pfRightClick(trough, 3);
					// If moving to another trough we must wait so current window closes and player starts moving towards another
					if(wait) {
					BotUtils.sleep(1000);
					wait = false; 
					}
					while(BotUtils.isMoving())
						BotUtils.sleep(10);
					int wnd = -1;
					while(true) {
						if(BotUtils.gui().getwnd("Trough")!=null) {
						wnd = BotUtils.getFuelMeter(BotUtils.gui().getwnd("Trough"));
						if(wnd==-1)
							BotUtils.sleep(10);
						else
							break;
						} else
							BotUtils.sleep(10);
					}
					if(wnd==100) {
						System.out.println("Trough full, removing from list and retrying");
						troughs.remove(0);
						wait = true;
						continue;
					}
					GItem i = null;
	                for (Widget w = BotUtils.playerInventory().child; w != null; w = w.next) {
	                    if (w instanceof GItem && ((GItem) w).resname().equals("gfx/invobjs/carrot")) {
	                    	i = (GItem) w;
	                        continue;
	                    }
	                }
	                if(i==null)
	                	break;
	                else
	                	BotUtils.takeItem(i);
					while(BotUtils.getItemAtHand()==null) {
						BotUtils.sleep(10);
					}
		            BotUtils.gui().map.wdgmsg("itemact", Coord.z, trough.rc, 0, 0, (int) trough.id, trough.rc, 0, -1);
					while(BotUtils.getItemAtHand()!=null) {
						BotUtils.sleep(10);
					}
				}
			}
		}
	});
	
    public void gobselect(Gob gob) {
        Resource res = gob.getres();
        if (res != null) {
            if (res.name.equals("gfx/terobjs/trough")) {
                if (!troughs.contains(gob)) {
                    troughs.add(gob);
                    lbls.settext(troughs.size() + "");
                }
            }
        }
    }
    
    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == cbtn)
            reqdestroy();
        else
            super.wdgmsg(sender, msg, args);
    }
    
    public void terminate() {
        terminate = true;
        if (t != null)
            t.interrupt();
        try {
            if (running)
                gameui().map.wdgmsg("click", Coord.z, gameui().map.player().rc, 1, 0);
        } catch (Exception e) { // ignored
        }
        if (gameui().map.pfthread != null) {
            gameui().map.pfthread.interrupt();
        }
        this.destroy();
    }
	
}