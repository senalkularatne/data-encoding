// SVG2PNG.java
// converts SVG to PNG
// java SVG2PNG < qrcode.svg > qrcode.png

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class SVG2PNG{

   static final int bufferSize = 4096;
   static final String svgTag = "<svg";
   static final String widthAtt = "width=";
   static final String heightAtt = "height=";
   static final int PNGHeaderSize = 33;
   static final byte[] signature = new byte[]{
    137 - 256, 80, 78, 71, 13, 10, 26, 10,
    0, 0, 0, 13, 73, 72, 68, 82 };
   static byte[] iend = new byte[]{
    0, 0, 0, 0, 'I', 'E', 'N', 'D', 0, 0, 0, 0 };
   byte[] PNGHeader = new byte[PNGHeaderSize];
   byte[] data = null;
   byte[] idat = null;
   int compressedDataLength = 0;

   int width = 0;
   int height = 0;
   byte[] buffer = new byte[bufferSize];
   int dataLength = 0;
   int dataPosition = 0;
   boolean[][] qr = null;
   CRC32 crc32 = new CRC32();
 
   void readSize(){
     try {
       dataLength = System.in.read(buffer);
     } catch (IOException e){
       System.err.println("IOException");
       System.exit(1);
     }
     int tagClosing = 4; while (buffer[tagClosing++] != '>');
     String headTag = new String(buffer, 0, tagClosing);
     if (headTag.indexOf(svgTag) != 0){
       System.err.println(" not a svg file");
       System.exit(1);
     }
     int pos = headTag.indexOf(widthAtt);
     int pos2 = headTag.indexOf('"', pos + 7);
     width = Integer.parseInt(headTag.substring(pos + 7, pos2));
     pos = headTag.indexOf(heightAtt);
     pos2 = headTag.indexOf('"', pos + 8);
     height = Integer.parseInt(headTag.substring(pos + 8, pos2));
     dataPosition = tagClosing;
     qr = new boolean[height][width];
     for (int i = 0; i < height; i++)
      for (int j = 0; j < width; j++) qr[i][j] = true;
   }  
 
  String nextPath(){
    int pos = dataPosition; for (; pos < dataLength; pos++)
     if (buffer[pos] == 'M') break;
    if (pos == dataLength) return null;
    int pos2 = pos; for (; pos2 < dataLength; pos2++)
     if (buffer[pos2] == 'z') break;
    dataPosition = pos2 + 1;
    return new String(buffer, pos, pos2 - pos + 1);
  }

  void allPaths(){
    String path = null;
    while ((path = nextPath()) != null)
     process(path);
  }

  void process(String move){
     int j = 0;
     int command = move.charAt(j); 
     int x0 = -1, y0 = -1;
     int x1 = -1, y1 = -1, x2 = -1, y2 = -1;
     while (command != 'z'){
       int i = j + 1;
       for (j++; ; j++) if (move.charAt(j) > '9') break;
       switch (command){
         case 'M': case 'L': int comma = move.indexOf(',', i);
            x2 = Integer.parseInt(move.substring(i, comma));
            y2 = Integer.parseInt(move.substring(comma + 1, j));
            if (command == 'M'){ x0 = x2; y0 = y2; }
            break;
         case 'V': 
            y2 = Integer.parseInt(move.substring(i, j));
            x2 = x1;
            break;
         case 'H':
            x2 = Integer.parseInt(move.substring(i, j));
            y2 = y1;
            break;
         case 'v': 
            y2 = y1 + Integer.parseInt(move.substring(i, j));
            x2 = x1;
            break;
         case 'h':
            x2 = x1 + Integer.parseInt(move.substring(i, j));
            y2 = y1;
            break;
         default: ;
       }
       if (command != 'M' && x1 != x2) flipPixels(y2, x1, x2);
       x1 = x2; y1 = y2;
       command = move.charAt(j);
       i = j + 1;
     }
     if (x0 != x2) flipPixels(y2, x0, x2);
   }

 void flipPixels(int y, int x1, int x2){
     if (x2 < x1){ int t = x1; x1 = x2; x2 = t; }
     for (int i = x1; i < x2; i++)
      for (int j = 0; j < y; j++) qr[i][j] = !qr[i][j];
 }


 // fill 4 bytes in buffer at offset with a number
 void fillNumber(byte[] buffer, int offset, long number){
   int k = 0; for (; k < 4; k++){
     buffer[offset + 3 - k] = (byte)(number & 0xff);
     number >>= 8;
 }
 }

 void fillPNGHeader(){
   for (int i = 0; i < 16; i++) PNGHeader[i] = signature[i];
   fillNumber(PNGHeader, 16, width);
   fillNumber(PNGHeader, 20, height);
   PNGHeader[24] = 1;
   for (int i = 25; i < 29; i++) PNGHeader[i] = 0;
   crc32.reset();
   crc32.update(PNGHeader, 12, 17);
   fillNumber(PNGHeader, 29, crc32.getValue());
 }

 void fillIDAT(){
   int lineWidth = width / 8 + 1;
   data = new byte[height * lineWidth];
   idat = new byte[height * lineWidth];
   int lineOffset = 0;
   for (int i = 0; i < height; i++){
     data[lineOffset] = 0;
     for (int j = 0; j < width / 8; j++){
       data[lineOffset + j] = 0;
       for (int k = 0; k < 8; k++){  // eight pixel per byte
         data[lineOffset + j] <<= 1;
         if (qr[i][j * 8 + k]) data[lineOffset + j] |= 1;
       }
     }
     lineOffset += lineWidth;
   }
   idat[4] = 'I'; idat[5] = 'D'; idat[6] = 'A'; idat[7] = 'T';
   Deflater compresser = new Deflater();
   compresser.setInput(data);
   compresser.finish();
   // your code for
   compressedDataLength = compresser.deflate(idat, 8, height*lineWidth - 8);
   compresser.end();
   fillNumber(idat, 0, compressedDataLength);
   crc32.reset();
   crc32.update(idat);
   fillNumber(idat, 8 + compressedDataLength, crc32.getValue());
   // 1. deflate data into idat at position 8.
   // 2. place compressedDataLength at position 0 of idat
   // 3. compute CRC for idat without the length
   // 4. append CRC after compressed data
   // idat = |length|"IDAT"|compressed data|CRC|
 }

 void fillIEND(){
   crc32.reset();
   crc32.update(iend, 4, 4);
   fillNumber(iend, 8, crc32.getValue());
 }

 void writePNG(){
  try {
   System.out.write(PNGHeader);
   System.out.write(idat, 0, compressedDataLength + 12);
   System.out.write(iend);
  } catch (IOException e){
    System.err.println(e.getMessage());
    System.exit(1);
  }
 }


 public static void main(String[] args){
      SVG2PNG sp = new SVG2PNG(); 
      sp.readSize();
      sp.allPaths();
      sp.fillPNGHeader();
      sp.fillIDAT();
      sp.fillIEND();
      sp.writePNG();
 }
}
