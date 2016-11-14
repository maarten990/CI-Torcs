import scr.Action;
import scr.SensorModel;

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

    public LoggingDriver() {
        super();

        sensor_log = new ArrayList<>();
        System.out.println("==== Logging output ====");
    }

    @Override
    public Action control(SensorModel sensors) {
        Action action = defaultControl(null, sensors);

        double[] inputs = DataModel.format_input(sensors);

        Double[] data = new Double[inputs.length + 3];
        data[0] = action.accelerate;
        data[1] = action.brake;
        data[2] = action.steering;

        for (int i = 0; i < inputs.length; ++i)
            data[i + 3] = inputs[i];

        sensor_log.add(data);
        return action;
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
