package esg.Engines;

import com.jsyn.JSyn;
import com.jsyn.unitgen.*;
import com.jsyn.util.WaveRecorder;

import java.io.File;
import java.io.FileNotFoundException;

public class ToyBoatEngine extends EngineBase {

    // Toy boat engine sound as presented in book Designing Sound
    // by Andy Farnell

    public static void main(String[] args) {
        new ToyBoatEngine().run();
    }

    @Override
    protected void run() {
        // -------------------------------------------------
        // Set which files to record to
        waveFile = new File("examples/toy_engine.wav");
        recorder = null;

        synth = JSyn.createSynthesizer();
        synth.add(lineOut = new LineOut());
        synth.start();
        lineOut.start();

        try {
            recorder = new WaveRecorder(synth, waveFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            synth.stop();
            return;
        }
        // -------------------------------------------------

        /*
        TODO -- fix the implementation as at the moment it does not
         yet sound like it's supposed to, and some components do not
         work as intended
         */

        // Create white noise and pass it through band pass filter
        WhiteNoise wn1 = new WhiteNoise();
        synth.add(wn1);
        wn1.amplitude.set(1.0);

        FilterBandPass bp1 = new FilterBandPass();
        synth.add(bp1);
        bp1.frequency.set(9);
        bp1.Q.set(15);
        wn1.output.connect(bp1.input);


        // Add an oscillator
        SineOscillator so1 = new SineOscillator(9);
        synth.add(so1);
        so1.amplitude.set(1.0);


        // Add oscillator and white noise together
        PassThrough ps1 = new PassThrough();
        ps1.input.connect(bp1.output);
        ps1.input.connect(so1.output);


        // Multiplying the summed output of both, which will
        // result in clipping
        Multiply multiply1 = new Multiply();
        multiply1.inputA.connect(ps1.output);
        multiply1.inputB.set(600);


        // Preform clipping by moving it down by one
        // then moving back up, effectively cutting out
        // the signal bellow amplitude 0
        Add adderClip1 = new Add();
        adderClip1.inputA.connect(multiply1.output);
        adderClip1.inputB.set(-1);

        Add adderClip2 = new Add();
        adderClip2.inputA.connect(adderClip1.output);
        adderClip2.inputB.set(1);


        // First high pass filter
        FilterHighPass hp1 = new FilterHighPass();
        synth.add(hp1);
        adderClip2.output.connect(hp1.input);
        hp1.frequency.set(10);

        // First low pass filter
        FilterLowPass lp1 = new FilterLowPass();
        synth.add(lp1);
        hp1.output.connect(lp1.input);
        lp1.frequency.set(30);


        // Second white noise, passed through high pass filter and band pass filter
        WhiteNoise wn2 = new WhiteNoise();
        synth.add(wn2);
        wn2.amplitude.set(1.0);

        FilterHighPass hp2 = new FilterHighPass();
        synth.add(hp2);
        hp2.frequency.set(1000);
        wn2.output.connect(hp2.input);

        FilterBandPass bp2 = new FilterBandPass();
        synth.add(bp2);
        bp2.frequency.set(590);
        bp2.Q.set(4);
        hp2.output.connect(bp2.input);


        // Multiplying the two branches together
        Multiply multiply2 = new Multiply();
        lp1.output.connect(multiply2.inputA);
        bp2.output.connect(multiply2.inputB);


        // Connections to three different band passes
        FilterBandPass bp3 = new FilterBandPass();
        synth.add(bp3);
        bp3.frequency.set(470);
        bp3.Q.set(8);
        multiply2.output.connect(bp3.input);

        FilterBandPass bp4 = new FilterBandPass();
        synth.add(bp4);
        bp4.frequency.set(780);
        bp4.Q.set(9);
        multiply2.output.connect(bp4.input);

        FilterBandPass bp5 = new FilterBandPass();
        synth.add(bp5);
        bp5.frequency.set(1024);
        bp5.Q.set(10);
        multiply2.output.connect(bp5.input);


        // Connect them all together
        PassThrough ps2 = new PassThrough();
        ps2.input.connect(bp3.output);
        ps2.input.connect(bp4.output);
        ps2.input.connect(bp5.output);


        // Pass the result through high pass filter
        FilterHighPass hp3 = new FilterHighPass();
        synth.add(hp3);
        ps2.output.connect(hp3.input);
        hp3.frequency.set(100);

        // Then multiply
        Multiply multiply3 = new Multiply();
        multiply3.inputA.connect(hp3.output);
        multiply3.inputB.set(2);


        // Connections coming to this output will be the
        // resulting sound output to speakers. We lower the
        // volume for that purpose
        mpOut = new Multiply();
        mpOut.inputA.connect(multiply3.output);
        mpOut.inputB.set(0.01);

        outputAndSave();
    }
}
