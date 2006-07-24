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
  prejar/               - Staging area for building jars.
  preverified/		- Results of preverification (.class files).
  test-compiled/	- Results of compiling test/ .
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

This compiles the Jili sources (ant compile), preverifies and packages
them into a .jar file (ant jar), converts that to RIM's .cod format (ant
cod), and downloads the .cod file onto an attached device (ant load).

Currently the "ant load" target builds the METestMIDlet which is a
midlet that runs all the tests that run on JME, and also JiliShell which
is an interactive interpreter.

If you wish to incorporate Jili into your own software then you'll have
to adapt the build procedure.


4. RELEASE NOTES


RELEASE 0.8.0

This release is intended to meet the planned "String and Table Library"
release.

The string, table, and os libraries are provided.


There are the following known bugs in this release:

(all of these were present in earlier releases)

load (in the base library) does not accept a file beginning with '#'.
(Ravenbrook job001436).

t[nil] does not work (Ravenbrook job001430).


RELEASE 0.7.0

This release is intended to meet the planned "API and Metamethods".  It
also has a partial compiler.

A complete Lua API is provided, and the execution of the Lua language is
complete, including metamethods.

The following notable improvements have been made:

Jili is now in the package mnj.lua.

Lua source can now be compiled and executed on the target.  This
functionality is not complete yet however.

Metamethods are now complete supported in the VM.

JiliShell is provided.  This is a MIDlet that allows Lua to be executed
interactively.  It is built by "ant cod" and placed in the bin/
directory.  It is also used to demonstrate what size jar file is needed
(about 83KB currently).

The "t.v = nil" bug is fixed (Ravenbrook job001451).

"x^y" now works, but not necessarily for all corner cases.

API is complete and documented.


There are the following known bugs in this release:

(all of these were present in earlier releases)

load (in the base library) does not accept a file beginning with '#'.
(Ravenbrook job001436).

t[nil] does not work (Ravenbrook job001430).


RELEASE 0.6.0

This release is intended to meet the planned "Base Library".

Lua's base library is provided and much of the Jili public API (the
documentation for which is available by going "ant doc") is also
provided.

The following notable improvements have been made:

"ant style" performs a Checkstyle coding standards style check.  Much of
the code has been modified to conform more to these conventions.

build.xml has been cleaned up and supports multiple platforms more
clearly and more easily.


There are the following known bugs in this release:

load (in the base library) does not accept a file beginning with '#'.
(Ravenbrook job001436).

t.v = nil does not work (Ravenbrook job001451).

Wrong package (Ravenbrook job001398).

t[nil] does not work (Ravenbrook job001430).  This was known from the
previous release.

x^y does not work (Ravenbrook job001405).  This is not likely to be
fixed.


RELEASE 0.5.0

This release is intended to meet the planned "Full VM".

All Lua expressions and statements are supported (there is no compiler,
so all Lua scripts must be pre-compiled), with the following noted
exceptions: Lua's power operator (x^y) is not supported; accessing a
table with a nil key (t[nil]) is no supported yet.  The power operator
requires a substantial amount of code in order to be implemented
properly, and it's not clear whether this is justified yet.  "t[nil]"
has a new meaning in Lua 5.1 (compared to Lua 5.0), it is intended to be
supported, but given the relative obscurity of the operation, its lack
of support is not seen as a problem at the moment.

The following notable improvements have been made since the last
release:

For loops (both varieties).

API names now consitently use Java style naming convention.

Partial base and string libraries are provided.

Bug fix: Lua equality for tables was wrong, now fixed.


RELEASE 0.4.0

This release is intended to meet the planned "Calculator Mode VM".

The following notable improvements have been made since the last
release:

readme.txt (this file) has been added.

Functions.

Little-endian (Intel) compiled binary chunks.

Tests moved to test/

Various bug fixes, including conversion of strings to numbers and
numbers to strings.



----
$Header$
