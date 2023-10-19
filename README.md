# pretty-dump for JSON and XML Solace messages
A useful utility that emulates SdkPerf `-md` output, echoing received Solace messages to the console, but pretty-printed for **JSON** and **XML**.
Now also with a display option for a compressed, one-line-per-message view.

## Requirements

Java 8+


## Building

```
./gradlew assemble
cd build/distributions
unzip PrettyDump.zip
cd PrettyDump
```

Or just download a [Release distribution](https://github.com/SolaceLabs/pretty-dump/releases) with everything built.


## Running

```
$ bin/PrettyDump
PrettyDump initializing...
PrettyDump connected to VPN 'default' on broker 'localhost'.
Subscribed to Direct topic: '>'

Starting. Press Ctrl-C to quit.
```


## Command line parameters

```
$ bin/PrettyDump -h   or --help

Usage: PrettyDump [host:port] [message-vpn] [username] [password] [topics|q:queue|b:queue] [indent]

 - If using TLS, remember "tcps://" before host
 - Default parameters will be: localhost default aaron pw ">" 4
 - One of:
    - comma-separated list of Direct topic subscriptions
    - "q:queueName" to consume from queue
    - "b:queueName" to browse a queue
       - Can browse all messages, or specific messages by ID
 - Optional indent: integer, default==4; specifying 0 compresses payload formatting
    - Use negative indent value (column width) for ultra-compact topic & payload only
    - Use negative zero (-0) for only topic
 - Default charset is UTF-8. Override by setting: export PRETTY_DUMP_OPTS=-Dcharset=whatever
    - e.g. export PRETTY_DUMP_OPTS=-Dcharset=Shift_JIS  (or "set" on Windows)
```


## Subscribing options - the 5th argument

### Direct Subscription(s)

By default, PrettyDump will subscribe to the "catch-all" multi-level wildcard topic `>`, which will show most (not all!) messages going through your VPN.

Specify a single topic subscription, or a comma-separated list: e.g. `"bus_trak/door/v1/007*/>, bus_trak/gps/v2/>"` (spaces will be stripped out).
 Remember to "quote" the whole argument if using the `>` wildcard as most shells treat this as a special character.

If you want to see *all* messages going through the VPN, then override the 5th argument with `">, */>, #*/>"` and this will also display any "hidden" messages
such as those published directly to queues, point-to-point messages, request-reply, REST responses in gateway mode, etc.

```
Subscribed to Direct topic: '>'
Subscribed to Direct topic: '*/>'
Subscribed to Direct topic: '#*/>'
```

### Queue Consume

To connect to a queue and consume (e.g. the messages will be ACKed and removed), then override the 5th argument with
`q:queueName`, e.g. `q:q1`.  You will receive a warning that messages will be removed from the queue after they are received.

```
Attempting to bind to queue 'q1' on the broker... Success!

Will consume/ACK all messages on queue 'q1'. Use browse 'b:' otherwise.
Are you sure? [y|yes]: 
```

### Browsing a Queue

To non-destructively view the messages on a queue, use the browse option: `b:queueName`.  You have the option of browsing
all messages, a single message based on Message ID, or a range of messages (either closed "`12345-67890`" or open-ended "`12345-`").

To find the ID of the messages on a queue, either use PubSub+ Manager, or use CLI or SEMP:

![View Message IDs in PubSubPlus Manager](https://github.com/SolaceLabs/pretty-dump/blob/main/browse-msgs.png)


Or, to just browse the first / oldest message on the queue, enter "`1`" or some other low number.

```
$ bin/PrettyDump aaron.messaging.solace.cloud aaron-demo-singapore me pw b:q1


PrettyDump initializing...
PrettyDump connected to VPN 'aaron-demo-singapore' on broker 'aaron.messaging.solace.cloud'.
Attempting to browse queue 'q1' on the broker... Success!

Browse all messages (press [ENTER]),
 or enter specific Message ID,
 or range of IDs (e.g. "25909-26183" or "3717384-"): 31737085

Starting. Press Ctrl-C to quit.
^^^^^^^^^^^^^^^^^ Start Message ^^^^^^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'bus_trak/gps/v2/004M/01398/001.31700/0103.80721/30/OK'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           NON_PERSISTENT
Message Id:                             31737085
Replication Group Message ID:           rmid1:102ee-0b5760c9706-00000000-01e444fd
Binary Attachment:                      len=173
JSON Object, TextMessage:
{
    "psgrCap": 1,
    "heading": 228,
    "busNum": 1398,
    "latitude": 1.317,
    "rpm": 1515,
    "speed": 60,
    "routeNum": "4M",
    "longitude": 103.80721,
    "status": "OK"
}
^^^^^^^^^^^^^^^^^^ End Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^
Browsing finished!
Main thread exiting.
Shutdown detected, quitting...
```


## Output Indent options - the 6th argument

### Regular, indent > 0

indent = 4 in this example:
```
PrettyDump connected, and running. Press Ctrl-C to quit.
^^^^^^^^^^^^^^^^^ Start Message ^^^^^^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'test'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Message Id:                             4728274
Binary Attachment:                      len=78
XML, BytesMessage:
<apps>
    <version>23</version>
    <another>hello</another>
    <that>this</that>
</apps>
^^^^^^^^^^^^^^^^^^ End Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^
^^^^^^^^^^^^^^^^^ Start Message ^^^^^^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'test'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Message Id:                             4728275
Binary Attachment:                      len=159
JSON Object, TextMessage:
{
    "firstName": "Aaron",
    "lastName": "Lee",
    "zipCode": "12345",
    "streetAddress": "Singapore",
    "birthdayDate": "1999/01/02",
    "customerId": "12345"
}
^^^^^^^^^^^^^^^^^^ End Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^
```

### Compact, indent = 0

```
PrettyDump connected, and running. Press Ctrl-C to quit.
^^^^^^^^^^^^^^^^^ Start Message ^^^^^^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'test'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Message Id:                             4728274
Binary Attachment:                      len=78
XML, BytesMessage:
<apps><version>23</version><another>hello</another><that>this</that></apps>
^^^^^^^^^^^^^^^^^^ End Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^
^^^^^^^^^^^^^^^^^ Start Message ^^^^^^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'test'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Message Id:                             4728275
Binary Attachment:                      len=159
JSON Object, TextMessage:
{"firstName": "Aaron","lastName": "Lee","zipCode": "12345","streetAddress": "Singapore","birthdayDate": "1999/01/02","customerId": "12345"}
^^^^^^^^^^^^^^^^^^ End Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^
```



### One-Line, indent < 0

indent = -35 in this example
```
pq-demo/stats/pq/sub-pq_3-c222       {"red":0,"oos":0,"queueName":"pq/3","slow":0,"rate":0,"ackd":0,"gaps":0,"flow":"FLOW_ACTIVE"}
pq/3/pub-44e7/e7-7/0/_          
pq-demo/stats/pq/pub-44e7            {"prob":0,"paused":false,"delay":0,"nacks":0,"rate":2,"resendQ":0,"keys":8,"activeFlow":true}
pq/3/pub-44e7/e7-5/0/_          
pq-demo/stats/pq/sub-pq_3-c222       {"red":0,"oos":0,"queueName":"pq/3","slow":0,"rate":0,"ackd":0,"gaps":0,"flow":"FLOW_ACTIVE"}
solace/samples/jcsmp/hello/aaron     +...4..probability...>���..from...aaron...age.......
pq/3/pub-44e7/e7-3/0/_          
pq-demo/stats/pq/pub-44e7            {"prob":0,"paused":false,"delay":0,"nacks":0,"rate":2,"resendQ":0,"keys":8,"activeFlow":true}
pq/3/pub-44e7/e7-0/0/_          
```

### One-Line, Topic only, indent = "-0"

```
bus_trak/gps/v2/013B/01058/001.37463/0103.93459/13/STOPPED
bus_trak/gps/v2/036M/01154/001.37578/0103.93151/31/OK
bus_trak/gps/v2/002A/01151/001.32270/0103.75160/30/OK
bus_trak/door/v1/033M/01434/open
bus_trak/gps/v2/033M/01434/001.42483/0103.71193/31/STOPPED
bus_trak/gps/v2/011X/01390/001.39620/0103.84082/11/OK
bus_trak/gps/v2/035B/01286/001.40101/0103.88913/12/OK
bus_trak/door/v1/005B/01464/open
bus_trak/gps/v2/006A/01291/001.29687/0103.78305/21/STOPPED
```
