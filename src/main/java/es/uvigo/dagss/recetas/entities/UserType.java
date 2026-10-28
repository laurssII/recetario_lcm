package es.uvigo.dagss.recetas.entities;

public enum UserType {
    ADMINISTRATOR("ADMINISTRATOR"),
    PATIENT("PATIENT"),
    DOCTOR("DOCTOR"),
    PHARMACY("PHARMACY");

    private final String label;

    UserType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
