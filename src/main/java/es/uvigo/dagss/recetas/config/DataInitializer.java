package es.uvigo.dagss.recetas.config;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import es.uvigo.dagss.recetas.daos.PatientDAO;
import es.uvigo.dagss.recetas.dto.*;
import es.uvigo.dagss.recetas.services.*;

/**
 * Seeds the database with sample data. It runs only if the patients table is empty, so it is safe on both
 * profiles: h2 (in-memory, recreated on every start) and mysql (seeds the first time only).
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final PatientDAO patientDAO;
    private final HealthCenterService healthCenterService;
    private final DoctorService doctorService;
    private final PatientService patientService;
    private final PharmacyService pharmacyService;
    private final MedicationService medicationService;
    private final AdministratorService administratorService;
    private final AppointmentService appointmentService;
    private final PrescriptionService prescriptionService;

    public DataInitializer(PatientDAO patientDAO, HealthCenterService healthCenterService,
                            DoctorService doctorService, PatientService patientService,
                            PharmacyService pharmacyService, MedicationService medicationService,
                            AdministratorService administratorService, AppointmentService appointmentService,
                            PrescriptionService prescriptionService) {
        this.patientDAO = patientDAO;
        this.healthCenterService = healthCenterService;
        this.doctorService = doctorService;
        this.patientService = patientService;
        this.pharmacyService = pharmacyService;
        this.medicationService = medicationService;
        this.administratorService = administratorService;
        this.appointmentService = appointmentService;
        this.prescriptionService = prescriptionService;
    }

    @Override
    public void run(String... args) {
        if (patientDAO.count() > 0) {
            log.info("Data already present in the database, skipping sample data load.");
            return;
        }
        log.info("Loading initial sample data...");

        administratorService.create(new AdministratorDTO(null, "Main Admin", "admin@prescriptions.local", "admin", true, null, null), "admin");

        var downtownCenter = healthCenterService.create(new HealthCenterDTO(null, "Downtown Health Center",
                new AddressDTO("1 Hospital Street", "Vigo", "36201", "Pontevedra"), "986000001", "downtown@healthsystem.example", true));
        var coiaCenter = healthCenterService.create(new HealthCenterDTO(null, "Coia Health Center",
                new AddressDTO("45 Coia Avenue", "Vigo", "36209", "Pontevedra"), "986000002", "coia@healthsystem.example", true));

        var drFernandez = doctorService.create(new DoctorDTO(null, "Laura", "Fernandez Perez", "10000001S", "LIC-4501",
                "986100001", "laura.fernandez@healthsystem.example", downtownCenter.id(), null, "lfernandez", true));
        var drIglesias = doctorService.create(new DoctorDTO(null, "Carlos", "Iglesias Novoa", "10000002Q", "LIC-4502",
                "986100002", "carlos.iglesias@healthsystem.example", downtownCenter.id(), null, "ciglesias", true));
        var drVazquez = doctorService.create(new DoctorDTO(null, "Marta", "Vazquez Lorenzo", "10000003V", "LIC-4503",
                "986100003", "marta.vazquez@healthsystem.example", coiaCenter.id(), null, "mvazquez", true));

        var patientXoan = patientService.create(patient("Xoan", "Rodriguez Iglesias", "20000001Y", "RIXO8504120001",
                "20 Principe Street", "Vigo", "36202", "986200001", LocalDate.of(1985, 4, 12), downtownCenter.id(), drFernandez.id(), "xrodriguez"));
        var patientAna = patientService.create(patient("Ana", "Blanco Estevez", "20000002F", "BEAN7809030002",
                "55 Urzaiz Street", "Vigo", "36204", "986200002", LocalDate.of(1978, 9, 3), downtownCenter.id(), drFernandez.id(), "ablanco"));
        var patientDiego = patientService.create(patient("Diego", "Pardo Caamano", "20000003P", "PCDI9001220003",
                "8 Vigo Crossing", "Vigo", "36206", "986200003", LocalDate.of(1990, 1, 22), downtownCenter.id(), drIglesias.id(), "dpardo"));
        var patientSofia = patientService.create(patient("Sofia", "Dominguez Real", "20000004D", "DRSO6606300004",
                "12 Areal Street", "Vigo", "36201", "986200004", LocalDate.of(1966, 6, 30), downtownCenter.id(), drIglesias.id(), "sdominguez"));
        var patientLucia = patientService.create(patient("Lucia", "Otero Vidal", "20000005X", "OVLU9511080005",
                "12 Coia Avenue", "Vigo", "36209", "986200005", LocalDate.of(1995, 11, 8), coiaCenter.id(), drVazquez.id(), "lotero"));
        var patientManuel = patientService.create(patient("Manuel", "Castro Barros", "20000006B", "CBMA5802170006",
                "3 Marques de Valladares Street", "Vigo", "36201", "986200006", LocalDate.of(1958, 2, 17), coiaCenter.id(), drVazquez.id(), "mcastro"));

        var compostelaPharmacy = pharmacyService.create(pharmacy("Compostela Pharmacy", "Marta", "Lopez Souto",
                "30000001C", "PLIC-778899", "5 Compostela Square", "Vigo", "36201", "986333444", "compostela"));
        pharmacyService.create(pharmacy("Principe Pharmacy", "Ivan", "Suarez Mera",
                "30000002K", "PLIC-556677", "33 Principe Street", "Vigo", "36202", "986555666", "principe"));

        var ibuprofen = medicationService.create(new MedicationDTO(null, "Ibuprofen Cinfa 600mg", "Ibuprofen",
                "Cinfa Laboratories", "Anti-inflammatories", 30, true));
        var paracetamol = medicationService.create(new MedicationDTO(null, "Paracetamol Normon 1g", "Paracetamol",
                "Normon Laboratories", "Analgesics", 20, true));
        var omeprazole = medicationService.create(new MedicationDTO(null, "Omeprazole Cinfa 20mg", "Omeprazole",
                "Cinfa Laboratories", "Gastric protectors", 28, true));
        var enalapril = medicationService.create(new MedicationDTO(null, "Enalapril Normon 20mg", "Enalapril",
                "Normon Laboratories", "Antihypertensives", 30, true));
        medicationService.create(new MedicationDTO(null, "Simvastatin Cinfa 20mg", "Simvastatin",
                "Cinfa Laboratories", "Lipid-lowering agents", 28, true));

        LocalDateTime yesterday9 = LocalDate.now().minusDays(1).atTime(9, 0);
        LocalDateTime dayBeforeYesterday10 = LocalDate.now().minusDays(2).atTime(10, 0);

        var completedAppointment = appointmentService.create(new NewAppointmentRequest(patientXoan.id(), yesterday9));
        appointmentService.markCompleted(completedAppointment.id());

        var noShowAppointment = appointmentService.create(new NewAppointmentRequest(patientDiego.id(), dayBeforeYesterday10));
        appointmentService.markNoShow(noShowAppointment.id());

        appointmentService.create(new NewAppointmentRequest(patientAna.id(), LocalDate.now().plusDays(1).atTime(9, 30)));
        appointmentService.create(new NewAppointmentRequest(patientSofia.id(), LocalDate.now().plusDays(1).atTime(10, 0)));
        appointmentService.create(new NewAppointmentRequest(patientLucia.id(), LocalDate.now().plusDays(2).atTime(11, 0)));
        appointmentService.create(new NewAppointmentRequest(patientManuel.id(), LocalDate.now().plusDays(2).atTime(11, 15)));

        prescriptionService.create(new NewPrescriptionRequest(patientXoan.id(), drFernandez.id(), ibuprofen.id(),
                2.0, "1 tablet every 12 hours, after meals", LocalDate.now().plusMonths(2)));
        prescriptionService.create(new NewPrescriptionRequest(patientAna.id(), drFernandez.id(), omeprazole.id(),
                1.0, "1 tablet before breakfast, on an empty stomach", LocalDate.now().plusMonths(3)));
        prescriptionService.create(new NewPrescriptionRequest(patientDiego.id(), drIglesias.id(), paracetamol.id(),
                3.0, "1 tablet every 8 hours if in pain", LocalDate.now().plusDays(20)));
        prescriptionService.create(new NewPrescriptionRequest(patientSofia.id(), drIglesias.id(), enalapril.id(),
                1.0, "1 tablet with breakfast", LocalDate.now().plusMonths(6)));
        prescriptionService.create(new NewPrescriptionRequest(patientLucia.id(), drVazquez.id(), ibuprofen.id(),
                2.0, "1 tablet every 12 hours", LocalDate.now().plusMonths(1)));

        // Xoan's sample refill can be filled from today at Compostela Pharmacy, but it is left SCHEDULED
        // so that "fill" can be tried from the demo client.

        log.info("Sample data loaded: 1 admin, 2 health centers, 3 doctors, 6 patients, 2 pharmacies, "
                + "5 medications, 6 appointments and 5 prescriptions (with their refill plans).");
        log.info(">> Interactive documentation: http://localhost:8080/swagger-ui.html");
        log.info(">> Demo login credentials (username / password):");
        log.info("   administrator: admin / admin");
        log.info("   doctor:        lfernandez / LIC-4501");
        log.info("   patient:       xrodriguez / 20000001Y");
        log.info("   pharmacy:      compostela / PLIC-778899");
    }

    private PatientDTO patient(String name, String lastName, String nationalId, String healthCardNumber,
                                String street, String city, String postalCode, String phone,
                                LocalDate birthDate, Long healthCenterId, Long doctorId, String username) {
        return new PatientDTO(null, name, lastName, nationalId, healthCardNumber, "36" + healthCardNumber.substring(4),
                new AddressDTO(street, city, postalCode, "Pontevedra"), phone,
                username + "@example.com", birthDate, healthCenterId, null, doctorId, null, username, true);
    }

    private PharmacyDTO pharmacy(String name, String pharmacistFirstName, String pharmacistLastName,
                                  String taxId, String licenseNumber, String street, String city,
                                  String postalCode, String phone, String username) {
        return new PharmacyDTO(null, name, pharmacistFirstName, pharmacistLastName, taxId, licenseNumber,
                new AddressDTO(street, city, postalCode, "Pontevedra"), phone,
                username + "@example.com", username, true);
    }
}
