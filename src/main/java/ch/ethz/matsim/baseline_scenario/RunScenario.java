package ch.ethz.matsim.baseline_scenario;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunScenario {
	static public void main(String[] args) {
		Config config = ConfigUtils.loadConfig(args[0]);

		config.global().setNumberOfThreads(Integer.parseInt(args[1]));
		config.qsim().setNumberOfThreads(Integer.parseInt(args[2]));

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new BaselineModule());

		controler.run();
	}
}
