import { basename, extname, join } from 'node:path';
import { readdirSync } from 'node:fs';
import { loadYaml, resolveRepoPath } from './yaml-loader.mjs';

const queriesPath = resolveRepoPath('scripts/grafana/queries');

function isYamlFile(fileName) {
  return ['.yml', '.yaml'].includes(extname(fileName).toLowerCase());
}

function validateQuery(alias, definition, sourceFile) {
  if (typeof definition !== 'object' || definition === null) {
    throw new Error(`Invalid query definition for ${alias} in ${sourceFile}`);
  }

  if (typeof definition.expr !== 'string' || definition.expr.trim() === '') {
    throw new Error(`Query ${alias} in ${sourceFile} must include a non-empty expr`);
  }
}

export function loadQueryRegistry() {
  const files = readdirSync(queriesPath)
    .filter(isYamlFile)
    .sort();
  const registry = {};

  for (const file of files) {
    const filePath = join(queriesPath, file);
    const raw = loadYaml(filePath);

    if (!raw || typeof raw !== 'object') {
      throw new Error(`Query file must be an object: ${filePath}`);
    }

    for (const [alias, definition] of Object.entries(raw)) {
      if (registry[alias]) {
        throw new Error(`Duplicate query alias ${alias} found in ${filePath}`);
      }

      validateQuery(alias, definition, filePath);
      registry[alias] = {
        alias,
        expr: definition.expr,
        zeroWhenNoData: Boolean(definition.zeroWhenNoData),
        source: basename(filePath),
      };
    }
  }

  return registry;
}

export function getQuery(registry, alias, panelTitle) {
  const query = registry[alias];
  if (!query) {
    throw new Error(`Unknown query alias ${alias} referenced by panel ${panelTitle}`);
  }
  return query;
}
