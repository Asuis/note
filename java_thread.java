public class java_thread implements Runnable{
    @Override
    public void run() {
        System.out.print("hello\n");
    }
    public static void main(String[] args) {
        Thread t = new Thread(new java_thread(),"hello");
        t.start();
        
    }
}