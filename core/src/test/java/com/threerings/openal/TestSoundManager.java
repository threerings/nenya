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

package com.threerings.openal;

import java.io.File;
import java.io.IOException;

import com.samskivert.util.Interval;
import com.samskivert.util.Queue;
import com.samskivert.util.RunQueue;

/**
 * Tests the OpenAL sound system.
 */
public class TestSoundManager
{
    public static void main (String[] args)
    {
        if (args.length == 0) {
            System.err.println("Usage: TestSoundManager sound.[wav|mp3|ogg]");
            System.exit(-1);
        }

        final Thread current = Thread.currentThread();
        RunQueue rqueue = new RunQueue() {
            public void postRunnable (Runnable r) {
                _queue.append(r);
            }
            public boolean isDispatchThread () {
                return (current == Thread.currentThread());
            }
            public boolean isRunning() {
                return true;
            }
        };

        final SoundManager smgr = SoundManager.createSoundManager(rqueue);
        ClipProvider provider = new WaveDataClipProvider();
        final SoundGroup group = smgr.createGroup(provider, 5);
        final String path = args[0];

        String lpath = path.toLowerCase();
        if (lpath.endsWith("mp3") || lpath.endsWith(".ogg")) {
            // play the mp3/ogg file in a loop
            try {
                new FileStream(smgr, new File(args[0]), true).play();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Interval i = new Interval(rqueue) {
                @Override public void expired () {
                    smgr.updateStreams(0.1f);
                }
            };
            i.schedule(100L, true);
        } else {
            // queue up an interval to play a sound over and over
            Interval i = new Interval(rqueue) {
                @Override public void expired () {
                    Sound sound = group.getSound(path);
                    sound.play(true);
                }
            };
            i.schedule(100L, true);
        }

        while (true) {
            Runnable r = _queue.get();
            r.run();
        }
    }

    protected static Queue<Runnable> _queue = new Queue<Runnable>();
}
