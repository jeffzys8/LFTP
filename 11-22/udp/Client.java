import java.net.*;
import java.io.*;

public class Client {
    public static void main(String[] args){
        try {
            DatagramSocket client = new DatagramSocket(5070);
            DatagramPacket packet = new DatagramPacket(new byte[1024],1024);
            packet.setPort(5060);
            packet.setAddress(InetAddress.getLocalHost());
            packet.setData("Hello Server".getBytes());
            client.send(packet);
            client.receive(packet); //阻塞
            System.out.println(packet.getAddress().getHostName() + "(" + packet.getPort() + "):" + new String(packet.getData()));
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}