// HuffmanB.java
// Huffman decoder
// Usage:  java HuffmanB < encoded > original

import java.io.*;
import java.util.*;

public class HuffmanB{


  int[][] codetree = null;
  int buf = 0; int position = 0;
  int actualNumberOfSymbols = 0;
  int filesize = 0;


 void readTree(){  // read Huffman tree
  try{
   actualNumberOfSymbols = System.in.read();
   //System.err.println("ERRRORRR:" + actualNumberOfSymbols);
   codetree = new int[actualNumberOfSymbols * 2 - 1][2];
   
   for (int i = 0; i < actualNumberOfSymbols * 2 - 1; i++){
     codetree[i][0] = System.in.read();
     codetree[i][1] = System.in.read();
   }
   for (int i = 0; i < 3; i++){  // read filesize
     int a = System.in.read();
     filesize |= a << (i * 8);
   }
  } catch (IOException e){
     System.err.println(e);
     System.exit(1);
  }
 }

 int inputBit(){ // get one bit from System.in
   if (position == 0)
     try{
       buf = System.in.read();
       if (buf < 0){ return -1;
 }
            
       position = 0x80;
     }catch(IOException e){
        System.err.println(e);
        return -1;
     }
   int t = ((buf & position) == 0) ? 0 : 1;
   position >>= 1;  
   return t;
 }

 void decode(){  // Your two lines of code for updating k are needed for this to work.
  int bit = -1;   // next bit from compressed file: 0 or 1.  no more bit: -1
  int k = 0;  // index to the Huffman tree array; k = 0 is the root of tree
  int n = 0;  // number of symbols decoded, stop the while loop when n == filesize
  int numBits = 0; // number of bits in the compressed file
  while ((bit = inputBit()) >= 0){
    k = codetree[k][bit];
    numBits++;
    if (codetree[k][0] == 0){  // leaf
       System.out.write(codetree[k][1]);
       if (n++ == filesize) break; // ignore any additional bits
       k = 0;
    }
  }
  System.out.flush();
 }

 public static void main(String[] args){
      if(args.length > 1) {
         try {
             System.setIn(new FileInputStream(args[1]));
         } catch (IOException ex) {
             System.err.println("Failed to read file.");
         }
     }
  HuffmanB huffman = new HuffmanB();
  huffman.readTree();
  huffman.decode(); 
 }
}
