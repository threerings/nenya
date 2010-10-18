//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
// http://code.google.com/p/nenya/
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

package com.threerings.jme.util;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.BandCombineOp;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;

import com.google.common.collect.Maps;

import com.jme.image.Image;
import com.jmex.bui.BImage;

import com.threerings.media.image.Colorization;
import com.threerings.media.image.ImageUtil;

import com.threerings.resource.ResourceManager;

import static com.threerings.jme.Log.log;

/**
 * Manages a weak cache of image data to make life simpler for callers that don't want to worry
 * about coordinating shared use of the same images.
 */
public class ImageCache
{
    /**
     * Creates a JME-compatible image from the supplied buffered image.
     */
    public static Image createImage (BufferedImage bufimg, boolean flip)
    {
        return createImage(bufimg, 1f, flip);
    }

    /**
     * Colorizes the supplied buffered image (which must be an 8-bit colormapped image), then
     * converts the colorized image into a form that JME can display.
     */
    public static Image createImage (BufferedImage bufimg, Colorization[] zations, boolean flip)
    {
        return createImage(bufimg, zations, 1f, flip);
    }

    /**
     * Colorizes the supplied buffered image (which must be an 8-bit colormapped image), then
     * converts the colorized image into a form that JME can display.
     */
    public static Image createImage (BufferedImage bufimg, Colorization[] zations, float scale,
                                     boolean flip)
    {
        return createImage(ImageUtil.recolorImage(bufimg, zations), scale, flip);
    }

    /**
     * Creates a JME-compatible image from the supplied buffered image.
     */
    public static Image createImage (BufferedImage bufimg, float scale, boolean flip)
    {
        // make sure images are square powers of two
        int width = (int)(bufimg.getWidth() * scale);
        int height = (int)(bufimg.getHeight() * scale);
        int tsize = nextPOT(Math.max(width, height));

        // convert the the image to the format that OpenGL prefers
        BufferedImage dispimg = createCompatibleImage(
            tsize, tsize, bufimg.getColorModel().hasAlpha());

        // flip the image to convert into OpenGL's coordinate system
        AffineTransform tx = null;
        if (flip) {
            tx = AffineTransform.getScaleInstance(scale, -scale);
            tx.translate((tsize - width) / 2, (height - tsize) / 2 - bufimg.getHeight());
        }

        // "convert" the image by rendering the old into the new
        Graphics2D gfx = (Graphics2D)dispimg.getGraphics();
        gfx.drawImage(bufimg, tx, null);
        gfx.dispose();

        // now extract the image data into a JME image
        return convertImage(dispimg);
    }

    /**
     * Creates a buffered image in a format compatible with LWJGL with the specified dimensions.
     *
     * @param transparent if true, the image will be four bytes per pixel (RGBA), if false it will
     * be three (and have no alpha channel).
     */
    public static BufferedImage createCompatibleImage (int width, int height, boolean transparent)
    {
        if (transparent) {
            return new BufferedImage(GL_ALPHA_MODEL, Raster.createInterleavedRaster(
                                         DataBuffer.TYPE_BYTE, width, height, 4, null),
                                     false, null);
        } else {
            return new BufferedImage(GL_OPAQUE_MODEL, Raster.createInterleavedRaster(
                                         DataBuffer.TYPE_BYTE, width, height, 3, null),
                                     false, null);
        }
    }

    /**
     * Converts an image that was created with {@link #createCompatibleImage} into a JME {@link
     * Image}. The data is assumed to have already been "flipped".
     */
    public static Image convertImage (BufferedImage bufimg)
    {
        Image image = new Image();
        image.setType(bufimg.getColorModel().hasAlpha() ? Image.RGBA8888 : Image.RGB888);
        image.setWidth(bufimg.getWidth());
        image.setHeight(bufimg.getHeight());
        image.setData(convertImage(bufimg, null));
        return image;
    }

    /**
     * Converts the supplied image (which must have been created with {@link
     * #createCompatibleImage}) into a {@link ByteBuffer} that can be passed to {@link
     * Image#setData}.
     *
     * @param target the results of a previous call to {@link #convertImage} that will be
     * overwritten or null if a new buffer should be allocated. Of course a reused buffer must be
     * used with the same image or one with the exact same configuration.
     */
    public static ByteBuffer convertImage (BufferedImage image, ByteBuffer target)
    {
        DataBufferByte dbuf = (DataBufferByte)image.getRaster().getDataBuffer();
        byte[] data = dbuf.getData();
        if (target == null) {
            target = ByteBuffer.allocateDirect(data.length);
            target.order(ByteOrder.nativeOrder());
        }
        target.clear();
        target.put(data);
        target.flip();
        return target;
    }

