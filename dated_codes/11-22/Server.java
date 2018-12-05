import java.net.*;
import java.io.*;

public class Server {
    public static void main(String[] args) {
        try {
            DatagramSocket server = new DatagramSocket(5060);   //设置接收端口
            DatagramPacket packet = new DatagramPacket(new byte[128], 128);

            //直接开始读取文件
            FileOutputStream fos = null;
            File f2 = new File(args[0]);
            fos = new FileOutputStream(f2);
            int count = 0;
            do{
                server.receive(packet); //阻塞
                System.out.println("Rcv:" + new String(packet.getData()));
                // fos.write(packet.getData(), 0, 64);
                ++count;
            }while(packet.getData()[0] != 0);
    

            System.out.println("总共接收包:" + count);
            fos.close();

            packet.setData("Hello Client".getBytes());
            packet.setPort(5070);
            packet.setAddress(InetAddress.getLocalHost());
            server.send(packet);
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}