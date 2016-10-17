package purus;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import haven.Button;
import haven.Charlist;
import haven.Coord;
import haven.Equipory;
import haven.FlowerMenu;
import haven.FlowerMenu.Petal;
import haven.GAttrib;
import haven.GItem;
import haven.GameUI;
import haven.Gob;
import haven.HavenPanel;
import haven.IMeter;
import haven.Inventory;
import haven.ItemInfo;
import haven.Loading;
import haven.Resource;
import haven.UI;
import haven.VMeter;
import haven.WItem;
import haven.Widget;
import haven.Window;

public class BotUtils {

	private final UI ui;
    private haven.Widget w;
    private haven.Inventory i;
    public Petal[] opts;
    private static Pattern liquidPattern;
    String liquids =  haven.Utils.join("|", new String[] { "Water", "Piping Hot Tea", "Tea" });
    String pattern = String.format("[0-9.]+ l of (%s)", liquids);
    Map<Class<? extends GAttrib>, GAttrib> attr = new HashMap<Class<? extends GAttrib>, GAttrib>();
    
	public BotUtils (UI ui, Widget w, Inventory i) {
		this.ui = ui;
		this.w = w;
		this.i = i;
		liquidPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
	}
	
	// Returns amount of free inventory slots
	public int invFreeSlots() {
		int takenSlots = 0;
		for (Widget i = playerInventory().child; i != null; i = i.next) {
			if (i instanceof WItem) {
				WItem buf = (WItem) i;
				takenSlots += buf.size().x * buf.size().y;
			}
		}
		int allSlots = playerInventory().size().x * playerInventory().size().y;
		return allSlots - takenSlots;
	}
	
	// Pushes button in window
	public void pushButton(String btnName, String wndName) {	
		Window wnd = gui().getwnd(wndName);
		for (Widget i = wnd.child; i != null; i = i.next) {
            if (i instanceof Button) {
                Button b = (Button) i;
                if (b.text.text.equals(btnName)) {
                    b.click();
                    break;
                }
            }
}
        }
	
    public GameUI gui() {
    	return ui.gui;
    }
    
    // Returns amount of fuel ticks from range 0-100
    // Returns -1 if window could not be found
    public int getFuelMeter(Window window) {
    	if(window.getchild(VMeter.class)!=null)
    	return window.getchild(VMeter.class).amount;
    	else return -1;
    }
    
    // Transfers item
    public void transferItem(WItem item) {
    	item.item.wdgmsg("transfer", Coord.z);
    }
    
    // Waits for window to appear
    public void waitForWindow(String windowName) {
    	while(gui().getwnd(windowName)==null) {
    		sleep(10);
    	}
    }
    
    // Returns witems with specific names from inventory
    public List<WItem> getInventoryItemsByNames(Inventory invwdg, List<String> items) {
    	List<WItem> witems = new ArrayList<WItem>();
    	for(WItem wi : getInventoryContents(invwdg)) {
			String resname = wi.item.resname();
    		for(String s : items) {
    			if(resname.equals(s))
    				witems.add(wi);
    		}
    	}
		return witems;
    }
    
    // Returns witems with specific name from inventory
    public List<WItem> getInventoryItemsByName(Inventory invwdg, String item) {
    	List<WItem> witems = new ArrayList<WItem>();
    	for(WItem wi : getInventoryContents(invwdg)) {
			String resname = wi.item.resname();
    			if(resname.equals(item))
    				witems.add(wi);
    	}
		return witems;
    }
    
    // Returns all items that inventory contains
    public List<WItem> getInventoryContents(Inventory invwdg) {
        List<WItem> witems = new ArrayList<WItem>();
        for (Widget witm = invwdg.lchild; witm != null; witm = witm.prev) {
            if (witm instanceof WItem) {
                witems.add((WItem) witm);
            }
        }
        return witems;
    }
    
    // Logout to char selection
    public void logoutChar() {
        gui().act("lo", "cs");
    }
    
    // Chooses character from char selection
    public void chooseChar(String name)  {
        Charlist.choose_player(name);
        sleep(1000);
        if(gui().getwnd("Restart")!=null) {
        	Window wnd = gui().getwnd("Restart");
        	System.out.println("Found it");
        	for (Widget i = wnd.child; i != null; i = i.next) {
    			if (i instanceof Button) {
    				Button b = (Button) i;
    				System.out.println(b.text);
    				if (b.text.text.equals("Yes")) {
    					b.click();
    					break;
    				}
    			}
    		}
        }
    }
    
    // Logs off
    public void logout() {
    	gui().act("lo");
    }
    
