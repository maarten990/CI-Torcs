import org.encog.engine.network.activation.ActivationTANH;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataPair;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.Propagation;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
import scr.SensorModel;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class NeuralNetwork implements Serializable {

    private static final long serialVersionUID = -88L;
    private BasicNetwork network;

    /**
     * hidden: the number of nodes to use in the hidden layer
     * epochs: the number of epochs to train for
     */
    NeuralNetwork(int hidden) {
        network = new BasicNetwork();
        network.addLayer(new BasicLayer(null, false, 22));
        network.addLayer(new BasicLayer(new ActivationTANH(), true, hidden));
        network.addLayer(new BasicLayer(new ActivationTANH(), true, 3));
        network.getStructure().finalizeStructure();
        network.reset(); // initializes the weights randomly
    }

    public void train(int epochs) {
        // get all the csv files in the training directory
        List<String> filenames = new ArrayList<>();
        try {
            Files.list(Paths.get("train_data"))
                    .map(String::valueOf)
                    .filter(path -> path.endsWith(".csv"))
                    .forEach(path -> filenames.add(path));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        MLDataSet dataset = new BasicMLDataSet();

        // load all the training files into the dataset
        for (String filename : filenames) {
            System.out.printf("Adding %s to training data\n", filename);
            try {
                Data data = DataModel.load_data(filename);
                for (int i = 0; i < data.X.length; ++i) {
                    data.Y[i][0] = clamp(data.Y[i][0], 0, 1);
                    data.Y[i][1] = clamp(data.Y[i][1], 0, 1);
                    data.Y[i][2] = clamp(data.Y[i][2], -1, 1);

                    dataset.add(new BasicMLDataPair(new BasicMLData(data.X[i]),
                                new BasicMLData(data.Y[i])));
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }

        // training loop
        Propagation train = new ResilientPropagation(network, dataset);
        for (int epoch = 1; epoch < epochs; ++epoch) {
            train.iteration();
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

    /**
     * Output is an array of the form [acceleration, brake, steering]
     */
    public double[] getOutput(SensorModel a) {
        double[] input = DataModel.format_input(a);
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
