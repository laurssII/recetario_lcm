package es.uvigo.dagss.recetas.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.uvigo.dagss.recetas.daos.PharmacyDAO;
import es.uvigo.dagss.recetas.daos.RefillDAO;
import es.uvigo.dagss.recetas.dto.RefillDTO;
import es.uvigo.dagss.recetas.entities.Pharmacy;
import es.uvigo.dagss.recetas.entities.Refill;
import es.uvigo.dagss.recetas.entities.RefillStatus;
import es.uvigo.dagss.recetas.exceptions.InvalidOperationException;
import es.uvigo.dagss.recetas.exceptions.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class RefillServiceTest {

    @Mock
    private RefillDAO refillDAO;
    @Mock
    private PharmacyDAO pharmacyDAO;

    private RefillService service;
    private LocalDate today;
    private Pharmacy pharmacy;

    @BeforeEach
    void setUp() {
        service = new RefillService(refillDAO, pharmacyDAO);
        today = LocalDate.now();
        pharmacy = new Pharmacy();
        pharmacy.setId(3L);
    }

    private Refill refill(LocalDate validFrom, LocalDate validUntil, RefillStatus status) {
        Refill r = new Refill(null, validFrom, validUntil, 1, status, null);
        r.setId(9L);
        when(refillDAO.findById(9L)).thenReturn(Optional.of(r));
        return r;
    }

    private void givenPharmacyAndSave() {
        when(pharmacyDAO.findById(3L)).thenReturn(Optional.of(pharmacy));
        when(refillDAO.save(any(Refill.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void fillingAValidRefillMarksItFilledAndRecordsThePharmacy() {
        Refill r = refill(today.minusDays(2), today.plusDays(5), RefillStatus.SCHEDULED);
        givenPharmacyAndSave();

        RefillDTO result = service.fill(9L, 3L);

        assertThat(result.status()).isEqualTo(RefillStatus.FILLED);
        assertThat(result.pharmacyId()).isEqualTo(3L);
        assertThat(r.getStatus()).isEqualTo(RefillStatus.FILLED);
        assertThat(r.getPharmacy()).isSameAs(pharmacy);
        verify(refillDAO).save(r);
    }

    @Test
    void theFirstAndLastDayOfTheValidityWindowAreBothAccepted() {
        refill(today, today.plusDays(7), RefillStatus.SCHEDULED);
        givenPharmacyAndSave();
        assertThat(service.fill(9L, 3L).status()).isEqualTo(RefillStatus.FILLED);

        refill(today.minusDays(7), today, RefillStatus.SCHEDULED);
        assertThat(service.fill(9L, 3L).status()).isEqualTo(RefillStatus.FILLED);
    }

    @Test
    void aRefillThatIsNotValidYetIsRejected() {
        Refill r = refill(today.plusDays(1), today.plusDays(8), RefillStatus.SCHEDULED);

        assertThatThrownBy(() -> service.fill(9L, 3L)).isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("validity window");

        assertThat(r.getStatus()).isEqualTo(RefillStatus.SCHEDULED);
        verify(refillDAO, never()).save(any());
    }

    @Test
    void anExpiredRefillIsRejected() {
        Refill r = refill(today.minusDays(8), today.minusDays(1), RefillStatus.SCHEDULED);

        assertThatThrownBy(() -> service.fill(9L, 3L)).isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("validity window");

        assertThat(r.getStatus()).isEqualTo(RefillStatus.SCHEDULED);
        verify(refillDAO, never()).save(any());
    }

    @Test
    void aRefillThatWasAlreadyFilledCannotBeFilledTwice() {
        refill(today.minusDays(2), today.plusDays(5), RefillStatus.FILLED);

        assertThatThrownBy(() -> service.fill(9L, 3L)).isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("SCHEDULED");

        verify(refillDAO, never()).save(any());
    }

    @Test
    void aCancelledRefillCannotBeFilled() {
        refill(today.minusDays(2), today.plusDays(5), RefillStatus.CANCELLED);

        assertThatThrownBy(() -> service.fill(9L, 3L)).isInstanceOf(InvalidOperationException.class);

        verify(refillDAO, never()).save(any());
    }

    @Test
    void fillingAnUnknownRefillIsReportedAsNotFound() {
        when(refillDAO.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.fill(9L, 3L)).isInstanceOf(ResourceNotFoundException.class);

        verify(refillDAO, never()).save(any());
    }

    @Test
    void fillingWithAnUnknownPharmacyIsReportedAsNotFoundAndLeavesTheRefillUntouched() {
        Refill r = refill(today.minusDays(2), today.plusDays(5), RefillStatus.SCHEDULED);
        when(pharmacyDAO.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.fill(9L, 3L)).isInstanceOf(ResourceNotFoundException.class);

        assertThat(r.getStatus()).isEqualTo(RefillStatus.SCHEDULED);
        assertThat(r.getPharmacy()).isNull();
        verify(refillDAO, never()).save(any());
    }

    @Test
    void thePharmacyLookupIgnoresCaseAndSurroundingSpacesInTheHealthCardNumber() {
        Refill r = new Refill(null, today, today.plusDays(7), 1, RefillStatus.SCHEDULED, null);
        when(refillDAO.findByPrescriptionPatientHealthCardNumberAndStatusOrderByValidFromAsc(
                "RIXO8504120001", RefillStatus.SCHEDULED)).thenReturn(List.of(r));

        List<RefillDTO> result = service.pendingByHealthCardNumber("  rixo8504120001 ");

        assertThat(result).hasSize(1);
    }

    @Test
    void aPatientsPendingRefillsKeepTheOrderTheRepositoryGivesThem() {
        Refill soon = new Refill(null, today, today.plusDays(7), 1, RefillStatus.SCHEDULED, null);
        Refill later = new Refill(null, today.plusDays(8), today.plusDays(22), 1, RefillStatus.SCHEDULED, null);
        when(refillDAO.findByPrescriptionPatientIdAndStatusOrderByValidFromAsc(5L, RefillStatus.SCHEDULED))
                .thenReturn(List.of(soon, later));

        List<RefillDTO> result = service.pendingForPatient(5L);

        assertThat(result).extracting(RefillDTO::validFrom).containsExactly(today, today.plusDays(8));
    }
}
