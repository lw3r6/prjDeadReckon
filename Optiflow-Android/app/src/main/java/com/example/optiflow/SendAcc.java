package com.example.optiflow;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class SendAcc extends Thread{

    private final static int UDP_SERVER_PORT = 5000;
    private final ArrayList<String> mMessages = new ArrayList<>();
    private String mServer;
    private DatagramSocket udpSocket;

    private boolean mRun = true;

    SendAcc(String server) {
        this.mServer = server;
        try {
            udpSocket = new DatagramSocket(UDP_SERVER_PORT);
        } catch (SocketException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {

        while (mRun) {
            try {
                InetAddress serverAddr = InetAddress.getByName(mServer);

                // Starting handshake with server by pinging server
                String outMsg = "START SDP";
                byte[] buf = (outMsg).getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length,serverAddr, UDP_SERVER_PORT);
                udpSocket.send(packet);

                // Continuing handshake by waiting for message
                buf = new byte[2048];
                packet = new DatagramPacket(buf, buf.length);
                Log.d("waiting for handshake: ", "");
                udpSocket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                Log.d("handshake message in", received);


                while (mRun) {
                    String message;

                    // Wait for message
                    synchronized (mMessages) {
                        while (mMessages.isEmpty()) {
                            try {
                                mMessages.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        // Get message and remove from the list
                        message = mMessages.get(0);
                        mMessages.remove(0);
                    }

                    //send output msg
                    outMsg = message;

                    buf = (outMsg).getBytes();
                    packet = new DatagramPacket(buf, buf.length,serverAddr, UDP_SERVER_PORT);
                    udpSocket.send(packet);
                }

            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {

                Log.d("ip: ", mServer);
                e.printStackTrace();
            }
        }
    }

    public void send(String message) {
        synchronized (mMessages) {
            mMessages.add(message);
            mMessages.notify();
        }
    }

    public void close() {
        mRun = false;
    }
}