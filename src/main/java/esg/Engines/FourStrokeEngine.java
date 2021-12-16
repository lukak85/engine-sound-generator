package esg.Engines;

import com.jsyn.JSyn;
import com.jsyn.unitgen.*;
import com.jsyn.util.WaveRecorder;

import java.io.File;
import java.io.FileNotFoundException;

public class FourStrokeEngine extends EngineBase {

    // Basic four-stroke engine, as presented by the work "Physically informed car
    // engine sound synthesis for virtual and augmented environments"
    // by Stefano Baldan, Helene Lachambre, Stefano Delle Monache, Patrick Boussard

    public static void main(String[] args) {
        new FourStrokeEngine().run();
    }

    InterpolatingDelay id1;
    InterpolatingDelay id2;
    InterpolatingDelay id3;
    InterpolatingDelay id4;

    WhiteNoise wn1;
    WhiteNoise wn2;
    WhiteNoise wn3;
    WhiteNoise wn4;

    FilterLowPass lp1;
    FilterLowPass lp2;
    FilterLowPass lp3;
    FilterLowPass lp4;

    int fundamentalFrequency = 20;

    SawtoothOscillatorBL overtone1;
    SawtoothOscillatorBL overtone2;
    SawtoothOscillatorBL overtone3;

    ParabolicEnvelope pe;

    InterpolatingDelay id5;
    InterpolatingDelay id6;
    InterpolatingDelay id7;
    InterpolatingDelay id8;

