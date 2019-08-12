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

import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.and;
import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.or;
import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.not;
import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.util.Collections;
import java.util.Set;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.cnf.Cnf;
import net.ssehub.kernel_haven.cnf.CnfVariable;
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
import net.ssehub.kernel_haven.util.logic.Variable;
import net.ssehub.kernel_haven.util.logic.VariableFinder;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Detects if an feature effect is not covered in the variability model.
 * @author El-Sharkawy
 * @author Slawomir Duszynski
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
        Cnf varModelNegated = null;
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
        
        //compute the negated feature model as Cnf
        try {
			varModelNegated = converter.convert(not(varModel.asFormula()));
		} catch (ConverterException e1) {
            LOGGER.logException("Could not convert negated variability model to CNF", e1);
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
                	Formula featureEffect = or(not(new Variable(varName)), feConstraint); // Variable => feConstraint
                	Cnf featureEffectAsCnf = converter.convert(featureEffect); 
                    Cnf feViolationAsCnf = converter.convert(and(varName, not(feConstraint)));// NOT (Variable => feConstraint)
                                        
                    ISatSolver solver = SatSolverFactory.createSolver(varModel, false);
                    
                    boolean isCommonPart = solver.isSatisfiable(featureEffectAsCnf);
                    
                    if(!isCommonPart) {
                    	mismatchResult = new ConfigMismatchResult(varName, feConstraint, MismatchResultType.CONTRADICTION);
                    }
                    else {
                    	boolean isVmMoreGeneral = solver.isSatisfiable(feViolationAsCnf);
                    	
                    	ISatSolver negatedSolver = SatSolverFactory.createSolver(varModelNegated, false);
                    	boolean isEffectMoreGeneral = negatedSolver.isSatisfiable(featureEffectAsCnf);
                    	
                    	if(isVmMoreGeneral) {
                            mismatchResult = new ConfigMismatchResult(varName, feConstraint,
                                    isEffectMoreGeneral ? MismatchResultType.PARTIAL_OVERLAP : MismatchResultType.VM_MORE_GENERAL);
                            
                            if(isEffectMoreGeneral) {
                            	//special case: check if the partial overlap is only possible when the feature is deselected; if so, change the status
                            	Cnf featureActive = converter.convert(and(new Variable(varName), featureEffect)); // Variable AND featureEffect
                            
                            	if(!solver.isSatisfiable(featureActive)) {
                            		//only possible to satisfy with the varName negated
                                    mismatchResult = new ConfigMismatchResult(varName, feConstraint, MismatchResultType.PARTIAL_OVERLAP_DEAD);
                            	}
                            }
                    	}
                    	else {
                            mismatchResult = new ConfigMismatchResult(varName, feConstraint,
                                    isEffectMoreGeneral ? MismatchResultType.FORMULA_MORE_GENERAL : MismatchResultType.CONSISTENT);
                            
                            if(isEffectMoreGeneral && feConstraint.toString().equals("1")) {
                            	//special case: the SAT checks do not properly detect equivalence with a feature effect of TRUE
                            	//find if the variable is not implying anything in the FM, and if so, return a CONSISTENT finding
                            	if(!checkVariableHasImplications(varModel, varName)) {
                                    mismatchResult = new ConfigMismatchResult(varName, feConstraint, MismatchResultType.CONSISTENT);
                            	}
                            }
                    	}
                    	
                    }
                    
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

    /**
	 * @param varModel
	 * @param varName
	 * @return true if the variable implies anything in the varModel 
	 */
	private boolean checkVariableHasImplications(Cnf varModel, String varName) {
		if(!varModel.getAllVarNames().contains(varName)) {
			return false;
		}

		//if the variable is only occurring as non-negated in the CNF then it is not on the left side of any implication
		for(int i=0;i<varModel.getRowCount();i++) {
			for(CnfVariable v:varModel.getRow(i)) {
				if(v.isNegation() && v.getName().equals(varName)) {
					return true;
				}
			}
		}
		
		return false;
	}

	@Override
    public @NonNull String getResultName() {
        return "Configuration Mismatches";
    }

}
