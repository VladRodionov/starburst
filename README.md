# Starburst text provider

## Build prerequisites

* Java 11
* Maven 3.x

## Build

```mvn packge -DskipTests```

## Run

```cd ./bin```
```run.sh absolute-path-to-file```

## How I tested it

```telnet localhost 10322```

## Assumptions on client-server protocol

All client requests and all server's responses ends with CRLF 2 chars sequence

## Architecture

* Server (initialization, index creation (if needed), network conections serving)
* RequestHandler - it is a runnable task which is submitted to executor thread pool on each incoming request
* FileIndex - creates index and keeps it on disk, allowing to server very large files
* Commands - each command has its own class implementation: QUIT, SHUTDOWN, ERR (generic error response) and GET. New commands can be added easily.

## Scalability 

On a file sizes it is limited only by available disk space. Performance will be good until index and text file can fit into a RAM, then it will decrease. 
On a request per second performance side - should be fine when all data can fit into a RAM and will gradually decrease once a text file increases.
So, for a 100GB file and 7.5G RAM I expect that performance will be limited only by a disk I/O

## Time spent

approx 9 h.



