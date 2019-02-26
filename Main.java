package app;

import java.io.IOException;

import io.InsufficientBitsLeftException;

public class Main {
	public static void main(String[] args) throws IOException, InsufficientBitsLeftException {
		// files for encoder
		String input_decoded_file = "out.dat";
		String output_encoded_file = "compressed.dat";
		// files for decoder
		String output_decoded_file = "reuncompressed.dat";
		
		VideoEncoder encoder = new VideoEncoder(input_decoded_file, output_encoded_file);
		encoder.encode();
		
		VideoDecoder decoder = new VideoDecoder(output_encoded_file, output_decoded_file);
		decoder.decode();
		
	}
}
