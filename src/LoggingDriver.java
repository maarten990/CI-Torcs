import scr.Action;
import scr.SensorModel;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LoggingDriver extends DefaultDriver {
    public ArrayList<Double[]> sensor_log;
    private JFrame frame;
    private Action keylog_action;

    public LoggingDriver() {
        super();

        keylog_action = new Action();

        frame = new JFrame("Keylogger");
        frame.setBounds(50, 100, 300, 300);
        frame.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {

            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
                //System.out.printf("Pressed %d\n", keyEvent.getKeyCode());
                switch(keyEvent.getKeyCode()) {
                    case 37:
                        if (keylog_action.steering < 1)
                            keylog_action.steering += 0.5;
                        break;
                    case 39:
                        if (keylog_action.steering > -1)
                            keylog_action.steering  -= 0.5;
                        break;
                    case 38:
                        keylog_action.accelerate = 1;
                        break;
                    case 40:
                        keylog_action.brake = 1;
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {
                switch(keyEvent.getKeyCode()) {
                    case 37:
                    case 39:
                        keylog_action.steering = 0;
                        break;
                    case 38:
                    case 40:
                        keylog_action.accelerate = 0;
                        keylog_action.brake = 0;
                        break;
                }
            }
        });
        frame.setVisible(true);

        sensor_log = new ArrayList<>();
        System.out.println("==== Logging output ====");
    }

    @Override
    public Action control(SensorModel sensors) {
        Action action = defaultControl(null, sensors);
        //Action action = manualControl(sensors);

        double[] inputs = neuralNetwork.model.format_input(sensors, false);

        Double[] data = new Double[inputs.length + 3];
        data[0] = action.accelerate;
        data[1] = action.brake;
        data[2] = action.steering;

        for (int i = 0; i < inputs.length; ++i)
            data[i + 3] = inputs[i];

        sensor_log.add(data);
        return action;
    }

    public Action manualControl(SensorModel sensors) {
        return keylog_action;
    }

    @Override
    public void exit() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Blah blah first line that gets skipped (☞ﾟヮﾟ)☞");

        // convert each row of data to a comma separated string
        for (Double[] row : sensor_log) {
            String line = Arrays.stream(row)
                    .map(d -> d.toString())
                    .collect(Collectors.joining(","));

            lines.add(line);
        }

        String path = "train_data/" + getTrackName() + LocalDateTime.now().toString() + ".csv";
        try {
            Files.write(Paths.get(path), lines);
            System.out.printf("Wrote output to %s\n", path);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
