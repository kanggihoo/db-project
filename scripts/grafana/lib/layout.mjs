const PROMETHEUS_ROW_PANEL_HEIGHT = 1;
const PROMETHEUS_ROW_PANEL_WIDTH = 24;

const DEFAULT_GRID_BY_TYPE = {
  stat: { w: 4, h: 4 },
  timeseries: { w: 12, h: 8 },
  table: { w: 12, h: 8 },
};

export function rowPanelGrid(y) {
  return {
    h: PROMETHEUS_ROW_PANEL_HEIGHT,
    w: PROMETHEUS_ROW_PANEL_WIDTH,
    x: 0,
    y,
  };
}

export function createRowLayoutState() {
  return {
    cursorX: 0,
    cursorY: 0,
    lineHeight: 0,
  };
}

function useDefaultLayout(panelType, fallbackState) {
  const defaultSpec = DEFAULT_GRID_BY_TYPE[panelType] || DEFAULT_GRID_BY_TYPE.timeseries;
  let { cursorX, cursorY, lineHeight } = fallbackState;
  const w = defaultSpec.w;
  const h = defaultSpec.h;

  if (cursorX + w > PROMETHEUS_ROW_PANEL_WIDTH) {
    cursorX = 0;
    cursorY += lineHeight;
    lineHeight = 0;
  }

  const yOffset = cursorY;
  cursorX += w;
  lineHeight = Math.max(lineHeight, h);

  if (cursorX >= PROMETHEUS_ROW_PANEL_WIDTH) {
    cursorX = 0;
    cursorY += lineHeight;
    lineHeight = 0;
  }

  return {
    layout: {
      x: 0,
      yOffset,
      w,
      h,
    },
    nextState: { cursorX, cursorY, lineHeight },
  };
}

function normalizeType(type) {
  return String(type || '').toLowerCase();
}

function hasLayoutOverride(layout) {
  if (!layout || typeof layout !== 'object') {
    return false;
  }
  return Object.keys(layout).some((key) => ['x', 'yOffset', 'w', 'h'].includes(key));
}

export function panelGrid(panelSpec, rowContentY, layoutState) {
  const panelType = normalizeType(panelSpec.type);
  const defaultSpec = DEFAULT_GRID_BY_TYPE[panelType] || DEFAULT_GRID_BY_TYPE.timeseries;

  if (hasLayoutOverride(panelSpec.layout)) {
    const layout = panelSpec.layout || {};
    const w = Number.isFinite(layout.w) ? Number(layout.w) : defaultSpec.w;
    const h = Number.isFinite(layout.h) ? Number(layout.h) : defaultSpec.h;
    const x = Number.isFinite(layout.x) ? Number(layout.x) : 0;
    const yOffset = Number.isFinite(layout.yOffset) ? Number(layout.yOffset) : 0;

    return {
      grid: {
        x,
        y: rowContentY + yOffset,
        w,
        h,
      },
      yOffset,
      h,
      x,
    };
  }

  const fallback = useDefaultLayout(panelType, layoutState || createRowLayoutState());
  layoutState.cursorX = fallback.nextState.cursorX;
  layoutState.cursorY = fallback.nextState.cursorY;
  layoutState.lineHeight = fallback.nextState.lineHeight;

  const yOffset = fallback.layout.yOffset;
  return {
    grid: {
      x: fallback.layout.x,
      y: rowContentY + yOffset,
      w: fallback.layout.w,
      h: fallback.layout.h,
    },
    yOffset,
    h: fallback.layout.h,
    x: fallback.layout.x,
  };
}