    // Teleports to hearth fire
    public void tpHF() {
    	gui().menu.wdgmsg("act", new Object[]{"travel", "hearth"});
		sleep(4000);
        while(gui().prog >= 0) {
        	sleep(100);
        }
    }
    
    // Returns energy (0-100%)
    public int getEnergy() {
        IMeter.Meter nrj = gui().getmeter("nrj", 0);
        if (nrj == null) {
            return -1;
        }
        return nrj.a;
    }
    
    // Clicks gob with pf rightclick (pathfinds near it and then rightclicks it)
    public void pfRightClick(Gob gob, int mod) {
    	gui().map.pfRightClick(gob, -1, 3, mod, null);
    }
    
	// Move to coords with pathfinder, 2nd argument for modifier (null to just move)
	public void pfLeftClick(Coord c, String mod) {
		gui().map.pfLeftClick(c, mod);
	}
    
    // Drinks water/tea from containers in inventory
    public void drink() {
		GameUI gui = HavenPanel.lui.root.findchild(GameUI.class);
		 WItem item = findDrink(playerInventory());
		 
		 if (item != null) {
			 item.item.wdgmsg("iact", Coord.z, 3);
			 sleep(250);
				@SuppressWarnings("deprecation")
				FlowerMenu menu = ui.root.findchild(FlowerMenu.class);
		            if (menu != null) {
		                for (FlowerMenu.Petal opt : menu.opts) {
		                    if (opt.name.equals("Drink")) {
		                        menu.choose(opt);
		                        menu.destroy();
		                        sleep(500);
		        	            while(gui.prog >= 0) {
		        	            	sleep(100);
		        	            }
		                    }
		                }
		            }
		 }
    }
    
    public void sysMsg(String msg, Color color ) {
    	ui.root.findchild(GameUI.class).msg(msg,color);
    }
    
    // Sets speed for player
    // 0 = Crawl 1 = Walk  2 = Run 3 = Sprint
    public void setSpeed(int speed) {
    	haven.Speedget.setSpeed = true;
    	haven.Speedget.SpeedToSet = speed;
    }
    
	// Takes item in hand
    public void takeItem(Widget item) {
        item.wdgmsg("take", Coord.z);
        while(getItemAtHand()==null) {
        	sleep(10);
        }
    }
    
    //  Returns item in hand
    public GItem getItemAtHand() {
        for (GameUI.DraggedItem item : ui.gui.hand)
            return item.item;
        for (GameUI.DraggedItem item : ui.gui.handSave)
            return item.item;
        return null;
    }
    
	//Drops thing from hand 
	public void drop_item(int mod) {
		ui.gui.map.wdgmsg("drop", Coord.z, gui().map.player().rc, mod);
	}
	
	// Use item in hand to ground below player, for example, plant carrot
	public void mapInteractClick(int mod) {
		 ui.gui.map.wdgmsg("itemact", getCenterScreenCoord(), player().rc, 3, ui.modflags());
	}
	
	// return center of screen
		public Coord getCenterScreenCoord() {
			Coord sc, sz;
				sz =  ui.gui.map.sz;
				sc = new Coord((int) Math.round(Math.random() * 200 + sz.x / 2
						- 100), (int) Math.round(Math.random() * 200 + sz.y / 2
						- 100));
				return sc;
		}
	
	// Find object by ID, returns null if not found (duh)
    public Gob findObjectById(long id) {
        return ui.sess.glob.oc.getgob(id);
    }
	
	// true if player moving
	public boolean isMoving() {
		if (player().getv()==0)
			return false;
		else
		return true;
	}
	
	// Chooses option from flower menu
	public void Choose(Petal option) {
       w.wdgmsg("cl", option.num, ui.modflags());
	}
	
	// Click some object with item on hand
	// Modifier 1 - shift; 2 - ctrl; 4 alt;
    public void itemClick(Gob gob, int mod) {
        ui.gui.map.wdgmsg("itemact", Coord.z, gob.rc, mod, 0, (int)gob.id, gob.rc, 0, -1);
    }
	
	// Click some object with specific button and modifier
	// Button 1 = Left click and 3 = right click
	// Modifier 1 - shift; 2 - ctrl; 4 - alt;
    public void doClick(Gob gob, int button, int mod) {
        ui.gui.map.wdgmsg("click", Coord.z, gob.rc, button, 0, mod, (int)gob.id, gob.rc, 0, -1);
    }

