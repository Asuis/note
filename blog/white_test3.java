/**
 * white_test3
 * 题目3
 */
public class white_test3 {

    int leap = 0;
    int isleap(int year) {
        if (year%4==0) {
            if(year%100==0) {
                if(year%400==0) {
                    leap = 1;
                    return leap;
                }else {
                    leap = 0;
                    return leap;
                }
            }else{
                leap = 1;
                return leap;
            }
        }else {
            leap = 0;
            return leap;
        }
    }
    public static void main(String[] args) {
        System.out.println("arg0");
    }
}