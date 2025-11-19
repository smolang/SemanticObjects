#!/usr/bin/env python3
"""
Create time-series visualisations for SMOL copper simulation output.

Parses parsed_output.txt and generates:
- Individual 3-panel plots for each redox layer
- Combined 3×2 comparison plot for two main layers

Uses the same visualisation style as the Python equivalent scripts.
"""

import re
import matplotlib.pyplot as plt
import numpy as np
from collections import defaultdict
import sys


def parse_smol_output(filepath):
    """
    Parse SMOL simulation output (parsed_output.txt format).

    Returns:
        Dictionary with timestep data for each redox layer
    """
    data = {
        'timesteps': [],
        'times_ma': [],
        'layers': defaultdict(lambda: {
            'depth': 0,
            'organic': 0,
            'temperature': 0,
            'precipitation_events': []
        })
    }

    with open(filepath, 'r') as f:
        content = f.read()

    # Extract all timesteps (SMOL format with decimal time and variable spacing)
    timestep_pattern = r'TIMESTEP #(\d+):\s+([\d.]+) Ma ago'
    for match in re.finditer(timestep_pattern, content):
        data['timesteps'].append(int(match.group(1)))
        data['times_ma'].append(float(match.group(2)))

    # Extract all redox precipitation events (SMOL format)
    # Pattern for SMOL output format (BEFORE and AFTER on same line)
    precip_pattern = (
        r'\*\*\* REDOX BOUNDARY PRECIPITATION \*\*\*\s+'
        r'Depth: ([\d.]+) m, T= ([\d.]+) °C\s+'
        r'BEFORE: Ox=([\d.]+), Cu=([\d.]+) ppm, Sal=([\d.]+) wt%\s+AFTER:\s+Ox=([\d.]+)\s*,\s*Cu=([\d.]+) ppm, Sal=([\d.]+) wt%\s+'
        r'Cu DEPOSITED: ([\d.]+) kg/m³[^\n]*\s+'
        r'CUMULATIVE Cu IN LAYER: ([\d.]+) kg/m³\s+'
        r'Unit organic content: ([\d.]+) wt%'
    )

    # Find all precipitation events
    for match in re.finditer(precip_pattern, content, re.MULTILINE):
        depth = float(match.group(1))
        temperature = float(match.group(2))
        before_ox = float(match.group(3))
        before_cu = float(match.group(4))
        before_sal = float(match.group(5))
        after_ox = float(match.group(6))
        after_cu = float(match.group(7))
        after_sal = float(match.group(8))
        cu_deposited = float(match.group(9))
        cumulative_cu = float(match.group(10))
        organic = float(match.group(11))

        # Find which timestep this belongs to by looking backwards
        event_pos = match.start()
        timestep_matches = list(re.finditer(timestep_pattern, content[:event_pos]))
        if timestep_matches:
            last_timestep_match = timestep_matches[-1]
            current_timestep = int(last_timestep_match.group(1))
            current_time = float(last_timestep_match.group(2))

            # Store in layer data
            layer_data = data['layers'][depth]
            layer_data['depth'] = depth
            layer_data['organic'] = organic
            layer_data['temperature'] = temperature

            layer_data['precipitation_events'].append({
                'time': current_time,
                'timestep': current_timestep,
                'before_ox': before_ox,
                'after_ox': after_ox,
                'before_sal': before_sal,
                'after_sal': after_sal,
                'before_cu': before_cu,
                'after_cu': after_cu,
                'cu_deposited': cu_deposited,
                'cumulative_cu': cumulative_cu
            })

    return data


def kg_m3_to_wt_percent(kg_m3):
    """
    Convert Cu concentration from kg/m³ to wt%.

    Conversion: 0.1 wt% = 1000 kg/m³
    Therefore: wt% = (kg/m³ / 1000) * 0.1
    """
    return (kg_m3 / 1000.0) * 0.1


