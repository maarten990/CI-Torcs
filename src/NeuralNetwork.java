import org.encog.engine.network.activation.ActivationLinear;
import org.encog.engine.network.activation.ActivationTANH;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataPair;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.Propagation;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class NeuralNetwork implements Serializable {

    private static final long serialVersionUID = -88L;
    public int history;

    public DataModel road_model;
    public DataModel dirt_model;

    public BasicNetwork road_network;
    public BasicNetwork dirt_network;
    public BasicNetwork q_network;
    public BasicNetwork dirt_q;

    public double[] acc_offsets = {-0.5, 0, 0.5};

    /**
     * hidden: the number of nodes to use in the hidden layer
     * epochs: the number of epochs to train for
     * history: number of previous steps to take into account
     */
    NeuralNetwork(int hidden, int history) {
        this.history = history;

        road_network = create_network(hidden);
        dirt_network = create_network(hidden);

        q_network = create_q_network();
        dirt_q = create_q_network();

        road_model = new DataModel();
        dirt_model = new DataModel();
    }

    private BasicNetwork create_network(int hidden) {
        BasicNetwork network = new BasicNetwork();
        network.addLayer(new BasicLayer(null, false, 22 + (history * 22)));
        network.addLayer(new BasicLayer(new ActivationTANH(), true, hidden));
        network.addLayer(new BasicLayer(new ActivationTANH(), true, 3));
        network.getStructure().finalizeStructure();
        network.reset(); // initializes the weights randomly

        return network;
    }

    private BasicNetwork create_q_network() {
        BasicNetwork network = new BasicNetwork();
        // 3 inputs for the control network output, 36 inputs for the opponent sensors
        network.addLayer(new BasicLayer(null, false, 22));
        network.addLayer(new BasicLayer(new ActivationLinear(), true, 12));
        network.addLayer(new BasicLayer(new ActivationLinear(), true, this.acc_offsets.length));

        network.getStructure().finalizeStructure();
        network.reset(); // initializes the weights randomly

        return network;
    }

    public void train(int epochs, String training_folder, String dirt_folder) {
        System.out.println("Training road");
        train(epochs, training_folder, road_network, road_model);
        System.out.println("Training dirt");
        train(epochs, dirt_folder, dirt_network, dirt_model);
    }

    public void train(int epochs, String training_folder, BasicNetwork network, DataModel model) {
        // get all the csv files in the training directory
        List<String> filenames = new ArrayList<>();
        try {
            Files.list(Paths.get(training_folder))
                    .map(String::valueOf)
                    .filter(path -> path.endsWith(".csv"))
                    .forEach(path -> filenames.add(path));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        System.out.printf("Training on %s\n", filenames);

        MLDataSet dataset = new BasicMLDataSet();

        // load all the training files into the dataset
        try {
            Data data = model.load_data(filenames);
            for (int i = 0; i < data.X.length; ++i) {
                data.Y[i][0] = clamp(data.Y[i][0], 0, 1);
                data.Y[i][1] = clamp(data.Y[i][1], 0, 1);
                data.Y[i][2] = clamp(data.Y[i][2], -1, 1);

                if (i < history + 1)
                    continue;

                double[] new_x = new double[network.getInputCount()];
                // fill the input array
                ArrayList<Double> input_list = new ArrayList<>();
                for (int h = history; h >= 0; --h) {
                    double[] t_minus_h = data.X[i - h];
                    for (double x : t_minus_h) {
                        input_list.add(x);
                    }
                }

                for (int j = 0; j < new_x.length; ++j) {
                    new_x[j] = input_list.get(j);
                }

                dataset.add(new BasicMLDataPair(new BasicMLData(new_x),
                            new BasicMLData(data.Y[i])));
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        // training loop
        Propagation train = new ResilientPropagation(network, dataset);
        for (int epoch = 1; epoch < epochs; ++epoch) {
            train.iteration();

            if (epoch % 100 == 0)
                System.out.printf("Epoch #%d: Error %f\n", epoch, train.getError());
        }

        train.finishTraining();
    }

    public void retrain_q_road(int epochs) {
        retrain_q(epochs, q_network, "q_data");
    }

    public void retrain_q_dirt(int epochs) {
        retrain_q(epochs, dirt_q, "q_dirt");
    }

    private void retrain_q(int epochs, BasicNetwork network, String data_folder) {
        // get all the csv files in the training directory
        List<String> filenames = new ArrayList<>();
        try {
            Files.list(Paths.get(data_folder))
                    .map(String::valueOf)
                    .filter(path -> path.endsWith(".csv"))
                    .forEach(path -> filenames.add(path));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        System.out.printf("Training on %s\n", filenames);

        MLDataSet dataset = new BasicMLDataSet();

        // load all the training files into the dataset
        try {
            Data data = road_model.load_q_data(filenames);
            for (int i = 0; i < data.X.length; ++i) {
                dataset.add(new BasicMLDataPair(new BasicMLData(data.X[i]),
                        new BasicMLData(data.Y[i])));
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        // training loop
        Propagation train = new ResilientPropagation(network, dataset);
        for (int epoch = 1; epoch < epochs; ++epoch) {
            train.iteration();

            if (epoch % 100 == 0)
                System.out.printf("Epoch #%d: Error %f\n", epoch, train.getError());
        }

        train.finishTraining();
    }

    public double clamp(double value, double min, double max) {
        if (value < min)
            return min;
        else if (value > max)
            return max;
        else
            return value;
    }

    public double[] getRoadOutput(List<double[]> histories) {
        return getOutput(histories, road_network);
    }

    public double[] getDirtOutput(List<double[]> histories) {
        return getOutput(histories, dirt_network);
    }

    public double[] getQOutput(double[] input) {
        double[] output = new double[q_network.getOutputCount()];
        q_network.compute(input, output);

        return output;
    }

    public double[] getQDirtOutput(double[] input) {
        double[] output = new double[dirt_q.getOutputCount()];
        dirt_q.compute(input, output);

        return output;
    }

    /**
     * Output is an array of the form [acceleration, brake, steering]
     */
    public double[] getOutput(List<double[]> histories, BasicNetwork network) {
        ArrayList<Double> input_list = new ArrayList<>();

        for (double[] h : histories) {
            for (double element : h) {
                input_list.add(element);
            }
        }

        double[] input = new double[input_list.size()];
        for (int i = 0; i < input.length; ++i)
            input[i] = input_list.get(i);

        double[] output = new double[3];
        network.compute(input, output);

        return output;
    }

    //Store the state of this neural network
    public void storeGenome() {
        ObjectOutputStream out = null;
        try {
            //create the memory folder manually
            out = new ObjectOutputStream(new FileOutputStream("memory/mydriver.mem"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (out != null) {
                out.writeObject(this);
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load a neural network from memory
    public static NeuralNetwork loadGenome() {

        // Read from disk using FileInputStream
        InputStream f_in = null;
        try {
            f_in = new FileInputStream("memory/mydriver.mem");
        } catch (IOException e) {
            // load from inside a jar
            f_in = NeuralNetwork.class.getResourceAsStream("/memory/mydriver.mem");
        }

        // Read object using ObjectInputStream
        ObjectInputStream obj_in = null;
        try {
            obj_in = new ObjectInputStream(f_in);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Read an object
        try {
            if (obj_in != null) {
                return (NeuralNetwork) obj_in.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
