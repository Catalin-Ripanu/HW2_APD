# HW2_APD

## Implementation Overview

This project demonstrates Java multithreading concepts and facilities for managing orders in an online shopping application, similar to Emag. The byte implementation variant was chosen for this assignment.

### Key Components

1. **FileChannels**: Allow mapping of data chunks from input files into memory.
2. **HelperObject class**: Encapsulates thread properties including:
   - MappedByteBuffer
   - Size
   - Position

3. **setNewBuffer() method**: Implements the bonus feature for dividing work between threads.
4. **Executor**: Uses a fixed thread pool of size nrThread (passed as a program parameter).
5. **Main method**: Serves as the entry point and sets up the entire implementation.

## Thread Logic

### RunnableOrderThread Class (Level 1 Threads)

- Models the logic for level 1 threads.
- In the run() method:
  1. Each thread obtains its chunk from the command file and saves it in its buffer.
  2. Uses synchronization to ensure thread-safety when calling setNewBuffer().
  3. The last parent thread (highest id) starts after other threads finish processing.
- Uses a barrier to ensure all threads start equally from a certain point.
- Implements a latch object to limit the number of concurrent threads to P on each level.
- Creates and manages level 2 execution threads.

### RunnableOrderProductThread Class (Level 2 Threads)

- Models the logic for level 2 threads.
- Similar reasoning to RunnableOrderThread.
- Addresses challenges in MappedByteBuffer to String conversion using the duplicate() method.

## Bonus Implementation: setNewBuffer() Method

The algorithm design is based on two critical aspects:

1. **Generation**: Handles cases where a thread's chunk might contain an incomplete command (e.g., "o_dwri3").
2. **Correction**: Checks if the last character in the chunk is "o", and eliminates the problem from that buffer if it's not.

The correction phase modifies the size at each step, updating the new position at the end.

## File Handling and Memory Management

- All started files and channels are closed at the end of the program in the main method.

## References

- [ByteBuffer to String in Java](https://stackoverflow.com/questions/43234003/bytebuffer-to-string-in-java)
- [Java MappedByteBuffer Guide](https://www.baeldung.com/java-mapped-byte-buffer)
- [Java API Examples for MappedByteBuffer](https://www.programcreek.com/java-api-examples/?api=java.nio.MappedByteBuffer)
- [Memory-Mapped File in Java](https://www.geeksforgeeks.org/what-is-memory-mapped-file-in-java/)
- [FileChannel, Memory-Mapped I/O, and Locks in Java](https://www.happycoders.eu/java/filechannel-memory-mapped-io-locks/)
