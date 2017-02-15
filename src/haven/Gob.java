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

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import haven.resutil.BPRadSprite;
import purus.CustomHitbox;
import java.awt.*;
import java.util.*;
import java.util.concurrent.Callable;

public class Gob implements Sprite.Owner, Skeleton.ModOwner, Rendered {
    public Coord2d rc;
    public Coord sc;
    public Coord3f sczu;
    public double a;
    public boolean virtual = false;
    int clprio = 0;
    public long id;
    public int frame;
    public final Glob glob;
    Map<Class<? extends GAttrib>, GAttrib> attr = new HashMap<Class<? extends GAttrib>, GAttrib>();
    public Collection<Overlay> ols = new LinkedList<Overlay>() {
        public boolean add(Overlay item) {
	        /* XXX: Remove me once local code is changed to use addol(). */
            if(glob.oc.getgob(id) != null) {
                // FIXME: extend ols with a method for adding sprites without triggering changed.
                if (item.id != Sprite.GROWTH_STAGE_ID && item != animalradius)
                    glob.oc.changed(Gob.this);
            }
            return(super.add(item));
        }
    };

    private final Collection<ResAttr.Cell<?>> rdata = new LinkedList<ResAttr.Cell<?>>();
    private final Collection<ResAttr.Load> lrdata = new LinkedList<ResAttr.Load>();
    private int cropstgmaxval = 0;
    private Overlay gobpath = null;
    private Overlay bowvector = null;
    private final Map<Gob, Gob.Overlay> playerhighlight = new HashMap<Gob, Gob.Overlay>();
    private static final Material.Colors dframeEmpty = new Material.Colors(new Color(87, 204, 73, 255));
    private static final Material.Colors dframeDone = new Material.Colors(new Color(209, 42, 42, 255));
    private static final Gob.Overlay animalradius = new Gob.Overlay(new BPRadSprite(100.0F, -10.0F, BPRadSprite.smatDanger));
    private static final Set<String> additionalmobs = new HashSet<>(Arrays.asList(
            "gfx/kritter/boar/boar", "gfx/kritter/badger/badger", "gfx/kritter/wolverine/wolverine"));
    // knocked will be null if pose update request hasn't been received yet
    public Boolean knocked = null;
    public Type type = null;

    public enum Type {
        OTHER(0), DFRAME(1), TREE(2), BUSH(3), BOULDER(4), PLAYER(5), SIEGE_MACHINE(6),
        PLANT(16), MULTISTAGE_PLANT(17),
        MOB(32), MAMMOTH(33), BEAR(34), LYNX(35), TROLL(38), WALRUS(39),
        WOODEN_SUPPORT(64), STONE_SUPPORT(65), TROUGH(66), BEEHIVE(67);

        public final int value;

        Type(int value) {
            this.value = value;
        }

        boolean is(Type g) {
            return (value & g.value) != 0;
        }
    }

    public static class Overlay implements Rendered {
        public Indir<Resource> res;
        public MessageBuf sdt;
        public Sprite spr;
        public int id;
        public boolean delign = false;

        public Overlay(int id, Indir<Resource> res, Message sdt) {
            this.id = id;
            this.res = res;
            this.sdt = new MessageBuf(sdt);
            spr = null;
        }

        public Overlay(Sprite spr) {
            this.id = -1;
            this.res = null;
            this.sdt = null;
            this.spr = spr;
        }

        public Overlay(int id, Sprite spr) {
            this.id = id;
            this.res = null;
            this.sdt = null;
            this.spr = spr;
        }

        public static interface CDel {
            public void delete();
        }

        public static interface CUpd {
            public void update(Message sdt);
        }

        public static interface SetupMod {
            public void setupgob(GLState.Buffer buf);

            public void setupmain(RenderList rl);
        }

        public void draw(GOut g) {
        }

        public boolean setup(RenderList rl) {
            if (spr != null)
                rl.add(spr, null);
            return (false);
        }

        public Object staticp() {
            return((spr == null)?null:spr.staticp());
        }
    }

    /* XXX: This whole thing didn't turn out quite as nice as I had
     * hoped, but hopefully it can at least serve as a source of
     * inspiration to redo attributes properly in the future. There
     * have already long been arguments for remaking GAttribs as
     * well. */
    public static class ResAttr {
        public boolean update(Message dat) {
            return (false);
        }

