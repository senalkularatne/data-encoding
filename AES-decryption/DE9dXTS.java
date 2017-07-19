// DE9dXTS.java
// Implementing XTS-AES decryption
// Usage: java DE9dXTS doublekey tweak < DE9testXTS.de9 > original.txt

import java.io.*;
import java.util.*;

public class DE9dXTS{

  static final int numberOfBits = 8;
  static final int fieldSize = 1 << numberOfBits;
  static final int irreducible = 0x11b;
  static final int logBase = 3;
  static final int msb = 0x80;
  static final int modulus = 0x87;
  static final byte[][] A = new byte[][] {
            {1, 1, 1, 1, 1, 0, 0, 0},
            {0, 1, 1, 1, 1, 1, 0, 0},
 	    {0, 0, 1, 1, 1, 1, 1, 0},
            {0, 0, 0, 1, 1, 1, 1, 1},
	    {1, 0, 0, 0, 1, 1, 1, 1},
            {1, 1, 0, 0, 0, 1, 1, 1},
            {1, 1, 1, 0, 0, 0, 1, 1},
            {1, 1, 1, 1, 0, 0, 0, 1}
	};
  static final byte[] B = new byte[] { 0, 1, 1, 0, 0, 0, 1, 1};  
  static final byte[][] G = new byte[][] {
            {2, 1, 1, 3},
            {3, 2, 1, 1},
            {1, 3, 2, 1},
            {1, 1, 3, 2}
        };
  static final byte[][] Gi = new byte[][] {
            {14, 9, 13, 11},
            {11, 14, 9, 13},
            {13, 11, 14, 9},
            {9, 13, 11, 14}
        };
  int[] alog = new int[fieldSize];
  int[] log = new int[fieldSize];
  int[] S = new int[fieldSize];
  int[] Si = new int[fieldSize];
  static final int blockSize = 16;
  static final int numberOfRounds = 11;
  int[] state = new int[blockSize];
  int[][] roundKey1 = new int[numberOfRounds][blockSize];  
  int[][] roundKey2 = new int[numberOfRounds][blockSize];  
  String hexkey = null;
  int[] T = new int[blockSize];

  int modMultiply(int a, int b, int m){
    int product = 0;
    for (; b > 0; b >>= 1){
      if ((b & 1) > 0) product ^= a;
      a <<= 1;
      if ((a & fieldSize) > 0) a ^= m;
    }
    return product;
  }    

  void makeLog(){
    alog[0] = 1;
    for (int i = 1; i < fieldSize; i++)
      alog[i] = modMultiply(logBase, alog[i - 1], irreducible);
    for (int i = 1; i < fieldSize; i++) log[alog[i]] = i;
  }

  int logMultiply(int a, int b){
    return (a == 0 || b == 0) ? 0 : alog[(log[a] + log[b]) % (fieldSize - 1)];
  }

  int multiplicativeInverse(int a){
    return alog[fieldSize - 1 - log[a]];
  }

  void buildS(){
     int[] bitColumn = new int[8];
     for (int i = 0; i < fieldSize; i++){
       int inverse = i < 2 ? i : multiplicativeInverse(i);
       for (int k = 0; k < 8; k++)
           bitColumn[k] = inverse >> (7 - k) & 1;
       S[i] = 0;
       for (int k = 0; k < 8; k++){
          int bit = B[k];
          for (int l = 0; l < 8; l++)
            if (bitColumn[l] == 1) bit ^= A[k][l];
          S[i] ^= bit << 7 - k;
       }
       Si[S[i]] = i;
    }
  }

 int readBlock(){
   byte[] data = new byte[blockSize];
   int len = 0;
   try {
     len = System.in.read(data);
   } catch (IOException e){
     System.err.println(e.getMessage());
     System.exit(1);
   }
   if (len <= 0) return len;
   for (int i = 0; i < len; i++){
     if (data[i] < 0) state[i] = data[i] + fieldSize;
     else state[i] = data[i];
   }
   return len;
 }

