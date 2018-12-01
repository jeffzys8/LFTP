import java.net.*;
import java.io.*;

public class Server {

    static final int SERVER_PORT = 6060;   // 连接端开始端口号
    static final int START_SERVER_PORT = 15000;  // 用于传输的最小端口号
    
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
                System.out.println("服务器开始监听");
                listen_server.receive(listen_packet); //阻塞, 接受客户端的连接请求
            }
            catch(Exception e){
                sendError("Fail to accept client's requst.");
            }
            // 新建一个线程，传递Handler对象对传输进行处理
            System.out.println("New request: " + listen_packet.getAddress().toString() + ":" + listen_packet.getPort());

            // 获取可用端口，启动socket
            int server_port = START_SERVER_PORT;
            while(true){
                try{
                    socket = new DatagramSocket(port); //客户端自选端口
                }
                catch(Exception e){  
                    port++;
                    continue;
                }
                break;
            }

            // 新建线程进行处理
            Thread thread = new Thread(new SendHandler(socket, "local.txt"));
            thread.start();
            
            // Runnable handler= new Handler(listen_packet.getAddress(), listen_packet.getPort(), args[0]);
            // Thread thread = new Thread(handler);
        }
    }
}

class Handler implements Runnable{

    int server_port;
    int client_port;
    String filename;
    InetAddress client_address;


    Handler(InetAddress ca, int cp, String fn){
        this.client_address = ca;
        this.client_port = cp;
        this.filename = fn;
    }

    public void run(){

        //这里需要调用一个线程安全的获取空闲端口的方法，此处先不做。


        //进行传输
        try{
            server_port = ++current_server_port;
            DatagramSocket socket = new DatagramSocket(server_port);
            DatagramPacket packet = new DatagramPacket(new byte[128],128);
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