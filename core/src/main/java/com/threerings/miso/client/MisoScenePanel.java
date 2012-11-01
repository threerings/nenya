//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
// https://github.com/threerings/nenya
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.miso.client;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Icon;
import javax.swing.JFrame;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.samskivert.util.StringUtil;

import com.samskivert.swing.Controller;
import com.samskivert.swing.RadialMenu;
import com.samskivert.swing.RuntimeAdjust;
import com.samskivert.swing.event.CommandEvent;

import com.threerings.media.VirtualMediaPanel;
import com.threerings.media.sprite.Sprite;
import com.threerings.media.tile.ObjectTile;
import com.threerings.media.tile.Tile;
import com.threerings.media.tile.TileManager;
import com.threerings.media.tile.TileSet;
import com.threerings.media.util.AStarPathUtil;
import com.threerings.media.util.MathUtil;
import com.threerings.media.util.Path;

import com.threerings.miso.MisoPrefs;
import com.threerings.miso.client.DirtyItemList.DirtyItem;
import com.threerings.miso.data.MisoSceneModel;
import com.threerings.miso.data.ObjectInfo;
import com.threerings.miso.tile.BaseTile;
import com.threerings.miso.tile.AutoFringer.FringeTile;
import com.threerings.miso.util.MisoContext;
import com.threerings.miso.util.MisoSceneMetrics;
import com.threerings.miso.util.MisoUtil;

import static com.threerings.miso.Log.log;

/**
 * Renders a Miso scene for all to see.
 */
