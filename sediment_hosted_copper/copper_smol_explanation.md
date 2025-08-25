# Justification Document for Sediment-Hosted Copper Deposit Simulation

## Simulation Architecture Rationale

### Overall Structure

The simulation follows an object-oriented design that mirrors the physical architecture of sedimentary basins. Unlike the petroleum system simulation, which focuses on hydrocarbon generation and migration, this copper system simulation emphasises chemical precipitation at redox boundaries. This fundamental difference drives the architectural choices throughout the code.

The class hierarchy begins with `GeoObject` as the base class, providing spatial relationships and basic properties. This class is extended by `CopperGeoUnit`, which adds copper-specific attributes, such as salinity, copper content, and redox state. This two-level hierarchy allows future expansion while maintaining clarity about which properties are universal versus copper-specific.

### Time Flow Direction

The simulation runs ~~backwards~~ forwards through geological time (from 1000 Ma to the present) using 10 million-year time steps. This approach allows us to model basin evolution chronologically: first sedimentation and burial, then the onset of fluid flow during basin inversion or orogeny. The 10 Ma time step strikes a balance between computational efficiency and geological resolution.

### Fluid Flow Model

Unlike petroleum systems, where hydrocarbons migrate upward and accumulate in structural traps, copper-bearing brines flow upward but precipitate upon encountering chemical traps (redox boundaries). This fundamental difference explains why the simulation processes units from bottom to top, modifying brine chemistry at each step rather than simply accumulating fluids.

## Parameter Value Justifications

### Temporal Parameters

The simulation begins at 1000 Ma to represent the formation of Mesoproterozoic basement, consistent with the age of the basement rocks in the Zambian Copperbelt. Mineralisation starts at 600 Ma, corresponding to the Neoproterozoic timing of ore formation during the Lufilian orogeny. This 400 Ma gap allows for basin filling and burial before ore-forming fluids begin circulating.

### Temperature Parameters

The surface temperature of 25°C represents a reasonable tropical paleoclimate for the Neoproterozoic. The geothermal gradient of 30°C/km falls within the typical continental range of 25-35°C/km and matches heat flow estimates from the Copperbelt region.

The critical vein formation temperature of 150°C derives directly from Selley et al. (2005) fluid inclusion data. Their study showed temperatures ranging from 110 °C to 170°C for early mineralisation, with vein formation occurring at the higher end of this range. The 150°C threshold represents the transition from low-temperature diagenetic processes to higher-temperature hydrothermal vein formation.

### Salinity Parameters

The vein formation salinity threshold of 31 wt% (weight percentage) NaCl equivalent is based on the break in fluid inclusion data from Selley et al. (2005). They report salinities of 11-21 wt% for early fluids and 31-39 wt% for vein-forming fluids. The 31 wt% threshold captures this transition to high-salinity, metal-rich fluids capable of forming economic vein deposits.

The 5 wt% evaporite dissolution increment per flow event represents a balance between dissolution kinetics and flow rates. Given that total salinity must increase from ~15 wt% to ~35 wt% over the mineralisation history, and evidence suggests multiple flow events, a 5 wt% increment allows this evolution over 4-5 fluid pulses, consistent with geological observations.

### Fluid Chemistry Parameters

The initial copper concentration of 100 ppm represents typical basinal brines that have leached metals from red beds. This value falls within the range reported from fluid inclusion analyses and basin modelling studies of sediment-hosted copper systems globally.

The fluid oxidation parameter of 0.9 (on a scale of 0-1) represents highly oxidised brines necessary for copper transport. Copper must be in the Cu²⁺ state to remain dissolved as chloride complexes. The high initial oxidation ensures efficient metal transport until the fluid encounters reducing conditions.

### Precipitation Parameters

The precipitation efficiency of 0.7 (70%) at redox boundaries reflects the reality that not all dissolved copper precipitates immediately upon encountering reducing conditions. Kinetic limitations, incomplete mixing, and fluid bypass typically result in 60-80% of the available copper precipitating at the first reducing horizon encountered. The 70% value represents a reasonable middle ground.

The oxidation-reduction factor of 0.3 (meaning 70% of the oxidising capacity is consumed) derives from reaction stoichiometry. The reduction of Cu²⁺ to copper sulphides consumes oxidants, and field observations show that fluids exiting ore zones have significantly reduced oxidation potential. This phenomenon prevents the remobilisation of already-precipitated ore.

### Vein Concentration Factor

The vein concentration factor of 20 reflects the economic reality that vein ores are typically 10-50 times higher grade than disseminated ores. This phenomenon occurs because veins represent focused fluid flow through fractures with multiple mineralisation pulses, while disseminated ore forms from a single pass of fluid through porous rock. A factor of 20 produces realistic grade distributions matching natural deposits.

## Unit-Specific Design Rationales

### Basement Unit

Basement units are assigned only 10% precipitation efficiency because they lack the reducing agents necessary for chemical precipitation to occur. Mineralisation in the basement relies on physical processes (cooling, pressure drop) rather than chemical reduction. The restriction to vein-only mineralisation reflects the absence of primary porosity in crystalline rocks.

### Reduced Sediment Unit

These units implement the core ore-forming process, known as redox precipitation. The requirement for three conditions (copper present, fluid oxidised, unit reduced) mirrors the natural system where all three must coincide for ore formation. The additional 30% precipitation when temperature and salinity triggers are met represents enhanced precipitation due to convective circulation and higher metal concentrations in hot, saline fluids.

The organic content threshold of 0.5 wt% for maintaining reducing conditions is based on Total Organic Carbon (TOC) analyses from ore-hosting shales. Below this threshold, insufficient reducing agent exists to precipitate significant copper.

### Oxidised Sediment Unit

These units increase fluid oxidation by 10% to represent interaction with hematite and other ferric minerals in red beds. The minimal precipitation (5% in veins only) reflects that oxidising conditions prevent chemical precipitation, limiting mineralisation to physical mechanisms in structural sites.

### Evaporite Unit

The extremely low permeability (0.01 mD) makes evaporites effective aquitards, focusing fluid flow laterally. The 5 wt% salinity increase per event, capped at 39 wt%, represents partial dissolution during fluid transit. The cap prevents unrealistic supersaturation and matches maximum observed salinities in the Copperbelt.

## Stratigraphic Architecture

The model stratigraphy simplifies the Zambian Copperbelt sequence while preserving essential elements. Five oxidised units represent the Lower Roan clastics (fluid conduits), three reduced units model the Ore Shale (precipitation sites), five more oxidised units continue the sequence, and a thick evaporite cap provides salinity and hydrological sealing.

Layer thicknesses of 100m for most units and 50m for ore horizons reflect typical bed thicknesses in the Copperbelt. The thinner ore horizons align with observations that economic mineralisation often concentrates in relatively thin, reduced intervals.