// RSAPSSB.java
// RSA-PSS verification 
// Usage:  java RSAPSSB publicKey signature < message 

import java.io.*;
import java.util.*;
import java.math.*;
import java.security.*;

public class RSAPSSB{

   static final int hLen = 20;
   static final int sLen = 20;
   static final int inBufferSize = 4096;
   Random rand = new Random();
   BigInteger n = null;
   int emBits;
   int emLen;
   BigInteger e = null;
   byte[] inBuffer = new byte[inBufferSize]; 
   int messageLen = 0;
   MessageDigest MD = null;
   byte[] mHash = null;
   byte[] padding1 = new byte[8];
   byte[] salt = null;
   byte[] H = null;
   byte[] H2 = null;
   byte[] DB = null;
   byte[] dbMask = null;
   byte[] EM = null;
   BigInteger m = null;
   BigInteger s = null;


   void readPublicKey(String filename){
    Scanner in = null;
    try {
     in = new Scanner(new File(filename));
    } catch (FileNotFoundException e){
      System.err.println(filename + " not found");
      System.exit(1);
    }
     e = new BigInteger(in.nextLine(), 16);
     n = new BigInteger(in.nextLine(), 16);
     in.close();
     emBits = n.bitLength() - 1;
     emLen = emBits % 8 > 0 ? emBits / 8 + 1 : emBits / 8;
   }

   void readSignature(String filename){
    Scanner in = null;
    try {
     in = new Scanner(new File(filename));
    } catch (FileNotFoundException e){
      System.err.println(filename + " not found");
      System.exit(1);
    }
     s = new BigInteger(in.nextLine(), 16);
     in.close();
   }

   void verifyStep1(){
     byte[] inBuffer = new byte[inBufferSize]; 
     int messageLen = 0;
     try {
       MD = MessageDigest.getInstance("SHA-1");
     } catch (NoSuchAlgorithmException e){
       System.err.println(e.getMessage());
       System.exit(1);
     }
    do {
     try {
        messageLen = System.in.read(inBuffer);
     } catch (IOException e){
        System.err.println(e.getMessage());
        System.exit(1);
     }
     if (messageLen > 0) MD.update(inBuffer, 0, messageLen);
    } while (messageLen > 0);
     mHash = MD.digest();
   }

   void verifyStep2(){
     if (emLen < hLen + sLen + 2){
        System.err.println("Inconsistent: emLen too big");
        System.exit(1);
     }
   }

   void verifyStep3(){
     int lastByte = EM[emLen - 1];
     if (lastByte < 0) lastByte += 256;
     if (lastByte != 0xbc){
        System.err.println("Inconsistent: BC");
        System.exit(1);
     }
   }

    void verifyStep4(){  // opposite of encodingStep8, fill DB and H
        // step4.1: allocate array DB: DB = new byte[....]; length emLen-hLen-1
        // step4.2: allocate array H: byte[] of length hLen
        // step4.3: copy DB from EM using a for loop similar to one in encodingStep8
        // step4.4: copy H from EM , see encodingStep8
        DB = new byte[emLen - hLen - 1];
        H = new byte[hLen];
        for (int i = 0; i < emLen - hLen - 1; i++)
            DB[i] = EM[i];
        for (int i = 0; i < hLen; i++)
            H[i] = EM[emLen - hLen - 1 + i];
        //H[hLen - 1] = (byte)0xbc;
    }

   void verifyStep5(){  // checking the result of encodingStep7
     int diff = 8 * emLen - emBits;
     int singleBit = 0x80;  
     for (int i = 0; i < diff; i++){
        if ((DB[0] & singleBit) != 0){ 
          System.err.println("Inconsistent: verify step 5");
          System.exit(1);
        }
        singleBit >>= 1;
     }
   }

    void verifyStep6(){  //  identical to encodingStep5
        dbMask = MGF1(H, emLen - hLen - 1);
    }

    void verifyStep7(){  // identical to encodingStep6
        for (int i = 0; i < emLen - hLen - 1; i++)
            DB[i] ^= dbMask[i];
    }

    void verifyStep8(){  // identical to encodingStep7
        int diff = 8 * emLen - emBits;
        int singleBit = 0x80;  int mask = 0xff;
        for (int i = 0; i < diff; i++){
            mask ^= singleBit;
            singleBit >>= 1;
        }
        DB[0] &= mask;
    }

    void verifyStep9(){  // checking padding2, or part of encodingStep4
        int border = emLen - hLen - sLen - 2;
        for (int i = 0; i < border; i++){
            if (DB[i] != 0) {
                System.err.println("Inconsistent: verify step 9 != 0");
                System.exit(1);
            }
        }
     if (DB[border] != 1){
          System.err.println("Inconsistent: verify step 9");
          System.exit(1);
        }
   }

    void verifyStep10(){  // opposite of part of encodingStep4, fill salt
        int border = emLen - hLen - sLen - 2;
        // step10.1: allocate array salt: byte[] of length sLen
        salt = new byte[sLen];
        // step10.2: copy salt from DB using a for loop similar to encodingStep4
        //for (int i = 0; i < border; i++) salt[i] = 0;
        //salt[border] = 1;
        for (int i = 0; i < sLen; i++)
            salt[i] = DB[border + i + 1];
    }

   void verifyStep11(){  // part of encodingStep2, fill padding1
     for (int i = 0; i < 8; i++) padding1[i] = 0;
   }
  
   void verifyStep12(){  // identical to encodingStep3, except naming the digest H2
        MD.reset();
        MD.update(padding1);
        MD.update(mHash);
        H2 = MD.digest(salt);
    }

   void verifyStep13(){ // compare H with H2 or simply print them out
     int i = 0; for (; i < hLen; i++) if (H[i] != H2[i]) break;
        if (i < hLen) System.out.println("Inconsistent at " + i + ": verfy step 13");
     else System.out.println("Signature verified");
   }

   byte[] MGF1(byte[] X, int maskLen){
     byte[] mask = new byte[maskLen];
     byte[] counter = new byte[4];
     for (int i = 0; i < 4; i++) counter[i] = 0;
     int k = maskLen % hLen > 0 ? maskLen / hLen : maskLen / hLen - 1;
     int offset = 0;
     for (byte count = 0; count <= k; count++){
       MD.reset(); MD.update(X); counter[3] = count; byte[] h = MD.digest(counter);
       for (int i = 0; i < hLen; i++) if (offset + i < maskLen) mask[offset + i] = h[i];
       offset += hLen;
     }
     return mask;
   }

  void decrypt(){
     m = // compute m from s : m is s modPow e n
        m = s.modPow(e, n);
     EM = m.toByteArray();
     emLen = EM.length;
  }

   void verify(){
      decrypt();
      verifyStep1();
      verifyStep2();
      verifyStep3();  
      verifyStep4(); 
      verifyStep5();
      verifyStep6();
      verifyStep7();  
      verifyStep8();
      verifyStep9();
      verifyStep10();  
      verifyStep11();
      verifyStep12();
      verifyStep13();
   }
 

 public static void main(String[] args){
   if (args.length < 2){
     System.out.println("Usage: java RSAPSSB publicKey signature < message");
     return;
   }
   RSAPSSB rsa = new RSAPSSB();
   rsa.readPublicKey(args[0]);
   rsa.readSignature(args[1]);
   rsa.verify();
 }
}
