class BubbleSort {
 public static void main(String[] a) {
  System.out.println(new BBS().Init(10));
 }
}


// This class contains the array of integers and
// methods to initialize, print and sort the array
// using Bublesort
class BBS {

 int[] number;
 int size;

 // Initialize array of integers
 public int Init(int sz) {
  size = sz;
  number = new int[sz];

  number[0] = 20;
  number[1] = 7;
  number[2] = 12;
  number[3] = 18;
  number[4] = 2;
  number[5] = 11;
  number[6] = 6;
  number[7] = 9;
  number[8] = 19;
  number[9] = 5;

  return 0;
 }

}
