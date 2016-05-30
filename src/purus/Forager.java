package purus;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import haven.Audio;
import haven.Button;
import haven.CheckBox;
import haven.Coord;
import haven.FlowerMenu;
import haven.Gob;
import haven.Inventory;
import haven.MapView;
import haven.TextEntry;
import haven.UI;
import haven.WItem;
import haven.Widget;
import haven.Window;

public class Forager  {
	
	BotUtils BotUtils;
    private Window window;
	private final UI ui;
    
    private Coord startCoord;
    private String coordfilename;
    private String configname;
    private String charname;
    private ArrayList<Coord> route = new ArrayList<Coord>();
    private String[] forageables;
    
    //Does something to avoid getting KO when animals are nearby
    private boolean animalcheck = true;
    private String[] dangerousanimals = {"gfx/kritter/badger", "gfx/kritter/boar", 
    		"gfx/kritter/bear", "gfx/kritter/bat", "gfx/kritter/lynx"};
   
    private boolean start = false;
    
	public Forager(UI ui, Widget w, Inventory i) {
		this.ui = ui;
		BotUtils = new BotUtils(ui, w, i);
	}
	/* Käyttöohjeet:
	 * 1. Tee Koord makerilla koordit
	 * 2. Tee .config päätteinen tiedosto jossa res pathit poimittaviin juttuihin
	 * 3. Käynnistä forager ja laita filenamet sitten paina start
	 * 4. Aloituspaikan pitää olla sama mistä koordeja alettiin tehdä
	 * -- Muista painaa enter kun laitat filenamet --
	 * Tällähetkellä ei varo yhtään sikoja karhuja ja ties mitä
	 * EI myöskään juo eli walk tai crawl suositeltavaa
	 */
	// TODO: Jemmaa invissä olevat itemit forageables listalla hf läheisimpään LC/Small chest
	public void Run () {
		t.start();	
		}
		Thread t = new Thread(new Runnable() {
		public void run()  {
			charname = BotUtils.gui().chrid;
			window  = BotUtils.gui().add(new StatusWindow(), 300, 500);
			// When start button in window is pressed it will start the loop
			while(!start) {
				BotUtils.sleep(100);
			}
			// Define starting coordinates, it moves with coord file relative to starting coords
			startCoord = new Coord(getX(), getY());
			// Load coordinates from route and list of forageables to pick from config
			loadRoute();
            loadConfig();
            // Disable sound
            Audio.volume = 0;
            // TODO: This loop is evil
            boolean running = true;
            runloop:
            while(running) {
            // Check if character is starving to avoid unwanted starvation deaths
            if(BotUtils.getEnergy() <= 20) {
            	BotUtils.sysMsg("Starving! Forager bot cancelled", Color.white);
            	window.destroy();
            	t.stop();
            }
            	// Loop that goes through the route
				for(int i = 0; i < route.size(); i++) {
					int x = startCoord.x - route.get(i).x;
					int y = startCoord.y - route.get(i).y;
					System.out.println("Forager: Moving to: " + x + ", " + y);
					BotUtils.pfLeftClick(new Coord(x, y), null);
                	BotUtils.sleep(2000);
					int retry = 0;
	                    while(BotUtils.player().rc.x!=x||BotUtils.player().rc.y!=y) {
	                        if(checkAnimals())
	                        	continue runloop;
	                    	// Horrible workaround, probably
	                    	if(!BotUtils.isMoving()) {
	                    		retry++;
	                    		// If player is not moving for 10 seconds retry pf click
	                    		if(retry>100) {
	                    			retry = 0;
	                    			System.out.println("Player seems not to be moving, retrying click to: " + x + ", " + y);
	            					BotUtils.pfLeftClick(new Coord(x, y), null);
	                    		}
	                    	}
	        				BotUtils.sleep(100);
					}
	                ArrayList<Gob> gobs = new ArrayList<Gob>(BotUtils.findObjectsByNames(5000, forageables));
	                boolean keepPicking = true;
	                while(keepPicking) {
	                	if(gobs.isEmpty()) {
	    					BotUtils.pfLeftClick(new Coord(x, y), null);
	    					retry = 0;
		                    while(BotUtils.player().rc.x!=x||BotUtils.player().rc.y!=y) {
		                        if(checkAnimals())
		                        	continue runloop;
		                    	// Horrible workaround, probably
		                    	if(!BotUtils.isMoving()) {
		                    		retry++;
		                    		// If player is not moving for 10 seconds retry pf click
		                    		if(retry>100) {
		                    			retry = 0;
		                    			System.out.println("Player seems not to be moving, retrying click to: " + x + ", " + y);
		            					BotUtils.pfLeftClick(new Coord(x, y), null);
		                    		}
		                    	}
		        				BotUtils.sleep(100);
						}
	                		keepPicking = false;
	                		break;
	                	}
	                	BotUtils.pfLeftClick(gobs.get(0).rc, null);
	                	System.out.println("Picking " + gobs.get(0).getres().name);
	                	// Try moving 5 times after that just give up
	                	int retrytimeout = 0;
	                	boolean shallcontinue = true;
	                	retry = 0;
	                    while(BotUtils.player().rc.x!=gobs.get(0).rc.x||BotUtils.player().rc.y!=gobs.get(0).rc.y) {
	                        if(checkAnimals())
	                        	continue runloop;
	                    	// Probably terrible way to do this again
	                    	if(!BotUtils.isMoving()) {
	                    		retry++;
	                    		// If player is not moving for 10 seconds retry pf click
	                    		if(retry>100) {
            	                	System.out.println("Player stopped moving while moving to pick curio, retrying click");
	                    			if(retrytimeout==5) {
	            	                	System.out.println("Cannot reach path to the curio, skipping");
	                    				shallcontinue = false;
	                    				 break;
	                    			}
	                    			retrytimeout++;
	                    			retry = 0;
	        	                	BotUtils.pfLeftClick(gobs.get(0).rc, null);
	                    		}
	                    	}
	                    	BotUtils.sleep(100);
	                    }
	                    if(shallcontinue) {
	                    BotUtils.doClick(gobs.get(0), 3, 0);
	                    BotUtils.sleep(1000);
	                    pick(gobs.get(0));
	                    }
	                    gobs.remove(0);
    					retry = 0;
    					BotUtils.pfLeftClick(new Coord(x, y), null);
	                    while(BotUtils.player().rc.x!=x||BotUtils.player().rc.y!=y) {
	                        if(checkAnimals())
	                        	continue runloop;
	                    	// Horrible workaround, probably
	                    	if(!BotUtils.isMoving()) {
	                    		retry++;
	                    		// If player is not moving for 10 seconds retry pf click
	                    		if(retry>100) {
	                    			retry = 0;
	                    			System.out.println("Player seems not to be moving, retrying click to: " + x + ", " + y);
	            					BotUtils.pfLeftClick(new Coord(x, y), null);
	                    		}
	                    	}
	        				BotUtils.sleep(100);
					}
	                }
	                
				}
				// After route teleport back to HF
				BotUtils.sleep(1000);
				BotUtils.tpHF();
	            
	            // Stores picked forageables in nearest chest
	            // TODO: If chest is full check if theres other chests nearby + add more containers
        		System.out.println("Emptying inventory to nearby small chests with space in them");
            	ArrayList<Gob> gobs = new ArrayList<Gob>(BotUtils.findObjectsByNames(5000, "gfx/terobjs/chest"));
            	Boolean repeat = true;
            	while(repeat) {
            	if(gobs.isEmpty()) {
            		BotUtils.sysMsg("Could not find chest nearby, stopping forager", Color.WHITE);
            		t.stop();
            	}
            	BotUtils.doClick(gobs.get(0), 3, 0);
            	BotUtils.sleep(1000);
            	BotUtils.waitForWindow("Chest");
        		List<WItem> invitems = new ArrayList<WItem>();
        		List<String> forageableitems = new ArrayList<String>();  
        		for(String s : forageables) {
        			forageableitems.add("gfx/invobjs/herbs/"+s.replaceFirst("gfx/terobjs/herbs/", ""));
        		}
        		invitems = BotUtils.getInventoryItems(BotUtils.playerInventory(), forageableitems);
        		for(WItem wi : invitems) {
        				BotUtils.transferItem(wi);
        				// We shall have some delay between moving items, just in case
        				BotUtils.sleep(10);
        			}
        		BotUtils.sleep(1000);
        		if(!BotUtils.getInventoryItems(BotUtils.playerInventory(), forageableitems).isEmpty()) {
        			gobs.remove(0);
        		} else
        			repeat = false;
            	}
	            // Logout, wait for forageables to respawn and then log on
            	BotUtils.logoutChar();
				BotUtils.sleep(1000);
	            while(BotUtils.gui().prog >= 0) {
	            	BotUtils.sleep(100);
	            }
	            // Wait for 20 minutes so forageables respawn
            	BotUtils.sleep(1200000);
            	BotUtils.chooseChar(charname);
            	BotUtils.sleep(5000);
            	BotUtils.tpHF();
		}
            	
				BotUtils.sysMsg("Foraging bot finished", Color.WHITE);
				window.destroy();
		}
		
	public void pick(Gob gob) {
		FlowerMenu menu = ui.root.findchild(FlowerMenu.class);
        if (menu != null) {
            for (FlowerMenu.Petal opt : menu.opts) {
                if (opt.name.equals("Pick")) {
                    menu.choose(opt);
                    menu.destroy();
        			while(BotUtils.findObjectById(gob.id) != null) {
        				BotUtils.sleep(100);
        			}
                }
            }
        }
	}
	
	public void loadConfig() {
		try {
			BufferedReader br = new BufferedReader(Files.newBufferedReader(Paths.get("scripts/"+configname+".config")));
			// Probably there is a better way but atleast this works
			ArrayList<String> foo = new ArrayList<String>();
			while(br.ready()) {
				String line = br.readLine();
				foo.add(line);
			}
			forageables = new String[foo.size()];
			forageables = foo.toArray(forageables);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void loadRoute() {
		try {
			BufferedReader br = new BufferedReader(Files.newBufferedReader(Paths.get("scripts/"+coordfilename+".pbot")));
			while(br.ready()) {
				int x = 0;
				int y = 0;
				String line = br.readLine();
				String[] foo = line.split(" ");
				for(int i = 0; i < foo.length; i++) {
					if(i==0) {
						x = Integer.parseInt(foo[0]);
					} else if(i==1)
						y = Integer.parseInt(foo[1]);
					}
				route.add(new Coord(x, y));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int getX() {
		return (int)BotUtils.player().rc.x;
	}
	
	public int getY() {
		return (int)BotUtils.player().rc.y;
	}
	
	public boolean checkAnimals() {
		// Return true if foraging code must start looping from the beginning, false if it can just continue
		if (!animalcheck)
			return false;
		if(BotUtils.findObjectByNames(150, dangerousanimals)!=null){
			BotUtils.gui().map.canceltasks();
			System.out.println("Detected an dangeous animal. It may attack at any time. So we must deal with it. (logging out)");
			BotUtils.logoutChar();
			BotUtils.sleep(1000);
            while(BotUtils.gui().prog >= 0) {
            	BotUtils.sleep(100);
            }
			BotUtils.sleep(600000);
			BotUtils.chooseChar(charname);
			BotUtils.sleep(5000);
			BotUtils.tpHF();
			BotUtils.sleep(5000);
			return true;
		} else
			return false;
	}
		});
		
	private class StatusWindow extends Window {
			public StatusWindow() {
	            super(Coord.z, "Forager");
	            setLocal(true);
	            int y = 0;
            add(new TextEntry(120, "Enter coord filename") {
                {dshow = true;}

                public void activate(String text) {
                    coordfilename = text;
                }
            }, new Coord(0, y));
            y += 25;
            add(new TextEntry(120, "Enter config filename") {
                {dshow = true;}
                public void activate(String text) {
                    configname = text;
                }
            }, new Coord(0, y));
            y += 25;
            add(new Button(120, "Start") {
                public void click() {
                	gameui().msg("Started foraging bot", Color.WHITE);
                	start = true;
                }
            }, new Coord(0, y));
            y += 35;
            add(new Button(120, "Cancel") {
                public void click() {
                	gameui().msg("Cancelled foraging bot", Color.WHITE);
                	window.destroy();
                	t.stop();
                }
            }, new Coord(0, y));
            y += 35;
            add(new CheckBox("Avoid Animals (Logout)") {
                {
                    a = animalcheck;
                }

                public void set(boolean val) {
                	animalcheck = val;
                    a = val;
                }
            }, new Coord(0, y));
            pack();
        }
        public void wdgmsg(Widget sender, String msg, Object... args) {
            if (sender == this && msg.equals("close")) {
            	gameui().msg("Foraging bot cancelled", Color.WHITE);
            	window.destroy();
            }
            super.wdgmsg(sender, msg, args);
        }
	}
}