        public void dispose() {
        }

        public static class Cell<T extends ResAttr> {
            final Class<T> clsid;
            Indir<Resource> resid = null;
            MessageBuf odat;
            public T attr = null;

            public Cell(Class<T> clsid) {
                this.clsid = clsid;
            }

            void set(ResAttr attr) {
                if (this.attr != null)
                    this.attr.dispose();
                this.attr = clsid.cast(attr);
            }
        }

        private static class Load {
            final Indir<Resource> resid;
            final MessageBuf dat;

            Load(Indir<Resource> resid, Message dat) {
                this.resid = resid;
                this.dat = new MessageBuf(dat);
            }
        }

        @Resource.PublishedCode(name = "gattr", instancer = FactMaker.class)
        public static interface Factory {
            public ResAttr mkattr(Gob gob, Message dat);
        }

        public static class FactMaker implements Resource.PublishedCode.Instancer {
            public Factory make(Class<?> cl) throws InstantiationException, IllegalAccessException {
                if (Factory.class.isAssignableFrom(cl))
                    return (cl.asSubclass(Factory.class).newInstance());
                if (ResAttr.class.isAssignableFrom(cl)) {
                    try {
                        final java.lang.reflect.Constructor<? extends ResAttr> cons = cl.asSubclass(ResAttr.class).getConstructor(Gob.class, Message.class);
                        return (new Factory() {
                            public ResAttr mkattr(Gob gob, Message dat) {
                                return (Utils.construct(cons, gob, dat));
                            }
                        });
                    } catch (NoSuchMethodException e) {
                    }
                }
                return (null);
            }
        }
    }

    public static class Static {}
    public static class SemiStatic {}

    public Gob(Glob glob, Coord2d c, long id, int frame) {
        this.glob = glob;
        this.rc = c;
        this.id = id;
        this.frame = frame;
        loc.tick();
    }

    public Gob(Glob glob, Coord2d c) {
        this(glob, c, -1, 0);
    }

    public static interface ANotif<T extends GAttrib> {
        public void ch(T n);
    }

    public void ctick(int dt) {
        for (GAttrib a : attr.values())
            a.ctick(dt);
        for (Iterator<Overlay> i = ols.iterator(); i.hasNext(); ) {
            Overlay ol = i.next();
            if (ol.spr == null) {
                try {
                    ol.spr = Sprite.create(this, ol.res.get(), ol.sdt.clone());
                } catch (Loading e) {
                }
            } else {
                boolean done = ol.spr.tick(dt);
                if ((!ol.delign || (ol.spr instanceof Overlay.CDel)) && done)
                    i.remove();
            }
        }
        if (virtual && ols.isEmpty())
            glob.oc.remove(id);
    }

    /* Intended for local code. Server changes are handled via OCache. */
    public void addol(Overlay ol) {
        ols.add(ol);
    }
    public void addol(Sprite ol) {
        addol(new Overlay(ol));
    }

    public Overlay findol(int id) {
        for (Overlay ol : ols) {
            if (ol.id == id)
                return (ol);
        }
        return (null);
    }

    public void tick() {
        for (GAttrib a : attr.values())
            a.tick();
        loadrattr();
    }

    public void dispose() {
        for (GAttrib a : attr.values())
            a.dispose();
        for (ResAttr.Cell rd : rdata) {
            if (rd.attr != null)
                rd.attr.dispose();
        }
    }

    public void move(Coord2d c, double a) {
        Moving m = getattr(Moving.class);
        if (m != null)
            m.move(c);
        this.rc = c;
        this.a = a;
    }

    public Coord3f getc() {
        Moving m = getattr(Moving.class);
        Coord3f ret = (m != null) ? m.getc() : getrc();
        DrawOffset df = getattr(DrawOffset.class);
        if (df != null)
            ret = ret.add(df.off);
        return (ret);
    }

    public Coord3f getrc() {
        return(glob.map.getzp(rc));
    }
    
    public int getStage() {
        //
        Resource res = getres();
        if (res != null && res.name.startsWith("gfx/terobjs/plants") && !res.name.endsWith("trellis")) {
    	GAttrib rd = getattr(ResDrawable.class);
    	final int stage = ((ResDrawable) rd).sdt.peekrbuf(0);
        return stage;
        } else
        return 404;
        //
    }

