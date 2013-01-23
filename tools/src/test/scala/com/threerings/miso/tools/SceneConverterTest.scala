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

package com.threerings.miso.tools

import org.junit.Test
import org.junit.Assert._

import playn.core.json.JsonImpl

import com.threerings.miso.data.SimpleMisoSceneModel
import com.threerings.miso.tools.json.SimpleMisoSceneWriter
import com.threerings.miso.tools.xml.SimpleMisoSceneParser

/**
 * Tests the Json scene writing code.
 */
class SceneConverterTest
{
  @Test def convertScene () {
    val path = "rsrc/scenes/idyll.xml"
    val in = getClass.getClassLoader.getResourceAsStream(path)
    assertTrue("Missing file " + path, in != null)
    val parser = new SimpleMisoSceneParser("")
    val model = parser.parseScene(in)
    assertTrue("No miso scene found in " + path, model != null)
    val out = new JsonImpl().newWriter
    SimpleMisoSceneWriter.write(model, out)
    // println(out.write)
  }
}
