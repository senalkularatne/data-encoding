// YUVWaveletA.java
// (5,3) wavelet transform of the YUV transform of a RGB image in BMP format
// run length of zero and c1 encoding of the H coefficients
// Usage: java YUVWavelet fib512.txt < BMP image > encoded

import java.io.*;
import java.util.*;

public class YUVWaveletA{
   static final int range = 512;
   String[] codeword = new String[range];
   int width, height;
   int[][][] raw; 
   int[][][] yuv; 
   int buf = 0;        // buffer for outputBits
   int position = 0;        // buffer used for outputBits

 void readCode(String filename){
  Scanner in = null;
  try {
   in = new Scanner(new File(filename));
  } catch (FileNotFoundException e){
    System.err.println(filename + " not found");
    System.exit(1);
  }
  for (int i = 0; i < range; i++)
   codeword[i] = in.nextLine();
  in.close();
 }

 void outputbits(String bitstring){
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


 void readHeader(){
   byte[] header = new byte[54];
   try {
      System.in.read(header);
      System.out.write(header);
   } catch (IOException e){
     System.err.println(e.getMessage());
     System.exit(1);
   }
   if (header[0] != 'B' || header[1] != 'M'
      || header[14] != 40 || header[28] != 24)
     System.exit(1);
   int w1 = header[18]; int w2 = header[19];
   if (w1 < 0) w1 += 256; if (w2 < 0) w2 += 256;
   width = w2 * 256 + w1;
   int h1 = header[22]; int h2 = header[23];
   if (h1 < 0) h1 += 256; if (h2 < 0) h2 += 256;
   height = h2 * 256 + h1;
 }

 void readImage(){
   byte[] image = new byte[height * width * 3];
   raw = new int[height][width][3];
   yuv = new int[height][width][3];
   try {
      System.in.read(image);
   } catch (IOException e){
     System.err.println(e.getMessage());
     System.exit(1);
   }
   int index = 0;
   for (int i = 0; i < height; i++)
    for (int j = 0; j < width; j++)
      for (int k = 0; k < 3; k++){
        raw[i][j][k] = image[index++];
        if (raw[i][j][k] < 0) raw[i][j][k] += 256;
   }
 }

 void yuvTransform(){  // RGB to YUV
   for (int i = 0; i < height; i++)
    for (int j = 0; j < width; j++){
     yuv[i][j][0] =(int)((65.738 * raw[i][j][2] + 129.057 * raw[i][j][1]
        + 25.064 * raw[i][j][0])/256 + 16);
     yuv[i][j][1] = (int)((-37.945 * raw[i][j][2] - 74.494 * raw[i][j][1]
        + 112.439 * raw[i][j][0])/256 + 128);
     yuv[i][j][2] = (int)((112.439 * raw[i][j][2] - 94.154 * raw[i][j][1]
        - 18.285 * raw[i][j][0])/256 + 128);
   }
 }


 void uvSubsampling(){
   for (int i = 0; i < height / 2; i++)
    for (int j = 0; j < width / 2; j++)
     for (int k = 1; k < 3; k++)
      yuv[i][j][k] = (yuv[2 * i][2 * j][k]
    + yuv[2 * i][2 * j + 1][k]
       + yuv[2 * i + 1][2 * j][k] + yuv[2 * i + 1][2 * j + 1][k]) / 4;
 }

 void rowTransform(int height, int width, int k){
   int halfWidth = width / 2;
   for (int i = 0; i < height; i++){
     for (int j = 0; j < halfWidth - 1; j++)
       raw[i][2 * j + 1][k] = yuv[i][2 * j + 1][k] - 
        (yuv[i][2 * j][k] + yuv[i][2 * j + 2][k]) / 2;
     raw[i][width - 1][k] = yuv[i][width - 1][k] - yuv[i][width - 2][k];
     for (int j = 1; j < halfWidth; j++)
       raw[i][2 * j][k] = yuv[i][2 * j][k] + 
        (raw[i][2 * j - 1][k] + raw[i][2 * j + 1][k] + 2) / 4;
     raw[i][0][k] = yuv[i][0][k] + (raw[i][1][k] + 1) / 2;
     for (int j = 0; j < halfWidth; j++){
       yuv[i][j][k] = raw[i][2 * j][k];
       yuv[i][halfWidth + j][k] = raw[i][2 * j + 1][k];
     }
   }
 }

 void columnTransform(int height, int width, int k){
   int halfHeight = height / 2;
   for (int j = 0; j < width; j++){
     for (int i = 0; i < halfHeight - 1; i++)
       raw[2 * i + 1][j][k] = yuv[2 * i + 1][j][k] - 
        (yuv[2 * i][j][k] + yuv[2 * i + 2][j][k]) / 2;
     raw[height - 1][j][k] = yuv[height - 1][j][k] - yuv[height - 2][j][k];
     for (int i = 1; i < halfHeight; i++)
       raw[2 * i][j][k] = yuv[2 * i][j][k] + 
        (raw[2 * i - 1][j][k] + raw[2 * i + 1][j][k] + 2) / 4;
     raw[0][j][k] = yuv[0][j][k] + (raw[1][j][k] + 1) / 2;
     for (int i = 0; i < halfHeight; i++){
       yuv[i][j][k] = raw[2 * i][j][k];
       yuv[halfHeight + i][j][k] = raw[2 * i + 1][j][k];
     }
   }
 }


 void waveletTransform(){
    rowTransform(height, width, 0);
    columnTransform(height, width, 0);
    for (int k = 1; k < 3; k++){
      rowTransform(height / 2, width / 2, k);
      columnTransform(height / 2, width / 2, k);
    }
    rowTransform(height / 2, width / 2, 0);
    columnTransform(height / 2, width / 2, 0);
    for (int k = 1; k < 3; k++){
      rowTransform(height / 4, width / 4, k);
      columnTransform(height / 4, width / 4, k);
    }
    rowTransform(height / 4, width / 4, 0);
    columnTransform(height / 4, width / 4, 0);
    for (int k = 1; k < 3; k++){
      rowTransform(height / 8, width / 8, k);
      columnTransform(height / 8, width / 8, k);
    }
 }

 void shrink(){
   int threshold = 12;
   int[][] borders = new int[3][4];
   borders[0][0] = height; borders[0][1] = width;
   borders[1][0] = borders[2][0] = height / 2; 
   borders[1][1] = borders[2][1] = width / 2; 
   borders[0][2] = height / 4; borders[0][3] = width / 4;
   borders[1][2] = borders[2][2] = height / 8; 
   borders[1][3] = borders[2][3] = width / 8; 
   for (int k = 0; k < 3; k++)
    for (int i = 0; i < borders[k][0]; i++)
     for (int j = 0; j < borders[k][1]; j++){
        if (i > borders[k][2] || j > borders[k][3]){
         if (yuv[i][j][k] > 0 && yuv[i][j][k] <= threshold) yuv[i][j][k] = 0;
         if (yuv[i][j][k] < 0 && yuv[i][j][k] >= -threshold) yuv[i][j][k] = 0;
         if (yuv[i][j][k] != 0) yuv[i][j][k] /= 8;
        }
   }
 }

 int mapValue(int v){
   int c = v >= 0 ? v * 2 + 1 : -v * 2;
   return c - 1;
 }

 void encode(){
   for (int i = 0; i < height; i++){
    boolean isZero = false;
    int runLength = 0;
    for (int j = 0; j < width; j++){
     if (yuv[i][j][0] == 0) if (isZero) runLength++;
                 else{ isZero = true; runLength = 0; }
     else{
      if (isZero){ 
       outputbits(codeword[0]);
       outputbits(codeword[runLength]);
       isZero = false;
      }
      outputbits(codeword[mapValue(yuv[i][j][0])]);
     }
    }
    if (isZero){
       outputbits(codeword[0]);
       outputbits(codeword[runLength]);
    }
   }

 for (int k = 1; k < 3; k++){
   for (int i = 0; i < height / 2; i++){
    boolean isZero = false;
    int runLength = 0;
    for (int j = 0; j < width / 2; j++){
     if (yuv[i][j][k] == 0) if (isZero) runLength++;
                 else{ isZero = true; runLength = 0; }
     else{
      if (isZero){ 
       outputbits(codeword[0]);
       outputbits(codeword[runLength]);
       isZero = false;
      }
      outputbits(codeword[mapValue(yuv[i][j][k])]);
     }
    }
    if (isZero){
       outputbits(codeword[0]);
       outputbits(codeword[runLength]);
    }
   }
  }

   if (position > 0){             // leftover bits
      
     buf <<= (8 - position);
       
     System.out.write(buf);
     
   }
 
   System.out.flush();
 }



 public static void main(String[] args){
  if (args.length < 1){ System.err.println("Usage: java YUVWaveletA fib512 < in.bmp > out");
    return; }
  YUVWaveletA yuvw = new YUVWaveletA();
  yuvw.readCode(args[0]);
  yuvw.readHeader();
  yuvw.readImage();
  yuvw.yuvTransform();
  yuvw.uvSubsampling();
  yuvw.waveletTransform();
  yuvw.shrink();
  yuvw.encode();
 }
}

