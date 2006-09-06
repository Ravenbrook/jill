JAVA IMPLEMENTATION OF LUA FOR INTUWAVE - README FOR RELEASE 0.X.Y

David Jones, Ravenbrook Limited
drj@ravenbrook.com

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
Mobile Edition environments.  It is intended to run in an environment
that supports CLDC 1.1 and MIDP 2.0.  Currently no features of MIDP 2.0
are used in the core software (test harnesses and examples do use MIDP
2.0 features).

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

This compiles the Jili sources (ant compile), preverifies and packages
them into a .jar file (ant jar), converts that to RIM's .cod format (ant
cod), and downloads the .cod file onto an attached device (ant load).

Currently the "ant load" target builds the METestMIDlet which is a
midlet that runs the same set of tests that "ant test" runs; all of
these should run on JME; "ant load" also builds JiliShell which
is an interactive interpreter.

If you wish to incorporate Jili into your own software then you'll have
to adapt the build procedure.


4. RELEASE NOTES


RELEASE 0.16.0

This release is intended to improve the speed of execution of Lua
scripts.

The internal implementation of the VM has been changed to reduce
allocation of transient garbage, particular of Double instances.


RELEASE 0.15.0

This release is intended to improve the speed of execution of Lua
scripts.

The implementation of tables has been revised to improve the case when
a Lua table is used like an array (many integer keys contiguous with 1).

Some scripts run in 23% of the time they took in release 0.14.0.


RELEASE 0.14.0

This release is intended to improve the speed of execution of Lua
scripts.

Measurements taken by Ravenbrook, using SpeedMIDlet, indicate a speed
increase over 0.13.0 of approximately 20% on the BlackBerry 8700g.


RELEASE 0.13.0

This release is a maintenance release.

Lua threads can now be pre-empted by installing a hook.  See Lua.setHook
and (more usefully probably) the example in test/mnj/lua/HookTest, the
preempt method runs the scripts in the speed/ directory concurrently.

A Lua thread can now be suspended by throwing an instance of
java.lang.RuntimeException.  See the documentation for Lua.yield and the
test program mnj.lua.CoroTest.test10.

No other bugs have been fixed, nor have any new ones been discovered
since release 0.12.0.


RELEASE 0.12.0

This release is a maintenance release.

Executable example of how to run multiple cooperating Lua scripts is in
test/mnj/lua/MultiTask.java (and .lua) and can be executed (in JSE) by
running "ant multitask".


The following bugs have been fixed in this release:

Ravenbrook job001510.  table.concat{1,2,3} and similar produced the
wrong results.  This is now fixed.  This bug was present ever since 
table.concat was provided in release 0.8.0 but no test invoked this
case.

Ravenbrook job001514.  It is not clear how Intuwave use Jili as an m-Network
task.  This is hopefully clarified somewhat by the
test/mnj/lua/MultiTask.java example, but the job is left open, as I
expect more work / support.


Plus loads of previous bugs still open and unfixed.


RELEASE 0.11.0

This release is a maintenance release, intended to improve quality.

The advanced acceptance tests (as modified by Ravenbrook and executed
with "ant advanced") run to completion almost without error.


There are the following known bugs in this release:

The advanced acceptance tests do not run correctly (Ravenbrook
job001480).  There are two issues here: one is a string.dump/load issue
(see below), the other is that Java out of memory errors do not get
translated into Lua errors.

The output of string.format when using scientific notation ('%e' or
sufficiently big or small numbers using '%g') does not always produce
two digits after the 'e' (or 'E').  This may differ from the output
produced by PUC-Rio Lua (which in any case depends on the underlying C
implementation, and that does differ between different implementations).
Ravenbrook job001466.

Jili allows a table to be indexed with NaN.  PUC-Rio does not allow this
and raises a Lua error.  Ravenbrook job001470.

Jili allows arithmetic on hex strings. [[1+'a']] produces a result when
it should produce a Lua error.  Ravenbrook job001498.

load cannot accept the output from string.dump.  This is one of the
defects exhibited by the advanced acceptance tests.  Arguably, this
behaviour falls outside the documented contract for string.dump. Ravenbrook
job001505.

