# Some examples of how to use libstreaming

This repository contains three simple examples of how to use libstreaming.

### Libstreaming ?

You can find out more about libstreaming [here](https://github.com/fyhertz/libstreaming).

### Example 1

Shows how to use the RTSP server.

### Example 2

Shows how to start a stream and obtain a SDP that you will then want to hand to the receiver.

### Example 3

**This example shows how you can use libstreaming with a Wowza Media Server.**

The stream is published to the Wowza server using the RTSP protocol and the [RtspClient](http://dallens.fr/majorkernelpanic/libstreaming/doc-v3/net/majorkernelpanic/streaming/rtsp/RtspClient.html) class of libstreaming. Follow [this tutorial](http://www.wowza.com/forums/content.php?354) to find out how to configure the RTSP/RTP publishing access on your Wowza server, and how to playback the stream with RTMP in a flash player. The example3 also contains an example of how to use JW Player, you will need to get a free license for JW player to use it.
