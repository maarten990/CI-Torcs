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


public class NeuralNetwork implements Serializable {

    private static final long serialVersionUID = -88L;
    private BasicNetwork network;

    /**
     * hidden: the number of nodes to use in the hidden layer
     * epochs: the number of epochs to train for
     */
    NeuralNetwork(int hidden, int epochs) {
        network = new BasicNetwork();
        network.addLayer(new BasicLayer(null, false, 22));
        network.addLayer(new BasicLayer(new ActivationTANH(), true, hidden));
        network.addLayer(new BasicLayer(new ActivationTANH(), true, 3));
        network.getStructure().finalizeStructure();
        network.reset(); // initializes the weights randomly

        String[] filenames = {
                "./train_data/aalborg.csv",
                "./train_data/alpine-1.csv",
                "./train_data/f-speedway.csv"
        };

        MLDataSet dataset = new BasicMLDataSet();

        // load all the training files into the dataset
        for (String filename : filenames) {
            try {
                Data data = DataModel.load_data(filename);
                for (int i = 0; i < data.X.length; ++i) {
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
        FileInputStream f_in = null;
        try {
            f_in = new FileInputStream("memory/mydriver.mem");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
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