def create_layer_diagram(layer_data, depth, output_file):
    """Create multi-panel time-series diagram for a single redox layer."""

    if not layer_data['precipitation_events']:
        print(f"No precipitation events found for layer at {depth}m")
        return

    # Extract time series from precipitation events
    events = layer_data['precipitation_events']
    times = [e['time'] for e in events]
    oxidation_before = [e['before_ox'] for e in events]
    oxidation_after = [e['after_ox'] for e in events]
    salinity = [e['before_sal'] for e in events]
    cu_deposited = [e['cu_deposited'] for e in events]
    cumulative_cu = [e['cumulative_cu'] for e in events]

    # Create figure with 3 subplots
    fig, (ax1, ax2, ax3) = plt.subplots(3, 1, figsize=(12, 10), sharex=True)

    fig.suptitle(
        f'Redox Layer at {depth:.0f}m depth ({layer_data["organic"]:.1f}% organic carbon, T={layer_data["temperature"]:.1f}°C)\n'
        f'Time-Series Evolution',
        fontsize=14, fontweight='bold'
    )

    # ========== PANEL 1: SALINITY ==========
    ax1.plot(times, salinity, 'o-', color='steelblue', linewidth=2,
             markersize=4, label='Brine salinity')
    ax1.axhline(y=31, color='red', linestyle='--', linewidth=2,
                label='Vein formation threshold (31 wt%)')
    ax1.set_ylabel('Salinity (wt% NaCl)', fontweight='bold', fontsize=11)
    ax1.set_ylim(bottom=20)
    ax1.grid(True, alpha=0.3)
    ax1.legend(loc='upper right', fontsize=9)
    ax1.set_title('(a) Brine Salinity at Redox Boundary', loc='left', fontweight='bold')

    # ========== PANEL 2: OXIDATION STATE ==========
    ax2.plot(times, oxidation_before, 's-', color='orange', linewidth=2,
             markersize=5, label='Before precipitation', alpha=0.8)
    ax2.plot(times, oxidation_after, 'o-', color='darkgreen', linewidth=2,
             markersize=4, label='After precipitation', alpha=0.8)
    ax2.axhline(y=0.5, color='red', linestyle='--', linewidth=2,
                label='Redox threshold (0.5)')
    ax2.fill_between(times, oxidation_after, oxidation_before,
                     alpha=0.2, color='orange', label='Oxidation consumed')
    ax2.set_ylabel('Oxidation State', fontweight='bold', fontsize=11)
    ax2.set_ylim(0, 1.0)
    ax2.grid(True, alpha=0.3)
    ax2.legend(loc='upper right', fontsize=9)
    ax2.set_title('(b) Oxidation State Changes', loc='left', fontweight='bold')

    # ========== PANEL 3: COPPER PRECIPITATION ==========
    ax3_twin = ax3.twinx()

    # Bar chart for individual precipitation events
    width = 5  # Bar width in Ma
    bars = ax3.bar(times, cu_deposited, width=width, alpha=0.6,
                   color='brown', label='Cu deposited per event', edgecolor='black')

    # Line plot for cumulative copper
    line = ax3_twin.plot(times, cumulative_cu, 'o-', color='darkred',
                         linewidth=2.5, markersize=6, label='Cumulative Cu',
                         zorder=10)

    ax3.set_ylabel('Cu Deposited per Event (kg/m³)', fontweight='bold', fontsize=11, color='brown')
    ax3_twin.set_ylabel('Cumulative Cu (kg/m³)', fontweight='bold', fontsize=11, color='darkred')
    ax3.set_xlabel('Time (Ma ago)', fontweight='bold', fontsize=12)
    ax3.tick_params(axis='y', labelcolor='brown')
    ax3_twin.tick_params(axis='y', labelcolor='darkred')
    ax3.grid(True, alpha=0.3, axis='x')

    # Combine legends
    bars_legend = ax3.legend(loc='upper left', fontsize=9)
    ax3_twin.legend(loc='upper right', fontsize=9)
    ax3.add_artist(bars_legend)

    ax3.set_title('(c) Copper Precipitation Events', loc='left', fontweight='bold')

    # Set x-axis to show geological time (past on right)
    if times:
        ax3.set_xlim(max(times) + 10, min(times) - 10)

    plt.tight_layout()
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    print(f"✓ Saved diagram: {output_file}")

    # Print summary statistics
    print(f"\n{'='*60}")
    print(f"LAYER SUMMARY: {depth:.0f}m depth")
    print(f"{'='*60}")
    print(f"Temperature: {layer_data['temperature']:.1f}°C")
    print(f"Organic content: {layer_data['organic']:.1f} wt%")
    print(f"Total precipitation events: {len(events)}")
    print(f"Time span: {max(times):.0f} - {min(times):.0f} Ma ago")
    print(f"Average salinity: {np.mean(salinity):.1f} wt%")
    print(f"Initial oxidation: {oxidation_before[0]:.3f}")
    print(f"Final oxidation: {oxidation_after[-1]:.3f}")
    print(f"Total Cu deposited: {cumulative_cu[-1]:.2f} kg/m³")
    print(f"Average Cu per event: {np.mean(cu_deposited):.2f} kg/m³")
    plt.close(fig)


