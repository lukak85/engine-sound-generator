package esg.Engines;

import com.jsyn.Synthesizer;
import com.jsyn.unitgen.LineOut;
import com.jsyn.unitgen.Multiply;
import com.jsyn.util.WaveRecorder;

import java.io.File;
import java.io.IOException;

public abstract class EngineBase {

    protected Synthesizer synth;
    protected LineOut lineOut;

    protected File waveFile;
    protected WaveRecorder recorder;
    protected Multiply mpOut;

    protected abstract void run();

    protected void outputAndSave() {
        // Output the sound to speakers
        mpOut.output.connect(0, lineOut.input, 0);
        mpOut.output.connect(0, lineOut.input, 1);

        // Save the sound to a file specified above
        mpOut.output.connect( 0, recorder.getInput(), 0 ); // left
        mpOut.output.connect( 0, recorder.getInput(), 1 ); // right
        recorder.start();

        do {
            try {
                synth.sleepFor(5.0);
            } catch (InterruptedException e) {
            }
        } while (!isFinished());

        recorder.stop();
        try {
            recorder.close();
        } catch (IOException e) {
        }

        synth.stop();
    }

    protected static boolean isFinished() {
        return true;
    }
}
