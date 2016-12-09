import java.util.concurrent.TimeUnit;

import co.paralleluniverse.fibers.SuspendExecution;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.SimProcess;
import desmoj.core.simulator.TimeSpan;

public class Nurse extends SimProcess{

	public Nurse(Model owner, String name, boolean showInTrace) {
		super(owner, name, showInTrace);
	}

	public void lifeCycle() throws SuspendExecution {
		ClinicModel model = (ClinicModel) this.getModel();
		while(true){
			//check if something is waiting
			if(model.patientWaitingForNurseQueue.isEmpty()){
				//nobody is waiting for nurse
				model.nurseIdleQueue.insert(this);
				passivate();
			}
			else{
				//patient waiting
				Patient patient = model.patientWaitingForNurseQueue.first();
				model.patientWaitingForNurseQueue.remove(patient);
				hold(new TimeSpan(model.getNurseServiceTime(),TimeUnit.MINUTES));
				model.costIncuredByClinic.update(100);
				//let patient continue its life cycle
				patient.activate();
			}
		}
	}

}
