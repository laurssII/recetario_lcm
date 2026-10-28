package es.uvigo.dagss.recetas.daos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.beans.factory.annotation.Autowired;

import es.uvigo.dagss.recetas.entities.Address;
import es.uvigo.dagss.recetas.entities.Doctor;
import es.uvigo.dagss.recetas.entities.HealthCenter;
import es.uvigo.dagss.recetas.entities.Patient;

@DataJpaTest
class PatientDAOTest {

    @Autowired
    private PatientDAO patientDAO;

    @Autowired
    private DoctorDAO doctorDAO;

    @Autowired
    private HealthCenterDAO healthCenterDAO;

    @Test
    void shouldFindPatientsByApproximateName() {
        HealthCenter center = healthCenterDAO.save(new HealthCenter("Test Health Center",
                new Address("123 Fake Street", "Vigo", "36200", "Pontevedra"), "986000000", "center@test.example"));
        Doctor doctor = doctorDAO.save(new Doctor("Smith", "11111111A", "LIC-1", "986000001", center,
                "msmith", "LIC-1", "Mary", "msmith@test.example"));

        patientDAO.save(new Patient("Garcia", "22222222B", "HC-1", "SSN-1",
                new Address("1 Real Street", "Vigo", "36201", "Pontevedra"), "986111111",
                LocalDate.of(1990, 1, 1), center, doctor, "jgarcia", "22222222B", "John", "jgarcia@test.example"));
        patientDAO.save(new Patient("Soto", "33333333C", "HC-2", "SSN-2",
                new Address("2 New Street", "Vigo", "36201", "Pontevedra"), "986222222",
                LocalDate.of(1992, 2, 2), center, doctor, "asoto", "33333333C", "Anna", "asoto@test.example"));

        List<Patient> result = patientDAO.findByNameContaining("oh");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("John");
    }
}
