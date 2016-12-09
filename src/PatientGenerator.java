import java.util.concurrent.TimeUnit;

import co.paralleluniverse.fibers.SuspendExecution;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.SimProcess;
import desmoj.core.simulator.TimeSpan;

public class PatientGenerator extends SimProcess {

	public PatientGenerator(Model owner, String name, boolean showInTrace) {
		super(owner, name, showInTrace);
	}

	//Generates patients for the duration of the simulation. 8am to 8pm (0 minutes to 720 minutes).
	public void lifeCycle() throws SuspendExecution {
		ClinicModel model = (ClinicModel) this.getModel();
		while(true){
			int interarrivalTime = (int) model.getInterArrivalTime(model.presentTime().getTimeAsDouble());
			//wait for interarrival
			this.hold(new TimeSpan(interarrivalTime,TimeUnit.MINUTES));
			//create new patient and activate him/her
			Patient patient = new Patient(model,"Customer",true);
			patient.activate();
		}
	}

}
