package net.ssehub.kernel_haven.config_mismatches;

import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Categorizes the results of the {@link ConfigMismatchDetector}.
 * @author El-Sharkawy
 *
 */
public enum MismatchResultType {

    CONSISTENT("Consistent"),
    CONFLICT_WITH_VARMODEL("Conflicts with VarModel"),
    VARIABLE_NOT_DEFINED("Variable not defined in VarModel"),
    FORMULA_NOT_SUPPORTED("Formula contains undefined Variables"),
    ERROR("Unexpected error occured.");
    
    private @NonNull String description;
    
    /**
     * Sole constructor.
     * @param description Human readable description/meaning.
     */
    private MismatchResultType(@NonNull String description) {
        this.description = description;
    }
    
    /**
     * Returns the description of the result.
     * @return The description of the result.
     */
    public @NonNull String getDescription() {
        return description;
    }
}
