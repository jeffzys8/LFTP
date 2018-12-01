/*
This class is meant for the Receive' process
*/
import java.net.*;
import java.io.*;
import java.util.*;

public class ReceiveHandler implements Runnable{

    //socket
    //filename

    DatagramPacket packet = new DatagramPacket(new byte[128],128);
    DatagramSocket socket;
    String filename;

    ReceiveHandler(DatagramSocket socket, String filename){
        this.socket = socket;
        this.filename = filename;
    }

    public void sendError(String msg){
        System.out.println("Error: " + msg);
        System.exit(0);
    }

    public void run(){
        try {
            // 向服务器发送传输文件的请求
            // 开始读取文件
            packet.setData(new byte[64]);
            FileOutputStream fos = null;
            File f2 = new File(filename);
            fos = new FileOutputStream(f2);
            int count = 0;
            do{
                socket.receive(packet); //阻塞
                // System.err.println("Rcv:" + new String(packet.getData()));
                fos.write(packet.getData(), 0, packet.getLength());
                count++;
            }while(packet.getData()[0] != 0);
    
            System.out.println("总共接收包:" + count);
            fos.close();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}