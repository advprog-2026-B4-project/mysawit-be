package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.domain;

/**
 * Fixed keys for the three main wage variables.
 * Labels and descriptions are embedded here to keep them co-located with the domain.
 */
public enum VariableKey {

    UPAH_BURUH(
            "Upah Buruh per Kg",
            "Upah Buruh untuk setiap kilogram sawit yang dipanen"
    ),
    UPAH_SUPIR(
            "Upah Supir Truk per Kg",
            "Upah Supir untuk setiap kilogram sawit yang dibawa"
    ),
    UPAH_MANDOR(
            "Upah Mandor per Kg",
            "Upah Mandor untuk setiap kilogram sawit yang diterima pabrik produksi"
    );

    private final String label;
    private final String description;

    VariableKey(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel()       { return label; }
    public String getDescription() { return description; }
}
