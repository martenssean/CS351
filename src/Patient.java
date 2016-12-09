import co.paralleluniverse.fibers.SuspendExecution;
import desmoj.core.dist.BoolDistBernoulli;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.SimProcess;

public class Patient extends SimProcess {

	protected double arrivalTime;

	/**
	 * The patients lifecycle.
	 */
	public Patient(Model owner, String name, boolean showInTrace) {
		super(owner, name, showInTrace);
	}

	public void lifeCycle() throws SuspendExecution {
		ClinicModel model = (ClinicModel) this.getModel();
		this.arrivalTime = model.presentTime().getTimeAsDouble();
		model.sendTraceNote("Patient is arriving in System");
		// k/8 chance of leaving for k patients in the queue already.
		boolean doesBalk = model.doesPatientBalk(model.patientWaitingForNurseQueue.length());
		if (doesBalk == true) {
			//patient leaves the system and costs clinic big money.
			model.costIncuredByClinic.update(500);
		}
		else{
			//they stay and enter the system.
			model.patientWaitingForNurseQueue.insert(this);
			if(!model.nurseIdleQueue.isEmpty()){
				//get nurse for service, and remove nurse from idle queue
				Nurse nurse = model.nurseIdleQueue.first();
				model.nurseIdleQueue.remove(nurse);
				nurse.activate();
			}
			this.passivate();
			
			//after service by nurse
			boolean needsSpecialist = model.needsSpecialist();
			if(needsSpecialist == true){
				//patient leaves if he/she has spent 30 minutes in the system before getting sent to specialist
				double timeInSystem = model.presentTime().getTimeAsDouble() - this.arrivalTime;
				//patient also leaves if no service rooms are available
				if(timeInSystem < 30 && model.patientWaitingForSpecialistQueue.length() < model.numberOfRoomsForSpecialist){
					//send patient to specialist
					model.patientWaitingForSpecialistQueue.insert(this);
					if(!model.specialistIdleQueue.isEmpty()){
						//get specialist for service, and remove specialist from idle queue.
						Specialist specialist = model.specialistIdleQueue.first();
						model.specialistIdleQueue.remove(specialist);
						specialist.activate();
					}
				}
				else{
					//the patient needed a specialist but spent too much time in system already or not enough rooms.
					//cost clinic money
					model.costIncuredByClinic.update(500);
				}
			}
			//we are done.
		}
		this.passivate();
		model.sendTraceNote("Patient leaves the system");
	}

}
