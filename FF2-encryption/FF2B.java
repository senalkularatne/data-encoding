// FF2A.java
// Implementing FF2 decryption
// Usage: java FF2B key string

import java.io.*;
import java.util.*;
import java.math.BigInteger;

public class FF2B{

  static final int numberOfBits = 8;
  static final int fieldSize = 1 << numberOfBits;
  static final int irreducible = 0x11b;
  static final int logBase = 3;
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
  int[] alog = new int[fieldSize];
  int[] log = new int[fieldSize];
  int[] S = new int[fieldSize];
  static final int blockSize = 16;
  static final int numberOfRounds = 11;
  int[] state = new int[blockSize];
  int[][] roundKey = new int[numberOfRounds][blockSize];  
  BigInteger radix = new BigInteger("26");
  int n; // length of string
  int u, v;
  int[] XA; int[] XB;  // numeral strings, A nd B in Figure 7.16

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
    }
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

 void readKey(String filename){
   Scanner in = null;
   try {
     in = new Scanner(new File(filename));
   } catch (FileNotFoundException e){
     System.err.println(filename + " not found");
     System.exit(1);
   }
   String hexkey = in.nextLine();
   for (int i = 0; i < blockSize; i++) roundKey[0][i] = 
     Integer.parseInt(hexkey.substring(i * 2, (i + 1) * 2), 16);
   in.close();
 }

 void expandKey(){
   int rcon = 1;
   for (int i = 1; i < numberOfRounds; i++){  
     roundKey[i][0] = S[roundKey[i-1][13]] ^ rcon;
     rcon <<= 1; if (rcon > 0xFF) rcon ^= irreducible;
     roundKey[i][1] = S[roundKey[i-1][14]];
     roundKey[i][2] = S[roundKey[i-1][15]];
     roundKey[i][3] = S[roundKey[i-1][12]];
     for (int k = 0; k < 4; k++) 
        roundKey[i][k] ^= roundKey[i-1][k];
     for (int k = 4; k < blockSize; k++) 
        roundKey[i][k] = roundKey[i][k-4] ^ roundKey[i-1][k];
   }
 }

 void addRoundKey(int round){
   for (int k = 0; k < blockSize; k++) 
      state[k] ^= roundKey[round][k];
 }

  void blockCipher(){
    addRoundKey(0);
    for (int i = 1; i < numberOfRounds; i++){
      subBytes();
      shiftRows();
      if (i < numberOfRounds - 1) mixColumns();
      addRoundKey(i);
    }
  }

  void getString(String X){
	n = X.length();
	u = n / 2;
	v = n - u;
	XA = new int[u]; XB = new int[v];
	for (int i = 0; i < u; i++) XA[i] = X.charAt(i) - 'a';
	for (int i = 0; i < v; i++) XB[i] = X.charAt(u + i) - 'a';
  }	

  BigInteger num(int[] X){
	byte[] oneByte = new byte[1];
	oneByte[0] = (byte)(X[0]);
	BigInteger number = new BigInteger(oneByte);
	for (int i = 1; i < X.length; i++){
		oneByte[0] = (byte)(X[i]);
		number = number.multiply(radix).add(new BigInteger(oneByte));
	}
	return number;
  }

  void computeJ(){  // step 4 of Figure 7.15, J is state
	state[0] = radix.intValue();
	for (int i = 1; i < blockSize; i++) state[i] = 0;
	state[2] = n;
	blockCipher();
	for (int i = 0; i < blockSize; i++) roundKey[0][i] = state[i];
	expandKey();
  }

  void FF2rounds(){  // step 5 of Algorithm FF2, Figure 7.15
	byte[] blockInBytes = new byte[blockSize + 1]; // for BigInteger use
	for (int i = 0; i < 10; i++){
		state[0] = i;  // making Q in Figure 7.15 5.i
		byte[] byteArray = num(XB).toByteArray();
		int len = byteArray.length;
		for (int j = 0; j < len; j++){
			int t = byteArray[j];
			if (t < 0) t += 256;
			state[blockSize - len + j] = t;
		}
		for (int j = 1; j < blockSize -len; j++) state[j] = 0;
		blockCipher();	 // Y <- CIPH_J(Q), 5.ii 
		for (int j = 0; j < blockSize; j++) blockInBytes[j + 1] = (byte)(state[j]);
		blockInBytes[0] = 0;  // so that y is not negative
		BigInteger y = new BigInteger(blockInBytes);
		int m = (i % 2 == 0) ? u : v;  // 5.iv
		BigInteger c = num(XA).add(y);  // 5.v
		int[] C = new int[m];  // 5.vi
		for (int j = m - 1; j >= 0; j--){
			BigInteger[] qr = c.divideAndRemainder(radix);
			C[j] = qr[1].intValue();  
			c = qr[0];
		}
		XA = XB; XB = C; // 5.vii and 5.viii
	}
 }

void decrypted(){
	
}
public static void main(String[] args){
   if (args.length < 2){
     System.err.println("Usage: java FF2B keyfile string");
     return;
   }
   FF2A ff2 = new FF2A();
   ff2.makeLog();
   ff2.buildS(); 
   ff2.readKey(args[0]);
   ff2.expandKey();
   ff2.getString(args[1]);
   ff2.computeJ();
   ff2.FF2rounds();
   //ff2.encrypted();
   ff2.decrypted();
}
}
