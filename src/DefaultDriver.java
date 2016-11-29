import cicontest.algorithm.abstracts.AbstractDriver;
import cicontest.algorithm.abstracts.DriversUtils;
import cicontest.torcs.controller.extras.ABS;
import cicontest.torcs.controller.extras.AutomatedClutch;
import cicontest.torcs.controller.extras.AutomatedGearbox;
import cicontest.torcs.controller.extras.AutomatedRecovering;
import cicontest.torcs.genome.IGenome;
import scr.Action;
import scr.SensorModel;

import java.util.ArrayList;
import java.util.List;

public class DefaultDriver extends AbstractDriver {

    public NeuralNetwork neuralNetwork;
    private double lastRightTrackEdge;
    private double lastLeftTrackEdge;
    private List history;
    public int n_history = 1;


    public DefaultDriver() {
        initialize();

        history = new ArrayList();

        int input_size = 22 + (22*n_history);
        int hidden_layer = (input_size + 3) / 2;

        // uncomment to train a new network
        //neuralNetwork = new NeuralNetwork(hidden_layer, n_history);
        //System.out.printf("Input size: %d, hidden layer size: %d\n", neuralNetwork.network.getInputCount(),
        //        neuralNetwork.network.getLayerNeuronCount(1));
        //neuralNetwork.train(1000, "train_data", "dirt_data");
        //neuralNetwork.storeGenome();

        // uncomment to load a previously saved network
        neuralNetwork = NeuralNetwork.loadGenome();
    }

    private void initialize() {
        this.enableExtras(new AutomatedClutch());
        this.enableExtras(new AutomatedGearbox());
        this.enableExtras(new AutomatedRecovering());
        this.enableExtras(new ABS());
    }

    @Override
    public void loadGenome(IGenome genome) {
        if (genome instanceof DefaultDriverGenome) {
            DefaultDriverGenome myGenome = (DefaultDriverGenome) genome;
        } else {
            System.err.println("Invalid Genome assigned");
        }
    }

    @Override
    public double getAcceleration(SensorModel sensors) {
        return 0;
    }

    @Override
    public double getSteering(SensorModel sensors) {
        return 0;
    }

    public Action getActionFromNetwork(SensorModel sensors) {
        if (history.size() < n_history + 1) {
            history.add(neuralNetwork.model.format_input(sensors, true));
            return new Action();
        }
        double[] input = neuralNetwork.model.format_input(sensors, true);

        double[] output;
        if (getTrackName().toLowerCase().contains("dirt") || getTrackName().toLowerCase().contains("mixed"))
            output = neuralNetwork.getDirtOutput(history);
        else
            output = neuralNetwork.getRoadOutput(history);

        // update the history
        history.add(input);
        history.remove(0);

        Action action = new Action();

        action.accelerate = output[0];
        action.brake = output[1];
        action.steering = output[2];

        return action;
    }

    @Override
    public String getDriverName() {
        return "The Venga Bus";
    }

    @Override
    public Action control(SensorModel sensors) {
        Action action = getActionFromNetwork(sensors);

        // recovery if the car stalls
        if (sensors.getSpeed() < 20){
            action.accelerate = 1;
            action.brake = 0;
        }

        return action;
    }

    @Override
    public Action controlWarmUp(SensorModel sensors) {
        return control(sensors);
    }

    @Override
    public Action controlQualification(SensorModel sensors) {
        return control(sensors);
    }

    @Override
    public Action controlRace(SensorModel sensors) {
        return control(sensors);
    }

    @Override
    public Action defaultControl(Action action, SensorModel sensors) {
        if (action == null) {
            action = new Action();
        }

        double currentLeftTrackEdge;
        double currentRightTrackEdge;

        if (sensors.getTrackEdgeSensors()[6] != -1.0) {
            lastLeftTrackEdge = sensors.getTrackEdgeSensors()[6];
        }
        currentLeftTrackEdge = lastLeftTrackEdge;

        if (sensors.getTrackEdgeSensors()[12] != -1.0) {
            lastRightTrackEdge = sensors.getTrackEdgeSensors()[12];
        }
        currentRightTrackEdge = lastRightTrackEdge;

        double limit = Math.max(70, -65 + Math.sqrt(Math.max(0.0, sensors.getTrackEdgeSensors()[9])) * 30);
        double delta = sensors.getSpeed() - limit;

        if (delta > 0) {
            action.accelerate = 0.0;
            action.brake = 5 * delta / (limit + 2);
        }
        else if (delta < 0) {
            action.accelerate = -5 * delta / (limit + 2);
            action.brake = 0.0D;
        }

        //action.steering = DriversUtils.alignToTrackAxis(sensors, 0.1);

        action.steering = ((currentLeftTrackEdge * currentLeftTrackEdge - currentRightTrackEdge * currentRightTrackEdge) / 100);

        if (getTrackName().toLowerCase().contains("dirt") || getTrackName().toLowerCase().contains("mixed")) {
            if (sensors.getSpeed() > 80) {
                action.accelerate = 0;
            }
        }

        return action;
    }
}