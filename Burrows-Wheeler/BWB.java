// BWB.java
// Burrows Wheller decoder
// Usage:  java BWB < encoded > original

import java.io.*;
import java.lang.*;

class BWB{
  static int BLOCKSIZE = 8192;
  static int numberOfSymbols = 256;
  int length = 0;  // length of block 
  int[] A = new int[numberOfSymbols]; 
  int[] L = new int[BLOCKSIZE];  // the Burrows-Wheeler transformation
  int[] F = new int[BLOCKSIZE]; 
  int[] T = new int[BLOCKSIZE];
  int I;  // the position of text after suffix sort

 void initializeA(){
    for (int i = 0; i < numberOfSymbols; i++) A[i] = i;    
 }
  
 void readBlock(){
   byte[] buffer = new byte[BLOCKSIZE + 2];
   try{
     length = System.in.read(buffer) - 2;
   }catch(IOException e){
      System.err.println(e.getMessage());
      System.exit(1);
   }
   if (length <= 0) return;
   int i1 = buffer[0]; if (i1 < 0) i1 += 256;
   int i0 = buffer[1]; if (i0 < 0) i0 += 256;
   I = i1 * 256 + i0;
   for (int i = 0; i < length; i++){
      int j = buffer[i + 2];
      if (j < 0) j += 256; 
      int t = A[j];
      L[i] = t;
      for (int k = j; k > 0; k--) A[k] = A[k-1]; 
      A[0] = t; // move to front
   }
 }

 void deBW(){
   for (int i = 0; i < length; i++){
     int j = i - 1; for (; j >= 0; j--)
       if (L[i] < F[j]) F[j + 1] = F[j];
       else break;
     F[j + 1] = L[i];
   }
   int j = 0;
   for (int i = 0; i < length; i++){
    if (i > 0 && F[i] > F[i - 1]) j = 0;
    for (; j < length; j++) if (L[j] == F[i]) break;
    T[i] = j++;
   }
   j = I;
   for (int i = 0; i < length; i++){
     System.out.write(F[j]);
     j = T[j];
   }
 }

 void decode(){
   initializeA();
   while (true){
     readBlock();
     if (length <= 0) return;
     deBW();
     if (length < BLOCKSIZE) return;
   }
 }

 public static void main(String[] args){
  BWB bw = new BWB();
  bw.decode();
 }
}

