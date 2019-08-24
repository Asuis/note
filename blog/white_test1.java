/**
 * white_test1
 * 题目1
 */
public class white_test1 {
    public static double atoe(char s[]) {
        double val = 0.;
        double power = .0;
        int i = 0;
        int sign = 0;
        int e = 0;
        char c = '+';
        sign = 0;

        int len = s.length;

        //判断正负
        if (s[i]=='+'|| s[i]=='-') {
            sign = (s[i++]=='+')?1:-1;
        }
        //整数处理
        for(val=0;s[i]>='0'&&s[i]<='9';i++){
            val = val*10+(s[i]-'0');
        }
        //小数处理
        if(s[i]=='.') {
            i++;
            for(power=1;s[i]>='0'&&s[i]<='9';i++){
                val=val*10+s[i]-'0';
                power*=10;
            }
            val = val/power;
        }
        //操作符处理
        if(s[i]=='+'||s[i]=='-'){
            c = s[i];
            i++;
        }
        //指数处理
        for(e=0;s[i]>='0'&&s[i]<='9';i++) {
            e = e*10+(s[i]-'0');
        }
        
        if(c=='+'){
            for(i=e;i>0;i--) {
                val*=10;
            }
        }else {
            for(i=e;i>0;i--){
                val/=10;
            }
        }
        return val*sign;
    }
    public static void main(String[] args) {
        double b = atoe(new char[]{'+','1','.','5','+','1','0','.'});
        System.out.println(b);
    }
}