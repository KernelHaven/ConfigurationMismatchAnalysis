package net.ssehub.kernel_haven.config_mismatches;

import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.and;
import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.not;
import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.util.Collections;
import java.util.Set;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.cnf.Cnf;
import net.ssehub.kernel_haven.cnf.ConverterException;
import net.ssehub.kernel_haven.cnf.FormulaToCnfConverterFactory;
import net.ssehub.kernel_haven.cnf.FormulaToCnfConverterFactory.Strategy;
import net.ssehub.kernel_haven.cnf.IFormulaToCnfConverter;
import net.ssehub.kernel_haven.cnf.ISatSolver;
import net.ssehub.kernel_haven.cnf.SatSolverFactory;
import net.ssehub.kernel_haven.cnf.SolverException;
import net.ssehub.kernel_haven.cnf.VmToCnfConverter;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.util.FormatException;
import net.ssehub.kernel_haven.util.ProgressLogger;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.VariableFinder;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Detects if an feature effect is not covered in the variability model.
 * @author El-Sharkawy
 *
 */
public class ConfigMismatchDetector extends AnalysisComponent<ConfigMismatchResult> {

    private @NonNull AnalysisComponent<VariableWithFeatureEffect> feFinder;
    private @NonNull AnalysisComponent<VariabilityModel> vmProvider;
    private @NonNull IFormulaToCnfConverter converter;

    /**
     * Creates a new {@link ConfigMismatchDetector} for the given feature effect finder.
     * @param config The global configuration.
     * @param vmProvider The variability model, usually <tt>PipelineAnalysis.getVmComponent()</tt>.
     * @param feFinder The component to get the feature effects (constraints extracted from code).
     * 
     * @throws SetUpException If no variability model is passed or it could not be translated into CNF representation.
     */
    public ConfigMismatchDetector(@NonNull Configuration config,
            @NonNull AnalysisComponent<VariabilityModel> vmProvider,
            @NonNull AnalysisComponent<VariableWithFeatureEffect> feFinder) throws SetUpException {
        
        super(config);
        this.feFinder = feFinder;
        this.vmProvider = vmProvider;
        converter = FormulaToCnfConverterFactory.create(Strategy.RECURISVE_REPLACING);
    }

    @Override
    protected void execute() {
        Cnf varModel = null;
        Set<String> variables = null;
        try {
            VariabilityModel vm = vmProvider.getNextResult();
            if (vm != null) {
                variables = Collections.unmodifiableSet(vm.getVariableMap().keySet());
                varModel = new VmToCnfConverter().convertVmToCnf(vm);
            }
        } catch (FormatException e) {
            LOGGER.logException("Can't convert variability model to CNF", e);
        }
        
        if (varModel == null || variables == null) {
            LOGGER.logError("Couldn't get variability model.");
            return;
        }
        
        ProgressLogger progress = new ProgressLogger(notNull(getClass().getSimpleName()));
        
        VariableWithFeatureEffect variable;
        while ((variable = feFinder.getNextResult()) != null) {
            ConfigMismatchResult mismatchResult = null;
            String varName = variable.getVariable();
            Formula feConstraint = variable.getFeatureEffect();
            
            if (!variables.contains(varName)) {
                mismatchResult = new ConfigMismatchResult(varName, feConstraint,
                    MismatchResultType.VARIABLE_NOT_DEFINED);
            } else {
                VariableFinder varFinder = new VariableFinder();
                feConstraint.accept(varFinder);
                boolean allVarsKnown = true;
                for (String var : varFinder.getVariableNames()) {
                    if (!variables.contains(var)) {
                        allVarsKnown = false;
                        break;
                    }
                }
                if (!allVarsKnown) {
                    mismatchResult = new ConfigMismatchResult(varName, feConstraint,
                        MismatchResultType.FORMULA_NOT_SUPPORTED);
                }
            }
            
            if (null == mismatchResult) {
                try {
                    Cnf feViolationAsCnf = converter.convert(and(varName, not(feConstraint)));
                    
                    // check if sat(VarModel AND Variable is selected AND feature effect is violated)
                    ISatSolver solver = SatSolverFactory.createSolver(varModel, false);
                    boolean isMissing = solver.isSatisfiable(feViolationAsCnf);
                    
                    mismatchResult = new ConfigMismatchResult(varName, feConstraint,
                        isMissing ? MismatchResultType.CONFLICT_WITH_VARMODEL : MismatchResultType.CONSISTENT);
                } catch (ConverterException e) {
                    mismatchResult = new ConfigMismatchResult(varName, feConstraint, MismatchResultType.ERROR);
                    LOGGER.logError("Could not translate feature effect constraint for variable: "
                        + variable.getVariable() + ", reason: " + e.getMessage());
                } catch (SolverException e) {
                    mismatchResult = new ConfigMismatchResult(varName, feConstraint, MismatchResultType.ERROR);
                    LOGGER.logError("Could not solve feature effect constraint for variable: "
                            + variable.getVariable() + ", reason: " + e.getMessage());
                }
            }
            addResult(mismatchResult);
            progress.processedOne();
        }
        progress.close();
    }

    @Override
    public @NonNull String getResultName() {
        return "Configuration Mismatches";
    }

}
