let latch = new java.util.concurrent.CountDownLatch(1);

let ws = Websocket("wss://echo.websocket.org", {"User-Agent": "TTS Server"})
ws.on('close', function(code, reason){ // Int, String
    println("closed: " + code + ", " + reason))
   latch.countDown()
})

ws.on('error', function(err, response){ // String, okio.Response
    println("error: " + err + ", code: " + response.code() + ", message: " + response.message())
    latch.countDown()
})

ws.on('binary', function(byteString){ // okio.ByteString
    println("receive binary: " + msg)
})

ws.on('text', function(msg){ // String
    println("receive text: " + msg)
})

ws.on('open', function(){
    ws.send("Hello world!")
})

latch.await()

// 关闭
// ws.close(1007, "close~~")

// 强制断开
// ws.cancel()