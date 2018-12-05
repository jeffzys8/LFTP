import java.net.*;
import java.io.*;
import java.util.*;

public class Client {
    enum Command{
        upload,
        download
    }
    
    public static void sendError(String msg){
        System.out.println("Error: " + msg);
        System.exit(0);
    }

    public static void sendUsage(){
        System.out.println("Usage:");
        System.out.println("    Upload a file: lsend [serverName] [fileName]");
        System.out.println("    Doawnload a file: lget [serverName] [fileName]");
    }

    static final int SERVER_PORT = 6060;
    static final int START_PORT = 8000; // 寻找可用端口的起始端口
    public static void main(String[] args){
        String commandStr = null, serverName = null, fileName = null;
        Command command = Command.download;
        DatagramSocket socket;  // the udp socket
        DatagramPacket packet;  // the packet of udp socket
        int port; // the port of socket
    
        InetAddress serverAddr = null;
        try{
            commandStr = args[0];
            serverName = args[1];
            fileName = args[2];
        }
        catch(Exception e){
            sendUsage();
            sendError("Invalid arguments");
        }

        /* 读取指令 */
        if(commandStr.equals("lsend")){
            command = Command.upload;    
        }
        else if(commandStr.equals("lget")){
            command = Command.download;
        }
        else{
            sendUsage();
            sendError("No such command");
        }

        /* 地址解析 */
        try{
            serverAddr = InetAddress.getByName(serverName);
        }
        catch(Exception e){
            sendError("Invalid server name or address");
        }

        /* 获取可用端口 */
        port = START_PORT;
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

        /* (上传文件)检查文件是否可读 */
        if(command == Command.upload){
            File tempFile = new File(fileName);
            if(!tempFile.exists()){
                sendError("File doesn't exist");
            }
        }

        /* 向服务器发送请求 (在后面应该变成三次握手建立连接) */
        packet = new DatagramPacket(new byte[128],128);
        packet.setPort(SERVER_PORT);   // 设置服务器端口
        packet.setAddress(serverAddr);
        packet.setData(command.toString().getBytes());
        try{socket.send(packet);}
        catch(Exception e){
            sendError("Unknown socket error.");
        }

        /* 根据指令设置handler并进行新开线程进行处理 */
        Thread thread;
        if(command == Command.download){
            thread = new Thread(new ReceiveHandler(socket, fileName));
        }
        else{
            thread = new Thread(new SendHandler());
        }
        thread.start();
    }
}