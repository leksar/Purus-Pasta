package haven;


import java.util.*;

public class InventoryBelt extends Widget implements DTarget {
    private static final Tex invsq = Resource.loadtex("gfx/hud/invsq-opaque");
    private static final Coord sqsz = new Coord(36, 33);
    public boolean dropul = true;
    public Coord isz;
    Map<GItem, WItem> wmap = new HashMap<GItem, WItem>();

    public void draw(GOut g) {
        Coord c = new Coord();
        for (c.x = 0; c.x < isz.x; c.x++) {
            g.image(invsq, c.mul(sqsz));
        }
        super.draw(g);
    }

    public InventoryBelt(Coord sz) {
        super(invsq.sz().add(new Coord(-1 + 3, -1)).mul(sz).add(new Coord(1, 1)));
        isz = sz;
    }

    @Override
    public boolean mousewheel(Coord c, int amount) {
        return false;
    }

    @Override
    public void addchild(Widget child, Object... args) {
        add(child);
        Coord c = (Coord) args[0];
        if (child instanceof GItem) {
            GItem i = (GItem) child;
            wmap.put(i, add(new WItem(i), c.mul(sqsz).add(1, 1)));
        }
    }

    @Override
    public void cdestroy(Widget w) {
        super.cdestroy(w);
        if (w instanceof GItem) {
            GItem i = (GItem) w;
            ui.destroy(wmap.remove(i));
        }
    }

    @Override
    public boolean drop(Coord cc, Coord ul) {
        Coord dc = dropul ? ul.add(sqsz.div(2)).div(sqsz) : cc.div(sqsz);
        wdgmsg("drop", dc);
        return(true);
    }

    @Override
    public boolean iteminteract(Coord cc, Coord ul) {
        return (false);
    }

    @Override
    public void uimsg(String msg, Object... args) {
        if (msg == "sz") {
            isz = (Coord) args[0];
            resize(invsq.sz().add(new Coord(-1, -1)).mul(isz).add(new Coord(1, 1)));
        } else if(msg == "mode") {
            dropul = (((Integer)args[0]) == 0);
        } else {
            super.uimsg(msg, args);
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if(!msg.endsWith("-identical"))
            super.wdgmsg(sender, msg, args);
    }
}
