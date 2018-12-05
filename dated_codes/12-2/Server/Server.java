import java.net.*;
import java.io.*;

public class Server {

    static final int SERVER_PORT = 6060;   // 连接端开始端口号
    static final int START_SERVER_PORT = 15000;  // 用于传输的最小端口号
    
    enum Command{
        upload,
        download
    }

    public static void sendError(String msg){
        System.out.println("Error: " + msg);
        System.exit(0);
    }

    public static void main(String[] args) {
        DatagramSocket listen_server = null;
        DatagramPacket listen_packet = null;
        try {
            listen_server = new DatagramSocket(SERVER_PORT);   //设置固定的服务端口
            listen_packet = new DatagramPacket(new byte[128], 128);
        }
        catch(Exception e){
            sendError("Fail to start server.");
        }

        //循环对到来的连接进行监听
        while(true){
            try{
                System.out.println("Server starts listening.");
                listen_server.receive(listen_packet); //阻塞, 接受客户端的连接请求
            }
            catch(Exception e){
                sendError("Unknown socket error.");
                listen_server.close();
                System.exit(0);
            }            
            System.out.println("New request: " + listen_packet.getAddress().toString() + ":" + listen_packet.getPort());

            // 新开线程处理该用户
            Runnable handler= new Handler(listen_packet.getAddress(), listen_packet.getPort(), listen_packet.getData());
            Thread thread = new Thread(handler);
            thread.start();
        }
    }
}



class Handler implements Runnable{

    int client_port;
    InetAddress client_address;
    byte[] client_request;


    Handler(InetAddress ca, int cp, byte[] req){
        this.client_address = ca;
        this.client_port = cp;
        this.client_request = req;
    }

    public void run(){

        // 获取可用端口，启动一个新的socket专门处理该用户
        int port = Server.SERVER_PORT;
        DatagramSocket socket = null;
        while(true){
            try{
                socket = new DatagramSocket(port);
            }
            catch(Exception e){  
                port++;
                continue;
            }
            break;
        }

        /* first handshake - 获取Request的指令 */
        byte[] packetData = client_request;
        int command = packetData[Utils.HEADER_COMMAND];
        int seq = Utils.GetSeq(packetData);
        int fnSize = Utils.bytesToInt(packetData, Utils.HEADER_DATASIZE); //获取数据长度(这里是文件名)
        String filename = new String(Utils.getPartialBytes(packetData, Utils.HEADER_DATA, Utils.HEADER_DATA+fnSize-1));

        /* second handshake - 向Client发送反馈 (需在send后启动定时器) */
        packetData = new byte[128];
        if(command == 3){   //Client请求下载
            // 检测文件是否存在
            File tempFile = new File(filename);
            if(tempFile.exists()){
                Utils.SetACK(packetData, 200);
            } else{
                Utils.SetACK(packetData, 404);
            }
        }
        else if(command == 7){  //客户请求上传
            Utils.SetACK(packetData, 200);
        }
        else{
            // 返回该指令无效
            Utils.SetACK(packetData, 401);
        }
        DatagramPacket packet = new DatagramPacket(packetData, 128, client_address, client_port);
        try{socket.send(packet);}catch(Exception e){} //二次握手同时告知Client新端口
            

        /* third handshake - 接收Client的确认信息 */
        socket.receive(packet);
        

        //进行传输(需要搬移到SendHandler)
        try{
            
            packet.setPort(client_port);
            packet.setAddress(client_address);
            FileInputStream fis = null;
            File file = new File(filename);
            
            fis = new FileInputStream(file);
            byte[] bs = new byte[64];    //IO缓冲区(也即每次发送包大小)设置为64bytes

            System.out.println("开始发送文件("+filename+")至: " + client_address.toString() + ":" + client_port);
            int count = 0, length = 0;
            while ((length = fis.read(bs)) != -1) {
                packet.setData(bs, 0, length);
                socket.send(packet);
                ++count;
                // Thread.sleep(10);  //沉睡500ms
            }
            System.out.println("发送至" + client_address.toString() + ":" + client_port + "完成。总共发送包:" + count);
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