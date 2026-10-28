package es.uvigo.dagss.recetas.services;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.uvigo.dagss.recetas.daos.DoctorDAO;
import es.uvigo.dagss.recetas.daos.MedicationDAO;
import es.uvigo.dagss.recetas.daos.PatientDAO;
import es.uvigo.dagss.recetas.daos.PrescriptionDAO;
import es.uvigo.dagss.recetas.daos.RefillDAO;
import es.uvigo.dagss.recetas.dto.NewPrescriptionRequest;
import es.uvigo.dagss.recetas.dto.PrescriptionDTO;
import es.uvigo.dagss.recetas.dto.RefillDTO;
import es.uvigo.dagss.recetas.entities.Doctor;
import es.uvigo.dagss.recetas.entities.Medication;
import es.uvigo.dagss.recetas.entities.Patient;
import es.uvigo.dagss.recetas.entities.Prescription;
import es.uvigo.dagss.recetas.entities.Refill;
import es.uvigo.dagss.recetas.entities.RefillStatus;
import es.uvigo.dagss.recetas.exceptions.InvalidOperationException;
import es.uvigo.dagss.recetas.exceptions.ResourceNotFoundException;

@Service
@Transactional
public class PrescriptionService {

    /** Margin, in days, before and after the exact date of each refill. */
    private static final long MARGIN_DAYS = 7;

    private final PrescriptionDAO prescriptionDAO;
    private final RefillDAO refillDAO;
    private final PatientDAO patientDAO;
    private final DoctorDAO doctorDAO;
    private final MedicationDAO medicationDAO;

    public PrescriptionService(PrescriptionDAO prescriptionDAO, RefillDAO refillDAO, PatientDAO patientDAO,
                                DoctorDAO doctorDAO, MedicationDAO medicationDAO) {
        this.prescriptionDAO = prescriptionDAO;
        this.refillDAO = refillDAO;
        this.patientDAO = patientDAO;
        this.doctorDAO = doctorDAO;
        this.medicationDAO = medicationDAO;
    }

    @Transactional(readOnly = true)
    public List<PrescriptionDTO> activePrescriptionsForPatient(Long patientId) {
        return prescriptionDAO.findByPatientIdAndActiveOrderByStartDateAsc(patientId, true)
                .stream()
                .map(p -> PrescriptionDTO.from(p, refillsOf(p.getId())))
                .toList();
    }

    public PrescriptionDTO create(NewPrescriptionRequest request) {
        Patient patient = patientDAO.findById(request.patientId())
                .orElseThrow(() -> ResourceNotFoundException.of("Patient", request.patientId()));
        Doctor doctor = doctorDAO.findById(request.doctorId())
                .orElseThrow(() -> ResourceNotFoundException.of("Doctor", request.doctorId()));
        Medication medication = medicationDAO.findById(request.medicationId())
                .orElseThrow(() -> ResourceNotFoundException.of("Medication", request.medicationId()));

        LocalDate startDate = LocalDate.now();
        if (!request.endDate().isAfter(startDate)) {
            throw new InvalidOperationException("The end date must be after the start date (today)");
        }

        Prescription prescription = new Prescription(medication, patient, doctor, request.dailyDose(),
                request.instructions(), startDate, request.endDate(), true);
        prescription = prescriptionDAO.save(prescription);

        List<Refill> plan = generateRefillPlan(prescription);
        refillDAO.saveAll(plan);

        return PrescriptionDTO.from(prescription, plan.stream().map(RefillDTO::from).toList());
    }

    /** Cancels the prescription and, as a logical cascade, the refills of its plan that are still SCHEDULED. */
    public void cancel(Long prescriptionId) {
        Prescription prescription = prescriptionDAO.findById(prescriptionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Prescription", prescriptionId));
        prescription.setActive(false);
        prescriptionDAO.save(prescription);

        List<Refill> refills = refillDAO.findByPrescriptionId(prescriptionId);
        for (Refill refill : refills) {
            if (refill.getStatus() == RefillStatus.SCHEDULED) {
                refill.setStatus(RefillStatus.CANCELLED);
            }
        }
        refillDAO.saveAll(refills);
    }

    /**
     * Computes the refill plan. Each refill dispenses one box; a box lasts (units per package / daily dose)
     * days, and the duration of the prescription is split into as many boxes as needed. Each refill is valid from
     * a week before to a week after the exact date on which its box is needed (no margin before the first one).
     */
    private List<Refill> generateRefillPlan(Prescription prescription) {
        double dailyDose = prescription.getDailyDose();
        int unitsPerPackage = prescription.getMedication().getUnitsPerPackage();
        if (dailyDose <= 0 || unitsPerPackage <= 0) {
            throw new InvalidOperationException("The daily dose and the medication's units per package must be greater than 0");
        }

        long durationDays = ChronoUnit.DAYS.between(prescription.getStartDate(), prescription.getEndDate());
        double daysPerBox = unitsPerPackage / dailyDose;
        int boxCount = (int) Math.ceil(durationDays / daysPerBox);
        boxCount = Math.max(boxCount, 1);

        List<Refill> plan = new ArrayList<>();
        for (int i = 0; i < boxCount; i++) {
            LocalDate exactDate = prescription.getStartDate().plusDays(Math.round(daysPerBox * i));

            LocalDate validFrom = (i == 0)
                    ? prescription.getStartDate() // no "before" margin on the first refill
                    : exactDate.minusDays(MARGIN_DAYS);
            LocalDate validUntil = exactDate.plusDays(MARGIN_DAYS);
            if (validUntil.isAfter(prescription.getEndDate())) {
                validUntil = prescription.getEndDate();
            }

            plan.add(new Refill(prescription, validFrom, validUntil, 1, RefillStatus.SCHEDULED, null));
        }
        return plan;
    }

    private List<RefillDTO> refillsOf(Long prescriptionId) {
        return refillDAO.findByPrescriptionId(prescriptionId).stream().map(RefillDTO::from).toList();
    }
}
