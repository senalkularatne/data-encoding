// Numbers.java
// Various functions for Chapter 2 number theory concepts

import java.io.*;
import java.util.*;

public class Numbers{

  boolean divides(int a, int b){ return b % a == 0; }

  void divisors(int n){
	for (int i = 1; i <= n; i++) 
	if (divides(i, n)) {
	System.out.print(i + ", ");
	}
  }
  
  void divisors2(int n){
	for (int i = 1; i <= n; i++) 
	if (divides(i, n)){
	System.out.print(i + ", ");
	}
  }
  
  boolean isPrime(int n){
	int i = 2; for (; i < n / 2; i++) if (divides(i, n)) return false;
	return true;
  }

  void listPrimes(int n){
  int counter = 0;
	for (int i = 2; i < n; i++) 
	if (isPrime(i)){
		counter++;
	} 
	System.out.println(counter);
  }
		
  int gcd(int a, int b){
	if (a < b){ int t = a; a = b; b = t; }
	int r = a % b;
	while (r > 0){ a = b; b = r; r = a % b; }
	return b;
  }

  boolean relativelyPrime(int a, int b){ return gcd(a, b) == 1; }

  int order(int a, int modulus){
	int m = 1;
	int power = a;
	while (m < modulus && power > 1){ power = (power * a) % modulus; m++; }
	if (m < modulus) return m;
	return -1;
  } 

  int totient(int n){
	int relativelyPrimeNumbers = 1;
	for (int i = 2; i < n; i++) if (relativelyPrime(n, i)) relativelyPrimeNumbers++;
	return relativelyPrimeNumbers;
  }

  void primitiveRoots(int modulus){
	int phi = totient(modulus);
	for (int a = 2; a < modulus; a++) if (order(a, modulus) == phi)
		System.out.print(a + ", ");
  }

  void additionTable(int modulus){
	System.out.print("+"); 
	for (int i = 0; i < modulus; i++) System.out.print(" " + i);
	System.out.println();
	for (int i = 0; i < modulus; i++){
		System.out.print(i);
		for (int j = 0; j < modulus; j++) System.out.print(" " + ((i + j) % modulus));
		System.out.println();
	}
	System.out.println();
  }

  void multiplicationTable(int modulus){
	System.out.print("x"); 
	for (int i = 0; i < modulus; i++){
		System.out.print(" " + i);
	}
	System.out.println();
	for (int i = 0; i < modulus; i++){
		System.out.print(i);
		for (int j = 0; j < modulus; j++){
			System.out.print(" " + ((i * j) % modulus));
		}
		System.out.println();
	}
	System.out.println();
  }

  void powerTable(int modulus){
	System.out.print("^"); 
	for (int i = 2; i < modulus; i++) System.out.print(" " + i);
	System.out.println();
	for (int i = 1; i < modulus; i++){
		int power = i;
		System.out.print(i);
		for (int j = 2; j < modulus; j++){
			power = (power * i) % modulus;
			System.out.print(" " + power);
		}
		System.out.println();
	}
	System.out.println();
  }

  void discreteLog(int modulus, int base){
	int[] logs = new int[modulus];
	int power = base;
	logs[base] = 1;
	for (int i = 2; i < modulus; i++){
		power = (power * base) % modulus;
		logs[power] = i;
	}
	for (int i = 1; i < modulus; i++)
		System.out.println(i + " " + logs[i]);
  }
	
 public static void main(String[] args){
  Numbers numbers = new Numbers();
    System.out.println("");
  //Questions 1
  int x = numbers.gcd(1160718174, 316258250);
  System.out.println("The GCD of 1160718174 and 316258250: " + x);  System.out.println("");
  //Questions 2
  System.out.print("The Divisors of 1989:  ");
  numbers.divisors(1989);  System.out.println("");  System.out.println("");
  System.out.print("The Divisors of 2017:  ");
  numbers.divisors2(2017);  System.out.println("");  System.out.println("");
  //Questions 3
  System.out.print("Number of Prime Numbers: ");
  numbers.listPrimes(2000);  System.out.println("");
  System.out.print("Number of Prime Numbers: ");
  numbers.listPrimes(3000);  System.out.println("");
  //Question 4
  int y = numbers.totient(255);
  int y1 = numbers.totient(256);
  int y2 = numbers.totient(257);
  System.out.println("Euler's totient function values of 255: " + y); System.out.println("");
  System.out.println("Euler's totient function values of 256: " + y1); System.out.println("");
  System.out.println("Euler's totient function values of 257: " + y2); System.out.println("");
  //Question 5
  System.out.println("Addition Table:");
  numbers.additionTable(8);
  System.out.println("Multiplication Table:");
  numbers.multiplicationTable(8);
  System.out.println("Powers Table:");
  numbers.powerTable(8);
  //Question 6
  System.out.print("All primitive roots of numbers between 2 and 19: ");
  numbers.primitiveRoots(19); System.out.println(""); System.out.println("");
  //Question 7
  System.out.println("Select and indicate a primitive root of 19 as the base and list discrete log of numbers between 1 and 18 mod 19.");
  numbers.discreteLog(19, 18);
 }
}
