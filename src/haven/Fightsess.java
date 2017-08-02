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

import java.awt.*;
import java.util.*;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Fightsess extends Widget {
    public static final Tex cdframe = Resource.loadtex("gfx/hud/combat/cool");
    public static final Tex actframe = Buff.frame;
    public static final Coord actframeo = Buff.imgoff;
    public static final Tex indframe = Resource.loadtex("gfx/hud/combat/indframe");
    public static final Coord indframeo = (indframe.sz().sub(32, 32)).div(2);
    public static final Tex useframe = Resource.loadtex("gfx/hud/combat/lastframe");
    public static final Coord useframeo = (useframe.sz().sub(32, 32)).div(2);
    public static final int actpitch = 50;
    public final Indir<Resource>[] actions;
    public final boolean[] dyn;
    public int use = -1;
    public Coord pcc;
    public int pho;
    private final Fightview fv;
    private final Tex[] keystex = new Tex[10];
    private final Tex[] keysftex = new Tex[10];

    private static final Map<String, Color> openings = new HashMap<String, Color>(4) {{
        put("paginae/atk/dizzy",new Color(8, 103, 136));
        put("paginae/atk/offbalance", new Color(8, 103, 1));
        put("paginae/atk/cornered", new Color(221, 28, 26));
        put("paginae/atk/reeling", new Color(203, 168, 6));
    }};
    private Coord simpleOpeningSz = new Coord(32, 32);

    @RName("fsess")
    public static class $_ implements Factory {
        public Widget create(Widget parent, Object[] args) {
            int nact = (Integer) args[0];
            return (new Fightsess(nact, parent.getparent(GameUI.class).fv));
        }
    }

    @SuppressWarnings("unchecked")
    public Fightsess(int nact, Fightview fv) {
        this.fv = fv;
        pho = -40;
        this.actions = (Indir<Resource>[]) new Indir[nact];
        this.dyn = new boolean[nact];

        for(int i = 0; i < 10; i++) {
            keystex[i] = Text.renderstroked(FightWnd.keys[i], Color.WHITE, Color.BLACK, Text.num12boldFnd).tex();
            if (i < 5)
                keysftex[i] = keystex[i];
            else
                keysftex[i] = Text.renderstroked(FightWnd.keysf[i - 5], Color.WHITE, Color.BLACK, Text.num12boldFnd).tex();
        }
    }

    public void presize() {
        resize(parent.sz);
        pcc = sz.div(2);
    }

    protected void added() {
        presize();
    }

    private void updatepos() {
        MapView map;
        Gob pl;
        if (((map = getparent(GameUI.class).map) == null) || ((pl = map.player()) == null) || (pl.sc == null))
            return;
        pcc = pl.sc;
        pho = (int) (pl.sczu.mul(20f).y) - 20;
    }

    private static final Resource tgtfx = Resource.local().loadwait("gfx/hud/combat/trgtarw");
    private final Map<Pair<Long, Resource>, Sprite> cfx = new CacheMap<Pair<Long, Resource>, Sprite>();
    private final Collection<Sprite> curfx = new ArrayList<Sprite>();

    private void fxon(long gobid, Resource fx) {
        MapView map = getparent(GameUI.class).map;
        Gob gob = ui.sess.glob.oc.getgob(gobid);
        if((map == null) || (gob == null))
            return;
        Pair<Long, Resource> id = new Pair<Long, Resource>(gobid, fx);
        Sprite spr = cfx.get(id);
        if(spr == null)
            cfx.put(id, spr = Sprite.create(null, fx, Message.nil));
        map.drawadd(gob.loc.apply(spr));
        curfx.add(spr);
    }

    public void tick(double dt) {
        for(Sprite spr : curfx)
            spr.tick((int)(dt * 1000));
        curfx.clear();
    }

    private static final Text.Furnace ipf = new PUtils.BlurFurn(new Text.Foundry(Text.serif, 18, new Color(128, 128, 255)).aa(true), 1, 1, new Color(48, 48, 96));
    private final Text.UText<?> ip = new Text.UText<Integer>(ipf) {
        public String text(Integer v) {
            return (Config.altfightui ? v.toString() : "IP: " + v);
        }

        public Integer value() {
            return (fv.current.ip);
        }
    };
    private final Text.UText<?> oip = new Text.UText<Integer>(ipf) {
        public String text(Integer v) {
            return (Config.altfightui ? v.toString() : "IP: " + v);
        }

        public Integer value() {
            return (fv.current.oip);
        }
    };

    private static Coord actc(int i) {
        int rl = 5;

        int row = i / rl;
        if (Config.combatkeys == 1)
            row ^= 1;

        return(new Coord((actpitch * (i % rl)) - (((rl - 1) * actpitch) / 2), 125 + (row * actpitch)));
    }

    private static final Coord cmc = new Coord(0, 67);
    private static final Coord usec1 = new Coord(-65, 67);
    private static final Coord usec2 = new Coord(65, 67);
    private Indir<Resource> lastact1 = null, lastact2 = null;
    private Text lastacttip1 = null, lastacttip2 = null;

    public void draw(GOut g) {
        updatepos();
        double now = System.currentTimeMillis() / 1000.0;

        GameUI gui = gameui();
        int gcx = gui.sz.x / 2;

        for (Buff buff : fv.buffs.children(Buff.class)) {
            Coord bc = Config.altfightui ? new Coord(gcx - buff.c.x - Buff.cframe.sz().x - 80, 180) : pcc.add(-buff.c.x - Buff.cframe.sz().x - 20, buff.c.y + pho - Buff.cframe.sz().y);
            drawOpening(g, buff, bc);
        }

        if (fv.current != null) {
            for (Buff buff : fv.current.buffs.children(Buff.class)) {
                Coord bc = Config.altfightui ? new Coord(gcx + buff.c.x + 80, 180) : pcc.add(buff.c.x + 20, buff.c.y + pho - Buff.cframe.sz().y);
                drawOpening(g, buff, bc);
            }

            g.aimage(ip.get().tex(), Config.altfightui ? new Coord(gcx - 45, 200) : pcc.add(-75, 0), 1, 0.5);
            g.aimage(oip.get().tex(), Config.altfightui ? new Coord(gcx + 45, 200) : pcc.add(75, 0), 0, 0.5);

            if (fv.lsrel.size() > 1)
                fxon(fv.current.gobid, tgtfx);
        }

        {
            Coord cdc = Config.altfightui ? new Coord(gcx, 200) : pcc.add(cmc);
            if (now < fv.atkct) {
                double cd = fv.atkct - now;
                double a = (now - fv.atkcs) / (fv.atkct - fv.atkcs);
                g.chcolor(255, 0, 128, 224);
                g.fellipse(cdc, Config.altfightui ? new Coord(24, 24) : new Coord(22, 22), Math.PI / 2 - (Math.PI * 2 * Math.min(1.0 - a, 1.0)), Math.PI / 2);
                g.chcolor();
                if (Config.showcooldown)
                    g.atextstroked(Utils.fmt1DecPlace(cd), cdc, 0.5, 0.5, Color.WHITE, Color.BLACK, Text.num11Fnd);
            }
            g.image(cdframe, Config.altfightui ? new Coord(gameui().sz.x / 2, 200).sub(cdframe.sz().div(2)) : cdc.sub(cdframe.sz().div(2)));
        }

        try {
            Indir<Resource> lastact = fv.lastact;
            if (lastact != this.lastact1) {
                this.lastact1 = lastact;
                this.lastacttip1 = null;
            }
            long lastuse = fv.lastuse;
            if (lastact != null) {
                Tex ut = lastact.get().layer(Resource.imgc).tex();
                Coord useul = Config.altfightui ? new Coord(gcx - 69, 120) : pcc.add(usec1).sub(ut.sz().div(2));
                g.image(ut, useul);
                g.image(useframe, useul.sub(useframeo));
                double a = now - (lastuse / 1000.0);
                if (a < 1) {
                    Coord off = new Coord((int) (a * ut.sz().x / 2), (int) (a * ut.sz().y / 2));
                    g.chcolor(255, 255, 255, (int) (255 * (1 - a)));
                    g.image(ut, useul.sub(off), ut.sz().add(off.mul(2)));
                    g.chcolor();
                }
            }
        } catch (Loading l) {
        }

        if (fv.current != null) {
            try {
                Indir<Resource> lastact = fv.current.lastact;
                if (lastact != this.lastact2) {
                    this.lastact2 = lastact;
                    this.lastacttip2 = null;
                }
                long lastuse = fv.current.lastuse;
                if (lastact != null) {
                    Tex ut = lastact.get().layer(Resource.imgc).tex();
                    Coord useul = Config.altfightui ? new Coord(gcx + 69 - ut.sz().x, 120) : pcc.add(usec2).sub(ut.sz().div(2));
                    g.image(ut, useul);
                    g.image(useframe, useul.sub(useframeo));
                    double a = now - (lastuse / 1000.0);
                    if (a < 1) {
                        Coord off = new Coord((int) (a * ut.sz().x / 2), (int) (a * ut.sz().y / 2));
                        g.chcolor(255, 255, 255, (int) (255 * (1 - a)));
                        g.image(ut, useul.sub(off), ut.sz().add(off.mul(2)));
                        g.chcolor();
                    }
                }
            } catch (Loading l) {
            }
        }

        for (int i = 0; i < actions.length; i++) {
            Coord ca = Config.altfightui ? new Coord(gcx - 18, gui.sz.y - 250).add(actc(i)).add(16, 16) : pcc.add(actc(i));
            Indir<Resource> act = actions[i];
            try {
                if (act != null) {
                    Tex img = act.get().layer(Resource.imgc).tex();
                    ca = ca.sub(img.sz().div(2));
                    g.image(img, ca);
                    if (i == use) {
                        g.image(indframe, ca.sub(indframeo));
                    } else {
                        g.image(actframe, ca.sub(actframeo));
                    }

                    if (Config.combshowkeys) {
                        Tex key = Config.combatkeys == 0 ? keystex[i] : keysftex[i];
                        g.image(key, ca.sub(indframeo).add(indframe.sz().x / 2 - key.sz().x / 2, indframe.sz().y - 6));
                    }
                }
            } catch (Loading l) {
            }
            ca.x += actpitch;
        	}
    }

    private void drawOpening(GOut g, Buff buff, Coord bc) {
        if (Config.combaltopenings) {
            try {
                Resource res = buff.res.get();
                Color clr = openings.get(res.name);
                if (clr == null) {
                    buff.draw(g.reclip(bc, buff.sz));
                    return;
                }

                if (buff.ameter >= 0) {
                    g.image(buff.cframe, bc);
                    g.chcolor(Color.BLACK);
                    g.frect(bc.add(buff.ameteroff), buff.ametersz);
                    g.chcolor(Color.WHITE);
                    g.frect(bc.add(buff.ameteroff), new Coord((buff.ameter * buff.ametersz.x) / 100, buff.ametersz.y));
                } else {
                    g.image(buff.frame, bc);
                }

                bc.x += 3;
                bc.y += 3;

                g.chcolor(clr);
                g.frect(bc, simpleOpeningSz);

                g.chcolor(Color.WHITE);
                if (buff.atex == null)
                    buff.atex = Text.renderstroked(buff.ameter + "", Color.WHITE, Color.BLACK, Text.num12boldFnd).tex();
                Tex atex = buff.atex;
                bc.x = bc.x + simpleOpeningSz.x / 2 - atex.sz().x / 2;
                bc.y = bc.y + simpleOpeningSz.y / 2 - atex.sz().y / 2;
                g.image(atex, bc);
                g.chcolor();
            } catch (Loading l) {
            }
        } else {
            buff.draw(g.reclip(bc, buff.sz));
        }
    }

    private Widget prevtt = null;
    private Text acttip = null;
    public static final String[] keytips = {"1", "2", "3", "4", "5", "Shift+1", "Shift+2", "Shift+3", "Shift+4", "Shift+5"};
    public Object tooltip(Coord c, Widget prev) {
        int cx = gameui().sz.x / 2;

        for (Buff buff : fv.buffs.children(Buff.class)) {
            Coord dc = Config.altfightui ? new Coord(cx - buff.c.x - Buff.cframe.sz().x - 80, 180) : pcc.add(-buff.c.x - Buff.cframe.sz().x - 20, buff.c.y + pho - Buff.cframe.sz().y);
            if (c.isect(dc, buff.sz)) {
                Object ret = buff.tooltip(c.sub(dc), prevtt);
                if (ret != null) {
                    prevtt = buff;
                    return (ret);
                }
            }
        }

        if (fv.current != null) {
            for (Buff buff : fv.current.buffs.children(Buff.class)) {
                Coord dc = Config.altfightui ? new Coord(cx + buff.c.x + 80, 180) : pcc.add(buff.c.x + 20, buff.c.y + pho - Buff.cframe.sz().y);
                if (c.isect(dc, buff.sz)) {
                    Object ret = buff.tooltip(c.sub(dc), prevtt);
                    if (ret != null) {
                        prevtt = buff;
                        return (ret);
                    }
                }
            }
        }

        for (int i = 0; i < actions.length; i++) {
            Coord ca = Config.altfightui ? new Coord(cx - 18, gameui().sz.y - 250).add(actc(i)).add(16, 16) : pcc.add(actc(i));
            Indir<Resource> act = actions[i];
            try {
                if (act != null) {
                    Tex img = act.get().layer(Resource.imgc).tex();
                    ca = ca.sub(img.sz().div(2));
                    if (c.isect(ca, img.sz())) {
                        if (dyn[i])
                            return ("Combat discovery");
                        String tip = act.get().layer(Resource.tooltip).t + " ($b{$col[255,128,0]{" + keytips[i] + "}})";
                        if((acttip == null) || !acttip.text.equals(tip))
                            acttip = RichText.render(tip, -1);
                        return(acttip);
                    }
                }
            } catch (Loading l) {
            }
            ca.x += actpitch;
        }

        try {
            Indir<Resource> lastact = this.lastact1;
            if(lastact != null) {
                Coord usesz = lastact.get().layer(Resource.imgc).sz;
                Coord lac = Config.altfightui ? new Coord(cx - 69, 120).add(usesz.div(2)) : pcc.add(usec1);
                if(c.isect(lac.sub(usesz.div(2)), usesz)) {
                    if(lastacttip1 == null)
                        lastacttip1 = Text.render(lastact.get().layer(Resource.tooltip).t);
                    return(lastacttip1);
                }
            }
        } catch(Loading l) {}
        try {
            Indir<Resource> lastact = this.lastact2;
            if(lastact != null) {
                Coord usesz = lastact.get().layer(Resource.imgc).sz;
                Coord lac = Config.altfightui ? new Coord(cx + 69 - usesz.x, 120).add(usesz.div(2)) : pcc.add(usec2);
                if(c.isect(lac.sub(usesz.div(2)), usesz)) {
                    if(lastacttip2 == null)
                        lastacttip2 = Text.render(lastact.get().layer(Resource.tooltip).t);
                    return(lastacttip2);
                }
            }
        } catch(Loading l) {}
        return (null);
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "act") {
            int n = (Integer) args[0];
            if (args.length > 1) {
                Indir<Resource> res = ui.sess.getres((Integer) args[1]);
                actions[n] = res;
                dyn[n] = ((Integer) args[2]) != 0;
            } else {
                actions[n] = null;
            }
        } else if (msg == "use") {
            this.use = (Integer) args[0];
        } else if (msg == "used") {
        } else {
            super.uimsg(msg, args);
        }
    }

    public boolean globtype(char key, KeyEvent ev) {
        if (ev.getKeyCode() == KeyEvent.VK_TAB && ev.isControlDown()) {
            Fightview.Relation cur = fv.current;
            if (cur != null) {
                fv.lsrel.remove(cur);
                fv.lsrel.addLast(cur);
            }
            fv.wdgmsg("bump", (int) fv.lsrel.get(0).gobid);
            return (true);
        }

        if (Config.combatkeys == 0) {
            if ((key == 0) && (ev.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK | KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) == 0) {
                int n = -1;
                switch(ev.getKeyCode()) {
                    case KeyEvent.VK_1: n = 0; break;
                    case KeyEvent.VK_2: n = 1; break;
                    case KeyEvent.VK_3: n = 2; break;
                    case KeyEvent.VK_4: n = 3; break;
                    case KeyEvent.VK_5: n = 4; break;
                }
                if((n >= 0) && ((ev.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0))
                    n += 5;
                if((n >= 0) && (n < actions.length)) {
                    wdgmsg("use", n);
                    return(true);
                }
            }
        } else { // F1-F5
            if (key == 0) {
                int n = -1;
                switch(ev.getKeyCode()) {
                    case KeyEvent.VK_1: n = 0; break;
                    case KeyEvent.VK_2: n = 1; break;
                    case KeyEvent.VK_3: n = 2; break;
                    case KeyEvent.VK_4: n = 3; break;
                    case KeyEvent.VK_5: n = 4; break;
                    case KeyEvent.VK_F1: n = 5; break;
                    case KeyEvent.VK_F2: n = 6; break;
                    case KeyEvent.VK_F3: n = 7; break;
                    case KeyEvent.VK_F4: n = 8; break;
                    case KeyEvent.VK_F5: n = 9; break;
                }
                if((n >= 0) && (n < actions.length)) {
                    wdgmsg("use", n);
                    return(true);
                }
            }
        }

        return(super.globtype(key, ev));
    }
    private static class DefBar {
        private static final Coord bsz = new Coord(20, 10);

        private final Map<AttackType, List<Buff>> defs = new HashMap<AttackType, List<Buff>>();

        public void addBuff(Buff buff) {
            Resource.Image img = buff.getImage();
            if (img != null) {
                int attackType = CombatHelper.getAttackType(img.img);
                for (AttackType type : AttackType.All) {
                    if ((type.value & attackType) != 0) {
                        List<Buff> buffs = defs.get(type);
                        if (buffs == null) {
                            buffs = new ArrayList<Buff>();
                            defs.put(type, buffs);
                        }
                        buffs.add(buff);
                    }
                }
            }
        }

        public void draw(GOut g, Coord c) {
            int y = 15;
            for (AttackType type : defs.keySet()) {
                int x = 0;
                for (Buff buff : defs.get(type)) {
                    g.chcolor(type.color, 128);
                    g.frect(c.add(x, -y), new Coord((buff.ameter * bsz.x) / 100, bsz.y));
                    g.chcolor(Color.LIGHT_GRAY);
                    g.rect(c.add(x, -y), bsz);
                    g.chcolor();
                    x += bsz.x + 5;
                }
                y += bsz.y + 5;
            }
        }
    }
}