(and this perennial from previous releases)

load (in the base library) does not accept a file beginning with '#'.
(Ravenbrook job001436).


Known bugs from previous releases that have been fixed:

Mystery string.dump/loadstring problem is no longer a mystery (was
Ravenbrook job001483, but replaced by Ravenbrook job001505).

t[nil] now works (Ravenbrook job001430).


Other bugs that have been fixed:

nil is now modelled by a nonce object, not by null.  Ravenbrook jobs
job001391 and job001430.

Errors for < (and <=) are now generated properly.  Ravenbrook job001419.

Many bugs and test modifications in order to improve the execution of
the advanced acceptance tests (Ravenbrook job001480).

Fixed crash (Java exception) for some uses of '%bxy' in string.find and
similar.  Ravenbrook job001492.

Removed a stub implementation of the undocumented gcinfo function (this
was used by an earlier version of the acceptance tests and caused a
misleading error).
Ravenbrook job001493.

Error message for type errors in arithmetic expressions (like 1+'x')
specified the incorrect type for the operand.  Now it's fixed.
Ravenbrook job001497.

Metamethod invocation was incorrect when one of the operands was a
constant (eg t+1 where t had an __add metamethod).  Now it's fixed.
Ravenbrook job001499.

Error values produced by loadstring in error cases (syntax errors etc)
were not correct.  Now they are.  Ravenbrook job001504.


RELEASE 0.10.0

This release is intended to meet the planned "Full Compiler" release.
It was intended to be number 1.0.0, but the number of outstanding
defects warrant an 0.x release.

This release should be fully functional in all intended areas (the major
lacunae being the debug and io library and weakness).

The basic acceptance tests run to completion and should produce
acceptable output.  Try "ant a-test"

Many of the advanced acceptances tests have required modification to
remove (or change) the use of deprecated features.  Modified versions
can be found in test\mnj\lua\accept-advanced and the test suite can be
run with "ant advanced-test" (there are several failures at the moment).


There are the following known bugs in this release:

The advanced acceptance tests do not run correctly (Ravenbrook
job001480).  Really this is many separate bugs, some of which are
mentioned here.

Mystery string.dump/loadstring problem (Ravenbrook job001483).

(and these from previous releases that are  still present)

load (in the base library) does not accept a file beginning with '#'.
(Ravenbrook job001436).

t[nil] does not work (Ravenbrook job001430).


Known bugs from previous releases that have been fixed:

From release 0.9.0: require"os" (and similar) does not work
(Ravenbrook job001472).


Other bugs that have been fixed:

tostring(-0) is now the same as PUC-Rio (Ravenbrook job001423).

string.format should now handle all combinations of precision and %e,
%f, and %g formats more correctly (Ravenbrook job001466). And also
handles %i and %u (Ravenbrook job001484, Intuwave defect 10333).

"x and 7" bug in the compiler.  Ravenbrook job001477 (this was reported
by Intuwave too)

getfenv now supports some numeric arguments.  Ravenbrook job001478.  We
said we weren't going to do this, but it turned out to be simpler to add
than to change the existing uses in the tests (Lua.where requires much
of the internal mechanics anyway).

Many bugs fixed, and some of the test's use of deprecated features
removed, in support of getting the advanced acceptance tests running.
Ravenbrook job001480.

Expressions like "{unpack(x)}" failed when x had many elements.
Ravenbrook job0001481, Intuwave defect 10332.

Various errors on resuming on a coroutine were not handled correctly
(Ravenbrook job001486).

Can now get the metatable for a thread (coroutine) (Ravenbrook
job001487).

The 0-length pattern '' did not work for string.gsub, now it does
(Ravenbrook job001489)


RELEASE 0.9.0

This release is intended to meet the planned "Module and Coroutine
Library" release.

The package (module), coroutine, and math libraries are provided.  All
of them are cut down from their PUC-Rio counterparts to some extent.


There are the following known bugs in this release:

require"os" (and similar) does not work (Ravenbrook job001472)

(and these from previous releases that are still present)

load (in the base library) does not accept a file beginning with '#'.
(Ravenbrook job001436).

t[nil] does not work (Ravenbrook job001430).


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
