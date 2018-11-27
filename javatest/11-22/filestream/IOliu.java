import java.io.*;
import java.util.*;
public class IOliu {

    public static void main(String[] args) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            File f1 = new File(args[0]);
            File f2 = new File(args[1]);

            fis = new FileInputStream(f1);
            fos = new FileOutputStream(f2);

            byte[] bs = new byte[50000];
            Date d = new Date();
            int i;
            System.out.println("不带缓冲的 50000B 开始：" + d.toString());
            while ((i = fis.read(bs)) != -1) {
                fos.write(bs, 0, i);
            }
            Date d2 = new Date();
            System.out.println("不带缓冲的 50000B 结束：" + d2.toString());

        } catch (IOException ex) {
            System.out.println(ex);
        } finally {
            try {
                fis.close();
                fos.close();
            } catch (IOException ex) {
                System.out.println(ex);
            }
        }
    }
}