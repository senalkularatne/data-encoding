// BWA.java
// Burrows-Wheeler transform to reduce entropy
// Usage: java BWA < original > encoded

import java.io.*;
import java.util.*;

class BWA{
  static int BLOCKSIZE = 8192;
  static int numberOfSymbols = 256;
  int[] s = new int[BLOCKSIZE * 2];  // text block repeated twice for sorting
  int length = 0;  // length of block 
  int[] v = new int[BLOCKSIZE]; // vector for suffix sorting
  int[] L = new int[BLOCKSIZE];  // the Burrows-Wheeler transformation
  int I;  // the position of text after suffix sort
  int[] A = new int[numberOfSymbols]; 

 class Suffix implements Comparable{
  int position;
  public Suffix(int p){ position = p; }
  public int compareTo(Object obj){
    Suffix o = (Suffix)obj;
    int k = 0;
    while (k < length && s[position + k] == s[o.position + k]) k++;
    if (k == length) return position - o.position;
    else return s[position + k] - s[o.position + k];
  }
 }    

 void initializeA(){
    for (int i = 0; i < numberOfSymbols; i++) A[i] = i;    
 }

 void readBlock(){
   byte[] buffer = new byte[BLOCKSIZE];
   try{
     length = System.in.read(buffer);
   }catch(IOException e){
      System.err.println(e.getMessage());
      System.exit(1);
   }
   if (length <= 0) return;
    for (int i = 0; i < length; i++){
       int c = buffer[i];
       if (c < 0) c += 256; 
       s[i] = s[length + i] = c;
    }
 }

 void suffixSort(){
   TreeSet<Suffix> tset = new TreeSet<Suffix>();
   for (int i = 0; i < length; i++) tset.add(new Suffix(i));
   int j = 0;
   for (Suffix o: tset) v[j++] = o.position;
 }
      
 void wheeler(){ 
    L = new int[length];
    for (int i = 0; i < length; i++)
       if (v[i] == 0){
          I=i;   // position of text 
          L[i] = s[length - 1];   // The last character
       }
       else L[i] = s[v[i] - 1]; 
    System.out.write(I / 256); System.out.write(I % 256); // 2 bytes for I
 }

 void moveToFront(){
    int i,j,k;
    for (i = 0; i < length; i++){ 
       int t = L[i];
       for (j = 0; t != A[j]; j++); 
       System.out.write(j);        // j is the position of L[i]
       for (k = j; k > 0; k--) 
          A[k] = A[k-1];       // move L[i] to front
       A[0] = t; 
    }
 }

 void encode(){
    initializeA();
    while (true){
      readBlock();
      if (length <= 0) break;
      suffixSort();
      wheeler();
      moveToFront();
      if (length < BLOCKSIZE) break;
    }
    System.out.flush();
 }

  
 public static void main(String[] args){
    BWA bw = new BWA(); 
    bw.encode();
  }
 } 
