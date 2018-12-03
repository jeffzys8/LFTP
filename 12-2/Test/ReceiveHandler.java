/*
This class is meant for the Client's Receiving process
*/
import java.net.*;
import java.io.*;
import java.util.*;

public class ReceiveHandler implements Runnable{

    final static int BUFFER_SIZE = 512 * 1024; //接收缓冲区大小, 为了实验效果, 将其设为 SendHandler的发送缓冲区的一半

    DatagramPacket packet = new DatagramPacket(new byte[128],128);
    DatagramSocket socket;
    String filename;
    InetAddress dstAddr;
    int dstPort;
    int startSEQ;

    Boolean end;

    ReceiveHandler(DatagramSocket socket, String filename, InetAddress dstAddr, int dstPort, int seq){
        this.socket = socket;
        this.filename = filename;
        this.dstAddr = dstAddr;
        this.dstPort = dstPort;
        this.startSEQ = seq;
        end = false;
    }

    public void sendError(String msg){
        System.out.println("Error: " + msg);
        System.exit(0);
    }

    public void run(){

        try {

            byte[] buffer = new byte[BUFFER_SIZE];
            int bfsp = 0;     //buffer起始点标记位

            // 新建线程接收socket数据 (生产者)
            MySocketReader mySocketReaer = new MySocketReader();
            Thread thread_GET = new Thread(MySocketReader);

            // 主线程进行写文件操作
            while(!end){
                if(thread_GET.end){
                    end = true;
                }
                //查看缓冲区是否有未写入文件的内容
            }

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