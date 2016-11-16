import cicontest.algorithm.abstracts.AbstractRace;
import cicontest.algorithm.abstracts.DriversUtils;
import cicontest.torcs.controller.Driver;
import cicontest.torcs.controller.Human;
import cicontest.torcs.race.Race;
import cicontest.torcs.race.RaceResult;
import cicontest.torcs.race.RaceResults;
import scr.Controller;

import java.util.function.Supplier;

public class DefaultRace extends AbstractRace {

	public int[] runQualification(DefaultDriverGenome[] drivers, boolean withGUI){
		DefaultDriver[] driversList = new DefaultDriver[drivers.length + 1 ];
		for(int i=0; i<drivers.length; i++){
			driversList[i] = new DefaultDriver();
			driversList[i].loadGenome(drivers[i]);
		}
		return runQualification(driversList, withGUI);
	}

	
	public double[] runRace(DefaultDriverGenome[] drivers, boolean withGUI, Supplier<DefaultDriver> driver_factory){
		int size = Math.min(10, drivers.length);
		DefaultDriver[] driversList = new DefaultDriver[size];
		for(int i=0; i<size; i++){
			driversList[i] = driver_factory.get();
			driversList[i].loadGenome(drivers[i]);
		}

		return runRaceWithLaptime(driversList, withGUI, true);
	}

	public double[] runRaceWithLaptime(Driver[] drivers, boolean withGUI, boolean randomOrder) {
		double[] fitness = new double[drivers.length];
		if(drivers.length > 10) {
			throw new RuntimeException("Only 10 drivers are allowed in a RACE");
		} else {
			Race race = new Race();
			race.setTrack(this.tracktype, this.track);
			race.setTermination(Race.Termination.LAPS, this.laps);
			race.setStage(Controller.Stage.RACE);
			Driver[] results = drivers;
			int i = drivers.length;

			for(int var8 = 0; var8 < i; ++var8) {
				Driver driver = results[var8];
				race.addCompetitor(driver);
			}

			if(randomOrder) {
				race.shuffleOrder();
			}

			RaceResults var10;
			if(withGUI) {
				var10 = race.runWithGUI();
			} else {
				var10 = race.run();
			}

			for(i = 0; i < drivers.length; ++i) {
				fitness[i] = ((RaceResult)var10.get(drivers[i])).getBestLapTime();
			}

			this.printResults(drivers, var10);
			return fitness;
		}
	}

	public void showBest(){
		if(DriversUtils.getStoredGenome() == null ){
			System.err.println("No best-genome known");
			return;
		}
		
		DefaultDriverGenome best = (DefaultDriverGenome) DriversUtils.getStoredGenome();
		DefaultDriver driver = new DefaultDriver();
		driver.loadGenome(best);
		
		DefaultDriver[] driversList = new DefaultDriver[1];
		driversList[0] = driver;
		runQualification(driversList, true);
	}
	
	public void showBestRace(){
		if(DriversUtils.getStoredGenome() == null ){
			System.err.println("No best-genome known");
			return;
		}
	
		DefaultDriver[] driversList = new DefaultDriver[1];
		
		for(int i=0; i<10; i++){
			DefaultDriverGenome best = (DefaultDriverGenome) DriversUtils.getStoredGenome();
			DefaultDriver driver = new DefaultDriver();
			driver.loadGenome(best);
			driversList[i] = driver;
		}
		
		runRace(driversList, true, true);
	}
	
	public void raceBest(){
		
		if(DriversUtils.getStoredGenome() == null ){
			System.err.println("No best-genome known");
			return;
		}
		
		Driver[] driversList = new Driver[10];
		for(int i=0; i<10; i++){
			DefaultDriverGenome best = (DefaultDriverGenome) DriversUtils.getStoredGenome();
			DefaultDriver driver = new DefaultDriver();
			driver.loadGenome(best);
			driversList[i] = driver;
		}
		driversList[0] = new Human();
		runRace(driversList, true, true);
	}
}
