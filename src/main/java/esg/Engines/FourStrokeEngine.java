package esg.Engines;

import com.jsyn.JSyn;
import com.jsyn.data.DoubleTable;
import com.jsyn.data.Function;
import com.jsyn.unitgen.*;
import com.jsyn.util.WaveRecorder;
import esg.Units.WrapUnit;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;

public class FourStrokeEngine extends EngineBase {

    // Basic four-stroke engine, roughly follows the model presented by the work
    // "Physically informed car engine sound synthesis for virtual and augmented
    // environments"
    // by Stefano Baldan, Helene Lachambre, Stefano Delle Monache, Patrick Boussard

    public static void main(String[] args) {
        new FourStrokeEngine().run();
    }

    Add sawtoothMain;

    int speed;
    double overtoneFrequency1, overtoneFrequency2, overtoneFrequency3;
    double overtoneAmplitude1, overtoneAmplitude2, overtoneAmplitude3;
    double overtonePhase1, overtonePhase2, overtonePhase3;
    double transmissionDelay1, transmissionDelay2, transmissionDelay3;
    double warpDelay;
    double waveguideWrapping;
    double parabolaDelay;
    double mixParabola;

    @Override
    protected void parseJSON(JSONObject engine)
    {
        overtoneFrequency1 = ((Number)engine.get("overtoneFrequency1")).doubleValue();
        overtoneFrequency2 = ((Number)engine.get("overtoneFrequency2")).doubleValue();
        overtoneFrequency3 = ((Number)engine.get("overtoneFrequency3")).doubleValue();

        overtoneAmplitude1 = ((Number)engine.get("overtoneAmplitude1")).doubleValue();
        overtoneAmplitude2 = ((Number)engine.get("overtoneAmplitude2")).doubleValue();
        overtoneAmplitude3 = ((Number)engine.get("overtoneAmplitude3")).doubleValue();

        transmissionDelay1 = ((Number)engine.get("transmissionDelay1")).doubleValue();
        transmissionDelay2 = ((Number)engine.get("transmissionDelay2")).doubleValue();
        transmissionDelay3 = ((Number)engine.get("transmissionDelay3")).doubleValue();

        overtonePhase1 = ((Number)engine.get("overtonePhase1")).doubleValue();
        overtonePhase2 = ((Number)engine.get("overtonePhase2")).doubleValue();
        overtonePhase3 = ((Number)engine.get("overtonePhase3")).doubleValue();

        warpDelay = ((Number)engine.get("warpDelay")).doubleValue();

        speed = (int) (((Number)engine.get("speed")).doubleValue() * 40);
        waveguideWrapping = ((Number)engine.get("waveguideWrapping")).doubleValue();

        parabolaDelay = ((Number)engine.get("parabolaDelay")).doubleValue();
        mixParabola = ((Number)engine.get("mixParabola")).doubleValue();
    }

