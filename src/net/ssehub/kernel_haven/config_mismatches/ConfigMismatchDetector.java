package net.ssehub.kernel_haven.config_mismatches;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.cnf.Cnf;
import net.ssehub.kernel_haven.cnf.ConverterException;
import net.ssehub.kernel_haven.cnf.FormulaToCnfConverterFactory;
import net.ssehub.kernel_haven.cnf.IFormulaToCnfConverter;
import net.ssehub.kernel_haven.cnf.SatSolver;
import net.ssehub.kernel_haven.cnf.SolverException;
import net.ssehub.kernel_haven.cnf.VmToCnfConverter;
import net.ssehub.kernel_haven.cnf.FormulaToCnfConverterFactory.Strategy;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.util.FormatException;
import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.Variable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Detects if an feature effect is not covered in the variability model.
 * @author El-Sharkawy
 *
 */
public class ConfigMismatchDetector extends AnalysisComponent<VariableWithFeatureEffect> {

    private AnalysisComponent<VariableWithFeatureEffect> feFinder;
    private Cnf varModel;
    private IFormulaToCnfConverter converter;

    /**
     * Creates a new {@link ConfigMismatchDetector} for the given feature effect finder.
     * @param config The global configuration.
     * @param vm The variability model, usually <tt>PipelineAnalysis.getVmComponent()</tt>.
     * @param feFinder The component to get the feature effects (constraints extracted from code).
     * 
     * @throws SetUpException If no variability model is passed or it could not be translated into CNF representation.
     */
    public ConfigMismatchDetector(Configuration config, AnalysisComponent<VariabilityModel> vm,
        AnalysisComponent<VariableWithFeatureEffect> feFinder) throws SetUpException {
        
        super(config);
        this.feFinder = feFinder;
        try {
            varModel = new VmToCnfConverter().convertVmToCnf(vm.getNextResult());
        } catch (FormatException e) {
            throw new SetUpException(e);
        }
        converter = FormulaToCnfConverterFactory.create(Strategy.RECURISVE_REPLACING);
    }

    @Override
    protected void execute() {
        
        VariableWithFeatureEffect variable;
        
        
        while ((variable = feFinder.getNextResult()) != null) {
            try {
                Formula var = new Variable(variable.getVariable());
                Formula notFeatureEffect = new Negation(variable.getFeatureEffect());
                Formula feViolation = new Conjunction(var, notFeatureEffect);
                Cnf feViolationAsCnf = converter.convert(feViolation);
                
                // check if sat(VarModel AND Variable is selected AND feature effect is violated)
                SatSolver solver = new SatSolver(varModel);
                boolean isMissing = solver.isSatisfiable(feViolationAsCnf);
                
                if (isMissing) {
                    addResult(variable);
                }
            } catch (ConverterException e) {
                Logger.get().logError("Could not translate feature effect constraint for variable: "
                    + variable.getVariable() + ", reason: " + e.getMessage());
            } catch (SolverException e) {
                Logger.get().logError("Could not solve feature effect constraint for variable: "
                    + variable.getVariable() + ", reason: " + e.getMessage());
            }
        }
    }

    @Override
    public String getResultName() {
        return "Configuration Mismatches";
    }

}
