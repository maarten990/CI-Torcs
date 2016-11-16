import org.encog.neural.networks.BasicNetwork;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EvolutionaryStuff {
    private double previous_best;

    public EvolutionaryStuff() {
    }

    public void evolve(String track) {
        DefaultDriverAlgorithm algorithm;
        BasicNetwork network = new DefaultDriver().neuralNetwork.network;

        // get the base time
        previous_best = runRace(track, network);
        System.out.printf("Base time: %f\n", previous_best);

        // repeat the entire thing a few times
        for (int i = 0; i < 10; ++i) {
            System.out.printf("-- Generation %d --\n", i + 1);

            // create 10 randomly mutated networks
            ArrayList<BasicNetwork> networks = new ArrayList<>();
            for (int j = 0; j < 10; ++j) {
                BasicNetwork new_network = (BasicNetwork) network.clone();
                EvolutionaryStuff.mutate(new_network);
                networks.add(new_network);
            }

            // record the racetime for each algorithm
            ArrayList<Double> times = new ArrayList<>();

            for (BasicNetwork n : networks) {
                double time = runRace(track, n);
                times.add(time);
                System.out.printf("%.2f ", time);
            }
            System.out.printf("\n");


            List<Integer> sorted = IntStream.range(0, networks.size())
                    .boxed()
                    .sorted((x, y) -> times.get(x).compareTo(times.get(y)))
                    .collect(Collectors.toList());

            BasicNetwork merged = IntStream.range(0, networks.size())
                    .boxed()
                    .filter(t -> times.get(t) < previous_best)
                    .map(networks::get)
                    .reduce(network, EvolutionaryStuff::combine);

            System.out.printf("Best time this generation: %f\n", times.get(sorted.get(0)));
            network = merged;
            previous_best = runRace(track, network);
            System.out.printf("Merged time: %f\n\n", previous_best);

            if (i == 9) {
                NeuralNetwork n = new NeuralNetwork(12);
                n.network = network;
                n.storeGenome();
            }
        }
    }

    public static double runRace(String track, BasicNetwork n) {
        // backup stdout
        PrintStream stdout = System.out;

        // redirect stdout
        try {
            System.setOut(new PrintStream(new File("/dev/null")));
        } catch (FileNotFoundException e) {
        }

        DefaultDriverAlgorithm algorithm = new DefaultDriverAlgorithm();
        algorithm.track = track;
        double time = algorithm.run_with_results(n);
        System.setOut(stdout);

        return time;
    }

    public static void mutate(BasicNetwork network) {
        Random rng = new Random();

        // mutate the hidden-to-output weights
        for (int from = 0; from < 12; ++from) {
            for (int to = 0; to < 3; ++to) {
                double old_value = network.getWeight(1, from, to);
                network.setWeight(1, from, to, old_value + (0.005 * rng.nextGaussian()));
            }
        }

        // mutate the input-to-hidden weights
        /*
        for (int from = 0; from < 22; ++from) {
            for (int to = 0; to < 12; ++to) {
                if (rng.nextDouble() < 0.10) {
                    double old_value = network.getWeight(0, from, to);
                    network.setWeight(1, from, to, old_value + (0.0001 * rng.nextGaussian()));
                }
            }
        }
        */
    }

    public static BasicNetwork combine(BasicNetwork a, BasicNetwork b) {
        BasicNetwork out = (BasicNetwork)a.clone();
        for (int from = 0; from < 12; ++from) {
            for (int to = 0; to < 3; ++to) {
                double a_value = a.getWeight(1, from, to);
                double b_value = b.getWeight(1, from, to);
                out.setWeight(1, from, to, (a_value + b_value) / 2);
            }
        }

        return out;
    }
}
