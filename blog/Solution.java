public class Solution {
    public static void reOrderArray(int [] array) {
        int arr[] = new int[array.length];
        int count = 0;
        for(int i=0;i<arr.length;i++){
            if(array[i]%2==1){
                arr[count] = array[i];
                count++;
            }
        }
        for(int i=0;i<arr.length;i++){
            if(array[i]%2==0){
                arr[count] = array[i];
                count++;
            }
        }
        array = arr;
        for (int i = 0; i < arr.length; i++) {
            System.out.println(array[i]);
        }
    }
    public static void main(String[] args) {
        reOrderArray(new int[]{1,2,3,4,5,6,7});
    }
}