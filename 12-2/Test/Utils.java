public class Utils{
    final static int HEADER_COMMAND = 0;
    final static int HEADER_SEQ = 1;
    final static int HEADER_ACK = 5;
    final static int HEADER_WINDOW = 9;
    final static int HEADER_DATASIZE = 13;
    final static int HEADER_DATA = 17;

    public static void sendError(String msg){
        System.out.println("Error: " + msg);
        System.exit(0);
    }

    public static void sendUsage(){
        System.out.println("Usage:");
        System.out.println("    Upload a file: lsend [serverName] [fileName]");
        System.out.println("    Doawnload a file: lget [serverName] [fileName]");
    }

    public static int min3(int a, int b, int c){
        if(a < b && a < c)
            return a;
        else if(b < c)
            return b;
        else
            return c;
    }
    
    public static int min2(int a, int b){
        return a < b ? a : b;
    }

    public static byte[] intToBytes( int value ) 
	{ 
		byte[] src = new byte[4];
		src[3] =  (byte) ((value>>24) & 0xFF);
		src[2] =  (byte) ((value>>16) & 0xFF);
		src[1] =  (byte) ((value>>8) & 0xFF);  
		src[0] =  (byte) (value & 0xFF);				
		return src; 
    }
    
    public static byte[] getPartialBytes(byte[] src, int start, int end){
        byte[] result = new byte[end-start+1];
        for(int i = 0; i <= end-start; ++i){
            result[i] = src[start+i];
        }
        return result;
    }

    public static void BytesInsertInt( byte[] src, int value, int offset){
        byte[] valByte = intToBytes(value); //初始Seq#.设置为client_port*10
        for(int i = 0; i < 4; i++){
            src[i+offset] = valByte[i];
        }
    }

    public static void BytesInsertByte( byte[] src, byte[] toAdd, int offset){
        for(int i = 0; i < toAdd.length; i++){
            src[i+offset] = toAdd[i];
        }
    }

    public static void BytesInsertString( byte[] src, String str, int offset){
        byte[] strByte = str.getBytes();
        for(int i = 0; i < strByte.length; i++){
            src[i+offset] = strByte[i];
        }
    }

    public static int bytesToInt(byte[] src, int offset) {
		int value;	
		value = (int) ((src[offset] & 0xFF) 
				| ((src[offset+1] & 0xFF)<<8) 
				| ((src[offset+2] & 0xFF)<<16) 
				| ((src[offset+3] & 0xFF)<<24));
		return value;
    }

    public static void SetSeq(byte[] src, int seq){
        BytesInsertInt(src, seq, HEADER_SEQ); 
    }
    
    public static int GetSeq(byte[] src){
        return bytesToInt(src, HEADER_SEQ);
    }

    public static void SetACK(byte[] src, int ack){
        BytesInsertInt(src, ack, HEADER_ACK); 
    }

    public static int GetACK(byte[] src){
        return bytesToInt(src, HEADER_ACK);
    }

    public static int GetWindow(byte[] src){
        return bytesToInt(src, HEADER_WINDOW);
    }

    public static void SetDataSize(byte[] src, int size){
        BytesInsertInt(src, size, HEADER_DATASIZE);
    }

    public static void SetData(byte[] src, byte[] data){
        BytesInsertByte(src, data, HEADER_DATA);
    }

    public static void SetCommand(byte[] src, int command){
        src[0] = (byte) (command & 0xFF);
    }
}