    @Override
    protected void run() {
        // -------------------------------------------------
        // Read parameters
        // -------------------------------------------------

        this.readParameters("./example-parameters/FourStrokeEngineParameters.json");


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

        SawtoothOscillator st;
        synth.add(st = new SawtoothOscillator());
        st.frequency.set(speed);
        st.amplitude.set(0.5);

        sawtoothMain = new Add();
        synth.add(sawtoothMain);
        sawtoothMain.inputA.connect(st.output);
        sawtoothMain.inputB.set(0.5);

        // -----------------------------------------------
        // Cylinder sound
        // -----------------------------------------------

        // Replicate phasor 4 times and give it a different
        // delay each time
        InterpolatingDelay id1;
        InterpolatingDelay id2;
        InterpolatingDelay id3;
        InterpolatingDelay id4;
        synth.add(id1 = new InterpolatingDelay());
        synth.add(id2 = new InterpolatingDelay());
        synth.add(id3 = new InterpolatingDelay());
        synth.add(id4 = new InterpolatingDelay());
        id1.input.connect(sawtoothMain.output);
        id2.input.connect(sawtoothMain.output);
        id3.input.connect(sawtoothMain.output);
        id4.input.connect(sawtoothMain.output);
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
        WhiteNoise wn1;
        WhiteNoise wn2;
        WhiteNoise wn3;
        WhiteNoise wn4;
        wn1 = new WhiteNoise();
        wn2 = new WhiteNoise();
        wn3 = new WhiteNoise();
        wn4 = new WhiteNoise();
        synth.add(wn1);
        synth.add(wn2);
        synth.add(wn3);
        synth.add(wn4);


        FilterLowPass lp1;
        FilterLowPass lp2;
        FilterLowPass lp3;
        FilterLowPass lp4;

        lp1 = new FilterLowPass();
        lp2 = new FilterLowPass();
        lp3 = new FilterLowPass();
        lp4 = new FilterLowPass();
        synth.add(lp1);
        synth.add(lp2);
        synth.add(lp3);
        synth.add(lp4);
        lp1.frequency.set(20);
        lp2.frequency.set(20);
        lp3.frequency.set(20);
        lp4.frequency.set(20);

        lp1.input.connect(wn1.output);
        lp2.input.connect(wn2.output);
        lp3.input.connect(wn3.output);
        lp4.input.connect(wn4.output);


        // Aggregate white noise that passed trough LPF with
        // the delayed phasor
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

        // Add them together, which will represent
        // the engine sound
        PassThrough psen = new PassThrough();
        psen.input.connect(mp1.output);
        psen.input.connect(mp2.output);
        psen.input.connect(mp3.output);
        psen.input.connect(mp4.output);


        // -----------------------------------------------
        // Exhaust sound
        // -----------------------------------------------

        // Parabolic function
        Multiply parabola = parabola();


        // -------------------------------------------
        // FMs
        // -------------------------------------------

        // FM1
        Add fm1 = fm1();

        // FM2
        Add fm2 = fm2();


        // -------------------------------------------
        // Overtones
        // -------------------------------------------

        Multiply overtone1 = overtone(overtoneFrequency1, overtoneAmplitude1, 8, overtonePhase1, transmissionDelay1);
        Multiply overtone2 = overtone(overtoneFrequency2, overtoneAmplitude2, 16, overtonePhase2, transmissionDelay2);
        Multiply overtone3 = overtone(overtoneFrequency3, overtoneAmplitude3, 4, overtonePhase3, transmissionDelay3);


        // -------------------------------------------
        // Delay chain
        // -------------------------------------------

        InterpolatingDelay delayChain = delayChain(parabola, fm1, fm2, overtone1, overtone2, overtone3);


        // -------------------------------------------
        // High pass filter for engine
        // -------------------------------------------

        FilterHighPass hpe;
        synth.add(hpe = new FilterHighPass());
        hpe.input.connect(delayChain.output);
        hpe.frequency.set(200);


        // -------------------------------------------
        // High pass filter for engine
        // -------------------------------------------

        FilterHighPass hpen;
        synth.add(hpen = new FilterHighPass());
        hpen.input.connect(psen.output);
        hpen.frequency.set(2);


        // -------------------------------------------
        // Add the results together
        // -------------------------------------------
        Multiply mpen = new Multiply();
        mpen.inputA.connect(hpen.output); // Engine
        mpen.inputB.connect(hpe.output); // Exhaust

        Multiply mpenout = new Multiply();
        mpenout.inputA.connect(mpen.output);
        mpenout.inputB.set(0.5);

        // Create output unit
        mpOut = new Multiply();
        mpOut.inputA.connect(mpenout.output);
        mpOut.inputB.set(0.2); // Volume

        outputAndSave();
    }

    //

