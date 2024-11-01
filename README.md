# JetBrains Internship Test Task 2

This is my (Luca Tagliavini) submission for the application to JetBrains
Internship programme. <br>
My project of choice is: Advanced SSH Agent Management.

Following are instructions on how to build/test/run the code in this repository.

## Building

This project uses Gradle as a build system.
The following should be sufficient to build the application jar.

```dtd
$ ./gradlew build
```

NOTE: If you're on a Windows platform, for this and all the following commands you
may need to use the `gradlew.bat` script instead of `gradlew`.

## Testing

This project contains tests for the protocol implementation (serialization, deserialization).
They can be executed with:

```dtd
$ ./gradlew test
```

## Running

This project contains two main components:
- (a) Server
- (a) Client

The client was not required by the specification, but it's useful
to test the behaviour of the server during development or verification.

Instruction on how to run the project differ based on which component
you want to run.

## Running the Server

To run the server you need to provide two arguments:

- `socket_path`: The path on which you want to open the UNIX socket for listening.
- `file_path`: The path where the file managed by the server shall be written.

For example, you can run the server through Gradle using:

```
$ ./gradlew run --console=plain --args="server $PWD/socket $PWD/file"
```

This will launch the server and have it listen/write to a socket/file in the current directory.

The server will print verbose logs about connections it receives and any operation it performs on the file.

## Running the client

NOTE: Make sure you have started the server on the socket path you want to
connect to before launching the client!

To run the client, you need to provide it the path to the socket the server is listening on. <br>
For example, assuming you're executing the server in a separate terminal with the previously provided example command,
you can start the client with the following:

```
$ ./gradlew run --console=plain --args="client $PWD/socket"
```

When started, the client will provide you with a small help section to explain it usage:

```
Command syntax:
        <op> <data...>
Where <op> is one of the follwoing:
        2                     Append the text in data to the output file
        3                     Clear the output file
        5                     Send a ping
        q                     Quit
```

## Client usage example

This section outlines an example of how the client can be used to test the server implementation. <br>
We assume that the server has already been started and listening on a socket at path `/path/to/socket`.

Here's an example client execution interacting with the server, using all the supported commands:

```
Connecting to /path/to/socket
Command syntax:
        <op> <data...>
Where <op> is one of the follwoing:
        2                     Append the text in data to the output file
        3                     Clear the output file
        5                     Send a ping
        q                     Quit

> 3
< Ok (0) null

> 2 hello world
< Ok (0) null

> 2 howdy
< Ok (0) null

> 2
< Ok (0) null

> 3 test
< Error (56) Invalid message: Message with type Clear has content length 4

> 5
< Ok (0) null

> q
Disconnecting
```