package purus;

import java.awt.Color;

import haven.GItem;
import haven.Gob;
import haven.Inventory;
import haven.UI;
import haven.Widget;
import haven.FlowerMenu.Petal;

public class FillOven {

	private final UI ui;
    private haven.Widget w;
    private haven.Inventory i;
    private int branchesfed;
    public Petal[] opts;
    private Widget window; 
	BotUtils BotUtils;

	public FillOven (UI ui, Widget w, Inventory i) {
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
		/*
		 * 1. Etsi lahin uuni
		 * 2. Etsi oksa invista ja ota se kateen
		 * 3. Klikkaa uunia
		 * 4. Toista oksan otto ja uunin klikkaus niin etta 4 oksaa sisalla
		 * 5. Valmis!!
		 */
		Gob gob = BotUtils.findObjectByNames(50, "gfx/terobjs/oven");
		
		branchesfed = 0;
		while(branchesfed != 4) {
		GItem item = BotUtils.getItemAtHand();
        if (item == null) {
       	 Inventory inv = BotUtils.playerInventory();
            for (Widget w = inv.child; w != null; w = w.next) {
                if (w instanceof GItem && isBranch((GItem) w)) {
                    item = (GItem)w;
                    break;
                	}
                }
                	}  else if(!isBranch(item)) {
        	BotUtils.sysMsg("Item in hand isn't a branch", Color.WHITE);
        	BotUtils.sysMsg("Oven Filler Cancelled", Color.WHITE);
            t.stop();
            return;
        } 	           
        if (item != null) {
            BotUtils.takeItem(item);
        } else {
        	BotUtils.sysMsg("Couldn't find any branches", Color.WHITE);
        	BotUtils.sysMsg("Oven Filler Cancelled", Color.WHITE);
            t.stop();
            return;
        }
		GItem item2 = BotUtils.getItemAtHand();
		//String resiname = itemName(item2);
		while(!isBranch((GItem) w)) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			item = BotUtils.getItemAtHand();
			//resiname = itemName(item);
		}
		BotUtils.doClick(gob, 3, 1);
		while(isBranch((GItem) w)) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			GItem item3 = BotUtils.getItemAtHand();
			//resiname = itemName(item3);
		}
		branchesfed ++;
		}
		BotUtils.sysMsg("Oven Filler finished", Color.WHITE);
		t.stop();
		}
	final String[] branch = {"gfx/invobjs/branch"};
    protected boolean isBranch(final GItem item) {
        String resName = item.resname();
        if (resName != null && !resName.isEmpty()) {
            for (String food : branch)
                if (resName.contains(food))
                    return true;
       }
        return false;
    }
    public String itemName(GItem item) {
    return item.resname();
    }
	});
}