    protected InterpolatingDelay delayChain(
            Multiply parabola,
            Add fm1,
            Add fm2,
            Multiply overtone1,
            Multiply overtone2,
            Multiply overtone3
    ) {
        InterpolatingDelay id5;
        InterpolatingDelay id6;
        InterpolatingDelay id7;
        InterpolatingDelay id8;

        synth.add(id5 = new InterpolatingDelay());
        synth.add(id6 = new InterpolatingDelay());
        synth.add(id7 = new InterpolatingDelay());
        synth.add(id8 = new InterpolatingDelay());

        // Quadrant 1
        PassThrough ps1;
        synth.add(ps1 = new PassThrough());
        ps1.input.connect(parabola.output);
        ps1.input.connect(fm2.output);
        ps1.input.connect(id8.output);

        id5.delay.set(0.062);
        id5.allocate(44100);
        id5.input.connect(ps1.output);


        // Quadrant 2
        PassThrough ps2;
        synth.add(ps2 = new PassThrough());
        ps2.input.connect(overtone1.output);
        ps2.input.connect(fm1.output);
        ps2.input.connect(id5.output);

        id6.delay.set(0.124);
        id6.allocate(44100);
        id6.input.connect(ps2.output);


        // Quadant 3
        PassThrough ps3;
        synth.add(ps3 = new PassThrough());
        ps3.input.connect(overtone2.output);
        ps3.input.connect(overtone3.output);
        ps3.input.connect(fm1.output);
        ps3.input.connect(id6.output);

        id7.delay.set(0.186);
        id7.allocate(44100);
        id7.input.connect(ps3.output);


        // Quadrant 4
        PassThrough ps4;
        synth.add(ps4 = new PassThrough());
        ps4.input.connect(fm2.output);
        ps4.input.connect(id7.output);

        id8.delay.set(0.248);
        id8.allocate(44100);
        id8.input.connect(ps4.output);

        return id5;
    }


    // ---------------------------------------------------------------------------------
    // *************************** OVERTONES *******************************************
    // ---------------------------------------------------------------------------------

    protected Multiply overtone(double otF, double otA, int i, double otP, double otD) {
        Multiply mp1 = new Multiply();
        synth.add(mp1);
        mp1.inputA.connect(sawtoothMain.output);
        mp1.inputB.set(i);

        InterpolatingDelay id;
        synth.add(id = new InterpolatingDelay());
        id.allocate(44100);
        id.delay.set(100 * otD * 300);
        id.input.connect(mp1.output);

        WrapUnit wu1 = new WrapUnit();
        synth.add(wu1);
        wu1.input.connect(id.output);

        Add inletPhase = new Add();
        synth.add(inletPhase);
        inletPhase.inputA.set(otP);
        inletPhase.inputB.set(0);

        Maximum max;
        synth.add(max = new Maximum());
        max.inputA.connect(wu1.output);
        max.inputB.connect(inletPhase.output);

        Multiply ipn = new Multiply();
        synth.add(ipn);
        inletPhase.inputA.set(-1);
        inletPhase.inputB.connect(inletPhase.output);

        Add add1 = new Add();
        synth.add(add1);
        add1.inputA.connect(max.output);
        add1.inputB.connect(ipn.output);

        Add sig1 = new Add();
        synth.add(sig1);
        sig1.inputA.set(1);
        sig1.inputB.set(0);

        Multiply mp2 = new Multiply();
        synth.add(mp2);
        mp2.inputA.connect(inletPhase.output);
        mp2.inputB.set(-1);

        Add add2 = new Add();
        synth.add(add2);
        add2.inputA.connect(sig1.output);
        add2.inputB.connect(mp2.output);

        Multiply mp3 = new Multiply();
        synth.add(mp3);
        mp3.inputA.connect(inletPhase.output);
        mp3.inputB.set(-1);

        Add sig2 = new Add();
        synth.add(sig2);
        sig2.inputA.set(1);
        sig2.inputB.set(0);

        Multiply mp4 = new Multiply();
        synth.add(mp4);
        mp4.inputA.connect(sig2.output);
        mp4.inputB.connect(mp3.output);

        Multiply mp5 = new Multiply();
        synth.add(mp5);
        mp5.inputA.connect(ipn.output);
        mp5.inputB.connect(mp4.output);

        Add inletFrequency = new Add();
        synth.add(inletFrequency);
        inletFrequency.inputA.set(12 * otF);
        inletFrequency.inputB.set(0);

        Multiply mp6 = new Multiply();
        synth.add(mp6);
        mp6.inputA.connect(inletFrequency.output);
        mp6.inputB.connect(inletPhase.output);

        Multiply mp7 = new Multiply();
        synth.add(mp7);
        mp7.inputA.connect(mp5.output);
        mp7.inputB.connect(mp6.output);

        WrapUnit wu = new WrapUnit();
        synth.add(wu);
        wu.input.connect(mp7.output);

        Function cubeFunction = new Function()
        {
            public double evaluate( double x )
            {
                return (-4 * Math.pow(x - 5, 2) + 1) / 2;
            }
        };
        FunctionEvaluator fe;
        synth.add(fe = new FunctionEvaluator());
        fe.function.set(cubeFunction);

        fe.input.connect(wu.output);

        Add sig3 = new Add();
        synth.add(sig3);
        sig3.inputA.set(1);
        sig3.inputB.set(0);

        Multiply idi = new Multiply();
        synth.add(idi);
        idi.inputA.connect(wu1.output);
        idi.inputB.set(-1);

        Add add3 = new Add();
        synth.add(sig3);
        add3.inputA.connect(sig3.output);
        add3.inputB.connect(idi.output);

        Multiply mp8 = new Multiply();
        synth.add(mp8);
        mp8.inputA.connect(fe.output);
        mp8.inputB.connect(add3.output);

        Add inletAmplitude = new Add();
        synth.add(inletAmplitude);
        inletAmplitude.inputA.set(12 * otA);
        inletAmplitude.inputB.set(0);

        Multiply mp9 = new Multiply();
        synth.add(mp9);
        mp9.inputA.connect(inletAmplitude.output);
        mp9.inputB.connect(mp8.output);

        return mp9;
    }


