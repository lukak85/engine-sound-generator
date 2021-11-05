package esg;

import com.jsyn.JSyn;
import com.jsyn.Synthesizer;
import com.jsyn.unitgen.LineOut;
import com.jsyn.unitgen.PassThrough;
import com.jsyn.unitgen.SineOscillator;

public class GenerateEngineSound {

	Synthesizer synth;
	LineOut lineOut;

	public static void main(String[] args) {
		new GenerateEngineSound().start();
	}

	private void start() {
		synth = JSyn.createSynthesizer();
		synth.add(lineOut = new LineOut());
		synth.start();
		lineOut.start();

		PassThrough ps = new PassThrough();

		SineOscillator so1 = new SineOscillator(1000, 0.5);
		SineOscillator so2 = new SineOscillator(2000, 0.5);

		synth.add(so1);
		synth.add(so2);

		so1.output.connect(ps.input);
		so2.output.connect(ps.input);

		ps.output.connect(lineOut.input);

		try {
			synth.sleepFor(2.0);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		synth.stop();
	}
}
