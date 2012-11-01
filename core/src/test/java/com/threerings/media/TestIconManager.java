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

package com.threerings.media;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.samskivert.swing.HGroupLayout;
import com.samskivert.swing.util.SwingUtil;

import com.threerings.resource.ResourceManager;

import com.threerings.media.image.ClientImageManager;
import com.threerings.media.tile.TileManager;

/**
 * Does something extraordinary.
 */
public class TestIconManager
{
    public static void main (String[] args)
    {
        try {
            JFrame frame = new JFrame("TestIconManager");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            ResourceManager rmgr = new ResourceManager("rsrc");
            ClientImageManager imgr = new ClientImageManager(rmgr, frame);
            TileManager tmgr = new TileManager(imgr);
            IconManager iconmgr = new IconManager(
                tmgr, "rsrc/config/media/iconmgr.properties");

            JPanel panel = new JPanel(new HGroupLayout());
            for (int i = 0; i < 8; i++) {
                panel.add(new JButton(iconmgr.getIcon("test", i)));
            }

            frame.getContentPane().add(panel, BorderLayout.CENTER);
            frame.pack();
            SwingUtil.centerWindow(frame);
            frame.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
