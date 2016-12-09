import java.util.concurrent.TimeUnit;

import co.paralleluniverse.fibers.SuspendExecution;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.SimProcess;
import desmoj.core.simulator.TimeSpan;

public class Specialist extends SimProcess {

	public Specialist(Model owner, String name, boolean showInTrace) {
		super(owner, name, showInTrace);
	}

	public void lifeCycle() throws SuspendExecution {
		ClinicModel model = (ClinicModel) this.getModel();
		while (true) {
			// check if something is waiting
			if (model.patientWaitingForSpecialistQueue.isEmpty()) {
				// nobody is waiting for specialist
				model.specialistIdleQueue.insert(this);
				passivate();
			} else {
				// patient waiting
				Patient patient = model.patientWaitingForSpecialistQueue.first();
				model.patientWaitingForSpecialistQueue.remove(patient);
				hold(new TimeSpan(model.getSpecialistServiceTime(), TimeUnit.MINUTES));
				model.costIncuredByClinic.update(200);
				// let patient continue its life cycle
				patient.activate();
			}
		}
	}

}
