# ConfigurationMismatchAnalysis

![Build Status](https://jenkins.sse.uni-hildesheim.de/buildStatus/icon?job=KernelHaven_Configuration_Mismatches)

An analysis plugin for [KernelHaven](https://github.com/KernelHaven/KernelHaven).

Analysis components for detection of Configuration Mismatches as published by [El-Sharkawy, Krafczyk, and Schmid](https://dl.acm.org/citation.cfm?id=3106208).

## Usage

Place [`ConfigurationMismatchAnalysis.jar`](https://jenkins.sse.uni-hildesheim.de/view/KernelHaven/job/KernelHaven_Configuration_Mismatches/lastSuccessfulBuild/artifact/build/jar/ConfigurationMismatchAnalysis.jar) in the plugins folder of KernelHaven.

The following analysis components can be used as part of a `ConfiguredPipelineAnalysis`:
* `net.ssehub.kernel_haven.config_mismatches.ConfigMismatchDetector`

## Dependencies

In addition to KernelHaven, this plugin has the following dependencies:
* [FeatureEffectAnalyzer](https://github.com/KernelHaven/FeatureEffectAnalysis)
* [CnfUtils](https://github.com/KernelHaven/CnfUtils)

## License

This plugin is licensed under the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0.html).
