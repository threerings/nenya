//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
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

package com.threerings.util;

import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import static com.threerings.NenyaLog.log;

public class KeyTimerApp
{
    public KeyTimerApp ()
    {
        _frame = new TestFrame();
        _frame.setSize(400, 300);
    }

    public void run ()
    {
        _frame.setVisible(true);
    }

    public static void main (String[] args)
    {
        KeyTimerApp app = new KeyTimerApp();
        app.run();
    }

    protected class TestFrame extends Frame implements KeyListener
    {
        public TestFrame ()
        {
            addKeyListener(this);
            _prStart = _rpStart = -1;
        }

        public void keyPressed (KeyEvent e)
        {
            long now = System.currentTimeMillis();
            _prStart = now;

            if (_rpStart != -1) {
                log.info("RP\t" + (now - _rpStart));
            }

            logKey("keyPressed", e);
        }

        public void keyReleased (KeyEvent e)
        {
            long now = System.currentTimeMillis();
            _rpStart = now;

            log.info("PR\t" + (now - _prStart));

            logKey("keyReleased", e);
        }

        public void keyTyped (KeyEvent e)
        {
            logKey("keyTyped", e);
        }

        /**
         * Logs the given message and key.
         */
        protected void logKey (String msg, KeyEvent e)
        {
            int keyCode = e.getKeyCode();
            log.info(msg + " [key=" + KeyEvent.getKeyText(keyCode) + "].");
        }

        protected long _prStart, _rpStart;
    }

    protected Frame _frame;
}