    public double geta() {
        return a;
    }

    private Class<? extends GAttrib> attrclass(Class<? extends GAttrib> cl) {
        while (true) {
            Class<?> p = cl.getSuperclass();
            if (p == GAttrib.class)
                return (cl);
            cl = p.asSubclass(GAttrib.class);
        }
    }

    public void setattr(GAttrib a) {
        Class<? extends GAttrib> ac = attrclass(a.getClass());
        attr.put(ac, a);

        if (Config.showplayerpaths && gobpath == null && a instanceof LinMove) {
            Gob pl = glob.oc.getgob(MapView.plgob);
            if (pl != null) {
                Following follow = pl.getattr(Following.class);
                if (pl == this ||
                        (follow != null && follow.tgt() == this)) {
                    gobpath = new Overlay(new GobPath(this));
                    ols.add(gobpath);
                }
            }
        }
    }

    public <C extends GAttrib> C getattr(Class<C> c) {
        GAttrib attr = this.attr.get(attrclass(c));
        if (!c.isInstance(attr))
            return (null);
        return (c.cast(attr));
    }

    public void delattr(Class<? extends GAttrib> c) {
        attr.remove(attrclass(c));
        if (attrclass(c) == Moving.class && gobpath != null) {
            ols.remove(gobpath);
            gobpath = null;
            MapView.pllastcc = null;
        }
    }

