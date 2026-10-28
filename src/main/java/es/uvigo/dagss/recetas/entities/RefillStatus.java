package es.uvigo.dagss.recetas.entities;

public enum RefillStatus {
    SCHEDULED("SCHEDULED"),
    FILLED("FILLED"),
    CANCELLED("CANCELLED");

    private final String label;

    RefillStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
