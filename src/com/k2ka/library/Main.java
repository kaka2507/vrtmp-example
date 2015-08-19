package com.k2ka.library;

import com.k2ka.library.vrtmp.RTMPPublisher;
import com.k2ka.library.vrtmp.RTMPPublisherListener;
import com.k2ka.library.vrtmp.io.RTMPConnection;
import com.k2ka.library.vrtmp.utils.Util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static RTMPPublisher publisher;
    public static boolean isRunning = false;
    public static void main(String[] args) {
        publisher = new RTMPConnection();

        String rtmp_url = "rtmp://120.138.75.44:8080/live"; // rtmp server url
        String channel_name = "test"; // channel name
        ExecutorService worker = Executors.newSingleThreadExecutor();
        RTMPPublisherListener listener = new RTMPPublisherListener() {
            @Override
            public void onInitComplete() {
                try {
                    String inputFile = "d:/cuc_ieschool.flv"; // change your input file
                    File inFile = new File(inputFile);
                    InputStream inputStream = new BufferedInputStream(new FileInputStream(inFile));
                    isRunning = true;
                    // read flv header
                    byte[] flv_header = new byte[9];
                    Util.readBytesUntilFull(inputStream, flv_header);
                    System.out.println(Util.toHexString(flv_header));
                    long beginTimestamp = System.currentTimeMillis();
                    while(isRunning) {
                        // read flv tag
                        int previousTagLength = Util.readUnsignedInt32(inputStream);
                        int tagType = inputStream.read();
                        int tagSize = Util.readUnsignedInt24(inputStream);
                        int timestamp = Util.readUnsignedInt24(inputStream);
                        int timestampEx = inputStream.read();
                        timestamp = (timestampEx << 24) | timestamp;
                        int streamID = Util.readUnsignedInt24(inputStream);
                        byte[] body = new byte[tagSize];
                        Util.readBytesUntilFull(inputStream, body);
                        System.out.println("timestamp:" + timestamp);
                        publisher.SendFLVTag(tagType, body, tagSize, timestamp);
                        long delta = System.currentTimeMillis() - beginTimestamp;
                        if(delta < timestamp) {
                            Thread.sleep(timestamp - delta);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Could not read file with e:" + e.toString());
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(RTMPPublisher.ERROR error, String s) {
                isRunning = false;
                System.out.println("Receive error: " + error + " msg:" + s);
            }
        };
        publisher.Init(rtmp_url, worker, listener, channel_name);
    }
}
