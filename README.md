# Systematically Generated Sample Models
Licensed under the Apache License 2.0 (see [LICENSE](LICENSE))

## Models for high UML metamodel and profile coverage (work in progress)

**Covered need:** sample model files that can be used to test the robustness of model processing facilities (exporters, coverters, storage mechanisms).

**Goal:** optimize for covering the UML metamodel / schema to a very high extent. Generally speaking,
   * the more Element types have instances in the model, the better
   * the more variety in attribute values (with non-default assignment), the better,
   * the more cross-reference kinds, the better
   * the more Relationship kinds, the better.
   
Additionally, for specific UML profiles (of which the SysML variant is published here),   
     * the more Stereotypes applied, the better,
     * the more tag definitions that have actual tagged values (with non-default assignments), the better.

In particular, both for cross-references and Relationship types, our aim is not simply to generate one instance for each kind. 
   Rather, we aim to generate cross-reference and containment reference instances and Relationship instances for each combination of _concrete element types_ conformant to the source and target metaclasses. So while the metamodel defines `ownedBehavior` between `BehavioredClassifier` and `Behavior`, a *concerete triple* to be covered would be `Actor` having an `ownedBehavior` that is a `ProtocolStateMachine`.   
   
**Non-goals:** 
   * publically available model providing coverage of UML profiles other than SysML 
   * avoiding warning/error markers if validation is performed against any validation suite
   * semantically meaningful content 
   * covering `Element.syncElement`, which is a MagicDraw-specific, non-standard feature that is not defined in UML

**Results achieved:** 
See detailed coverage reports in the folder `coverage-info`. These were produced by evaluating the model artifacts against the UML metamodel resp. SysML profile, using the coverage measurement tool discussed below.

Most importantly, for the pure UML model artifacts:
   * all UML metaclasses have some instances,
   * all UML metamodel features, including _derived_ ones, are found to have at least one occurrence in the model, with the exception of `Element.syncElement` (non-goal at the time, see above) and some low-level technical features with names starting with underscores (their eOpposite features seem to have instances, so not seeing these results is likely just a MagicDraw API bug),
   * for UML metamodel relations, over 14 thousand combinations of concrete triples (see above) are covered

Likewise, in the models that conform to SysML:
   * all SysML stereotypes have some instances,
   * all public SysML tag definitions, including _derived_ ones, have some features.


**Downloadable model artifacts:** 
   * Pure UML projects
     * __MagicDraw/Cameo version 19: DEPRECATED, not entirely up to date, will be removed in future releases:_ [download here](https://github.com/IncQueryLabs/systematic-sample-models/blob/main/cameo-models/uml-coverage/Systematic%20Sample%20Model%20-%20UML%20in%20MD19.mdzip?raw=true) 
     * MagicDraw/Cameo version 2021x or newer: [download here](https://github.com/IncQueryLabs/systematic-sample-models/blob/main/cameo-models/uml-coverage/Systematic%20Sample%20Model%20-%20UML%20in%20MD2021x.mdzip?raw=true)
     * MagicDraw/Cameo version 2022x or newer: [download here](https://github.com/IncQueryLabs/systematic-sample-models/blob/main/cameo-models/uml-coverage/Systematic%20Sample%20Model%20-%20UML%20in%20MD2022x.mdzip?raw=true)
   * Projects with SysML coverage
     * __MagicDraw/Cameo version 19: DEPRECATED, not entirely up to date, will be removed in future releases:_ [download here](https://github.com/IncQueryLabs/systematic-sample-models/blob/main/cameo-models/sysml-coverage/Systematic%20Sample%20Model%20-%20SysML%20in%20MD19.mdzip?raw=true) 
     * MagicDraw/Cameo version 2021x or newer: [download here](https://github.com/IncQueryLabs/systematic-sample-models/blob/main/cameo-models/sysml-coverage/Systematic%20Sample%20Model%20-%20SysML%20in%20MD2021x.mdzip?raw=true)
     * MagicDraw/Cameo version 2022x or newer: [download here](https://github.com/IncQueryLabs/systematic-sample-models/blob/main/cameo-models/sysml-coverage/Systematic%20Sample%20Model%20-%20SysML%20in%20MD2022x.mdzip?raw=true)

## Metamodel and profile coverage measurement

The high metamodel and profile coverage is attested by a MagicDraw / Cameo plug-in utility that measures the extent of coverage. The above referenced coverage reports were generated using this tool.
The source code of this tool is provided as is, without build script or developer guide.

Find the source code in the folder `tools/com.incquerylabs.magicdraw.plugin.metamodelreport`, see also the [user guide](tools/com.incquerylabs.magicdraw.plugin.metamodelreport/userguide.md). 