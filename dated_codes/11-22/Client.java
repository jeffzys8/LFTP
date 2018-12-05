import java.net.*;
import java.io.*;
import java.util.*;

public class Client {
    public static void main(String[] args){
        try {
            DatagramSocket client = new DatagramSocket(5070);
            DatagramPacket packet = new DatagramPacket(new byte[128],128);
            packet.setPort(5060);   //设置传输端口
            packet.setAddress(InetAddress.getLocalHost());  //地址暂时设为Locolhost
            
            // packet.setData("Hello Server".getBytes());
            // client.send(packet);

            /**开始循环读文件并发送*/
            FileInputStream fis = null;
            File f1 = new File(args[0]);
            fis = new FileInputStream(f1);
            byte[] bs = new byte[64];    //IO缓冲区(也即每次发送包大小)设置为64bytes
            int i;
            System.out.println("开始发送文件");
            int count = 0;
            while ((i = fis.read(bs)) != -1) {
                packet.setData(bs);
                client.send(packet);
                ++count;
            }
            System.out.println("总共发送包:" + count);
            fis.close();

            byte[] end = {0};
            packet.setData(end); //发送结束信号 
            client.send(packet);

            client.receive(packet); //阻塞
            System.out.println(packet.getAddress().getHostName() + "(" + packet.getPort() + "):" + new String(packet.getData()));
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}