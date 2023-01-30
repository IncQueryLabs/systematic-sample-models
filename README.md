# Systematically Generated Sample Models
Licensed under the Apache License 2.0 (see [LICENSE](LICENSE))

## Models for high UML metamodel coverage (work in progress)

**Covered need:** sample model files that can be used to test the robustness of model processing facilities (exporters, coverters, storage mechanisms).

**Goal:** optimize for covering the UML metamodel / schema to a very high extent. Generally speaking,
   * the more Element types have instances in the model, the better
   * the more variety in attribute values, the better,
   * the more cross-reference kinds, the better
   * the more Relationship kinds, the better.

   In particular, both for cross-references and Relationship types, our aim is not simply to generate one instance for each kind. 
   Rather, we aim to generate cross-reference instances and Relationship instances for each combination of _concrete element types_ conformant to the source and target metaclasses.   
   
   In time, the models hosted here will progressively grow to cover more and more.
   
**Non-goals:** 
   * coverage of UML profiles
   * avoiding warning/error markers if validation is performed against any validation suite
   * semantically meaningful content 

**Downloadable UML model artifacts:** 
   * MagicDraw/Cameo version 19: to be uploaded
   * MagicDraw/Cameo version 2021x or newer: [download here](https://github.com/IncQueryLabs/systematic-sample-models/blob/main/cameo-models/uml-coverage/Systematic%20Sample%20Model%20-%20UML%20in%20MD2021x.mdzip?raw=true)
