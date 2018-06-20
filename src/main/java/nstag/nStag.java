package nstag;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadFactory;
import com.google.crypto.tink.aead.AeadKeyTemplates;

import java.security.GeneralSecurityException;
import java.util.Scanner;

public abstract class nStag {
	protected Scanner in = new Scanner(System.in);

	protected String origPath, fileToHide, outPath;

	/**
	 * Converts a decimal number into an array of bits. Can handle both signed and unsigned binary numbers.
	 *
	 * @param b         Number to convert to array of bits
	 * @param numOfBits Number of bits in the number that is being converted
	 * @param signed    True for interpreting as a signed number, false for interpreting as an unsigned number
	 * @return Array of bits representing the decimal number passed as an argument, in signed or unsigned form
	 */
	protected static int[] getBits(int b, int numOfBits, boolean signed) {
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
	protected static int toByte(int[] bits, boolean signed) {
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

	protected byte[] offerToEncrypt(byte[] bytes) {
		System.out.print("Do you want to encrypt the data? Y/N: ");
		String ans = in.nextLine();

		if ("y".equalsIgnoreCase(ans)) {
			KeysetHandle keysetHandle;
			Aead aead;
			try {
				keysetHandle = KeysetHandle.generateNew(AeadKeyTemplates.AES128_GCM);
				aead = AeadFactory.getPrimitive(keysetHandle);

				System.out.print("Please enter the password to use: ");
				return aead.encrypt(bytes, in.nextLine().getBytes());
			} catch (GeneralSecurityException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	protected byte[] offerToDecrypt(byte[] bytes) {
		System.out.print("Is this file encrypted? Y/N: ");
		String ans = in.nextLine();

		if ("y".equalsIgnoreCase(ans)) {
			KeysetHandle keysetHandle;
			Aead aead;
			try {
				keysetHandle = KeysetHandle.generateNew(AeadKeyTemplates.AES128_GCM);
				aead = AeadFactory.getPrimitive(keysetHandle);

				System.out.print("Please enter the password: ");
				return aead.decrypt(bytes, in.nextLine().getBytes());
			} catch (GeneralSecurityException e) {
				e.printStackTrace();
			}
		}

		return null;
	}
}