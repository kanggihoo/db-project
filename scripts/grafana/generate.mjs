import { compileDashboard } from './lib/dashboard-compiler.mjs';
import { writeDashboard } from './lib/write-dashboard.mjs';

const result = compileDashboard();
writeDashboard(result.output, result.dashboard);
