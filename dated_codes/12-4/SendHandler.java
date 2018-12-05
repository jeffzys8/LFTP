/*
This class is meant for the Client's Sending process
*/
import java.net.*;
import java.io.*;
import java.util.*;


public class SendHandler implements Runnable{
    
    final static int BYTE_PER_READ_FILE = 1024 * 1024; //每次发送缓冲区空，将会从文件中读满缓冲区；该常量为每次读的字节数 (目前定为1M)
    final static int MAX_SEG_SIZE = 1024; //最大传输单元 (暂定为1KB) 

    DatagramPacket packet;
    DatagramSocket socket;
    String filename;
    InetAddress dstAddr;
    int dstPort;
    int startSEQ;
    int rwind = 5 * MAX_SEG_SIZE;   //流量控制窗口 (初始化为5MSS, 但其实应该是握手的时候交换这个数据的初始值)
    int cwind = MAX_SEG_SIZE;       //阻塞控制窗口, 慢启动(初始值为1MSS)
    int last_cwind = 102400;        //一开始将last_cwind设置为无穷大, 以便于快速启动

    SendHandler(DatagramSocket socket, String filename, InetAddress dstAddr, int dstPort, int seq){
        this.socket = socket;
        this.filename = filename;
        this.dstAddr = dstAddr;
        this.dstPort = dstPort;
        this.startSEQ = seq;
        packet = new DatagramPacket(new byte[MAX_SEG_SIZE+64], MAX_SEG_SIZE+64); //还有header
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

            byte[] bs = new byte[BYTE_PER_READ_FILE];    // 本地缓冲区，也即每次读文件的大小；每次在消耗完后读
 
            int waitingSEQ = 0 + startSEQ;  // 当前等待ACK的序号，用于传输
            int tosendSEQ = 0 + startSEQ;   // 当前即将发送的序号，用于传输
            int bfpt_waiting_seq = 0;   //buffer的指针，作用同上；每次读取文件清零
            int bfpt_tosend_seq = 0;    //buffer的指针，作用同上；每次读取文件清零

            Boolean end = false;

            //每次读满一个缓冲区，然后使用GBN协议将缓冲区内数据全部传完；然后再读满，再传；循环这个过程直到文件被读完。

            System.out.println("开始发送文件("+filename+")至: " + dstAddr.toString() + ":" + dstPort);

            int bufferLength = 0;
            while ((bufferLength = fis.read(bs)) != -1 && !end) {
                bfpt_tosend_seq = 0;
                bfpt_waiting_seq = 0;
                // 开启2个新的线程: ACK接收器, 超时定时器
                MyAckRecver myAckRecver = new MyAckRecver(waitingSEQ, rwind);
                Thread thread_ACK = new Thread(myAckRecver);
                thread_ACK.start();
                
                MyTimer myTimer = new MyTimer();
                Thread thread_timer = new Thread(myTimer);

                // 开始将这一条缓冲区里的内容全部发出并ACK
                while(bfpt_tosend_seq != bufferLength){
                    // 检查是否收到了新ACK,并作出相应处理
                    rwind = myAckRecver.rwind;
                    if(myAckRecver.largestACK > waitingSEQ){
                        // 收到新的ACK,更新数据并检查Timer是关闭还是重置
                        int gap = myAckRecver.largestACK - waitingSEQ;
                        waitingSEQ += gap;
                        bfpt_waiting_seq += gap;
                        if(waitingSEQ == tosendSEQ){ //当前发的全部收到ACK
                            myTimer.reset();
                            thread_timer.wait(); //关闭计时器进入沉睡
                        }
                        else{   //仍在等待下一部分ACK, 重启计时器
                            myTimer.reset();
                            thread_timer.interrupt();  
                        }

                        // 阻塞控制: 快速恢复 & 拥塞避免
                        if(cwind < last_cwind)
                            cwind *= 2;
                        else
                            cwind += MAX_SEG_SIZE;

                    }

                    // 检查是否超时, 是的话Go back n
                    if(myTimer.timesup){
                        // 将即将发送的SEQ回退到最早需要ACK的位置
                        tosendSEQ = waitingSEQ;
                        bfpt_tosend_seq = bfpt_waiting_seq;
                        // 超时后 timeup 时间翻倍
                        myTimer.sleepTime *= 2;
                        if(myTimer.sleepTime > 2000){ //超时时间超过2s, 直接结束
                            end = true;
                            Utils.sendError("Connection cut off due to timeout");
                            break;
                        }

                        // 阻塞控制
                        last_cwind = cwind;
                        cwind /= 2;
                    }

                    // 本次发包的长度
                    int toSendSize = Utils.min3(
                            // 最长报文
                        MAX_SEG_SIZE,       
                            // 流量or阻塞窗口限制
                        Utils.min2(cwind, rwind) - (bfpt_tosend_seq - bfpt_waiting_seq),
                            // 缓冲区剩余数据
                        bufferLength - bfpt_tosend_seq    
                    );
                    
                    // 愉快发包
                    Utils.SetSeq(packet.getData(), tosendSEQ);
                    Utils.SetDataSize(packet.getData(), toSendSize);
                    Utils.SetData(packet.getData(), Utils.getPartialBytes(bs, bfpt_tosend_seq, bfpt_tosend_seq + toSendSize - 1));
                    try{
                        socket.send(packet);
                    } catch(Exception e){
                        Utils.sendError("Unknown socket error.");
                    }
                    // 若当前无定时器, 则开启超时定时器
                    if(!thread_timer.isAlive())
                        thread_timer.start();

                    tosendSEQ += toSendSize;
                    bfpt_tosend_seq += toSendSize;
                }
            }
            System.out.println("发送至" + dstAddr.toString() + ":" + dstPort + "完成.");
            fis.close();

            // 发送传输结束信号, 把SEQ设置为200
            Utils.SetSeq(packet.getData(), 200);
            socket.send(packet);

            //关闭socket，结束端口占用
            socket.close();
        }
        catch(Exception e){}
    }

    public class MyTimer implements Runnable{

        final int DEFAULT_SLEEP_TIME = 500; //初始超时时间设为500ms
        public int sleepTime;
        public Boolean timesup;
        public Boolean restart;

        public MyTimer(){
            sleepTime = DEFAULT_SLEEP_TIME; 
            timesup = false;
            restart = false;
        }

        public void reset(){
            restart = true;
            timesup = true;
            sleepTime = DEFAULT_SLEEP_TIME;
        }

        public void run(){
            do{
                restart = false;
                try{Thread.sleep(sleepTime);}catch(Exception e){}
            }
            while(restart);
            timesup = true;
        }
    }

    public class MyAckRecver implements Runnable{
        
        public int largestACK;
        public int rwind;
        DatagramPacket packet;
        Boolean exit;

        public MyAckRecver(int largestACK, int rwind){
            this.largestACK = largestACK;
            this.rwind = rwind;
            packet = new DatagramPacket(new byte[64], 64); //返回的packet一般不大
            exit = false;
        }

        public void run(){
            while(!exit){
                try{
                    SendHandler.this.socket.receive(packet);
                } catch(Exception e){
                    Utils.sendError("Unknown socket error.");
                }
                rwind = Utils.GetWindow(packet.getData());
                int getACK = Utils.GetACK(packet.getData());
                if(getACK > largestACK){
                    largestACK = getACK; 
                }
            }
        }
    }
}