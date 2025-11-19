# Copper Simulation Events

## Series of Events in the Copper Simulation (1000 Ma to 0 Ma)

### Phase 1: Basin Formation and Sedimentation (1000 Ma - 600 Ma)
During this phase, the basin fills with sediments but **no mineralisation occurs yet**. The stratigraphy is established but fluid flow has not begun.

**Key observations:**
- No copper precipitation
- No changes in copper content
- Temperature increases with depth according to geothermal gradient (30°C/km)
- Initial parameter values remain stable

### Phase 2: Mineralisation Phase (600 Ma - 0 Ma)
Starting at 600 Ma, oxidized brines begin circulating through the system. This is when all the important events occur.

## Trigger Events to Monitor

"Salinity Evolution" should be the first event (increase salinity).

### 1. **Redox Boundary Precipitation (Primary Trigger)**
**When:** When oxidised brine (oxidation > 0.5) encounters reduced sediment units (isReduced = true, organicContent > 0.5)

**Where:** At reduced sediment units (shales) - typically at depths around 500-800m

**What happens:**
- 70% of dissolved copper precipitates (precipitation_efficiency = 0.7)
- Brine oxidation drops to 30% of original value
- Parameters affected: copperContent increases, brineOxidation decreases

### 2. **Metal Precipitation Triggers (Secondary Triggers)**
Veins form when both conditions are met:
- Temperature ≥ 150°C 
- Salinity > 31 wt% NaCl

**Where veins can form:**
- **Basement units** (depth > ~5000m): 10% of remaining copper precipitates as veins
- **Reduced sediment units with both triggers**: Additional 30% precipitation as veins
- **Oxidised sediment units with both triggers**: 5% precipitation as veins only

### 3. **Salinity Evolution**
**Evaporite dissolution:** When brine passes through evaporite units
- Salinity increases by 5 wt% per event
- Capped at 39 wt% maximum

### 4. **Oxidation State Changes**
- **Oxidised sediments:** Increase brine oxidation by 10% (capped at 1.0)
- **Reduced sediments:** Decrease to 30% after precipitation
- **Basement:** Slight increase (10%)

## Expected Stratigraphic Sequence (bottom to top)

1. **Basement (1000m thick)** - granite/gneiss
2. **Lower oxidised clastics (5 units × 100m)** - red beds
3. **Ore horizon reduced shales (3 units × 50m)** - main precipitation zone
4. **Middle oxidised clastics (5 units × 100m)**
5. **Upper reduced shales (2 units × 50m)** - secondary ore zone
6. **Upper oxidised clastics (2 units × 100m)**
7. **Evaporite cap (1 unit × 200m)** - salinity source

## Key Parameter Evolution During Each Fluid Flow Event

For each 10 Ma time step from 600 Ma to 0 Ma, monitor:

### 1. **Salinity** 
- Starts at 25 wt%
- Increases when passing evaporites
- Critical threshold: >31 wt% for veins

### 2. **Copper Content**
- Starts at 100 ppm in brine
- Decreases as it precipitates
- Flow stops if <0.1 ppm remains

### 3. **Porosity & Permeability**
- Controls fluid flow rates
- Evaporites: 0.01 mD (seal)
- Oxidised sediments: 100 mD (conduit)
- Reduced sediments: 10 mD

### 4. **Organic Content**
- >0.5 wt% maintains reducing conditions
- Critical for redox precipitation

### 5. **Temperature**
- Calculated as: 25°C + (depth_km × 30°C/km)
- 150°C threshold typically at ~4.2 km depth

## Ontology Reasoning Checks
The simulation also performs automated checks using the ontology reasoner:
- High temperature zones detection
- Redox boundary identification