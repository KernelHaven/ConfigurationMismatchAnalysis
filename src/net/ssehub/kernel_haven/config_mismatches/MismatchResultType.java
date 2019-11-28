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
