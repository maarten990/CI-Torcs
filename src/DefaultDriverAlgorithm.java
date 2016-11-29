import java.io.File;
import java.util.function.Supplier;

import cicontest.algorithm.abstracts.AbstractAlgorithm;
import cicontest.algorithm.abstracts.DriversUtils;
import cicontest.torcs.controller.Driver;
import org.encog.neural.networks.BasicNetwork;
import race.TorcsConfiguration;

public class DefaultDriverAlgorithm extends AbstractAlgorithm {

    private static final long serialVersionUID = 654963126362653L;

    DefaultDriverGenome[] drivers = new DefaultDriverGenome[1];
    double[] results = new double[1];
    boolean use_logging = false;
    boolean human = false;
    boolean with_gui = true;
    String track = "aalborg";
    String tracktype = "road";

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

        results = race.runRace(drivers, false, supplier);

        return results[0];
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
            run_all_tracks(true, false);
        } else if (args.length > 0 && args[0].equals("-test")) {
            run_all_tracks(false, false);
        } else if (args.length > 0 && args[0].equals("-evolve")) {
            evolve();
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

    public static void run_all_tracks(boolean use_logging, boolean with_gui) {
        DefaultDriverAlgorithm algorithm = new DefaultDriverAlgorithm();
        algorithm.use_logging = use_logging;
        algorithm.with_gui = with_gui;
        algorithm.human = false;

        String[] road_tracks = {"aalborg", "corkscrew", "brondehach", "alpine-1", "alpine-2", "forza", "ruudskogen"};
        String[] dirt_tracks = {"dirt-1", "dirt-2", "mixed-1", "mixed-2"};

        for (String track : road_tracks) {
            System.out.println(track);
            algorithm.track = track;
            algorithm.run();
        }

        for (String track : dirt_tracks) {
            System.out.println(track);
            algorithm.track = track;
            algorithm.tracktype = "dirt";
            algorithm.run();
        }
    }

    public static void evolve() {
        EvolutionaryStuff e = new EvolutionaryStuff();
        e.evolve("aalborg");
    }
}