    /**
     * Creates an image cache that will obtain its image data from the supplied resource manager.
     */
    public ImageCache (ResourceManager rsrcmgr)
    {
        _rsrcmgr = rsrcmgr;
    }

    /**
     * See {@link #getImage(String,boolean)}.
     */
    public Image getImage (String rsrcPath)
    {
        return getImage(rsrcPath, 1f, true);
    }

    /**
     * See {@link #getImage(String,float,boolean)}.
     */
    public Image getImage (String rsrcPath, float scale)
    {
        return getImage(rsrcPath, scale, true);
    }

    /**
     * Loads up an image from the cache if possible or from the resource manager otherwise, in
     * which case it is prepared for use by JME and OpenGL. <em>Note:</em> these images are cached
     * separately from the {@link BImage} and {@link BufferedImage} caches.
     *
     * @param flip whether or not to convert the image from normal computer coordinates into OpenGL
     * coordinates when loading. <em>Note:</em> this information is not cached, an image must
     * <em>always</em> be requested as flipped or not flipped.
     */
    public Image getImage (String rsrcPath, boolean flip)
    {
        return getImage(rsrcPath, 1f, flip);
    }

    /**
     * Loads up an image from the cache if possible or from the resource manager otherwise, in
     * which case it is prepared for use by JME and OpenGL. <em>Note:</em> these images are cached
     * separately from the {@link BImage} and {@link BufferedImage} caches.
     *
     * @param flip whether or not to convert the image from normal computer coordinates into OpenGL
     * coordinates when loading. <em>Note:</em> this information is not cached, an image must
     * <em>always</em> be requested as flipped or not flipped.
     * @param scale a scale factor to apply to the image.  As with the flip setting, the scale
     * factor is not cached.
     */
    public Image getImage (String rsrcPath, float scale, boolean flip)
    {
        // first check the cache
        WeakReference<Image> iref = _imgcache.get(rsrcPath);
        Image image;
        if (iref != null && (image = iref.get()) != null) {
            return image;
        }

        // load the image data from the resource manager
        BufferedImage bufimg;
        try {
            bufimg = _rsrcmgr.getImageResource(rsrcPath);
        } catch (Throwable t) {
            log.warning("Unable to load image resource " +
                    "[path=" + rsrcPath + "].", t);
            // cope; return an error image of abitrary size
            bufimg = ImageUtil.createErrorImage(64, 64);
        }

        // create and cache a new JME image with the appropriate data
        image = createImage(bufimg, scale, flip);
        _imgcache.put(rsrcPath, new WeakReference<Image>(image));
        return image;
    }

    /**
     * Loads up an image from the cache if possible or from the resource manager otherwise, in
     * which case it is prepared for use by BUI.  <em>Note:</em> these images are cached separately
     * from the {@link Image} and {@link BufferedImage} caches.
     */
    public BImage getBImage (String rsrcPath)
    {
        return getBImage(rsrcPath, 1f, false);
    }

    /**
     * Loads up an image from the cache if possible or from the resource manager otherwise, in
     * which case it is prepared for use by BUI.  <em>Note:</em> these images are cached separately
     * from the {@link Image} and {@link BufferedImage} caches.
     *
     * @param scale If none-unity the image will be scaled by the supplied factor before being
     * converted into a {@link BImage}.
     * @param returnNull If set to true, will return a null instead of generating an error image on
     * failure
     */
    public BImage getBImage (String rsrcPath, float scale, boolean returnNull)
    {
        // first check the cache
        String key = rsrcPath + ":" + scale;
        WeakReference<BImage> iref = _buicache.get(key);
        BImage image;
        if (iref != null && (image = iref.get()) != null) {
            return image;
        }

        // load the image data from the resource manager
        BufferedImage bufimg;
        try {
            bufimg = _rsrcmgr.getImageResource(rsrcPath);
        } catch (Throwable t) {
            if (returnNull) {
                return null;
            }
            log.warning("Unable to load image resource [path=" + rsrcPath + "].", t);
            // cope; return an error image of abitrary size
            bufimg = ImageUtil.createErrorImage(64, 64);
        }

        // create and cache a new BUI image with the appropriate (scaled if necessary) data
        image = new BImage(scale == 1f ? bufimg :
                           bufimg.getScaledInstance(Math.round(bufimg.getWidth()*scale),
                                                    Math.round(bufimg.getHeight()*scale),
                                                    BufferedImage.SCALE_SMOOTH), true);
        _buicache.put(key, new WeakReference<BImage>(image));
        return image;
    }

