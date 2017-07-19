// YUVWaveletB.java
// inverse of YUVWaveletA.java
// Usage: java YUVWaveletB < encoded > original.bmp

import java.io.*;

public class YUVWaveletB{
    static int fibSize = 13;  // fib[12] = 377
    static int[] fib = new int[]{
            1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377 };
    int width, height;
    int[][][] raw;
    int[][][] yuv;
    int dataLength = 0;
    byte[] data = null;
    int index = 0;        // used by inputBit
    int pos = 0x80;        //  used for inputBit


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

    void readData(){
        data = new byte[height * width * 3];
        raw = new int[height][width][3];
        yuv = new int[height][width][3];
        try {
            dataLength = System.in.read(data);
        } catch (IOException e){
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    boolean inputBit(){
        boolean one = ((data[index] & pos) != 0);
        pos >>= 1;
        if (pos == 0){
            pos = 0x80;
            index++;
        }
        return one;
    }

    int deFib(){
        int value = 0;
        // Your code to read the next C1 codeword and decode it into
        // the variable "value"
        for(int i=0;i<fibSize;i++){
            if(inputBit()){
                value+=fib[i];
            }
        }
        return value;
    }

    void decode(){
        for (int i = 0; i < height; i++){
            int j = 0; int value = 0; int runLength = 0;
            while (j < width){
                value = deFib() - 1;
                if (value == 0){
                    runLength = deFib();
                    for (int l = 0; l < runLength; l++)
                        yuv[i][j + l][0] = 0;
                    j += runLength;
                }else yuv[i][j++][0] = unMapValue(value);
            }
        }
        for (int k = 1; k < 3; k++){
            for (int i = 0; i < height / 2; i++){
                int j = 0; int value = 0; int runLength = 0;
                while (j < width / 2){
                    value = deFib() - 1;
                    if (value == 0){
                        runLength = deFib();
                        for (int l = 0; l < runLength; l++) yuv[i][j + l][k] = 0;
                        j += runLength;
                    }else yuv[i][j++][k] = unMapValue(value);
                }
            }
        }
    }

    int unMapValue(int v){
        if ((v & 0x01) == 0) return v / 2;
        else return -((v + 1) / 2);
    }

    void inverseRowTransform(int height, int width, int k){
        int halfWidth = width / 2;
        for (int i = 0; i < height; i++){
            for (int j = 0; j < halfWidth; j++){
                raw[i][2 * j][k] = yuv[i][j][k];
                raw[i][2 * j + 1][k] = yuv[i][halfWidth + j][k];
            }
            yuv[i][0][k] = raw[i][0][k] - (raw[i][1][k] + 1) / 2;
            for (int j = 1; j < halfWidth; j++) {
                yuv[i][2 * j][k] = raw[i][2 * j][k] - (raw[i][2 * j - 1][k] + raw[i][2 * j + 1][k] + 2) / 4;
            }
            yuv[i][width - 1][k] = raw[i][width - 1][k] + yuv[i][width - 2][k];
            for (int j = 0; j < halfWidth - 1; j++) {
                yuv[i][2 * j + 1][k] = raw[i][2 * j + 1][k] + (yuv[i][2 * j][k] + yuv[i][2 * j + 2][k]) / 2;
            }
        }
    }

    void inverseColumnTransform(int height, int width, int k){
        int halfHeight = height / 2;
        for (int j = 0; j < width; j++){
            for (int i = 0; i < halfHeight; i++){
                raw[2 * i][j][k] = yuv[i][j][k];
                raw[2 * i + 1][j][k] = yuv[halfHeight + i][j][k];
            }
            yuv[0][j][k] = raw[0][j][k] - (raw[1][j][k] + 1) / 2;
            for (int i = 1; i < halfHeight; i++) {
                yuv[2 * i][j][k] = raw[2 * i][j][k] - (raw[2 * i - 1][j][k] + raw[2 * i + 1][j][k] + 2) / 4;
            }
            yuv[height - 1][j][k] = raw[height - 1][j][k] + yuv[height - 2][j][k];
            for (int i = 0; i < halfHeight - 1; i++) {
                yuv[2 * i + 1][j][k] = raw[2 * i + 1][j][k] + (yuv[2 * i][j][k] + yuv[2 * i + 2][j][k]) / 2;
            }
        }
    }


    void inverseUvSubsampling(){
        for (int i = height / 2 - 1; i >= 0; i--)
            for (int j = width / 2 - 1; j >= 0; j--)
                for (int k = 1; k < 3; k++)
                    yuv[2 * i][2 * j][k]  = yuv[2 * i][2 * j + 1][k]
                            = yuv[2 * i + 1][2 * j][k] = yuv[2 * i + 1][2 * j + 1][k]
                            = yuv[i][j][k];
    }

    void unshrink(){
        int[][] borders = new int[3][4];
        borders[0][0] = height; borders[0][1] = width;
        borders[1][0] = borders[2][0] = height / 2;
        borders[1][1] = borders[2][1] = width / 2;
        borders[0][2] = height / 4; borders[0][3] = width / 4;
        borders[1][2] = borders[2][2] = height / 8;
        borders[1][3] = borders[2][3] = width / 8;
        for (int k = 0; k < 3; k++)
            for (int i = 0; i < borders[k][0]; i++)
                for (int j = 0; j < borders[k][1]; j++)
                    if (i > borders[k][2] || j > borders[k][3])
                        if (yuv[i][j][k] != 0)
                            yuv[i][j][k] *= 8;
    }



    void inverseWaveletTransform(){
        inverseColumnTransform(height / 4, width / 4, 0);
        inverseRowTransform(height / 4, width / 4, 0);
        for (int k = 1; k < 3; k++){
            inverseColumnTransform(height / 8, width / 8, k);
            inverseRowTransform(height / 8, width / 8, k);
        }
        inverseColumnTransform(height / 2, width / 2, 0);
        inverseRowTransform(height / 2, width / 2, 0);
        for (int k = 1; k < 3; k++){
            inverseColumnTransform(height / 4, width / 4, k);
            inverseRowTransform(height / 4, width / 4, k);
        }
        inverseColumnTransform(height, width, 0);
        inverseRowTransform(height, width, 0);
        for (int k = 1; k < 3; k++){
            inverseColumnTransform(height / 2, width / 2, k);
            inverseRowTransform(height / 2, width / 2, k);
        }
    }


    void inverseYuvTransform(){  // YUV to RGB
        for (int i = 0; i < height; i++)
            for (int j = 0; j < width; j++){
                int y = (yuv[i][j][0] - 16) * 298;
                int u = yuv[i][j][1] - 128;
                int v = yuv[i][j][2] - 128;
                raw[i][j][2] = (y + 409 * v)/256;
                raw[i][j][1] = (y - 100 * u - 208 * v)/256;
                raw[i][j][0] = (y + 516 * u)/256;
                for (int k = 0; k < 3; k++){
                    if (raw[i][j][k] < 0) raw[i][j][k] = 0;
                    if (raw[i][j][k] > 255) raw[i][j][k] = 255;
                }
            }
    }


    void dumpRaw(){
        for (int i = 0; i < height; i++)
            for (int j = 0; j < width; j++)
                for (int k = 0; k < 3; k++) System.out.write(raw[i][j][k]);
        System.out.flush();
    }


    public static void main(String[] args){
        YUVWaveletB yuvw = new YUVWaveletB();
        yuvw.readHeader();
        yuvw.readData();
        yuvw.decode();
        yuvw.unshrink();
        yuvw.inverseWaveletTransform();
        yuvw.inverseUvSubsampling();
        yuvw.inverseYuvTransform();
        yuvw.dumpRaw();
    }
}
