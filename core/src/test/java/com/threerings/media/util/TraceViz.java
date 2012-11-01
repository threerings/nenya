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

package com.threerings.media.util;

import java.io.File;
import java.io.IOException;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.samskivert.swing.VGroupLayout;
import com.samskivert.swing.util.SwingUtil;

import com.threerings.media.image.ClientImageManager;
import com.threerings.media.image.ImageUtil;

import static com.threerings.media.Log.log;

/**
 * Simple application for testing image trace functionality.
 */
public class TraceViz
{
    public TraceViz (String[] args)
        throws IOException
    {
        // create the frame and image manager
        _frame = new JFrame();
        _frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ClientImageManager imgr = new ClientImageManager(null, _frame);

        // set up the content panel
        JPanel content = (JPanel)_frame.getContentPane();
        content.setBackground(Color.white);
        content.setLayout(new VGroupLayout());

        // load the image
        BufferedImage image = ImageIO.read(new File(args[0]));
        if (image == null) {
            throw new RuntimeException("Failed to read file " +
                                       "[file=" + args[0] + "].");
        }

//         // create a compatible image
//         BufferedImage cimage = imgr.createImage(
//             image.getWidth(null), image.getHeight(null),
//             Transparency.TRANSLUCENT);
//         Graphics g = cimage.getGraphics();
//         g.drawImage(image, 0, 0, null);
//         g.dispose();
//         image = cimage;

        // create the traced image
        Image timage = ImageUtil.createTracedImage(
            imgr, image, Color.red, 5, 0.4f, 0.1f);

        // display the (prepared) original and traced image
        content.add(new JLabel(new ImageIcon(image)));
        content.add(new JLabel(new ImageIcon(timage)));
    }

    public void run ()
    {
        _frame.pack();
        SwingUtil.centerWindow(_frame);
        _frame.setVisible(true);
    }

    public static void main (String[] args)
    {
        try {
            if (args.length == 0) {
                System.out.println(
                    "Usage: java com.threerings.media.util.TraceViz " +
                    "<image file>");
                return;
            }

            TraceViz app = new TraceViz(args);
            app.run();

        } catch (Exception e) {
            log.warning("Failed to run application.", e);
        }
    }

    protected JFrame _frame;
}
