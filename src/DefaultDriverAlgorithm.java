import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import cicontest.algorithm.abstracts.AbstractAlgorithm;
import cicontest.algorithm.abstracts.DriversUtils;
import cicontest.torcs.controller.Driver;
import org.encog.neural.networks.BasicNetwork;
import race.TorcsConfiguration;

public class DefaultDriverAlgorithm extends AbstractAlgorithm {

    private static final long serialVersionUID = 654963126362653L;

    DefaultDriverGenome[] drivers = new DefaultDriverGenome[1];
    private double[] results = new double[1];
    private boolean use_logging = false;
    private boolean human = false;
    private boolean with_gui = true;
    String track = "aalborg";
    private String tracktype = "road";

    public Class<? extends Driver> getDriverClass() {
        return DefaultDriver.class;
    }

    public double run_with_results(BasicNetwork network) {
        DefaultDriverGenome genome = new DefaultDriverGenome();
        drivers[0] = genome;

        //Start a race
        DefaultRace race = new DefaultRace();
        race.setTrack(track, tracktype);
        race.laps = 1;

        Supplier<DefaultDriver> supplier = () -> {
            DefaultDriver d = new DefaultDriver();
            d.neuralNetwork.road_network = network;

            return d;
        };

        results = race.runRace(drivers, with_gui, supplier);

        return results[0];
    }

    // disables output prints and returns the laptime
    public double[] run_with_results(int n_drivers, double epsilon) {
        // backup stdout
        PrintStream stdout = System.out;

        // redirect stdout
        try {
            System.setOut(new PrintStream(new File("/dev/null")));
        } catch (FileNotFoundException e) {
        }

        drivers = new DefaultDriverGenome[n_drivers];
        for (int i = 0; i < n_drivers; ++i) {
            DefaultDriverGenome genome = new DefaultDriverGenome();
            drivers[i] = genome;
        }

        //Start a race
        DefaultRace race = new DefaultRace();
        race.setTrack(track, tracktype);
        race.laps = 1;

        Supplier<DefaultDriver> driver_factory;
        if (use_logging)
            driver_factory = () -> new LoggingDriver(human);
        else
            driver_factory = () -> {DefaultDriver d = new DefaultDriver(); d.epsilon = epsilon; return d;};

        results = race.runRace(drivers, with_gui, driver_factory);

        // restore stdout
        System.setOut(stdout);

        return results;
    }

    public void run(boolean continue_from_checkpoint) {
        if (!continue_from_checkpoint) {
            //init NN
            DefaultDriverGenome genome = new DefaultDriverGenome();
            drivers[0] = genome;

            //Start a race
            DefaultRace race = new DefaultRace();
            race.setTrack(track, tracktype);
            race.laps = 1;

            // create the appropriate driver factory so we can feed it into the racing function
            Supplier<DefaultDriver> driver_factory;
            if (use_logging)
                driver_factory = () -> new LoggingDriver(human);
            else
                driver_factory = () -> new DefaultDriver();


            results = race.runRace(drivers, with_gui, driver_factory);

            // Save genome/nn
            DriversUtils.storeGenome(drivers[0]);
        }
        // create a checkpoint this allows you to continue this run later
        DriversUtils.createCheckpoint(this);
        //DriversUtils.clearCheckpoint();
    }

    public static void main(String[] args) {

        //Set path to torcs.properties
        TorcsConfiguration.getInstance().initialize(new File("torcs.properties"));
        /*
		 *
		 * Start without arguments to run the algorithm
		 * Start with -continue to continue a previous run
		 * Start with -show to show the best found
		 * Start with -show-race to show a race with 10 copies of the best found
		 * Start with -human to race against the best found
		 *
		 */
        DefaultDriverAlgorithm algorithm = new DefaultDriverAlgorithm();
        DriversUtils.registerMemory(algorithm.getDriverClass());
        if (args.length > 0 && args[0].equals("-show")) {
            new DefaultRace().showBest();
        } else if (args.length > 0 && args[0].equals("-show-race")) {
            new DefaultRace().showBestRace();
        } else if (args.length > 0 && args[0].equals("-human")) {
            new DefaultRace().raceBest();
        } else if (args.length > 0 && args[0].equals("-log")) {
            run_all_tracks(true, false, 1, false);
        } else if (args.length > 0 && args[0].equals("-test")) {
            run_all_tracks(false, false, 1, false);
        } else if (args.length > 0 && args[0].equals("-evolve")) {
            evolve();
        } else if (args.length > 0 && args[0].equals("-qlearning")) {
            run_all_tracks(false, false, 1, true);
        } else if (args.length > 0 && args[0].equals("-continue")) {
            if (DriversUtils.hasCheckpoint()) {
                DriversUtils.loadCheckpoint().run(true);
            } else {
                algorithm.run();
            }
        } else {
            algorithm.run();
        }
    }

    public static void run_all_tracks(boolean use_logging, boolean with_gui, int n_drivers, boolean qlearn) {
        // uncomment to train a new network
        //new DefaultDriver().train(0, 16, 2000);
        DefaultDriverAlgorithm algorithm = new DefaultDriverAlgorithm();
        algorithm.use_logging = use_logging;
        algorithm.with_gui = with_gui;
        algorithm.human = false;

        String[] road_tracks = {"aalborg", "corkscrew", "brondehach", "alpine-1", "alpine-2", "forza", "ruudskogen"};
        String[] dirt_tracks = {"dirt-1", "dirt-2", "mixed-1", "mixed-2"};

        double epsilon = qlearn ? 1.0 : 0.0;
        for (int i = 0; i < (qlearn ? 30 : 1); ++i) {
            if (qlearn) {
                try {
                    for (Path file : Files.list(Paths.get("q_data")).collect(Collectors.toList()))
                        Files.delete(file);
                    for (Path file : Files.list(Paths.get("q_dirt")).collect(Collectors.toList()))
                        Files.delete(file);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }

                epsilon = Math.max(0.1, epsilon - 0.05);
            }

            System.out.printf("Epsilon: %f\n", epsilon);

            for (String track : road_tracks) {
                algorithm.track = track;
                algorithm.tracktype = "road";
                double[] laptimes = algorithm.run_with_results(n_drivers, epsilon);
                System.out.printf("%s: %s\n", track,
                        Arrays.stream(laptimes).boxed()
                                .map(x -> String.format("%.2f", x))
                                .collect(Collectors.joining(", ")));
            }

            for (String track : dirt_tracks) {
                algorithm.track = track;
                algorithm.tracktype = "dirt";
                double[] laptimes = algorithm.run_with_results(n_drivers, epsilon);
                System.out.printf("%s: %s\n", track,
                        Arrays.stream(laptimes).boxed()
                                .map(x -> String.format("%.2f", x))
                                .collect(Collectors.joining(", ")));
            }

            if (qlearn) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }

                DefaultDriver d = new DefaultDriver();
                d.neuralNetwork.retrain_q_road(500);
                d.neuralNetwork.retrain_q_dirt(500);
                d.neuralNetwork.storeGenome();
            }

        }
    }

    public static void evolve() {
        EvolutionaryStuff e = new EvolutionaryStuff();
        e.evolve("aalborg");
    }
}