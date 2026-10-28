package es.uvigo.dagss.recetas.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

/**
 * Refill plan rules: one box per refill; a box lasts (units per package / daily dose) days; the number of refills
 * is the treatment length divided by that, rounded up (minimum 1); each refill is valid from 7 days before to
 * 7 days after its exact date (none before the first one), and none outlives the prescription.
 */
@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock
    private PrescriptionDAO prescriptionDAO;
    @Mock
    private RefillDAO refillDAO;
    @Mock
    private PatientDAO patientDAO;
    @Mock
    private DoctorDAO doctorDAO;
    @Mock
    private MedicationDAO medicationDAO;

    private PrescriptionService service;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        service = new PrescriptionService(prescriptionDAO, refillDAO, patientDAO, doctorDAO, medicationDAO);
        today = LocalDate.now();
    }

    /** Stubs the three lookups of create() and the saves, for a medication with the given units per package. */
    private void givenPatientDoctorAndMedication(int unitsPerPackage) {
        Patient patient = new Patient();
        patient.setId(1L);
        Doctor doctor = new Doctor();
        doctor.setId(2L);
        Medication medication = new Medication("Test 600mg", "Test Active Ingredient", "Test Manufacturer",
                "Test Category", unitsPerPackage, true);

        when(patientDAO.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorDAO.findById(2L)).thenReturn(Optional.of(doctor));
        when(medicationDAO.findById(3L)).thenReturn(Optional.of(medication));
        // lenient: tests that expect the request to be rejected never reach the saves
        lenient().when(prescriptionDAO.save(any(Prescription.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(refillDAO.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private PrescriptionDTO create(double dailyDose, LocalDate endDate) {
        return service.create(new NewPrescriptionRequest(1L, 2L, 3L, dailyDose, "every 12 hours", endDate));
    }

    @Test
    void shouldGenerateOneRefillPerFifteenDaysOfTreatment() {
        // 30-unit package and 2 units/day -> one box lasts 15 days; 60-day treatment -> 4 boxes
        givenPatientDoctorAndMedication(30);
        LocalDate endDate = today.plusDays(60);

        PrescriptionDTO result = create(2.0, endDate);

        assertThat(result.endDate()).isEqualTo(endDate);
        assertThat(result.refills()).hasSize(4);
        assertThat(result.refills().get(0).boxes()).isEqualTo(1);
    }

    @Test
    void eachRefillIsValidFromSevenDaysBeforeToSevenDaysAfterItsExactDate() {
        givenPatientDoctorAndMedication(30);

        List<RefillDTO> plan = create(2.0, today.plusDays(60)).refills();

        // exact dates: day 0, 15, 30 and 45
        assertThat(plan).extracting(RefillDTO::validFrom)
                .containsExactly(today, today.plusDays(8), today.plusDays(23), today.plusDays(38));
        assertThat(plan).extracting(RefillDTO::validUntil)
                .containsExactly(today.plusDays(7), today.plusDays(22), today.plusDays(37), today.plusDays(52));
    }

    @Test
    void theFirstRefillHasNoMarginBeforeItsDate() {
        givenPatientDoctorAndMedication(30);

        RefillDTO first = create(2.0, today.plusDays(60)).refills().get(0);

        assertThat(first.validFrom()).isEqualTo(today);
    }

    @Test
    void noRefillOutlivesThePrescription() {
        // 50 days -> 4 boxes (3.33 rounded up); the last one would end on day 52 but is cut at day 50
        givenPatientDoctorAndMedication(30);
        LocalDate endDate = today.plusDays(50);

        List<RefillDTO> plan = create(2.0, endDate).refills();

        assertThat(plan).hasSize(4);
        assertThat(plan.get(3).validUntil()).isEqualTo(endDate);
        for (RefillDTO r : plan) {
            assertThat(r.validUntil()).isBeforeOrEqualTo(endDate);
        }
    }

    @Test
    void aTreatmentShorterThanOneBoxStillGetsOneRefill() {
        givenPatientDoctorAndMedication(30);
        LocalDate endDate = today.plusDays(5);

        List<RefillDTO> plan = create(2.0, endDate).refills();

        assertThat(plan).hasSize(1);
        assertThat(plan.get(0).validFrom()).isEqualTo(today);
        assertThat(plan.get(0).validUntil()).isEqualTo(endDate);
    }

    @Test
    void aTreatmentThatIsAnExactMultipleOfTheBoxDurationDoesNotGetAnExtraRefill() {
        // 30 days / 15 days per box = exactly 2 boxes
        givenPatientDoctorAndMedication(30);

        List<RefillDTO> plan = create(2.0, today.plusDays(30)).refills();

        assertThat(plan).hasSize(2);
    }

    @Test
    void aBoxThatLastsAFractionOfADayIsRoundedToTheNearestDayForTheExactDate() {
        // 28 units at 1.5/day -> a box lasts 18.67 days; 60 days -> ceil(3.21) = 4 boxes
        // exact dates: round(0), round(18.67), round(37.33), round(56) = day 0, 19, 37, 56
        givenPatientDoctorAndMedication(28);

        List<RefillDTO> plan = create(1.5, today.plusDays(60)).refills();

        assertThat(plan).hasSize(4);
        assertThat(plan).extracting(RefillDTO::validUntil)
                .containsExactly(today.plusDays(7), today.plusDays(26), today.plusDays(44), today.plusDays(60));
        assertThat(plan.get(3).validFrom()).isEqualTo(today.plusDays(49));
    }

    @Test
    void aHigherDoseNeedsMoreRefillsForTheSameTreatment() {
        // 30 units at 4/day -> 7.5 days per box; 30 days -> 4 boxes (instead of 2 at 2/day)
        givenPatientDoctorAndMedication(30);

        assertThat(create(4.0, today.plusDays(30)).refills()).hasSize(4);
    }

    @Test
    void everyRefillStartsScheduledWithOneBoxAndNoPharmacy() {
        givenPatientDoctorAndMedication(30);

        List<RefillDTO> plan = create(2.0, today.plusDays(60)).refills();

        for (RefillDTO r : plan) {
            assertThat(r.status()).isEqualTo(RefillStatus.SCHEDULED);
            assertThat(r.boxes()).isEqualTo(1);
            assertThat(r.pharmacyId()).isNull();
        }
    }

    @Test
    void thePlanIsPersistedTogetherWithThePrescription() {
        givenPatientDoctorAndMedication(30);

        create(2.0, today.plusDays(60));

        ArgumentCaptor<List<Refill>> saved = ArgumentCaptor.forClass(List.class);
        verify(refillDAO).saveAll(saved.capture());
        assertThat(saved.getValue()).hasSize(4);
        for (Refill r : saved.getValue()) {
            assertThat(r.getPrescription()).isNotNull();
        }
    }

    @Test
    void thePrescriptionStartsTodayAndIsActive() {
        givenPatientDoctorAndMedication(30);

        create(2.0, today.plusDays(60));

        ArgumentCaptor<Prescription> saved = ArgumentCaptor.forClass(Prescription.class);
        verify(prescriptionDAO).save(saved.capture());
        assertThat(saved.getValue().getStartDate()).isEqualTo(today);
        assertThat(saved.getValue().isActive()).isTrue();
        assertThat(saved.getValue().getDailyDose()).isEqualTo(2.0);
        assertThat(saved.getValue().getInstructions()).isEqualTo("every 12 hours");
    }

    @Test
    void anEndDateOfTodayOrEarlierIsRejectedAndNothingIsSaved() {
        givenPatientDoctorAndMedication(30);

        assertThatThrownBy(() -> create(2.0, today)).isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> create(2.0, today.minusDays(3))).isInstanceOf(InvalidOperationException.class);

        verify(prescriptionDAO, never()).save(any());
        verify(refillDAO, never()).saveAll(any());
    }

    @Test
    void aDailyDoseOfZeroIsRejectedWithoutSavingAnyRefill() {
        givenPatientDoctorAndMedication(30);

        assertThatThrownBy(() -> create(0, today.plusDays(10))).isInstanceOf(InvalidOperationException.class);

        verify(refillDAO, never()).saveAll(any());
    }

    @Test
    void aMedicationWithoutUnitsPerPackageIsRejectedWithoutSavingAnyRefill() {
        givenPatientDoctorAndMedication(0);

        assertThatThrownBy(() -> create(2.0, today.plusDays(10))).isInstanceOf(InvalidOperationException.class);

        verify(refillDAO, never()).saveAll(any());
    }

    @Test
    void anUnknownPatientIsReportedAsNotFound() {
        when(patientDAO.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> create(2.0, today.plusDays(10))).isInstanceOf(ResourceNotFoundException.class);

        verify(prescriptionDAO, never()).save(any());
    }

    @Test
    void anUnknownDoctorIsReportedAsNotFound() {
        when(patientDAO.findById(1L)).thenReturn(Optional.of(new Patient()));
        when(doctorDAO.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> create(2.0, today.plusDays(10))).isInstanceOf(ResourceNotFoundException.class);

        verify(prescriptionDAO, never()).save(any());
    }

    @Test
    void anUnknownMedicationIsReportedAsNotFound() {
        when(patientDAO.findById(1L)).thenReturn(Optional.of(new Patient()));
        when(doctorDAO.findById(2L)).thenReturn(Optional.of(new Doctor()));
        when(medicationDAO.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> create(2.0, today.plusDays(10))).isInstanceOf(ResourceNotFoundException.class);

        verify(prescriptionDAO, never()).save(any());
    }

    @Test
    void cancellingDeactivatesThePrescriptionAndCancelsOnlyThePendingRefills() {
        Prescription prescription = new Prescription();
        prescription.setId(7L);
        Refill pending1 = new Refill(prescription, today, today.plusDays(7), 1, RefillStatus.SCHEDULED, null);
        Refill alreadyFilled = new Refill(prescription, today, today.plusDays(7), 1, RefillStatus.FILLED, null);
        Refill pending2 = new Refill(prescription, today.plusDays(8), today.plusDays(22), 1, RefillStatus.SCHEDULED, null);
        when(prescriptionDAO.findById(7L)).thenReturn(Optional.of(prescription));
        when(refillDAO.findByPrescriptionId(7L)).thenReturn(List.of(pending1, alreadyFilled, pending2));

        service.cancel(7L);

        assertThat(prescription.isActive()).isFalse();
        verify(prescriptionDAO).save(prescription);
        assertThat(pending1.getStatus()).isEqualTo(RefillStatus.CANCELLED);
        assertThat(pending2.getStatus()).isEqualTo(RefillStatus.CANCELLED);
        assertThat(alreadyFilled.getStatus()).isEqualTo(RefillStatus.FILLED);
        verify(refillDAO).saveAll(List.of(pending1, alreadyFilled, pending2));
    }

    @Test
    void cancellingAnUnknownPrescriptionIsReportedAsNotFound() {
        when(prescriptionDAO.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(99L)).isInstanceOf(ResourceNotFoundException.class);

        verify(refillDAO, never()).saveAll(any());
    }

    @Test
    void activePrescriptionsComeWithTheirRefills() {
        Prescription prescription = new Prescription();
        prescription.setId(3L);
        prescription.setActive(true);
        Refill refill = new Refill(prescription, today, today.plusDays(7), 1, RefillStatus.SCHEDULED, null);
        when(prescriptionDAO.findByPatientIdAndActiveOrderByStartDateAsc(1L, true)).thenReturn(List.of(prescription));
        when(refillDAO.findByPrescriptionId(3L)).thenReturn(List.of(refill));

        List<PrescriptionDTO> result = service.activePrescriptionsForPatient(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(3L);
        assertThat(result.get(0).refills()).hasSize(1);
        assertThat(result.get(0).refills().get(0).status()).isEqualTo(RefillStatus.SCHEDULED);
    }
}
