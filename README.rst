Happy New Year!
===============

This is an Android application that plays a "Happy New Year" song with random
notes.  It requires Android Gingerbread v2.3.3 (API 10).  It can be built on
the command line with Ant::

    ant debug

-> bin/HappyNewYear-debug.apk can now be installed on your phone with adb::

   adb install -r -s bin/HappyNewYear-debug.apk

Of course it's also possible to import the project into Eclipse, then build it
and install it from there.


About the code
--------------

This is not a very well synchronised sequencer, it relies a lot on the duration
of the samples.  So sometimes you can hear a glitch between loops.  It plays
the guitar chords continuously, then picks up to 3 vocal notes for each word
and can also add some random vibraphone notes.


What next?
----------

So a first improvement would be to make it more "real-time".  This may involve
some lower level coding in C and JNI as it's not clear whether the required
degree of granularity can be achieved with the Java Android SDK.

Then it should be fairly easy to make it more flexible to use other samples in
different ways.  This could be packed in a "sequence" archive, with sound
samples and sequence rules in a text file.


`verdigris.mu <http://verdigris.mu>`_
-------------------------------------

You can hear what this app sounds like as well as other music, software and
electronics things on `verdigris.mu <http://verdigris.mu/article/3>`_.
