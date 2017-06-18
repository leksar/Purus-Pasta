/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import static haven.Inventory.invsq;

import java.awt.Color;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import haven.res.ui.tt.Armor;

public class Equipory extends Widget implements DTarget {
    private static final Tex bg = Resource.loadtex("gfx/hud/equip/bg");
    private static final int rx = 34 + bg.sz().x;
    private static final int acx = 34 + bg.sz().x / 2;
    private static final Text.Foundry acf = new Text.Foundry(Text.sans, Config.fontsizeglobal).aa(true);
    private Tex armorclass = null;
    public static final Coord ecoords[] = {
            new Coord(0, 0),
            new Coord(rx, 0),
            new Coord(0, 33),
            new Coord(rx, 33),
            new Coord(0, 66),
            new Coord(rx, 66),
            new Coord(0, 99),
            new Coord(rx, 99),
            new Coord(0, 132),
            new Coord(rx, 132),
            new Coord(0, 165),
            new Coord(rx, 165),
            new Coord(0, 198),
            new Coord(rx, 198),
            new Coord(0, 231),
            new Coord(rx, 231),
            new Coord(34, 0),
    };
    static Coord isz;

    static {
        isz = new Coord();
        for (Coord ec : ecoords) {
            if (ec.x + invsq.sz().x > isz.x)
                isz.x = ec.x + invsq.sz().x;
            if (ec.y + invsq.sz().y > isz.y)
                isz.y = ec.y + invsq.sz().y;
        }
    }

    Map<GItem, WItem[]> wmap = new HashMap<GItem, WItem[]>();
    private final Avaview ava;
    public WItem[] quickslots = new WItem[ecoords.length];
    private List<GItem> checkForDrop = new LinkedList<GItem>();

    @RName("epry")
    public static class $_ implements Factory {
        public Widget create(Widget parent, Object[] args) {
            long gobid;
            if(args.length < 1)
                gobid = parent.getparent(GameUI.class).plid;
            else if(args[0] == null)
                gobid = -1;
            else
                gobid = Utils.uint32((Integer)args[0]);
            return(new Equipory(gobid));
        }
    }

    public Equipory(long gobid) {
        super(isz);
        ava = add(new Avaview(bg.sz(), gobid, "equcam") {
            public boolean mousedown(Coord c, int button) {
                return (false);
            }

            public void draw(GOut g) {
                g.image(bg, Coord.z);
                super.draw(g);
            }

            Outlines outlines = new Outlines(true);

            protected void setup(RenderList rl) {
                super.setup(rl);
                rl.add(outlines, null);
            }

            protected java.awt.Color clearcolor() {
                return (null);
            }
        }, new Coord(34, 0));
        ava.color = null;
    }
    
    @Override
    public void tick(double dt) throws InterruptedException {
	super.tick(dt);
	try {
	    if (!checkForDrop.isEmpty()) {
		GItem g = checkForDrop.get(0);
		if (g.resname().equals("gfx/invobjs/leech")) {
			 g.drop = true; 
			//ui.gui.map.wdgmsg("drop", Coord.z);
		}
		checkForDrop.remove(0);
	    }
	} catch (Resource.Loading ignore) {
	}
    }

    public void addchild(Widget child, Object... args) {
        if (child instanceof GItem) {
            add(child);
            GItem g = (GItem) child;
            WItem[] v = new WItem[args.length];
            for (int i = 0; i < args.length; i++) {
                int ep = (Integer) args[i];
                v[i] = quickslots[ep] = add(new WItem(g), ecoords[ep].add(1, 1));
            }
            wmap.put(g, v);
	    if(Config.dropleeches){
		checkForDrop.add(g);
	    }

            if (armorclass != null) {
                armorclass.dispose();
                armorclass = null;
            }
        } else {
            super.addchild(child, args);
        }
    }

    public void cdestroy(Widget w) {
        super.cdestroy(w);
        if (w instanceof GItem) {
            GItem i = (GItem) w;
            for (WItem v : wmap.remove(i)) {
                ui.destroy(v);
                for (int qsi = 0; qsi < ecoords.length; qsi++) {
                    if (quickslots[qsi] == v) {
                        quickslots[qsi] = null;
                        break;
                    }
                }
            }
            if (armorclass != null) {
                armorclass.dispose();
                armorclass = null;
            }
        }
    }

    public void uimsg(String msg, Object... args) {
        if(msg == "pop") {
            ava.avadesc = Composited.Desc.decode(ui.sess, args);
        } else {
            super.uimsg(msg, args);
        }
    }

    public int epat(Coord c) {
        for (int i = 0; i < ecoords.length; i++) {
            if (c.isect(ecoords[i], invsq.sz()))
                return (i);
        }
        return (-1);
    }

    public boolean drop(Coord cc, Coord ul) {
        wdgmsg("drop", epat(cc));
        return (true);
    }

    public void drawslots(GOut g) {
        for(int i = 0; i < 16; i++)
            g.image(invsq, ecoords[i]);
    }

    public void draw(GOut g) {
        drawslots(g);
        super.draw(g);

        if (armorclass == null) {
            int h = 0, s = 0, t = 0;
            try {
                for (int i = 0; i < quickslots.length; i++) {
                    WItem itm = quickslots[i];
                    if (itm != null) {
                        for (ItemInfo info : itm.item.info()) {
                            if (info instanceof Armor) {
                                h += ((Armor)info).hard;
                                s += ((Armor)info).soft;
                                t = h + s;
                                break;
                            }
                        }
                    }
                }
                armorclass = Text.render(Resource.getLocString(Resource.BUNDLE_LABEL, "Armor Class: ") + h + "/" + s + " (" + t + ")", Color.BLACK, acf).tex();
            } catch (Exception e) { // fail silently
            }
        }
        if (armorclass != null)
            g.image(armorclass, new Coord(acx - armorclass.sz().x / 2, bg.sz().y - armorclass.sz().y));
    }

    public boolean iteminteract(Coord cc, Coord ul) {
        return (false);
    }
}
