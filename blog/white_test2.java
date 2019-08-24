/**
 * white_test2
 * 题目2
 */
public class white_test2 {

    public boolean check(int x,int y) {
        int i,j;
        for(i=0;i<8;i++) {
            if(a[i]==1) {
                return false;
            }
        }
        for(i=0;i<8;i++) {
            if(b[i]==1) {
                return false;
            }
        }
        i=x;j=y;
        while (i>0&&j>0) {
            i--;j--;
        }
        for (i = 0; i < 8 && j < 8; i++,j++) {
            if (a[i][j]==1) {
                return false;
            }
        }
        i=x;j=y;
        while (i>0&&j<7) {
            i--;
            j++;
        }
        return false;
    }
    public static void main(String[] args) {
        
    }
}