public class MisoScenePanel extends VirtualMediaPanel
    implements MouseListener, MouseMotionListener, AStarPathUtil.TraversalPred, RadialMenu.Host
{
    /** Show flag that indicates we should show all tips. */
    public static final int SHOW_TIPS = (1 << 0);

    /**
     * Creates a blank miso scene display. Configure it with a scene model via
     * {@link #setSceneModel} to cause it to display something.
     */
    public MisoScenePanel (MisoContext ctx, MisoSceneMetrics metrics)
    {
        super(ctx.getFrameManager());
        _ctx = ctx;
        _metrics = metrics;
        _rethinkOp = new RethinkOp(_metrics);
        _applicator = new TileOpApplicator(_metrics);

        // set ourselves up
        setOpaque(true);
        addMouseListener(this);
        addMouseMotionListener(this);

        // create the resolver if it's not already around
        _resolver = _resolvers.get(_ctx);
        if (_resolver == null) {
            _resolver = new SceneBlockResolver();
            _resolver.setDaemon(true);
            _resolver.setPriority(Thread.MIN_PRIORITY);
            _resolver.start();
            _resolvers.put(_ctx, _resolver);
        }
    }

    /**
     * Configures this display with a scene model which will immediately be resolved and
     * displayed.
     */
    public void setSceneModel (MisoSceneModel model)
    {
        _model = model;

        // clear out old blocks and objects
        clearScene();

        centerOnTile(0, 0);
        if (isShowing()) {
            rethink();
            _remgr.invalidateRegion(_vbounds);
        }
    }

    /**
     * Clears out our old scene business.
     */
    protected void clearScene ()
    {
        _blocks.clear();
        _vizobjs.clear();
        _fringes.clear();
        _masks.clear();
        if (_dpanel != null) {
            _dpanel.newScene();
        }
    }

    /**
     * Completely invalidates our current resolved scene and re-resolves it from the ground up.
     */
    public void refreshScene ()
    {
        clearScene();
        // Don't repaint immediately if we've gotten new penders from our rethink
        _delayRepaint = rethink() > 0;
        _remgr.invalidateRegion(_vbounds);
    }

    /**
     * Moves the scene such that the specified tile is in the center.
     */
    public void centerOnTile (int tx, int ty)
    {
        Rectangle trect = MisoUtil.getTilePolygon(_metrics, tx, ty).getBounds();
        int nx = trect.x + trect.width/2 - _vbounds.width/2;
        int ny = trect.y + trect.height/2 - _vbounds.height/2;
//         Log.info("Centering on t:" + StringUtil.coordsToString(tx, ty) +
//                  " b:" + StringUtil.toString(trect) +
//                  " vb: " + StringUtil.toString(_vbounds) +
//                  ", n:" + StringUtil.coordsToString(nx, ny) + ".");
        setViewLocation(nx, ny);
    }

    /**
     * Returns the scene model being displayed by this panel. <em>Do
     * not</em> modify!
     */
    public MisoSceneModel getSceneModel ()
    {
        return _model;
    }

    /**
     * Returns the scene metrics in use by this panel. <em>Do not</em> modify!
     */
    public MisoSceneMetrics getSceneMetrics ()
    {
        return _metrics;
    }

    /**
     * Set whether or not to highlight object tooltips (and potentially other scene entities).
     */
    public void setShowFlags (int flags, boolean on)
    {
        int oldshow = _showFlags;

        if (on) {
            _showFlags |= flags;
        } else {
            _showFlags &= ~flags;
        }

        if (oldshow != _showFlags) {
            showFlagsDidChange(oldshow);
        }
    }

    /**
     * Check to see if the specified show flag is on.
     */
    public boolean checkShowFlag (int flag)
    {
        return (0 != (flag & _showFlags));
    }

    /**
     * Called when our show flags have changed.
     */
    protected void showFlagsDidChange (int oldflags)
    {
        if ((oldflags & SHOW_TIPS) != (_showFlags & SHOW_TIPS)) {
            for (SceneObjectIndicator indic : _indicators.values()) {
                dirtyIndicator(indic);
            }
        }
    }

    /**
     * Returns the top-most object over which the mouse is hovering; this may be a sprite or a
     * {@link SceneObject}.
     */
    public Object getHoverObject ()
    {
        return _hobject;
    }

    /**
     * Returns the tile coordinates of the tile over which the mouse is hovering.
     */
    public Point getHoverCoords ()
    {
        return _hcoords;
    }

    /**
     * Returns an iterator over all resolved {@link SceneBlock} instances.
     */
    public Iterator<SceneBlock> enumerateResolvedBlocks ()
    {
        return _blocks.values().iterator();
    }

    /**
     * Returns the resolved block that contains the specified tile coordinate or null if no block
     * is resolved for that coordinate.
     */
    public SceneBlock getBlock (int tx, int ty)
    {
        int bx = MathUtil.floorDiv(tx, _metrics.blockwid);
        int by = MathUtil.floorDiv(ty, _metrics.blockhei);
        return _blocks.get(compose(bx, by));
    }

    /**
     * Computes a path for the specified sprite to the specified tile coordinates.
     *
     * @param loose if true, an approximate path will be returned if a complete path cannot be
     * located. This path will navigate the sprite "legally" as far as possible and then walk the
     * sprite in a straight line to its final destination. This is generally only useful if the
     * the path goes "off screen".
     */
    public Path getPath (Sprite sprite, int x, int y, boolean loose)
    {
        // sanity check
        if (sprite == null) {
            throw new IllegalArgumentException(
                "Can't get path for null sprite [x=" + x + ", y=" + y + ".");
        }

        // get the destination tile coordinates
        Point src = MisoUtil.screenToTile(
            _metrics, sprite.getX(), sprite.getY(), new Point());
        Point dest = MisoUtil.screenToTile(_metrics, x, y, new Point());

        // compute our longest path from the screen size
        int longestPath = 3 * (getWidth() / _metrics.tilewid);

        // get a reasonable tile path through the scene
        long start = System.currentTimeMillis();
        List<Point> points = AStarPathUtil.getPath(
            this, sprite, longestPath, src.x, src.y, dest.x, dest.y, loose);
        long duration = System.currentTimeMillis() - start;

        // sanity check the number of nodes searched so that we can keep an eye out for bogosity
        if (duration > 500L) {
            int considered = AStarPathUtil.getConsidered();
            log.warning("Considered " + considered + " nodes for path from " +
                        StringUtil.toString(src) + " to " +
                        StringUtil.toString(dest) +
                        " [duration=" + duration + "].");
        }

        // construct a path object to guide the sprite on its merry way
        return (points == null) ? null :
            new TilePath(_metrics, sprite, points, x, y);
    }

    /**
     * Converts the supplied full coordinates to screen coordinates.
     */
    public Point getScreenCoords (int x, int y)
    {
        return MisoUtil.fullToScreen(_metrics, x, y, new Point());
    }

    /**
     * Converts the supplied screen coordinates to full coordinates.
     */
    public Point getFullCoords (int x, int y)
    {
        return MisoUtil.screenToFull(_metrics, x, y, new Point());
    }

    /**
     * Converts the supplied screen coordinates to tile coordinates.
     */
    public Point getTileCoords (int x, int y)
    {
        return MisoUtil.screenToTile(_metrics, x, y, new Point());
    }

    /**
     * Clears any radial menu being displayed.
     */
    public void clearRadialMenu ()
    {
        if (_activeMenu != null) {
            _activeMenu.deactivate();
        }
    }

    /**
     * Reports the memory usage of the resolved tiles in the current scene block.
     */
    public void reportMemoryUsage ()
    {
        Map<Tile.Key,BaseTile> base = Maps.newHashMap();
        Set<BaseTile> fringe = Sets.newHashSet();
        Map<Tile.Key,ObjectTile> object = Maps.newHashMap();
        long[] usage = new long[3];
        for (SceneBlock block : _blocks.values()) {
            block.computeMemoryUsage(base, fringe, object, usage);
        }
        log.info("Scene tile memory usage",
            "scene", StringUtil.shortClassName(this),
            "base", base.size() + "->" + (usage[0] / 1024) + "k",
            "fringe", fringe.size() + "->" + (usage[1] / 1024) + "k",
            "obj", object.size() + "->" + (usage[2] / 1024) + "k");
    }

    @Override
    public void addNotify ()
    {
        super.addNotify();

        if (_resolveDebug.getValue()) {
            _dpanel = new ResolutionView(this);
            _dframe = new JFrame("Scene block resolver");
            _dframe.setContentPane(_dpanel);
            _dframe.pack();
            _dframe.setVisible(true);
        }
    }

    @Override
    public void removeNotify ()
    {
        super.removeNotify();

        if (_dpanel != null) {
            _dframe.dispose();
            _dpanel = null;
            _dframe = null;
        }
    }

    // documentation inherited from interface
    public void mouseClicked (MouseEvent e)
    {
        // nothing doing
    }

    // documentation inherited from interface
    public void mousePressed (MouseEvent e)
    {
        // ignore mouse presses if we're not responsive
        if (!isResponsive()) {
            return;
        }

        if (e.getButton() == MouseEvent.BUTTON1) {
            if (_hobject instanceof Sprite) {
                handleSpritePressed((Sprite)_hobject, e.getX(), e.getY());
                return;
            } else if (_hobject instanceof SceneObject) {
                handleObjectPressed((SceneObject)_hobject, e.getX(), e.getY());
                return;
            }
        }
        // if not button1, or _hobject not Sprite or SceneObject...
        handleMousePressed(_hobject, e);
    }

    /**
     * Programmatically "click" a scene object. This results in a call to
     * {@link #handleObjectPressed} with click coordinates in the center of the object.
     */
    public void pressObject (SceneObject scobj)
    {
        int px = scobj.bounds.x + scobj.bounds.width/2;
        int py = scobj.bounds.y + scobj.bounds.height/2;
        handleObjectPressed(scobj, px, py);
    }

    /**
     * Called when the user presses the mouse button over a sprite.
     */
    protected void handleSpritePressed (Sprite sprite, int mx, int my)
    {
    }

    /**
     * Called when the user presses the mouse button over an object.
     */
    protected void handleObjectPressed (final SceneObject scobj, int mx, int my)
    {
        String action = scobj.info.action;
        final ObjectActionHandler handler = ObjectActionHandler.lookup(action);

        // if there's no handler, just fire the action immediately
        if (handler == null) {
            fireObjectAction(null, scobj, new SceneObjectActionEvent(
                                 this, 0, action, 0, scobj));
            return;
        }

        // if the action's not allowed, pretend like we handled it
        if (!handler.actionAllowed(action)) {
            return;
        }

        // if there's no menu for this object, fire the action immediately
        RadialMenu menu = handler.handlePressed(scobj);
        if (menu == null) {
            fireObjectAction(handler, scobj, new SceneObjectActionEvent(
                                 this, 0, action, 0, scobj));
            return;
        }

        // make the menu surround the clicked object, but with consistent size
        Rectangle mbounds = getRadialMenuBounds(scobj);

        _activeMenu = menu;
        _activeMenu.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent e) {
                if (e instanceof CommandEvent) {
                    fireObjectAction(handler, scobj, e);
                } else {
                    SceneObjectActionEvent event = new SceneObjectActionEvent(
                        e.getSource(), e.getID(), e.getActionCommand(),
                        e.getModifiers(), scobj);
                    fireObjectAction(handler, scobj, event);
                }
            }
        });
        _activeMenu.activate(this, mbounds, scobj);
    }

    /**
     * Returns an appropriate set of menu bounds for the specified object. Returns a rectangle of
     * the size specified by {@link #getObjectRadialSize} centered around the object.
     */
    protected Rectangle getRadialMenuBounds (SceneObject scobj)
    {
        Rectangle mbounds = new Rectangle(scobj.bounds);
        Dimension radbox = getObjectRadialSize();
        if (mbounds.width != radbox.width) {
            mbounds.x += (mbounds.width-radbox.width)/2;
            mbounds.width = radbox.width;
        }
        if (mbounds.height != radbox.height) {
            mbounds.y += (mbounds.height-radbox.height)/2;
            mbounds.height = radbox.height;
        }
        return mbounds;
    }

    /**
     * Returns the size of the rectangle around which we create an object's radial menu. The
     * default is a sensible size, but derived classes may wish to tune the value to make their
     * menus lay out in a more aestetically pleasing manner.
     */
    protected Dimension getObjectRadialSize ()
    {
        return DEF_RADIAL_RECT;
    }

    /**
     * Called when an object or object menu item has been clicked.
     */
    protected void fireObjectAction (
        ObjectActionHandler handler, SceneObject scobj, ActionEvent event)
    {
        if (handler == null) {
            Controller.postAction(event);
        } else {
            handler.handleAction(scobj, event);
        }
    }

    /**
     * Called when the mouse is pressed over an unknown or non-existent hover object.
     *
     * @param hobject the hover object at the time of the mouse press or null if no hover object
     * is active.
     *
     * @return true if the mouse press was handled, false if not.
     */
    protected boolean handleMousePressed (Object hobject, MouseEvent event)
    {
        return false;
    }

    // documentation inherited from interface
    public void mouseReleased (MouseEvent e)
    {
        // nothing doing; everything is handled on pressed
    }

    // documentation inherited from interface
    public void mouseEntered (MouseEvent e)
    {
        // nothing doing
    }

    // documentation inherited from interface
    public void mouseExited (MouseEvent e)
    {
        // clear the highlight tracking data
        _hcoords.setLocation(-1, -1);
        changeHoverObject(null);
        _remgr.invalidateRegion(_vbounds);
    }

    // documentation inherited from interface
    public void mouseDragged (MouseEvent e)
    {
        // nothing doing
    }

    // documentation inherited from interface
    public void mouseMoved (MouseEvent e)
    {
        int x = e.getX(), y = e.getY();

        // update the mouse's tile coordinates
        updateTileCoords(x, y, _hcoords);

        // stop now if we're not responsive
        if (!isResponsive()) {
            return;
        }

        // give derived classes a chance to start with a hover object
        Object hobject = computeOverHover(x, y);

        // if they came up with nothing, compute the list of objects over
        // which the mouse is hovering
        if (hobject == null) {
            // start with the sprites that contain the point
            _spritemgr.getHitSprites(_hitSprites, x, y);
            int hslen = _hitSprites.size();
            for (int ii = 0; ii < hslen; ii++) {
                Sprite sprite = _hitSprites.get(ii);
                appendDirtySprite(_hitList, sprite);
            }

            // add the object tiles that contain the point
            getHitObjects(_hitList, x, y);

            // sort the list of hit items by rendering order
            _hitList.sort();

            // the last element in the array is what we want (assuming
            // there are any items in the array)
            int icount = _hitList.size();
            if (icount > 0) {
                DirtyItem item = _hitList.get(icount-1);
                hobject = item.obj;
            }

            // clear out the hitlists
            _hitList.clear();
            _hitSprites.clear();
        }

        // if the user isn't hovering over a sprite or object with an
        // action, allow derived classes to provide some other hover
        if (hobject == null) {
            hobject = computeUnderHover(x, y);
        }

        changeHoverObject(hobject);
    }

    /**
     * Gives derived classes a chance to compute a hover object that takes precedence over sprites
     * and actionable objects. If this method returns non-null, no sprite or object hover
     * calculations will be performed and the object returned will become the new hover object.
     */
    protected Object computeOverHover (int mx, int my)
    {
        return null;
    }

    /**
     * Gives derived classes a chance to compute a hover object that is used if the mouse is not
     * hovering over a sprite or actionable object. If this method is called, it means that there
     * are no sprites or objects under the mouse. Thus if it returns non-null, the object returned
     * will become the new hover object.
     */
    protected Object computeUnderHover (int mx, int my)
    {
        return null;
    }

    // documentation inherited
    public boolean canTraverse (Object traverser, int tx, int ty)
    {
        SceneBlock block = getBlock(tx, ty);
        return (block == null) ? canTraverseUnresolved(traverser, tx, ty) :
            block.canTraverse(traverser, tx, ty);
    }

    /**
     * Derived classes can control whether or not we consider unresolved tiles to be traversable
     * or not.
     */
    protected boolean canTraverseUnresolved (Object traverser, int tx, int ty)
    {
        return false;
    }

    @Override
    public Rectangle getViewBounds ()
    {
        return _vbounds;
    }

    @Override
    public Component getComponent ()
    {
        return this;
    }

    // documentation inherited from interface
    public void repaintRect (int x, int y, int width, int height)
    {
        // translate back into view coordinates
        x -= _vbounds.x;
        y -= _vbounds.y;
        repaint(x, y, width, height);
    }

    // documentation inherited from interface
    public void menuDeactivated (RadialMenu menu)
    {
        _activeMenu = null;
    }

    @Override
    public void setBounds (int x, int y, int width, int height)
    {
        super.setBounds(x, y, width, height);

        // if we change size...
        if (width != _rsize.width || height != _rsize.height) {
            // ...adjust our view location to preserve the center of the
            // screen...
            int dx = (_rsize.width-width)/2, dy = (_rsize.height-height)/2;
//             Log.info("Adjusting offset " +
//                      "rsize:" + StringUtil.toString(_rsize) +
//                      " nsize:" + width + "x" + height +
//                      " vb:" + StringUtil.toString(_vbounds) +
//                      " d:" + StringUtil.coordsToString(dx, dy) + ".");
            setViewLocation(_nx+dx, _ny+dy);
            _rsize.setSize(width, height);

            // ...and force a rethink on the next tick
            _ulpos = null;
        }
    }

    @Override
    protected void viewLocationDidChange (int dx, int dy)
    {
        super.viewLocationDidChange(dx, dy);

        // compute the tile coordinates of our upper left screen coordinate and request a rethink
        // if they've changed
        MisoUtil.screenToTile(_metrics, _vbounds.x, _vbounds.y, _tcoords);
        if (_ulpos == null || !_tcoords.equals(_ulpos)) {
            // if this is a forced rethink (_ulpos is null), we might delay paint as a result of
            // it, but only if we queue up blocks for resolution in our rethink
            boolean mightDelayPaint = false;
            if (_ulpos == null) {
                _ulpos = new Point();
                mightDelayPaint = true;
            }
            _ulpos.setLocation(_tcoords);
            if (rethink() > 0) {
                _delayRepaint = mightDelayPaint;
                // If this is a complete repaint, turn off visibility while we're resolving to
                // keep child components or media panels from drawing.
                if (_delayRepaint) {
                    setVisible(false);
                }
                log.info("Got new pending blocks",
                    "need", _visiBlocks.size(), "of", _pendingBlocks,
                    "view", StringUtil.toString(_vbounds), "delay", _delayRepaint);
            }
        }
    }

    /**
     * Derived classes can override this method and provide a colorizer that will be used to
     * colorize the supplied scene object when rendering.
     */
    protected TileSet.Colorizer getColorizer (ObjectInfo oinfo)
    {
        return null;
    }

    /**
     * Computes the tile coordinates of the supplied sprite and appends it to the supplied dirty
     * item list.
     */
    protected void appendDirtySprite (DirtyItemList list, Sprite sprite)
    {
        MisoUtil.screenToTile(_metrics, sprite.getX(), sprite.getY(), _tcoords);
        list.appendDirtySprite(sprite, _tcoords.x, _tcoords.y);
    }

    /**
     * Returns the tile manager from which we load our tiles.
     */
    protected TileManager getTileManager ()
    {
        return _ctx.getTileManager();
    }

    /**
     * This is called when our view position has changed by more than one tile in any direction.
     * Herein we do a whole crapload of stuff:
     *
     * <ul>
     *   <li>Queue up loads for any new influential blocks.</li>
     *   <li>Flush any blocks that are no longer influential.</li>
     *   <li>Recompute the list of potentially visible scene objects.</li>
     * </ul>
     *
     * @return the count of blocks pending after this rethink.
     */
    protected int rethink ()
    {
        // recompute our "area of influence"
        computeInfluentialBounds();

//         Log.info("Rethinking vb:" + StringUtil.toString(_vbounds) +
//                  " ul:" + StringUtil.toString(_ulpos) +
//                  " ibounds: " + StringUtil.toString(_ibounds));

        // not to worry if we presently have no scene model
        if (_model == null) {
            return 0;
        }

        // compute the intersecting set of blocks
        _applicator.applyToTiles(_ibounds, _rethinkOp);
//         Log.info("Influential blocks " +
//                  StringUtil.toString(_rethinkOp.blocks) + ".");

        // prune any blocks that are no longer influential
        Point key = new Point();
        for (Iterator<SceneBlock> iter = _blocks.values().iterator(); iter.hasNext(); ) {
            SceneBlock block = iter.next();
            key.x = block.getBounds().x;
            key.y = block.getBounds().y;
            if (!_rethinkOp.blocks.contains(key)) {
                log.debug("Flushing block " + block + ".");
                if (_dpanel != null) {
                    _dpanel.blockCleared(block);
                }
                iter.remove();
            }
        }

        for (Point origin : _rethinkOp.blocks) {
            int bx = MathUtil.floorDiv(origin.x, _metrics.blockwid);
            int by = MathUtil.floorDiv(origin.y, _metrics.blockhei);
            int bkey = compose(bx, by);
            if (!_blocks.containsKey(bkey)) {
                SceneBlock block = new SceneBlock(
                    this, origin.x, origin.y, _metrics.blockwid, _metrics.blockhei);
                boolean visible = block.getFootprint().getBounds().intersects(_vibounds);
                block.setVisiBlock(visible);
                _blocks.put(bkey, block);

                // queue the block up to be resolved
                _pendingBlocks++;
                if (visible) {
                    _visiBlocks.add(block);
                }
                _resolver.resolveBlock(block, visible);
                if (_dpanel != null) {
                    _dpanel.queuedBlock(block);
                }
            }
        }
        _rethinkOp.blocks.clear();

        // recompute our visible object set
        recomputeVisible();

        log.debug("Rethunk [pending=" + _pendingBlocks + ", visible=" + _visiBlocks.size() + "].");
        return _visiBlocks.size();
    }
    /**
     * Calls through to {@link #computeInfluentialBounds(Rectangle, Rectangle, Rectangle)} with
     * _vbounds, _ibounds and _vibounds.
     */
    protected void computeInfluentialBounds ()
    {
        computeInfluentialBounds(_vbounds, _ibounds, _vibounds);
    }


    /**
     * Configures <code>influentialBounds</code> to contain the bounds of the potentially
     * "influential" world and <code>visibleBlockBounds</code> to contain bounds that are used
     * to determine which blocks should be resolved before making the view visible.
     *
     * <p>Everything that intersects the influential area will be resolved on the expectation
     * that it could be scrolled into view at any time. The influential bounds should be large
     * enough that the time between a block becoming influential and the time at which it is
     * resolved is longer than the expected time by which it will be scrolled into view, otherwise
     * the users will see the man behind the curtain.
     */
    public static void computeInfluentialBounds (Rectangle visibleBounds,
        Rectangle influentualBounds, Rectangle visibleBlockBounds)
    {
        int infborx = 3 * visibleBounds.width / 4;
        int infbory = visibleBounds.height / 2;
        // we go extra on the height because objects below can influence fairly high up
        influentualBounds.setBounds(visibleBounds.x - infborx, visibleBounds.y - infbory,
            visibleBounds.width + 2 * infborx, visibleBounds.height + 3 * infbory);
        visibleBlockBounds.setBounds(visibleBounds.x - visibleBounds.width / 4,
            visibleBounds.y, visibleBounds.width + visibleBounds.width / 2,
            visibleBounds.height + infbory);
    }

    /**
     * Returns the bounds for which all intersecting scene blocks are kept resolved. Do not modify
     * the rectangle returned by this method.
     */
    protected Rectangle getInfluentialBounds ()
    {
        return _ibounds;
    }

    /**
     * Called by the scene block when it has started its resolution.
     */
    protected void blockResolving (SceneBlock block)
    {
        if (_dpanel != null) {
            _dpanel.resolvingBlock(block);
        }
    }

    /**
     * Called by the scene block if it has come up for resolution but is no longer influential.
     */
    protected void blockAbandoned (SceneBlock block)
    {
        if (_dpanel != null) {
            _dpanel.blockCleared(block);
        }

        blockFinished(block);
    }

    /**
     * Called by a scene block when it has completed its resolution process.
     */
    protected void blockResolved (SceneBlock block)
    {
        if (_dpanel != null) {
            _dpanel.resolvedBlock(block);
        }

        Rectangle sbounds = block.getScreenBounds();
        if (!_delayRepaint && sbounds != null && sbounds.intersects(_vbounds)) {
//            warnVisible(block, sbounds);
            // if we have yet further blocks to resolve, queue up a repaint now so that we get these
            // data onscreen as quickly as possible
            if (_pendingBlocks > 1) {
                recomputeVisible();
                _remgr.invalidateRegion(sbounds);
            }
        }

        blockFinished(block);
    }

    /**
     * Called whenever a block is done resolving, whether it was successfully resolved or if it
     * was abandoned.
     */
    protected void blockFinished (SceneBlock block)
    {
        --_pendingBlocks;

        // once all the visible pending blocks have completed their
        // resolution, recompute our visible object set and show ourselves
        if (_visiBlocks.remove(block) && _visiBlocks.size() == 0) {
            allBlocksFinished();
        }
    }

    /**
     * Called to handle the proceedings once our last resolving block has been finished.
     */
    protected void allBlocksFinished ()
    {
        recomputeVisible();
        log.info("Restoring repaint... ", "left", _pendingBlocks, "view",
            StringUtil.toString(_vbounds));
        _delayRepaint = false;
        // Need to restore visibility as it may have been turned of as a result of the delay
        setVisible(true);
        _remgr.invalidateRegion(_vbounds);
    }

    /**
     * Issues a warning to the error log that the specified block became visible prior to being
     * resolved. Derived classes may wish to augment or inhibit this warning.
     */
    protected void warnVisible (SceneBlock block, Rectangle sbounds)
    {
        log.warning("Block visible during resolution " + block +
                    " sbounds:" + StringUtil.toString(sbounds) +
                    " vbounds:" + StringUtil.toString(_vbounds) + ".");
    }

    /**
     * Recomputes our set of visible objects and their indicators.
     */
    protected void recomputeVisible ()
    {
        // flush our visible object set which we'll recreate later
        _vizobjs.clear();

        Rectangle vbounds = new Rectangle(
            _vbounds.x-_metrics.tilewid, _vbounds.y-_metrics.tilehei,
            _vbounds.width+2*_metrics.tilewid,
            _vbounds.height+2*_metrics.tilehei);

        for (SceneBlock block : _blocks.values()) {
            if (!block.isResolved()) {
                continue;
            }

            // links this block to its neighbors; computes coverage
            block.update(_blocks);

            // see which of this block's objects are visible
            SceneObject[] objs = block.getObjects();
            for (SceneObject obj : objs) {
                if (obj.bounds != null &&
                    vbounds.intersects(obj.bounds)) {
                    _vizobjs.add(obj);
                }
            }
        }

        // recompute our object indicators
        computeIndicators();

//         Log.info("Computed " + _vizobjs.size() + " visible objects from " +
//                  _blocks.size() + " blocks.");

//         Log.info(StringUtil.listToString(_vizobjs, new StringUtil.Formatter() {
//             public String toString (Object object) {
//                 SceneObject scobj = (SceneObject)object;
//                 return (TileUtil.getTileSetId(scobj.info.tileId) + ":" +
//                         TileUtil.getTileIndex(scobj.info.tileId));
//             }
//         }));
    }

    /**
     * Masks off the lower 16 bits of the supplied integers and composes
     * them into a single int.
     */
    protected static int compose (int x, int y)
    {
        return (x << 16) | (y & 0xFFFF);
    }

    /**
     * Compute the indicators for any objects in the scene.
     */
    public void computeIndicators ()
    {
        Map<SceneObject, SceneObjectIndicator> _unupdated = Maps.newHashMap(_indicators);
        for (int ii = 0, nn = _vizobjs.size(); ii < nn; ii++) {
            SceneObject scobj = _vizobjs.get(ii);
            String action = scobj.info.action;

            // if the object has no action, skip it
            if (StringUtil.isBlank(action)) {
                continue;
            }

            // if we have an object action handler, possibly let them veto
            // the display of this tooltip and action
            ObjectActionHandler oah = ObjectActionHandler.lookup(action);
            if (oah != null && !oah.isVisible(action)) {
                continue;
            }

            String tiptext = getTipText(scobj, action);
            if (tiptext != null) {
                Icon icon = getTipIcon(scobj, action);
                SceneObjectIndicator indic = _unupdated.remove(scobj);
                if (indic == null) {
                    // let the object action handler create the indicator if it exists, otherwise
                    // just use a regular tip
                    if (oah != null) {
                        indic = oah.createIndicator(this, tiptext, icon);
                    } else {
                        indic = new SceneObjectTip(tiptext, icon);
                    }
                    _indicators.put(scobj, indic);
                } else {
                    indic.update(icon, tiptext);
                }
                dirtyIndicator(indic);
            }
        }
        // clear out any no longer used indicators
        for (SceneObject toremove : _unupdated.keySet()) {
            SceneObjectIndicator indic = _indicators.remove(toremove);
            indic.removed();
            dirtyIndicator(indic);
        }

        _indicatorsLaidOut = false;
    }

    /**
     * Derived classes can provide human readable object tips via this method.
     */
    protected String getTipText (SceneObject scobj, String action)
    {
        ObjectActionHandler oah = ObjectActionHandler.lookup(action);
        return (oah == null) ? action : oah.getTipText(action);
    }

    /**
     * Provides an icon for this tooltip, the default looks up an object action handler for the
     * action and requests the icon from it.
     */
    protected Icon getTipIcon (SceneObject scobj, String action)
    {
        ObjectActionHandler oah = ObjectActionHandler.lookup(action);
        return (oah == null) ? null : oah.getTipIcon(action);
    }

    /**
     * Dirties the specified indicator.
     */
    protected void dirtyIndicator (SceneObjectIndicator indic)
    {
        if (indic != null) {
            Rectangle r = indic.getBounds();
            if (r != null) {
                _remgr.invalidateRegion(r);
            }
        }
    }

    /**
     * Change the hover object to the new object.
     */
    protected void changeHoverObject (Object newHover)
    {
        if (newHover == _hobject) {
            return;
        }
        Object oldHover = _hobject;
        _hobject = newHover;
        hoverObjectChanged(oldHover, newHover);
    }

    /**
     * A place for subclasses to react to the hover object changing.
     * One of the supplied arguments may be null.
     */
    protected void hoverObjectChanged (Object oldHover, Object newHover)
    {
        // deal with objects that care about being hovered over
        if (oldHover instanceof SceneObject) {
            SceneObject oldhov = (SceneObject)oldHover;
            if (oldhov.setHovered(false)) {
                _remgr.invalidateRegion(oldhov.bounds);
            }
        }
        if (newHover instanceof SceneObject) {
            SceneObject newhov = (SceneObject)newHover;
            if (newhov.setHovered(true)) {
                _remgr.invalidateRegion(newhov.bounds);
            }
        }

        // dirty the indicators associated with the hover objects
        dirtyIndicator(_indicators.get(oldHover));
        dirtyIndicator(_indicators.get(newHover));
    }

    /**
     * Adds to the supplied dirty item list, all of the object tiles that are hit by the specified
     * point (meaning the point is contained within their bounds and intersects a non-transparent
     * pixel in the actual object image.
     */
    protected void getHitObjects (DirtyItemList list, int x, int y)
    {
        for (SceneObject scobj : _vizobjs) {
            Rectangle pbounds = scobj.bounds;
            if (!pbounds.contains(x, y)) {
                continue;
            }

            // see if we should skip it
            if (skipHitObject(scobj)) {
                continue;
            }

            // now check that the pixel in the tile image is non-transparent at that point
            if (!scobj.tile.hitTest(x - pbounds.x, y - pbounds.y)) {
                continue;
            }

            // we've passed the test, add the object to the list
            list.appendDirtyObject(scobj);
        }
    }

    /**
     * Determines whether we should skip the specified object when compiling the list of objects
     * under a specified point using {@link #getHitObjects}. The default implementation returns
     * <code>true</code> if the object has no action.
     */
    protected boolean skipHitObject (SceneObject scobj)
    {
        return StringUtil.isBlank(scobj.info.action);
    }

    /**
     * Converts the supplied screen coordinates into tile coordinates, writing the values into the
     * supplied {@link Point} instance and returning true if the screen coordinates translated
     * into a different set of tile coordinates than were already contained in the point (so that
     * the caller can know to update a highlight, for example).
     *
     * @return true if the tile coordinates have changed.
     */
    protected boolean updateTileCoords (int sx, int sy, Point tpos)
    {
        Point npos = MisoUtil.screenToTile(_metrics, sx, sy, new Point());
        if (!tpos.equals(npos)) {
            tpos.setLocation(npos.x, npos.y);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void paint (Graphics g)
    {
        if (_delayRepaint) {
            return;
        }
        super.paint(g);
    }

    @Override
    protected void paintInFront (Graphics2D gfx, Rectangle dirty)
    {
        super.paintInFront(gfx, dirty);

        // paint any active menu (this should in theory check to see if
        // the active menu intersects one or more of the dirty rects)
        if (_activeMenu != null) {
            _activeMenu.render(gfx);
        }
    }

    @Override
    protected void paintBetween (Graphics2D gfx, Rectangle dirty)
    {
        // render any intersecting tiles
        paintTiles(gfx, dirty);

        // render anything that goes on top of the tiles
        paintBaseDecorations(gfx, dirty);

        // render our dirty sprites and objects
        paintDirtyItems(gfx, dirty);

        // draw sprite paths
        if (_pathsDebug.getValue()) {
            _spritemgr.renderSpritePaths(gfx);
        }

        // paint any extra goodies
        paintExtras(gfx, dirty);
    }

    /**
     * We don't want sprites rendered using the standard mechanism because we intersperse them
     * with objects in our scene and need to manage their z-order.
     */
    @Override
    protected void paintBits (Graphics2D gfx, int layer, Rectangle dirty)
    {
        _animmgr.paint(gfx, layer, dirty);
    }

    /**
     * A function where derived classes can paint things after the base tiles have been rendered
     * but before anything else has been rendered (so that whatever is painted appears to be on
     * the ground).
     */
    protected void paintBaseDecorations (Graphics2D gfx, Rectangle clip)
    {
        // nothing for now
    }

    /**
     * Renders the dirty sprites and objects in the scene to the given graphics context.
     */
    protected void paintDirtyItems (Graphics2D gfx, Rectangle clip)
    {
        // add any sprites impacted by the dirty rectangle
        _dirtySprites.clear();
        _spritemgr.getIntersectingSprites(_dirtySprites, clip);
        int size = _dirtySprites.size();
        for (int ii = 0; ii < size; ii++) {
            Sprite sprite = _dirtySprites.get(ii);
            Rectangle bounds = sprite.getBounds();
            if (!bounds.intersects(clip)) {
                continue;
            }
            appendDirtySprite(_dirtyItems, sprite);
//             Log.info("Dirtied item: " + sprite);
        }

        // add any objects impacted by the dirty rectangle
        for (SceneObject scobj : _vizobjs) {
            if (!scobj.bounds.intersects(clip)) {
                continue;
            }
            _dirtyItems.appendDirtyObject(scobj);
//             Log.info("Dirtied item: " + scobj);
        }

//         Log.info("paintDirtyItems [items=" + _dirtyItems.size() + "].");

        // sort the dirty items so that we can paint them back-to-front
        _dirtyItems.sort();
        _dirtyItems.paintAndClear(gfx);
    }

    /**
     * A function where derived classes can paint extra stuff while we've
     * got the clipping region set up.
     */
    protected void paintExtras (Graphics2D gfx, Rectangle clip)
    {
        if (isResponsive()) {
            paintIndicators(gfx, clip);
        }
    }

    /**
     * Paint all the appropriate indicators for our scene objects.
     */
    protected void paintIndicators (Graphics2D gfx, Rectangle clip)
    {
        // make sure the indicators are ready
        if (!_indicatorsLaidOut) {
            for (Map.Entry<SceneObject, SceneObjectIndicator> entry : _indicators.entrySet()) {
                SceneObjectIndicator indic = entry.getValue();
                if (!indic.isLaidOut()) {
                    indic.layout(gfx, entry.getKey(), _vbounds);
                    dirtyIndicator(indic);
                }
            }
            _indicatorsLaidOut = true;
        }

        if (checkShowFlag(SHOW_TIPS)) {
            // show all the indicators
            for (SceneObjectIndicator indic : _indicators.values()) {
                paintIndicator(gfx, clip, indic);
            }

        } else {
            // show maybe one indicator
            SceneObjectIndicator indic = _indicators.get(_hobject);
            if (indic != null) {
                paintIndicator(gfx, clip, indic);
            }
        }
    }

    /**
     * Paint the specified indicator if it intersects the clipping rectangle.
     */
    protected void paintIndicator (Graphics2D gfx, Rectangle clip, SceneObjectIndicator tip)
    {
        if (clip.intersects(tip.getBounds())) {
            tip.paint(gfx);
        }
    }

    /**
     * Renders the base and fringe layer tiles that intersect the
     * specified clipping rectangle.
     */
    protected void paintTiles (Graphics2D gfx, Rectangle clip)
    {
        // go through rendering our tiles
        _paintOp.setGraphics(gfx);
        _applicator.applyToTiles(clip, _paintOp);
        _paintOp.setGraphics(null);
    }

    /**
     * Fills the specified tile with the given color at 50% alpha.
     * Intended for debug-only tile highlighting purposes.
     */
    protected void fillTile (
        Graphics2D gfx, int tx, int ty, Color color)
    {
        Composite ocomp = gfx.getComposite();
        gfx.setComposite(ALPHA_FILL_TILE);
        Polygon poly = MisoUtil.getTilePolygon(_metrics, tx, ty);
        gfx.setColor(color);
        gfx.fill(poly);
        gfx.setComposite(ocomp);
    }

    /** Returns the base tile for the specified tile coordinate. */
    protected BaseTile getBaseTile (int tx, int ty)
    {
        SceneBlock block = getBlock(tx, ty);
        return (block == null) ? null : block.getBaseTile(tx, ty);
    }

    /** Returns the fringe tile for the specified tile coordinate. */
    protected BaseTile getFringeTile (int tx, int ty)
    {
        SceneBlock block = getBlock(tx, ty);
        return (block == null) ? null : block.getFringeTile(tx, ty);
    }

    /** Computes the fringe tile for the specified coordinate. */
    protected BaseTile computeFringeTile (int tx, int ty)
    {
        return _ctx.getTileManager().getAutoFringer().getFringeTile(_model, tx, ty, _fringes,
            _masks);
    }

    /**
     * Returns true if we're responding to user input. This is used to control the display of
     * tooltips and other potential user interactions. By default we are always responsive.
     */
    protected boolean isResponsive ()
    {
        return true;
    }

    /** Used by {@link MisoScenePanel#paintTiles}. */
    protected class PaintTileOp implements TileOp
    {
        public void setGraphics (Graphics2D gfx) {
            _gfx = gfx;
            _thw = 0;
            _thh = 0;
            _fhei = 0;
            _fm = null;

            // if we're showing coordinates, we need to do some setting up
            if (gfx != null && _coordsDebug.getValue()) {
                _fm = gfx.getFontMetrics(_font);
                _fhei = _fm.getAscent();
                _thw = _metrics.tilehwid;
                _thh = _metrics.tilehhei;
                gfx.setFont(_font);
            }
        }

        public void apply (int tx, int ty, Rectangle tbounds) {
            // draw the base and fringe tile images
            try {
                Tile tile;
                boolean passable = true;

                if ((tile = getBaseTile(tx, ty)) != null) {
                    tile.paint(_gfx, tbounds.x, tbounds.y);
                    passable = ((BaseTile)tile).isPassable();

                } else {
                    // draw black where there are no tiles
                    Polygon poly = MisoUtil.getTilePolygon(_metrics, tx, ty);
                    _gfx.setColor(Color.black);
                    _gfx.fill(poly);
                }

                if ((tile = getFringeTile(tx, ty)) != null) {
                    tile.paint(_gfx, tbounds.x, tbounds.y);
                    passable = passable && ((BaseTile)tile).isPassable();
                }

                // highlight impassable tiles
                if (_traverseDebug.getValue()) {
                    if (!passable) {
                        // highlight tiles blocked by base or fringe in yellow
                        fillTile(_gfx, tx, ty, Color.yellow);

                    } else if (!canTraverse(null, tx, ty)) {
                        // highlight passable non-traversable tiles in green
                        fillTile(_gfx, tx, ty, Color.green);
                    }
                }

            } catch (ArrayIndexOutOfBoundsException e) {
                log.warning("Whoops, booched it", "tx", tx, "ty", ty, "tb.x", tbounds.x);
                e.printStackTrace(System.err);
            }

            // if we're showing coordinates, do that
            if (_coordsDebug.getValue()) {
                // set the color according to the scene block
                int bx = MathUtil.floorDiv(tx, _metrics.blockwid);
                int by = MathUtil.floorDiv(ty, _metrics.blockhei);
                if (((bx % 2) ^ (by % 2)) == 0) {
                    _gfx.setColor(Color.white);
                } else {
                    _gfx.setColor(Color.yellow);
                }

                // get the top-left screen coordinates of the tile
                int sx = tbounds.x, sy = tbounds.y;

                // draw x-coordinate
                String str = String.valueOf(tx);
                int xpos = sx + _thw - (_fm.stringWidth(str) / 2);
                _gfx.drawString(str, xpos, sy + _thh);

                // draw y-coordinate
                str = String.valueOf(ty);
                xpos = sx + _thw - (_fm.stringWidth(str) / 2);
                _gfx.drawString(str, xpos, sy + _thh + _fhei);

                // draw the tile polygon as well
                _gfx.draw(MisoUtil.getTilePolygon(_metrics, tx, ty));
            }
        }

        protected Graphics2D _gfx;
        protected FontMetrics _fm;
        protected int _thw, _thh, _fhei;
        protected Font _font = new Font("Arial", Font.PLAIN, 7);
    }

    /** Provides access to a few things. */
    protected MisoContext _ctx;

    /** Contains basic scene metrics like tile width and height. */
    protected MisoSceneMetrics _metrics;

    /** The scene model to be displayed. */
    protected MisoSceneModel _model;

    /** Tracks the size at which we were last "rethunk". */
    protected Dimension _rsize = new Dimension();

    /** Contains the tile coords of our upper-left view coord. */
    protected Point _ulpos;

    /** Contains the bounds of our "area of influence" in screen coords. */
    protected Rectangle _ibounds = new Rectangle();

    /** Contains the bounds of our visible "area of influence" in screen coords. */
    protected Rectangle _vibounds = new Rectangle();

    /** Used by {@link #rethink}. */
    protected RethinkOp _rethinkOp;

    /** Contains our scene blocks. See {@link #getBlock} for details. */
    protected ConcurrentHashMap<Integer, SceneBlock> _blocks =
        new ConcurrentHashMap<Integer, SceneBlock>();

    /** A count of blocks in the process of being resolved. */
    protected int _pendingBlocks;

    /** Used to track visible blocks that are waiting to be resolved. */
    protected Set<SceneBlock> _visiBlocks = Sets.newHashSet();

    /** Used to avoid repaints while we don't yet have resolved all the
     * blocks needed to render the visible view. */
    protected boolean _delayRepaint = false;

    /** A list of the potentially visible objects in the scene. */
    protected List<SceneObject> _vizobjs = Lists.newArrayList();

    /**
     * Map of the masks used to calculate fringes in this scene.
     */
    protected Map<Long, BufferedImage> _masks = Maps.newHashMap();

    /**
     * Map of active fringe tiles. Scene blocks have hard references to fringe tiles in this map
     * for the tiles they're using, so the blocks coming in and out of the influential bounds
     * determines which tiles remain in the map. The map is from FringeTile to FringeTile so a
     * fully created FringeTile can be extracted from the map using a tile that contains only
     * what's needed for hashCode and equals: id and passability.
     */
    protected Map<FringeTile, WeakReference<FringeTile>> _fringes =
        new WeakHashMap<FringeTile, WeakReference<FringeTile>>();

    /** The dirty sprites and objects that need to be re-painted. */
    protected DirtyItemList _dirtyItems = new DirtyItemList();

    /** The working sprites list used when calculating dirty regions. */
    protected List<Sprite> _dirtySprites = Lists.newArrayList();

    /** Used to paint tiles. */
    protected PaintTileOp _paintOp = new PaintTileOp();

    /** Temporary point used for intermediate calculations. */
    protected Point _tcoords = new Point();

    /** Used to collect the list of sprites "hit" by a particular mouse
     * location. */
    protected List<Sprite> _hitSprites = Lists.newArrayList();

    /** The list that we use to track and sort the items over which the
     * mouse is hovering. */
    protected DirtyItemList _hitList = new DirtyItemList();

    /** Info on the object that the mouse is currently hovering over. */
    protected Object _hobject;

    /** The item that the user has clicked on with the mouse. */
    protected Object _armedItem = null;

    /** The active radial menu (or null). */
    protected RadialMenu _activeMenu;

    /** Used to track the tile coordinates over which the mouse is hovering. */
    protected Point _hcoords = new Point();

    /** Our object indicators, indexed by the object that they indicate. */
    protected Map<SceneObject, SceneObjectIndicator> _indicators = Maps.newHashMap();

    /** Have the indicators been laid out? */
    protected boolean _indicatorsLaidOut = false;

    /** Flags indicating which features we should show in the scene. */
    protected int _showFlags = 0;

    /** The scene block resolver for this scene panel's context. */
    protected SceneBlockResolver _resolver;

    /** Scene block resolvers shared by all scene panels in a context. */
    protected static Map<MisoContext, SceneBlockResolver> _resolvers =
        new WeakHashMap<MisoContext, SceneBlockResolver>();

    // used to display debugging information on scene block resolution
    protected JFrame _dframe;
    protected ResolutionView _dpanel;

    protected TileOpApplicator _applicator;

    /** A debug hook that toggles debug rendering of traversable tiles. */
    protected static RuntimeAdjust.BooleanAdjust _traverseDebug =
        new RuntimeAdjust.BooleanAdjust(
            "Toggles debug rendering of traversable and impassable tiles in " +
            "the iso scene view.", "narya.miso.iso_traverse_debug_render",
            MisoPrefs.config, false);

    /** A debug hook that toggles debug rendering of tile coordinates. */
    protected static RuntimeAdjust.BooleanAdjust _coordsDebug =
        new RuntimeAdjust.BooleanAdjust(
            "Toggles debug rendering of tile coordinates in the iso scene " +
            "view.", "narya.miso.iso_coords_debug_render",
            MisoPrefs.config, false);

    /** A debug hook that toggles debug rendering of sprite paths. */
    protected static RuntimeAdjust.BooleanAdjust _pathsDebug =
        new RuntimeAdjust.BooleanAdjust(
            "Toggles debug rendering of sprite paths in the iso scene view.",
            "narya.miso.iso_paths_debug_render", MisoPrefs.config, false);

    /** A debug hook that toggles the block resolution display. */
    protected static RuntimeAdjust.BooleanAdjust _resolveDebug =
        new RuntimeAdjust.BooleanAdjust(
            "Enables a view displaying the status of scene block resolution.",
            "narya.miso.iso_paths_debug_resolve", MisoPrefs.config, false);

    /** The stroke used to draw dirty rectangles. */
    protected static final Stroke DIRTY_RECT_STROKE = new BasicStroke(2);

    /** The alpha used to fill tiles for debugging purposes. */
    protected static final Composite ALPHA_FILL_TILE =
        AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);

    /** The default size of the "box" that defines the size of our radial
     * menu circles. */
    protected static final Dimension DEF_RADIAL_RECT = new Dimension(80, 80);
}
