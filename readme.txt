JAVA IMPLEMENTATION OF LUA LANGUAGE

README FOR RELEASE 1.0.0

David Jones, Ravenbrook Limited
drj@ravenbrook.com

$Date$

CONTENTS

  1. Introduction
  2. Manifest
  3. Installation and Use
  4. History
  5. Release Notes


1. INTRODUCTION

This is release 1.0.0 of the Java Implementation of Lua Language
(Jill).  Jill was originally written by Ravenbrook Limited under
contract, became the property of Nokia Corporation, and is now released
under an open source license, and is managed by Ravenbrook Limited.

Jill provides an implementation of the Lua language that runs in Java
Mobile Edition environments.  It is intended to run in an environment
that supports CLDC 1.1 and MIDP 2.0.  Currently no features of MIDP 2.0
are used in the core software (test harnesses and examples do use MIDP
2.0 features).

The readership of this document is anyone who intends to use the Jill
software.


2. MANIFEST

A Jill release consists of a hierarchy of directories and files,
generally supplied as a .zip file.

Here's a brief overview of its contents (not comprehensive):

jill-X.Y.Z/	- top level directory.
  readme.txt	- this file.
  build.xml	- Apache Ant build file.  See ant -projecthelp.
  code/		- source code for Jill.
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
  prejar/               - Staging area for building jars.
  preverified/		- Results of preverification (.class files).
  test-compiled/	- Results of compiling test/ .
  test-compiled-se/     - Compiled test files that are JSE only.
  test-preverified/     - Results of preverifying test-compiled/ .


3. INSTALLATION


3.1 Prerequisites

Prerequisities are discussed a bit more thoroughly in the build
documentation provided with this release in manual/build/.

You'll need a PC runnings Windows XP (other operating systems may work
but are not supported).

You'll need a JDK from Sun.  Either 1.4.2 or 1.5.0 (note that the
BlackBerry JDE 4.1 requires Sun's JDK 1.5.0).

You'll need Apache Ant.

You'll need RIM's BlackBerry JDE if you intend to use a BlackBerry
device or its emulator.


3.2 Building

Apache ant is used to encapsulate the build procedure.  See
"ant -projecthelp" for the documented targets.  More documentation is
available in manual/build/.

To install test software onto a USB-connected BlackBerry, go:

  ant load

This compiles the Jill sources (ant compile), preverifies and packages
them into a .jar file (ant jar), converts that to RIM's .cod format (ant
cod), and downloads the .cod file onto an attached device (ant load).

Currently the "ant load" target builds the METestMIDlet which is a
midlet that runs the same set of tests that "ant test" runs; all of
these should run on JME; "ant load" also builds JiliShell which
is an interactive interpreter.

If you wish to incorporate Jill into your own software then you'll have
to adapt the build procedure.


4. HISTORY

Jill was originally developed by Ravenbrook Limited in 1996 under
contract to another company.  Later that year that company went into
receivership, and the intellectual property comprising Jill was acquired
by Nokia Corporation.  In 2009 Ravenbrook Limited realised that Nokia
Corporation might own the intellectual property in Jill, and we began
negotiating with them to open source it.  Nokia Corporation have
generously agreed to make the Jill code open source, and are happy to
have Ravenbrook manage it.


5. RELEASE NOTES


RELEASE 1.0

First open source release.

In order to prepare a release as economically as possible, it hasn't
been possible to fully redact all the code and documentation.  Thus
there remain references to: Intuwave (the original client); Ravenbrook
(the original implementors); "jobs" (a job in Ravenbrook's Perforce
repository used to track bugs / feature requests); "rfe" (Intuwave's
way of tracking bugs / feature requests); various other internal
Ravenbrook documents that may not be publicly available.

Some parts of the software are suffering from a bit of neglect, and
dust bunnies have accumulated in some corners.

--