def create_combined_comparison(layer1_data, depth1, layer2_data, depth2,
                                output_file='combined_layer_comparison.png'):
    """Create combined comparison plot for both layers."""

    # Extract time series for layer 1
    events1 = layer1_data['precipitation_events']
    times1 = [e['time'] for e in events1]
    ox_before1 = [e['before_ox'] for e in events1]
    ox_after1 = [e['after_ox'] for e in events1]
    salinity1 = [e['before_sal'] for e in events1]
    cu_deposited1 = [kg_m3_to_wt_percent(e['cu_deposited']) for e in events1]
    cumulative_cu1 = [kg_m3_to_wt_percent(e['cumulative_cu']) for e in events1]

    # Extract time series for layer 2
    events2 = layer2_data['precipitation_events']
    times2 = [e['time'] for e in events2]
    ox_before2 = [e['before_ox'] for e in events2]
    ox_after2 = [e['after_ox'] for e in events2]
    salinity2 = [e['before_sal'] for e in events2]
    cu_deposited2 = [kg_m3_to_wt_percent(e['cu_deposited']) for e in events2]
    cumulative_cu2 = [kg_m3_to_wt_percent(e['cumulative_cu']) for e in events2]

    # Create figure with 3 rows x 2 columns
    fig, axes = plt.subplots(3, 2, figsize=(16, 12), sharex='col')

    # Column titles
    axes[0, 0].text(0.5, 1.15, f'Layer 1: {depth1:.0f}m depth ({layer1_data["organic"]:.1f}% org C, T={layer1_data["temperature"]:.1f}°C)',
                    transform=axes[0, 0].transAxes, ha='center', fontsize=13, fontweight='bold')
    axes[0, 1].text(0.5, 1.15, f'Layer 2: {depth2:.0f}m depth ({layer2_data["organic"]:.1f}% org C, T={layer2_data["temperature"]:.1f}°C)',
                    transform=axes[0, 1].transAxes, ha='center', fontsize=13, fontweight='bold')

    # ========== ROW 1: SALINITY ==========
    # Layer 1
    axes[0, 0].plot(times1, salinity1, 'o-', color='steelblue', linewidth=2.5,
                    markersize=5, label='Brine salinity')
    axes[0, 0].axhline(y=31, color='red', linestyle='--', linewidth=2,
                       label='Vein threshold (31 wt%)', alpha=0.7)
    axes[0, 0].set_ylabel('Salinity (wt% NaCl)', fontweight='bold', fontsize=11)
    axes[0, 0].set_ylim(20, 40)
    axes[0, 0].grid(True, alpha=0.3)
    axes[0, 0].legend(loc='upper right', fontsize=9)
    axes[0, 0].set_title('(a) Salinity Evolution', loc='left', fontweight='bold')

    # Layer 2
    axes[0, 1].plot(times2, salinity2, 'o-', color='steelblue', linewidth=2.5,
                    markersize=5, label='Brine salinity')
    axes[0, 1].axhline(y=31, color='red', linestyle='--', linewidth=2,
                       label='Vein threshold (31 wt%)', alpha=0.7)
    axes[0, 1].set_ylabel('Salinity (wt% NaCl)', fontweight='bold', fontsize=11)
    axes[0, 1].set_ylim(20, 40)
    axes[0, 1].grid(True, alpha=0.3)
    axes[0, 1].legend(loc='upper right', fontsize=9)
    axes[0, 1].set_title('(b) Salinity Evolution', loc='left', fontweight='bold')

    # ========== ROW 2: OXIDATION STATE ==========
    # Layer 1
    axes[1, 0].plot(times1, ox_before1, 's-', color='orange', linewidth=2,
                    markersize=6, label='Before precipitation', alpha=0.8)
    axes[1, 0].plot(times1, ox_after1, 'o-', color='darkgreen', linewidth=2,
                    markersize=5, label='After precipitation', alpha=0.8)
    axes[1, 0].axhline(y=0.5, color='red', linestyle='--', linewidth=2,
                       label='Redox threshold (0.5)', alpha=0.7)
    axes[1, 0].fill_between(times1, ox_after1, ox_before1,
                            alpha=0.2, color='orange', label='Ox consumed')
    axes[1, 0].set_ylabel('Oxidation State', fontweight='bold', fontsize=11)
    axes[1, 0].set_ylim(0, 1.0)
    axes[1, 0].grid(True, alpha=0.3)
    axes[1, 0].legend(loc='upper right', fontsize=9)
    axes[1, 0].set_title('(c) Oxidation Changes', loc='left', fontweight='bold')

    # Layer 2
    axes[1, 1].plot(times2, ox_before2, 's-', color='orange', linewidth=2,
                    markersize=6, label='Before precipitation', alpha=0.8)
    axes[1, 1].plot(times2, ox_after2, 'o-', color='darkgreen', linewidth=2,
                    markersize=5, label='After precipitation', alpha=0.8)
    axes[1, 1].axhline(y=0.5, color='red', linestyle='--', linewidth=2,
                       label='Redox threshold (0.5)', alpha=0.7)
    axes[1, 1].fill_between(times2, ox_after2, ox_before2,
                            alpha=0.2, color='orange', label='Ox consumed')
    axes[1, 1].set_ylabel('Oxidation State', fontweight='bold', fontsize=11)
    axes[1, 1].set_ylim(0, 1.0)
    axes[1, 1].grid(True, alpha=0.3)
    axes[1, 1].legend(loc='upper right', fontsize=9)
    axes[1, 1].set_title('(d) Oxidation Changes', loc='left', fontweight='bold')

    # ========== ROW 3: COPPER PRECIPITATION ==========
    # Layer 1
    ax1_twin = axes[2, 0].twinx()
    width = 5
    axes[2, 0].bar(times1, cu_deposited1, width=width, alpha=0.6,
                   color='brown', label='Cu per event', edgecolor='black')
    ax1_twin.plot(times1, cumulative_cu1, 'o-', color='darkred',
                  linewidth=2.5, markersize=7, label='Cumulative Cu', zorder=10)
    axes[2, 0].set_ylabel('Cu per Event (wt%)', fontweight='bold', fontsize=11, color='brown')
    ax1_twin.set_ylabel('Cumulative Cu (wt%)', fontweight='bold', fontsize=11, color='darkred')
    axes[2, 0].set_xlabel('Time (Ma ago)', fontweight='bold', fontsize=12)
    axes[2, 0].tick_params(axis='y', labelcolor='brown')
    ax1_twin.tick_params(axis='y', labelcolor='darkred')
    axes[2, 0].grid(True, alpha=0.3, axis='x')
    bars_leg = axes[2, 0].legend(loc='upper left', fontsize=9)
    ax1_twin.legend(loc='upper right', fontsize=9)
    axes[2, 0].add_artist(bars_leg)
    axes[2, 0].set_title('(e) Copper Precipitation', loc='left', fontweight='bold')

    # Layer 2
    ax2_twin = axes[2, 1].twinx()
    axes[2, 1].bar(times2, cu_deposited2, width=width, alpha=0.6,
                   color='brown', label='Cu per event', edgecolor='black')
    ax2_twin.plot(times2, cumulative_cu2, 'o-', color='darkred',
                  linewidth=2.5, markersize=7, label='Cumulative Cu', zorder=10)
    axes[2, 1].set_ylabel('Cu per Event (wt%)', fontweight='bold', fontsize=11, color='brown')
    ax2_twin.set_ylabel('Cumulative Cu (wt%)', fontweight='bold', fontsize=11, color='darkred')
    axes[2, 1].set_xlabel('Time (Ma ago)', fontweight='bold', fontsize=12)
    axes[2, 1].tick_params(axis='y', labelcolor='brown')
    ax2_twin.tick_params(axis='y', labelcolor='darkred')
    axes[2, 1].grid(True, alpha=0.3, axis='x')
    bars_leg = axes[2, 1].legend(loc='upper left', fontsize=9)
    ax2_twin.legend(loc='upper right', fontsize=9)
    axes[2, 1].add_artist(bars_leg)
    axes[2, 1].set_title('(f) Copper Precipitation', loc='left', fontweight='bold')

    # Set x-axis to show geological time
    if times1:
        axes[2, 0].set_xlim(max(times1) + 10, min(times1) - 10)
    if times2:
        axes[2, 1].set_xlim(max(times2) + 10, min(times2) - 10)

    plt.tight_layout()
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    print(f"✓ Saved combined comparison: {output_file}")

    # Print comparison statistics
    print(f"\n{'='*70}")
    print(f"COMPARISON SUMMARY")
    print(f"{'='*70}")
    print(f"\n{'Layer 1':<30} {'Layer 2':<30}")
    print(f"{'-'*70}")
    print(f"{'Depth:':<20} {depth1:.0f}m{'':<20} {depth2:.0f}m")
    print(f"{'Temperature:':<20} {layer1_data['temperature']:.1f}°C{'':<16} {layer2_data['temperature']:.1f}°C")
    print(f"{'Organic content:':<20} {layer1_data['organic']:.1f} wt%{'':<14} {layer2_data['organic']:.1f} wt%")
    print(f"{'Events:':<20} {len(events1)}{'':<20} {len(events2)}")
    print(f"{'Time span:':<20} {max(times1):.0f}-{min(times1):.0f} Ma{'':<10} {max(times2):.0f}-{min(times2):.0f} Ma")
    print(f"{'Avg salinity:':<20} {np.mean(salinity1):.1f} wt%{'':<12} {np.mean(salinity2):.1f} wt%")
    print(f"{'Initial oxidation:':<20} {ox_before1[0]:.3f}{'':<16} {ox_before2[0]:.3f}")
    print(f"{'Final oxidation:':<20} {ox_after1[-1]:.3f}{'':<16} {ox_after2[-1]:.3f}")
    print(f"{'Total Cu deposited:':<20} {cumulative_cu1[-1]:.3f} wt%{'':<10} {cumulative_cu2[-1]:.3f} wt%")
    print(f"{'Avg Cu per event:':<20} {np.mean(cu_deposited1):.4f} wt%{'':<8} {np.mean(cu_deposited2):.4f} wt%")

    plt.close(fig)