  void subBytes(){
    for (int i = 0; i < blockSize; i++) 
      state[i] = S[state[i]];
  }

 void shiftRows(){
   int temp = state[2]; state[2] = state[10]; state[10] = temp;
   temp = state[6]; state[6] = state[14]; state[14] = temp;
   temp = state[1]; state[1] = state[5]; state[5] = state[9]; 
   state[9] = state[13]; state[13] = temp;
   temp = state[3]; state[3] = state[15]; state[15] = state[11];
   state[11] = state[7]; state[7] = temp;
 }

  void mixColumns(){
   int[] temp = new int[4];
   for (int k = 0; k < 4; k++){
    for (int i = 0; i < 4; i++){
      temp[i] = 0;
      for (int j = 0; j < 4; j++)  
        temp[i] ^= logMultiply(G[j][i], state[k * 4 + j]);
    }
    for (int i = 0; i < 4; i++) state[k * 4 + i] = temp[i];
   }
  }


  void inverseSubBytes(){
    for (int i = 0; i < blockSize; i++) 
      state[i] = Si[state[i]];
  }

 void inverseShiftRows(){
   // Your code from DE8
 }

  void inverseMixColumns(){
   int[] temp = new int[4];
   for (int k = 0; k < 4; k++){
    for (int i = 0; i < 4; i++){
      temp[i] = 0;
      for (int j = 0; j < 4; j++)  
        temp[i] ^= logMultiply(Gi[j][i], state[k * 4 + j]);
    }
    for (int i = 0; i < 4; i++) state[k * 4 + i] = temp[i];
   }
  }

 void readKey(String filename){
   Scanner in = null;
   try {
     in = new Scanner(new File(filename));
   } catch (FileNotFoundException e){
     System.err.println(filename + " not found");
     System.exit(1);
   }
   hexkey = in.nextLine();
   in.close();
 }

 void expandKey(){
   for (int i = 0; i < blockSize; i++){ 
    roundKey1[0][i] = Integer.parseInt(hexkey.substring(i * 2, (i + 1) * 2), 16);
    int j = blockSize + i;
    roundKey2[0][i] = Integer.parseInt(hexkey.substring(j * 2, (j + 1) * 2), 16);
   }
   int rcon = 1;
   for (int i = 1; i < numberOfRounds; i++){  
     roundKey1[i][0] = S[roundKey1[i-1][13]] ^ rcon;
     rcon <<= 1; if (rcon > 0xFF) rcon ^= irreducible;
     roundKey1[i][1] = S[roundKey1[i-1][14]];
     roundKey1[i][2] = S[roundKey1[i-1][15]];
     roundKey1[i][3] = S[roundKey1[i-1][12]];
     for (int k = 0; k < 4; k++) 
        roundKey1[i][k] ^= roundKey1[i-1][k];
     for (int k = 4; k < blockSize; k++) 
        roundKey1[i][k] = roundKey1[i][k-4] ^ roundKey1[i-1][k];
   }
   rcon = 1;
   for (int i = 1; i < numberOfRounds; i++){  
     roundKey2[i][0] = S[roundKey2[i-1][13]] ^ rcon;
     rcon <<= 1; if (rcon > 0xFF) rcon ^= irreducible;
     roundKey2[i][1] = S[roundKey2[i-1][14]];
     roundKey2[i][2] = S[roundKey2[i-1][15]];
     roundKey2[i][3] = S[roundKey2[i-1][12]];
     for (int k = 0; k < 4; k++) 
        roundKey2[i][k] ^= roundKey2[i-1][k];
     for (int k = 4; k < blockSize; k++) 
        roundKey2[i][k] = roundKey2[i][k-4] ^ roundKey2[i-1][k];
   }
 }

 void addRoundKey2(int round){
   for (int k = 0; k < blockSize; k++) 
      state[k] ^= roundKey2[round][k];
 }

