import { existsSync, readFileSync } from 'node:fs';
import { dirname, isAbsolute, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parse } from 'yaml';

const rootPath = dirname(dirname(dirname(dirname(fileURLToPath(import.meta.url)))));

function normalizePath(filePath) {
  if (!filePath) {
    throw new Error('YAML path must be provided');
  }

  return isAbsolute(filePath) ? filePath : resolve(rootPath, filePath);
}

export function resolveRepoPath(relativePath) {
  return normalizePath(relativePath);
}

export function loadYaml(filePath) {
  const resolvedPath = normalizePath(filePath);

  if (!existsSync(resolvedPath)) {
    throw new Error(`YAML file not found: ${resolvedPath}`);
  }

  const content = readFileSync(resolvedPath, 'utf8');

  try {
    const parsed = parse(content);
    return parsed ?? {};
  } catch (error) {
    throw new Error(`Failed to parse YAML: ${resolvedPath}. ${error?.message || 'unknown parse error'}`);
  }
}