def main():
    """Main function."""

    # Default input file
    input_file = 'parsed_output.txt'

    if len(sys.argv) > 1:
        input_file = sys.argv[1]

    print("=" * 70)
    print("SMOL COPPER SIMULATION VISUALISATION")
    print("=" * 70)
    print(f"Input file: {input_file}")

    # Parse data
    print("\nParsing SMOL simulation output...")
    data = parse_smol_output(input_file)

    # Find layers with copper deposition
    copper_layers = {depth: layer for depth, layer in data['layers'].items()
                     if layer['precipitation_events']}

    if not copper_layers:
        print("ERROR: No copper-bearing layers found in output!")
        print("Make sure the input file contains redox precipitation events.")
        return

    print(f"\nFound {len(copper_layers)} copper-bearing redox layers:")
    for depth, layer in sorted(copper_layers.items(), reverse=True):
        print(f"  - {depth:.0f}m depth ({layer['organic']:.1f}% organic, T={layer['temperature']:.1f}°C): "
              f"{len(layer['precipitation_events'])} precipitation events")

    # Create diagram for each layer
    print("\nGenerating individual layer diagrams...")
    for depth, layer_data in sorted(copper_layers.items(), reverse=True):
        output_file = f"redox_layer_{depth:.0f}m_timeseries.png"
        create_layer_diagram(layer_data, depth, output_file)

    # If we have at least 2 layers, create combined comparison
    if len(copper_layers) >= 2:
        print("\nGenerating combined comparison diagram...")
        sorted_depths = sorted(copper_layers.keys(), reverse=True)
        depth1, depth2 = sorted_depths[0], sorted_depths[1]

        print(f"Comparing two main copper-bearing layers:")
        print(f"  Layer 1: {depth1:.0f}m depth ({copper_layers[depth1]['organic']:.1f}% organic)")
        print(f"  Layer 2: {depth2:.0f}m depth ({copper_layers[depth2]['organic']:.1f}% organic)")

        create_combined_comparison(
            copper_layers[depth1], depth1,
            copper_layers[depth2], depth2
        )

    print("\n" + "=" * 70)
    print("VISUALISATION COMPLETE")
    print("=" * 70)


if __name__ == "__main__":
    main()
