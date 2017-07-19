// HuffmanA.java
// Huffman encoder
// Usage: java HuffmanA original > encoded

import java.io.*;
import java.util.*;

class Node implements Comparable{
  Node left, right;
  int symbol;
  int frequency;
  public Node(Node l, Node r, int s, int f){
    left = l; right = r; symbol = s; frequency = f;
  }
  public int compareTo(Object obj){
   Node n = (Node)obj;
   return frequency - n.frequency;
  }
}

public class HuffmanA{

  static final int numberOfSymbols = 256;
  static final int blockSize = 1024;
  int[] freq = new int[numberOfSymbols];
  Node tree = null;
  String[] codewords = new String[numberOfSymbols];
  int[][] codetree = null; // Huffman tree with actualNumberOfSymbols leaves 
  int buf = 0; int position = 0;  // used by outputbits()
  int actualNumberOfSymbols = 0;  // number of symbols with freq > 0
  int filesize = 0;

  void count(String filename){ // count symbol frequencies
    byte[] buffer = new byte[blockSize];
    FileInputStream fis = null;
    try {
      //filename = "compressed.huf";
      fis = new FileInputStream(filename);
    } catch (FileNotFoundException e){
      System.err.println(filename + " not found");
      System.exit(1);
    }
    int len = 0;
    for (int i = 0; i < numberOfSymbols; i++) freq[i] = 0;
   try {
    while ((len = fis.read(buffer)) >= 0){
      for (int i = 0; i < len; i++){
       int symbol = buffer[i];
       if (symbol < 0) symbol += 256;
       freq[symbol]++;
      }
      filesize += len;
    }
    fis.close();
   } catch (IOException e){
      System.err.println("IOException");
      System.exit(1);
   }
  }

 void entropy(){
   double sum = 0;
   for (int i = 0; i < numberOfSymbols; i++) if (freq[i] > 0){
     actualNumberOfSymbols++;
     sum += freq[i] * Math.log(((double)freq[i]) / filesize);
   }
   sum /= filesize * Math.log(2.0);
   System.out.println(actualNumberOfSymbols + " " + -sum);
 }

  void makeTree(){  // make Huffman prefix codeword tree
   PriorityQueue<Node> pq = new PriorityQueue<Node>();
   for (int i = 0; i < numberOfSymbols; i++) if (freq[i] > 0){
       actualNumberOfSymbols++;
       pq.add(new Node(null, null, i, freq[i]));
   }
   int nodeLabel = numberOfSymbols;
   while (pq.size() > 1){
     Node a = pq.poll(); Node b = pq.poll();  // remove two subtress
     pq.add(new Node(a, b, nodeLabel++, a.frequency + b.frequency)); 
        // add the merged subtree
   }
   tree = pq.poll();  // root of tree as the last single subtree
  }

  void dfs(Node n, String code){  // generate all codewords
    if (n.symbol >= numberOfSymbols){  // internal node
      dfs(n.left, code + "0"); dfs(n.right, code + "1");
    }else codewords[n.symbol] = code;  // leaf
  }

  void makeCodewords(){
    dfs(tree, "");
  }

 void buildTreeArray(){  // make an array for the tree
  codetree = new int[actualNumberOfSymbols * 2 - 1][2];
  int treeSize = 1;
  for (int i = 0; i < actualNumberOfSymbols * 2 - 1; i++)
    codetree[i][0] = codetree[i][1] = 0;
  for (int i = 0; i < numberOfSymbols; i++) 
   if (codewords[i] != null){
    int len = codewords[i].length();
    int k = 0;
    for (int j = 0; j < len; j++){
      int side = codewords[i].charAt(j) - '0';
      if (codetree[k][side] <= 0) codetree[k][side] = treeSize++;
      k = codetree[k][side];
    }
    codetree[k][1] = i;
  }
 }
    
  void outputTree(){
    System.out.write(actualNumberOfSymbols);  // number of used symbols
    for (int i = 0; i < actualNumberOfSymbols * 2 - 1; i++){  // Huffman tree
      System.out.write(codetree[i][0]);
      System.out.write(codetree[i][1]);
    }
    for (int i = 0; i < 3; i++){  // three bytes for filesize
      System.out.write(filesize & 0xff);
      filesize >>= 8; 
    }
  }
    
  void encode(String filename){ // compress filename to System.out
    byte[] buffer = new byte[blockSize];
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(filename);
    } catch (FileNotFoundException e){
      System.err.println(filename + " not found");
      System.exit(1);
    }
    int len = 0;
   try {
    while ((len = fis.read(buffer)) >= 0){
      for (int i = 0; i < len; i++){
       int symbol = buffer[i];
       if (symbol < 0) symbol += 256;
       outputbits(codewords[symbol]);
      }
    }
    fis.close();
   } catch (IOException e){
      System.err.println("IOException");
      System.exit(1);
   }
    if (position > 0){ System.out.write(buf << (8 - position)); }
    System.out.flush();
  }

  void outputbits(String bitstring){ // output codeword
     for (int i = 0; i < bitstring.length(); i++){
      buf <<= 1;
      if (bitstring.charAt(i) == '1') buf |= 1;
      position++;
      if (position == 8){
         position = 0;
         System.out.write(buf);
         buf = 0;
      }
     }
  }
    

  public static void main(String[] args){
    if (args.length < 1){
     System.err.println("Usage: java HuffmanA original > encoded");
     return;
    }
    HuffmanA huffman = new HuffmanA();
    huffman.count(args[0]);
    huffman.makeTree();
    huffman.makeCodewords();
    huffman.buildTreeArray(); 
    huffman.outputTree();
    //huffman.encode("compressed.huf"); 
    huffman.encode(args[0]);
  }
}
