package esg.Units;

import com.jsyn.unitgen.UnitFilter;

/**
 * Custom unit generator that can be used with other JSyn units.
 * Wrap the input value and write it to output port.
 *
 * @author Phil Burk (C) 2010 Mobileer Inc
 *
 */
public class WrapUnit extends UnitFilter
{
    @Override
    public void generate( int start, int limit )
    {
        // Get signal arrays from ports.
        double[] inputs = input.getValues();
        double[] outputs = output.getValues();

        for (int i = start; i < limit; i++)
        {
            double x = inputs[i];

            x += 1;
            x = x % 1;

            outputs[i] = x;
        }
    }
}
