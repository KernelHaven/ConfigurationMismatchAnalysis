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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Assert;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.fe_analysis.Settings;
import net.ssehub.kernel_haven.fe_analysis.Settings.SimplificationType;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder;
import net.ssehub.kernel_haven.fe_analysis.pcs.PcFinder;
import net.ssehub.kernel_haven.test_utils.TestAnalysisComponentProvider;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Common part for the different kind of {@link PcFinder} tests, copied from Feature Effect Analysis test cases,
 * as this is not provided in plug-ins. 
 * 
 * @param <R> The analysis result type
 * @author El-Sharkawy
 *
 */
@SuppressWarnings("null")
public abstract class AbstractFinderTests<R> {
    
    /**
     * Runs the {@link PcFinder} on the passed element and returns the result for testing.
     * @param element A mocked element, which should be analyzed by the {@link PcFinder}.
     * @param simplification The simplification strategy to apply. Anything, except for
     * {@link SimplificationType#NO_SIMPLIFICATION}, works only from ANT. 
     * @return The detected presence conditions.
     */
    protected List<R> runAnalysis(CodeElement<?> element, SimplificationType simplification) {
        // Generate configuration
        @NonNull TestConfiguration tConfig = null;
        Properties config = new Properties();
        if (null != simplification) {
            config.setProperty(Settings.SIMPLIFIY.getKey(), simplification.name());
        }
        try {
            tConfig = new TestConfiguration(config);
        } catch (SetUpException e) {
            Assert.fail("Could not generate test configuration: " + e.getMessage());
        }
        
        // Create virtual files
        File file1 = new File("file1.c");
        SourceFile<CodeElement<?>> sourceFile1 = new SourceFile<>(file1);
        if (element != null) {
            sourceFile1.addElement(element);
        }
        
        List<R> results = new ArrayList<>();
        try {
            AnalysisComponent<SourceFile<?>> cmComponent
                    = new TestAnalysisComponentProvider<SourceFile<?>>(sourceFile1);
            AnalysisComponent<R> finder = callAnalysor(tConfig, cmComponent);
            R result;
            do {
                result = finder.getNextResult();
                if (null != result) {
                    results.add(result);
                }
            } while (result != null);
        } catch (SetUpException e) {
            Assert.fail("Setting up the " + PcFinder.class.getSimpleName() + " failed: " + e.getMessage());
        }   

        return results;
    }

    /**
     * Calls the analysis component (e.g., {@link PcFinder} or {@link FeatureEffectFinder}.
     * @param tConfig the configuration to pass.
     * @param cmComponent The mocked test file.
     * 
     * @return The analysis component.
     * @throws SetUpException If analysis fails.
     */
    protected abstract AnalysisComponent<R> callAnalysor(@NonNull TestConfiguration tConfig,
            @NonNull AnalysisComponent<SourceFile<?>> cmComponent) throws SetUpException;
}
