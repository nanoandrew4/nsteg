package nstag;

import com.google.crypto.tink.*;
import com.google.crypto.tink.aead.AeadFactory;
import com.google.crypto.tink.aead.AeadKeyTemplates;
import com.lambdaworks.crypto.SCrypt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Scanner;

public abstract class nStag {
	protected static Scanner in = new Scanner(System.in);

	protected static String origPath, fileToHide, outPath;

	static int KEY_BITS_COUNT = 92 * 8;
	final static int SIZE_BITS_COUNT = 4 * 8;

	/**
	 * Converts a decimal number into an array of bits. Can handle both signed and unsigned binary numbers.
	 *
	 * @param b         Number to convert to array of bits
	 * @param numOfBits Number of bits in the number that is being converted
	 * @param signed    True for interpreting as a signed number, false for interpreting as an unsigned number
	 * @return Array of bits representing the decimal number passed as an argument, in signed or unsigned form
	 */
	protected static int[] intToBitArray(int b, int numOfBits, boolean signed) {
		int[] bits = new int[numOfBits];
		boolean neg = b < 0;
		if (signed && neg) {
			b = ~b; // NOT op, flips all the bits so it can be operated upon normally
			bits[0] = 1; // Set first bit to indicate the bits represent a negative number
		}

		/*
		 * Break number down into binary. If number is negative, all 0's should be 1's, and viceversa. That is
		 * achieved with the modulo operation, by adding one and mod 2, the bits will be opposite what they should be,
		 * which is the desired output when a number is negative.
		 */
		for (int i = 0; i < numOfBits; i++) {
			bits[numOfBits - 1 - i] = (b + (signed && neg ? 1 : 0)) % 2;
			b >>>= 1;
		}
		return bits;
	}

	/**
	 * Converts an array of integers (containing exclusively binary numbers) to a decimal representation of itself.
	 * Can handle conversion of both signed and unsigned binary numbers.
	 *
	 * @param bits   Array of bits to be converted to a decimal integer
	 * @param signed True for interpreting as a signed number, false to interpret as an unsigned number
	 * @return Integer representation of the array of bits
	 */
	protected static long bitArrayToLong(int[] bits, boolean signed) {
		boolean neg = signed && bits[0] == 1;
		int b = signed && neg ? -128 : 0;
		for (int i = (signed && neg ? 1 : 0); i < bits.length; i++)
			if (bits[i] != 0)
				b += Math.pow(2, bits.length - 1 - i);
		return b;
	}

	protected void requestInput(String fileType, boolean encode) {
		if (encode)
			System.out.print("Path to " + fileType + " to hide data in: ");
		else
			System.out.print("Path to " + fileType + " to decode data from: ");
		origPath = in.nextLine();
		if (encode) {
			System.out.print("Path to file to be hidden: ");
			fileToHide = in.nextLine();
		}
		if (encode)
			System.out.print("Desired path and filename (with extension) for output " + fileType + ": ");
		else
			System.out.print("Desired path and filename (with extension) for output decoded file: ");
		outPath = in.nextLine();
	}

	protected static byte[][] offerToEncrypt(byte[] bytesToEncrypt) {
		byte[][] keyAndData = new byte[2][];
		keyAndData[1] = bytesToEncrypt;

		System.out.print("Do you want to encrypt the data? Y/N: ");
		String ans = in.nextLine();

		if ("n".equalsIgnoreCase(ans))
			return keyAndData;

		KeysetHandle keysetHandle;

		try {
			keysetHandle = KeysetHandle.generateNew(AeadKeyTemplates.AES128_GCM);
			Aead aead = AeadFactory.getPrimitive(keysetHandle);

			// Writes key to start of bit array, after encryption, otherwise decrypting the data would not be possible
			keyAndData[0] = writeKeyToBitDeque(keysetHandle);

			// Generate password hash to be used to encrypt the data
			System.out.print("Please enter the password to use: ");

			// Encrypt data and generate seed for scrambling the bits later
			keyAndData[1] = aead.encrypt(bytesToEncrypt, in.nextLine().getBytes());
		} catch (GeneralSecurityException e) {
			System.err.println("Encryption failed, aborting...");
			return keyAndData;
		}

		return keyAndData;
	}

	private static byte[] writeKeyToBitDeque(KeysetHandle keysetHandle) {
		byte[] keyBits = new byte[KEY_BITS_COUNT];
		try {
			byte[] key;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			CleartextKeysetHandle.write(keysetHandle, BinaryKeysetWriter.withOutputStream(baos));
			key = baos.toByteArray();

			int pos = 0;
			for (int keyByte : key) {
				int[] kBits = intToBitArray(keyByte, 8, true);
				for (int b : kBits)
					keyBits[pos++] = (byte) b;
			}

		} catch (IOException e) {
			System.err.println("Error obtaining key, no encryption will be performed, aborting...");
		}

		return keyBits;
	}

	protected static byte[] offerToDecrypt(byte[] bytesToDecrypt, byte[] key) {
		System.out.print("Is this file encrypted? Y/N: ");
		String ans = in.nextLine();

		if ("n".equalsIgnoreCase(ans))
			return bytesToDecrypt;

		System.out.print("Please enter the password to use: ");
		KeysetHandle keysetHandle;

		try {
			keysetHandle = CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(key));
			Aead aead = AeadFactory.getPrimitive(keysetHandle);

			// Encrypt data and generate seed for scrambling the bits later
			return aead.decrypt(bytesToDecrypt, in.nextLine().getBytes());
		} catch (GeneralSecurityException | IOException e) {
			System.err.println("Encryption failed, aborting...");
			e.printStackTrace();
			return bytesToDecrypt;
		}
	}
/*
	private static long genSeed(String pass) {
		BigInteger num = new BigInteger("0");
		for (int i = 0; i < pass.length(); i++) {
			BigInteger charPosVal = new BigInteger("" + (int) pass.charAt(i));
			BigInteger charPosPow = new BigInteger("" + 128);
			charPosPow = charPosPow.pow(i);
			charPosVal = charPosVal.multiply(charPosPow);
			num = num.add(charPosVal);
		}

		BigInteger passLength = new BigInteger("" + pass.length());
		BigInteger passLengthPow = new BigInteger("" + 128);
		passLengthPow = passLengthPow.pow(2 * pass.length());
		passLength = passLength.multiply(passLengthPow);
		num = num.add(passLength);

		int[] init = new int[63];
		for (int i = 0; i < 63; i++)
			init[i] = num.toString().charAt(i);

		Random rand = new Random(bitArrayToLong(init, false));

		int randBitPos = rand.nextInt(num.toString().length() - 64);

		int[] seedBits = new int[63];
		for (int i = 0; i < 63; i++)
			seedBits[i] = num.toString().charAt(randBitPos + i);
		return bitArrayToLong(seedBits, false);
	}
*/
}
