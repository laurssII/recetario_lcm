package es.uvigo.dagss.recetas.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.uvigo.dagss.recetas.daos.AppointmentDAO;
import es.uvigo.dagss.recetas.daos.PatientDAO;
import es.uvigo.dagss.recetas.dto.AppointmentDTO;
import es.uvigo.dagss.recetas.dto.AvailableSlotDTO;
import es.uvigo.dagss.recetas.dto.NewAppointmentRequest;
import es.uvigo.dagss.recetas.entities.Appointment;
import es.uvigo.dagss.recetas.entities.AppointmentStatus;
import es.uvigo.dagss.recetas.entities.Doctor;
import es.uvigo.dagss.recetas.entities.Patient;
import es.uvigo.dagss.recetas.exceptions.InvalidOperationException;
import es.uvigo.dagss.recetas.exceptions.ResourceNotFoundException;

/** Office hours are 8:30-15:30 in 15-minute slots, so a day has 28 slots (the last one starts at 15:15). */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    private static final LocalDate DAY = LocalDate.of(2030, 3, 4);

    @Mock
    private AppointmentDAO appointmentDAO;
    @Mock
    private PatientDAO patientDAO;

    private AppointmentService service;
    private Doctor doctor;
    private Patient patient;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(appointmentDAO, patientDAO);
        doctor = new Doctor();
        doctor.setId(10L);
        doctor.setName("Laura");
        doctor.setLastName("Fernandez");
        patient = new Patient();
        patient.setId(5L);
        patient.setName("Xoan");
        patient.setLastName("Rodriguez");
        patient.setDoctor(doctor);
    }

    private void givenPatientIsFound() {
        when(patientDAO.findById(5L)).thenReturn(Optional.of(patient));
    }

    private void givenBookedSlots(LocalDate day, LocalDateTime... slots) {
        List<Appointment> booked = java.util.Arrays.stream(slots)
                .map(s -> new Appointment(patient, doctor, s, AppointmentStatus.SCHEDULED)).toList();
        when(appointmentDAO.findByDoctorIdAndStatusAndDateTimeBetween(
                10L, AppointmentStatus.SCHEDULED, day.atTime(8, 30), day.atTime(15, 30))).thenReturn(booked);
    }

    private AppointmentDTO book(LocalDateTime dateTime) {
        return service.create(new NewAppointmentRequest(5L, dateTime));
    }

    private Appointment anAppointment(AppointmentStatus status) {
        Appointment a = new Appointment(patient, doctor, DAY.atTime(9, 0), status);
        a.setId(77L);
        when(appointmentDAO.findById(77L)).thenReturn(Optional.of(a));
        when(appointmentDAO.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        return a;
    }

    @Test
    void aFreeDayHasTwentyEightSlotsFromHalfPastEightToQuarterPastThree() {
        givenPatientIsFound();
        givenBookedSlots(DAY);

        List<AvailableSlotDTO> slots = service.availableSlots(5L, DAY);

        assertThat(slots).hasSize(28);
        assertThat(slots.get(0).start()).isEqualTo(DAY.atTime(8, 30));
        assertThat(slots.get(1).start()).isEqualTo(DAY.atTime(8, 45));
        assertThat(slots.get(27).start()).isEqualTo(DAY.atTime(15, 15));
        assertThat(slots).extracting(AvailableSlotDTO::start).doesNotContain(DAY.atTime(15, 30), DAY.atTime(8, 15));
    }

    @Test
    void aBookedSlotIsNotOffered() {
        givenPatientIsFound();
        givenBookedSlots(DAY, DAY.atTime(9, 0), DAY.atTime(12, 15));

        List<AvailableSlotDTO> slots = service.availableSlots(5L, DAY);

        assertThat(slots).hasSize(26);
        assertThat(slots).extracting(AvailableSlotDTO::start)
                .doesNotContain(DAY.atTime(9, 0), DAY.atTime(12, 15))
                .contains(DAY.atTime(8, 45), DAY.atTime(9, 15));
    }

    @Test
    void slotsAreTakenFromTheScheduleOfThePatientsOwnDoctor() {
        givenPatientIsFound();
        givenBookedSlots(DAY);

        service.availableSlots(5L, DAY);

        verify(appointmentDAO).findByDoctorIdAndStatusAndDateTimeBetween(
                10L, AppointmentStatus.SCHEDULED, DAY.atTime(8, 30), DAY.atTime(15, 30));
    }

    @Test
    void aPatientWithoutDoctorCannotSeeSlots() {
        patient.setDoctor(null);
        givenPatientIsFound();

        assertThatThrownBy(() -> service.availableSlots(5L, DAY)).isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void slotsForAnUnknownPatientAreReportedAsNotFound() {
        when(patientDAO.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.availableSlots(5L, DAY)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void bookingAFreeSlotCreatesAScheduledAppointmentWithThePatientsDoctor() {
        givenPatientIsFound();
        LocalDateTime slot = DAY.atTime(9, 0);
        when(appointmentDAO.findByDoctorIdAndStatusAndDateTimeBetween(
                10L, AppointmentStatus.SCHEDULED, slot, slot.plusMinutes(1))).thenReturn(List.of());
        when(appointmentDAO.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentDTO result = book(slot);

        assertThat(result.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(result.dateTime()).isEqualTo(slot);
        assertThat(result.patientId()).isEqualTo(5L);
        assertThat(result.doctorId()).isEqualTo(10L);
        assertThat(result.duration()).isEqualTo(15);
        ArgumentCaptor<Appointment> saved = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentDAO).save(saved.capture());
        assertThat(saved.getValue().getDoctor()).isSameAs(doctor);
    }

    @Test
    void bookingASlotThatIsAlreadyTakenIsRejectedAndNothingIsSaved() {
        givenPatientIsFound();
        LocalDateTime slot = DAY.atTime(9, 0);
        Appointment other = new Appointment(new Patient(), doctor, slot, AppointmentStatus.SCHEDULED);
        when(appointmentDAO.findByDoctorIdAndStatusAndDateTimeBetween(
                10L, AppointmentStatus.SCHEDULED, slot, slot.plusMinutes(1))).thenReturn(List.of(other));

        assertThatThrownBy(() -> book(slot)).isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("no longer available");

        verify(appointmentDAO, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"08:30", "08:45", "12:00", "15:15"})
    void firstLastAndInnerSlotsOfTheDayAreAccepted(String time) {
        givenPatientIsFound();
        when(appointmentDAO.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentDTO result = book(DAY.atTime(LocalTime.parse(time)));

        assertThat(result.dateTime().toLocalTime()).isEqualTo(LocalTime.parse(time));
    }

    @ParameterizedTest
    @ValueSource(strings = {"00:00", "07:00", "08:15", "09:10", "10:01", "15:30", "15:45", "16:00", "23:45"})
    void timesOutsideOfficeHoursOrOffTheQuarterHourAreRejected(String time) {
        givenPatientIsFound();

        assertThatThrownBy(() -> book(DAY.atTime(LocalTime.parse(time)))).isInstanceOf(InvalidOperationException.class);

        verify(appointmentDAO, never()).save(any());
    }

    @Test
    void aPatientWithoutDoctorCannotBook() {
        patient.setDoctor(null);
        givenPatientIsFound();

        assertThatThrownBy(() -> book(DAY.atTime(9, 0))).isInstanceOf(InvalidOperationException.class);

        verify(appointmentDAO, never()).save(any());
    }

    @Test
    void bookingForAnUnknownPatientIsReportedAsNotFound() {
        when(patientDAO.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> book(DAY.atTime(9, 0))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancellingSetsTheStatusToCancelled() {
        Appointment a = anAppointment(AppointmentStatus.SCHEDULED);

        AppointmentDTO result = service.cancel(77L);

        assertThat(result.status()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(appointmentDAO).save(a);
    }

    @Test
    void markingNoShowSetsTheStatusToNoShow() {
        Appointment a = anAppointment(AppointmentStatus.SCHEDULED);

        assertThat(service.markNoShow(77L).status()).isEqualTo(AppointmentStatus.NO_SHOW);
        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.NO_SHOW);
    }

    @Test
    void markingCompletedSetsTheStatusToCompleted() {
        Appointment a = anAppointment(AppointmentStatus.SCHEDULED);

        assertThat(service.markCompleted(77L).status()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void statusChangesOnAnUnknownAppointmentAreReportedAsNotFound() {
        when(appointmentDAO.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(99L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.markNoShow(99L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.markCompleted(99L)).isInstanceOf(ResourceNotFoundException.class);

        verify(appointmentDAO, never()).save(any());
    }

    @Test
    void theDoctorsAgendaCoversTheWholeRequestedDay() {
        Appointment a = new Appointment(patient, doctor, DAY.atTime(9, 0), AppointmentStatus.SCHEDULED);
        when(appointmentDAO.findByDoctorIdAndDateTimeBetweenOrderByDateTimeAsc(
                10L, DAY.atStartOfDay(), DAY.plusDays(1).atStartOfDay())).thenReturn(List.of(a));

        List<AppointmentDTO> agenda = service.scheduleForDay(10L, DAY);

        assertThat(agenda).hasSize(1);
        assertThat(agenda.get(0).patientName()).isEqualTo("Xoan Rodriguez");
        assertThat(agenda.get(0).doctorName()).isEqualTo("Laura Fernandez");
    }

    @Test
    void theDoctorsAgendaDefaultsToToday() {
        LocalDate before = LocalDate.now();

        service.scheduleForDay(10L, null);

        LocalDate after = LocalDate.now();
        ArgumentCaptor<LocalDateTime> from = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> to = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(appointmentDAO).findByDoctorIdAndDateTimeBetweenOrderByDateTimeAsc(
                org.mockito.ArgumentMatchers.eq(10L), from.capture(), to.capture());
        assertThat(from.getValue().toLocalDate()).isBetween(before, after);
        assertThat(from.getValue().toLocalTime()).isEqualTo(LocalTime.MIDNIGHT);
        assertThat(to.getValue()).isEqualTo(from.getValue().plusDays(1));
    }

    @Test
    void aPatientSeesOnlyTheirScheduledAppointments() {
        Appointment a = new Appointment(patient, doctor, DAY.atTime(9, 0), AppointmentStatus.SCHEDULED);
        when(appointmentDAO.findByPatientIdAndStatusOrderByDateTimeAsc(5L, AppointmentStatus.SCHEDULED))
                .thenReturn(List.of(a));

        assertThat(service.scheduledForPatient(5L)).hasSize(1);
    }

    @Test
    void adminBrowsingUsesTheFilterThatMatchesTheGivenParameters() {
        LocalDateTime from = DAY.atStartOfDay();
        LocalDateTime to = DAY.plusDays(1).atStartOfDay();

        service.browseForAdmin(DAY, null, null);
        service.browseForAdmin(DAY, 10L, null);
        service.browseForAdmin(DAY, null, 5L);
        service.browseForAdmin(DAY, 10L, 5L);

        verify(appointmentDAO).findByDateTimeBetweenOrderByDateTimeAsc(from, to);
        verify(appointmentDAO).findByDateTimeBetweenAndDoctorIdOrderByDateTimeAsc(from, to, 10L);
        verify(appointmentDAO).findByDateTimeBetweenAndPatientIdOrderByDateTimeAsc(from, to, 5L);
        verify(appointmentDAO).findByDateTimeBetweenAndDoctorIdAndPatientIdOrderByDateTimeAsc(from, to, 10L, 5L);
    }
}
