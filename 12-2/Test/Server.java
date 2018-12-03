import java.net.*;
import java.io.*;

public class Server {

    final static int COMMAND_GET = 3;
    final static int COMMAND_SEND = 7;

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

    final static int COMMAND_GET = 3;
    final static int COMMAND_SEND = 7;

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

        /* first handshake - 获取Request的指令, 初始SEQ值 */
        byte[] packetData = client_request;
        int command = packetData[Utils.HEADER_COMMAND];
        int startSEQ = Utils.GetSeq(packetData);
        int fnSize = Utils.bytesToInt(packetData, Utils.HEADER_DATASIZE); //获取数据长度(这里是文件名)
        String fileName = new String(Utils.getPartialBytes(packetData, Utils.HEADER_DATA, Utils.HEADER_DATA+fnSize-1));

        /* second handshake - 向Client发送反馈 (需在send后启动定时器) */
        packetData = new byte[128];
        if(command == 3){   //Client请求下载
            // 检测文件是否存在
            File tempFile = new File(fileName);
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
        try{socket.receive(packet);}catch(Exception e){}
        

        /* 连接建立，新建线程进行处理(和client正好是相反的) */
        Thread thread;
        if(command == COMMAND_GET){
            thread = new Thread(new SendHandler(socket, fileName, client_address, client_port, startSEQ));
        }
        else{
            
            thread = new Thread(new ReceiveHandler(socket, fileName, client_address, client_port, startSEQ));
        }
        thread.start();
    }
}