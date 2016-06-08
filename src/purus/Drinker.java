package purus;

import haven.Inventory;
import haven.UI;
import haven.Widget;

public class Drinker {
	BotUtils BotUtils;
	// This class is horrible
	
	public Drinker (UI ui, Widget w, Inventory i) {
		BotUtils = new BotUtils(ui, w, i);
	}
	
	public void Run () {
	t.start();	
	}
	Thread t = new Thread(new Runnable() {
	public void run()  {
		BotUtils.drink();
	}
	});

}
