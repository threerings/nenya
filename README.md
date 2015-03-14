The Nenya library
=================

The Nenya library provides various facilities for making networked multiplayer
games. Its various packages include:

* geom, util - basic tools for doing data structure manipulation and some
  geometry math
* resource - tools for bundling, deploying and managing media (images,
  sounds, etc.) with a game
* media - a framework for doing "active" rendering in Java
* media.image - tools for loading, caching, manipulating and displaying images
* media.sound - tools for loading, caching, and playing audio
* media.animation, media.sprite - works in concert with the active
  rendering system and provides tools for defining and manipulating
  sprites (graphical entities that follow paths) and animations
  (graphical entities that affect the display in other ways)
* miso - a framework for defining and displaying isometrically rendered scenes
* cast - a framework for defining and using recolorable, composited
  characters with different poses and actions

[Javadoc documentation](http://threerings.github.com/nenya/apidocs/) is provided.

Tutorial style documentation is somewhat sparse at the moment, but inspection
of the code in the `src/test/java` directory shows examples of use of many
features of the library.

Building
--------

The library is built using Maven, or [Ant](http://ant.apache.org/).

The Maven build uses the standard targets:

    package: builds the code and creates the jars and swcs
    install: builds and installs the artifacts into your local Maven repository
    test: builds and runs the unit tests

The Ant build uses the following targets:

    all: builds the distribution files and javadoc documentation
    compile: builds only the class files (dist/classes)
    javadoc: builds only the javadoc documentation (dist/docs)
    dist: builds the distribution jar files (dist/*.jar)

Artifacts
---------

Nenya provides three different build artifacts, for differing purposes:

* nenya: contains the main Java library; exports dependencies only for
  libraries which are required by a running client or server.
* nenya-tools: contains the media precompilation portions of Nenya which one
  need integrate into their build; exports dependencies for libraries needed
  when running precompilation.
* nenyalib: contains the main ActionScript library; exports dependencies for
  libraries needed when building a SWF using nenyalib.

Nenya is published to Maven Central. To add a Nenya dependency to a
Maven project, add the following to your `pom.xml`:

    <dependencies>
      <dependency>
        <groupId>com.threerings</groupId>
        <artifactId>nenya</artifactId>
        <version>1.6</version>
      </dependency>
    </dependencies>

To add it to an Ivy, SBT, or other Maven repository using project, simply
remove the vast majority of the boilerplate above.

If you prefer to download pre-built binaries, those can be had here:

* [nenya-1.6.jar](http://repo2.maven.org/maven2/com/threerings/nenya/1.6/nenya-1.6.jar)
* [nenya-tools-1.6.jar](http://repo2.maven.org/maven2/com/threerings/nenya-tools/1.6/nenya-tools-1.6.jar)
* [nenyalib-1.6.swc](http://repo2.maven.org/maven2/com/threerings/nenyalib/1.6/nenyalib-1.6.swc)

Distribution
------------

The Nenya library is released under the LGPL. The most recent version of the
library is available at http://github.com/threerings/nenya

Contact
-------

Questions, comments, and other worldly endeavors can be handled via the [Three
Rings Libraries](http://groups.google.com/group/ooo-libs) Google Group.

Nenya is actively developed by the scurvy dogs at
[Three Rings](http://www.threerings.net) Contributions are welcome.
