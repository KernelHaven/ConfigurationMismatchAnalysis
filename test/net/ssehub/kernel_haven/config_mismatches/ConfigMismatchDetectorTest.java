package net.ssehub.kernel_haven.config_mismatches;

import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.and;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.CodeBlock;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.fe_analysis.Settings.SimplificationType;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder;
import net.ssehub.kernel_haven.fe_analysis.pcs.PcFinder;
import net.ssehub.kernel_haven.test_utils.TestAnalysisComponentProvider;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.logic.Variable;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityModelDescriptor.ConstraintFileType;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Tests the {@link ConfigMismatchDetector}.
 * @author El-Sharkawy
 *
 */
@SuppressWarnings("null")
public class ConfigMismatchDetectorTest extends AbstractFinderTests<ConfigMismatchResult> {

    private AnalysisComponent<VariabilityModel> vm;
    
    /**
     * Tests if a code nesting, contrary to the nesting in the variability model can be identified as configuration
     * mismatch.
     */
    @Test
    public void testDetectionOfContraryNesting() {
        // Load Variability Model: A is nested in B
        setVarModel(new File("testdata/ANestedInB.cnf"));
        
        // Mock code file: B is nested in A
        Variable varA = new Variable("ALPHA");
        Variable varB = new Variable("BETA");
        CodeBlock element = new CodeBlock(varA);
        CodeBlock nestedElement = new CodeBlock(and(varB, varA));
        element.addNestedElement(nestedElement);
        List<ConfigMismatchResult> results = detectConfigMismatches(element);
        
        // One mismatch detected
        Assert.assertEquals(2, results.size());
        
        // Variable A is not problematic
        ConfigMismatchResult var = results.get(0);
        Assert.assertEquals(varA.getName(), var.getVariable());
        Assert.assertEquals("1", var.getFeatureEffect().toString());
        Assert.assertEquals(MismatchResultType.CONSISTENT.getDescription(), var.getResult());
        
        // Problematic variable is B, which is always nested below A, which is not covered by the variability model
        var = results.get(1);
        Assert.assertEquals(varB.getName(), var.getVariable());
        // Not covered (precondition) constraint for B is: A
        Assert.assertEquals("ALPHA", var.getFeatureEffect().toString());
        Assert.assertEquals(MismatchResultType.CONFLICT_WITH_VARMODEL.getDescription(), var.getResult());        
    }
    
    /**
     * Tests if a code nesting is not reported if it is used in the same way as specified by the variability model.
     */
    @Test
    public void testNoFalseReportForCorrectUsage() {
        // Load Variability Model: A is nested in B
        setVarModel(new File("testdata/ANestedInB.cnf"));
        
        // Mock code file: A is nested in B
        Variable varA = new Variable("ALPHA");
        Variable varB = new Variable("BETA");
        CodeBlock element = new CodeBlock(varB);
        CodeBlock nestedElement = new CodeBlock(and(varB, varA));
        element.addNestedElement(nestedElement);
        List<ConfigMismatchResult> results = detectConfigMismatches(element);
        
        // One mismatch detected
        Assert.assertEquals(2, results.size());
        for (ConfigMismatchResult configMismatchResult : results) {
            Assert.assertEquals(MismatchResultType.CONSISTENT.getDescription(), configMismatchResult.getResult());
        }
    }
    
    /**
     * Tests if free variable in model, which can be used in all situations in code is consistent.
     */
    @Test
    public void testUnConstrainedVariableIsConsistent() {
        // Load Variability Model: A is nested in B
        setVarModel(new File("testdata/ANestedInB.cnf"));
        
        // Mock code file: only GAMMA in one block
        Variable varG = new Variable("GAMMA");
        CodeElement element = new CodeBlock(varG);
        List<ConfigMismatchResult> results = detectConfigMismatches(element);
        
        // One mismatch detected
        Assert.assertEquals(1, results.size());
        ConfigMismatchResult var = results.get(0);
        Assert.assertEquals(varG.getName(), var.getVariable());
        Assert.assertEquals(MismatchResultType.CONSISTENT.getDescription(), var.getResult());
    }
    
    /**
     * Tests unspecified variable found.
     */
    @Test
    public void testUnDefinedVariable() {
        // Load Variability Model: A is nested in B
        setVarModel(new File("testdata/ANestedInB.cnf"));
        
        // Mock code file: only A_UNDEFINED_VAR in one block
        Variable varU = new Variable("A_UNDEFINED_VAR");
        CodeElement element = new CodeBlock(varU);
        List<ConfigMismatchResult> results = detectConfigMismatches(element);
        
        // One mismatch detected
        Assert.assertEquals(1, results.size());
        ConfigMismatchResult var = results.get(0);
        Assert.assertEquals(varU.getName(), var.getVariable());
        Assert.assertEquals(MismatchResultType.VARIABLE_NOT_DEFINED.getDescription(), var.getResult());
    }
    
    /**
     * Tests unspecified variable in constraint found.
     */
    @Test
    public void testUnDefinedConstraints() {
        // Load Variability Model: A is nested in B
        setVarModel(new File("testdata/ANestedInB.cnf"));
        
        // Mock code file: B is nested in A_UNDEFINED_VAR
        Variable varA = new Variable("A_UNDEFINED_VAR");
        Variable varB = new Variable("BETA");
        CodeBlock element = new CodeBlock(varA);
        CodeBlock nestedElement = new CodeBlock(and(varB, varA));
        element.addNestedElement(nestedElement);
        List<ConfigMismatchResult> results = detectConfigMismatches(element);
        
        // One mismatch detected
        Assert.assertEquals(2, results.size());
        
        // Variable A_UNDEFINED_VAR is not defined
        ConfigMismatchResult var = results.get(0);
        Assert.assertEquals(varA.getName(), var.getVariable());
        Assert.assertEquals("1", var.getFeatureEffect().toString());
        Assert.assertEquals(MismatchResultType.VARIABLE_NOT_DEFINED.getDescription(), var.getResult());
        
        // Problematic variable is B, which is always nested below A, which is not covered by the variability model
        var = results.get(1);
        Assert.assertEquals(varB.getName(), var.getVariable());
        // Not checkable (precondition) constraint for B is: A_UNDEFINED_VAR
        Assert.assertEquals("A_UNDEFINED_VAR", var.getFeatureEffect().toString());
        Assert.assertEquals(MismatchResultType.FORMULA_NOT_SUPPORTED.getDescription(), var.getResult());        
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
        varModel.getDescriptor().setConstraintFileType(ConstraintFileType.DIMACS);
        
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
    private List<ConfigMismatchResult> detectConfigMismatches(CodeElement element) {
        return super.runAnalysis(element, SimplificationType.NO_SIMPLIFICATION);
    }
    
    @Override
    protected AnalysisComponent<ConfigMismatchResult> callAnalysor(@NonNull TestConfiguration tConfig,
            @NonNull AnalysisComponent<SourceFile> cmComponent) throws SetUpException {
        
        PcFinder pcFinder = new PcFinder(tConfig, cmComponent);
        FeatureEffectFinder feFinder = new FeatureEffectFinder(tConfig, pcFinder);
        ConfigMismatchDetector cmDetector = new ConfigMismatchDetector(tConfig, vm, feFinder);
        cmDetector.execute();
        
        return cmDetector;
    }

}