    @Override
    protected void run() {
        // -------------------------------------------------
        // Set which files to record to
        waveFile = new File("examples/four_stroke.wav");
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
        TODO -- add the exhaust model
         */

        SawtoothOscillatorBL sawtooth;
        synth.add(sawtooth = new SawtoothOscillatorBL());
        sawtooth.frequency.set(fundamentalFrequency);

        // -----------------------------------------------
        // Cylinder sound
        // -----------------------------------------------

        // Fan out the phasor and delay it four times
        synth.add(id1 = new InterpolatingDelay());
        synth.add(id2 = new InterpolatingDelay());
        synth.add(id3 = new InterpolatingDelay());
        synth.add(id4 = new InterpolatingDelay());
        id1.input.connect(sawtooth.output);
        id2.input.connect(sawtooth.output);
        id3.input.connect(sawtooth.output);
        id4.input.connect(sawtooth.output);
        id1.allocate(44100);
        id2.allocate(44100);
        id3.allocate(44100);
        id4.allocate(44100);
        id1.delay.set(0);
        id2.delay.set(0.25);
        id3.delay.set(0.5);
        id4.delay.set(0.75);


        // Create white noise and pass it trough a low pass
        // filter
        wn1 = new WhiteNoise();
        wn2 = new WhiteNoise();
        wn3 = new WhiteNoise();
        wn4 = new WhiteNoise();
        synth.add(wn1);
        synth.add(wn2);
        synth.add(wn3);
        synth.add(wn4);

        lp1 = new FilterLowPass();
        lp2 = new FilterLowPass();
        lp3 = new FilterLowPass();
        lp4 = new FilterLowPass();
        synth.add(lp1);
        synth.add(lp2);
        synth.add(lp3);
        synth.add(lp4);
        lp1.frequency.set(200);
        lp2.frequency.set(200);
        lp3.frequency.set(200);
        lp4.frequency.set(200);

        lp1.input.connect(wn1.output);
        lp2.input.connect(wn2.output);
        lp3.input.connect(wn3.output);
        lp4.input.connect(wn4.output);


        // Connect white noise that passed trough LPF to the
        // delayed phasor
        Multiply mp1 = new Multiply();
        Multiply mp2 = new Multiply();
        Multiply mp3 = new Multiply();
        Multiply mp4 = new Multiply();

        mp1.inputA.connect(id1.output);
        mp1.inputB.connect(lp1.output);
        mp2.inputA.connect(id2.output);
        mp2.inputB.connect(lp2.output);
        mp3.inputA.connect(id3.output);
        mp3.inputB.connect(lp3.output);
        mp4.inputA.connect(id4.output);
        mp4.inputB.connect(lp4.output);

        // -----------------------------------------------
        // Exhaust sound
        // -----------------------------------------------

        synth.add(overtone1 = new SawtoothOscillatorBL());
        synth.add(overtone2 = new SawtoothOscillatorBL());
        synth.add(overtone3 = new SawtoothOscillatorBL());
        overtone1.frequency.set(fundamentalFrequency);
        overtone2.frequency.set(fundamentalFrequency);
        overtone3.frequency.set(fundamentalFrequency);

        // Parabolic function
        synth.add(pe = new ParabolicEnvelope());
        pe.frequency.set(fundamentalFrequency);

        Multiply mp5;
        synth.add(mp5 = new Multiply());
        mp5.inputA.connect(pe.output);
        mp5.inputB.connect(sawtooth.output);

        // -------------------------------------------
        // FMs
        // -------------------------------------------
        // FM1
        Multiply mpfm1 = new Multiply();
        synth.add(mpfm1);
        mpfm1.inputA.connect(sawtooth.output);
        mpfm1.inputB.set(1);

        Add adderfm1 = new Add();
        synth.add(adderfm1);
        adderfm1.inputA.connect(mpfm1.output);
        adderfm1.inputB.set(1);

        Multiply mpfm2 = new Multiply();
        synth.add(mpfm2);
        mpfm2.inputA.connect(sawtooth.output);
        mpfm2.inputB.set(1);

        Add adderfm2 = new Add();
        synth.add(adderfm2);
        adderfm2.inputA.connect(mpfm2.output);
        adderfm2.inputB.set(1);

        // -------------------------------------------

        // Delay chain
        synth.add(id5 = new InterpolatingDelay());
        synth.add(id6 = new InterpolatingDelay());
        synth.add(id7 = new InterpolatingDelay());
        synth.add(id8 = new InterpolatingDelay());

        // Quadrant 1
        PassThrough ps1;
        synth.add(ps1 = new PassThrough());
        ps1.input.connect(mp5.output);
        ps1.input.connect(id8.output);
        // --
        ps1.input.connect(adderfm1.output);
        // --

        id5.delay.set(0.25);
        id5.allocate(2205);
        id5.input.connect(ps1.output);


        // Quadrant 2
        PassThrough ps2;
        synth.add(ps2 = new PassThrough());
        ps2.input.connect(overtone1.output);
        ps2.input.connect(id5.output);
        // --
        ps2.input.connect(adderfm2.output);
        // --

        id6.delay.set(0.5);
        id6.allocate(2205);
        id6.input.connect(ps2.output);

        // Quadant 3
        PassThrough ps3;
        synth.add(ps3 = new PassThrough());
        ps3.input.connect(overtone2.output);
        ps3.input.connect(overtone3.output);
        ps3.input.connect(id6.output);
        // --
        ps3.input.connect(adderfm2.output);
        // --

        id7.delay.set(0.75);
        id7.allocate(2205);
        id7.input.connect(ps3.output);

        // Quadrant 4
        PassThrough ps4;
        synth.add(ps4 = new PassThrough());
        id8.delay.set(1);
        id8.allocate(2205);
        id8.input.connect(id7.output);
        // --
        ps4.input.connect(adderfm1.output);
        // --


        // Add the results together
        PassThrough ps = new PassThrough();
        ps.input.connect(mp1.output);
        ps.input.connect(mp2.output);
        ps.input.connect(mp3.output);
        ps.input.connect(mp4.output);
        // TODO -- fix this input, since it's probably not correct
        ps.input.connect(id8.output);

        // Create output unit
        mpOut = new Multiply();
        mpOut.inputA.connect(ps.output);
        mpOut.inputB.set(0.5);

        outputAndSave();
    }
}
