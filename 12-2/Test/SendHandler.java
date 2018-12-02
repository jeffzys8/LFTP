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
    InetAddress dstAddr;
    int dstPort;

    SendHandler(DatagramSocket socket, String filename, InetAddress dstAddr, int dstPort){
        this.socket = socket;
        this.filename = filename;
        this.dstAddr = dstAddr;
        this.dstPort = dstPort;
    }
    
    public void sendError(String msg){
        System.out.println("Error: " + msg);
        System.exit(0);
    }

    public void run(){
        try{
            packet.setPort(dstPort);
            packet.setAddress(dstAddr);
            FileInputStream fis = null;
            File file = new File(filename);
            
            fis = new FileInputStream(file);
            byte[] bs = new byte[1024*10];    //本地缓冲区为10M，也即每次读文件的大小；每次在消耗完后读
            
            //每次读满一个缓冲区，然后使用GBN协议将缓冲区内数据全部传完；然后再读满，再传；循环这个过程直到文件被读完。

            System.out.println("开始发送文件("+filename+")至: " + dstAddr.toString() + ":" + dstPort);
            int count = 0, length = 0;
            while ((length = fis.read(bs)) != -1) {
                packet.setData(bs, 0, length);
                socket.send(packet);
                ++count;
                // Thread.sleep(10);  //沉睡500ms
            }
            System.out.println("发送至" + dstAddr.toString() + ":" + dstPort + "完成。总共发送包:" + count);
            fis.close();

            //发送传输结束信号
            byte[] end = {0};
            packet.setData(end);
            socket.send(packet);

            //关闭socket，结束端口占用
            socket.close();
        }
        catch(Exception e){}
    }
}