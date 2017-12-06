package net.ssehub.kernel_haven.config_mismatches;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.CodeBlock;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.fe_analysis.Settings.SimplificationType;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.fe_analysis.pcs.PcFinder;
import net.ssehub.kernel_haven.test_utils.TestAnalysisComponentProvider;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Variable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Tests the {@link ConfigMismatchDetector}.
 * @author El-Sharkawy
 *
 */
public class ConfigMismatchDetectorTest extends AbstractFinderTests<VariableWithFeatureEffect> {

    private AnalysisComponent<VariabilityModel> vm;
    
    /**
     * Initializes the logger.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        Logger.init();
    }
    
    /**
     * Tests if a code nesting, contrary to the nesting in the variability model can be identified as configuration
     * mismatch.
     */
    @Test
    public void testDetectionOfContraryNesting() {
        // Load Variability Model: A is nested in B
        setVarModel(new File("testdata/ANestedInB.cnf"));
        
        // Mock code file: B is nested in A
        Variable varA = new Variable("A");
        Variable varB = new Variable("B");
        CodeElement element = new CodeBlock(varA);
        CodeElement nestedElement = new CodeBlock(new Conjunction(varB, varA));
        element.addNestedElement(nestedElement);
        List<VariableWithFeatureEffect> results = detectConfigMismatches(element);
        
        Assert.assertEquals(1, results.size());
    }

    /**
     * Loads and sets the variability model based on the given CNF file.
     * @param cnfFile A CNF representation of the variability model (must exist).
     */
    private void setVarModel(File cnfFile) {
        Assert.assertTrue("VarModel file does not exist: " + cnfFile.getAbsolutePath(), cnfFile.exists());
        
        Set<VariabilityVariable> variables = new HashSet<>();
        VariabilityVariable alpha = new VariabilityVariable("ALPHA", "bool", 1);
        variables.add(alpha);
        variables.add(new VariabilityVariable("BETA", "bool", 2));
        variables.add(new VariabilityVariable("GAMMA", "bool", 3));
        VariabilityModel varModel = new VariabilityModel(cnfFile, variables);
        
        Assert.assertNotNull("Error: VariabilityModel not initialized.", varModel);
        try {
            vm = new TestAnalysisComponentProvider<VariabilityModel>(varModel);
        } catch (SetUpException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }
    
    /**
     * Runs the {@link ConfigMismatchDetector} on the passed element and returns the result for testing.
     * @param element A mocked element, which should be analyzed by the {@link ConfigMismatchDetector}. 
     * @return The detected configuration mismatches.
     */
    private List<VariableWithFeatureEffect> detectConfigMismatches(CodeElement element) {
        return super.runAnalysis(element, SimplificationType.NO_SIMPLIFICATION);
    }
    
    @Override
    protected AnalysisComponent<VariableWithFeatureEffect> callAnalysor(TestConfiguration tConfig,
        AnalysisComponent<SourceFile> cmComponent) throws SetUpException {
        
        PcFinder pcFinder = new PcFinder(tConfig, cmComponent);
        FeatureEffectFinder feFinder = new FeatureEffectFinder(tConfig, pcFinder);
        ConfigMismatchDetector cmDetector = new ConfigMismatchDetector(tConfig, vm, feFinder);
        cmDetector.execute();
        
        return cmDetector;
    }

}