    // ---------------------------------------------------------------------------------
    // *************************** FM1 *************************************************
    // ---------------------------------------------------------------------------------

    protected Add fm1() {
        SawtoothOscillator st;
        synth.add(st = new SawtoothOscillator());
        st.frequency.set(speed);

        SineOscillator so;
        synth.add(so = new SineOscillator());
        so.phase.set(0.75);

        InterpolatingDelay id;
        synth.add(id = new InterpolatingDelay());
        id.allocate(44100);
        id.delay.set(100 * warpDelay);
        id.input.connect(so.output);

        Multiply mp1;
        synth.add(mp1 = new Multiply());
        mp1.inputA.set(speed);
        mp1.inputB.set(waveguideWrapping);

        FilterLowPass lp = new FilterLowPass();
        synth.add(lp);
        lp.frequency.set(0.2);
        lp.input.connect(mp1.output);

        Multiply mp2;
        synth.add(mp2 = new Multiply());
        mp2.inputA.connect(id.output);
        mp2.inputB.connect(lp.output);

        Add add2;
        synth.add(add2 = new Add());
        add2.inputA.connect(mp2.output);
        add2.inputB.set(0.5);

        return add2;
    }


    // ---------------------------------------------------------------------------------
    // *************************** FM2 *************************************************
    // ---------------------------------------------------------------------------------

    protected Add fm2() {
        InterpolatingDelay id;
        synth.add(id = new InterpolatingDelay());
        id.allocate(44100);
        id.delay.set(100 * warpDelay);
        id.input.connect(sawtoothMain.output);

        Multiply mp1;
        synth.add(mp1 = new Multiply());
        mp1.inputA.set(1);
        mp1.inputB.set(1);

        Multiply mp2;
        synth.add(mp2 = new Multiply());
        mp2.inputA.connect(id.output);
        mp2.inputB.set(-1);

        Add add1;
        synth.add(add1 = new Add());
        add1.inputA.connect(mp1.output);
        add1.inputB.connect(mp2.output);

        Add add2;
        synth.add(add2 = new Add());
        add2.inputA.connect(mp2.output);
        add2.inputB.set(0.5);

        return add2;
    }

    // ---------------------------------------------------------------------------------
    // ****************** PARABOLIC FUNCTION (engine vibration) ************************
    // ---------------------------------------------------------------------------------

    protected Multiply parabola() {
        InterpolatingDelay id;
        synth.add(id = new InterpolatingDelay());
        id.allocate(44100);
        id.delay.set(parabolaDelay * 100);
        id.input.connect(sawtoothMain.output);

        Function cubeFunction = new Function()
        {
            public double evaluate( double x )
            {
                return (Math.pow(x - 0.5, 2) * 4 + 1) * 3;
            }
        };

        FunctionEvaluator fe;
        synth.add(fe = new FunctionEvaluator());
        fe.function.set(cubeFunction);
        fe.input.connect(id.output);

        Multiply mp;
        synth.add(mp = new Multiply());
        mp.inputA.connect(fe.output);
        mp.inputB.set(mixParabola);

        return mp;
    }
}
