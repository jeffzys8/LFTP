/*
This class is meant for the Client's Receiving process
*/
import java.net.*;
import java.io.*;
import java.util.*;

public class ReceiveHandler implements Runnable{

    final static int BUFFER_SIZE = 512 * 1024; //接收缓冲区大小, 为了实验效果, 将其设为 SendHandler的发送缓冲区的一半
    final static int MAX_SEG_SIZE = 1024; //最大传输单元 (暂定为1KB) 
    
    DatagramPacket packet = new DatagramPacket(new byte[MAX_SEG_SIZE+64],MAX_SEG_SIZE+64);
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

    public class MyFileIO implements Runnable{

        byte[] buffer;
        int buffer_start;   // 数据开始段
        int buffer_end;     // 对于IO来说应该是是只读的
        Boolean end;

        public MyFileIO(byte[] bf){
            this.buffer = bf;
            this.buffer_start = 0;
            this.buffer_end = 0;
            end = false;
        }

        public void run(){
            // 打开文件流
            try{
                FileOutputStream fos = null;
                File f2 = new File(filename);
                fos = new FileOutputStream(f2);
                while(!end){
                    if(buffer_start < buffer_end){
                        fos.write(buffer, buffer_start, buffer_end - buffer_start);
                        buffer_start = buffer_end;
                    }
                }
                fos.close();
                }
            catch(Exception e){
                Utils.sendError("Unknown fileIO error.");
            }
        }

        public void reset(){
            buffer_start = 0;
            buffer_end = 0;
        }
    }

    public class MySocketIO implements Runnable{
        
        int buffer_start;
        int buffer_end;
        byte[] buffer;
        DatagramSocket socket;
        DatagramPacket packet;
        int ack;
        Boolean end;

        public MySocketIO(int a, byte[] bf){
            this.ack = a;
            this.buffer = bf;
            this.socket = ReceiveHandler.this.socket;
            this.packet = ReceiveHandler.this.packet;
            this.buffer_start = 0;
            this.buffer_end = 0;
            end = false;
        }

        public void run(){
            // 进行Socket Read操作
            try{
                while(!end){
                    socket.receive(packet);
                    int recv_seq = Utils.GetSeq(packet.getData());
                    int data_size = Utils.GetDataSize(packet.getData());
                    byte[] data = Utils.getPartialBytes(packet.getData(), Utils.HEADER_DATA, Utils.HEADER_DATA + data_size - 1);
                    if(recv_seq == ack){    //正好是所需要的包, 写入缓存, 发对应ACK
                        if(buffer_end + data_size <= BUFFER_SIZE){ //超出缓存空间会默认被丢弃
                            Utils.BytesInsertByte(buffer, data, buffer_end);
                            buffer_end += data_size;
                            ack += data_size;
                        }
                    }
                    // 处理收到结尾包的情况
                    else if(recv_seq == 200){ //结束
                        end = true;
                        ack = 201; // 回传201表示收到, 当然现在设定是服务器会无视.
                    }

                    // 只要收到包就都会有ACK包发出
                    Utils.SetACK(packet.getData(), ack);
                    Utils.SetWindow(packet.getData(), BUFFER_SIZE -  buffer_end);
                    socket.send(packet);
                }
            }
            catch(Exception e){
                Utils.sendError("Unknown socket error");
            }
        }

        public Boolean isEnd(){
            return end;
        }
    }

    public void run(){

        try {

            byte[] buffer = new byte[BUFFER_SIZE];
            int buffer_start = 0;     //buffer数据起始点
            int buffer_end = 0;     //buffer数据结束点后一位 (也即数据区间为[buffer_start, buffer_end) )
            int ack = startSEQ;     //回传用的seq

            // 新建线程进行写文件操作; 写操作只修改 buffer_start (生产者)
            MyFileIO myFileIO = new MyFileIO(buffer);
            Thread thread_fIO = new Thread(myFileIO);
            thread_fIO.start();

            // 新建线程进行读socket操作; 读socket操作只修改 buffer_end, ack (消费者)
            MySocketIO mySocketIO = new MySocketIO(ack, buffer);
            Thread thread_sIO = new Thread(mySocketIO);
            thread_sIO.start();

            // 主线程检测&协助生产者和消费者更新数据, 并发送ACK
            while(!end){

                
                // 更新buffer的数据端点
                buffer_start = myFileIO.buffer_start;
                buffer_end = mySocketIO.buffer_end;
                end = mySocketIO.isEnd();   //由socket线程决定是否收完
                // 更新两个线程的数据
                myFileIO.buffer_end = buffer_end;
                myFileIO.end = end;
                mySocketIO.buffer_start =buffer_start;  //这里其实没有什么关系, socket不会关心start

                // 缓冲区已满且文件IO结束
                if(buffer_start == BUFFER_SIZE && buffer_end == BUFFER_SIZE){ 
                    // 刷新缓冲区
                    buffer_start = 0;
                    buffer_end = 0;
                    // 刷新线程数据
                    myFileIO.buffer_start = 0;
                    myFileIO.buffer_end = 0;
                    mySocketIO.buffer_start = 0;
                    mySocketIO.buffer_end = 0;
                    // 发送ACK包, 用于更新window, 防止饥饿
                    Utils.SetACK(packet.getData(), ack);
                    Utils.SetWindow(packet.getData(), BUFFER_SIZE);
                    socket.send(packet);
                }
            }

            
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}