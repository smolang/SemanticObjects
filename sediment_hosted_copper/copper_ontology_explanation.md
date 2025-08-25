# Rationale for Extending the Existing Ontology for the Sediment-Hosted Copper Deposit

## The Three New Triggers

### Redox Environment Trigger

The most fundamental control on sediment-hosted copper mineralisation is the interaction between oxidised, metal-bearing fluids and reducing environments. As Hitzman et al. (2010) state in their opening sentence, these deposits "form by movement of oxidised, copper-bearing fluids across a reduction front that results in the precipitation of copper sulphides." This geochemical process is the primary mechanism for the deposition of copper.

In our ontology, we need to represent both oxidising and reducing environments as qualities that geological objects can possess. The redox trigger would activate when an oxidised fluid (such as a copper-bearing brine) encounters a reducing atmosphere (such as organic-rich sediments or hydrocarbons). This trigger is essential because it represents the fundamental chemical reaction that causes dissolved copper to precipitate as solid copper sulphides.

### Temperature Trigger for Vein Formation

According to Selley et al. (2005), fluid inclusion studies from the Zambian Copperbelt show a range of temperatures from approximately 100°C to 400°C, representing "a crude trend of increasing ore fluid salinities and temperatures with time, from diagenesis to early vein generation and postmetamorphic vein emplacement."

For our modelling purposes, we have selected 150°C as the threshold temperature for vein formation. This temperature represents the transition from lower-temperature diagenetic processes to higher-temperature vein formation. The temperature trigger is necessary because vein formation requires sufficient thermal energy to drive fluid flow through fractures and precipitate minerals. Without this trigger, our model cannot distinguish between conditions that produce disseminated mineralisation versus those that create economically significant vein deposits.

### Salinity Trigger for Vein Formation

The Selley et al. (2005) study reports salinities ranging from 11 to 39 weight percent NaCl equivalent in the Zambian Copperbelt. The higher salinity values (31-38 weight percent) are associated with vein formation, while lower salinities are associated with earlier diagenetic processes.

We have set the salinity trigger threshold at greater than 31 weight percent NaCl equivalent for vein formation. High salinity is crucial because it increases the metal-carrying capacity of the fluid, affecting fluid density, which in turn influences fluid flow patterns. The salinity trigger ensures our model accurately represents the conditions necessary for significant copper transport and deposition in veins.

## Implementation Strategy

### Trigger Definitions

Each trigger is implemented as a subclass of the existing trigger class in the ontology. This arrangement maintains consistency with the current ontology structure, where all triggers inherit from a standard parent class. The triggers include appropriate comments and labels to document their purpose and units of measurement.

### Supporting Classes

To model these three triggers, we need additional supporting classes:

**Quality Classes**: The redox trigger requires new quality classes for oxidising and reducing environments. These are modelled as subclasses of BFO's quality class, consistent with how temperature and other qualities are represented in the ontology.

**Process Classes**: When triggers activate, they initiate geological processes. We therefore need process classes for vein formation and copper precipitation. These processes are what create the ore deposits when trigger conditions are met. (GeoCore)

**Fluid Classes**: Since sediment-hosted copper systems specifically involve brines (saline fluids), we need a brine class as a subclass of earth fluid. This design allows us to model the specific type of fluid involved in copper transport. (GeoCore)

### Complex Class Restrictions

The complex class restrictions serve a critical purpose in enabling automated reasoning. These restrictions create anonymous classes that represent geological objects meeting specific trigger conditions. For example, a complex restriction can define "all geological objects with a temperature greater than 150°C that can participate in vein formation."

These restrictions enable the SMOL simulation to automatically identify when and where trigger conditions are met, eliminating the need for manual checking of each condition. This setup is consistent with how the existing oil window maturation trigger is implemented in the simulation.

#### Not Introducing New Restrictions for Redox Environment and Salinity

While class restrictions provide reasoning capabilities for the temperature trigger, they are not appropriate for the salinity and redox triggers due to fundamental differences in how these triggers operate within the sediment-hosted copper system. The salinity trigger differs from temperature because salinity is a property of the migrating fluid rather than the rock unit itself. In our simulation, brine salinity evolves dynamically as fluids flow through the stratigraphic sequence, increasing when passing through evaporites and remaining unchanged in other units. Creating a class restriction for salinity would require modelling fluid properties within the rock ontology, which conflates two distinct aspects of the system. The programmatic check in the simulation correctly captures this fluid-based trigger by evaluating the brine salinity at each point during flow, rather than attempting to assign salinity thresholds to rock units.

The redox trigger presents a different challenge for class restrictions. While we can identify reduced units through their `reducing_environment` quality, the actual trigger mechanism involves the interaction between an oxidised fluid and a reduced rock unit. This relation represents a dynamic process rather than a static property that can be captured through class restrictions. The precipitation of copper requires three simultaneous conditions: the presence of dissolved copper, an oxidised fluid capable of transporting that copper, and a reduced environment to cause precipitation. A class restriction could identify reduced units, but this would be redundant with the existing quality assignment and would not capture the essential fluid-rock interaction. The current implementation, which checks both rock properties and fluid chemistry programmatically, provides a more accurate representation of the geochemical process.

### Rock Type Additions

To support the sediment-hosted copper simulation, two additional rock type classes have been added to the ontology:

**Granite/Gneiss Class**: The simulation requires a representation of crystalline basement rocks, which commonly host copper mineralisation in fracture-controlled veins at deposits like Konkola and Musoshi in the Zambian Copperbelt. The `granite_gneiss` class is implemented as a subclass of `GeoCoreOntology_rock`, representing the crystalline basement complex that underlies sedimentary sequences. These rocks have fundamentally different properties from sedimentary units—near-zero primary porosity and no organic matter for reduction, meaning copper precipitation occurs only through physical processes in fractures when temperature and salinity thresholds are met.

**Evaporite Class**: Evaporites play a dual role in sediment-hosted copper systems. First, they act as sources of high-salinity brines through dissolution, providing the chloride ions necessary for complexing and transporting copper. Second, their low permeability makes them effective aquitards that focus fluid flow laterally, controlling the hydrodynamic regime. The `evaporite` class is implemented as a subclass of `GeoReservoirOntology_sedimentary_rock`, consistent with its sedimentary origin through chemical precipitation. This addition enables the model to represent the Upper Roan Group evaporites that are critical to brine generation in the Zambian Copperbelt.

