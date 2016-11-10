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

    @Override
    public String getDriverName() {
        return "The Venga Bus";
    }

    @Override
    public Action control(SensorModel sensors) {
        Action action = new Action();

        action.accelerate = getAcceleration(sensors);
        action.steering = getSteering(sensors);

        // stop accelerating if we're in a decent turn
        if (sensors.getSpeed() > 30 && Math.abs(action.steering) > 0.4)
            action.accelerate = 0;

        // what kind of madman would drive more than 100 km/h
        if (sensors.getSpeed() > 100)
            action.accelerate = 0;

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
        action.steering = DriversUtils.alignToTrackAxis(sensors, 0.5);
        if (sensors.getSpeed() > 60.0D) {
            action.accelerate = 0.0D;
            action.brake = 0.0D;
        }

        if (sensors.getSpeed() > 70.0D) {
            action.accelerate = 0.0D;
            action.brake = -1.0D;
        }

        if (sensors.getSpeed() <= 60.0D) {
            action.accelerate = (80.0D - sensors.getSpeed()) / 80.0D;
            action.brake = 0.0D;
        }

        if (sensors.getSpeed() < 30.0D) {
            action.accelerate = 1.0D;
            action.brake = 0.0D;
        }
        System.out.println("--------------" + getDriverName() + "--------------");
        System.out.println("Steering: " + action.steering);
        System.out.println("Acceleration: " + action.accelerate);
        System.out.println("Brake: " + action.brake);
        System.out.println("-----------------------------------------------");
        return action;
    }
}