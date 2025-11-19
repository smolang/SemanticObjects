#!/usr/bin/env python3
"""
Visualization Script for Sensitivity Analysis Results

Generates tornado diagram and summary comparison plots from sensitivity analysis CSV.

Usage:
    python visualize_results.py <results_csv>

Example:
    python visualize_results.py results/sensitivity_results_20251119_120000.csv
"""

import argparse
import json
import sys
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd


class SensitivityVisualizer:
    """Creates visualizations from sensitivity analysis results."""

    def __init__(self, results_csv, config_dir):
        """
        Initialize visualizer.

        Args:
            results_csv: Path to results CSV file
            config_dir: Path to config directory (for parameter metadata)
        """
        self.results_csv = Path(results_csv)
        self.config_dir = Path(config_dir)
        self.figures_dir = self.results_csv.parent.parent / 'figures'
        self.figures_dir.mkdir(exist_ok=True)

        # Load data
        self.df = pd.read_csv(results_csv)

        # Load parameter configuration for nice labels
        with open(self.config_dir / 'parameters.json', 'r') as f:
            self.param_config = json.load(f)

        # Filter successful runs only
        self.df_success = self.df[self.df['status'] == 'success'].copy()

        if len(self.df_success) == 0:
            raise ValueError("No successful runs found in results!")

    def get_param_label(self, param_name):
        """Get human-readable parameter label with units."""
        if param_name == 'baseline':
            return 'Baseline'

        config = self.param_config.get(param_name, {})
        base_name = param_name.replace('_', ' ').title()
        unit = config.get('unit', '')

        if unit:
            return f"{base_name}\n({unit})"
        return base_name

    def create_tornado_diagram(self):
        """
        Create tornado diagram showing parameter sensitivity.

        Horizontal bar chart with:
        - Parameters sorted by range (sensitivity)
        - Red bars (left): low parameter values reduce copper
        - Green bars (right): high parameter values increase copper
        - Black dashed line: baseline
        """
        # Get baseline value
        baseline_row = self.df_success[self.df_success['scenario'] == 'baseline']
        if len(baseline_row) == 0:
            print("WARNING: No baseline run found, using median as baseline")
            baseline_copper = self.df_success['total_copper'].median()
        else:
            baseline_copper = baseline_row.iloc[0]['total_copper']

        # Calculate min/max for each parameter
        param_ranges = []

        for param_name in self.param_config.keys():
            param_data = self.df_success[self.df_success['parameter'] == param_name]

            if len(param_data) < 2:
                continue  # Need at least 2 points

            min_copper = param_data['total_copper'].min()
            max_copper = param_data['total_copper'].max()
            range_copper = max_copper - min_copper

            param_ranges.append({
                'parameter': param_name,
                'min': min_copper,
                'max': max_copper,
                'range': range_copper,
                'label': self.get_param_label(param_name)
            })

        # Sort by range (most sensitive at bottom for tornado diagram convention)
        param_ranges_sorted = sorted(param_ranges, key=lambda x: x['range'])

        # Create figure
        fig, ax = plt.subplots(figsize=(10, 6))

        y_positions = np.arange(len(param_ranges_sorted))

        for i, item in enumerate(param_ranges_sorted):
            # Left bar (red): low value effect
            left_extent = baseline_copper - item['min']
            ax.barh(i, -left_extent, left=baseline_copper, height=0.6,
                    color='#d62728', alpha=0.7, label='Low' if i == 0 else '')

            # Right bar (green): high value effect
            right_extent = item['max'] - baseline_copper
            ax.barh(i, right_extent, left=baseline_copper, height=0.6,
                    color='#2ca02c', alpha=0.7, label='High' if i == 0 else '')

        # Baseline reference line
        ax.axvline(baseline_copper, color='black', linestyle='--', linewidth=2,
                   label=f'Baseline ({baseline_copper:.1f})')

        # Formatting
        ax.set_yticks(y_positions)
        ax.set_yticklabels([item['label'] for item in param_ranges_sorted])
        ax.set_xlabel('Total Copper Deposited (kg/m³)', fontweight='bold', fontsize=11)
        ax.set_title('Parameter Sensitivity: Tornado Diagram', fontweight='bold', fontsize=14)
        ax.grid(True, axis='x', alpha=0.3)
        ax.legend(loc='best', fontsize=10)

        plt.tight_layout()

        # Save
        output_path = self.figures_dir / 'tornado_diagram.png'
        plt.savefig(output_path, dpi=300, bbox_inches='tight')
        print(f"✓ Saved: {output_path}")

        plt.close()

    def create_summary_comparison(self):
        """
        Create summary bar chart comparing best/worst cases for each parameter.

        Shows baseline + best/worst scenario for each parameter.
        """
        # Get baseline value
        baseline_row = self.df_success[self.df_success['scenario'] == 'baseline']
        if len(baseline_row) == 0:
            baseline_copper = self.df_success['total_copper'].median()
        else:
            baseline_copper = baseline_row.iloc[0]['total_copper']

        # Collect scenarios
        scenarios = []
        labels = []
        colors = []

        # Baseline
        scenarios.append(baseline_copper)
        labels.append('Baseline')
        colors.append('steelblue')

        # For each parameter, find worst and best
        for param_name in self.param_config.keys():
            param_data = self.df_success[self.df_success['parameter'] == param_name]

            if len(param_data) < 2:
                continue

            min_row = param_data.loc[param_data['total_copper'].idxmin()]
            max_row = param_data.loc[param_data['total_copper'].idxmax()]

            # Worst case (minimum copper)
            scenarios.append(min_row['total_copper'])
            param_label = self.get_param_label(param_name).split('\n')[0]  # Remove units
            labels.append(f'{param_label}\n(worst)')
            colors.append('#d62728')  # Red

            # Best case (maximum copper)
            scenarios.append(max_row['total_copper'])
            labels.append(f'{param_label}\n(best)')
            colors.append('#2ca02c')  # Green

        # Create figure
        fig, ax = plt.subplots(figsize=(14, 6))

        x_positions = np.arange(len(scenarios))
        bars = ax.bar(x_positions, scenarios, color=colors, alpha=0.7, edgecolor='black')

        # Add value labels on bars
        for i, (bar, value) in enumerate(zip(bars, scenarios)):
            ax.text(bar.get_x() + bar.get_width() / 2, bar.get_height(),
                    f'{value:.1f}', ha='center', va='bottom', fontsize=9)

        # Baseline reference line
        ax.axhline(baseline_copper, color='black', linestyle='--', linewidth=2,
                   alpha=0.5, label=f'Baseline ({baseline_copper:.1f})')

        # Formatting
        ax.set_xticks(x_positions)
        ax.set_xticklabels(labels, rotation=45, ha='right')
        ax.set_ylabel('Total Copper Deposited (kg/m³)', fontweight='bold', fontsize=11)
        ax.set_title('Scenario Comparison: Best and Worst Cases',
                     fontweight='bold', fontsize=14)
        ax.grid(True, axis='y', alpha=0.3)
        ax.legend(loc='best', fontsize=10)

        plt.tight_layout()

        # Save
        output_path = self.figures_dir / 'summary_comparison.png'
        plt.savefig(output_path, dpi=300, bbox_inches='tight')
        print(f"✓ Saved: {output_path}")

        plt.close()

    def print_summary_table(self):
        """Print summary statistics table to console."""
        print(f"\n{'='*80}")
        print("SENSITIVITY SUMMARY")
        print(f"{'='*80}\n")

        # Get baseline
        baseline_row = self.df_success[self.df_success['scenario'] == 'baseline']
        if len(baseline_row) > 0:
            baseline_copper = baseline_row.iloc[0]['total_copper']
            print(f"Baseline Total Copper: {baseline_copper:.2f} kg/m³\n")

        print(f"{'Parameter':<30} {'Min Cu':<12} {'Max Cu':<12} {'Range':<12} {'Sensitivity':<12}")
        print(f"{'-'*80}")

        # Calculate for each parameter
        param_stats = []
        for param_name in self.param_config.keys():
            param_data = self.df_success[self.df_success['parameter'] == param_name]

            if len(param_data) < 2:
                continue

            min_cu = param_data['total_copper'].min()
            max_cu = param_data['total_copper'].max()
            range_cu = max_cu - min_cu

            param_stats.append({
                'parameter': param_name,
                'min': min_cu,
                'max': max_cu,
                'range': range_cu
            })

        # Sort by range
        param_stats.sort(key=lambda x: x['range'], reverse=True)

        for item in param_stats:
            # Classify sensitivity
            if item['range'] > 100:
                sensitivity = 'HIGH'
            elif item['range'] > 50:
                sensitivity = 'MODERATE'
            else:
                sensitivity = 'LOW'

            param_label = item['parameter'].replace('_', ' ').title()
            print(f"{param_label:<30} {item['min']:>10.1f}  {item['max']:>10.1f}  "
                  f"{item['range']:>10.1f}  {sensitivity:<12}")

        print(f"{'='*80}\n")


def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(
        description='Generate visualizations from sensitivity analysis results'
    )
    parser.add_argument(
        'results_csv',
        help='Path to sensitivity results CSV file'
    )

    args = parser.parse_args()

    results_path = Path(args.results_csv)
    if not results_path.exists():
        print(f"ERROR: Results file not found: {results_path}")
        sys.exit(1)

    # Config directory is in parent directory
    config_dir = results_path.parent.parent / 'config'

    # Create visualizer
    visualizer = SensitivityVisualizer(results_path, config_dir)

    # Generate plots
    print(f"{'='*80}")
    print("GENERATING VISUALIZATIONS")
    print(f"{'='*80}")

    visualizer.create_tornado_diagram()
    visualizer.create_summary_comparison()

    # Print summary
    visualizer.print_summary_table()

    print(f"{'='*80}")
    print("VISUALIZATIONS COMPLETE")
    print(f"{'='*80}")
    print(f"Output directory: {visualizer.figures_dir}")
    print(f"{'='*80}\n")


if __name__ == '__main__':
    main()
