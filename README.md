# Leader
Peer to peer system that allows peers to send requests to a leader node that handles those requests.

# task runPeer
Run peer has two required arguments, a username and port number, command line arguments should
be run using --args="name port".

# Task runLeader
Run Leader needs a minimum of two arguments, a username port number, and then the host and port number
of the peer you wish to connect to. Runleader can connect to any number of nodes given those
nodes are running before calling runLeader. Arguments should be passed with --args="name port host1
port1 host2 port2..."

## Constraint 4
In my first implementation I had no issues with the chat not working after killing any of the nodes.
In order to connect all nodes I first had the peers connect and wait for a message from the leader.
As soon as the leader started, I had the leader send a message to the server of each node specified
in the arguments that included the other nodes info as well as the leader nodes info. After this
each peer started a client thread for every other peer and a two way connection was established 
between every node, so if any node disconnected, the remaining nodes would still be connected and 
able to chat.

## Constraint 7a
Once all nodes are connected, the first leader is automatically chosen by the largest number
in the list of all nodes. This will always be the leader that gets run after all peers are set up.
The leader then sends out messages to all other nodes letting them know that it is still there.
Anytime a request is made the nodes will check again if the leader is still active. If the leader
has not sent a heartbeat in awhile, a new leader is selected. Once agian the leader is chosen by
the node with the highest number that was assigned to it at the beginning. In order for a node
to finalize becoming a leader, they must type something in the terminal in order to accept the 
leader role. This is something that I would work out if I had more time, but for now this is
how things will be done.

## Protocol
I used JSONObject to send data between the nodes with a fairly simple protocol. Every object
had a messagetype as well as data. The data was typically its own json object that contained 
either an array with information for the nodes needed to be connected to, or data for the calc
request. Every message was sent by opening up a socket and closing it afterwards with an expected
response from the server every time a message is sent. These responses are mostly just ackknowledgements
to help with debugging.