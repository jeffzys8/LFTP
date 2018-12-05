/*
This class is meant for the Client's Sending process
*/
import java.net.*;
import java.io.*;
import java.util.*;


public class SendHandler implements Runnable{
    
    DatagramPacket packet = new DatagramPacket(new byte[128],128);
    DatagramSocket socket;
    String filename;

    SendHandler(DatagramSocket socket, String filename){
        this.socket = socket;
        this.filename = filename;
    }
    
    public void sendError(String msg){
        System.out.println("Error: " + msg);
        System.exit(0);
    }

    public void run(){
        
    }
}