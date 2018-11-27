import java.net.*;
import java.io.*;
import java.util.*;

public class Client {
    static final int server_port = 5060;
    public static void main(String[] args){
        try {
            // 向服务器发送传输文件的请求
            DatagramSocket socket = new DatagramSocket(Integer.parseInt(args[0])); //客户端自选端口
            DatagramPacket packet = new DatagramPacket(new byte[128],128);
            packet.setPort(server_port);   // 设置服务器端口
            packet.setAddress(InetAddress.getLocalHost());  // 设置服务器地址
            
            packet.setData("Hello Server".getBytes());
            socket.send(packet);

            // 开始读取文件
            FileOutputStream fos = null;
            File f2 = new File(args[0]);
            fos = new FileOutputStream(f2);
            int count = 0;
            do{
                socket.receive(packet); //阻塞
                fos.write(packet.getData(), 0, 64);
                ++count;
            }while(packet.getData()[0] != 0);
    

            System.out.println("总共接收包:" + count);
            fos.close();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}