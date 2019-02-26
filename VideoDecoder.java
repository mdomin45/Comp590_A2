package app;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import ac.ArithmeticDecoder;
import io.InputStreamBitSource;
import io.InsufficientBitsLeftException;

public class VideoDecoder {
	private String _input_file;
	private String _output_file;
	private int _pixel_count;
	private int _pixels_in_frame;
	private int _frames;
	private int _range;
	private FileInputStream _in_stream;
	private FileOutputStream _out_stream;
	private InputStreamBitSource _bit_stream;
	private Integer[] _pixel_intensities;
	private FreqCountIntegerSymbolModel [] _models;
	private ArithmeticDecoder<Integer> _arithmetic_decoder;
	private Integer[] _last_frame;
	
	
	public VideoDecoder(String in_file, String out_file) throws InsufficientBitsLeftException, IOException {
		// TODO Auto-generated constructor stub
		_input_file = in_file;
		_output_file = out_file;
		_in_stream = new FileInputStream(_input_file);
		_out_stream = new FileOutputStream(_output_file);
		_bit_stream = new InputStreamBitSource(_in_stream);
		_pixel_count = _bit_stream.next(32); // first 32 bits are how many pixels
		_frames = _bit_stream.next(32); // next 32 bits are how many frames
		_range = _bit_stream.next(8); // last 8 are the bit width range
		_pixels_in_frame = _pixel_count / _frames;
		_arithmetic_decoder = new ArithmeticDecoder<Integer>(_range);
		_last_frame = new Integer[_pixels_in_frame]; // 4096 pixels/bytes per frame
		
		// have to start with a previous intensity, everything starts grey (125)
		int i = 0;
		while (i < 4096) {
			_last_frame[i] = 125;
			i++;
		}
		
		_pixel_intensities = new Integer[256];
		
		i = 0;
		while (i < 256) {
			_pixel_intensities[i] = i;
			i++;
		}
		
		_models = new FreqCountIntegerSymbolModel[256];
		
		i = 0;
		while (i < 256) {
			_models[i] = new FreqCountIntegerSymbolModel(_pixel_intensities);
			i++;
		}
	}
	
	public void decode() throws IOException, InsufficientBitsLeftException {
		
		// loop through the pixels in each frame
		for (int n = 0; n < _frames; n++) { // frames
			for (int k = 0; k < _pixels_in_frame; k++) { // pixels
				// left-most pixels
				if (k == 0 || (k % 64) == 0) {
					FreqCountIntegerSymbolModel model = _models[_last_frame[k]];
					int pixelIntensity = _arithmetic_decoder.decode(model, _bit_stream);
					_out_stream.write(pixelIntensity);
					model.addToCount(pixelIntensity);
					_last_frame[k] = pixelIntensity;
				}
				// all other pixels
				else {
					FreqCountIntegerSymbolModel model = _models[_last_frame[k - 1]];
					int pixelIntensity = _arithmetic_decoder.decode(model, _bit_stream);
					_out_stream.write(pixelIntensity);
					model.addToCount(pixelIntensity);
					_last_frame[k - 1] = pixelIntensity;
				}
			}
		}
		_in_stream.close();
		_out_stream.close();
	}

}
