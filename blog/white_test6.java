import java.util.Scanner;

/**
 * white_test6
 */
public class white_test6 {

    static void c(int x,int y) {
        if(x>10&&y>10) {
            int i = 1;
            if (x>y) {
                while (x*i%y!=0) {
                    i++;
                }
                System.out.println(x*i);
            }else{
                while (y*i%x!=0) {
                    i++;
                }
                System.out.println(y*i);
            }
        }
    }
}