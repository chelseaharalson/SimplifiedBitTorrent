# SimplifiedBitTorrent
This project creates a peer-to-peer network for file downloading. It resembles some features of BitTorrent, but much simplified. There are two pieces of software – peer and file owner. The file owner has a file, and it breaks the file into chunks of 100KB, each stored as a separate file. The file owner listens on a TCP port. It is designed as a server that can run multiple threads to serve multiple clients simultaneously. The peer requests the chunk ID list from its download neighbor, compares with its own to find the missing ones, and downloads those from its neighbor. In the mean time, it sends its own chunk ID list to its upload neighbor, and upon request uploads chunks to that neighbor. After a peer has all file chunks, it combines them into a single file.

## Parameters:
```
java Server <server port number> <filename>
java Client <host name> <server port number> <upload port number> <download port number> <my port number>
```

## Example Run For 5 Clients:
```
java Server 8397 image.png
java Client localhost 8397 9004 9001 9000
java Client localhost 8397 9000 9002 9001
java Client localhost 8397 9001 9003 9002
java Client localhost 8397 9002 9004 9003
java Client localhost 8397 9003 9000 9004
```