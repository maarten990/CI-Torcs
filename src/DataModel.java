import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.encog.util.arrayutil.NormalizeArray;
import scr.Action;
import scr.SensorModel;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataModel implements Serializable {
    private double[] means;
    private double[] stddevs;
    public DataModel() {
    }

    /**
     * Load a csv file into input and output data.
     * The first 3 columns are taken as input, the rest as output (the same format
     * as used by the given dataset)
     */
    public Data load_data(List<String> paths) throws IOException {
        ArrayList<double[]> outputs = new ArrayList<>();
        ArrayList<double[]> inputs = new ArrayList<>();

        for (String path : paths) {
            Reader in = new FileReader(path);
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(in);

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
        }

        double[][] input_array = inputs.toArray(new double[0][0]);
        double[][] output_array = outputs.toArray(new double[0][0]);

        // calculate the mean and the standard deviation of the data
        means = new double[input_array[0].length];
        stddevs = new double[input_array[0].length];
        for (int column = 0; column < means.length; ++column) {
            double mean = mean(input_array, column);
            double stddev = std_deviation(input_array, column, mean);

            means[column] = mean;
            stddevs[column] = stddev;
        }

        for (double[] input : input_array) {
            normalize(input);
        }

        return new Data(input_array, output_array);
    }

    public Data load_q_data(List<String> paths) throws IOException {
        ArrayList<double[]> outputs = new ArrayList<>();
        ArrayList<double[]> inputs = new ArrayList<>();

        for (String path : paths) {
            Reader in = new FileReader(path);
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(in);

            for (CSVRecord row : records) {
                double[] output_row = new double[6];
                double[] input_row = new double[row.size() - 6];

                for (int i = 0; i < row.size(); ++i) {
                    if (i < 6)
                        output_row[i] = (Double.parseDouble(row.get(i)));
                    else
                        input_row[i - 6] = (Double.parseDouble(row.get(i)));
                }

                outputs.add(output_row);
                inputs.add(input_row);
            }
        }

        double[][] input_array = inputs.toArray(new double[0][0]);
        double[][] output_array = outputs.toArray(new double[0][0]);

        return new Data(input_array, output_array);
    }

    /**
     * Format sensor output into an array with the same parameters as the training data.
     */
    public double[] format_input(SensorModel sensors, boolean normalize_data) {
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

        if (normalize_data)
            normalize(input);

        return input;
    }

    public double[] format_q_input(SensorModel sensors, Action action, boolean normalize_data) {
        double[] input = sensors.getOpponentSensors();
        double[] prefix = {action.accelerate, action.brake, action.steering};

        Double[] out = Stream.concat(Arrays.stream(prefix).boxed(), Arrays.stream(input) .boxed())
                .map(x -> x / 200.0)
                .toArray(Double[]::new);
        double[] out_array = Arrays.stream(out).mapToDouble(Double::doubleValue).toArray();

        return out_array;
    }

    public void normalize(double[] input) {
        for (int i = 0; i < input.length; ++i) {
            input[i] -= means[i];
            input[i] /= stddevs[i];
        }
    }

    private double mean(double[][] data, int column) {
        double sum = 0;
        for (int i = 0; i < data.length; ++i) {
            sum += data[i][column];
        }

        return sum / data.length;
    }

    private double std_deviation(double[][] data, int column, double mean) {
        double sum = 0;
        for (int i = 0; i < data.length; ++i) {
            sum += Math.pow(data[i][column] - mean, 2);
        }

        return Math.sqrt(sum / data.length);
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
