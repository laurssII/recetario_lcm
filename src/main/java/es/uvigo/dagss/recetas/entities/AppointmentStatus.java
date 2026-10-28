package es.uvigo.dagss.recetas.entities;

public enum AppointmentStatus {
    SCHEDULED("SCHEDULED"),
    CANCELLED("CANCELLED"),
    COMPLETED("COMPLETED"),
    NO_SHOW("NO_SHOW");

    private final String label;

    AppointmentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