    private Class<? extends ResAttr> rattrclass(Class<? extends ResAttr> cl) {
        while (true) {
            Class<?> p = cl.getSuperclass();
            if (p == ResAttr.class)
                return (cl);
            cl = p.asSubclass(ResAttr.class);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends ResAttr> ResAttr.Cell<T> getrattr(Class<T> c) {
        for (ResAttr.Cell<?> rd : rdata) {
            if (rd.clsid == c)
                return ((ResAttr.Cell<T>) rd);
        }
        ResAttr.Cell<T> rd = new ResAttr.Cell<T>(c);
        rdata.add(rd);
        return (rd);
    }

    public static <T extends ResAttr> ResAttr.Cell<T> getrattr(Object obj, Class<T> c) {
        if (!(obj instanceof Gob))
            return (new ResAttr.Cell<T>(c));
        return (((Gob) obj).getrattr(c));
    }

    private void loadrattr() {
        for (Iterator<ResAttr.Load> i = lrdata.iterator(); i.hasNext(); ) {
            ResAttr.Load rd = i.next();
            ResAttr attr;
            try {
                attr = rd.resid.get().getcode(ResAttr.Factory.class, true).mkattr(this, rd.dat.clone());
            } catch (Loading l) {
                continue;
            }
            ResAttr.Cell<?> rc = getrattr(rattrclass(attr.getClass()));
            if (rc.resid == null)
                rc.resid = rd.resid;
            else if (rc.resid != rd.resid)
                throw (new RuntimeException("Conflicting resattr resource IDs on " + rc.clsid + ": " + rc.resid + " -> " + rd.resid));
            rc.odat = rd.dat;
            rc.set(attr);
            i.remove();
        }
    }

    public void setrattr(Indir<Resource> resid, Message dat) {
        for (Iterator<ResAttr.Cell<?>> i = rdata.iterator(); i.hasNext(); ) {
            ResAttr.Cell<?> rd = i.next();
            if (rd.resid == resid) {
                if (dat.equals(rd.odat))
                    return;
                if ((rd.attr != null) && rd.attr.update(dat))
                    return;
                break;
            }
        }
        for (Iterator<ResAttr.Load> i = lrdata.iterator(); i.hasNext(); ) {
            ResAttr.Load rd = i.next();
            if (rd.resid == resid) {
                i.remove();
                break;
            }
        }
        lrdata.add(new ResAttr.Load(resid, dat));
        loadrattr();
    }

    public void delrattr(Indir<Resource> resid) {
        for (Iterator<ResAttr.Cell<?>> i = rdata.iterator(); i.hasNext(); ) {
            ResAttr.Cell<?> rd = i.next();
            if (rd.resid == resid) {
                i.remove();
                rd.attr.dispose();
                break;
            }
        }
        for (Iterator<ResAttr.Load> i = lrdata.iterator(); i.hasNext(); ) {
            ResAttr.Load rd = i.next();
            if (rd.resid == resid) {
                i.remove();
                break;
            }
        }
    }

    @Override
    public void draw(GOut g) {
    }

    private void determineType() {
        Resource res = null;
        try {
            res = getres();
        } catch (Loading l) {
        }
        if (res == null)
            return;
        String name = res.name;

        if (name.startsWith("gfx/terobjs/trees") && !name.endsWith("log") && !name.endsWith("oldtrunk"))
            type = Type.TREE;
        else if (name.endsWith("terobjs/plants/carrot") || name.endsWith("terobjs/plants/hemp"))
            type = Type.MULTISTAGE_PLANT;
        else if (name.startsWith("gfx/terobjs/plants") && !name.endsWith("trellis"))
            type = Type.PLANT;
        else if (name.startsWith("gfx/terobjs/bushes"))
            type = Type.BUSH;
        else if (name.equals("gfx/borka/body"))
            type = Type.PLAYER;
        else if (name.startsWith("gfx/terobjs/bumlings"))
            type = Type.BOULDER;
        else  if (name.endsWith("vehicle/bram") || name.endsWith("vehicle/catapult"))
            type = Type.SIEGE_MACHINE;
        else if (name.endsWith("/bear"))
            type = Type.BEAR;
        else if (name.endsWith("/lynx"))
            type = Type.LYNX;
        else if (name.endsWith("/walrus"))
            type = Type.WALRUS;
        else if (name.endsWith("/mammoth"))
            type = Type.MAMMOTH;
        else if (name.endsWith("/troll"))
            type = Type.TROLL;
        else if (additionalmobs.contains(name))
            type = Type.MOB;
        else if (name.endsWith("/minesupport") || name.endsWith("/ladder"))
            type = Type.WOODEN_SUPPORT;
        else if (name.endsWith("/column"))
            type = Type.STONE_SUPPORT;
        else if (name.endsWith("/trough"))
            type = Type.TROUGH;
        else if (name.endsWith("/beehive"))
            type = Type.BEEHIVE;
        else if (name.endsWith("/dframe"))
            type = Type.DFRAME;
        else
            type = Type.OTHER;
    }

    public boolean setup(RenderList rl) {
        loc.tick();
        for (Overlay ol : ols)
            rl.add(ol, null);
        for (Overlay ol : ols) {
            if (ol.spr instanceof Overlay.SetupMod)
                ((Overlay.SetupMod) ol.spr).setupmain(rl);
        }
        final GobHealth hlt = getattr(GobHealth.class);
        if (hlt != null)
            rl.prepc(hlt.getfx());

        if (MapView.markedGobs.contains(id))
            rl.prepc(MapView.markedFx);

        if (type == null)
            determineType();
        
        Resource res = null;
        try {
            res = getres();
        } catch (Loading l) {
        }

        if (Config.showdframestatus && type == Type.DFRAME) {
            boolean done = true;
            boolean empty = true;
            for (Overlay ol : ols) {
                try {
                    Indir<Resource> olires = ol.res;
                    if (olires != null) {
                        empty = false;
                        Resource olres = olires.get();
                        if (olres != null) {
                            if (olres.name.endsWith("-blood") || olres.name.endsWith("-windweed")) {
                                done = false;
                                break;
                            }
                        }
                    }
                } catch (Loading l) {
                }
            }
            if (done && !empty)
                rl.prepc(dframeDone);
            else if (empty)
                rl.prepc(dframeEmpty);
        }

        GobHighlight highlight = getattr(GobHighlight.class);
        if (highlight != null) {
            if (highlight.cycle <= 0)
                delattr(GobHighlight.class);
            else
                rl.prepc(highlight.getfx());
        }

        Drawable d = getattr(Drawable.class);
        if (d != null) {
            boolean hide = false;
            if (Config.hideall && res != null) {
                    hide = true;
            } else if (Config.hidegobs && res != null) {
                try {
                    if (Config.hidetrees) 
                        if (res != null && res.name.startsWith("gfx/terobjs/trees") && !res.name.endsWith("log") && !res.name.endsWith("oldtrunk")) 
                            hide = true;
                        
                    if (Config.hidecrops) 
                        if (res.name.startsWith("gfx/terobjs/plants") && !res.name.equals("gfx/terobjs/plants/trellis")) 
                        	hide = true;
                    
                    if (Config.hidewalls) 
                    	 if (res.name.startsWith("gfx/terobjs/arch/pali") && !res.name.equals("gfx/terobjs/arch/palisadegate") &&
                    			 !res.name.equals("gfx/terobjs/arch/palisadebiggate") || res.name.startsWith("gfx/terobjs/arch/brick")
                    			 && !res.name.equals("gfx/terobjs/arch/brickwallgate") &&!res.name.equals("gfx/terobjs/arch/brickwallbiggate")
                    			 || res.name.startsWith("gfx/terobjs/arch/pole") && !res.name.equals("gfx/terobjs/arch/polegate") && 
                    			 !res.name.equals("gfx/terobjs/arch/polebiggate")) 
                    		 hide = true;
                    
                    if (Config.hidewagons) 
                   	 if (res.name.startsWith("gfx/terobjs/vehicle/wagon")) 
                            hide = true;
                    
                    if (Config.hidehouses) 
                      	 if ((res.name.equals("gfx/terobjs/arch/stonemansion") || res.name.equals("gfx/terobjs/arch/logcabin") || res.name.equals("gfx/terobjs/arch/greathall") || res.name.equals("gfx/terobjs/arch/stonestead") || res.name.equals("gfx/terobjs/arch/timberhouse") || res.name.equals("gfx/terobjs/arch/stonetower"))) 
                      		 hide = true;
                      
                    if (Config.hidebushes) 
                     	 if ((res.name.startsWith("gfx/terobjs/bushes"))) 
                     		 hide = true;
                    
                    if (Config.hidedframes) 
                    	 if ((res.name.startsWith("gfx/terobjs/dframe"))) 
                    		 hide = true;
                    
                    if (Config.hidehfs) 
                    	 if ((res.name.startsWith("gfx/terobjs/pow"))) 
                    		 hide = true;
                    
                    if (Config.hidedcatchers) 
                    	 if ((res.name.startsWith("gfx/terobjs/dreca"))) 
                    		 hide = true;
                          
                     
                } catch (Loading le) {
                }
            }
            if (Config.hidegobs && type == Type.TREE) {
                GobHitbox.BBox bbox = GobHitbox.getBBox(this, true);
                if (bbox != null) {
                    rl.add(new Overlay(new GobHitbox(this, bbox.a, bbox.b, true)), null);
                }
            }

            if (Config.showboundingboxes) {
                GobHitbox.BBox bbox = GobHitbox.getBBox(this, true);
                if (bbox != null)
                    rl.add(new Overlay(new GobHitbox(this, bbox.a, bbox.b, false)), null);
            }
            
            if (!hide) 
               d.setup(rl);
            else if (hide)  {
            Resource.Neg neg = res.layer(Resource.Neg.class);
            if (neg != null && !Config.nohidebox)
            rl.add(new Overlay(new CustomHitbox(this, neg.ac, neg.bc, true)), null);
            else if(!Config.nohidebox)
            	rl.add(new Overlay(new CustomHitbox(this, new Coord(-5, -5), new Coord(5, 5), true)), null);
            }

            if (Config.showplantgrowstage) {
            	/* kommentoi TOISTAISEKSI
                try {
                    if (res != null && res.name.startsWith("gfx/terobjs/plants") && !res.name.endsWith("trellis")) {
                    	GAttrib rd = getattr(ResDrawable.class);
                    	final int stage = ((ResDrawable) rd).sdt.peekrbuf(0);
                    	int maxStage = 0;
                    	for (FastMesh.MeshRes layer : getres().layers(FastMesh.MeshRes.class)) {
                    		if (layer.id / 10 > maxStage) {
                    			maxStage = layer.id / 10;
                    		}
                    	}
									final int stageMax = maxStage;
									PView.Draw2D staged = new PView.Draw2D() {
										@Override
										public void draw2d(GOut g) {
											if (sc != null) {
												String str = String.format("%d/%d", new Object[]{stage, stageMax});
												if (!plantTex.containsKey(str)) {
													plantTex.put(str, Text.renderstroked(str, stage >= stageMax ? Color.GREEN : Color.RED, Color.BLACK, gobhpf).tex());
												}
												Tex tex = plantTex.get(str);
												g.image(tex, sc.sub(tex.sz().div(2)));
											}
										}	
	                                };
	                                rl.add(staged, null);

	                            }
	                            
                } catch (ArrayIndexOutOfBoundsException e) { // ignored
                }   
                */
                if (res != null && res.name.startsWith("gfx/terobjs/plants") && !res.name.endsWith("trellis")) {
                    GAttrib rd = getattr(ResDrawable.class);
                    if (rd != null) {
                        try {
                            int stage = ((ResDrawable) rd).sdt.peekrbuf(0);
                            if (cropstgmaxval == 0) {
                                for (FastMesh.MeshRes layer : res.layers(FastMesh.MeshRes.class)) {
                                    int stg = layer.id / 10;
                                    if (stg > cropstgmaxval)
                                        cropstgmaxval = stg;
                                }
                            }
                            Overlay ol = findol(Sprite.GROWTH_STAGE_ID);
                            if (ol == null && (stage == cropstgmaxval || stage >= 0 && stage <= 5)) {
                                addol(new Gob.Overlay(Sprite.GROWTH_STAGE_ID, new PlantStageSprite(stage, cropstgmaxval)));
                            }
                        } catch (ArrayIndexOutOfBoundsException e) { // ignored
                        }
                    }
                }

                if (type == Type.TREE || type == Type.BUSH) {
                    ResDrawable rd = getattr(ResDrawable.class);
                    if (rd != null && !rd.sdt.eom()) {
                        final int stage = rd.sdt.peekrbuf(0);
                        if (stage >= 0 && stage < 100) {
                            Overlay ol = findol(Sprite.GROWTH_STAGE_ID);
                            if (ol == null) {
                                addol(new Gob.Overlay(Sprite.GROWTH_STAGE_ID, new TreeStageSprite(stage)));
                            } else if (((TreeStageSprite)ol.spr).val != stage) {
                                ((TreeStageSprite)ol.spr).update(stage);
                            }
                        } 
                    }
                }
            }

            if (Config.showanimalrad && type != null &&  type.is(Type.MOB)) {
                boolean hasradius = ols.contains(animalradius);
                if ((knocked == null || knocked == Boolean.FALSE) && !hasradius)
                    ols.add(animalradius);
                else if (knocked == Boolean.TRUE && hasradius)
                    ols.remove(animalradius);
            }
            
           if (res != null && (res.name.contains("body") && !isplayer())) {
              /* boolean ispartymember = false;
               synchronized (ui.sess.glob.party.memb) {
                   ispartymember = ui.sess.glob.party.memb.containsKey(id);
               }
               if (!ispartymember && !playerhighlight.containsKey(this)) {
               */
        	   		if(!playerhighlight.containsKey(this)) {
            	   Overlay overlay = new Gob.Overlay(new PartyMemberOutline(this, new Color(255, 255, 255, 128), 0.1f));
                   ols.add(overlay);
                   playerhighlight.put(this, overlay);
        	   		}
              // } else if (playerhighlight.containsKey(this) && ispartymember)
            	//   playerhighlight.remove(this);
        	   
           }

            if (Config.showarchvector && type == Type.PLAYER && d instanceof Composite) {
                boolean targetting = false;

                Gob followGob = null;
                Moving moving = getattr(Moving.class);
                if (moving != null && moving instanceof Following)
                    followGob = ((Following)moving).tgt();

                for (Composited.ED ed : ((Composite) d).comp.cequ) {
                    try {
                        res = ed.res.res.get();
                        if (res != null && res.name.endsWith("huntersbow") && ed.res.sdt.peekrbuf(0) == 5) {
                            targetting = true;
                            if (bowvector == null) {
                                bowvector = new Overlay(new GobArcheryVector(this, followGob));
                                ols.add(bowvector);
                            }
                            break;
                        }
                    } catch (Loading l) {
                    }
                }

                if (!targetting && bowvector != null) {
                    ols.remove(bowvector);
                    bowvector = null;
                }
            }
        }
        Speaking sp = getattr(Speaking.class);
        if (sp != null)
            rl.add(sp.fx, null);
        KinInfo ki = getattr(KinInfo.class);
        if (ki != null)
            rl.add(ki.fx, null);
		return (false);
    }

    private static final Object DYNAMIC = new Object();
    private Object seq = null;
    public Object staticp() {
        if(seq == null) {
            int rs = 0;
            for(GAttrib attr : attr.values()) {
                Object as = attr.staticp();
                if(as == Rendered.CONSTANS) {
                } else if(as instanceof Static) {
                } else if(as == SemiStatic.class) {
                    rs = Math.max(rs, 1);
                } else {
                    rs = 2;
                    break;
                }
            }
            for(Overlay ol : ols) {
                Object os = ol.staticp();
                if(os == Rendered.CONSTANS) {
                } else if(os instanceof Static) {
                } else if(os == SemiStatic.class) {
                    rs = Math.max(rs, 1);
                } else {
                    rs = 2;
                    break;
                }
            }
            switch(rs) {
                case 0: seq = new Static(); break;
                case 1: seq = new SemiStatic(); break;
                default: seq = null; break;
            }
        }
        return((seq == DYNAMIC)?null:seq);
    }

    void changed() {
        seq = null;
    }

    public Random mkrandoom() {
        return (Utils.mkrandoom(id));
    }

    public Resource getres() {
        Drawable d = getattr(Drawable.class);
        if (d != null)
            return (d.getres());
        return (null);
    }

    public Glob glob() {
        return (glob);
    }

    /* Because generic functions are too nice a thing for Java. */
    public double getv() {
        Moving m = getattr(Moving.class);
        if (m == null)
            return (0);
        return (m.getv());
    }

    public final GLState olmod = new GLState() {
        public void apply(GOut g) {
        }

        public void unapply(GOut g) {
        }

        public void prep(Buffer buf) {
            for (Overlay ol : ols) {
                if (ol.spr instanceof Overlay.SetupMod) {
                    ((Overlay.SetupMod) ol.spr).setupgob(buf);
                }
            }
        }
    };

    public class Save extends GLState.Abstract {
        public Matrix4f cam = new Matrix4f(), wxf = new Matrix4f(),
                mv = new Matrix4f();
        public Projection proj = null;
        boolean debug = false;

        public void prep(Buffer buf) {
            mv.load(cam.load(buf.get(PView.cam).fin(Matrix4f.id))).mul1(wxf.load(buf.get(PView.loc).fin(Matrix4f.id)));
            Projection proj = buf.get(PView.proj);
            PView.RenderState wnd = buf.get(PView.wnd);
            Coord3f s = proj.toscreen(mv.mul4(Coord3f.o), wnd.sz());
            Gob.this.sc = new Coord(s);
            Gob.this.sczu = proj.toscreen(mv.mul4(Coord3f.zu), wnd.sz()).sub(s);
            this.proj = proj;
        }
    }

    public final Save save = new Save();

    public class GobLocation extends GLState.Abstract {
        private Coord3f c = null;
        private double a = 0.0;
        private final Location xl = new Location(Matrix4f.id, "gobx"), rot = new Location(Matrix4f.id, "gob");

        public void tick() {
            try {
                Coord3f c = getc();
                c.y = -c.y;
                if (Config.disableelev)
                    c.z = 0;
                if ((this.c == null) || !c.equals(this.c))
                    xl.update(Transform.makexlate(new Matrix4f(), this.c = c));
                if (this.a != Gob.this.a)
                    rot.update(Transform.makerot(new Matrix4f(), Coord3f.zu, (float) -(this.a = Gob.this.a)));
            } catch (Loading l) {
            }
        }

        public void prep(Buffer buf) {
            xl.prep(buf);
            rot.prep(buf);
        }
    }

    public final GobLocation loc = new GobLocation();

    public boolean isplayer() {
        return MapView.plgob == id;
    }

    public boolean isMoving() {
        if (getattr(LinMove.class) != null)
            return true;

        Following follow = getattr(Following.class);
        if (follow != null && follow.tgt().getattr(LinMove.class) != null)
            return true;

        return false;
    }

    public LinMove getLinMove() {
        LinMove lm = getattr(LinMove.class);
        if (lm != null)
            return lm;

        Following follow = getattr(Following.class);
        if (follow != null)
            return follow.tgt().getattr(LinMove.class);

        return null;
    }

    public boolean isFriend() {
        synchronized (glob.party.memb) {
            for (Party.Member m : glob.party.memb.values()) {
                if (m.gobid == id)
                    return true;
            }
        }

        KinInfo kininfo = getattr(KinInfo.class);
        if (kininfo == null || kininfo.group == 2 /*red*/)
            return false;

        return true;
    }
}
