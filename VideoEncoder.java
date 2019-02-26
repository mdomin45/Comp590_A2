package app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import ac.ArithmeticEncoder;
import io.OutputStreamBitSink;
import io.OutputStreamBitSink;

public class VideoEncoder {
	private String _input_file;
	private String _output_file;
	private int _frames = 300; // frames hard-coded
	private int _range = 40; // bit width range hard-coded
	private int _pixel_count;
	private int _pixels_in_frame;
	private Integer[] _pixel_intensities = new Integer[256];
	private Integer[] _last_frame;
	private FileInputStream _in_stream;
	private FileOutputStream _out_stream;
	private OutputStreamBitSink _bit_sink;
	private FreqCountIntegerSymbolModel[] _models;
	private ArithmeticEncoder<Integer> _arithmetic_encoder;
	
	public VideoEncoder(String in_file, String out_file) throws IOException {
		// TODO Auto-generated constructor stub
		_input_file = in_file;
		_output_file = out_file;
		_pixel_count = (int) new File(_input_file).length();
		_pixels_in_frame = _pixel_count / _frames; 
		_last_frame = new Integer[_pixels_in_frame];
		
		int i = 0;
		while (i < 256) {
			_pixel_intensities[i] = i;
			i++;
		}
		
		// create the models for each possible intensity
		_models = new FreqCountIntegerSymbolModel[256];
		
		i = 0;
		while (i < 256) {
			_models[i] = new FreqCountIntegerSymbolModel(_pixel_intensities);
			i++;
		}
		
		// create encoder, input, output, sink
		_arithmetic_encoder = new ArithmeticEncoder<Integer>(_range);
		_in_stream = new FileInputStream(_input_file);
		_out_stream = new FileOutputStream(_output_file);
		_bit_sink = new OutputStreamBitSink(_out_stream);
		
		// write # frames, # pixels, range
		_bit_sink.write(_pixel_count, 32);
		_bit_sink.write(_frames, 32);
		_bit_sink.write(_range, 8);
	}
	
	public void encode() throws IOException {
		// starting frames need some reference, using grey
		int i = 0;
		while (i < _pixels_in_frame) {
			_last_frame[i] = 125;
			i++;
		}
		
		// loop for each pixel in each frame
		for (int n = 0; n < _frames; n++) { // frames
			int current_frame = n;
			for (int k = 0; k < _pixels_in_frame; k++) { // pixels
				// gather intensity from the input file
				int intensity = _in_stream.read();
				// use last frame for models 
				// left-most pixels have no pixels to the left, so they just take from their previous intensity
				if (k == 0 || (k % 64) == 0) {
					FreqCountIntegerSymbolModel model = _models[_last_frame[k]];
					_arithmetic_encoder.encode(intensity, model, _bit_sink);
					model.addToCount(intensity);
					_last_frame[k] = intensity;
				}
				// all other pixels take from the previous intensity of the pixel to the left of them
				else {
					FreqCountIntegerSymbolModel model = _models[_last_frame[k - 1]];
					_arithmetic_encoder.encode(intensity, model, _bit_sink);
					model.addToCount(intensity);
					_last_frame[k - 1] = intensity;
				}
			}
		}
		
		// emit middle of arithmetic encoder, close input stream and output stream, pad bit sink 
		_arithmetic_encoder.emitMiddle(_bit_sink);
		_bit_sink.padToWord();
		_in_stream.close();
		_out_stream.close();
	}

}