    /**
     * Loads up a silhouette image (in which all non-transparent pixels are set to black) from the
     * cache if possible or from the resource manager otherwise, in which case it is prepared for
     * use by BUI.
     */
    public BImage getSilhouetteBImage (String rsrcPath, boolean returnNull)
    {
        // first check the cache
        String key = "silhouette:" + rsrcPath;
        WeakReference<BImage> iref = _buicache.get(key);
        BImage image;
        if (iref != null && (image = iref.get()) != null) {
            return image;
        }

        // load the image data from the resource manager
        BufferedImage bufimg, silimg;
        try {
            bufimg = _rsrcmgr.getImageResource(rsrcPath);

            // now turn it into a silhouette
            silimg = new BufferedImage(bufimg.getWidth(), bufimg.getHeight(),
                                       BufferedImage.TYPE_4BYTE_ABGR);
            new BandCombineOp(NON_ALPHA_TO_BROWN, null).filter(
                bufimg.getRaster(), silimg.getRaster());

        } catch (Throwable t) {
            if (returnNull) {
                return null;
            }
            log.warning("Unable to load image resource [path=" + rsrcPath + "].", t);
            // cope; return an error image of abitrary size
            silimg = ImageUtil.createErrorImage(64, 64);
        }

        // create and cache a new BUI image with the appropriate data
        image = new BImage(silimg, true);
        _buicache.put(key, new WeakReference<BImage>(image));
        return image;
    }

    /**
     * Colorizes the image with supplied path (which must be an 8-bit colormapped image), then
     * converts the colorized image into a form that JME can display.
     */
    public BImage createColorizedBImage (String path, Colorization[] zations, boolean flip)
    {
        return new BImage(ImageUtil.recolorImage(getBufferedImage(path), zations), flip);
    }

    /**
     * Loads up a buffered image from the cache if possible or from the resource manager
     * otherwise. <em>Note:</em> these images are cached separately from the {@link Image} and
     * {@link BImage} caches.
     */
    public BufferedImage getBufferedImage (String rsrcPath)
    {
        // first check the cache
        WeakReference<BufferedImage> iref = _bufcache.get(rsrcPath);
        BufferedImage image;
        if (iref != null && (image = iref.get()) != null) {
            return image;
        }

        // load the image data from the resource manager
        try {
            image = _rsrcmgr.getImageResource(rsrcPath);
        } catch (Throwable t) {
            log.warning("Unable to load image resource [path=" + rsrcPath + "].", t);
            // cope; return an error image of abitrary size
            image = ImageUtil.createErrorImage(64, 64);
        }

        _bufcache.put(rsrcPath, new WeakReference<BufferedImage>(image));
        return image;
    }

    /** Rounds the supplied value up to a power of two. */
    protected static int nextPOT (int value)
    {
        return (Integer.bitCount(value) > 1) ? (Integer.highestOneBit(value) << 1) : value;
    }

    /** Provides access to our image data. */
    protected ResourceManager _rsrcmgr;

    /** A cache of {@link Image} instances. */
    protected HashMap<String,WeakReference<Image>> _imgcache = Maps.newHashMap();

    /** A cache of {@link BImage} instances. */
    protected HashMap<String,WeakReference<BImage>> _buicache = Maps.newHashMap();

    /** A cache of {@link BufferedImage} instances. */
    protected HashMap<String,WeakReference<BufferedImage>> _bufcache = Maps.newHashMap();

    /** Used to create buffered images in a format compatible with OpenGL. */
    protected static ComponentColorModel GL_ALPHA_MODEL = new ComponentColorModel(
        ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] { 8, 8, 8, 8 },
        true, false, ComponentColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);

    /** Used to create buffered images in a format compatible with OpenGL. */
    protected static ComponentColorModel GL_OPAQUE_MODEL = new ComponentColorModel(
        ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] { 8, 8, 8, 0 },
        false, false, ComponentColorModel.OPAQUE, DataBuffer.TYPE_BYTE);

    /** An operation that converts all channels except alpha to brown. */
    protected static final float[][] NON_ALPHA_TO_BROWN = {
        { 0, 0, 0, 0x66/255f },
        { 0, 0, 0, 0x57/255f },
        { 0, 0, 0, 0x34/255f },
        { 0, 0, 0, 0.5f }
    };
}