	// Finds nearest crop with x stage
		 public Gob findNearestStageCrop(int radius, int stage, String... names) {
		        Coord plc = player().rc;
		        double min = radius;
		        Gob nearest = null;
		        synchronized (ui.sess.glob.oc) {
		            for (Gob gob : ui.sess.glob.oc) {
		                double dist = gob.rc.dist(plc);
		                if (dist < min) {
		                    boolean matches = false;
		                    for (String name : names) {
		                        if (isObjectName(gob, name)) {
		                        	if (gob.getStage() == stage) {
		                            matches = true;
		                            break;
		                        	}
		                            // TO DO: KEKSI MITEN HARVESTAA VAAN STAGE 4 OLEVAT PORKKANAT
		                        }
		                    }
		                    if (matches) {
		                        min = dist;
		                        nearest = gob;
		                    }
		                }
		            }
		        }
		        return nearest;
		    }
		 
	// Finds objects by name, returns list of them
	public List<Gob> findObjectsByNames(int radius, String... names) {
		 Coord plc = player().rc;
	        double min = radius;
	        List<Gob> gobs = new ArrayList<Gob>();
	        synchronized (ui.sess.glob.oc) {
	            for (Gob gob : ui.sess.glob.oc) {
	                double dist = gob.rc.dist(plc);
	                if (dist < min) {
	                	for(String s : names) {
	                	if(isObjectName(gob, s)) {
	                		gobs.add(gob);
	                	}
	                	}
	                }
	            }
	        }
	        return gobs;
	}
	
	// Finds nearest objects and returns closest one
	 public Gob findNearestTree(int radius) {
	        Coord plc = player().rc;
	        double min = radius;
	        Gob nearest = null;
	        synchronized (ui.sess.glob.oc) {
	            for (Gob gob : ui.sess.glob.oc) {
	                double dist = gob.rc.dist(plc);
	                if (dist < min) {
	                    boolean matches = false;
	                        if (isObjectName(gob, "gfx/terobjs/tree") || isObjectName(gob, "gfx/terobjs/bushes") && !gob.getres().name.contains("stump") && !gob.getres().name.contains("log")) {
	                            matches = true;
	                        }
	                    if (matches) {
	                        min = dist;
	                        nearest = gob;
	                    }
	                }
	            }
	        }
	        return nearest;
	    }
		 
	// Finds nearest objects and returns closest one
	 public Gob findObjectByNames(int radius, String... names) {
	        Coord plc = player().rc;
	        double min = radius;
	        Gob nearest = null;
	        synchronized (ui.sess.glob.oc) {
	            for (Gob gob : ui.sess.glob.oc) {
	                double dist = gob.rc.dist(plc);
	                if (dist < min) {
	                    boolean matches = false;
	                    for (String name : names) {
	                        if (isObjectName(gob, name)) {
	                            matches = true;
	                            break;
	                        }
	                    }
	                    if (matches) {
	                        min = dist;
	                        nearest = gob;
	                    }
	                }
	            }
	        }
	        return nearest;
	    }
	
	 // Returns players gob
    public Gob player() {
        return ui.gui.map.player();
    }
    
    public Inventory playerInventory() {
        return ui.gui.maininv;
    }
    
    public static boolean isObjectName(Gob gob, String name) {
        try {
            Resource res = gob.getres();
            return (res != null) && res.name.contains(name);
        } catch (Loading e) {
            return false;
        }   
    }
    
    public FlowerMenu getMenu() {
        return ui.root.findchild(FlowerMenu.class);
    }
    
    public WItem findDrink(Inventory inv) {
        for (WItem item : inv.children(WItem.class)) {
            if (canDrinkFrom(item))
                return item;
        }
        Equipory e = gui().getequipory();
        WItem l = e.quickslots[6];
        WItem r = e.quickslots[7];
        if(canDrinkFrom(l))
        	return l;
        if(canDrinkFrom(r))
			return r;
        return null;
    }
    public boolean canDrinkFrom(WItem item) {	
        ItemInfo.Contents contents = getContents(item);
        if (contents != null && contents.sub != null) {
            for (ItemInfo info : contents.sub) {
                if (info instanceof ItemInfo.Name) {
                    ItemInfo.Name name = (ItemInfo.Name) info;
                    if (name.str != null && liquidPattern.matcher(name.str.text).matches())
                        return true;
                }
            }
        }
        return false;
    }
    public ItemInfo.Contents getContents(WItem item) {
        try {
            for (ItemInfo info : item.item.info())
                if (info instanceof ItemInfo.Contents)
                    return (ItemInfo.Contents)info;
        } catch (Loading ignored) {}
        return null;
    }
    
	public void sleep(int t){
		try {
			Thread.sleep(t);
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
	}
}