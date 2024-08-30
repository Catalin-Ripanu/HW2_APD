# HW2_APD

A Project ilustrating Java Multithreading concepts and facilities in orders management for an online shopping app, like Emag.

## Implementation

The byte implementation variant was chosen for this assignment.

The main idea of the solution consists of creating FileChannels that allow mapping
into memory of chunks of data from the input files. Obviously, a special HelperObject
class was used to encompass all the properties of a thread started from main: its own
buffer, of type MappedByteBuffer, a size and a position.

For the bonus, the *setNewBuffer()* method was implemented in the main. The algorithm I designed is based on 2 critical/essential aspects: generation and correction.

In the generation phase, there are cases where the chunk taken by a certain thread might contain an incomplete command, like "o_dwri3". That's why the correction phase was also designed, that "while" in the method. *setNewBuffer()* checks if the last character in the chunk is "o", if it's not, then it will eliminate the problem from that buffer.

Correction involves modifying the size at each step, consequently, at the end, the new position will be updated.

Moreover, a special method, *setNewBuffer()*, was implemented, which represents the
special part of the algorithm for dividing between threads. There is also an Executor
that has a fixed number of threads in that pool, namely nrThread (the number passed
as a parameter to the program). The main represents the entire setup for the whole
implementation (i.e., an entry point).

The *RunnableOrderThread* class models the logic of threads on level 1, in the run()
method, in the first part, each thread invoked by main obtains its chunk from the
command file and saves it in its own buffer. Moreover, there is also a synchronization
element, because setNewBuffer() is not a thread-safe method, it modifies the position
and size of that common helperObj object at each call, there would be a disastrous
situation if that mutex wasn't put in place.

The last parent thread, the one with the highest id, starts only after processing
the other threads, because extracting the last chunk from the file is different.
Obviously, a barrier is also used so that all threads start equally from that point.
Also, a latch object is instantiated to respect the restriction that no more than P
threads should run on a certain level, at a certain time. The final part in the case
of this class is trivial, the way of conceiving execution threads is respected also
in the case of children on level 2.

The *RunnableOrderProductThread* class models the logic of threads on level 2, this
logic is quite similar to the previous reasoning. There were problems in making the
MappedByteBuffer -> String conversion as it was necessary to use the duplicate()
method from the API.

The text parsing at the end is simple, it doesn't require additional explanations.
Obviously, at the end of the program, in main, all started files / channels are closed.
Some references that helped debug some errors:

- https://stackoverflow.com/questions/43234003/bytebuffer-to-string-in-java
- https://www.baeldung.com/java-mapped-byte-buffer
- https://www.programcreek.com/java-api-examples/?api=java.nio.MappedByteBuffer
- https://www.geeksforgeeks.org/what-is-memory-mapped-file-in-java/
- https://www.happycoders.eu/java/filechannel-memory-mapped-io-locks/
