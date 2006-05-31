JAVA IMPLEMENTATION OF LUA FOR INTUWAVE - README FOR RELEASE 0.X.Y

David Jones, Ravenbrook Limited

$Date$

CONTENTS

  1. Introduction
  2. Manifest
  3. Installation and Use
  4. Release Notes


1. INTRODUCTION

This is release 0.X.Y of the Java Implementation of Lua for Intuwave
(Jili).

Jili provides an implementation of the Lua language that runs in Java
Mobile Edition environments (specifically, CLDC 1.1 and MIDP 2.0).

The readership of this document is anyone who intends to use the Jili
software.

This document is confidential.


2. MANIFEST

A Jili release consists of a hierarchy of directories and files,
generally supplied as a .zip file.

Here's a brief overview of its contents (not comprehensive):

jili-0.X.Y/	- top level directory.
  readme.txt	- this file.
  build.xml	- Apache Ant build file.  See ant -projecthelp.
  code/		- source code for Jili.
  design/	- design documentation, mostly in HTML.
    design/arch/	- Architecture
  lib/		- imported libraries used in building.
  manual/	- User's Manual.
  test/		- All test code and materials.

In addition various Ant build targets will populate the following
directories:

  bin/			- Installable binaries.  .cod and .jar files.
  compiled/		- Results of compiling code/ (.class files).
  javadoc/		- Public documentation for users.
  javadoc-private/	- Private documentation for maintainers.
  preverified/		- Results of preverification (.class files).
  test-compiled/	- Results of compiling test/ .


3. INSTALLATION


3.1 Prerequisites

You'll need a PC runnings Windows XP (other operating systems may work
but are not supported).

You'll need a JDK from Sun.  Either 1.4.2 or 1.5.0 (note that the
BlackBerry JDE 4.1 requires Sun's JDK 1.5.0).

You'll need Apache Ant.

You'll need RIM's BlackBerry JDE if you intend to use a BlackBerry
device or its emulator.

It's probable that the build procedure requires particular directories
to be on your PATH.  Apologies for the lack of guidance.


3.2 Building

Apache ant is used to encapsulate the build procedure.  See
"ant -projecthelp" for the documented targets.

To install test software onto a USB-connected BlackBerry, go:

  ant load

This compiles the Jili sources (ant compile), preverifies and packages
them into a .jar file (ant jar), converts that to RIM's .cod format (ant
cod), and downloads the .cod file onto an attached device (ant load).

Currently the "ant load" target builds the METestMIDlet which is a
midlet that runs all the tests that run on JME.

If you wish to incorporate Jili into your own software then you'll have
to adapt the build procedure.


4. RELEASE NOTES

Some things don't work, some things do.

----
$Header$
