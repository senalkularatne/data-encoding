// JBIGB.java
// Adaptive Arithmetic Decoding with Bi-level Images
// inverse of JBIGA.java
// Usage:java JBIGB < compressed > decoded.pbm

import java.io.*;

public class JBIGB{

   static final int maxRange = 65536;  // 2 ** 16
   static final int half = 32768;
   static final int quarter = 16384;
   static final int threequarters = 49152;
   static final int numberOfContexts = 1024;
   int width = 0, height = 0, bytesPerRow = 0;  // dimension of the image
   static final int BFSZ = 8192;
   byte[] buffer = new byte[BFSZ];
   int readLen = 0;
   int index =0;
   boolean[][] bitmap = null;
   
   int[][] count = new int[numberOfContexts][2];
   int low = 0; int high = maxRange;
   int inBuf = 0; int inPosition = 0;
   int outBuf = 0; int outPosition = 0;
   int codeword = 0;

 void readPBMHeader(){
  try{
    readLen = System.in.read(buffer);
  } catch (IOException e){
    System.err.println(e.getMessage());
    System.exit(1);
  }
  if (buffer[0] != 'P' || buffer[1] != '4'){
    System.err.println(" not P4");
    System.exit(1);
  }
  int pos = 0; 
  while (buffer[pos++] != '\n');
  int pos2 = pos + 1;
  while (buffer[pos2++] != '\n');
  String secondLine = new String(buffer, pos, pos2 - pos - 1);
  String[] terms = secondLine.split(" ");
  width = Integer.parseInt(terms[0]);
  height = Integer.parseInt(terms[1]);
  index = pos2;
  System.out.write(buffer, 0, pos2);
  bitmap = new boolean[3][width + 4];
  for (int i = 0; i < 3; i++) for (int j = 0; j < width + 4; j++) bitmap[i][j] = false;
  bytesPerRow = width / 8;
  for (int i = 0; i < numberOfContexts; i++) count[i][0] = count[i][1] = 1;
 }   


   void incrementCount(int context, boolean one){
  // increment count 
      int v = one ? 1 : 0;
      if (++count[context][v] >= quarter){
         count[context][0] >>= 1; count[context][1] >>= 1;
         if (count[context][1 - v] == 0) count[context][1 - v] = 1;
      }
   } 

   void outputBit(boolean bit){
  // save bits in buf when full output byte
      outBuf <<= 1;
      if (bit) outBuf |= 1;
      outPosition++;
      if (outPosition == 8){
         outPosition = 0;
         System.out.write(outBuf);
         outBuf = 0;
      }

   }

 int getNextByte(){
    if (index >= readLen){
      try{
        readLen = System.in.read(buffer);
      } catch (IOException e){
         System.err.println(e.getMessage());
         System.exit(1);
      }
      if (readLen < 0) return -1;
      index = 0; 
    }
    int b = buffer[index++];
    return b < 0 ? b + 256 : b;
 }


   int inputBit(){ 
      if (inPosition == 0){
            inBuf = getNextByte();
            if (inBuf < 0) return -1;
             
            inPosition = 0x80;
      }
      int t = ((inBuf & inPosition) == 0) ? 0 : 1;
      inPosition >>= 1; 
      return t;
   }     

   boolean update(int context){  // Your code needed to complete this function.
     int t = low + count[context][0] * (high - low) / (count[context][0] + count[context][1]);
     boolean ret = codeword >= t;
     if ( ret ) low = t; else high = t;
     for (;;){ // double until larger than quarter
       if (high < half){
         high *= 2;
         low *= 2;
         codeword *= 2;
         if (inputBit() > 0) codeword |= 1; 
       }else if (low >= half){ 
         high = (high * 2) - maxRange;
         low = (low * 2) - maxRange;
                codeword = (codeword * 2) - maxRange; // code for updating codeword
                if (inputBit() > 0)
                    codeword |= 1;
       }else if (low > quarter && high <= threequarters){
         low = (low * 4 - maxRange) / 2;
         high = (high * 4 - maxRange) / 2;
                codeword = (codeword * 4 - maxRange) / 2; // code for updating codeword
                if (inputBit() > 0)
                    codeword |= 1;
       }else break;
    }
    return ret;
  }

 int getContext(int column){
   // column >= 2
   int context = 0;
   for (int k = -1; k < 2; k++){ 
     context <<= 1; 
     if (bitmap[0][column + k]) context |= 1; 
   }  
   for (int k = -2; k < 3; k++){ 
     context <<= 1; 
     if (bitmap[1][column + k]) context |= 1; 
   }  
   for (int k = -2; k < 0; k++){ 
     context <<= 1; 
     if (bitmap[2][column + k]) context |= 1; 
   }  
   return context;
  }
    
 void uncompress(){
     for (int i = 0; i < 16; i++){
       codeword <<= 1; 
       codeword |= inputBit();
     }
     for (int i = 0; i < height; i++){
       for (int j = 0; j < width; j++){
        int context = getContext(j + 2);
        boolean one = update(context);
        incrementCount(context, one);  
        bitmap[2][j + 2] = one;
        outputBit(one);
      }
      for (int j = 2; j < width + 2; j++){
        bitmap[0][j] = bitmap[1][j];
        bitmap[1][j] = bitmap[2][j];
      }
        
    }
    System.out.flush();
 }

 public static void main(String[] args){
     JBIGB jbig = new JBIGB();
     jbig.readPBMHeader();
     jbig.uncompress(); 
 }
}

         
