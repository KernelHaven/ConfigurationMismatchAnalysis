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

import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Categorizes the results of the {@link ConfigMismatchDetector}.
 * @author El-Sharkawy
 * @author Slawomir Duszynski
 *
 */
public enum MismatchResultType {
	/*
	 * M - Feature Model
	 * E - Feature Effect
	 * 
	 * the comments below describe the satisfiability for 3 cases:
	 * SAT (M && -E)  ;   SAT (M && E) ;  SAT (-M && E)   
	 */
    CONSISTENT("Consistent"), 										// FALSE TRUE FALSE
    CONTRADICTION("Contradicts with VarModel"), 					// TRUE FALSE TRUE
    VM_MORE_GENERAL("VarModel is more general than the formula"),	//TRUE TRUE FALSE
    FORMULA_MORE_GENERAL("Formula is more general than the VarModel"),//FALSE TRUE TRUE
    PARTIAL_OVERLAP("VarModel and formula partially overlap"),		//TRUE TRUE TRUE
    PARTIAL_OVERLAP_DEAD("VarModel and formula partially overlap, but only if the feature is false"), //TRUE TRUE TRUE, + additional check for this case
    VARIABLE_NOT_DEFINED("Variable not defined in VarModel"),
    FORMULA_NOT_SUPPORTED("Formula contains undefined Variables"),
    ERROR("Unexpected error occured.");
	/*
	 * Assuming that:
	 * - the features in the feature model were not dead or always-selected
	 * - the feature effect of a feature cannot be FALSE  
	 * There are then no more possible cases. Especially:
	 * - there is no PARTIAL_OVERLAP_ALWAYS_SELECTED. As we only consider the feature effect of the current feature F, 
	 * 		and it has the form of F => Other, there is no possibility to prevent F from being set to false. 
	 * - the dead feature case can only happen for PARTIAL_OVERLAP. This is because in the other cases M or E are entirely within the set intersection, 
	 * 		and they (per assumption) do not contain dead or always-selected features. 
	 */
	
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
