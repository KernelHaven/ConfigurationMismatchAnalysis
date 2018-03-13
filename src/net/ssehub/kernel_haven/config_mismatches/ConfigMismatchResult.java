package net.ssehub.kernel_haven.config_mismatches;

import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.util.io.TableElement;
import net.ssehub.kernel_haven.util.io.TableRow;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * A single result of the configuration mismatch analysis. This consist of:
 * <ul>
 *   <li>A {@link VariableWithFeatureEffect}</li>
 *   <li>And the analysis result, a {@link MismatchResultType}</li>
 * </ul>
 * @author El-Sharkawy
 *
 */
@TableRow
public class ConfigMismatchResult extends VariableWithFeatureEffect {

    private @NonNull MismatchResultType result;
    
    /**
     * Creates a new, single mismatch result.
     * @param variable The variable name.
     * @param featureEffect The feature effect of the given variable. Must not be <code>null</code>.
     * @param result The result of the config mismatch analysis.
     */
    public ConfigMismatchResult(@NonNull String variable, @NonNull Formula featureEffect,
        @NonNull MismatchResultType result) {
        
        super(variable, featureEffect);
        this.result = result;
    }

    /**
     * Returns the variable name.
     * 
     * @return The name of the variable.
     */
    @TableElement(name = "Resolution", index = 2)
    public @NonNull String getResult() {
        return result.getDescription();
    }
}
