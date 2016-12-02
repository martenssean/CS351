import java.util.concurrent.TimeUnit;

import desmoj.core.dist.BoolDistBernoulli;
import desmoj.core.dist.ContDistExponential;
import desmoj.core.simulator.Experiment;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.ProcessQueue;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.statistic.Count;

public class ClinicModel extends Model {

	protected ProcessQueue<Patient> patientWaitingForNurseQueue;
	protected ProcessQueue<Patient> patientWaitingForSpecialistQueue;
	protected ProcessQueue<Nurse> nurseIdleQueue;
	protected ProcessQueue<Specialist> specialistIdleQueue;
	
	protected final int numberOfRoomsForSpecialist = 4;
	protected final int numberOfNurses = 1;
	protected Count costIncuredByClinic;
	
	private ContDistExponential patientArrival8To10;
	private ContDistExponential patientArrival10To1;
	private ContDistExponential patientArrival1To4;
	private ContDistExponential patientArrival4To8;

	private ContDistExponential nurseServiceTime;
	private ContDistExponential specialistServiceTime;

	public ClinicModel(Model owner, String modelName, boolean showInReport, boolean showInTrace) {
		super(owner, modelName, showInReport, showInTrace);
	}

	public String description() {
		return "Process orientated description of the hospital clinic model. For details see project description in our paper";
	}

	public void doInitialSchedules() {
		Nurse nurse = new Nurse(this, "Nurse", true);
		//Nurse nurse2 = new Nurse(this,"Nurse2",true);
		Specialist specialist = new Specialist(this, "Specialist", true);
		nurse.activate();
		//nurse2.activate();
		specialist.activate();

		PatientGenerator generator = new PatientGenerator(this, "Patient Generator", false);
		generator.activate();
	}

	public void init() {
		patientArrival8To10 = new ContDistExponential(this, "Early Interarrival", 15, true, false);
		patientArrival10To1 = new ContDistExponential(this, "Afternoon Interarrival", 6, true, false);
		patientArrival1To4 = new ContDistExponential(this, "Late Afternoon", 6, true, false);
		patientArrival4To8 = new ContDistExponential(this, "Late", 9, true, false);
		nurseServiceTime = new ContDistExponential(this, "Service time for Nurses", 8, true, false);
		specialistServiceTime = new ContDistExponential(this, "Service time for Specialists", 25, true, false);

		patientWaitingForNurseQueue = new ProcessQueue<Patient>(this, "Patients Waiting for Nurse", true, true);
		patientWaitingForSpecialistQueue = new ProcessQueue<Patient>(this, "Patients Waiting for Specialist", true,
				true);
		nurseIdleQueue = new ProcessQueue<Nurse>(this, "Nurse Waiting for Patients", true, true);
		specialistIdleQueue = new ProcessQueue<Specialist>(this, "Specialist Waiting for Patients", true, true);
		
		costIncuredByClinic = new Count(this,"Cost incured by the Clinic", true, true);
	}

	public double getInterArrivalTime(double time) {
		if (time <= 120) {
			return patientArrival8To10.sample();
		} else if (time <= 300) {
			return patientArrival10To1.sample();
		} else if (time <= 480) {
			return patientArrival1To4.sample();
		} else {
			return patientArrival4To8.sample();
		}
	}

	public double getNurseServiceTime() {
		return nurseServiceTime.sample();
	}

	public double getSpecialistServiceTime() {
		return specialistServiceTime.sample();
	}

	/**
	 * If a patient arrives when there are already k patients in the waiting
	 * room (not including himself/herself), the arriving patient immediately
	 * leaves (i.e., balks) and instead seeks treatment at a nearby emergency
	 * care center with probability k=8 (fork= 1;2;3;:::;8)
	 **/
	public boolean doesPatientBalk(int numberOfPatientsInQueue) {
		if (numberOfPatientsInQueue >= 8) {
			return true;
		} else if (numberOfPatientsInQueue == 0) {
			return false;
		} else {
			BoolDistBernoulli doesBalk = new BoolDistBernoulli(this, "True = patient leaves, false = stays",
					(double) numberOfPatientsInQueue / 8, true, false);
			return doesBalk.sample();
		}
	}
	
	public boolean needsSpecialist(){
		BoolDistBernoulli needsSpecialist = new BoolDistBernoulli(this,"True = patient needs specialist, false = leaves", .4, true, false);
		return needsSpecialist.sample();
	}

	public static void main(String[] args) {
		// Added to get proper display
		Experiment.setEpsilon(TimeUnit.MINUTES); // Can use minutes here
		Experiment.setReferenceUnit(TimeUnit.MINUTES);

		// create model and experiment
		ClinicModel model = new ClinicModel(null, "Clinic Model", true, true);
		Experiment exp = new Experiment("ClinicProcessSimulation");

		// connect both
		model.connectToExperiment(exp);

		// set experiment parameters
		exp.setShowProgressBar(false); // display a progress bar (or not)
		// set end of simulation at 720 minutes, runs for 12 hours
		exp.stop(new TimeInstant(720, TimeUnit.MINUTES));
		// set the period of the trace and debug
		exp.tracePeriod(new TimeInstant(0, TimeUnit.MINUTES), new TimeInstant(720, TimeUnit.MINUTES));
		exp.debugPeriod(new TimeInstant(0, TimeUnit.MINUTES), new TimeInstant(720, TimeUnit.MINUTES));

		// start the experiment at simulation time 0.0
		exp.start();

		// --> now the simulation is running until it reaches its end criterion
		// ...
		// ...
		// <-- afterwards, the main thread returns here
		if(!(model.nurseIdleQueue.length() == model.numberOfNurses)){
			//if all nurses are not idle at the end of simulation you may have to send a patient to ER
			boolean lastPatientNeedSpecialist = model.needsSpecialist();
			if(lastPatientNeedSpecialist){
				model.costIncuredByClinic.update(500);
			}
		}
		while(!model.patientWaitingForNurseQueue.isEmpty()){
			System.out.println("another one for nurse");
			model.patientWaitingForNurseQueue.removeFirst();
			//remaining patients leave for ER
			model.costIncuredByClinic.update(500);
		}
		while(!model.patientWaitingForSpecialistQueue.isEmpty()){
			//treat the ones waiting for specialist
			model.patientWaitingForSpecialistQueue.removeFirst();
			model.costIncuredByClinic.update(200);
			System.out.println("another one for specialist");
		}
		// generate the report (and other output files)
		exp.report();

		// stop all threads still alive and close all output files
		exp.finish();
	}
}
