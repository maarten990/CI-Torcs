import cicontest.algorithm.abstracts.AbstractDriver;
import cicontest.algorithm.abstracts.DriversUtils;
import cicontest.torcs.controller.extras.ABS;
import cicontest.torcs.controller.extras.AutomatedClutch;
import cicontest.torcs.controller.extras.AutomatedGearbox;
import cicontest.torcs.controller.extras.AutomatedRecovering;
import cicontest.torcs.genome.IGenome;
import scr.Action;
import scr.SensorModel;

public class DefaultDriver extends AbstractDriver {

    private NeuralNetwork neuralNetwork;
    private double lastRightTrackEdge;
    private double lastLeftTrackEdge;


    public DefaultDriver() {
        initialize();

        // uncomment to train a new network
        //neuralNetwork = new NeuralNetwork(12, 1000);
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
        double[] output = neuralNetwork.getOutput(sensors);
        return output[0];
    }

    @Override
    public double getSteering(SensorModel sensors) {
        double[] output = neuralNetwork.getOutput(sensors);
        return output[2];
    }

    public Action getActionFromNetwork(SensorModel sensors) {
        double[] output = neuralNetwork.getOutput(sensors);
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

        return action;
    }
}