 void inverseAddRoundKey1(int round){ // Your code from DE8
    for (int k = 0; k < blockSize; k++) 
   	  {
         state[k] ^= roundKey2[(numberOfRounds-1)-round][k];
      }
 }

  void blockDecipher1(){
    inverseAddRoundKey1(0);
    for (int i = 1; i < numberOfRounds; i++){
      inverseSubBytes();
      inverseShiftRows();
      inverseAddRoundKey1(i);
      if (i < numberOfRounds - 1) inverseMixColumns();
    }
  }

  void blockCipher2(){
    addRoundKey2(0);
    for (int i = 1; i < numberOfRounds; i++){
      subBytes();
      shiftRows();
      if (i < numberOfRounds - 1) mixColumns();
      addRoundKey2(i);
    }
  }

  void xdx(){ // XTS-AES block decryption
    addBlock(state, T);
    blockDecipher1();
    addBlock(state, T);
  }

 void readTweak(String filename){
   Scanner in = null;
   try {
     in = new Scanner(new File(filename));
   } catch (FileNotFoundException e){
     System.err.println(filename + " not found");
     System.exit(1);
   }
   String tweak = in.nextLine();
   in.close();
   for (int i = 0; i < blockSize; i++) 
    state[i] = Integer.parseInt(hexkey.substring(i * 2, (i + 1) * 2), 16);
   blockCipher2();
   copyBlock(T, state);
 }  

 void printBlock(int[] block){
   for (int k = 0; k < blockSize; k++)
     if (block[k] < 16) System.out.print("0" + Integer.toHexString(block[k]));
     else System.out.print(Integer.toHexString(block[k]));
   System.out.println();
 }
   

 void Tx2(){
   boolean carry = ((T[0] & msb) != 0);
   for (int i = 0; i < blockSize - 1; i++){
     if ((T[i] & msb) != 0) T[i] ^= msb;
     T[i] <<= 1;
     if ((T[i + 1] & msb) != 0) T[i] |= 1;
   }
   if ((T[blockSize - 1] & msb) != 0) T[blockSize - 1] ^= msb;
   T[blockSize - 1] <<= 1;
   if (carry) T[blockSize - 1] ^= modulus;
 }   

 void writeBlock(int[] block, int len){
   byte[] data = new byte[blockSize];
   for (int i = 0; i < len; i++)
     data[i] = (byte)(block[i]);   
   System.out.write(data, 0, len);
 }

 void addBlock(int[] destination, int[] source){
   for (int k = 0; k < blockSize; k++) 
      destination[k] ^= source[k];
 }

 void copyBlock(int[] destination, int[] source){
   for (int k = 0; k < blockSize; k++) 
      destination[k] = source[k];
 }

  void decrypt(){
    int[] lastBlock = new int[blockSize];
    int[] currentBlock = new int[blockSize];
    int[] lastT = new int[blockSize];
    int len = readBlock();
    copyBlock(lastBlock, state);
    while ((len = readBlock()) >= 0){
      if (len == blockSize){
        copyBlock(currentBlock, state);
        copyBlock(state, lastBlock);
        xdx();
        writeBlock(state, blockSize);
        copyBlock(lastBlock, currentBlock);
        Tx2();
      }else{
      	writeBlock(T, lastT);
      	Tx2();
      	copyBlock(lastBlock, state);
      	xdx();
      	writeBlock(state, lastBlock);
      	copyBlock(len, state);
      	copyBlock(lastT, T);
      	xdx();
      	writeBlock(state, currentBlock);
      	writeBlock(len, lastBlock);
      }
    }
    System.out.flush();
  } 


public static void main(String[] args){
   if (args.length < 2){
     System.err.println("Usage: java DE9dXTS doublekey tweak < DE9testXTS.de9 > original.txt");
     return;
   }
   DE9dXTS de9 = new DE9dXTS();
   de9.makeLog();
   de9.buildS(); 
   de9.readKey("stallingskey.txt");
   de9.expandKey();
   de9.readTweak("tweak.txt");
   de9.decrypt();
}
}
