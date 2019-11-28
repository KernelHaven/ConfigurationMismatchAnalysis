/*
 * Copyright 2017-2019 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
 *   <li>And the analysis result, a {@link DetailedMismatchResultType}</li>
 * </ul>
 * @author El-Sharkawy
 *
 */
@TableRow
public class DetailedConfigMismatchResult extends VariableWithFeatureEffect {

    private @NonNull DetailedMismatchResultType result;
    
    /**
     * Creates a new, single mismatch result.
     * @param variable The variable name.
     * @param featureEffect The feature effect of the given variable. Must not be <code>null</code>.
     * @param result The result of the config mismatch analysis.
     */
    public DetailedConfigMismatchResult(@NonNull String variable, @NonNull Formula featureEffect,
        @NonNull DetailedMismatchResultType result) {
        
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
