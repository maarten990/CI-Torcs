import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.encog.util.arrayutil.NormalizeArray;
import scr.SensorModel;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

public class DataModel {
    /**
     * Load a csv file into input and output data.
     * The first 3 columns are taken as input, the rest as output (the same format
     * as used by the given dataset)
     */
    public static Data load_data(String path) throws IOException {
        Reader in = new FileReader(path);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(in);

        ArrayList<double[]> outputs = new ArrayList<>();
        ArrayList<double[]> inputs = new ArrayList<>();

        // ugly boolean to skip the first row of the file, which contains the header
        boolean is_header = true;
        for (CSVRecord row : records) {
            if (is_header) {
                is_header = false;
                continue;
            }

            // one row seems to be corrupt or something, so skip any rows that
            // don't have the expected number of columns
            if (row.size() != 25)
                continue;

            double[] output_row = new double[3];
            double[] input_row = new double[row.size() - 3];

            for (int i = 0; i < row.size(); ++i) {
                if (i < 3)
                    output_row[i] = (Double.parseDouble(row.get(i)));
                else
                    input_row[i - 3] = (Double.parseDouble(row.get(i)));
            }

            outputs.add(output_row);
            inputs.add(input_row);
        }

        // Use Encog's normalizer to normalize the data. Neural networks love
        // normalized data.
        NormalizeArray normalizer = new NormalizeArray();
        normalizer.setNormalizedLow(-1);
        normalizer.setNormalizedHigh(1);

        double[][] input_array = inputs.toArray(new double[0][0]);
        double[][] output_array = outputs.toArray(new double[0][0]);

        for (int i = 0; i < input_array.length; ++i) {
            input_array[i] = normalizer.process(input_array[i]);
        }
        return new Data(input_array, output_array);
    }

    /**
     * Format sensor output into an array with the same parameters as the training data.
     */
    public static double[] format_input(SensorModel sensors) {
        ArrayList<Double> input_list = new ArrayList<>();
        input_list.add(sensors.getSpeed());
        input_list.add(sensors.getTrackPosition());
        input_list.add(sensors.getAngleToTrackAxis());

        for (double sensor : sensors.getTrackEdgeSensors()) {
            input_list.add(sensor);
        }

        double[] input = new double[22];
        for (int i = 0; i < input.length; ++i)
            input[i] = input_list.get(i);

        NormalizeArray normalizer = new NormalizeArray();
        normalizer.setNormalizedLow(-1);
        normalizer.setNormalizedHigh(1);
        input = normalizer.process(input);

        return input;
    }
}

/**
 * Simple wrapper class to hold training data along with the desired outputs.
 */
class Data {
    public double[][] X;
    public double[][] Y;

    public Data(double[][] X, double[][] Y) {
        this.X = X;
        this.Y = Y;
    }
}
