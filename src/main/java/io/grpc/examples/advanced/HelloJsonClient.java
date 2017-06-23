/*
 * Copyright 2016, Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *
 *    * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.grpc.examples.advanced;

import io.grpc.*;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.HelloWorldClient;
import io.grpc.stub.AbstractStub;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static io.grpc.stub.ClientCalls.blockingUnaryCall;

/**
 * Advanced example of how to swap out the serialization logic.  Normal users do not need to do
 * this.  This code is not intended to be a production-ready implementation, since JSON encoding
 * is slow.  Additionally, JSON serialization as implemented may be not resilient to malicious
 * input.
 * <p>
 * <p>If you are considering implementing your own serialization logic, contact the grpc team at
 * https://groups.google.com/forum/#!forum/grpc-io
 */
public final class HelloJsonClient {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(HelloWorldClient.class);

    private final ManagedChannel channel;
    private final HelloJsonStub blockingStub;

    /**
     * Construct client connecting to HelloWorld server at {@code host:port}.
     */
    public HelloJsonClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build();
        blockingStub = new HelloJsonStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * Say hello to server.
     */
    public void greet(String name) {
        logger.info("Will try to greet " + name + " ...");
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloReply response;
        try {
            response = blockingStub.sayHello(request);
        } catch (StatusRuntimeException e) {
            logger.error("RPC failed: {}", e.getStatus());
            return;
        }
        logger.info("Greeting: " + response.getMessage());
    }

    /**
     * Greet server. If provided, the first element of {@code args} is the name to use in the
     * greeting.
     */
    public static void main(String[] args) throws Exception {

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*4);

        for (int i=1;i<16;i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    HelloJsonClient client = new HelloJsonClient("localhost", 50051);
                    try {
                        byte[] buffer = new byte[1024];
                        for (int j=0;j<buffer.length;j++) {
                            buffer[j] = (byte) 0xFF;
                        }
                        /* Access a service running on the local machine on port 50051 */
                        String user = Arrays.toString(buffer);
                        client.greet(user);
                    } finally {
                        try {
                            client.shutdown();
                        } catch (InterruptedException e) {
                        }
                    }
                }
            });
        }

        executorService.shutdown();

        TimeUnit.SECONDS.sleep(10);
    }

    static final class HelloJsonStub extends AbstractStub<HelloJsonStub> {

        protected HelloJsonStub(Channel channel) {
            super(channel);
        }

        protected HelloJsonStub(Channel channel, CallOptions callOptions) {
            super(channel, callOptions);
        }

        @Override
        protected HelloJsonStub build(Channel channel, CallOptions callOptions) {
            return new HelloJsonStub(channel, callOptions);
        }

        public HelloReply sayHello(HelloRequest request) {
            return blockingUnaryCall(
                    getChannel(), GreeterGrpc.METHOD_SAY_HELLO, getCallOptions(), request);
        }
    }
}

