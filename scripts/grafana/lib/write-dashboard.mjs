import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname } from 'node:path';

export function writeDashboard(outputPath, dashboard) {
  mkdirSync(dirname(outputPath), { recursive: true });
  writeFileSync(outputPath, `${JSON.stringify(dashboard, null, 2)}\n`);
  console.log(`Generated ${outputPath}`);
  return outputPath;
}
